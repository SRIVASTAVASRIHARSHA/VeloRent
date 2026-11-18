import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.Scanner;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/** End-to-end checks against a real MySQL instance. The CI workflow provides an isolated database. */
class SmartCarRentalSystemIntegrationTest {
    private static SmartCarRentalSystem.RentalSystem system;
    private static PrintStream originalOut;
    private static ByteArrayOutputStream capturedOut;

    private static void input(String text) {
        SmartCarRentalSystem.SC = new Scanner(text);
    }

    private static String output() {
        return capturedOut.toString(StandardCharsets.UTF_8);
    }

    @BeforeAll
    static void setUp() {
        String testUrl = System.getenv("DB_URL");
        if (testUrl == null || !testUrl.contains("/smart_car_rental_test")) {
            throw new IllegalStateException("Integration tests require DB_URL to target the isolated smart_car_rental_test database.");
        }
        originalOut = System.out;
        capturedOut = new ByteArrayOutputStream();
        System.setOut(new PrintStream(capturedOut, true, StandardCharsets.UTF_8));
        system = new SmartCarRentalSystem.RentalSystem();
    }

    @AfterAll
    static void tearDown() {
        if (system != null) system.close();
        System.setOut(originalOut);
        SmartCarRentalSystem.SC = new Scanner(System.in);
    }

    @Test
    void allCustomerAndAdminOperationsPersistAndReload() {
        assertEquals(16, system.fleet.size(), "A new DB should receive the sample fleet");
        assertEquals(3, system.promos.size(), "A new DB should receive default promos");

        // Category surcharges must be included when applying a customer's daily budget.
        SmartCarRentalSystem.Preference luxuryBudget = new SmartCarRentalSystem.Preference();
        luxuryBudget.budget = 9800;
        luxuryBudget.passengers = 2;
        luxuryBudget.days = 1;
        luxuryBudget.fuel = "";
        luxuryBudget.category = "Luxury";
        luxuryBudget.start = LocalDate.now();
        assertTrue(system.recommend(luxuryBudget).isEmpty(), "Luxury category surcharges must count against the budget");
        luxuryBudget.budget = 10000;
        assertFalse(system.recommend(luxuryBudget).isEmpty());
        input("31-02-2026\n01-03-2026\n");
        assertEquals(LocalDate.of(2026, 3, 1), SmartCarRentalSystem.askDate("Strict date validation"),
            "An impossible calendar date must be rejected and requested again");

        // Browse fleet and calculate an estimate.
        system.browseFleet();
        input("101\n2\n");
        system.costCalculator();
        assertTrue(output().contains("Rental:"));

        // Find-and-book workflow: filters, recommendations, quote, promo prompt, confirmation.
        input("5000\n2\n1\n1\n1\n\n1\n\ny\n");
        system.findAndBook("Integration Customer");
        assertFalse(system.bookings.isEmpty());
        SmartCarRentalSystem.Booking returned = system.bookings.get(system.bookings.size() - 1);
        assertEquals(SmartCarRentalSystem.BookingStatus.ACTIVE, returned.status);

        // Customer return path.
        input(returned.id + "\n");
        system.processReturn("Integration Customer", false);
        assertEquals(SmartCarRentalSystem.BookingStatus.RETURNED, returned.status);

        // Create and cancel a future booking.
        SmartCarRentalSystem.Vehicle secondVehicle = system.findVehicle(102);
        input("\ny\n");
        system.makeBooking("Integration Customer", secondVehicle, 2, LocalDate.now().plusDays(2));
        SmartCarRentalSystem.Booking cancelled = system.bookings.get(system.bookings.size() - 1);
        input(cancelled.id + "\ny\n");
        system.cancelBooking("Integration Customer");
        assertEquals(SmartCarRentalSystem.BookingStatus.CANCELLED, cancelled.status);

        // Admin return path with an explicitly entered return date.
        SmartCarRentalSystem.Vehicle thirdVehicle = system.findVehicle(103);
        input("\ny\n");
        system.makeBooking("Admin Return Customer", thirdVehicle, 1, LocalDate.now());
        SmartCarRentalSystem.Booking adminReturn = system.bookings.get(system.bookings.size() - 1);
        input(adminReturn.id + "\n\n");
        system.processReturn("", true);
        assertEquals(SmartCarRentalSystem.BookingStatus.RETURNED, adminReturn.status);

        // Rental history and reporting.
        system.showHistory("Integration Customer");
        system.viewBookings();
        system.revenueReport();
        assertTrue(output().contains("REVENUE REPORT"));

        // Admin vehicle CRUD: add, update price, maintenance start/complete, remove.
        int newVehicleId = system.nextVehicleId();
        input("2\nQA Motors\nTest Hatch\n2\n5\n1500\n4.5\n");
        system.addVehicle();
        SmartCarRentalSystem.Vehicle added = system.findVehicle(newVehicleId);
        assertNotNull(added);

        input(newVehicleId + "\n1750\n");
        system.updatePrice();
        assertEquals(1750.0, added.pricePerDay, 0.001);

        input("1\n" + newVehicleId + "\n2\n" + newVehicleId + "\n0\n");
        system.maintenanceMenu();
        assertEquals(SmartCarRentalSystem.VehicleStatus.AVAILABLE, added.status);
        assertEquals(0, added.rentalsSinceService);

        input(newVehicleId + "\n");
        system.removeVehicle();
        assertNull(system.findVehicle(newVehicleId));

        // Promo code CRUD: add, then disable.
        input("1\nQA2026\n15\n1000\n1\n2\nQA2026\n0\n");
        system.promoMenu();
        SmartCarRentalSystem.Promo testPromo = system.findPromo("QA2026");
        assertNotNull(testPromo);
        assertFalse(testPromo.active);

        // Exercise the actual customer/admin menu dispatchers too.
        input("Menu Customer\n1\n5\n6\n101\n1\n0\n");
        system.customerMenu();
        input("admin123\n1\n5\n8\n9\n0\n0\n");
        system.adminMenu();

        // Force final persistence, close and reconstruct from MySQL.
        int expectedBookings = system.bookings.size();
        int expectedVehicles = system.fleet.size();
        int expectedPromos = system.promos.size();
        system.saveAll();
        system.close();
        system = new SmartCarRentalSystem.RentalSystem();
        assertEquals(expectedBookings, system.bookings.size(), "Bookings must survive application restart");
        assertEquals(expectedVehicles, system.fleet.size(), "Vehicles must survive application restart");
        assertEquals(expectedPromos, system.promos.size(), "Promo codes must survive application restart");
    }
}
