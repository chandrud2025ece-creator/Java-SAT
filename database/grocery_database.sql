-- ============================================================
-- ONLINE GROCERY ORDERING SYSTEM — DATABASE SCHEMA & SAMPLE DATA
-- Database Name: online_grocery_db
-- ============================================================

CREATE DATABASE IF NOT EXISTS online_grocery_db;
USE online_grocery_db;

-- ------------------------------------------------------------
-- Drop existing tables in reverse dependency order
-- ------------------------------------------------------------
DROP TABLE IF EXISTS payments;
DROP TABLE IF EXISTS order_items;
DROP TABLE IF EXISTS orders;
DROP TABLE IF EXISTS cart_items;
DROP TABLE IF EXISTS carts;
DROP TABLE IF EXISTS products;
DROP TABLE IF EXISTS categories;

-- ------------------------------------------------------------
-- 1. Table: categories
-- ------------------------------------------------------------
CREATE TABLE categories (
    category_id INT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(100) NOT NULL UNIQUE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ------------------------------------------------------------
-- 2. Table: products
-- ------------------------------------------------------------
CREATE TABLE products (
    product_id INT PRIMARY KEY AUTO_INCREMENT,
    category_id INT NOT NULL,
    name VARCHAR(150) NOT NULL,
    description TEXT,
    price DECIMAL(10, 2) NOT NULL CHECK (price > 0),
    stock INT NOT NULL DEFAULT 0 CHECK (stock >= 0),
    unit VARCHAR(30) NOT NULL DEFAULT 'packet',
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_product_category
        FOREIGN KEY (category_id)
        REFERENCES categories(category_id)
        ON UPDATE CASCADE
        ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX idx_products_category ON products(category_id);
CREATE INDEX idx_products_name ON products(name);
CREATE INDEX idx_products_active ON products(is_active);

-- ------------------------------------------------------------
-- 3. Table: carts (Default / Guest session carts)
-- ------------------------------------------------------------
CREATE TABLE carts (
    cart_id INT PRIMARY KEY AUTO_INCREMENT,
    session_token VARCHAR(64) NOT NULL UNIQUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ------------------------------------------------------------
-- 4. Table: cart_items
-- ------------------------------------------------------------
CREATE TABLE cart_items (
    cart_item_id INT PRIMARY KEY AUTO_INCREMENT,
    cart_id INT NOT NULL,
    product_id INT NOT NULL,
    quantity INT NOT NULL CHECK (quantity > 0),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT uk_cart_item_product UNIQUE (cart_id, product_id),
    CONSTRAINT fk_cart_item_cart
        FOREIGN KEY (cart_id)
        REFERENCES carts(cart_id)
        ON DELETE CASCADE,
    CONSTRAINT fk_cart_item_product
        FOREIGN KEY (product_id)
        REFERENCES products(product_id)
        ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX idx_cart_items_cart ON cart_items(cart_id);

-- ------------------------------------------------------------
-- 5. Table: orders
-- ------------------------------------------------------------
CREATE TABLE orders (
    order_id INT PRIMARY KEY AUTO_INCREMENT,
    customer_name VARCHAR(100) NOT NULL,
    customer_phone VARCHAR(20) NOT NULL,
    delivery_address VARCHAR(255) NOT NULL,
    delivery_instructions VARCHAR(255),
    total_amount DECIMAL(10, 2) NOT NULL CHECK (total_amount >= 0),
    status ENUM('PLACED', 'PAID', 'PROCESSING', 'SHIPPED', 'DELIVERED', 'CANCELLED') NOT NULL DEFAULT 'PLACED',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX idx_orders_status ON orders(status);
CREATE INDEX idx_orders_created ON orders(created_at);

-- ------------------------------------------------------------
-- 6. Table: order_items (Stores historical snapshot of ordered items)
-- ------------------------------------------------------------
CREATE TABLE order_items (
    order_item_id INT PRIMARY KEY AUTO_INCREMENT,
    order_id INT NOT NULL,
    product_id INT NOT NULL,
    product_name VARCHAR(150) NOT NULL,
    unit_price DECIMAL(10, 2) NOT NULL CHECK (unit_price > 0),
    quantity INT NOT NULL CHECK (quantity > 0),
    subtotal DECIMAL(10, 2) NOT NULL CHECK (subtotal >= 0),
    CONSTRAINT fk_order_item_order
        FOREIGN KEY (order_id)
        REFERENCES orders(order_id)
        ON DELETE CASCADE,
    CONSTRAINT fk_order_item_product
        FOREIGN KEY (product_id)
        REFERENCES products(product_id)
        ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX idx_order_items_order ON order_items(order_id);

-- ------------------------------------------------------------
-- 7. Table: payments
-- ------------------------------------------------------------
CREATE TABLE payments (
    payment_id INT PRIMARY KEY AUTO_INCREMENT,
    order_id INT NOT NULL UNIQUE,
    payment_method ENUM('CASH_ON_DELIVERY', 'UPI', 'CARD') NOT NULL,
    amount DECIMAL(10, 2) NOT NULL CHECK (amount >= 0),
    payment_status ENUM('PENDING', 'COMPLETED', 'FAILED', 'REFUNDED') NOT NULL DEFAULT 'PENDING',
    payment_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_payment_order
        FOREIGN KEY (order_id)
        REFERENCES orders(order_id)
        ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX idx_payments_order ON payments(order_id);
CREATE INDEX idx_payments_status ON payments(payment_status);

-- ------------------------------------------------------------
-- Sample Data Insertion
-- ------------------------------------------------------------

-- Categories
INSERT INTO categories (category_id, name) VALUES
(1, 'Fruits'),
(2, 'Vegetables'),
(3, 'Dairy'),
(4, 'Bakery'),
(5, 'Grains'),
(6, 'Beverages');

-- Products
INSERT INTO products (product_id, category_id, name, description, price, stock, unit, is_active) VALUES
(101, 5, 'Basmati Rice', 'Premium long grain aged basmati rice', 420.00, 50, 'bag (5kg)', TRUE),
(102, 5, 'Wheat Flour', '100% whole wheat stone-ground chakki fresh atta', 230.00, 60, 'bag (5kg)', TRUE),
(103, 3, 'Fresh Cow Milk', 'Pasteurized homogenized full cream cow milk', 65.00, 80, 'litre', TRUE),
(104, 3, 'Farm Fresh Eggs', 'Grade A fresh farm eggs rich in protein', 84.00, 100, 'packet (12 pcs)', TRUE),
(105, 4, 'Whole Wheat Bread', 'Soft and healthy whole grain brown bread', 45.00, 40, 'packet (400g)', TRUE),
(106, 1, 'Royal Gala Apples', 'Crisp, sweet and juicy red apples', 180.00, 75, 'kg', TRUE),
(107, 1, 'Fresh Bananas', 'Fresh naturally ripened Robusta bananas', 55.00, 90, 'kg', TRUE),
(108, 2, 'Organic Tomatoes', 'Farm fresh red ripe juicy tomatoes', 40.00, 120, 'kg', TRUE),
(109, 2, 'Fresh Onions', 'Selected medium-size fresh red onions', 35.00, 150, 'kg', TRUE),
(110, 2, 'Fresh Potatoes', 'Cleaned and sorted everyday cooking potatoes', 30.00, 140, 'kg', TRUE),
(111, 5, 'Pure Sunflower Oil', 'Refined healthy cooking sunflower oil', 145.00, 60, 'litre', TRUE),
(112, 5, 'Refined White Sugar', 'Sulphur-free clean refined crystalline sugar', 48.00, 100, 'kg', TRUE),
(113, 6, 'Assam Black Tea', 'Rich aroma CTC black tea leaves', 120.00, 50, 'packet (250g)', TRUE),
(114, 6, 'Filter Coffee Powder', 'Authentic roasted South Indian filter coffee blend', 160.00, 45, 'packet (200g)', TRUE);

-- Default Guest Shopping Cart
INSERT INTO carts (cart_id, session_token) VALUES
(1, 'DEFAULT_GUEST_CART')
ON DUPLICATE KEY UPDATE session_token = 'DEFAULT_GUEST_CART';

-- ============================================================
-- End of Script
-- ============================================================
