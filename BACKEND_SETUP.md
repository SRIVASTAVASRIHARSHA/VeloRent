# Add the Java backend to VeloRent

This package contains the patched Java/JDBC/MySQL console backend and an automated MySQL integration test.

## Add to the VeloRent repository

Copy the `backend/` folder and `.github/workflows/backend-ci.yml` into the root of the existing `VeloRent` repository. Keep the existing Next.js files in place.

## Run on macOS

Install the required tools if they are not already present:

```bash
brew install openjdk@21 maven mysql
brew services start mysql
```

Create the project database:

```bash
cd backend
mysql -u root < database/schema.sql
```

Set your local credentials and start the backend:

```bash
export DB_URL="jdbc:mysql://localhost:3306/smart_car_rental?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC"
export DB_USER="root"
export DB_PASSWORD="YOUR_MYSQL_PASSWORD"
export ADMIN_PASSWORD="YOUR_PRIVATE_ADMIN_PASSWORD"
mvn clean compile
mvn exec:java
```

The admin password falls back to `admin123` for the student demo unless `ADMIN_PASSWORD` is set. Set your own value before using it.

## Run the integration test safely

The integration test will only run when `DB_URL` points to a separate database named `smart_car_rental_test`.

```bash
mysql -u root -e "CREATE DATABASE IF NOT EXISTS smart_car_rental_test;"
export DB_URL="jdbc:mysql://localhost:3306/smart_car_rental_test?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC"
export DB_USER="root"
export DB_PASSWORD="YOUR_MYSQL_PASSWORD"
export ADMIN_PASSWORD="admin123"
mvn clean verify
```

Do not point integration tests at a database containing real bookings; they create and modify test data.

## What is covered

The integration test exercises sample-data seeding, vehicle browsing, estimates, recommendations and budget filtering, booking creation, customer return, booking cancellation, admin return, rental history, vehicle add/update/remove, maintenance start/completion, promo add/disable, reports, and persistence after reopening the database connection.

The GitHub Actions workflow runs the backend integration test against an isolated MySQL 8.4 service on pushes or pull requests that modify `backend/`.

## Current integration boundary

The Java console backend and the Next.js website remain separate applications. This does not turn the console program into an HTTP API. A REST API (for example, Spring Boot) is still required before the website's demo booking UI can read or write live MySQL data.
