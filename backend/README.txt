SMART CAR RENTAL SYSTEM - JAVA + JDBC + MYSQL

REQUIREMENTS
- Java 21
- Maven 3.9 or newer
- MySQL Server 8.0 or newer

DATABASE SETUP
1. Start MySQL Server.
2. Open MySQL Workbench (or the MySQL command line).
3. Run database/schema.sql. It creates the smart_car_rental database and
   the vehicles, bookings, and promos tables.
4. The application creates missing tables and inserts the starter fleet and
   default promo codes when the corresponding tables are empty.

CONFIGURE AND RUN (macOS / Linux)
From this project directory:

  export DB_URL="jdbc:mysql://localhost:3306/smart_car_rental?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC"
  export DB_USER="root"
  export DB_PASSWORD="YOUR_MYSQL_PASSWORD"
  export ADMIN_PASSWORD="YOUR_ADMIN_PASSWORD"
  mvn clean compile
  mvn exec:java

The admin password defaults to admin123 for this classroom/demo project when
ADMIN_PASSWORD is not set. Set a private value before using the application.
Do not commit database or admin credentials to GitHub.

FEATURES COVERED
Customer:
- Browse the vehicle fleet
- Find and book a vehicle using budget, seats, fuel, category, dates, and recommendations
- Apply loyalty and promo discounts
- Cancel bookings
- Return vehicles and calculate late fees
- View rental history
- Calculate rental estimates

Admin:
- View fleet and bookings
- Add, remove, and reprice vehicles
- Process a vehicle return
- Send vehicles to maintenance and complete service
- View revenue reports
- Add and enable/disable promo codes

DATABASE TABLES
- vehicles: vehicle details, price, rating, availability and service counters
- bookings: customer bookings, rental charges, tax, deposit, discounts and status
- promos: discount-code settings

TESTING
The integration test uses a real MySQL database and exercises booking, return,
cancellation, vehicle management, maintenance, promo management, reports, and
reload persistence. It deliberately refuses to run unless DB_URL points to a
separate database named smart_car_rental_test.

  CREATE DATABASE smart_car_rental_test;
  export DB_URL="jdbc:mysql://localhost:3306/smart_car_rental_test?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC"
  export DB_USER="root"
  export DB_PASSWORD="YOUR_MYSQL_PASSWORD"
  export ADMIN_PASSWORD="admin123"
  mvn clean verify

IMPORTANT LIMITATIONS
- This is a single-process console application, not a multi-user web service.
- The Next.js DriveEase website is not connected to this backend yet. A REST API
  layer is still needed before the website can read or write these MySQL records.
- The default admin password is for demonstration only; configure ADMIN_PASSWORD.
- The test suite must only be run against its dedicated test database.


RUN THE WEB API (for the Next.js DriveEase frontend)
From the repository root, after creating the database and setting DB_URL/DB_USER/DB_PASSWORD/ADMIN_PASSWORD:

  cd backend
  mvn clean compile
  mvn exec:java -Dexec.mainClass=RentalApiServer

The API listens on http://localhost:8080 by default. Set PORT to change it.
Set CORS_ORIGIN if the frontend is not running at http://localhost:3000.

API ROUTES
- GET /api/health
- GET /api/vehicles?startDate=YYYY-MM-DD&days=2
- POST /api/bookings (JSON: customer, vehicleId, startDate, days, optional promoCode)
- GET /api/bookings?customer=Name
- POST /api/bookings/{id}/cancel (JSON: customer)
- POST /api/bookings/{id}/return (JSON: customer)
- GET /api/admin/bookings (X-Admin-Password header)
- POST /api/admin/vehicles (X-Admin-Password header)
- PATCH /api/admin/vehicles/{id}/price
- DELETE /api/admin/vehicles/{id}
- POST /api/admin/vehicles/{id}/maintenance
- GET/POST /api/admin/promos; PATCH /api/admin/promos/{code}
- GET /api/admin/reports

The frontend reads NEXT_PUBLIC_API_BASE_URL from the repository-root .env.local file. Use http://localhost:8080/api for local development.
