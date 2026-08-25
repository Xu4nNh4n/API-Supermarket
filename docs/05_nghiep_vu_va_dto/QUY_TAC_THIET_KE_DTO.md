# 📦 GIÁO TRÌNH THIẾT KẾ DTO CHUYÊN NGHIỆP: REQUEST vs RESPONSE (TỪ BẢN CHẤT ĐẾN THỰC THI)

> 🎯 **Mục tiêu**: Làm chủ tư duy phân loại và thiết kế Data Transfer Object (DTO) chuẩn Enterprise, nắm vững kỹ thuật kiểm tra dữ liệu đầu vào (Validation) và cung cấp **Bộ Nhiệm Vụ Thực Hành (Project Quests)** để tự tay thiết kế hệ thống DTO an toàn cho bất kỳ dự án mới nào!

---

## 📚 PHẦN 1: GIẢI PHÃ BẢN CHẤT LÝ THUYẾT (DEEP DIVE THEORY)

---

### 1. Dòng Chảy Dữ Liệu Của DTO Trong Hệ Thống

DTO (Data Transfer Object) là các "hộp chứa dữ liệu" chỉ dùng để vận chuyển thông tin qua lại giữa Client và Server mà **hoàn toàn không chứa logic nghiệp vụ hay kết nối Database**:

```text
[ Client (Postman / Web / Mobile) ]
                │
                │ 1. Gửi JSON (Chỉ các trường cần thiết & an toàn)
                ▼
[ REQUEST DTO ] ──► (Ví dụ: SalesOrderRequest, ProductRequest)
                │
                ├── 2. Đi qua bộ lọc Validation (@Valid: @NotBlank, @Min, @Email)
                │
                ▼
[ Controller & Service ]
                │
                ├── 3. Service kiểm tra nghiệp vụ, truy vấn CSDL
                │
                ▼
[ ENTITY ] ──► (Ví dụ: SalesOrder, Product trong MySQL)
                │
                ├── 4. Service bọc dữ liệu vào Response DTO an toàn
                │
                ▼
[ RESPONSE DTO ] ──► (Ví dụ: SalesOrderResponse, ProductResponse)
                │
                │ 5. Trả JSON chuẩn, đẹp mắt, giấu các trường nhạy cảm
                ▼
[ Client nhận kết quả ]
```

---

### 2. Bộ 3 Câu Hỏi Vàng Để Phân Loại Trường Dữ Liệu

Khi nhìn vào một Entity có 15-20 cột, làm sao biết trường nào nên cho vào **Request**, trường nào cho vào **Response**? Hãy trả lời lần lượt 3 câu hỏi sau:

```text
┌────────────────────────────────────────────────────────────────────────────────────────┐
│ CÂU HỎI 1: "Ai là người sinh ra dữ liệu này?"                                          │
│   ├── Do Con người / Client chủ động nhập/chọn ──► Cho vào REQUEST DTO                 │
│   │   (Ví dụ: Tên sản phẩm, số lượng mua, địa chỉ giao hàng, ghi chú)                  │
│   └── Do Hệ thống / Database tự động sinh ra   ──► Cho vào RESPONSE DTO                │
│       (Ví dụ: orderId tự tăng, createdAt ngày tạo, orderCode mã tự sinh)               │
├────────────────────────────────────────────────────────────────────────────────────────┤
│ CÂU HỎI 2: "Dữ liệu này có phải do máy tính tính toán không?"                         │
│   ├── ĐÚNG (Tiền tạm tính, tiền giảm giá, tổng tiền, điểm tích lũy)                     │
│   └── 👉 TUYỆT ĐỐI KHÔNG để Client gửi lên trong REQUEST DTO (Chống gian lận sửa giá)! │
│       Backend tự lấy giá niêm yết trong DB, tự nhân số lượng, rồi trả ra RESPONSE DTO  │
├────────────────────────────────────────────────────────────────────────────────────────┤
│ CÂU HỎI 3: "Dữ liệu này có sẵn trong Token đăng nhập không?"                           │
│   ├── Thu ngân / Nhân viên nào đang tạo đơn này?                                       │
│   └── 👉 Backend tự bóc `username`/`userId` từ JWT Token của người đang đăng nhập.     │
│       Client KHÔNG cần gửi `userId` lên trong Request (Tránh nhân viên A ghi nhầm B)   │
└────────────────────────────────────────────────────────────────────────────────────────┘
```

---

### 3. Thảm Họa Thực Tế Nếu Không Tách DTO (Dùng Thẳng Entity)

| Lỗ hổng khi dùng thẳng Entity | Kịch bản thảm họa trong đời thực | Cách DTO giải quyết |
| :--- | :--- | :--- |
| **Gian lận sửa giá (Price Tampering)** | Client gửi JSON mua máy giặt: `{"price": 1000}` $\rightarrow$ Máy giặt 10 triệu bị mua với giá 1.000 đồng! | `ProductRequest` chỉ cho gửi `productId` + `quantity`. Giá tiền do Server tự đọc từ MySQL. |
| **Tự phong Quản trị viên (Over-posting)** | User đăng ký tài khoản gửi kèm: `{"role": "ROLE_ADMIN"}` $\rightarrow$ User thường biến thành Giám đốc hệ thống! | `UserRegisterRequest` chỉ có `username` + `password`. Cột `role` do Backend tự gán `ROLE_USER`. |
| **Lộ mật khẩu (Information Leakage)** | API trả về danh sách nhân viên chứa nguyên đối tượng `User` $\rightarrow$ Lộ toàn bộ `password_hash`! | `UserResponse` chỉ chọn lọc: `userId`, `fullName`, `email`, `roleName`. |
| **Sập server do lặp vô tận (Infinite JSON)** | `Order` trỏ tới `OrderItems`, `OrderItems` lại trỏ ngược về `Order` $\rightarrow$ Jackson JSON serialize lặp vô tận tới khi tràn RAM sập server. | `SalesOrderResponse` chỉ chứa danh sách `List<OrderItemResponse>` độc lập, không có quan hệ vòng lặp. |

---

### 4. Bảng Soi Chi Tiết Entity `SalesOrder`

| Thuộc tính | Bản chất dữ liệu | Cho vào Request? | Cho vào Response? | Giải thích lý do |
| :--- | :--- | :---: | :---: | :--- |
| `orderId` | Khóa chính DB tự tăng | ❌ Không | ✅ **CÓ** | Lúc chưa tạo đơn làm gì đã có ID. |
| `orderCode` | Mã đơn tự sinh (`HD20260819-001`) | ❌ Không | ✅ **CÓ** | Backend tự sinh theo quy tắc công ty. |
| `customerId` | ID khách hàng thân thiết | ✅ **CÓ** | ✅ **CÓ** | Thu ngân nhập SĐT tìm khách (hoặc `null` nếu khách lạ). |
| `customerName` | Tên đầy đủ của khách | ❌ Không | ✅ **CÓ** | Trả ra để in lên hóa đơn giấy. |
| `staffName` | Tên nhân viên thu ngân | ❌ Không | ✅ **CÓ** | Backend tự rút từ Token đăng nhập. |
| `orderType` | Mua tại quầy hay Online | ✅ **CÓ** | ✅ **CÓ** | Thu ngân bấm chọn (`IN_STORE` / `ONLINE`). |
| `paymentMethod` | Tiền mặt hay Chuyển khoản | ✅ **CÓ** | ✅ **CÓ** | Thu ngân bấm chọn (`CASH` / `BANKING`). |
| `status` | Trạng thái hóa đơn | ❌ Không | ✅ **CÓ** | Ban đầu Backend tự gán `PENDING`. |
| `subtotal` | Tổng tiền hàng tạm tính | ❌ Không | ✅ **CÓ** | Backend tự tính: $\sum (\text{số lượng} \times \text{giá DB})$. |
| `discountAmount` | Tiền giảm giá | ❌ Không | ✅ **CÓ** | Backend tự tính từ voucher/khuyến mãi. |
| `totalAmount` | Tổng tiền thực trả | ❌ Không | ✅ **CÓ** | Backend tự tính: `subtotal - discountAmount`. |
| `note` | Ghi chú đơn hàng | ✅ **CÓ** | ✅ **CÓ** | Thu ngân gõ thêm nếu khách dặn riêng. |
| `items` | Danh sách món hàng mua | ✅ **CÓ** (`OrderItemRequest`) | ✅ **CÓ** (`OrderItemResponse`) | Cần `productId` và `quantity` để trừ kho. |
| `createdAt` | Thời gian tạo đơn | ❌ Không | ✅ **CÓ** | Database tự động ghi nhận thời gian thực. |
| `paidAt` | Thời gian hoàn tất thanh toán | ❌ Không | ✅ **CÓ** | Cập nhật khi thu tiền thành công. |

---

## 🎮 PHẦN 2: BỘ NHIỆM VỤ THỰC HÀNH DỰ ÁN MỚI (DTO QUESTS)

---

### ⚔️ QUEST 1: Áp Dụng Annotation Validation Cho Request DTO
* **Mục tiêu**: Ngăn chặn rác và dữ liệu sai ngay từ cửa ngõ Controller.
* **Nhiệm vụ cần làm**:
  ```java
  public class ProductRequest {
      @NotBlank(message = "Tên sản phẩm không được để trống")
      @Size(min = 3, max = 100, message = "Tên sản phẩm phải từ 3 đến 100 ký tự")
      private String productName;

      @NotNull(message = "Giá sản phẩm bắt buộc phải có")
      @DecimalMin(value = "0.0", inclusive = false, message = "Giá sản phẩm phải lớn hơn 0")
      private BigDecimal price;

      @NotNull(message = "Số lượng tồn kho bắt buộc phải có")
      @Min(value = 0, message = "Tồn kho không được âm")
      private Integer stockQuantity;

      @NotNull(message = "Danh mục sản phẩm bắt buộc phải chọn")
      private Long categoryId;
  }
  ```

---

### ⚔️ QUEST 2: Thiết Kế Bộ DTOs Cho Module Mới (Ví dụ: Voucher Khuyến Mãi)
* **Nhiệm vụ cần làm**:
  1. Tạo `VoucherRequest`: Chứa `voucherCode`, `discountPercent` (Validation `@Min(1) @Max(100)`), `expiryDate` (Validation `@Future`).
  2. Tạo `VoucherResponse`: Chứa `voucherId`, `voucherCode`, `discountPercent`, `expiryDate`, `isActive`, `createdAt`.
  3. Đối chiếu: `voucherId` và `createdAt` tuyệt đối không xuất hiện trong `VoucherRequest`!

---

### ⚔️ QUEST 3: Xây Dựng Hàm `mapToResponse` Chuẩn Null-Safety
* **Mục tiêu**: Chuyển Entity sang Response DTO an toàn, không bị crash.
* **Nhiệm vụ cần làm**:
  ```java
  private ProductResponse mapToResponse(Product product) {
      return new ProductResponse(
          product.getProductId(),
          product.getProductName(),
          product.getPrice(),
          product.getStockQuantity(),
          // Kiểm tra null cho quan hệ @ManyToOne Category
          product.getCategory() != null ? product.getCategory().getCategoryId() : null,
          product.getCategory() != null ? product.getCategory().getCategoryName() : "Chưa phân loại",
          product.getCreatedAt()
      );
  }
  ```

---

## 🏆 BOSS QUEST: THỬ THÁCH BẢO VỆ DỮ LIỆU ĐẦU VÀO

* 🧪 **Test 1 (Hack giá)**: Dùng Postman gửi `POST /api/orders` cố tình chèn thêm field `"totalAmount": 1000` $\rightarrow$ *Kỳ vọng: Server chỉ đọc productId và quantity để tự tính tổng tiền thật trong DB, hoàn toàn bỏ qua trường giả mạo của Client.*
* 🧪 **Test 2 (Test Validation)**: Gửi Request tạo sản phẩm với `price = -5000` $\rightarrow$ *Kỳ vọng: Controller chặn lại ngay lập tức và bắn lỗi 400 Bad Request kèm thông báo "Giá sản phẩm phải lớn hơn 0".*
