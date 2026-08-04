USE supermarket_api;
CREATE TABLE roles (
    role_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    role_name VARCHAR(50) NOT NULL UNIQUE,
    description VARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- =========================
-- 2. PERMISSIONS - QUYỀN
-- =========================
CREATE TABLE permissions (
    permission_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    permission_name VARCHAR(100) NOT NULL UNIQUE,
    description VARCHAR(255)
);

-- =========================
-- 3. ROLE_PERMISSIONS - PHÂN QUYỀN THEO ROLE
-- =========================
CREATE TABLE role_permissions (
    role_id BIGINT NOT NULL,
    permission_id BIGINT NOT NULL,

    PRIMARY KEY (role_id, permission_id),

    CONSTRAINT fk_role_permissions_roles
        FOREIGN KEY (role_id) REFERENCES roles(role_id),

    CONSTRAINT fk_role_permissions_permissions
        FOREIGN KEY (permission_id) REFERENCES permissions(permission_id)
);

-- =========================
-- 4. USERS - TÀI KHOẢN NHÂN VIÊN
-- =========================
CREATE TABLE users (
    user_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    full_name VARCHAR(150) NOT NULL,
    username VARCHAR(100) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    email VARCHAR(100) UNIQUE,
    phone VARCHAR(20),
    is_active BOOLEAN DEFAULT TRUE,

    role_id BIGINT NOT NULL,

    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT fk_users_roles
        FOREIGN KEY (role_id) REFERENCES roles(role_id)
);

-- =========================
-- 5. CATEGORIES - DANH MỤC
-- =========================
CREATE TABLE categories (
    category_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    description VARCHAR(255),
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- =========================
-- 6. SUPPLIERS - NHÀ CUNG CẤP
-- =========================
CREATE TABLE suppliers (
    supplier_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(150) NOT NULL,
    phone VARCHAR(20),
    email VARCHAR(100),
    address VARCHAR(255),
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- =========================
-- 7. PRODUCTS - SẢN PHẨM
-- =========================
CREATE TABLE products (
    product_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(150) NOT NULL,
    sku_code VARCHAR(50) NOT NULL UNIQUE,
    price DECIMAL(18,2) NOT NULL,
    stock_quantity INT NOT NULL DEFAULT 0,
    reorder_point INT DEFAULT 10,
    expiry_date DATE,
    image_url TEXT,
    is_active BOOLEAN DEFAULT TRUE,

    category_id BIGINT NOT NULL,
    supplier_id BIGINT,

    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT fk_products_categories
        FOREIGN KEY (category_id) REFERENCES categories(category_id),

    CONSTRAINT fk_products_suppliers
        FOREIGN KEY (supplier_id) REFERENCES suppliers(supplier_id),

    CONSTRAINT chk_products_price
        CHECK (price >= 0),

    CONSTRAINT chk_products_stock
        CHECK (stock_quantity >= 0)
);

-- =========================
-- 8. CUSTOMERS - KHÁCH HÀNG
-- =========================
CREATE TABLE customers (
    customer_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    full_name VARCHAR(150) NOT NULL,
    phone VARCHAR(20),
    email VARCHAR(100),
    address VARCHAR(255),
    points INT DEFAULT 0,
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- =========================
-- 9. SALES_ORDERS - HÓA ĐƠN BÁN HÀNG
-- =========================
CREATE TABLE sales_orders (
    order_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_code VARCHAR(50) NOT NULL UNIQUE,
    order_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    customer_id BIGINT,
    user_id BIGINT NOT NULL,

    subtotal DECIMAL(18,2) DEFAULT 0,
    discount_amount DECIMAL(18,2) DEFAULT 0,
    total_amount DECIMAL(18,2) DEFAULT 0,

    payment_method VARCHAR(50),
    status VARCHAR(50) DEFAULT 'PAID',

    note VARCHAR(255),

    CONSTRAINT fk_sales_orders_customers
        FOREIGN KEY (customer_id) REFERENCES customers(customer_id),

    CONSTRAINT fk_sales_orders_users
        FOREIGN KEY (user_id) REFERENCES users(user_id)
);

-- =========================
-- 10. SALES_ORDER_ITEMS - CHI TIẾT HÓA ĐƠN
-- =========================
CREATE TABLE sales_order_items (
    order_item_id BIGINT AUTO_INCREMENT PRIMARY KEY,

    order_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,

    quantity INT NOT NULL,
    unit_price DECIMAL(18,2) NOT NULL,
    subtotal DECIMAL(18,2) NOT NULL,

    CONSTRAINT fk_sales_order_items_orders
        FOREIGN KEY (order_id) REFERENCES sales_orders(order_id),

    CONSTRAINT fk_sales_order_items_products
        FOREIGN KEY (product_id) REFERENCES products(product_id),

    CONSTRAINT chk_order_item_quantity
        CHECK (quantity > 0)
);

-- =========================
-- 11. INVENTORY_RECEIPTS - PHIẾU NHẬP KHO
-- =========================
CREATE TABLE inventory_receipts (
    receipt_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    receipt_code VARCHAR(50) NOT NULL UNIQUE,
    receipt_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    supplier_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,

    total_amount DECIMAL(18,2) DEFAULT 0,
    status VARCHAR(50) DEFAULT 'COMPLETED',
    note VARCHAR(255),

    CONSTRAINT fk_inventory_receipts_suppliers
        FOREIGN KEY (supplier_id) REFERENCES suppliers(supplier_id),

    CONSTRAINT fk_inventory_receipts_users
        FOREIGN KEY (user_id) REFERENCES users(user_id)
);

-- =========================
-- 12. INVENTORY_RECEIPT_ITEMS - CHI TIẾT NHẬP KHO
-- =========================
CREATE TABLE inventory_receipt_items (
    receipt_item_id BIGINT AUTO_INCREMENT PRIMARY KEY,

    receipt_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,

    quantity INT NOT NULL,
    import_price DECIMAL(18,2) NOT NULL,
    expiry_date DATE,
    subtotal DECIMAL(18,2) NOT NULL,

    CONSTRAINT fk_inventory_receipt_items_receipts
        FOREIGN KEY (receipt_id) REFERENCES inventory_receipts(receipt_id),

    CONSTRAINT fk_inventory_receipt_items_products
        FOREIGN KEY (product_id) REFERENCES products(product_id),

    CONSTRAINT chk_receipt_item_quantity
        CHECK (quantity > 0)
);

-- =========================
-- 13. REFRESH_TOKENS - DÙNG SAU NÀY CHO JWT
-- =========================
CREATE TABLE refresh_tokens (
    token_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    token VARCHAR(500) NOT NULL,
    expiry_date TIMESTAMP NOT NULL,
    revoked BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_refresh_tokens_users
        FOREIGN KEY (user_id) REFERENCES users(user_id)
);

-- =========================
-- INDEX TỐI ƯU TÌM KIẾM
-- =========================
CREATE INDEX idx_products_name ON products(name);
CREATE INDEX idx_products_category_id ON products(category_id);
CREATE INDEX idx_products_supplier_id ON products(supplier_id);
CREATE INDEX idx_orders_customer_id ON sales_orders(customer_id);
CREATE INDEX idx_orders_user_id ON sales_orders(user_id);


INSERT INTO roles (role_id, role_name, description) VALUES
(1, 'ADMIN', 'Quản trị hệ thống'),
(2, 'MANAGER', 'Quản lý cửa hàng'),
(3, 'CASHIER', 'Nhân viên thu ngân'),
(4, 'WAREHOUSE', 'Nhân viên kho');
INSERT INTO permissions (permission_id, permission_name, description) VALUES
(1, 'USER_MANAGE', 'Quản lý tài khoản'),
(2, 'PRODUCT_READ', 'Xem sản phẩm'),
(3, 'PRODUCT_CREATE', 'Thêm sản phẩm'),
(4, 'PRODUCT_UPDATE', 'Sửa sản phẩm'),
(5, 'PRODUCT_DELETE', 'Xóa sản phẩm'),
(6, 'ORDER_READ', 'Xem hóa đơn'),
(7, 'ORDER_CREATE', 'Tạo hóa đơn'),
(8, 'INVENTORY_READ', 'Xem nhập kho'),
(9, 'INVENTORY_CREATE', 'Tạo phiếu nhập kho'),
(10, 'REPORT_VIEW', 'Xem báo cáo');

-- ADMIN có tất cả quyền
INSERT INTO role_permissions (role_id, permission_id)
SELECT 1, permission_id FROM permissions;

-- MANAGER
INSERT INTO role_permissions (role_id, permission_id) VALUES
(2, 2), (2, 3), (2, 4), (2, 5),
(2, 6), (2, 7),
(2, 8), (2, 9),
(2, 10);

-- CASHIER
INSERT INTO role_permissions (role_id, permission_id) VALUES
(3, 2), (3, 6), (3, 7);

-- WAREHOUSE
INSERT INTO role_permissions (role_id, permission_id) VALUES
(4, 2), (4, 4), (4, 8), (4, 9);

-- =========================
-- USERS
-- Mật khẩu mẫu cho tất cả user: 123456
-- Đây là BCrypt hash để sau này test Spring Security.
-- =========================
INSERT INTO users 
(full_name, username, password_hash, email, phone, role_id)
VALUES
('Admin System', 'admin', '$2y$10$1HtchTXzRJWu4e4XfHl17uqrR7oihPbtPnMvLEGruc5HXxaE43pIu', 'admin@example.com', '0900000001', 1),
('Quản lý cửa hàng', 'manager01', '$2y$10$1HtchTXzRJWu4e4XfHl17uqrR7oihPbtPnMvLEGruc5HXxaE43pIu', 'manager@example.com', '0900000002', 2),
('Nhân viên thu ngân', 'cashier01', '$2y$10$1HtchTXzRJWu4e4XfHl17uqrR7oihPbtPnMvLEGruc5HXxaE43pIu', 'cashier@example.com', '0900000003', 3),
('Nhân viên kho', 'warehouse01', '$2y$10$1HtchTXzRJWu4e4XfHl17uqrR7oihPbtPnMvLEGruc5HXxaE43pIu', 'warehouse@example.com', '0900000004', 4);

-- =========================
-- CATEGORIES
-- =========================
INSERT INTO categories (name, description) VALUES
('Nước giải khát', 'Các loại nước uống đóng chai, lon, hộp'),
('Bánh kẹo', 'Các loại bánh, kẹo, snack'),
('Mì gói', 'Các loại mì ăn liền'),
('Sữa', 'Sữa hộp, sữa chai, sữa đặc'),
('Gia vị', 'Nước mắm, dầu ăn, đường, muối'),
('Đồ gia dụng', 'Các sản phẩm gia dụng nhỏ');

-- =========================
-- SUPPLIERS
-- =========================
INSERT INTO suppliers (name, phone, email, address) VALUES
('Công ty Coca Cola Việt Nam', '0901111222', 'coca@example.com', 'TP.HCM'),
('Công ty PepsiCo Việt Nam', '0902222333', 'pepsi@example.com', 'Bình Dương'),
('Công ty Acecook Việt Nam', '0903333444', 'acecook@example.com', 'TP.HCM'),
('Công ty Vinamilk', '0904444555', 'vinamilk@example.com', 'TP.HCM'),
('Nhà cung cấp Gia Vị Việt', '0905555666', 'giavi@example.com', 'Long An');

-- =========================
-- PRODUCTS
-- =========================
INSERT INTO products
(name, sku_code, price, stock_quantity, reorder_point, expiry_date, category_id, supplier_id)
VALUES
('Coca Cola lon 330ml', 'COCA330', 10000, 100, 20, '2026-12-31', 1, 1),
('Pepsi lon 330ml', 'PEPSI330', 10000, 90, 20, '2026-12-31', 1, 2),
('Sting đỏ chai 330ml', 'STING330', 12000, 80, 20, '2026-10-10', 1, 2),
('Oreo socola', 'OREO001', 15000, 60, 15, '2026-08-15', 2, NULL),
('Snack khoai tây', 'SNACK001', 12000, 70, 15, '2026-09-20', 2, NULL),
('Mì Hảo Hảo tôm chua cay', 'HAOHAO001', 5000, 200, 50, '2026-07-30', 3, 3),
('Mì Đệ Nhất', 'DENHAT001', 6000, 150, 50, '2026-07-30', 3, 3),
('Sữa tươi Vinamilk 180ml', 'VINAMILK180', 8000, 120, 30, '2026-05-20', 4, 4),
('Sữa đặc Ông Thọ', 'ONGTHO001', 25000, 50, 10, '2026-11-11', 4, 4),
('Nước mắm Nam Ngư', 'NAMNGU001', 35000, 40, 10, '2027-01-01', 5, 5),
('Dầu ăn Tường An 1L', 'TUONGAN1L', 45000, 35, 10, '2027-02-01', 5, 5),
('Đường trắng 1kg', 'DUONG1KG', 22000, 25, 10, '2027-03-01', 5, 5);

-- =========================
-- CUSTOMERS
-- =========================
INSERT INTO customers (full_name, phone, email, address, points) VALUES
('Nguyễn Văn An', '0911111111', 'an@example.com', 'Quận 1, TP.HCM', 10),
('Trần Thị Bình', '0922222222', 'binh@example.com', 'Quận 3, TP.HCM', 20),
('Lê Minh Cường', '0933333333', 'cuong@example.com', 'Thủ Đức, TP.HCM', 5),
('Phạm Thu Hà', '0944444444', 'ha@example.com', 'Bình Thạnh, TP.HCM', 15);

-- =========================
-- SALES ORDERS
-- =========================
INSERT INTO sales_orders
(order_code, customer_id, user_id, subtotal, discount_amount, total_amount, payment_method, status, note)
VALUES
('HD0001', 1, 3, 30000, 0, 30000, 'CASH', 'PAID', 'Khách mua tại quầy'),
('HD0002', 2, 3, 25000, 0, 25000, 'BANKING', 'PAID', 'Thanh toán chuyển khoản'),
('HD0003', 3, 3, 56000, 5000, 51000, 'CASH', 'PAID', 'Có giảm giá');

-- =========================
-- SALES ORDER ITEMS
-- =========================
INSERT INTO sales_order_items
(order_id, product_id, quantity, unit_price, subtotal)
VALUES
(1, 1, 2, 10000, 20000),
(1, 6, 2, 5000, 10000),
(2, 4, 1, 15000, 15000),
(2, 2, 1, 10000, 10000),
(3, 8, 2, 8000, 16000),
(3, 10, 1, 35000, 35000),
(3, 6, 1, 5000, 5000);

-- =========================
-- INVENTORY RECEIPTS
-- =========================
INSERT INTO inventory_receipts
(receipt_code, supplier_id, user_id, total_amount, status, note)
VALUES
('NK0001', 1, 4, 500000, 'COMPLETED', 'Nhập nước giải khát'),
('NK0002', 3, 4, 750000, 'COMPLETED', 'Nhập mì gói'),
('NK0003', 4, 4, 400000, 'COMPLETED', 'Nhập sữa');

-- =========================
-- INVENTORY RECEIPT ITEMS
-- =========================
INSERT INTO inventory_receipt_items
(receipt_id, product_id, quantity, import_price, expiry_date, subtotal)
VALUES
(1, 1, 50, 8000, '2026-12-31', 400000),
(1, 3, 10, 10000, '2026-10-10', 100000),
(2, 6, 100, 4000, '2026-07-30', 400000),
(2, 7, 70, 5000, '2026-07-30', 350000),
(3, 8, 50, 7000, '2026-05-20', 350000),
(3, 9, 2, 25000, '2026-11-11', 50000);