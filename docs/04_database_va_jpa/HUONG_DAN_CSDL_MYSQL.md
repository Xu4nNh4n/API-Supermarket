# 💾 GIÁO TRÌNH THIẾT KẾ CƠ SỞ DỮ LIỆU MYSQL & ERD TRONG SPRING BOOT

> 🎯 **Mục tiêu**: Làm chủ tư duy thiết kế CSDL quan hệ (RDBMS MySQL chuẩn 3NF), hiểu bản chất Khóa ngoại (Foreign Key), Đánh Index tăng tốc truy vấn, lựa chọn đúng kiểu dữ liệu (Tiền tệ dùng `DECIMAL`) và cung cấp **Bộ Nhiệm Vụ Thực Hành (Project Quests)** cho dự án mới!

---

## 📚 PHẦN 1: GIẢI PHÃ BẢN CHẤT LÝ THUYẾT (DEEP DIVE THEORY)

---

### 1. Sơ Đồ Quan Hệ Bảng (ERD Diagram) Của Hệ Thống

```text
  [ roles ] (1) ◄────────── (N) [ users ] (1) ◄────────── (N) [ refresh_tokens ]
                                   │
                                   │ (1)
                                   ▼ (N)
  [ categories ] (1) ◄──┐       [ sales_orders ] (1) ◄─── (N) [ sales_order_items ]
                        │          ▲                             │
  [ suppliers ]  (1) ◄──┼── (N)    │ (N)                         │ (N)
                        │          │                             ▼
                     [ products ] ─┴─────────────────────────────┘ (1)
                        ▲
                        │ (N)
  [ customers ]  (1) ────┘ (Khách hàng mua nhiều đơn)
```

---

### 2. Các Quy Tắc Thiết Kế CSDL Bất Di Bất Dịch Trong Doanh Nghiệp

#### 1. Tiền Tệ Tuyệt Đối Dùng `DECIMAL(15, 2)`, Cấm Dùng `FLOAT` / `DOUBLE`
* ❌ `FLOAT` / `DOUBLE`: Lưu số thực dạng dấu phẩy động theo chuẩn IEEE 754 $\rightarrow$ **Gặp lỗi sai số làm tròn** (Ví dụ: `0.1 + 0.2 = 0.30000000000000004`). Sau 10.000 giao dịch sẽ bị lệch hàng triệu đồng!
* ✅ `DECIMAL(15, 2)`: Lưu số chính xác tuyệt đối từng con số lẻ $\rightarrow$ Chuẩn mực bắt buộc cho tài chính kế toán (Tương đương `BigDecimal` trong Java).

#### 2. Khóa Chính (Primary Key) Luôn Dùng `BIGINT AUTO_INCREMENT` (hoặc UUID)
* Không dùng `INT` (tối đa chỉ 2 tỷ bản ghi). Các hệ thống bán lẻ lớn sau vài năm sẽ bị tràn số (Integer Overflow). Dùng `BIGINT` (8 bytes) đảm bảo lưu trữ hàng nghìn tỷ bản ghi an toàn.

#### 3. Khi Nào Cần Đánh Index (Chỉ Mục Tìm Kiếm)?
* Đánh Index giống như **Mục lục cuốn sách**: Giúp MySQL nhảy thẳng đến trang cần đọc mà không phải lật từng trang (Full Table Scan).
* **Nên đánh Index ở đâu?**:
  * Các cột thường xuyên xuất hiện trong mệnh đề `WHERE` (Ví dụ: `username`, `phone`, `order_code`).
  * Các cột khóa ngoại thường dùng để `JOIN` (Ví dụ: `customer_id`, `category_id`).
* ⚠️ **Lưu ý**: Không đánh Index vô tội vạ cho mọi cột vì mỗi lần `INSERT/UPDATE`, MySQL phải mất công cập nhật lại cây Index $\rightarrow$ làm chậm thao tác ghi.

---

## 🎮 PHẦN 2: BỘ NHIỆM VỤ THỰC HÀNH DỰ ÁN MỚI (DATABASE QUESTS)

---

### ⚔️ QUEST 1: Viết Câu Lệnh DDL Tạo Bảng Chuẩn Khóa Ngoại
* **Mục tiêu**: Tạo bảng có rằng buộc toàn vẹn dữ liệu.
* **Nhiệm vụ cần làm**:
  ```sql
  CREATE TABLE products (
      product_id BIGINT AUTO_INCREMENT PRIMARY KEY,
      product_name VARCHAR(150) NOT NULL,
      sku VARCHAR(50) NOT NULL UNIQUE,
      price DECIMAL(15, 2) NOT NULL,
      stock_quantity INT NOT NULL DEFAULT 0,
      category_id BIGINT NOT NULL,
      is_active BOOLEAN NOT NULL DEFAULT TRUE,
      created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
      CONSTRAINT fk_products_category FOREIGN KEY (category_id) REFERENCES categories(category_id)
  );
  ```

---

### ⚔️ QUEST 2: Đánh Index Cho Các Cột Tìm Kiếm Tần Suất Cao
* **Mục tiêu**: Tăng tốc độ truy vấn từ 2 giây xuống còn 5 mili-giây.
* **Nhiệm vụ cần làm**:
  ```sql
  -- Đánh Index cho mã hóa đơn và ngày tạo
  CREATE INDEX idx_sales_orders_code ON sales_orders(order_code);
  CREATE INDEX idx_sales_orders_customer_created ON sales_orders(customer_id, created_at DESC);
  ```

---

### ⚔️ QUEST 3: Viết Câu Lệnh Báo Cáo Doanh Thu (Aggregation SQL)
* **Nhiệm vụ cần làm**: Viết câu lệnh tính tổng doanh thu theo từng tháng:
  ```sql
  SELECT 
      DATE_FORMAT(paid_at, '%Y-%m') AS month,
      COUNT(order_id) AS total_orders,
      SUM(total_amount) AS total_revenue
  FROM sales_orders
  WHERE status = 'PAID'
  GROUP BY DATE_FORMAT(paid_at, '%Y-%m')
  ORDER BY month DESC;
  ```

---

## 🏆 BOSS QUEST: THỬ THÁCH ĐO HIỆU NĂNG VỚI `EXPLAIN`

* 🧪 **Thử nghiệm**:
  1. Chèn 100.000 bản ghi mẫu vào bảng `sales_orders`.
  2. Chạy lệnh: `EXPLAIN SELECT * FROM sales_orders WHERE order_code = 'HD-99999';`
  3. Quan sát cột `type` và `rows`:
     * Khi chưa có Index: `type = ALL` (Quét toàn bộ 100.000 dòng).
     * Sau khi tạo Index: `type = ref` / `const` và `rows = 1` (Tìm kiếm tức thì trong 1 mili-giây)!
