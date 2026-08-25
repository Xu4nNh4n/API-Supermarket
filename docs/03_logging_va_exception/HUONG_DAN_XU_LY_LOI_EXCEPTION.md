# 🚨 GIÁO TRÌNH XỬ LÝ NGOẠI LỆ TẬP TRUNG (GLOBAL EXCEPTION HANDLING)

> 🎯 **Mục tiêu**: Hiểu bản chất cơ chế gom bắt lỗi tập trung (`@RestControllerAdvice`) và cung cấp **Bộ Nhiệm Vụ Thực Hành (Project Quests)** để tự tay xây dựng hệ thống báo lỗi chuyên nghiệp, bảo mật, chuẩn REST API cho dự án mới!

---

## 📚 PHẦN 1: GIẢI PHÃ BẢN CHẤT LÝ THUYẾT (DEEP DIVE THEORY)

---

### 1. Dòng Chảy Xử Lý Lỗi Tập Trung

```text
[ Client (Frontend / Postman) ] ── (1. Gửi request mua 10 lon Coca) ──► [ Controller ]
                                                                             │
                                                                             ▼
[ Service Layer: Kiểm tra thấy kho chỉ còn 2 lon ]
        │
        ▼ (Ném lỗi: throw new InsufficientStockException("Kho chỉ còn 2 lon!"))
[ GlobalExceptionHandler (@RestControllerAdvice) ]
        │
        ├── 1. Bắt đúng class InsufficientStockException
        ├── 2. Ghi log cảnh báo: log.warn(...)
        ├── 3. Rút traceId từ MDC
        └── 4. Đóng gói vào đối tượng ApiErrorResponse
        │
        ▼
[ Trả về HTTP 400 Bad Request + JSON lỗi an toàn, đẹp mắt ] ──► [ Client ]
```

---

### 2. So Sánh Bắt Lỗi Thủ Công (`try-catch`) vs Xử Lý Tập Trung

| Tiêu chí | Dùng `try-catch` khắp nơi (Nghiệp dư) | Dùng `@RestControllerAdvice` (Chuẩn Enterprise) |
| :--- | :--- | :--- |
| **Độ sạch của Code** | ❌ Code bị rác bởi hàng trăm khối try-catch | ✅ Service chỉ tập trung tính toán, ném 1 dòng `throw new ...` là xong |
| **Định dạng JSON** | ❌ Mỗi controller trả về 1 kiểu cấu trúc lỗi khác nhau | ✅ 100% mọi API đều trả về chung 1 khuôn mẫu `ApiErrorResponse` |
| **Bảo mật** | ❌ Dễ làm lộ Stack trace đỏ lòm ra màn hình người dùng | ✅ Ẩn giấu chi tiết nhạy cảm của server, chỉ trả thông điệp an toàn |
| **Khả năng mở rộng** | ❌ Thêm 1 lỗi mới phải đi sửa hàng chục file | ✅ Chỉ cần khai báo 1 hàm `@ExceptionHandler` duy nhất trong Handler |

---

### 3. Phân Loại Các Mã Lỗi HTTP Phổ Biến

```text
  400 Bad Request           ──► Dữ liệu client gửi sai / Hết hàng trong kho (BadRequestException, InsufficientStockException)
  401 Unauthorized          ──► Chưa đăng nhập / Sai mật khẩu / Token hỏng (JwtAuthenticationEntryPoint)
  403 Forbidden             ──► Đã đăng nhập nhưng không đủ quyền Role (JwtAccessDeniedHandler)
  404 Not Found             ──► Không tìm thấy ID trong Database (ResourceNotFoundException)
  500 Internal Server Error ──► Lỗi sập server / NullPointerException (Bắt bởi Exception.class)
```

---

## 🎮 PHẦN 2: BỘ NHIỆM VỤ THỰC HÀNH DỰ ÁN MỚI (PROJECT QUESTS)

---

### ⚔️ QUEST 1: Xây Dựng `ApiErrorResponse.java`
* **Mục tiêu**: Định nghĩa khuôn mẫu JSON chuẩn trả về cho Frontend.
* **Nhiệm vụ cần làm**:
  ```java
  public class ApiErrorResponse {
      private LocalDateTime timestamp;
      private int status;
      private String error;
      private String message;
      private String traceId;
      private String path;
      private Map<String, String> validationErrors; // Chứa chi tiết lỗi từng ô nhập liệu
  }
  ```

---

### ⚔️ QUEST 2: Tạo Các Custom RuntimeException
* **Mục tiêu**: Tự chế các loại ngoại lệ nghiệp vụ riêng cho dự án.
* **Nhiệm vụ cần làm**:
  1. Tạo `ResourceNotFoundException extends RuntimeException` (Dùng khi tìm không thấy theo ID).
  2. Tạo `BadRequestException extends RuntimeException` (Dùng khi dữ liệu bị trùng hoặc vi phạm logic).
  3. Tạo `InsufficientStockException extends RuntimeException` (Dùng riêng cho nghiệp vụ kho hàng).

---

### ⚔️ QUEST 3: Xây Dựng `GlobalExceptionHandler`
* **Mục tiêu**: Gom toàn bộ lỗi hệ thống về 1 mối xử lý.
* **Nhiệm vụ cần làm**:
  1. Đánh dấu `@RestControllerAdvice` và `@Slf4j` lên đầu class.
  2. Viết hàm `@ExceptionHandler(ResourceNotFoundException.class)` $\rightarrow$ Trả về `HttpStatus.NOT_FOUND` (404).
  3. Viết hàm `@ExceptionHandler(InsufficientStockException.class)` $\rightarrow$ Trả về `HttpStatus.BAD_REQUEST` (400).
  4. Viết hàm `@ExceptionHandler(Exception.class)` $\rightarrow$ Bắt tất cả các lỗi bất ngờ còn lại, ghi `log.error` và trả về `HttpStatus.INTERNAL_SERVER_ERROR` (500).

---

### ⚔️ QUEST 4: Xử Lý Lỗi Validate Form (`@Valid`)
* **Mục tiêu**: Bắt lỗi khi người dùng bỏ trống hoặc nhập sai định dạng `@NotBlank`, `@Min`, `@Email`.
* **Nhiệm vụ cần làm**:
  1. Bắt ngoại lệ `@ExceptionHandler(MethodArgumentNotValidException.class)`.
  2. Duyệt qua danh sách `ex.getBindingResult().getFieldErrors()`.
  3. Gom lại thành Map: `{"productName": "Không được để trống", "price": "Phải lớn hơn 0"}`.
  4. Đính kèm Map này vào trường `validationErrors` của `ApiErrorResponse`.

---

## 🏆 BOSS QUEST: THỬ THÁCH KIỂM THỬ BẮT LỖI

* 🧪 **Test 1 (Lỗi 404)**: Gọi `GET /api/products/999999` (ID không tồn tại) $\rightarrow$ *Kỳ vọng: Trả về 404 Not Found kèm message "Không tìm thấy sản phẩm".*
* 🧪 **Test 2 (Lỗi Validation)**: Gọi `POST /api/products` với body `{ "price": -5000 }` $\rightarrow$ *Kỳ vọng: Trả về 400 Bad Request kèm chi tiết lỗi của trường `price`.*
* 🧪 **Test 3 (Lỗi 500)**: Thử nghiệm cố tình gọi một hàm gây `NullPointerException` $\rightarrow$ *Kỳ vọng: Server ghi log ERROR đỏ trên terminal, nhưng chỉ trả về JSON 500 "Lỗi máy chủ nội bộ" an toàn cho Client.*
