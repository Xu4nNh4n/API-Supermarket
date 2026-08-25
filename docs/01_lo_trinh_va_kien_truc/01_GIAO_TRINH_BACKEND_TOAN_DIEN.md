# 📘 BẢN ĐỒ TƯ DUY & BÍ KÍP TOÀN DIỆN DỰ ÁN SUPERMARKET API

> 🎯 **Mục tiêu giáo trình**: Giúp bạn hiểu sâu bản chất tất cả những gì **bạn đã trực tiếp xây dựng** trong dự án này: từ Security (JWT, Refresh Token Rotation), Logging (MDC TraceID), Exception Handling, JPA/Hibernate, Transaction, đến Thiết kế DTO và Phân trang an toàn.

---

## 🗺️ CHƯƠNG 1: TẠI SAO PHẢI CHIA NHIỀU TẦNG? (LAYERED ARCHITECTURE)

### 1. Thảm họa nếu viết tất cả vào 1 Controller (Fat Controller):
Tưởng tượng bạn viết toàn bộ: Đọc request, kiểm tra mật khẩu, viết câu lệnh SQL, gửi mail vào thẳng trong 1 hàm `login()` ở Controller:
* ❌ **Khó bảo trì**: File Controller dài 3.000 dòng, không ai dám vào sửa vì sợ hỏng.
* ❌ **Không thể tái sử dụng**: Chức năng "Trừ kho sản phẩm" vừa dùng ở quầy thu ngân, vừa dùng khi khách mua online, vừa dùng khi kiểm kho. Nếu viết trong Controller thì phải copy-paste code đi 3 nơi!
* ❌ **Không thể viết Unit Test**: Muốn test thử logic tính tiền thì bắt buộc phải chạy cả server web lên.

### 2. Mô hình phân tầng chuẩn mực trong dự án:

```text
[ Client (Frontend / Postman / Mobile) ]
                 │
                 │ 1. Gửi HTTP Request kèm Token JWT
                 ▼
[ MdcLoggingFilter (Gắn traceId) & SecurityFilter (Soi Token) ]
                 │
                 │ 2. Đã xác thực & nạp User vào SecurityContext
                 ▼
[ Controller Layer (Tiếp tân: Nhận & Validate @Valid dữ liệu) ]
                 │
                 │ 3. Truyền Request DTO
                 ▼
[ Service Layer (Bếp trưởng: Xử lý logic, tính tiền, trừ kho) ]
                 │
                 │ 4. Gọi truy vấn dữ liệu qua Interface
                 ▼
[ Repository Layer (Thủ kho: Nhận lệnh thao tác CSDL) ]
                 │
                 │ 5. Sinh & thực thi câu lệnh SQL / JPQL
                 ▼
[ MySQL Database (Kho lưu trữ dữ liệu an toàn) ]
```

| Tầng | Đóng vai trò gì? | Câu hỏi tự vấn khi viết code |
| :--- | :--- | :--- |
| **Filter** | Bảo vệ & Giám sát | *"Request này có traceId chưa? Token gửi lên có hợp lệ không?"* |
| **Controller** | Tiếp tân kiểm tra | *"Dữ liệu khách gửi lên có thiếu trường nào không?"* (Dùng `@Valid`, `@NotBlank`). |
| **Service** | Bếp trưởng xử lý | *"Nghiệp vụ siêu thị tính thế nào? Kho còn đủ hàng không? Có được giảm giá không?"* |
| **Repository** | Thủ kho lấy hàng | *"Lấy dữ liệu này từ bảng nào? Cần điều kiện `WHERE` gì?"* |

---

## 🧩 CHƯƠNG 2: OOP THỰC CHIẾN TRONG DỰ ÁN

| Khái niệm OOP | Bản chất đời thường | Nằm ở đâu trong code dự án? |
| :--- | :--- | :--- |
| **Encapsulation (Đóng gói)** | Tủ có khóa, chỉ mở bằng chìa | `private BigDecimal price;` $\rightarrow$ chỉ sửa qua `setPrice()` có kiểm tra giá không được âm. |
| **Abstraction (Trừu tượng)** | Nhìn Menu gọi món, không cần biết chảo/bếp | Controller gọi `salesOrderService.createSalesOrder()`. Controller chỉ nhìn Interface mà không cần quan tâm bên trong viết JPA hay JDBC. |
| **Polymorphism (Đa hình)** | Ổ cắm điện (cắm quạt, cắm sạc laptop đều chạy) | Đổi từ thanh toán tiền mặt sang VNPay/Momo bằng cách tạo class mới cùng implement `PaymentService`. |
| **Inheritance (Kế thừa)** | Con thừa hưởng nhà và xe của Cha | `JwtAuthenticationFilter extends OncePerRequestFilter`: Kế thừa hành vi "chạy đúng 1 lần mỗi request" từ Spring. |

---

## 🏛️ CHƯƠNG 3: 5 NGUYÊN TẮC SOLID TRONG SIÊU THỊ

```text
S - Single Responsibility : Mỗi class chỉ làm 1 việc duy nhất (AuthController chỉ lo đăng nhập, ProductController chỉ lo sản phẩm).
O - Open / Closed         : Mở rộng tính năng mới, không sửa code cũ (Dùng PageResponse<T> generic cho mọi bảng).
L - Liskov Substitution   : Con không được phản bội hợp đồng của Cha (ServiceImpl không được trả null khi Interface cam kết ném Exception).
I - Interface Segregation : Tách nhỏ Interface theo từng nghiệp vụ (ProductService, SalesOrderService riêng biệt, không gộp 1 cục to tướng).
D - Dependency Inversion  : Luôn phụ thuộc vào Interface qua Constructor Injection (Spring tự inject Bean, cấm dùng new ServiceImpl()).
```

---

## 🔄 CHƯƠNG 4: BỘ BA "REQUEST DTO - ENTITY - RESPONSE DTO"

```text
[ Client / Frontend ]
        │
        │ 1. Gửi JSON (chỉ các trường cần thiết)
        ▼
[ Request DTO (Ví dụ: OrderItemRequest) ] ── (Chỉ có productId, quantity)
        │
        │ 2. Kiểm tra tồn kho, trừ kho & tính tiền
        ▼
[ Entity trong DB (SalesOrder, Product) ] ── (Chứa khóa ngoại, mật khẩu băm, quan hệ)
        │
        │ 3. Đóng gói dữ liệu an toàn, đẹp mắt
        ▼
[ Response DTO (SalesOrderResponse) ] ── (Chứa đầy đủ tên món, giá, tổng tiền, ngày tạo)
        │
        │ 4. Trả JSON chuẩn về Client
        ▼
[ Client / Frontend nhận kết quả an toàn ]
```

### ❓ Tại sao cấm trả Entity ra ngoài Controller?
1. **Lỗ hổng bảo mật (Over-posting)**: Nếu dùng `User` Entity làm Request, kẻ xấu gửi `{"role": "ADMIN"}` để tự phong làm Quản trị viên.
2. **Lỗi lặp vô tận (Infinite Recursion)**: `SalesOrder` chứa `items`, `SalesOrderItem` lại chứa ngược `order` $\rightarrow$ Jackson JSON serialize lặp vô tận làm sập RAM server.
3. **Giấu trường nhạy cảm**: Entity `User` có trường `passwordHash`. Response DTO `UserResponse` sẽ giấu nhẹm trường này.

---

## 🛡️ CHƯƠNG 5: BẢO MẬT TOÀN DIỆN (ACCESS TOKEN & REFRESH TOKEN ROTATION)

Trong dự án, bạn đã triển khai mô hình bảo mật chuẩn quốc tế gồm **2 loại Token**:

```text
[ ĐĂNG NHẬP THÀNH CÔNG ]
         │
         ├──► 1. Access Token (JWT): Hạn ngắn (15 phút) ──► Gửi kèm mỗi request để gọi API.
         │
         └──► 2. Refresh Token (UUID): Hạn dài (7 ngày) ──► Băm SHA-256 lưu trong DB, dùng để xin cấp lại Access Token mới.
```

```text
Quy trình Refresh Token Rotation (Đổi vé mới - Hủy vé cũ):
Client gửi Refresh Token cũ
  │
  ├──► 1. Server băm SHA-256 token gửi lên và tìm trong DB
  ├──► 2. Nếu tìm thấy & còn hạn:
  │         ├── Thu hồi (revoke) Refresh Token cũ ngay lập tức (isRevoked = true).
  │         ├── Tạo 1 Refresh Token MỚI + 1 Access Token MỚI.
  │         └── Trả về cho Client.
  └──► 3. Nếu Refresh Token bị lộ/dùng lại: Hệ thống phát hiện ngay và từ chối cấp quyền!
```

### 🚨 Phân biệt 2 mã lỗi bảo mật:
* **401 Unauthorized (`JwtAuthenticationEntryPoint`)**: *"Bạn là ai? Tôi không biết (chưa gửi token, token sai hoặc token hết hạn)"*.
* **403 Forbidden (`JwtAccessDeniedHandler`)**: *"Tôi biết bạn là Thu ngân, nhưng API này chỉ dành riêng cho ADMIN"*.

---

## 📝 CHƯƠNG 6: LOGGING CHUYÊN NGHIỆP VỚI MDC & TRACE-ID

### 💥 Bài toán thực tế:
Khi có **10.000 người** cùng lúc bấm mua hàng trên website: Terminal server sẽ in ra hàng vạn dòng log đan xen nhau hỗn loạn như "nồi lẩu thập cẩm". Khi có 1 đơn hàng bị lỗi, làm sao biết dòng log nào thuộc về đơn hàng nào?

### 💡 Giải pháp bạn đã cài đặt: `MdcLoggingFilter` & `traceId`
1. Mỗi khi có request bay vào: `MdcLoggingFilter` sinh ra một chuỗi ngẫu nhiên duy nhất, ví dụ `traceId = abc-123-xyz`.
2. Gắn `traceId` vào **ThreadLocal của MDC (Mapped Diagnostic Context)**.
3. Mọi dòng log trong Service, Repo, Controller đều tự động có tiền tố `[traceId=abc-123-xyz]`.
4. Trả `traceId` về trong JSON lỗi cho Frontend. Lập trình viên chỉ cần copy mã này và tìm kiếm trên server là ra đúng 100% lịch sử của đúng request đó!

```text
Mức độ Log cần nhớ:
- log.info(...)  : Ghi lại hành động quan trọng thành công (Tạo đơn hàng #123 thành công).
- log.warn(...)  : Cảnh báo bất thường nhưng chưa làm sập hệ thống (Kho sắp hết hàng, đăng nhập sai lần 1).
- log.error(...) : Lỗi nghiêm trọng cần vào sửa ngay (Lỗi kết nối DB, lỗi ném ngoại lệ hệ thống).
```

---

## 💾 CHƯƠNG 7: GIAO DỊCH DATABASE (@TRANSACTIONAL) & ACID

### 💥 Thảm họa nếu không có `@Transactional`:
Khách mua 1 chiếc Tivi 15 triệu:
* Bước 1: Trừ tồn kho Tivi từ 1 về 0 trong Database $\rightarrow$ **Thành công**.
* Bước 2: Tạo bản ghi hóa đơn và trừ tiền $\rightarrow$ **Server mất điện / Sập nguồn!**
* 😱 **Hậu quả**: Kho mất 1 tivi nhưng hóa đơn không có, tiền không thu được!

### 🛡️ Cơ chế cứu cánh của `@Transactional`:
Spring biến toàn bộ các thao tác trong hàm thành **1 khối duy nhất (All or Nothing)**:
* **Commit**: Tất cả các bước đều chạy xong mượt mà $\rightarrow$ Lưu vĩnh viễn vào DB.
* **Rollback**: Chỉ cần 1 dòng code bị lỗi (hết hàng, sai giá, đứt mạng) $\rightarrow$ Spring lập tức **hoàn tác 100%** mọi thao tác trước đó về nguyên trạng ban đầu!

> [!IMPORTANT]
> * **Hàm chỉ ĐỌC (`getById`, `filter`, `getAll`)**: ❌ **KHÔNG** cần `@Transactional`.
> * **Hàm GHI/SỬA/XÓA (`createSalesOrder`, `payOrder`, `cancelOrder`)**: ✅ **BẮT BUỘC** có `@Transactional`.

---

## 🗄️ CHƯƠNG 8: JPA ENTITY & QUAN HỆ BẢNG (BÍ KÍP VIẾT MAPPER KHÔNG LỖI)

### 1. Tại sao từ `order` lại gọi được `order.getCustomer()`?
Vì trong file `SalesOrder.java` có khai báo quan hệ:
```java
@ManyToOne                         // "Nhiều đơn hàng thuộc về 1 khách hàng"
@JoinColumn(name = "customer_id")  // Cột khóa ngoại trong bảng sales_orders
private Customer customer;         // Nhờ có dòng này → bạn mới gọi được order.getCustomer()
```

### 2. Bảng tra cứu từ Entity nào gọi được gì:

| Từ Entity | Gọi được | Trả về kiểu | Lý do |
| :--- | :--- | :--- | :--- |
| `order.getCustomer()` | ✅ | `Customer` | `SalesOrder` có trường `@ManyToOne Customer customer` |
| `order.getUser()` | ✅ | `User` | `SalesOrder` có trường `@ManyToOne User user` |
| `order.getItems()` | ❌ | — | `SalesOrder` **không khai báo** `@OneToMany List<SalesOrderItem>` |
| `item.getProduct()` | ✅ | `Product` | `SalesOrderItem` có trường `@ManyToOne Product product` |
| `product.getCategory()` | ✅ | `Category` | `Product` có trường `@ManyToOne Category category` |

### 3. Phân biệt biến số ít vs số nhiều (Lỗi kinh điển):

```text
╔══════════════════════════════╦══════════════════════════════════════════╗
║ Biến / Kiểu dữ liệu          ║ Có gọi được .getOrderId() không?         ║
╠══════════════════════════════╬══════════════════════════════════════════╣
║ SalesOrder  order  (1 cái)   ║ ✅ CÓ — gọi thẳng order.getOrderId()    ║
║ List<SalesOrder>  orders     ║ ❌ KHÔNG — List chỉ có .size(), .get(0) ║
╚══════════════════════════════╩══════════════════════════════════════════╝
```

### 4. Khi nào dùng `.stream().map()`, khi nào gọi thẳng?

```text
┌──────────────────────────────────────────────────────────────┐
│ Trả về 1 đối tượng  (SalesOrderResponse)?                    │
│   → Gọi thẳng: return mapToResponse(order, items, ...);      │
│                                                              │
│ Trả về danh sách (List<SalesOrderResponse>)?                 │
│   → Bắt buộc: return orders.stream()                         │
│                 .map(order -> mapToResponse(...))            │
│                 .toList();                                   │
└──────────────────────────────────────────────────────────────┘
```

### 5. Quy tắc phòng thủ Null-Safety:

```java
// ❌ Nguy hiểm: Nếu khách vãng lai (customer = null) → Sập server ngay!
order.getCustomer().getFullName()

// ✅ An toàn: Kiểm tra null bằng toán tử 3 ngôi
order.getCustomer() != null ? order.getCustomer().getFullName() : "Khách vãng lai"
```

---

## 🚨 CHƯƠNG 9: XỬ LÝ NGOẠI LỆ TẬP TRUNG (GLOBAL EXCEPTION HANDLER)

Thay vì viết hàng chục khối `try-catch` lặp đi lặp lại ở mọi nơi:
1. Khi phát hiện sai sót nghiệp vụ trong Service, chỉ cần **ném (throw)** lỗi ra:
   ```java
   if (product.getStockQuantity() < request.getQuantity()) {
       throw new InsufficientStockException("Kho chỉ còn " + product.getStockQuantity() + " sản phẩm!");
   }
   ```
2. Class **`GlobalExceptionHandler`** (`@RestControllerAdvice`) đứng ở tầng cao nhất sẽ tự động bắt lấy ngoại lệ và chuyển đổi thành cấu trúc JSON chuẩn:

```json
{
  "timestamp": "2026-08-21T04:30:00Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Kho chỉ còn 2 sản phẩm!",
  "traceId": "c8f2a1b9-4d2e-4f1a-9e3b-5c8a1b2c3d4e",
  "path": "/api/orders"
}
```

---

## 📄 CHƯƠNG 10: PHÂN TRANG & SẮP XẾP AN TOÀN (PAGINATION & SORT WHITELIST)

### 💥 Lỗ hổng tiềm ẩn:
Nếu người dùng gọi API phân trang: `GET /api/products?sortBy=passwordHash` hoặc chèn các câu lệnh lạ vào tham số `sortBy` $\rightarrow$ Server có thể bị lộ thông tin nội bộ hoặc văng lỗi 500.

### 🛡️ Giải pháp bạn đã cài đặt: `PaginationValidator` & `ALLOWED_SORT_FIELDS`
* Trong Service, luôn khai báo 1 danh sách trắng (Whitelist):
  ```java
  private static final Set<String> ALLOWED_SORT_FIELDS = Set.of(
      "productId", "productName", "price", "stockQuantity", "createdAt"
  );
  ```
* Bất kỳ yêu cầu sắp xếp nào không nằm trong Whitelist này đều bị chặn lại ngay từ đầu bằng `BadRequestException` $\rightarrow$ Đảm bảo an toàn 100%!

---

## 🧠 BỘ CÂU HỎI TỰ KIỂM TRA TƯ DUY (SELF-CHECK CHALLENGE)

Dành ra 3 phút tự trả lời các câu hỏi sau để tự tin 100% khi đi phỏng vấn hoặc giải thích code:

1. **Hỏi**: Tại sao hệ thống cần cả **Access Token** (sống ngắn 15 phút) và **Refresh Token** (sống dài 7 ngày)?
   * *Gợi ý: Nếu chỉ dùng 1 token sống 7 ngày mà bị hacker đánh cắp thì chuyện gì xảy ra?*
2. **Hỏi**: Trong dự án, `traceId` được tạo ở đâu và giúp ích gì cho lập trình viên khi đọc log?
   * *Gợi ý: Xem lại `MdcLoggingFilter` và cách tìm log khi có 10.000 người cùng gọi API.*
3. **Hỏi**: Tại sao trong `SalesOrderServiceImpl`, hàm `createSalesOrder` bắt buộc có `@Transactional` còn hàm `getSalesOrderById` lại không cần?
   * *Gợi ý: Hàm nào có nguy cơ làm dữ liệu bị mất mát / không đồng nhất giữa kho và hóa đơn?*
4. **Hỏi**: Bạn duyệt qua danh sách và viết `orders.getOrderId()` bị lỗi compile. Vì sao?
   * *Gợi ý: `orders` là một danh sách (List), chỉ có từng phần tử đơn lẻ `order` bên trong mới có `.getOrderId()`.*
5. **Hỏi**: Mục đích của `ALLOWED_SORT_FIELDS` trong phân trang sản phẩm là gì?
   * *Gợi ý: Ngăn chặn người dùng sort theo các trường nhạy cảm hoặc không tồn tại.*

---

*Tài liệu này là bức tranh hoàn chỉnh nhất về toàn bộ kiến trúc bạn đã làm chủ trong dự án Supermarket API. Chúc bạn luôn tự tin và tiến bộ vượt bậc!*
