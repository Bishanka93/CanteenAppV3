-- Canteen App MySQL Database Schema
-- Run this script in MySQL to create the database and tables

-- Create Database
CREATE DATABASE IF NOT EXISTS canteen_app;
USE canteen_app;

-- Users Table
CREATE TABLE IF NOT EXISTS users (
    id INT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(100) NOT NULL,
    roll_no VARCHAR(50) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL,
    is_staff BOOLEAN DEFAULT FALSE,
    is_admin BOOLEAN DEFAULT FALSE,
    canteen_id INT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (canteen_id) REFERENCES canteens(id)
);

-- Canteens Table
CREATE TABLE IF NOT EXISTS canteens (
    id INT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(100) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Food Items Table
CREATE TABLE IF NOT EXISTS food_items (
    id INT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(100) NOT NULL,
    price DECIMAL(10, 2) NOT NULL,
    canteen_id INT NOT NULL,
    image_name VARCHAR(100),
    is_available BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (canteen_id) REFERENCES canteens(id)
);

-- Order Items Table
CREATE TABLE IF NOT EXISTS orders (
    id INT PRIMARY KEY AUTO_INCREMENT,
    token INT UNIQUE NOT NULL,
    canteen_id INT NOT NULL,
    canteen_name VARCHAR(100) NOT NULL,
    status VARCHAR(50) DEFAULT 'PENDING',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (canteen_id) REFERENCES canteens(id)
);

-- Order Details Table (for items in each order)
CREATE TABLE IF NOT EXISTS order_items (
    id INT PRIMARY KEY AUTO_INCREMENT,
    order_id INT NOT NULL,
    food_item_id INT NOT NULL,
    quantity INT NOT NULL,
    price_at_order DECIMAL(10, 2) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (order_id) REFERENCES orders(id) ON DELETE CASCADE,
    FOREIGN KEY (food_item_id) REFERENCES food_items(id)
);

-- Token Management Table
CREATE TABLE IF NOT EXISTS available_tokens (
    id INT PRIMARY KEY AUTO_INCREMENT,
    token INT UNIQUE NOT NULL,
    is_used BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Insert Sample Data
INSERT INTO canteens (id, name) VALUES
(1, 'Canteen A'),
(2, 'Canteen B');

INSERT INTO food_items (id, name, price, canteen_id, image_name, is_available) VALUES
(1, 'Chowmein', 50.0, 1, 'chowmein', TRUE),
(2, 'Fried Rice', 40.0, 1, 'fried_rice', TRUE),
(3, 'Veg Sandwich', 20.0, 1, 'veg_sandwich', TRUE),
(4, 'Veg Thali', 70.0, 2, 'veg_thali', TRUE),
(5, 'Masala Dosa', 90.0, 2, 'masala_dosa', TRUE),
(6, 'Chicken Thali', 100.0, 2, 'chicken_thali', TRUE);

INSERT INTO users (name, roll_no, password, is_staff, is_admin, canteen_id) VALUES
('Walter White', 'DC2024BTE0093', '12345', FALSE, FALSE, NULL),
('Canteen Manager', 'STAFF_A', 'admin123', TRUE, FALSE, 1),
('Main Admin', 'ADMIN', 'admin123', FALSE, TRUE, NULL);

-- Create Indexes for better performance
CREATE INDEX idx_user_rollno ON users(roll_no);
CREATE INDEX idx_canteen_id ON food_items(canteen_id);
CREATE INDEX idx_order_token ON orders(token);
CREATE INDEX idx_order_status ON orders(status);
CREATE INDEX idx_order_items_order ON order_items(order_id);

