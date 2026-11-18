CREATE DATABASE IF NOT EXISTS smart_car_rental;
USE smart_car_rental;

CREATE TABLE IF NOT EXISTS vehicles (
    id INT PRIMARY KEY,
    category VARCHAR(30) NOT NULL,
    brand VARCHAR(60) NOT NULL,
    model VARCHAR(80) NOT NULL,
    fuel VARCHAR(30) NOT NULL,
    seats INT NOT NULL,
    price_per_day DOUBLE NOT NULL,
    rating DOUBLE NOT NULL,
    status VARCHAR(20) NOT NULL,
    rentals_since_service INT NOT NULL,
    total_rentals INT NOT NULL
);

CREATE TABLE IF NOT EXISTS bookings (
    id INT PRIMARY KEY,
    customer VARCHAR(120) NOT NULL,
    vehicle_id INT NOT NULL,
    category VARCHAR(30) NOT NULL,
    vehicle_name VARCHAR(150) NOT NULL,
    start_day DATE NOT NULL,
    days INT NOT NULL,
    rent DOUBLE NOT NULL,
    tax DOUBLE NOT NULL,
    deposit DOUBLE NOT NULL,
    fine DOUBLE NOT NULL,
    cancel_fee DOUBLE NOT NULL,
    status VARCHAR(20) NOT NULL,
    return_day DATE NOT NULL,
    discount DOUBLE NOT NULL,
    discount_note VARCHAR(255)
);

CREATE TABLE IF NOT EXISTS promos (
    code VARCHAR(40) PRIMARY KEY,
    percent DOUBLE NOT NULL,
    max_off DOUBLE NOT NULL,
    min_days INT NOT NULL,
    active BOOLEAN NOT NULL
);
