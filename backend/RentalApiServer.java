import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.Executors;

/**
 * Small JSON REST API for the DriveEase Next.js frontend.
 * It deliberately reuses the existing rental rules and MySQL persistence layer.
 */
public class RentalApiServer {
    private static final ObjectMapper JSON = new ObjectMapper();
    private static final double GST_RATE = 0.18;
    private static final double MAX_DISCOUNT_PERCENT = 30.0;
    private final SmartCarRentalSystem.RentalSystem system;
    private final String adminPassword;

    private RentalApiServer(SmartCarRentalSystem.RentalSystem system) {
        this.system = system;
        this.adminPassword = System.getenv().getOrDefault("ADMIN_PASSWORD", "admin123");
    }

    public static void main(String[] args) throws Exception {
        SmartCarRentalSystem.RentalSystem system = new SmartCarRentalSystem.RentalSystem();
        int port = Integer.parseInt(System.getenv().getOrDefault("PORT", "8080"));
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
        RentalApiServer api = new RentalApiServer(system);
        server.createContext("/api/", api::handle);
        server.setExecutor(Executors.newFixedThreadPool(8));
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            server.stop(1);
            system.close();
        }));
        server.start();
        System.out.println("VeloRent API running at http://localhost:" + port);
    }

    private void handle(HttpExchange exchange) throws IOException {
        addCors(exchange);
        if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(204, -1);
            return;
        }
        try {
            String method = exchange.getRequestMethod().toUpperCase(Locale.ROOT);
            String path = exchange.getRequestURI().getPath();
            Map<String, String> query = queryParams(exchange.getRequestURI().getRawQuery());

            if ("/api/health".equals(path) && "GET".equals(method)) {
                send(exchange, 200, Map.of("status", "ok", "service", "VeloRent API", "database", "connected"));
                return;
            }
            synchronized (system) {
                if ("/api/vehicles".equals(path) && "GET".equals(method)) {
                    listVehicles(exchange, query);
                } else if ("/api/bookings".equals(path) && "POST".equals(method)) {
                    createBooking(exchange);
                } else if ("/api/bookings".equals(path) && "GET".equals(method)) {
                    listCustomerBookings(exchange, query);
                } else if (path.matches("/api/bookings/\\d+/(cancel|return)") && "POST".equals(method)) {
                    updateCustomerBooking(exchange, path);
                } else if (path.startsWith("/api/admin/")) {
                    requireAdmin(exchange);
                    handleAdmin(exchange, method, path);
                } else {
                    send(exchange, 404, Map.of("error", "Endpoint not found"));
                }
            }
        } catch (SecurityException ex) {
            send(exchange, 401, Map.of("error", ex.getMessage()));
        } catch (IllegalArgumentException ex) {
            send(exchange, 400, Map.of("error", ex.getMessage()));
        } catch (Exception ex) {
            ex.printStackTrace();
            send(exchange, 500, Map.of("error", "The request could not be completed. Check the backend log."));
        }
    }

    private void listVehicles(HttpExchange exchange, Map<String, String> query) throws IOException {
        system.refreshStatuses();
        LocalDate start = query.containsKey("startDate") ? LocalDate.parse(query.get("startDate")) : LocalDate.now();
        int days = query.containsKey("days") ? Integer.parseInt(query.get("days")) : 1;
        if (days < 1 || days > 90) throw new IllegalArgumentException("Rental duration must be between 1 and 90 days.");
        List<Map<String, Object>> vehicles = new ArrayList<>();
        for (SmartCarRentalSystem.Vehicle v : system.fleet) {
            Map<String, Object> item = vehicleJson(v);
            item.put("available", v.status != SmartCarRentalSystem.VehicleStatus.MAINTENANCE && system.isFree(v.id, start, days));
            vehicles.add(item);
        }
        send(exchange, 200, vehicles);
    }

    private void createBooking(HttpExchange exchange) throws IOException {
        JsonNode body = body(exchange);
        String customer = requiredText(body, "customer");
        int vehicleId = requiredInt(body, "vehicleId");
        int days = requiredInt(body, "days");
        LocalDate start = LocalDate.parse(requiredText(body, "startDate"));
        String promoCode = body.path("promoCode").asText("").trim();
        if (customer.length() > 120) throw new IllegalArgumentException("Customer name must be 120 characters or fewer.");
        if (days < 1 || days > 90) throw new IllegalArgumentException("Rental duration must be between 1 and 90 days.");
        if (start.isBefore(LocalDate.now())) throw new IllegalArgumentException("Pickup date cannot be in the past.");
        SmartCarRentalSystem.Vehicle vehicle = system.findVehicle(vehicleId);
        if (vehicle == null) throw new IllegalArgumentException("The selected vehicle does not exist.");
        if (vehicle.status == SmartCarRentalSystem.VehicleStatus.MAINTENANCE || !system.isFree(vehicleId, start, days)) {
            throw new IllegalArgumentException("This vehicle is not available for the selected dates.");
        }

        SmartCarRentalSystem.Booking booking = new SmartCarRentalSystem.Booking();
        booking.id = system.nextBookingId();
        booking.customer = customer.trim();
        booking.vehicleId = vehicle.id;
        booking.category = vehicle.category();
        booking.vehicleName = vehicle.name();
        booking.startDay = start;
        booking.days = days;

        double gross = vehicle.rentalCost(days);
        int completed = system.completedRentals(customer);
        double loyaltyPercent = completed >= 6 ? 10 : completed >= 3 ? 5 : 0;
        double loyaltyDiscount = gross * loyaltyPercent / 100.0;
        double promoDiscount = 0;
        String discountNote = loyaltyPercent > 0 ? "Loyalty " + (int) loyaltyPercent + "%" : "";
        if (!promoCode.isBlank()) {
            SmartCarRentalSystem.Promo promo = system.findPromo(promoCode);
            if (promo == null || !promo.active) throw new IllegalArgumentException("The promo code is invalid or inactive.");
            if (days < promo.minDays) throw new IllegalArgumentException("This promo requires at least " + promo.minDays + " rental days.");
            promoDiscount = gross * promo.percent / 100.0;
            if (promo.maxOff > 0) promoDiscount = Math.min(promoDiscount, promo.maxOff);
            discountNote += (discountNote.isBlank() ? "" : " + ") + promo.code;
        }
        booking.discount = Math.min(loyaltyDiscount + promoDiscount, gross * MAX_DISCOUNT_PERCENT / 100.0);
        booking.discountNote = discountNote;
        booking.rent = gross - booking.discount;
        booking.tax = booking.rent * GST_RATE;
        booking.deposit = vehicle.securityDeposit();

        system.bookings.add(booking);
        try {
            system.saveAll();
        } catch (RuntimeException ex) {
            system.bookings.remove(booking);
            throw ex;
        }
        send(exchange, 201, bookingJson(booking));
    }

    private void listCustomerBookings(HttpExchange exchange, Map<String, String> query) throws IOException {
        String customer = query.getOrDefault("customer", "").trim();
        if (customer.isBlank()) throw new IllegalArgumentException("Provide a customer query parameter.");
        List<Map<String, Object>> result = new ArrayList<>();
        for (SmartCarRentalSystem.Booking b : system.bookings) {
            if (b.customer.equalsIgnoreCase(customer)) result.add(bookingJson(b));
        }
        send(exchange, 200, result);
    }

    private void updateCustomerBooking(HttpExchange exchange, String path) throws IOException {
        String[] parts = path.split("/");
        int id = Integer.parseInt(parts[3]);
        String action = parts[4];
        JsonNode body = body(exchange);
        String customer = requiredText(body, "customer");
        SmartCarRentalSystem.Booking booking = system.findBooking(id);
        if (booking == null || !booking.customer.equalsIgnoreCase(customer)) {
            throw new IllegalArgumentException("No booking with that ID was found for this customer.");
        }
        if (booking.status != SmartCarRentalSystem.BookingStatus.ACTIVE) {
            throw new IllegalArgumentException("This booking is already " + booking.status + ".");
        }
        if ("cancel".equals(action)) {
            boolean late = ChronoUnit.DAYS.between(LocalDate.now(), booking.startDay) < 2;
            booking.cancelFee = late ? booking.rent * 0.20 : 0;
            booking.status = SmartCarRentalSystem.BookingStatus.CANCELLED;
        } else {
            if (LocalDate.now().isBefore(booking.startDay)) throw new IllegalArgumentException("The rental has not started yet.");
            SmartCarRentalSystem.Vehicle vehicle = system.findVehicle(booking.vehicleId);
            long lateDays = Math.max(0, ChronoUnit.DAYS.between(booking.dueDay(), LocalDate.now()));
            booking.fine = vehicle == null ? 0 : vehicle.lateFeePerDay() * lateDays;
            booking.returnDay = LocalDate.now();
            booking.status = SmartCarRentalSystem.BookingStatus.RETURNED;
            if (vehicle != null) {
                vehicle.recordCompletedRental();
                vehicle.status = vehicle.needsService() ? SmartCarRentalSystem.VehicleStatus.MAINTENANCE : SmartCarRentalSystem.VehicleStatus.AVAILABLE;
            }
        }
        system.saveAll();
        send(exchange, 200, bookingJson(booking));
    }

    private void handleAdmin(HttpExchange exchange, String method, String path) throws IOException {
        if ("/api/admin/bookings".equals(path) && "GET".equals(method)) {
            List<Map<String, Object>> result = new ArrayList<>();
            for (SmartCarRentalSystem.Booking b : system.bookings) result.add(bookingJson(b));
            send(exchange, 200, result);
            return;
        }
        if ("/api/admin/vehicles".equals(path) && "POST".equals(method)) {
            JsonNode body = body(exchange);
            String category = requiredText(body, "category");
            String brand = requiredText(body, "brand");
            String model = requiredText(body, "model");
            String fuel = requiredText(body, "fuel");
            if (!Set.of("Hatchback", "Sedan", "SUV", "Luxury").contains(category)) throw new IllegalArgumentException("Unsupported vehicle category.");
            if (!Set.of("Petrol", "Diesel", "Electric", "CNG").contains(fuel)) throw new IllegalArgumentException("Unsupported fuel type.");
            int seats = requiredInt(body, "seats");
            double price = requiredDouble(body, "pricePerDay");
            double rating = body.path("rating").asDouble(0);
            if (seats < 2 || seats > 12 || price < 100 || rating < 0 || rating > 5) throw new IllegalArgumentException("Check seats, daily price and rating values.");
            SmartCarRentalSystem.Vehicle v = SmartCarRentalSystem.makeVehicle(category, system.nextVehicleId(), brand, model, fuel, seats, price, rating, SmartCarRentalSystem.VehicleStatus.AVAILABLE, 0, 0);
            system.fleet.add(v);
            system.saveVehicles();
            send(exchange, 201, vehicleJson(v));
            return;
        }
        if (path.matches("/api/admin/vehicles/\\d+/price") && "PATCH".equals(method)) {
            int id = Integer.parseInt(path.split("/")[4]);
            SmartCarRentalSystem.Vehicle v = system.findVehicle(id);
            if (v == null) throw new IllegalArgumentException("Vehicle not found.");
            double price = requiredDouble(body(exchange), "pricePerDay");
            if (price < 100) throw new IllegalArgumentException("Daily price must be at least ₹100.");
            v.pricePerDay = price;
            system.saveVehicles();
            send(exchange, 200, vehicleJson(v));
            return;
        }
        if (path.matches("/api/admin/vehicles/\\d+") && "DELETE".equals(method)) {
            int id = Integer.parseInt(path.substring("/api/admin/vehicles/".length()));
            SmartCarRentalSystem.Vehicle v = system.findVehicle(id);
            if (v == null) throw new IllegalArgumentException("Vehicle not found.");
            if (system.activeBookingCount(id) > 0) throw new IllegalArgumentException("Cannot remove a vehicle with active or upcoming bookings.");
            system.fleet.remove(v);
            system.saveVehicles();
            send(exchange, 200, Map.of("message", "Vehicle removed."));
            return;
        }
        if (path.matches("/api/admin/vehicles/\\d+/maintenance") && "POST".equals(method)) {
            int id = Integer.parseInt(path.split("/")[4]);
            SmartCarRentalSystem.Vehicle v = system.findVehicle(id);
            if (v == null) throw new IllegalArgumentException("Vehicle not found.");
            boolean complete = body(exchange).path("complete").asBoolean(false);
            if (complete) v.completeService();
            else {
                if (v.status == SmartCarRentalSystem.VehicleStatus.RENTED) throw new IllegalArgumentException("A rented vehicle cannot be sent to maintenance.");
                v.status = SmartCarRentalSystem.VehicleStatus.MAINTENANCE;
            }
            system.saveAll();
            send(exchange, 200, vehicleJson(v));
            return;
        }
        if ("/api/admin/promos".equals(path) && "GET".equals(method)) {
            List<Map<String, Object>> promos = new ArrayList<>();
            for (SmartCarRentalSystem.Promo p : system.promos) promos.add(promoJson(p));
            send(exchange, 200, promos);
            return;
        }
        if ("/api/admin/promos".equals(path) && "POST".equals(method)) {
            JsonNode body = body(exchange);
            String code = requiredText(body, "code").toUpperCase(Locale.ROOT);
            if (system.findPromo(code) != null) throw new IllegalArgumentException("Promo code already exists.");
            double percent = requiredDouble(body, "percent");
            double maxOff = body.path("maxOff").asDouble(0);
            int minDays = body.path("minDays").asInt(1);
            if (percent <= 0 || percent > 50 || maxOff < 0 || minDays < 1 || minDays > 90) throw new IllegalArgumentException("Invalid promo settings.");
            SmartCarRentalSystem.Promo promo = new SmartCarRentalSystem.Promo(code, percent, maxOff, minDays, true);
            system.promos.add(promo);
            system.savePromos();
            send(exchange, 201, promoJson(promo));
            return;
        }
        if (path.matches("/api/admin/promos/[^/]+") && "PATCH".equals(method)) {
            String code = path.substring("/api/admin/promos/".length());
            SmartCarRentalSystem.Promo promo = system.findPromo(code);
            if (promo == null) throw new IllegalArgumentException("Promo code not found.");
            promo.active = body(exchange).path("active").asBoolean(!promo.active);
            system.savePromos();
            send(exchange, 200, promoJson(promo));
            return;
        }
        if ("/api/admin/reports".equals(path) && "GET".equals(method)) {
            send(exchange, 200, revenueReport());
            return;
        }
        send(exchange, 404, Map.of("error", "Admin endpoint not found"));
    }

    private Map<String, Object> revenueReport() {
        double rental = 0, tax = 0, fines = 0, cancellationFees = 0, discounts = 0;
        int active = 0, returned = 0, cancelled = 0;
        Map<String, Double> byCategory = new TreeMap<>();
        for (SmartCarRentalSystem.Booking b : system.bookings) {
            if (b.status == SmartCarRentalSystem.BookingStatus.ACTIVE) active++;
            else if (b.status == SmartCarRentalSystem.BookingStatus.CANCELLED) {
                cancelled++;
                cancellationFees += b.cancelFee;
                byCategory.merge(b.category, b.cancelFee, Double::sum);
            } else {
                returned++;
                rental += b.rent;
                tax += b.tax;
                fines += b.fine;
                discounts += b.discount;
                byCategory.merge(b.category, b.revenue(), Double::sum);
            }
        }
        Map<String, Object> report = new LinkedHashMap<>();
        report.put("activeBookings", active);
        report.put("returnedBookings", returned);
        report.put("cancelledBookings", cancelled);
        report.put("rentalIncome", rental);
        report.put("taxCollected", tax);
        report.put("lateFees", fines);
        report.put("cancellationFees", cancellationFees);
        report.put("discountsGiven", discounts);
        report.put("totalRevenue", rental + tax + fines + cancellationFees);
        report.put("revenueByCategory", byCategory);
        return report;
    }

    private Map<String, Object> vehicleJson(SmartCarRentalSystem.Vehicle v) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("id", v.id);
        item.put("name", v.name());
        item.put("brand", v.brand);
        item.put("model", v.model);
        item.put("type", v.category());
        item.put("seats", v.seats);
        item.put("fuel", v.fuel);
        item.put("price", v.pricePerDay);
        item.put("rating", v.rating);
        item.put("status", v.status.name());
        item.put("transmission", "Not specified");
        item.put("image", "");
        item.put("description", v.name() + " " + v.category() + " available for rental.");
        item.put("rentalsSinceService", v.rentalsSinceService);
        item.put("totalRentals", v.totalRentals);
        return item;
    }

    private Map<String, Object> bookingJson(SmartCarRentalSystem.Booking b) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("id", b.id);
        item.put("customer", b.customer);
        item.put("vehicleId", b.vehicleId);
        item.put("vehicleName", b.vehicleName);
        item.put("category", b.category);
        item.put("startDate", b.startDay.toString());
        item.put("returnDate", b.dueDay().toString());
        item.put("days", b.days);
        item.put("rent", b.rent);
        item.put("tax", b.tax);
        item.put("deposit", b.deposit);
        item.put("fine", b.fine);
        item.put("cancelFee", b.cancelFee);
        item.put("discount", b.discount);
        item.put("discountNote", b.discountNote);
        item.put("status", b.status.name());
        item.put("total", b.rent + b.tax + b.deposit + b.fine);
        return item;
    }

    private Map<String, Object> promoJson(SmartCarRentalSystem.Promo p) {
        return Map.of("code", p.code, "percent", p.percent, "maxOff", p.maxOff, "minDays", p.minDays, "active", p.active);
    }

    private void requireAdmin(HttpExchange exchange) {
        String supplied = exchange.getRequestHeaders().getFirst("X-Admin-Password");
        if (supplied == null || !adminPassword.equals(supplied)) throw new SecurityException("A valid admin password is required.");
    }

    private static JsonNode body(HttpExchange exchange) throws IOException {
        return JSON.readTree(exchange.getRequestBody());
    }

    private static String requiredText(JsonNode body, String key) {
        String value = body.path(key).asText("").trim();
        if (value.isBlank()) throw new IllegalArgumentException("Missing required field: " + key);
        return value;
    }

    private static int requiredInt(JsonNode body, String key) {
        if (!body.has(key) || !body.get(key).canConvertToInt()) throw new IllegalArgumentException("Invalid or missing field: " + key);
        return body.get(key).asInt();
    }

    private static double requiredDouble(JsonNode body, String key) {
        if (!body.has(key) || !body.get(key).isNumber()) throw new IllegalArgumentException("Invalid or missing field: " + key);
        return body.get(key).asDouble();
    }

    private static Map<String, String> queryParams(String raw) {
        Map<String, String> values = new HashMap<>();
        if (raw == null || raw.isBlank()) return values;
        for (String part : raw.split("&")) {
            String[] pair = part.split("=", 2);
            values.put(java.net.URLDecoder.decode(pair[0], StandardCharsets.UTF_8),
                pair.length > 1 ? java.net.URLDecoder.decode(pair[1], StandardCharsets.UTF_8) : "");
        }
        return values;
    }

    private static void addCors(HttpExchange exchange) {
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", System.getenv().getOrDefault("CORS_ORIGIN", "http://localhost:3000"));
        exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET,POST,PATCH,DELETE,OPTIONS");
        exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type,X-Admin-Password");
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
    }

    private static void send(HttpExchange exchange, int status, Object payload) throws IOException {
        byte[] bytes = JSON.writeValueAsBytes(payload);
        exchange.sendResponseHeaders(status, bytes.length);
        try (var output = exchange.getResponseBody()) {
            output.write(bytes);
        }
    }
}
