import java.io.*;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.*;
import java.sql.*;
public class SmartCarRentalSystem {
    private static final double GST_RATE = 0.18;
    private static final double MAX_TOTAL_DISCOUNT_PCT = 30.0;
    private static final int SERVICE_INTERVAL = 5;
    private static final String ADMIN_PASSWORD = System.getenv().getOrDefault("ADMIN_PASSWORD", "admin123");
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd-MM-uuuu").withResolverStyle(ResolverStyle.STRICT);
    static Scanner SC = new Scanner(System.in);
    // These values help us keep track of the current status of vehicles and bookings.
    // For example, a vehicle can be available, rented, or under maintenance.
    // These are the different statuses used by vehicles and bookings.
    enum VehicleStatus {
        AVAILABLE, RENTED, MAINTENANCE
    }
    enum BookingStatus {
        ACTIVE, RETURNED, CANCELLED
    }
    // Small helper methods used in different parts of the program.
    // They take input, check it, and format values before displaying or saving them.
    // These methods help with user input and displaying information.
    static String clean(String s) {
        return s == null ? "" : s.replace('|', ' ').trim();
    }
    static String money(double n) {
        return String.format(Locale.US, "Rs.%.2f", n);
    }
    static String lower(String s) {
        return s.toLowerCase(Locale.ROOT);
    }
    static String upper(String s) {
        return s.toUpperCase(Locale.ROOT);
    }
    static void line() {
        System.out.println("-".repeat(90));
    }
    // Reads text from the user. If empty input is not allowed, it asks again.
    static String askString(String prompt, boolean allowEmpty) {
        while (true) {
            System.out.print(prompt);
            String s = SC.nextLine().trim();
            if (!s.isEmpty() || allowEmpty) return s;
            System.out.println("  Please enter a value.");
        }
    }
    static String askString(String prompt) {
        return askString(prompt, false);
    }
    // Reads an integer and keeps asking until the user enters a valid number in the given range.
    static int askInt(String prompt, int min, int max) {
        while (true) {
            try {
                int n = Integer.parseInt(askString(prompt));
                if (n >= min && n <= max) return n;
            } catch (NumberFormatException ignored) {
            }
            System.out.printf("  Enter a whole number between %d and %d.%n", min, max);
        }
    }
    // Similar to askInt(), but this method is used when decimal values are allowed.
    static double askDouble(String prompt, double min, double max) {
        while (true) {
            try {
                double n = Double.parseDouble(askString(prompt));
                if (Double.isFinite(n) && n >= min && n <= max) return n;
            } catch (NumberFormatException ignored) {
            }
            System.out.printf("  Enter a number between %.2f and %.2f.%n", min, max);
        }
    }
    // Gets a date from the user. If the user presses Enter, today's date is used.
    static LocalDate askDate(String prompt) {
        while (true) {
            String s = askString(prompt + " [DD-MM-YYYY, Enter = today]: ", true);
            if (s.isEmpty()) return LocalDate.now();
            try {
                return LocalDate.parse(s, DATE_FORMAT);
            }
            catch (DateTimeParseException ex) {
                System.out.println("  Invalid date.");
            }
        }
    }
    static String date(LocalDate d) {
        return d.format(DATE_FORMAT);
    }
    // Splits one saved line into separate values using the | symbol.
    static List<String> split(String line) {
        return Arrays.asList(line.split("\\|", -1));
    }
    // This part contains the vehicle classes used by the rental system.
    // Vehicle classes and their different categories.
    // Parent class for all vehicle types. Common details and common calculations are kept here.
    // Each child class provides its own category and can change the pricing rules.
    static abstract class Vehicle {
        protected int id;
        protected String brand, model, fuel;
        protected int seats;
        protected double pricePerDay, rating;
        protected VehicleStatus status;
        protected int rentalsSinceService, totalRentals;
        Vehicle(int id, String brand, String model, String fuel, int seats, double price,
        double rating, VehicleStatus status, int since, int total) {
            this.id=id;
            this.brand=brand;
            this.model=model;
            this.fuel=fuel;
            this.seats=seats;
            this.pricePerDay=price;
            this.rating=rating;
            this.status=status;
            this.rentalsSinceService=since;
            this.totalRentals=total;
        }
        abstract String category();
        // Calculates the normal rental amount before any category-specific changes.
        double rentalCost(int days) {
            return pricePerDay * days;
        }
        // Calculates the security deposit that the customer has to pay.
        double securityDeposit() {
            return Math.max(2000.0, pricePerDay * 2.0);
        }
        double lateFeePerDay() {
            return pricePerDay * 1.5;
        }
        String name() {
            return brand + " " + model;
        }
        // Checks whether the vehicle has completed enough rentals to need servicing.
        boolean needsService() {
            return rentalsSinceService >= SERVICE_INTERVAL;
        }
        // Updates the rental counters after a vehicle is returned.
        void recordCompletedRental() {
            rentalsSinceService++;
            totalRentals++;
        }
        void completeService() {
            rentalsSinceService=0;
            status=VehicleStatus.AVAILABLE;
        }
        // Turns the vehicle details into one line so they can be saved in a text file.
        String serialize() {
            return String.join("|", category(), String.valueOf(id), brand, model, fuel,
            String.valueOf(seats), String.valueOf(pricePerDay), String.valueOf(rating),
            String.valueOf(status.ordinal()), String.valueOf(rentalsSinceService), String.valueOf(totalRentals));
        }
    }
    // Hatchback uses the common vehicle details, with its own long-rental discount.
    static class Hatchback extends Vehicle {
        Hatchback(int id,String b,String m,String f,int s,double p,double r,VehicleStatus st,int since,int total) {
            super(id,b,m,f,s,p,r,st,since,total);
        }
        @Override String category() {
            return "Hatchback";
        }
        @Override double rentalCost(int days) {
            double base=pricePerDay*days;
            return days>=7?base*.90:base;
        }
    }
    // Sedan has its own deposit amount and discount rule.
    static class Sedan extends Vehicle {
        Sedan(int id,String b,String m,String f,int s,double p,double r,VehicleStatus st,int since,int total) {
            super(id,b,m,f,s,p,r,st,since,total);
        }
        @Override String category() {
            return "Sedan";
        }
        @Override double securityDeposit() {
            return Math.max(3000.0,pricePerDay*2.0);
        }
        @Override double rentalCost(int days) {
            double base=pricePerDay*days;
            return days>=7?base*.95:base;
        }
    }
    // SUV has a higher deposit and a small extra charge in its rental cost.
    static class SUV extends Vehicle {
        SUV(int id,String b,String m,String f,int s,double p,double r,VehicleStatus st,int since,int total) {
            super(id,b,m,f,s,p,r,st,since,total);
        }
        @Override String category() {
            return "SUV";
        }
        @Override double securityDeposit() {
            return Math.max(5000.0,pricePerDay*2.5);
        }
        @Override double rentalCost(int days) {
            return pricePerDay*days*1.05;
        }
    }
    // Luxury vehicles have a higher deposit and late-return charge.
    static class Luxury extends Vehicle {
        Luxury(int id,String b,String m,String f,int s,double p,double r,VehicleStatus st,int since,int total) {
            super(id,b,m,f,s,p,r,st,since,total);
        }
        @Override String category() {
            return "Luxury";
        }
        @Override double securityDeposit() {
            return Math.max(15000.0,pricePerDay*3.0);
        }
        @Override double lateFeePerDay() {
            return pricePerDay*2.0;
        }
        @Override double rentalCost(int days) {
            return pricePerDay*days*1.10;
        }
    }
    // Creates the correct child class based on the category selected or loaded from a file.
    static Vehicle makeVehicle(String cat,int id,String b,String m,String f,int seats,double price,double rating,VehicleStatus st,int since,int total) {
        switch (lower(cat)) {
            case "hatchback": return new Hatchback(id,b,m,f,seats,price,rating,st,since,total);
            case "sedan": return new Sedan(id,b,m,f,seats,price,rating,st,since,total);
            case "suv": return new SUV(id,b,m,f,seats,price,rating,st,since,total);
            case "luxury": return new Luxury(id,b,m,f,seats,price,rating,st,since,total);
            default: return null;
        }
    }
    // These classes store booking details, discount codes, and the customer's vehicle choices.
    // Classes that store booking details and customer choices.
    // Stores all the information related to one customer's booking.
    static class Booking {
        int id, vehicleId, days;
        String customer, category, vehicleName, discountNote="";
        LocalDate startDay, returnDay=LocalDate.of(1970,1,1);
        double rent, tax, deposit, fine, cancelFee, discount;
        BookingStatus status=BookingStatus.ACTIVE;
        LocalDate dueDay() {
            return startDay.plusDays(days);
        }
        // Returns the amount that should count as revenue for this booking's current status.
        double revenue() {
            if(status==BookingStatus.RETURNED)return rent+tax+fine;
            if(status==BookingStatus.CANCELLED)return cancelFee;
            return 0;
        }
        String serialize() {
            return String.join("|",String.valueOf(id),customer,String.valueOf(vehicleId),category,vehicleName,
            String.valueOf(startDay.toEpochDay()),String.valueOf(days),String.valueOf(rent),String.valueOf(tax),String.valueOf(deposit),
            String.valueOf(fine),String.valueOf(cancelFee),String.valueOf(status.ordinal()),String.valueOf(returnDay.toEpochDay()),
            String.valueOf(discount),discountNote);
        }
    }
    // Stores the details of one discount code, such as its percentage and minimum rental days.
    static class Promo {
        String code;
        double percent,maxOff;
        int minDays;
        boolean active;
        Promo(String c,double p,double m,int d,boolean a) {
            code=c;
            percent=p;
            maxOff=m;
            minDays=d;
            active=a;
        }
        String serialize() {
            return String.join("|",code,String.valueOf(percent),String.valueOf(maxOff),String.valueOf(minDays),active?"1":"0");
        }
    }
    // Holds the customer's choices that are used to find suitable vehicles.
    static class Preference {
        double budget;
        int passengers,days;
        String fuel,category;
        LocalDate start;
    }
    // Stores a vehicle together with the score given by the recommendation system.
    static class Recommendation {
        Vehicle vehicle;
        double score;
        Recommendation(Vehicle v,double s) {
            vehicle=v;
            score=s;
        }
    }
    // This is the main working part of the project. It manages vehicles, bookings, files, and menus.
    // This class manages the main work of the rental system.
    static class RentalSystem {
        final List<Vehicle> fleet=new ArrayList<>();
        final List<Booking> bookings=new ArrayList<>();
        final List<Promo> promos=new ArrayList<>();
        private Connection connection;

        // Connects to MySQL and loads the saved project information.
        RentalSystem() {
            try {
                String url=System.getenv().getOrDefault(
                    "DB_URL",
                    "jdbc:mysql://localhost:3306/smart_car_rental?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC"
                );
                String user=System.getenv().getOrDefault("DB_USER","root");
                String password=System.getenv().getOrDefault("DB_PASSWORD","");
                connection=DriverManager.getConnection(url,user,password);
                createTables();
                loadVehicles();
                loadBookings();
                loadPromos();
                refreshStatuses();
                saveVehicles();
            } catch(SQLException e) {
                if(connection!=null) {
                    try { connection.close(); }
                    catch(SQLException closeError) { e.addSuppressed(closeError); }
                }
                System.out.println("Could not connect to MySQL: "+e.getMessage());
                System.out.println("Check that MySQL is running, the database exists, and DB_USER/DB_PASSWORD are correct.");
                throw new IllegalStateException("Database connection is required to run this application.",e);
            }
        }

        // Creates the tables the first time the application is connected to the database.
        void createTables() throws SQLException {
            String vehiclesSql="CREATE TABLE IF NOT EXISTS vehicles ("+
                "id INT PRIMARY KEY, category VARCHAR(30) NOT NULL, brand VARCHAR(60) NOT NULL, "+
                "model VARCHAR(80) NOT NULL, fuel VARCHAR(30) NOT NULL, seats INT NOT NULL, "+
                "price_per_day DOUBLE NOT NULL, rating DOUBLE NOT NULL, status VARCHAR(20) NOT NULL, "+
                "rentals_since_service INT NOT NULL, total_rentals INT NOT NULL)";
            String bookingsSql="CREATE TABLE IF NOT EXISTS bookings ("+
                "id INT PRIMARY KEY, customer VARCHAR(120) NOT NULL, vehicle_id INT NOT NULL, "+
                "category VARCHAR(30) NOT NULL, vehicle_name VARCHAR(150) NOT NULL, "+
                "start_day DATE NOT NULL, days INT NOT NULL, rent DOUBLE NOT NULL, tax DOUBLE NOT NULL, "+
                "deposit DOUBLE NOT NULL, fine DOUBLE NOT NULL, cancel_fee DOUBLE NOT NULL, "+
                "status VARCHAR(20) NOT NULL, return_day DATE NOT NULL, discount DOUBLE NOT NULL, "+
                "discount_note VARCHAR(255))";
            String promosSql="CREATE TABLE IF NOT EXISTS promos ("+
                "code VARCHAR(40) PRIMARY KEY, percent DOUBLE NOT NULL, max_off DOUBLE NOT NULL, "+
                "min_days INT NOT NULL, active BOOLEAN NOT NULL)";
            try(Statement st=connection.createStatement()) {
                st.executeUpdate(vehiclesSql);
                st.executeUpdate(bookingsSql);
                st.executeUpdate(promosSql);
            }
        }

        // Reads vehicle records from the MySQL vehicles table.
        void loadVehicles() throws SQLException {
            fleet.clear();
            String sql="SELECT id,category,brand,model,fuel,seats,price_per_day,rating,status,"+
                "rentals_since_service,total_rentals FROM vehicles ORDER BY id";
            try(Statement st=connection.createStatement(); ResultSet rs=st.executeQuery(sql)) {
                while(rs.next()) {
                    Vehicle v=makeVehicle(
                        rs.getString("category"),rs.getInt("id"),rs.getString("brand"),
                        rs.getString("model"),rs.getString("fuel"),rs.getInt("seats"),
                        rs.getDouble("price_per_day"),rs.getDouble("rating"),
                        VehicleStatus.valueOf(rs.getString("status")),
                        rs.getInt("rentals_since_service"),rs.getInt("total_rentals")
                    );
                    if(v!=null)fleet.add(v);
                }
            }
            // Add the sample fleet only when the database has no vehicles yet.
            if(fleet.isEmpty()) {
                seedFleet();
                saveVehicles();
            }
        }

        // Reads all previous bookings from the MySQL bookings table.
        void loadBookings() throws SQLException {
            bookings.clear();
            String sql="SELECT id,customer,vehicle_id,category,vehicle_name,start_day,days,rent,tax,"+
                "deposit,fine,cancel_fee,status,return_day,discount,discount_note FROM bookings ORDER BY id";
            try(Statement st=connection.createStatement(); ResultSet rs=st.executeQuery(sql)) {
                while(rs.next()) {
                    Booking b=new Booking();
                    b.id=rs.getInt("id");
                    b.customer=rs.getString("customer");
                    b.vehicleId=rs.getInt("vehicle_id");
                    b.category=rs.getString("category");
                    b.vehicleName=rs.getString("vehicle_name");
                    b.startDay=rs.getDate("start_day").toLocalDate();
                    b.days=rs.getInt("days");
                    b.rent=rs.getDouble("rent");
                    b.tax=rs.getDouble("tax");
                    b.deposit=rs.getDouble("deposit");
                    b.fine=rs.getDouble("fine");
                    b.cancelFee=rs.getDouble("cancel_fee");
                    b.status=BookingStatus.valueOf(rs.getString("status"));
                    b.returnDay=rs.getDate("return_day").toLocalDate();
                    b.discount=rs.getDouble("discount");
                    b.discountNote=rs.getString("discount_note");
                    bookings.add(b);
                }
            }
        }

        // Loads the discount codes saved in the MySQL promos table.
        void loadPromos() throws SQLException {
            promos.clear();
            String sql="SELECT code,percent,max_off,min_days,active FROM promos ORDER BY code";
            try(Statement st=connection.createStatement(); ResultSet rs=st.executeQuery(sql)) {
                while(rs.next()) {
                    promos.add(new Promo(rs.getString("code"),rs.getDouble("percent"),
                        rs.getDouble("max_off"),rs.getInt("min_days"),rs.getBoolean("active")));
                }
            }
            // Add the default discount codes when this is a new database.
            if(promos.isEmpty()) {
                promos.add(new Promo("WELCOME10",10,1500,1,true));
                promos.add(new Promo("WEEKEND15",15,3000,2,true));
                promos.add(new Promo("LONGSTAY20",20,6000,7,true));
                savePromos();
            }
        }

        // Saves the current vehicle list to the MySQL vehicles table.
        void saveVehicles() {
            String deleteSql="DELETE FROM vehicles";
            String insertSql="INSERT INTO vehicles(id,category,brand,model,fuel,seats,price_per_day,rating,status,rentals_since_service,total_rentals) VALUES(?,?,?,?,?,?,?,?,?,?,?)";
            try {
                connection.setAutoCommit(false);
                try(Statement del=connection.createStatement()) { del.executeUpdate(deleteSql); }
                try(PreparedStatement ps=connection.prepareStatement(insertSql)) {
                    for(Vehicle v:fleet) {
                        ps.setInt(1,v.id);
                        ps.setString(2,v.category());
                        ps.setString(3,v.brand);
                        ps.setString(4,v.model);
                        ps.setString(5,v.fuel);
                        ps.setInt(6,v.seats);
                        ps.setDouble(7,v.pricePerDay);
                        ps.setDouble(8,v.rating);
                        ps.setString(9,v.status.name());
                        ps.setInt(10,v.rentalsSinceService);
                        ps.setInt(11,v.totalRentals);
                        ps.addBatch();
                    }
                    ps.executeBatch();
                }
                connection.commit();
            } catch(SQLException e) {
                try { connection.rollback(); } catch(SQLException rollbackError) { e.addSuppressed(rollbackError); }
                throw new IllegalStateException("Vehicle database save failed; the transaction was rolled back.",e);
            } finally {
                try { connection.setAutoCommit(true); }
                catch(SQLException e) { throw new IllegalStateException("Could not restore database transaction mode after saving vehicles.",e); }
            }
        }

        // Saves the current booking list to the MySQL bookings table.
        void saveBookings() {
            String insertSql="INSERT INTO bookings(id,customer,vehicle_id,category,vehicle_name,start_day,days,rent,tax,deposit,fine,cancel_fee,status,return_day,discount,discount_note) VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
            try {
                connection.setAutoCommit(false);
                try(Statement del=connection.createStatement()) { del.executeUpdate("DELETE FROM bookings"); }
                try(PreparedStatement ps=connection.prepareStatement(insertSql)) {
                    for(Booking b:bookings) {
                        ps.setInt(1,b.id);
                        ps.setString(2,b.customer);
                        ps.setInt(3,b.vehicleId);
                        ps.setString(4,b.category);
                        ps.setString(5,b.vehicleName);
                        ps.setDate(6,java.sql.Date.valueOf(b.startDay));
                        ps.setInt(7,b.days);
                        ps.setDouble(8,b.rent);
                        ps.setDouble(9,b.tax);
                        ps.setDouble(10,b.deposit);
                        ps.setDouble(11,b.fine);
                        ps.setDouble(12,b.cancelFee);
                        ps.setString(13,b.status.name());
                        ps.setDate(14,java.sql.Date.valueOf(b.returnDay));
                        ps.setDouble(15,b.discount);
                        ps.setString(16,b.discountNote);
                        ps.addBatch();
                    }
                    ps.executeBatch();
                }
                connection.commit();
            } catch(SQLException e) {
                try { connection.rollback(); } catch(SQLException rollbackError) { e.addSuppressed(rollbackError); }
                throw new IllegalStateException("Booking database save failed; the transaction was rolled back.",e);
            } finally {
                try { connection.setAutoCommit(true); }
                catch(SQLException e) { throw new IllegalStateException("Could not restore database transaction mode after saving bookings.",e); }
            }
        }

        // Saves the current promotional codes to the MySQL promos table.
        void savePromos() {
            String insertSql="INSERT INTO promos(code,percent,max_off,min_days,active) VALUES(?,?,?,?,?)";
            try {
                connection.setAutoCommit(false);
                try(Statement del=connection.createStatement()) { del.executeUpdate("DELETE FROM promos"); }
                try(PreparedStatement ps=connection.prepareStatement(insertSql)) {
                    for(Promo p:promos) {
                        ps.setString(1,p.code);
                        ps.setDouble(2,p.percent);
                        ps.setDouble(3,p.maxOff);
                        ps.setInt(4,p.minDays);
                        ps.setBoolean(5,p.active);
                        ps.addBatch();
                    }
                    ps.executeBatch();
                }
                connection.commit();
            } catch(SQLException e) {
                try { connection.rollback(); } catch(SQLException rollbackError) { e.addSuppressed(rollbackError); }
                throw new IllegalStateException("Promo database save failed; the transaction was rolled back.",e);
            } finally {
                try { connection.setAutoCommit(true); }
                catch(SQLException e) { throw new IllegalStateException("Could not restore database transaction mode after saving promo codes.",e); }
            }
        }

        // Updates vehicle status and saves all project information to MySQL.
        void saveAll() {
            refreshStatuses();
            saveVehicles();
            saveBookings();
            savePromos();
        }

        // Adds sample vehicles when the program is run for the first time.
        void seedFleet() {
            String[][] data= {
                {
                    "Hatchback","Maruti","Swift","Petrol","5","1200","4.2"
                }
                , {
                    "Hatchback","Tata","Tiago EV","Electric","5","1500","4.4"
                }
                , {
                    "Hatchback","Hyundai","i20","Petrol","5","1400","4.3"
                }
                , {
                    "Sedan","Honda","City","Petrol","5","2200","4.5"
                }
                , {
                    "Sedan","Hyundai","Verna","Diesel","5","2100","4.3"
                }
                , {
                    "Sedan","Maruti","Dzire CNG","CNG","5","1700","4.1"
                }
                , {
                    "SUV","Mahindra","XUV700","Diesel","7","3500","4.6"
                }
                , {
                    "SUV","Toyota","Innova Crysta","Diesel","7","3300","4.7"
                }
                , {
                    "SUV","Tata","Nexon EV","Electric","5","2800","4.4"
                }
                , {
                    "Luxury","BMW","5 Series","Petrol","5","9000","4.8"
                }
                , {
                    "Luxury","Mercedes","E-Class","Diesel","5","9500","4.9"
                }
                , {
                    "Hatchback","Maruti","Wagon R CNG","CNG","5","1000","4.0"
                }
                , {
                    "Sedan","Skoda","Slavia","Petrol","5","2400","4.4"
                }
                , {
                    "SUV","Hyundai","Creta","Petrol","5","3000","4.5"
                }
                , {
                    "SUV","Mahindra","Scorpio-N","Diesel","7","3400","4.5"
                }
                , {
                    "Luxury","Audi","A6","Petrol","5","9800","4.7"
                }
            }
            ;
            int id=101;
            for(String[] d:data)fleet.add(makeVehicle(d[0],id++,d[1],d[2],d[3],Integer.parseInt(d[4]),Double.parseDouble(d[5]),Double.parseDouble(d[6]),VehicleStatus.AVAILABLE,0,0));
        }
        // Helper methods for finding records and checking whether a vehicle can be booked.
        // Methods for finding vehicles and checking their availability.
        Vehicle findVehicle(int id) {
            for(Vehicle v:fleet)if(v.id==id)return v;
            return null;
        }
        Booking findBooking(int id) {
            for(Booking b:bookings)if(b.id==id)return b;
            return null;
        }
        Promo findPromo(String code) {
            for(Promo p:promos)if(p.code.equals(upper(code)))return p;
            return null;
        }
        int nextVehicleId() {
            return fleet.stream().mapToInt(v->v.id).max().orElse(100)+1;
        }
        int nextBookingId() {
            return bookings.stream().mapToInt(b->b.id).max().orElse(1000)+1;
        }
        int completedRentals(String customer) {
            int n=0;
            for(Booking b:bookings)if(b.customer.equalsIgnoreCase(customer)&&b.status==BookingStatus.RETURNED)n++;
            return n;
        }
        boolean isNewCustomer(String customer) {
            for(Booking b:bookings)if(b.customer.equalsIgnoreCase(customer))return false;
            return true;
        }
        int activeBookingCount(int vehicleId) {
            int n=0;
            for(Booking b:bookings)if(b.vehicleId==vehicleId&&b.status==BookingStatus.ACTIVE)n++;
            return n;
        }
        // Checks whether the vehicle is free for every day of the requested rental period.
        boolean isFree(int vehicleId,LocalDate start,int days) {
            LocalDate end=start.plusDays(days);
            for(Booking b:bookings) {
                if(b.vehicleId!=vehicleId||b.status!=BookingStatus.ACTIVE)continue;
                LocalDate occupiedUntil=b.dueDay().isBefore(LocalDate.now())?LocalDate.of(9999,12,31):b.dueDay();
                if(start.isBefore(occupiedUntil)&&b.startDay.isBefore(end))return false;
            }
            return true;
        }
        // Updates vehicle status based on current bookings and service requirements.
        void refreshStatuses() {
            for(Vehicle v:fleet) {
                if(v.status==VehicleStatus.MAINTENANCE)continue;
                boolean out=false;
                for(Booking b:bookings)if(b.vehicleId==v.id&&b.status==BookingStatus.ACTIVE&&!b.startDay.isAfter(LocalDate.now())) {
                    out=true;
                    break;
                }
                v.status=out?VehicleStatus.RENTED:VehicleStatus.AVAILABLE;
            }
        }
        void printFleet() {
            System.out.printf("%-6s %-12s %-24s %-10s %-7s %-12s %-8s %s%n","ID","Category","Vehicle","Fuel","Seats","Rs/day","Rating","Status");
            line();
            for(Vehicle v:fleet)System.out.printf(Locale.US,"%-6d %-12s %-24s %-10s %-7d %-12.0f %-8.1f %s%n",v.id,v.category(),v.name(),v.fuel,v.seats,v.pricePerDay,v.rating,v.status);
        }
        String pickFuel() {
            System.out.println("  Fuel: 1) Any  2) Petrol  3) Diesel  4) Electric  5) CNG");
            int c=askInt("  Choose fuel (1-5): ",1,5);
            return c==1?"":new String[] {
                "Petrol","Diesel","Electric","CNG"
            }
            [c-2];
        }
        String pickCategory() {
            System.out.println("  Category: 1) Any  2) Hatchback  3) Sedan  4) SUV  5) Luxury");
            int c=askInt("  Choose category (1-5): ",1,5);
            return c==1?"":new String[] {
                "Hatchback","Sedan","SUV","Luxury"
            }
            [c-2];
        }
        // This method finds and ranks vehicles based on the customer's choices.
        // Finds vehicles matching the customer's choices, gives each one a score, and sorts the results.
        // A higher score means the vehicle is a better match for the customer.
        List<Recommendation> recommend(Preference p) {
            List<Recommendation> out=new ArrayList<>();
            // First, skip vehicles that do not match the customer's requirements.
            // Then, calculate a score for each vehicle that matches.
            for(Vehicle v:fleet) {
                double effectiveDailyPrice=v.rentalCost(p.days)/p.days;
                if(v.status==VehicleStatus.MAINTENANCE||!isFree(v.id,p.start,p.days)||effectiveDailyPrice>p.budget||v.seats<p.passengers)continue;
                if(!p.fuel.isEmpty()&&!v.fuel.equalsIgnoreCase(p.fuel))continue;
                if(!p.category.isEmpty()&&!v.category().equalsIgnoreCase(p.category))continue;
                double ratingScore=v.rating/5.0;
                double priceScore=1.0-(effectiveDailyPrice/p.budget)*.9;
                double spare=Math.min(v.seats-p.passengers,4);
                double seatScore=1.0-spare/5.0;
                out.add(new Recommendation(v,.5*ratingScore+.3*priceScore+.2*seatScore));
            }
            // Finally, put the vehicles with the highest scores at the top.
            out.sort((a,b)-> {
                int c=Double.compare(b.score,a.score);if(c!=0)return c;c=Double.compare(b.vehicle.rating,a.vehicle.rating);return c!=0?c:Double.compare(a.vehicle.pricePerDay,b.vehicle.pricePerDay);
            }
            );
            return out;
        }
        // Methods used by customers to browse, book, cancel, and return vehicles.
        void browseFleet() {
            System.out.println("\nFull fleet");
            printFleet();
        }
        // Displays the cost details of a booking, including rent, tax, deposit, and discounts.
        void printInvoice(Booking b,String title) {
            System.out.println("\n"+"=".repeat(70)+"\n  "+title+"\n"+"=".repeat(70));
            System.out.println("  Booking ID : "+b.id+"\n  Customer   : "+b.customer+"\n  Vehicle    : "+b.vehicleName+" ("+b.category+", ID "+b.vehicleId+")\n  Period     : "+date(b.startDay)+" to "+date(b.dueDay())+" ("+b.days+" days)");
            line();
            System.out.printf("%-42s %18s%n","  Rental charges",money(b.rent+b.discount));
            if(b.discount>0) {
                System.out.printf("%-42s %18s%n","  Discount ("+b.discountNote+")","-"+money(b.discount));
                System.out.printf("%-42s %18s%n","  Rental after discount",money(b.rent));
            }
            System.out.printf("%-42s %18s%n","  GST (18%)",money(b.tax));
            if(b.fine>0)System.out.printf("%-42s %18s%n","  Late-return fine",money(b.fine));
            if(b.cancelFee>0)System.out.printf("%-42s %18s%n","  Cancellation fee",money(b.cancelFee));
            System.out.printf("%-42s %18s%n","  Security deposit (refundable)",money(b.deposit));
            line();
        }
        // Takes the customer's preferences, shows matching vehicles, and lets them choose one to book.
        void findAndBook(String customer) {
            Preference p=new Preference();
            System.out.println("\n--- Find your car ---");
            p.budget=askDouble("  Max budget per day (Rs): ",500,1000000);
            p.passengers=askInt("  Number of passengers: ",1,12);
            p.fuel=pickFuel();
            p.category=pickCategory();
            p.days=askInt("  Rental duration (days): ",1,90);
            p.start=askDate("  Pickup date");
            if(p.start.isBefore(LocalDate.now())) {
                System.out.println("  Pickup date cannot be in the past.");
                return;
            }
            List<Recommendation> recs=recommend(p);
            if(recs.isEmpty()) {
                System.out.println("No vehicle is available for those dates and preferences.");
                return;
            }
            System.out.printf("%-5s %-6s %-12s %-22s %-10s %-7s %-8s %-18s %s%n","#","ID","Category","Vehicle","Fuel","Seats","Rating","Total (excl GST)","Match");
            line();
            for(int i=0;i<recs.size();i++) {
                Recommendation r=recs.get(i);
                Vehicle v=r.vehicle;
                System.out.printf(Locale.US,"%-5d %-6d %-12s %-22s %-10s %-7d %-8.1f %-18.0f %.0f%%%n",i+1,v.id,v.category(),v.name(),v.fuel,v.seats,v.rating,v.rentalCost(p.days),r.score*100);
            }
            int choice=askInt("Enter the # to book (0 to go back): ",0,recs.size());
            if(choice>0)makeBooking(customer,recs.get(choice-1).vehicle,p.days,p.start);
        }
        // Creates a booking after checking availability and calculating the final amount.
        void makeBooking(String customer,Vehicle v,int days,LocalDate start) {
            if(!isFree(v.id,start,days)) {
                System.out.println("Sorry, that car was just taken for those dates.");
                return;
            }
            Booking b=new Booking();
            b.id=nextBookingId();
            b.customer=clean(customer);
            b.vehicleId=v.id;
            b.category=v.category();
            b.vehicleName=v.name();
            b.startDay=start;
            b.days=days;
            double gross=v.rentalCost(days);
            int completed=completedRentals(customer);
            double loyaltyPct=completed>=6?10:completed>=3?5:0;
            double loyaltyOff=gross*loyaltyPct/100.0,promoOff=0;
            String note="";
            if(loyaltyPct>0) {
                note="Loyalty "+(int)loyaltyPct+"%";
                System.out.println("Loyalty discount applied: "+(int)loyaltyPct+"%.");
            }
            if(isNewCustomer(customer))System.out.println("Tip: new customers can use code WELCOME10.");
            while(true) {
                String code=askString("  Promo code (Enter to skip): ",true);
                if(code.isEmpty())break;
                Promo promo=findPromo(code);
                if(promo==null||!promo.active) {
                    System.out.println("Invalid or expired code.");
                    continue;
                }
                if(days<promo.minDays) {
                    System.out.println(promo.code+" needs a rental of at least "+promo.minDays+" days.");
                    continue;
                }
                promoOff=gross*promo.percent/100.0;
                if(promo.maxOff>0)promoOff=Math.min(promoOff,promo.maxOff);
                note+=(note.isEmpty()?"":" + ")+promo.code;
                System.out.println("Code "+promo.code+" accepted.");
                break;
            }
            double totalOff=Math.min(loyaltyOff+promoOff,gross*MAX_TOTAL_DISCOUNT_PCT/100.0);
            b.discount=totalOff;
            b.discountNote=clean(note);
            b.rent=gross-totalOff;
            b.tax=b.rent*GST_RATE;
            b.deposit=v.securityDeposit();
            printInvoice(b,"BOOKING QUOTE");
            System.out.println("Payable now (rent + GST + deposit): "+money(b.rent+b.tax+b.deposit));
            if(!askString("Confirm booking? (y/n): ").equalsIgnoreCase("y")) {
                System.out.println("Booking discarded.");
                return;
            }
            bookings.add(b);
            saveAll();
            System.out.println("Booking confirmed. Your booking ID is "+b.id+".");
        }
        // Finds the customer's active booking and cancels it using the project's cancellation rules.
        void cancelBooking(String customer) {
            int id=askInt("Booking ID to cancel: ",1,99999999);
            Booking b=findBooking(id);
            if(b==null||!b.customer.equalsIgnoreCase(customer)) {
                System.out.println("No booking with that ID under your name.");
                return;
            }
            if(b.status!=BookingStatus.ACTIVE) {
                System.out.println("That booking is already "+b.status+".");
                return;
            }
            boolean late=java.time.temporal.ChronoUnit.DAYS.between(LocalDate.now(),b.startDay)<2;
            double fee=late?b.rent*.20:0;
            double refund=b.rent+b.tax+b.deposit-fee;
            System.out.println("Cancellation fee: "+money(fee)+"\nRefund due: "+money(refund));
            if(!askString("Confirm cancellation? (y/n): ").equalsIgnoreCase("y"))return;
            b.status=BookingStatus.CANCELLED;
            b.cancelFee=fee;
            saveAll();
            System.out.println("Booking "+id+" cancelled.");
        }
        // Handles returning a vehicle and calculates any late fee if the vehicle is returned late.
        void processReturn(String customer,boolean admin) {
            int id=askInt("Booking ID being returned: ",1,99999999);
            Booking b=findBooking(id);
            if(b==null||(!admin&&!b.customer.equalsIgnoreCase(customer))) {
                System.out.println("No booking with that ID"+(admin?"":" under your name")+".");
                return;
            }
            if(b.status!=BookingStatus.ACTIVE) {
                System.out.println("That booking is already "+b.status+".");
                return;
            }
            Vehicle v=findVehicle(b.vehicleId);
            LocalDate ret=admin?askDate("  Return date"):LocalDate.now();
            if(ret.isAfter(LocalDate.now())) {
                System.out.println("Return date cannot be in the future.");
                return;
            }
            if(ret.isBefore(b.startDay)) {
                System.out.println("Rental has not started yet. Cancel the booking instead.");
                return;
            }
            long lateDays=Math.max(0,Duration.between(b.dueDay().atStartOfDay(),ret.atStartOfDay()).toDays());
            b.fine=v==null?0:v.lateFeePerDay()*lateDays;
            b.returnDay=ret;
            b.status=BookingStatus.RETURNED;
            if(v!=null) {
                v.recordCompletedRental();
                if(v.needsService())v.status=VehicleStatus.MAINTENANCE;
                else v.status=VehicleStatus.AVAILABLE;
            }
            // Persist the return before displaying a success confirmation or final invoice.
            saveAll();
            if(!admin)System.out.println("Return recorded for today ("+date(ret)+").");
            printInvoice(b,"FINAL INVOICE");
            if(lateDays>0)System.out.println("Returned "+lateDays+" day(s) late.");
            if(v!=null&&v.status==VehicleStatus.MAINTENANCE)
                System.out.println(v.name()+" has completed "+v.rentalsSinceService+" rentals and is now scheduled for maintenance.");
            double balance=b.deposit-b.fine;
            System.out.println((balance>=0?"Deposit refunded: ":"Extra amount due: ")+money(Math.abs(balance)));
            System.out.println("Total charged for the rental: "+money(b.rent+b.tax+b.fine));
        }
        // Shows all previous and current bookings made by the given customer.
        void showHistory(String customer) {
            boolean any=false;
            double spent=0;
            System.out.println("\nRental history for "+customer);
            for(Booking b:bookings)if(b.customer.equalsIgnoreCase(customer)) {
                any=true;
                System.out.printf("Booking %d | %-22s | %s | %d days | %s | %s%n",b.id,b.vehicleName,date(b.startDay),b.days,b.status,money(b.revenue()));
                spent+=b.revenue();
            }
            if(!any)System.out.println("No bookings found.");
            else System.out.println("Total spent: "+money(spent));
        }
        // Lets the user estimate the rental cost without making an actual booking.
        void costCalculator() {
            browseFleet();
            int id=askInt("Vehicle ID: ",1,999999);
            Vehicle v=findVehicle(id);
            if(v==null) {
                System.out.println("Unknown vehicle ID.");
                return;
            }
            int days=askInt("Days: ",1,365);
            double rent=v.rentalCost(days),tax=rent*GST_RATE;
            System.out.println("Rental: "+money(rent)+" GST: "+money(tax)+" Total: "+money(rent+tax)+" Deposit: "+money(v.securityDeposit())+" Late fee/day: "+money(v.lateFeePerDay()));
        }
        // Displays the options available to a customer after they enter their name.
        void customerMenu() {
            String name=askString("\nEnter your name: ");
            while(true) {
                refreshStatuses();
                System.out.println("\n=== Customer Menu ("+name+") ===\n 1. Browse fleet\n 2. Find & book a car\n 3. Cancel a booking\n 4. Return a car\n 5. My rental history\n 6. Rental cost calculator\n 0. Back");
                int c=askInt("Choice: ",0,6);
                switch(c) {
                    case 0:return;
                    case 1:browseFleet();
                    break;
                    case 2:findAndBook(name);
                    break;
                    case 3:cancelBooking(name);
                    break;
                    case 4:processReturn(name,false);
                    break;
                    case 5:showHistory(name);
                    break;
                    case 6:costCalculator();
                    break;
                }
            }
        }
        // The following methods are used by the admin to manage the rental business.
        // Methods used by the admin to manage vehicles, bookings, and reports.
        // Takes vehicle details from the admin and adds the new vehicle to the fleet.
        void addVehicle() {
            System.out.println("\n--- Add vehicle ---");
            String cat=pickCategory();
            if(cat.isEmpty()) {
                System.out.println("Choose a specific category.");
                return;
            }
            String brand=clean(askString("Brand: ")),model=clean(askString("Model: ")),fuel=pickFuel();
            if(fuel.isEmpty()) {
                System.out.println("Choose a specific fuel.");
                return;
            }
            int seats=askInt("Seats: ",2,12);
            double price=askDouble("Price per day (Rs): ",100,1000000),rating=askDouble("Initial rating (0-5): ",0,5);
            Vehicle v=makeVehicle(cat,nextVehicleId(),brand,model,fuel,seats,price,rating,VehicleStatus.AVAILABLE,0,0);
            fleet.add(v);
            saveVehicles();
            System.out.println("Added vehicle ID "+v.id);
        }
        // Removes a vehicle only when it does not have active or upcoming bookings.
        void removeVehicle() {
            int id=askInt("Vehicle ID to remove: ",1,999999);
            Vehicle v=findVehicle(id);
            if(v==null) {
                System.out.println("No such vehicle.");
                return;
            }
            if(activeBookingCount(id)>0) {
                System.out.println("Cannot remove a vehicle with active or upcoming bookings.");
                return;
            }
            fleet.remove(v);
            saveVehicles();
            System.out.println("Removed "+v.name()+".");
        }
        // Changes the daily rental price of a selected vehicle.
        void updatePrice() {
            int id=askInt("Vehicle ID: ",1,999999);
            Vehicle v=findVehicle(id);
            if(v==null) {
                System.out.println("No such vehicle.");
                return;
            }
            v.pricePerDay=askDouble("New price per day (Rs): ",100,1000000);
            saveVehicles();
            System.out.println("Price updated.");
        }
        // Displays the bookings stored in the system.
        void viewBookings() {
            if(bookings.isEmpty()) {
                System.out.println("No bookings yet.");
                return;
            }
            for(Booking b:bookings)System.out.printf("%-7d %-18s %-22s %-12s %-5d %-12s %s%n",b.id,b.customer,b.vehicleName,date(b.startDay),b.days,b.status,money(b.revenue()));
        }
        // Calculates and displays income from rentals, taxes, fines, and cancellation fees.
        void revenueReport() {
            Map<String,Double> byCategory=new TreeMap<>();
            Map<String,Integer> count=new TreeMap<>();
            double rental=0,tax=0,fines=0,cancel=0,given=0;
            int active=0,returned=0,cancelled=0;
            for(Booking b:bookings) {
                if(b.status==BookingStatus.ACTIVE) {
                    active++;
                    continue;
                }
                if(b.status==BookingStatus.CANCELLED) {
                    cancelled++;
                    cancel+=b.cancelFee;
                    byCategory.merge(b.category,b.cancelFee,Double::sum);
                    continue;
                }
                returned++;
                rental+=b.rent;
                tax+=b.tax;
                fines+=b.fine;
                given+=b.discount;
                byCategory.merge(b.category,b.revenue(),Double::sum);
                count.merge(b.category,1,Integer::sum);
            }
            System.out.println("\nREVENUE REPORT");
            line();
            System.out.println("Bookings: "+active+" active, "+returned+" completed, "+cancelled+" cancelled");
            System.out.println("Rental income: "+money(rental)+"\nGST collected: "+money(tax)+"\nLate-return fines: "+money(fines)+"\nCancellation fees: "+money(cancel)+"\nDiscounts given (already net): "+money(given));
            line();
            System.out.println("TOTAL REVENUE: "+money(rental+tax+fines+cancel));
            for(String category:byCategory.keySet())System.out.println(category+": "+money(byCategory.get(category))+" ("+count.getOrDefault(category,0)+" completed)");
            fleet.stream().sorted((a,b)->Integer.compare(b.totalRentals,a.totalRentals)).limit(3).filter(v->v.totalRentals>0).forEach(v->System.out.println("Most rented: "+v.name()+" - "+v.totalRentals+" rentals"));
        }
        // Allows the admin to send vehicles for service or mark their service as completed.
        void maintenanceMenu() {
            while(true) {
                System.out.println("\n--- Maintenance ---");
                for(Vehicle v:fleet)if(v.status==VehicleStatus.MAINTENANCE||v.needsService()||v.rentalsSinceService>=SERVICE_INTERVAL-1)System.out.println(v.id+" | "+v.name()+" | "+v.status+" | Rentals since service: "+v.rentalsSinceService);
                System.out.println("1. Send vehicle to service\n2. Complete service\n0. Back");
                int c=askInt("Choice: ",0,2);
                if(c==0)return;
                int id=askInt("Vehicle ID: ",1,999999);
                Vehicle v=findVehicle(id);
                if(v==null) {
                    System.out.println("No such vehicle.");
                    continue;
                }
                if(c==1) {
                    if(v.status==VehicleStatus.RENTED)System.out.println("Vehicle is rented out right now.");
                    else {
                        int upcoming=activeBookingCount(id);
                        v.status=VehicleStatus.MAINTENANCE;
                        saveAll();
                        System.out.println("Sent to maintenance.");
                        if(upcoming>0)System.out.println("Warning: "+upcoming+" active/upcoming booking(s) need review.");
                    }
                } else if(v.status!=VehicleStatus.MAINTENANCE)System.out.println("Vehicle is not in maintenance.");
                else {
                    v.completeService();
                    saveAll();
                    System.out.println("Service complete; vehicle is available again.");
                }
            }
        }
        // Allows the admin to add discount codes or enable and disable existing ones.
        void promoMenu() {
            while(true) {
                System.out.println("\n--- Promo codes ---");
                for(Promo p:promos)System.out.printf("%-14s %-8.0f %-12s %-10d %s%n",p.code,p.percent,p.maxOff>0?String.valueOf((long)p.maxOff):"none",p.minDays,p.active?"Active":"Disabled");
                System.out.println("1. Add code\n2. Enable/disable code\n0. Back");
                int c=askInt("Choice: ",0,2);
                if(c==0)return;
                if(c==1) {
                    String code=upper(clean(askString("New code: ")));
                    if(code.isEmpty()||findPromo(code)!=null) {
                        System.out.println("Code is empty or already exists.");
                        continue;
                    }
                    double pct=askDouble("Discount percent (1-50): ",1,50),cap=askDouble("Max discount in Rs (0 = no cap): ",0,1000000);
                    int days=askInt("Minimum rental days: ",1,90);
                    promos.add(new Promo(code,pct,cap,days,true));
                    savePromos();
                    System.out.println("Added "+code+".");
                } else {
                    Promo p=findPromo(askString("Code: "));
                    if(p==null) {
                        System.out.println("No such code.");
                        continue;
                    }
                    p.active=!p.active;
                    savePromos();
                    System.out.println(p.code+" is now "+(p.active?"active":"disabled")+".");
                }
            }
        }
        // Checks the admin password and then displays the admin options.
        void adminMenu() {
            if(!askString("\nAdmin password: ").equals(ADMIN_PASSWORD)) {
                System.out.println("Wrong password.");
                return;
            }
            while(true) {
                refreshStatuses();
                System.out.println("\n=== Admin Menu ===\n1. View fleet\n2. Add vehicle\n3. Remove vehicle\n4. Update daily price\n5. View all bookings\n6. Process a return\n7. Maintenance\n8. Revenue report\n9. Manage promo codes\n0. Back");
                int c=askInt("Choice: ",0,9);
                switch(c) {
                    case 0:return;
                    case 1:browseFleet();
                    break;
                    case 2:addVehicle();
                    break;
                    case 3:removeVehicle();
                    break;
                    case 4:updatePrice();
                    break;
                    case 5:viewBookings();
                    break;
                    case 6:processReturn("",true);
                    break;
                    case 7:maintenanceMenu();
                    break;
                    case 8:revenueReport();
                    break;
                    case 9:promoMenu();
                    break;
                }
            }
        }
        // Releases the MySQL connection when the application exits.
        void close() {
            if(connection!=null) {
                try { connection.close(); }
                catch(SQLException e) { System.err.println("Warning: could not close the database connection: "+e.getMessage()); }
            }
        }

        // This menu is shown first and lets the user choose Customer or Admin.
        // This is the first menu shown when the program starts.
        void run() {
            System.out.println("\n*******************************************\n        SMART CAR RENTAL SYSTEM\n*******************************************");
            while(true) {
                System.out.println("\n=== Main Menu ===\n1. Customer\n2. Admin\n0. Exit");
                int c=askInt("Choice: ",0,2);
                if(c==0) {
                    saveAll();
                    System.out.println("Thank you for using Smart Car Rental. Goodbye!");
                    return;
                }
                if(c==1)customerMenu();
                else adminMenu();
            }
        }
    }
    // Program execution starts here. Create the rental system and show its main menu.
    public static void main(String[] args) {
        RentalSystem system=null;
        int exitCode=0;
        try {
            system=new RentalSystem();
            system.run();
        } catch(IllegalStateException e) {
            System.err.println("Application stopped because a required database operation failed: "+e.getMessage());
            System.err.println("The operation was not reported as successful.");
            exitCode=1;
        } finally {
            if(system!=null)system.close();
        }
        if(exitCode!=0)System.exit(exitCode);
    }
}
