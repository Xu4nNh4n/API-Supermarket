# CÁC PHẦN CHƯA HOÀN THÀNH

Tài liệu này là checklist công việc tiếp theo của Supermarket API.

Nguyên tắc sử dụng:

- Làm lần lượt từ trên xuống.
- Chỉ đánh dấu `[x]` sau khi code compile và đã test API.
- Mỗi lần chỉ nên xử lý một nhóm nhỏ để dễ tìm lỗi.
- Sau mỗi mục, chạy:

```powershell
.\mvnw.cmd clean compile
.\mvnw.cmd test
```

---

## Trạng thái hiện tại

Những phần chính đã có:

- CRUD cho User, Product, Category, Supplier và Role.
- Request DTO và validation cho phần lớn API.
- Response DTO cho User, Product và Supplier.
- Phân trang, sắp xếp, tìm kiếm và lọc.
- Đăng nhập bằng BCrypt.
- Tạo và kiểm tra JWT.
- JWT Authentication Filter.
- Kiểm tra tài khoản đang hoạt động.
- Phân quyền API theo Role.
- Response JSON cho lỗi 400, 401, 403, 404 và 500.
- Swagger/OpenAPI và kiểm thử bằng Bruno.

Phần xử lý đăng nhập sai bằng `UnauthorizedException` đã hoàn thành.

---

## 1. Sửa lỗi update User khi email null

Mức ưu tiên: Cao

### Vấn đề

Trong `UserServiceImpl`, code đang gọi:

```java
existingUser.getEmail().equals(request.getEmail())
```

Nếu email hiện tại của User là `null`, code có thể phát sinh
`NullPointerException` và trả HTTP 500.

### File cần mở

```text
src/main/java/com/api/supermarket/service/Impl/UserServiceImpl.java
```

### Gợi ý

- Tìm hiểu `Objects.equals(a, b)`.
- Method này so sánh an toàn kể cả khi một hoặc hai giá trị là `null`.
- Cần import `java.util.Objects`.

### Kiểm thử

- [x] Update User đang có email bình thường.
- [x] Update User đang có email null.
- [x] Giữ nguyên email cũ không bị báo trùng.
- [x] Dùng email của User khác phải trả 400.
- [x] `clean compile` thành công.

---

## 2. Chuẩn hóa cách update isActive

Mức ưu tiên: Cao

### Vấn đề

Khi client không gửi `isActive`:

- User và Product có thể tự chuyển thành `true`.
- Supplier và Category có thể bị gán thành `null`.

Update không nên tự thay đổi trạng thái nếu client không gửi field này.

### File cần mở

```text
src/main/java/com/api/supermarket/service/Impl/UserServiceImpl.java
src/main/java/com/api/supermarket/service/Impl/ProductServiceImpl.java
src/main/java/com/api/supermarket/service/Impl/SupplierServiceImpl.java
src/main/java/com/api/supermarket/service/Impl/CategoryServiceImpl.java
```

### Quy tắc nên áp dụng

Khi tạo mới:

```text
isActive null -> mặc định true
```

Khi cập nhật:

```text
isActive null     -> giữ trạng thái cũ
isActive có giá trị -> cập nhật theo request
```

### Gợi ý

Trong method update, chỉ gọi setter khi request có giá trị khác null.

### Kiểm thử

- [ ] Tạo mới không gửi `isActive` thì dữ liệu nhận `true`.
- [ ] Update không gửi `isActive` thì trạng thái cũ được giữ nguyên.
- [ ] Update gửi `false` thì trạng thái chuyển thành false.
- [ ] Update gửi `true` thì trạng thái chuyển thành true.

---

## 3. Bỏ createAt khỏi Request DTO

Mức ưu tiên: Cao

### Vấn đề

Các Entity đang khai báo:

```java
@Column(name = "created_at", insertable = false, updatable = false)
```

Điều đó có nghĩa `created_at` do database tạo và ứng dụng không được insert
hoặc update field này. Vì vậy client không cần gửi `createAt`.

### File cần sửa

```text
src/main/java/com/api/supermarket/dto/request/CategoryRequest.java
src/main/java/com/api/supermarket/dto/request/SupplierRequest.java
src/main/java/com/api/supermarket/service/Impl/CategoryServiceImpl.java
src/main/java/com/api/supermarket/service/Impl/SupplierServiceImpl.java
```

### Công việc

- [x] Xóa field `createAt` khỏi `CategoryRequest`.
- [x] Xóa getter/setter `createAt` khỏi `CategoryRequest`.
- [x] Xóa field `createAt` khỏi `SupplierRequest`.
- [x] Xóa getter/setter `createAt` khỏi `SupplierRequest`.
- [x] Xóa các lệnh `setCreateAt(request.getCreateAt())`.
- [x] Giữ `createAt` trong Response DTO nếu client cần xem ngày tạo.

### Kiểm thử

- [x] Tạo Category không cần truyền `createAt`.
- [x] Tạo Supplier không cần truyền `createAt`.
- [ ] Database vẫn tự sinh `created_at`.
- [x] Response Supplier vẫn trả ngày tạo.

---

## 4. Kiểm tra khóa ngoại trước khi xóa

Mức ưu tiên: Cao

### Supplier

Supplier đã có bước kiểm tra Product liên quan bằng:

```text
ProductRepository.existsBySupplierId(...)
```

Cần kiểm thử:

- [ ] Xóa Supplier không có Product trả 204.
- [ ] Xóa Supplier đang có Product trả 400.
- [ ] Xóa Supplier không tồn tại trả 404.

Nên tìm Supplier trước, sau đó mới kiểm tra Product liên quan để luồng lỗi rõ ràng.

### Category

Hiện Category chưa kiểm tra Product liên quan trước khi xóa.

File cần mở:

```text
src/main/java/com/api/supermarket/repository/ProductRepository.java
src/main/java/com/api/supermarket/service/Impl/CategoryServiceImpl.java
```

Gợi ý:

- Tạo derived query kiểm tra Product theo `categoryId`.
- Nếu còn Product, ném `BadRequestException`.
- Nếu Category không tồn tại, trả 404.

Checklist:

- [ ] Xóa Category không có Product trả 204.
- [ ] Xóa Category đang có Product trả 400.
- [ ] Xóa Category không tồn tại trả 404.

### Role

Hiện Role chưa kiểm tra User liên quan trước khi xóa.

File cần mở:

```text
src/main/java/com/api/supermarket/repository/UserRepository.java
src/main/java/com/api/supermarket/service/Impl/RoleServiceImpl.java
```

Gợi ý:

- Tạo derived query kiểm tra User theo `roleId`.
- Nếu còn User, ném `BadRequestException`.

Checklist:

- [ ] Xóa Role không có User trả 204.
- [ ] Xóa Role đang có User trả 400.
- [ ] Xóa Role không tồn tại trả 404.

---

## 5. Hoàn thiện CategoryResponse

Mức ưu tiên: Trung bình

### Vấn đề

Category hiện vẫn được trả trực tiếp dưới dạng Entity.

### Thứ tự file nên làm

```text
1. dto/response/CategoryResponse.java
2. service/CategoryService.java
3. service/Impl/CategoryServiceImpl.java
4. controller/CategoryController.java
```

### Field gợi ý

```text
categoryId
categoryName
description
isActive
createAt
```

### Công việc

- [ ] Tạo `CategoryResponse`.
- [ ] Tạo `mapToResponse(Category category)`.
- [ ] Đổi return type trong `CategoryService`.
- [ ] Map danh sách bằng Stream.
- [ ] Đổi `PageResponse<Category>` thành `PageResponse<CategoryResponse>`.
- [ ] Đổi return type trong `CategoryController`.

### Kiểm thử

- [ ] GET danh sách Category.
- [ ] GET Category theo ID.
- [ ] POST Category.
- [ ] PUT Category.
- [ ] Search và active.
- [ ] Phân trang vẫn trả metadata đúng.

---

## 6. Hoàn thiện RoleResponse

Mức ưu tiên: Trung bình

### Thứ tự file nên làm

```text
1. dto/response/RoleResponse.java
2. service/RoleService.java
3. service/Impl/RoleServiceImpl.java
4. controller/RoleController.java
```

### Field gợi ý

```text
roleId
roleName
description
createAt
```

### Công việc

- [ ] Tạo `RoleResponse`.
- [ ] Tạo `mapToResponse(Role role)`.
- [ ] Đổi toàn bộ return type trong Service.
- [ ] Đổi toàn bộ return type trong Controller.
- [ ] Không trả trực tiếp Role Entity.

---

## 7. Thêm validation cho LoginRequest

Mức ưu tiên: Trung bình

### File cần mở

```text
src/main/java/com/api/supermarket/dto/request/LoginRequest.java
src/main/java/com/api/supermarket/controller/AuthController.java
```

### Công việc

- [ ] Username không được để trống.
- [ ] Password không được để trống.
- [ ] Thêm `@Valid` tại request body của API login.

### Kết quả mong đợi

```text
Thiếu username/password -> HTTP 400
Sai username/password   -> HTTP 401
Đúng username/password  -> HTTP 200
```

---

## 8. Validate phân trang và sắp xếp

Mức ưu tiên: Trung bình

### Vấn đề

Các giá trị sau có thể làm API rơi vào lỗi 500:

```text
page < 0
size <= 0
size quá lớn
sortBy không tồn tại
sortDir không phải asc hoặc desc
```

### Module cần áp dụng

```text
Product
User
Supplier
Category
```

### Quy tắc gợi ý

```text
page >= 0
1 <= size <= 100
sortDir chỉ nhận asc hoặc desc
sortBy phải nằm trong danh sách field cho phép
```

### Kiểm thử

- [ ] Page âm trả 400.
- [ ] Size bằng 0 trả 400.
- [ ] Size quá lớn trả 400.
- [ ] `sortDir=abc` trả 400.
- [ ] `sortBy` không tồn tại trả 400.

---

## 9. Tách CreateUserRequest và UpdateUserRequest

Mức ưu tiên: Trung bình

### Vấn đề

`UserRequest` hiện được dùng chung cho create và update.

Password đang bắt buộc, vì vậy khi chỉ muốn sửa tên hoặc email, client vẫn phải gửi
password mới và Service sẽ mã hóa lại password.

### Hướng làm

```text
CreateUserRequest
  -> password bắt buộc

UpdateUserRequest
  -> password không bắt buộc
  -> chỉ đổi password khi client gửi giá trị mới
```

### Kiểm thử

- [ ] Create User thiếu password trả 400.
- [ ] Update User không gửi password vẫn thành công.
- [ ] Update có password mới thì BCrypt hash được thay đổi.
- [ ] API không bao giờ trả password hash.

---

## 10. Tối ưu ProductResponse để tránh N+1 query

Mức ưu tiên: Trung bình, làm sau khi DTO đã hoàn thiện.

### Vấn đề

`ProductServiceImpl.mapToResponse()` đang query Category và Supplier cho từng Product.

Ví dụ một trang có 10 Product có thể phát sinh:

```text
1 query lấy Product
10 query lấy Category
10 query lấy Supplier
```

### Hướng tìm hiểu

- Quan hệ `@ManyToOne`.
- `JOIN FETCH`.
- DTO projection.
- EntityGraph.

Chỉ chọn một giải pháp sau khi hiểu rõ quan hệ Entity. Không cần tối ưu vội khi đang
hoàn thiện luồng DTO cơ bản.

### Kiểm thử

- [ ] ProductResponse vẫn có categoryName và supplierName.
- [ ] Không phát sinh hai query bổ sung cho mỗi Product.
- [ ] Phân trang vẫn hoạt động đúng.

---

## 11. Chuẩn hóa cấu hình bí mật

Mức ưu tiên: Cao trước khi đưa code lên Git hoặc triển khai.

### Không nên ghi trực tiếp

```text
Database password
JWT secret
```

### File cần mở

```text
src/main/resources/application.properties
```

### Hướng cấu hình

```properties
spring.datasource.password=${DB_PASSWORD}
jwt.secret=${JWT_SECRET}
```

Có thể dùng giá trị mặc định chỉ cho môi trường local, nhưng không được commit secret thật.

### Công việc

- [ ] Đổi database password sang biến môi trường.
- [ ] Đổi JWT secret sang biến môi trường.
- [ ] Thay secret/password đã từng bị commit nếu repository được chia sẻ.
- [ ] Tắt hoặc cấu hình `show-sql` phù hợp khi triển khai.
- [ ] Cân nhắc `spring.jpa.open-in-view=false`.

---

## 12. Viết Unit Test

Mức ưu tiên: Cao sau khi hoàn thiện DTO và lỗi nghiệp vụ.

Hiện project chỉ có test:

```text
contextLoads()
```

Test này chứng minh Spring context khởi động được, chưa kiểm tra nghiệp vụ.

### Bắt đầu với SupplierServiceImplTest

Các trường hợp nên test:

- [ ] Tạo Supplier thành công.
- [ ] Email trùng trả `BadRequestException`.
- [ ] Số điện thoại trùng trả `BadRequestException`.
- [ ] Lấy Supplier không tồn tại trả `ResourceNotFoundException`.
- [ ] Không cho xóa Supplier đang có Product.
- [ ] Update không gửi `isActive` giữ trạng thái cũ.
- [ ] Mapping sang `SupplierResponse` đúng.

### Sau đó viết

```text
UserServiceImplTest
ProductServiceImplTest
CategoryServiceImplTest
RoleServiceImplTest
AuthServiceImplTest
```

### Công cụ cần học

```text
JUnit 5
Mockito
@Mock
@InjectMocks
when(...).thenReturn(...)
assertThrows(...)
assertEquals(...)
verify(...)
```

---

## 13. Viết Integration Test cho Security

Mức ưu tiên: Sau Unit Test.

Các trường hợp cần kiểm tra:

- [ ] Login đúng trả 200 và token.
- [ ] Login sai trả 401, không phải 500.
- [ ] Không có token gọi API bảo vệ trả 401.
- [ ] Token không hợp lệ trả 401.
- [ ] Token của tài khoản bị khóa trả 401.
- [ ] ADMIN gọi API ADMIN thành công.
- [ ] Role không đủ quyền trả 403.

Không nên để test phụ thuộc trực tiếp vào database local đang sử dụng hằng ngày.

---

## 14. Thêm Refresh Token

Mức ưu tiên: Sau khi login, exception và test đã ổn định.

### Cần thiết kế trước

- Access token sống ngắn.
- Refresh token sống dài hơn.
- Refresh token phải có khả năng thu hồi.
- Không dùng access token hết hạn để gọi API nghiệp vụ.

### Các thành phần dự kiến

```text
RefreshToken entity hoặc cơ chế lưu token
RefreshTokenRepository
RefreshTokenService
RefreshTokenRequest
TokenResponse
POST /api/auth/refresh
POST /api/auth/logout
```

### Trường hợp cần test

- [ ] Refresh token hợp lệ cấp access token mới.
- [ ] Refresh token hết hạn bị từ chối.
- [ ] Refresh token đã thu hồi bị từ chối.
- [ ] Logout làm refresh token không còn sử dụng được.

---

## 15. Transaction và JPA Auditing

Mức ưu tiên: Sau khi Unit Test cơ bản.

### Transaction

Tìm hiểu và áp dụng `@Transactional` cho các nghiệp vụ có nhiều thao tác database cần
thành công hoặc thất bại cùng nhau.

### Auditing

Mục tiêu:

```text
createdAt tự động khi tạo
updatedAt tự động khi cập nhật
```

Nội dung cần tìm hiểu:

```text
@EnableJpaAuditing
@CreatedDate
@LastModifiedDate
@EntityListeners(AuditingEntityListener.class)
```

---

## 16. Liquibase

Mức ưu tiên: Sau khi hiểu Entity và schema hiện tại.

### Mục tiêu

- Quản lý thay đổi database bằng file migration.
- Không sửa schema thủ công mà không lưu lịch sử.
- Mỗi thay đổi có changeset riêng.

### Các bước dự kiến

```text
1. Thêm dependency Liquibase
2. Tạo master changelog
3. Tạo changelog cho schema hiện tại
4. Chạy thử trên database mới
5. Thêm changeset cho thay đổi tiếp theo
```

---

## 17. Logging

Mức ưu tiên: Trước khi triển khai.

### Cần log

- Lỗi 500 kèm stack trace ở server.
- Login thất bại nhưng không log password.
- Thao tác tạo, sửa, xóa quan trọng.
- Các lỗi tích hợp hoặc database.

### Không được log

```text
Password
JWT secret
Toàn bộ access token
Thông tin nhạy cảm không cần thiết
```

---

## 18. Docker và Nginx

Mức ưu tiên: Đã hoàn thành cấu hình cơ bản.

### Docker

- [x] Tạo Dockerfile cho Spring Boot (Multi-stage Eclipse Temurin Java 21).
- [x] Build file JAR và package.
- [x] Build Docker image thành công.
- [x] Tạo docker-compose kết nối Backend với MySQL máy host (`host.docker.internal`).
- [x] Truyền biến môi trường an toàn qua file `.env` (`DB_PASSWORD`, `JWT_SECRET`).
- [x] Chạy ứng dụng dưới non-root user `spring`.

### Nginx

- [x] Cấu hình reverse proxy cổng 80 chuyển tiếp vào Spring Boot cổng 8080.
- [x] Chuyển tiếp nguyên vẹn Authorization header cho JWT.
- [x] Test request đi qua Nginx (`http://localhost/api/...`).
- [ ] Mở rộng load balancing với 2 backend instance khi cần.

---

## 19. Module Sales Order (Hóa đơn bán hàng)

### Entity & Repository
- [x] Entity `Customer`, `SalesOrder`, `SalesOrderItem`.
- [x] Repository `CustomerRepository`, `SalesOrderRepository`, `SalesOrderItemRepository`.

### DTO
- [x] `OrderItemRequest`, `SalesOrderRequest`.
- [x] `OrderItemResponse`, `SalesOrderResponse`.

### Exception & Logging
- [x] `InsufficientStockException` (Lỗi tồn kho).
- [x] `MdcLoggingFilter` (Gắn `traceId` tự động vào MDC).
- [x] Xử lý `InsufficientStockException` trong `GlobalExceptionHandler`.

### Service & Controller
- [ ] Interface `SalesOrderService`.
- [ ] Triển khai `SalesOrderServiceImpl` (Trừ kho, tính tiền, `@Transactional`, ghi log chuẩn).
- [ ] `SalesOrderController` (POST `/api/orders`, PUT pay, PUT cancel, GET filter).

---

## Mục đang làm tiếp theo

Bắt đầu tại:

```text
src/main/java/com/api/supermarket/service/SalesOrderService.java
src/main/java/com/api/supermarket/service/Impl/SalesOrderServiceImpl.java
```

Nhiệm vụ:

```text
Viết logic tạo hóa đơn, kiểm tra tồn kho, trừ số lượng sản phẩm và tính toán tổng tiền thanh toán.
```

