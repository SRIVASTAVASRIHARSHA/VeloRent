VELORENT – NEXT.JS FRONTEND + JAVA/JDBC/MYSQL BACKEND

OVERVIEW

VeloRent contains: - A Next.js frontend built with React and
TypeScript. - A Java backend using JDBC and MySQL. - A Java console
application for customer/admin operations. - A separate Java REST API
(RentalApiServer) intended for the Next.js frontend. - A MySQL database
named smart_car_rental.

IMPORTANT: The console application and REST API are different entry
points. Running mvn exec:java may start the console application. To
connect the website to the backend, start RentalApiServer as described
below.

REQUIREMENTS

Backend: - Java 21 - Maven 3.9 or newer - MySQL Server 8.0 or newer

Frontend: - Node.js (use a supported current LTS release) - npm
(included with Node.js)

DATABASE SETUP

1.  Start the MySQL Server.
2.  Create/import the application schema.

Windows PowerShell (run from the backend directory): Get-Content ..sql |
mysql -u root -p

macOS/Linux (run from the backend directory): mysql -u root -p <
database/schema.sql

The schema creates the smart_car_rental database and the vehicles,
bookings, and promos tables. The application can create missing tables
and seed starter data when the corresponding tables are empty.

To verify the database: mysql -u root -p

Then, in the MySQL prompt: SHOW DATABASES; USE smart_car_rental; SHOW
TABLES; exit;

BACKEND CONFIGURATION

Set the database connection values in the terminal where you will start
the backend. Replace the example password values with your own
credentials.

Windows PowerShell:
$env:DB_URL=“jdbc:mysql://localhost:3306/smart_car_rental?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC”
$env:DB_USER=“root” $env:DB_PASSWORD=“YOUR_MYSQL_PASSWORD”
$env:ADMIN_PASSWORD=“YOUR_PRIVATE_ADMIN_PASSWORD”

macOS/Linux: export
DB_URL=“jdbc:mysql://localhost:3306/smart_car_rental?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC”
export DB_USER=“root” export DB_PASSWORD=“YOUR_MYSQL_PASSWORD” export
ADMIN_PASSWORD=“YOUR_PRIVATE_ADMIN_PASSWORD”

Do not commit database credentials, admin passwords, or secret .env
files to GitHub. The demo admin password may default to admin123 if
ADMIN_PASSWORD is not set; configure a private value instead.

RUN THE JAVA CONSOLE APPLICATION

From the backend directory, compile and run:

mvn clean compile mvn exec:java

This starts the text-based customer/admin menu in the terminal. It is
not the HTTP server used by the website.

RUN THE JAVA REST API (FOR THE NEXT.JS FRONTEND)

Use a separate terminal. Set the same DB_URL, DB_USER, DB_PASSWORD, and
ADMIN_PASSWORD environment variables shown above.

From the repository root: cd backend mvn clean compile

Then start the REST API: mvn exec:java
“-Dexec.mainClass=RentalApiServer”

IMPORTANT MAVEN CONFIGURATION: The exec-maven-plugin configuration in
backend/pom.xml must allow the command-line main-class override. If the
command above still starts the console menu, open backend/pom.xml and
change the plugin’s mainClass setting from: SmartCarRentalSystem to:
${exec.mainClass}

Save the file, then run the API command again. Keep this terminal open
while using the website.

The API listens on: http://localhost:8080

Optional settings: - PORT changes the API listening port. - CORS_ORIGIN
changes the allowed frontend origin (default local frontend:
http://localhost:3000).

API ROUTES

-   GET /api/health
-   GET /api/vehicles?startDate=YYYY-MM-DD&days=2
-   POST /api/bookings JSON: customer, vehicleId, startDate, days,
    optional promoCode
-   GET /api/bookings?customer=Name
-   POST /api/bookings/{id}/cancel JSON: customer
-   POST /api/bookings/{id}/return JSON: customer
-   GET /api/admin/bookings (X-Admin-Password header)
-   POST /api/admin/vehicles (X-Admin-Password header)
-   PATCH /api/admin/vehicles/{id}/price
-   DELETE /api/admin/vehicles/{id}
-   POST /api/admin/vehicles/{id}/maintenance
-   GET/POST /api/admin/promos
-   PATCH /api/admin/promos/{code}
-   GET /api/admin/reports

Test the API in a browser: http://localhost:8080/api/health
http://localhost:8080/api/vehicles

The health route should return a successful response. The vehicles route
should return vehicle data as JSON when the database is available.

RUN THE NEXT.JS FRONTEND

Open a second terminal at the repository root (the folder containing
package.json and package-lock.json).

Install the exact dependencies from the lockfile: npm ci

Create a repository-root file named .env.local with:
NEXT_PUBLIC_API_BASE_URL=http://localhost:8080/api

Start the frontend: npm run dev

Open: http://localhost:3000

Keep both terminals running: - Terminal 1: backend REST API on port
8080. - Terminal 2: Next.js frontend on port 3000.

The frontend can communicate with the Java backend only when the REST
API is running and the frontend API base URL is configured correctly.
Running the Java console menu does not connect the website to the
backend.

TESTING THE BACKEND

The integration test uses a real MySQL database and modifies test data.
It must only run against a separate database named
smart_car_rental_test. Never point the integration test at a database
containing real bookings.

Create the dedicated test database: mysql -u root -p -e “CREATE DATABASE
IF NOT EXISTS smart_car_rental_test;”

Windows PowerShell:
$env:DB_URL=“jdbc:mysql://localhost:3306/smart_car_rental_test?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC”
$env:DB_USER=“root” $env:DB_PASSWORD=“YOUR_MYSQL_PASSWORD”
$env:ADMIN_PASSWORD=“admin123” mvn clean verify

macOS/Linux: export
DB_URL=“jdbc:mysql://localhost:3306/smart_car_rental_test?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC”
export DB_USER=“root” export DB_PASSWORD=“YOUR_MYSQL_PASSWORD” export
ADMIN_PASSWORD=“admin123” mvn clean verify

A successful test run should report zero failures and zero errors,
followed by BUILD SUCCESS.

If the test reports that a new database should contain 3 default promos
but finds 4, inspect the test database seeding and test isolation before
changing the expected value. Do not run tests against the
production/application database to work around this failure.

FEATURES COVERED BY THE BACKEND

Customer: - Browse the vehicle fleet. - Find and book vehicles using
budget, seats, fuel, category, dates, and recommendations. - Apply
loyalty and promo discounts. - Cancel bookings. - Return vehicles and
calculate late fees. - View rental history. - Calculate rental
estimates.

Admin: - View fleet and bookings. - Add, remove, and reprice vehicles. -
Process vehicle returns. - Send vehicles to maintenance and complete
service. - View revenue reports. - Add and enable/disable promo codes.

The integration test is intended to exercise sample-data seeding,
vehicle browsing, estimates, recommendations and budget filtering,
booking creation, customer return, cancellation, admin return, rental
history, vehicle add/update/remove, maintenance, promo management,
reports, and persistence after reopening a database connection.

GITHUB ACTIONS

The repository includes backend and frontend workflow files under:
.github/workflows/

GitHub Actions can run the configured checks when matching changes are
pushed or pull requests are opened. Check the repository’s Actions tab
for the actual workflow result.

TROUBLESHOOTING

1.  PowerShell reports that ‘<’ is reserved: Use: Get-Content ..sql |
    mysql -u root -p

2.  Maven cannot delete backend: Stop any running Java process (Ctrl+C),
    close terminals that are using the build output, and retry. If
    necessary, close VS Code and retry. OneDrive synchronization can
    sometimes hold files open; a project folder outside OneDrive (for
    example C:) may help.

3.  The website reports missing React/Next.js modules: Run npm ci from
    the repository root, not from backend.

4.  The terminal shows the Smart Car Rental System menu instead of the
    API: You started the console entry point. Check the
    exec-maven-plugin mainClass configuration described in RUN THE JAVA
    REST API.

5.  The frontend cannot reach the API: Confirm the API is running at
    http://localhost:8080, check the repository-root .env.local value,
    restart npm run dev after changing .env.local, and check CORS_ORIGIN
    if using a different frontend origin.

SECURITY

-   Never commit passwords, API keys, or private .env files.
-   Use a private ADMIN_PASSWORD rather than the demo default.
-   Keep integration tests pointed at smart_car_rental_test only.
-   Do not expose the local database or API publicly without appropriate
    authentication, authorization, and deployment security.