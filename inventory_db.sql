-- ============================================================
-- inventory_db.sql
-- Run this in phpMyAdmin or MySQL CLI to set up the database.
-- ============================================================

CREATE DATABASE IF NOT EXISTS inventory_db
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE inventory_db;

-- ── users ────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS users (
    user_id    INT          NOT NULL AUTO_INCREMENT,
    username   VARCHAR(50)  NOT NULL UNIQUE,
    password   VARCHAR(255) NOT NULL,          -- SHA-256 hex hash
    full_name  VARCHAR(100) NOT NULL,
    created_at TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (user_id)
);

-- ── categories ───────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS categories (
    category_id   INT          NOT NULL AUTO_INCREMENT,
    category_name VARCHAR(100) NOT NULL UNIQUE,
    description   VARCHAR(255),
    PRIMARY KEY (category_id)
);

-- ── products ─────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS products (
    product_id         INT            NOT NULL AUTO_INCREMENT,
    product_name       VARCHAR(100)   NOT NULL,
    category_id        INT            NOT NULL,
    unit               VARCHAR(30)    NOT NULL,
    unit_price         DECIMAL(10,2)  NOT NULL,
    stock_quantity     INT            NOT NULL DEFAULT 0,
    low_stock_threshold INT           NOT NULL DEFAULT 5,
    created_at         TIMESTAMP      DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (product_id),
    FOREIGN KEY (category_id) REFERENCES categories(category_id)
);

-- ── Seed data ─────────────────────────────────────────────────
-- Default admin account.
-- Username: admin   Password: admin123
-- SHA-256 of "admin123" = 240be518fabd2724ddb6f04eeb1da5967448d7e831c08c8fa822809f74c720a
INSERT IGNORE INTO users (username, password, full_name)
VALUES ('admin',
        '240be518fabd2724ddb6f04eeb1da5967448d7e831c08c8fa822809f74c720a',
        'Store Administrator');

-- Sample categories
INSERT IGNORE INTO categories (category_name, description) VALUES
    ('Beverages',        'Drinks and liquids'),
    ('Snacks',           'Chips, candies, and biscuits'),
    ('Cleaning Supplies','Soap, detergent, and sanitizers'),
    ('School Supplies',  'Notebooks, pens, and other school items');

-- Sample products
INSERT IGNORE INTO products (product_name, category_id, unit, unit_price, stock_quantity, low_stock_threshold)
VALUES
    ('Mineral Water 500ml', 1, 'bottles', 15.00, 120, 20),
    ('Softdrink 1.5L',      1, 'bottles', 55.00, 30,  10),
    ('Chips (Large)',        2, 'pcs',     35.00, 50,  15),
    ('Biscuits Assorted',   2, 'packs',   25.00, 4,   10),
    ('Dishwashing Liquid',  3, 'bottles', 45.00, 0,   5),
    ('Ballpen Blue',        4, 'pcs',     8.00,  200, 30),
    ('Intermediate Pad',    4, 'pcs',     22.00, 8,   15);
