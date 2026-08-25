# 🏗️ GIÁO TRÌNH KIẾN TRÚC PHÂN TẦNG & QUY TRÌNH PHÁT TRIỂN MODULE MỚI

> 🎯 **Mục tiêu**: Hiểu sâu sắc trách nhiệm của từng package trong kiến trúc Layered Architecture của Spring Boot, giải phẫu luồng đi của 1 request từ Client đến Database, nắm vững **Quy trình 6 bước chuẩn** khi tạo module mới và cung cấp **Bộ Nhiệm Vụ Thực Hành (Project Quests)**!
> ⏱️ **Thời gian đọc**: ~18 phút.

---

## 📚 PHẦN 1: GIẢI PHÃ BẢN CHẤT LÝ THUYẾT (DEEP DIVE THEORY)

---

### 1. Bản Chất Phân Tầng: Tại Sao Không Viết Chung 1 Chỗ?

Tưởng tượng một **Nhà Hàng 5 Sao**:
* **Tiếp tân (Controller)**: Đứng ở cửa đón khách, kiểm tra xem khách có đặt bàn trước không, nhận phiếu yêu cầu món ăn. Tiếp tân **không bao giờ tự vào bếp xào nấu hay chạy xuống kho lấy thịt**.
* **Bếp trưởng (Service)**: Nhận phiếu gọi món, tính toán công thức nấu, nêm nếm gia vị, kiểm tra hạn sử dụng. Bếp trưởng **không ra đón khách và không trực tiếp quản lý kho thóc**.
* **Thủ kho (Repository)**: Quản lý kho nguyên liệu, nhận lệnh từ Bếp trưởng để xuất thịt/rau (Database).

```text
[ Client (Khách ăn) ]
         │
         │ 1. Gửi HTTP Request (Phiếu gọi món)
         ▼
[ CONTROLLER LAYER (Tiếp tân) ] ── (Nhận request, kiểm tra @Valid dữ liệu đầu vào)
         │
         │ 2. Truyền Request DTO
         ▼
[ SERVICE LAYER (Bếp trưởng) ] ── (Tính toán tiền, kiểm tra tồn kho, trừ kho, @Transactional)
         │
         │ 3. Gọi lệnh truy vấn
         ▼
[ REPOSITORY LAYER (Thủ kho) ] ── (Sinh câu lệnh SQL: SELECT, INSERT, UPDATE, DELETE)
         │
         │ 4. Thao tác dữ liệu
         ▼
[ MYSQL DATABASE (Kho nguyên liệu) ]
```

---

### 2. Giải Phẫu Chi Tiết Trách Nhiệm Từng Package

```text
src/main/java/com/api/supermarket/
│
├── config/          ⚙️ Cấu hình Bean toàn hệ thống (SecurityConfig, JacksonConfig, CorsConfig, JpaAuditing)
├── controller/      🎮 Cửa ngõ API: Nhận Request, gọi Service và trả về ResponseEntity
├── dto/             📦 Hộp vận chuyển dữ liệu (Data Transfer Objects)
│   ├── request/     📥 Dữ liệu Client gửi vào (Có kèm các annotation Validation: @NotBlank, @Min)
│   └── response/    📤 Dữ liệu an toàn Server trả ra (Ẩn mật khẩu băm, định dạng ngày tháng đẹp mắt)
├── entity/          💾 Object ánh xạ trực tiếp với bảng trong MySQL (Mapping qua JPA @Entity)
├── repository/      🗄️ Interface truy vấn dữ liệu (Kế thừa JpaRepository, viết câu truy vấn JPQL)
├── service/         📜 Interface: Khai báo "Hợp đồng" các hàm nghiệp vụ
│   └── Impl/        ⚙️ Class cài đặt: Nơi viết logic tính toán, gọi Repo và chuyển đổi mapToResponse
├── security/        🛡️ Bộ gác cổng: JwtAuthenticationFilter, JwtTokenProvider, EntryPoint (401), AccessDenied (403)
└── exception/       🚨 Bắt lỗi tập trung: GlobalExceptionHandler, ApiErrorResponse, Custom Exception
```

---

### 3. Quy Trình 6 Bước Chuẩn Khi Phát Triển 1 Module Mới (Ví Dụ: Module `Voucher`)

Khi được giao làm một chức năng mới từ A đến Z, luôn tuân theo đúng thứ tự 6 bước sau:

```text
┌────────────────────────────────────────────────────────────────────────────────────────┐
│ BƯỚC 1: TẠO ENTITY (entity/Voucher.java)                                                │
│   ├── Khai báo các cột bảng: voucherId, voucherCode, discountPercent, expiryDate...    │
│   └── Gắn các annotation: @Entity, @Table, @Id, @GeneratedValue                        │
├────────────────────────────────────────────────────────────────────────────────────────┤
│ BƯỚC 2: TẠO REPOSITORY (repository/VoucherRepository.java)                              │
│   ├── Kế thừa: JpaRepository<Voucher, Long>                                            │
│   └── Khai báo các hàm tìm kiếm: findByVoucherCode(String code), existsByVoucherCode() │
├────────────────────────────────────────────────────────────────────────────────────────┤
│ BƯỚC 3: TẠO BỘ DTOs (dto/request & dto/response)                                       │
│   ├── VoucherRequest: Chứa validation @NotBlank, @Min(1), @Max(100), @Future           │
│   └── VoucherResponse: Chứa các trường trả ra ngoài cho Frontend                       │
├────────────────────────────────────────────────────────────────────────────────────────┤
│ BƯỚC 4: TẠO SERVICE & SERVICEIMPL                                                      │
│   ├── VoucherService (Interface): Khai báo createVoucher, applyVoucher, getVoucherById  │
│   └── VoucherServiceImpl: Viết logic xử lý + hàm mapToResponse + @Transactional        │
├────────────────────────────────────────────────────────────────────────────────────────┤
│ BƯỚC 5: TẠO CONTROLLER (controller/VoucherController.java)                             │
│   ├── Đánh dấu: @RestController, @RequestMapping("/api/vouchers")                      │
│   └── Gọi VoucherService và bọc kết quả vào ResponseEntity                             │
├────────────────────────────────────────────────────────────────────────────────────────┤
│ BƯỚC 6: KIỂM THỬ API TRÊN POSTMAN / BRUNO                                               │
│   ├── Test trường hợp thành công (HTTP 200 / 201)                                      │
│   └── Test trường hợp dữ liệu sai (HTTP 400 Bad Request, 404 Not Found)                │
└────────────────────────────────────────────────────────────────────────────────────────┘
```

---

## 🚨 PHẦN 2: BẢNG BẮT BỆNH LỖI KIẾN TRÚC THƯỜNG GẶP (TROUBLESHOOTING)

| Triệu chứng sai phạm | Hậu quả nghiêm trọng | Cách sửa chuẩn xác |
| :--- | :--- | :--- |
| **Controller gọi thẳng Repository** | Bỏ qua tầng Service $\rightarrow$ Logic kiểm tra quyền, tính tiền, trừ kho và `@Transactional` bị viết lặp lại ở nhiều nơi. | Controller **chỉ được phép gọi Service**. |
| **Trả trực tiếp Entity ra Controller** | Lộ mật khẩu `password_hash`, gây lỗi lặp vô tận JSON làm sập RAM server. | Bắt buộc chuyển đổi Entity $\rightarrow$ `Response DTO` qua hàm `mapToResponse`. |
| **Dùng từ khóa `new ServiceImpl()`** | Phá vỡ cơ chế Dependency Injection của Spring, không thể viết Unit Test mock được. | Luôn dùng **Constructor Injection** để Spring tự truyền Bean vào. |
| **Service chứa mã HTTP (`HttpStatus.OK`)** | Tầng nghiệp vụ bị dính chặt vào giao thức HTTP (nếu sau này chuyển sang chạy bằng Message Queue Kafka thì hỏng hết code). | Service chỉ trả về dữ liệu thuần (DTO, List, Exception), Controller mới là nơi quyết định mã HTTP status. |

---

## 🎮 PHẦN 3: BỘ NHIỆM VỤ THỰC HÀNH DỰ ÁN MỚI (ARCHITECTURE QUESTS)

---

### ⚔️ QUEST 1: Xây Dựng Tầng CSDL & Repository Cho Module `Category`
* **Nhiệm vụ**: Tạo `entity/Category.java` và `repository/CategoryRepository.java`.
* **Yêu cầu**:
  1. Entity chứa `categoryId`, `categoryName` (unique), `description`, `isActive`.
  2. Repository kế thừa `JpaRepository<Category, Long>` và viết thêm hàm `boolean existsByCategoryNameIgnoreCase(String name)`.

---

### ⚔️ QUEST 2: Xây Dựng Tầng Nghiệp Vụ Service & Mapper
* **Nhiệm vụ**: Tạo `service/CategoryService.java` và `service/Impl/CategoryServiceImpl.java`.
* **Yêu cầu**:
  1. Viết hàm `createCategory(CategoryRequest request)`: Kiểm tra trùng tên $\rightarrow$ ném `BadRequestException` nếu đã tồn tại.
  2. Viết hàm `getCategoryById(Long id)`: Tìm trong DB $\rightarrow$ ném `ResourceNotFoundException` nếu không thấy.
  3. Viết hàm chuyển đổi `mapToResponse(Category category)`.

---

### ⚔️ QUEST 3: Xây Dựng Tầng Controller & Phân Quyền
* **Nhiệm vụ**: Tạo `controller/CategoryController.java`.
* **Yêu cầu**:
  1. Đánh dấu `@RestController` và `@RequestMapping("/api/categories")`.
  2. Dùng Constructor Injection để tiêm `CategoryService`.
  3. Gắn `@Valid` trước `@RequestBody CategoryRequest`.
  4. Gắn `@PreAuthorize("hasRole('ADMIN')")` cho API tạo và xóa danh mục.

---

## 🏆 BOSS QUEST: THỬ THÁCH THÊM TRƯỜNG DỮ LIỆU TOÀN DIỆN

* 🧪 **Thử thách**: Yêu cầu nghiệp vụ thay đổi — Bạn cần thêm cột `display_order INT` (thứ tự hiển thị trên giao diện) vào bảng `categories`.
* 📋 **Yêu cầu thực hiện tuần tự**:
  1. Cập nhật Entity `Category.java` $\rightarrow$ thêm trường `private Integer displayOrder;`.
  2. Cập nhật `CategoryRequest.java` $\rightarrow$ thêm `@Min(1) private Integer displayOrder;`.
  3. Cập nhật `CategoryResponse.java` và hàm `mapToResponse()`.
  4. Mở Postman test tạo danh mục có `displayOrder = 1` $\rightarrow$ *Kỳ vọng: API trả về JSON chứa đầy đủ trường mới mượt mà!*
