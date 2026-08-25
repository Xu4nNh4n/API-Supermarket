# 💾 GIÁO TRÌNH TRANSACTION & JPA AUDITING TRONG SPRING BOOT

> 🎯 **Mục tiêu**: Làm chủ cơ chế giao dịch CSDL (Tính toàn vẹn dữ liệu ACID với `@Transactional`) và tự động hóa ghi nhận thời gian tạo/sửa (`JPA Auditing`) kèm **Bộ Nhiệm Vụ Thực Hành (Project Quests)** cho dự án mới!

---

## 📚 PHẦN 1: GIẢI PHÃ BẢN CHẤT LÝ THUYẾT (DEEP DIVE THEORY)

---

### 1. Bản Chất Của Transaction (Giao Dịch ACID)

Một Transaction là một chuỗi các thao tác CSDL được gộp thành **1 khối duy nhất (Tất cả hoặc Không có gì - All or Nothing)**:

```text
[ Bắt đầu hàm Service có @Transactional ]
                   │
                   ▼ (Spring Proxy mở 1 Giao dịch DB)
        Thực hiện câu SQL 1: UPDATE products SET stock = stock - 2
                   │
        Thực hiện câu SQL 2: INSERT INTO sales_orders ...
                   │
        Thực hiện câu SQL 3: INSERT INTO sales_order_items ...
                   │
      ┌────────────┴───────────────────────────┐
      ▼                                        ▼
[ Chạy xong 100% không lỗi ]        [ Có RuntimeException giữa chừng ]
      │                                        │
      ▼                                        ▼
   COMMIT                                  ROLLBACK
(Lưu vĩnh viễn vào DB)             (Hoàn tác 100% về nguyên trạng)
```

---

### 2. Khi Nào Cần Dùng `@Transactional`?

| Loại hàm trong Service | Có cần `@Transactional`? | Lý do |
| :--- | :---: | :--- |
| **Chỉ đọc (Read-only)**: `getById`, `getAll`, `filter` | ❌ **KHÔNG** | Không thay đổi dữ liệu, không có nguy cơ xung đột. |
| **Ghi 1 bảng đơn giản**: `updateUserPhone` | ⚠️ Có thể không bắt buộc | Vì bản thân câu lệnh SQL đơn lẻ đã là 1 transaction ngầm. |
| **Ghi nhiều bảng phức tạp**: `createSalesOrder`, `payOrder`, `cancelSalesOrder`, `refresh` | ✅ **BẮT BUỘC** | Cần đảm bảo tính đồng nhất giữa kho, tiền và hóa đơn! |

> ⚠️ **Quy tắc quan trọng**: Mặc định Spring chỉ tự động Rollback đối với **`RuntimeException` (Unchecked Exception)**. Nếu bạn bắt lỗi bằng `catch` mà nuốt lỗi (không ném ra tiếp), Spring sẽ tưởng hàm chạy thành công và vẫn COMMIT dữ liệu!

---

### 3. JPA Auditing: Tự Động Ghi Ngày Tạo & Ngày Cập Nhật

Thay vì phải gõ thủ công `entity.setCreatedAt(LocalDateTime.now())` ở khắp mọi Service:
* **JPA Auditing** cho phép Hibernate tự động điền `createdAt` khi INSERT và `updatedAt` khi UPDATE.

```text
Class BaseEntity:
  ├── @CreatedDate   private LocalDateTime createdAt;  (Tự điền khi INSERT)
  └── @LastModifiedDate private LocalDateTime updatedAt;  (Tự cập nhật khi UPDATE)
```

---

## 🎮 PHẦN 2: BỘ NHIỆM VỤ THỰC HÀNH DỰ ÁN MỚI (PROJECT QUESTS)

---

### ⚔️ QUEST 1: Kích Hoạt JPA Auditing Toàn Hệ Thống
* **Mục tiêu**: Cấu hình Spring tự động theo dõi thời gian của Entity.
* **Nhiệm vụ cần làm**:
  1. Thêm annotation `@EnableJpaAuditing` vào file `SupermarketApplication.java` (hoặc file cấu hình `JpaConfig.java`).
  2. Tạo class `BaseEntity` có `@MappedSuperclass` và `@EntityListeners(AuditingEntityListener.class)`.
  3. Cho các Entity như `Product`, `Customer`, `SalesOrder` kế thừa (`extends BaseEntity`).

---

### ⚔️ QUEST 2: Áp Dụng `@Transactional` Đúng Chuẩn
* **Mục tiêu**: Đảm bảo an toàn giao dịch ở tầng Service.
* **Nhiệm vụ cần làm**:
  1. Luôn import chuẩn: `import org.springframework.transaction.annotation.Transactional;`.
  2. Đặt `@Transactional` lên đầu method `createSalesOrder(request)`.
  3. Bên trong hàm:
     * Bước 1: Duyệt danh sách món hàng $\rightarrow$ Trừ tồn kho trong `Product`.
     * Bước 2: Lưu `SalesOrder`.
     * Bước 3: Lưu danh sách `SalesOrderItem`.

---

### ⚔️ QUEST 3: Xử Lý Rollback Khi Có Lỗi Nghiệp Vụ
* **Mục tiêu**: Kiểm soát hành vi Rollback khi ném Custom Exception.
* **Nhiệm vụ cần làm**:
  1. Đảm bảo mọi Custom Exception (`InsufficientStockException`, `BadRequestException`) đều `extends RuntimeException`.
  2. Khi phát hiện sản phẩm hết hàng hoặc giá không hợp lệ $\rightarrow$ Ném Exception $\rightarrow$ Spring tự động Rollback toàn bộ các câu lệnh đã chạy trước đó.

---

## 🏆 BOSS QUEST: THỬ THÁCH MÔ PHỎNG SỰ CỐ ĐỨT GÃY DỮ LIỆU

* 🧪 **Thử nghiệm**:
  1. Trong hàm `createSalesOrder`, sau khi đã chạy lệnh trừ tồn kho sản phẩm thứ nhất thành công $\rightarrow$ Cố tình chèn một dòng: `throw new RuntimeException("Mất kết nối server đột ngột!");`.
  2. Bật Postman gọi API tạo đơn.
  3. Mở MySQL kiểm tra số lượng tồn kho của sản phẩm xem có bị trừ mất không?
  4. 👉 *Kỳ vọng: Nhờ có `@Transactional`, số lượng tồn kho trong Database vẫn giữ nguyên vẹn 100%!*
