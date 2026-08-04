# BẢN ĐỒ KIẾN TRÚC SUPERMARKET API

Tài liệu này giải thích cách project Supermarket API được chia thành nhiều package, vì sao cần tách như vậy, trách nhiệm của từng lớp và thứ tự code thực sự chạy khi client gọi API.

Mục tiêu không chỉ là biết **file nào chứa code gì**, mà còn hiểu:

- Vì sao không viết toàn bộ code trong Controller.
- Vì sao Entity không nên dùng trực tiếp làm dữ liệu request/response.
- Vì sao cần cả Service interface và ServiceImpl.
- JWT đi qua những file nào trước khi request vào Controller.
- Lỗi 400, 401, 403, 404 và 500 được tạo ở đâu.
- Khi cần thêm một chức năng mới thì nên bắt đầu từ file nào.

---

## 1. Bức tranh tổng thể

Project đang đi theo kiến trúc phân lớp:

```text
Client (Bruno / Swagger / Frontend)
              |
              | HTTP request
              v
       Spring Security Filter
              |
              v
          Controller
              |
              v
       Service interface
              |
              v
        ServiceImpl
          |       |
          |       +----> Mapper: Entity -> Response DTO
          v
       Repository
              |
              v
      JPA / Hibernate
              |
              v
           MySQL
```

Khi có lỗi, luồng có thể rẽ sang:

```text
Service ném exception
        |
        v
GlobalExceptionHandler
        |
        v
ApiErrorResponse dạng JSON
```

Nếu lỗi xảy ra trong tầng Spring Security trước Controller:

```text
Chưa đăng nhập --------------> JwtAuthenticationEntryPoint -> 401
Đã đăng nhập nhưng thiếu quyền -> JwtAccessDeniedHandler ----> 403
```

Điểm quan trọng: mỗi lớp chỉ nên giải quyết một nhóm trách nhiệm. Việc tách lớp làm code dài hơn lúc ban đầu, nhưng giúp đọc, sửa, test và mở rộng dễ hơn khi project lớn lên.

---

## 2. Cấu trúc package hiện tại

```text
com.api.supermarket
|
|-- config/              Cấu hình các bean và Spring Security
|-- controller/          Nhận HTTP request và trả HTTP response
|-- dto/
|   |-- request/         Dữ liệu client được phép gửi vào
|   `-- response/        Dữ liệu API chủ động trả ra
|-- entity/              Ánh xạ object Java với bảng trong MySQL
|-- exception/           Khai báo và xử lý lỗi tập trung
|-- repository/          Truy vấn dữ liệu qua Spring Data JPA
|-- security/            Tạo, đọc JWT và xử lý xác thực/phân quyền
|-- service/             Khai báo các nghiệp vụ mà hệ thống cung cấp
|   `-- Impl/            Viết chi tiết cách thực hiện nghiệp vụ
`-- SupermarketApplication.java
```

Tên package `Impl` hiện đang viết hoa. Java convention thường dùng package chữ thường hoàn toàn, ví dụ `service.impl`. Cấu trúc hiện tại vẫn chạy được; có thể chuẩn hóa sau để tránh thay đổi nhiều file cùng lúc trong lúc đang học chức năng chính.

---

## 3. Vì sao cần tách từng lớp?

### 3.1. Controller: cửa vào của API

Ví dụ:

```text
controller/ProductController.java
```

Controller chịu trách nhiệm:

- Định nghĩa URL bằng `@RequestMapping`, `@GetMapping`, `@PostMapping`...
- Đọc path variable, query parameter và request body.
- Kích hoạt validation bằng `@Valid`.
- Gọi đúng method của Service.
- Trả kết quả và HTTP status cho client.
- Khai báo quyền truy cập bằng `@PreAuthorize` khi cần.

Ví dụ rút gọn:

```java
@PostMapping
public ProductResponse createProduct(
        @Valid @RequestBody ProductRequest request) {
    return productService.createProduct(request);
}
```

Ý nghĩa từng phần:

- `@PostMapping`: nhận request HTTP POST tại `/api/products`.
- `@RequestBody`: chuyển JSON client gửi thành object `ProductRequest`.
- `@Valid`: kiểm tra các annotation validation trong `ProductRequest`.
- `productService.createProduct(request)`: chuyển việc xử lý nghiệp vụ cho Service.
- `ProductResponse`: xác định cấu trúc JSON trả về.

Controller **không nên** tự truy vấn Repository, tự kiểm tra SKU, tự tạo Product rồi tự lưu. Nếu làm vậy, Controller sẽ vừa xử lý HTTP, vừa xử lý nghiệp vụ, vừa xử lý database. Khi đó class sẽ lớn nhanh, khó test và một nghiệp vụ khó dùng lại ở nơi khác.

Controller nên mỏng: nhận dữ liệu, chuyển tiếp, trả kết quả.

### 3.2. Service interface: hợp đồng nghiệp vụ

Ví dụ:

```text
service/ProductService.java
```

Interface khai báo hệ thống **có thể làm gì**, chưa nói chi tiết làm bằng cách nào:

```java
ProductResponse createProduct(ProductRequest request);

PageResponse<ProductResponse> filterProducts(
        int page,
        int size,
        String sortBy,
        String sortDir,
        String keyword,
        Long categoryId,
        Long supplierId,
        Boolean active
);
```

Lợi ích của interface:

- Controller phụ thuộc vào hợp đồng `ProductService`, không phụ thuộc trực tiếp cách cài đặt.
- Nhìn vào interface có thể biết toàn bộ chức năng của module mà chưa cần đọc code dài trong ServiceImpl.
- Dễ tạo implementation khác hoặc mock Service khi viết unit test.
- Buộc ServiceImpl phải implement đủ method và đúng kiểu dữ liệu.

Ví dụ, Controller chỉ biết gọi `createProduct()`. Nó không cần biết dữ liệu được lưu bằng JPA, JDBC hay gọi sang một service khác.

Đây là tư tưởng Dependency Inversion: tầng bên ngoài phụ thuộc vào abstraction thay vì phụ thuộc chặt vào implementation.

### 3.3. ServiceImpl: nơi xử lý nghiệp vụ

Ví dụ:

```text
service/Impl/ProductServiceImpl.java
```

Đây là nơi trả lời các câu hỏi nghiệp vụ như:

- SKU đã tồn tại chưa?
- Category và Supplier được chọn có tồn tại không?
- Khi không tìm thấy Product thì trả lỗi nào?
- Giá trị mặc định của `isActive` là gì?
- Entity được chuyển thành `ProductResponse` như thế nào?
- Dữ liệu được phân trang và sắp xếp ra sao?

Ví dụ luồng tạo sản phẩm:

```text
ProductRequest
    |
    | kiểm tra SKU
    | kiểm tra categoryId
    | kiểm tra supplierId
    v
Tạo Product entity
    |
    v
productRepository.save(product)
    |
    v
mapToResponse(savedProduct)
    |
    v
ProductResponse
```

ServiceImpl được đánh dấu `@Service` để Spring tạo bean và quản lý object này. Constructor injection giúp Spring truyền các Repository cần thiết vào ServiceImpl:

```java
public ProductServiceImpl(
        ProductRepository productRepository,
        CategoryRepository categoryRepository,
        SupplierRepository supplierRepository) {
    this.productRepository = productRepository;
    this.categoryRepository = categoryRepository;
    this.supplierRepository = supplierRepository;
}
```

Không cần viết `new ProductRepository()` vì Repository là bean do Spring Data tạo. Constructor injection còn giúp thấy rõ class đang phụ thuộc vào những gì và dễ truyền mock khi test.

### 3.4. Repository: cổng truy cập database

Ví dụ:

```text
repository/ProductRepository.java
```

Repository chịu trách nhiệm truy vấn dữ liệu, không chịu trách nhiệm quyết định nghiệp vụ.

Khi extends `JpaRepository<Product, Long>`, Repository có sẵn:

- `findAll()`
- `findById(id)`
- `save(entity)`
- `delete(entity)`
- `existsById(id)`
- `findAll(pageable)`

Spring Data JPA còn có thể tạo query từ tên method:

```java
boolean existsBySkuCode(String skuCode);
List<Product> findByCategoryId(Long categoryId);
List<Product> findByCategoryIdAndSupplierId(Long categoryId, Long supplierId);
```

Tên sau `findBy`, `existsBy` phải dựa trên **tên thuộc tính Java trong Entity**, không dựa trực tiếp trên tên cột MySQL.

Ví dụ:

```java
@Column(name = "name")
private String productName;
```

Tên field Java là `productName`, nên method Repository phải dùng `findByProductName...`, dù tên cột MySQL là `name`.

Với query viết bằng JPQL:

```java
SELECT p FROM Product p WHERE LOWER(p.productName) LIKE ...
```

`Product` và `productName` cũng là tên Entity và field Java. Chỉ native SQL mới dùng trực tiếp tên bảng/cột MySQL.

### 3.5. Entity: hình dạng dữ liệu trong database

Ví dụ:

```text
entity/Product.java
```

Entity dùng để ánh xạ giữa object Java và bản ghi MySQL:

```java
@Entity
@Table(name = "products")
public class Product {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "product_id")
    private Long productId;

    @Column(name = "name")
    private String productName;
}
```

Ánh xạ tương ứng:

| Java | MySQL |
|---|---|
| `Product` | bảng `products` |
| `productId` | cột `product_id` |
| `productName` | cột `name` |

Entity nên tập trung vào cấu trúc dữ liệu lưu trữ và quan hệ JPA. Entity không nên quyết định HTTP response hay chứa logic phụ thuộc vào Controller.

### 3.6. Request DTO: dữ liệu được phép đi vào

Ví dụ:

```text
dto/request/ProductRequest.java
```

Request DTO là hợp đồng đầu vào giữa client và API. Nó có thể chứa validation:

```java
@NotBlank(message = "Tên sản phẩm không được để trống")
private String productName;
```

Vì sao không nhận thẳng `Product` entity?

- Client có thể gửi các field mà hệ thống không muốn cho sửa, chẳng hạn `productId`.
- Entity thay đổi theo database, nhưng API không nhất thiết phải thay đổi theo.
- Validation của request và ràng buộc database là hai việc khác nhau.
- Create request và update request có thể có quy tắc khác nhau.
- Giảm nguy cơ mass assignment: client tự gán field nhạy cảm ngoài ý muốn.

Request DTO trả lời câu hỏi: **client được gửi những gì?**

### 3.7. Response DTO: dữ liệu được phép đi ra

Ví dụ:

```text
dto/response/ProductResponse.java
dto/response/UserResponse.java
```

Response DTO trả lời câu hỏi: **API muốn công khai những gì?**

Ví dụ `User` entity có `passwordHash`, nhưng `UserResponse` không có field đó. Vì vậy API không vô tình trả password hash ra client.

`ProductResponse` còn có thể bổ sung dữ liệu thân thiện:

```text
categoryId   = 1
categoryName = "Đồ uống"
```

Trong database Product chỉ cần lưu khóa ngoại `categoryId`, còn response có thể trả thêm tên category để frontend không phải tự đoán.

### 3.8. Mapper: cầu nối giữa Entity và Response DTO

Hiện tại mapper là private method trong ServiceImpl:

```java
private ProductResponse mapToResponse(Product product) {
    // đọc thêm tên category và supplier
    // tạo ProductResponse
}
```

Mapper cần thiết vì Entity và Response có mục đích khác nhau. Không nên ép Entity phải mang đúng hình dạng response.

Với project hiện tại, để mapper riêng trong từng ServiceImpl là hợp lý và dễ học. Khi mapping nhiều hoặc bị lặp ở nhiều service, có thể tách thành package `mapper/` hoặc dùng MapStruct.

Lưu ý hiệu năng hiện tại: `mapToResponse()` gọi `categoryRepository.findById()` và `supplierRepository.findById()` cho từng Product. Nếu một trang có 10 Product, có thể phát sinh thêm khoảng 20 query. Đây là dạng vấn đề N+1. Chưa cần sửa ngay khi dữ liệu nhỏ, nhưng về sau nên ánh xạ `@ManyToOne` hoặc dùng query projection/join fetch.

### 3.9. PageResponse: response dùng chung cho phân trang

```text
dto/response/PageResponse.java
```

`PageResponse<T>` dùng generic `T`, nên có thể dùng chung:

```java
PageResponse<ProductResponse>
PageResponse<UserResponse>
PageResponse<CategoryResponse>
```

Nó chứa hai nhóm thông tin:

- `content`: dữ liệu của trang hiện tại.
- Metadata: `pageNumber`, `pageSize`, `totalElements`, `totalPages`, `last`.

Nếu mỗi module tự tạo `ProductPageResponse`, `UserPageResponse`..., code sẽ lặp lại gần như hoàn toàn. Generic giúp giữ một cấu trúc response thống nhất.

---

## 4. Luồng CRUD thực tế của Product

### 4.1. GET danh sách, lọc và phân trang

Request ví dụ:

```http
GET /api/products?page=0&size=10&sortBy=price&sortDir=desc&keyword=sua&active=true
Authorization: Bearer <access-token>
```

Thứ tự chạy:

```text
1. JwtAuthenticationFilter kiểm tra token
2. ProductController.getAllProducts(...)
3. ProductService.filterProducts(...)
4. ProductServiceImpl.filterProducts(...)
5. Tạo Sort
6. Tạo Pageable bằng PageRequest.of(...)
7. ProductRepository.filterProducts(..., pageable)
8. Hibernate tạo SQL và truy vấn MySQL
9. MySQL trả dữ liệu
10. Hibernate tạo Page<Product>
11. mapToResponse() đổi từng Product thành ProductResponse
12. ServiceImpl tạo PageResponse<ProductResponse>
13. Controller trả object
14. Jackson chuyển object thành JSON
15. Client nhận response
```

`page` bắt đầu từ `0`, vì Spring Data dùng zero-based page index.

`Sort.by(sortBy)` dùng tên field Entity. Ví dụ đúng là `productId`, `productName`, `price`; không nên gửi tên cột database như `product_id` nếu Entity không có field đó.

### 4.2. GET theo ID

```text
GET /api/products/10
  -> ProductController.getProductById(10)
  -> ProductServiceImpl.getProductById(10)
  -> productRepository.findById(10)
```

Nếu tìm thấy:

```text
Product entity -> mapToResponse() -> ProductResponse -> JSON 200
```

Nếu không tìm thấy:

```text
Optional.empty()
  -> orElseThrow(ResourceNotFoundException)
  -> GlobalExceptionHandler
  -> ApiErrorResponse
  -> JSON 404
```

### 4.3. POST tạo Product

```text
JSON body
  -> ProductRequest
  -> @Valid
  -> ProductController
  -> ProductServiceImpl
  -> kiểm tra nghiệp vụ
  -> Product entity
  -> Repository.save()
  -> MySQL INSERT
  -> ProductResponse
  -> JSON
```

Có hai loại kiểm tra khác nhau:

1. Validation hình thức đặt trong DTO:
   - Tên có bị trống không?
   - Giá có nhỏ hơn giá trị cho phép không?
   - Field bắt buộc có bị null không?

2. Validation nghiệp vụ đặt trong ServiceImpl:
   - SKU có bị trùng không?
   - Category có tồn tại không?
   - Supplier có tồn tại không?

DTO không thể tự biết database đang có SKU nào, nên kiểm tra trùng phải nằm ở Service/Repository.

### 4.4. PUT cập nhật Product

Khi update, Service cần lấy Product cũ trước:

```text
findById(id)
  -> không có: ném 404
  -> có: kiểm tra dữ liệu mới
  -> cập nhật field trên entity cũ
  -> save(entity)
```

Khi kiểm tra SKU, phải cho phép Product giữ lại chính SKU hiện tại. Chỉ báo trùng nếu SKU mới khác SKU cũ và SKU mới đã thuộc về Product khác.

### 4.5. DELETE Product

```text
DELETE /api/products/10
  -> kiểm tra Product có tồn tại
  -> repository.delete(product)
  -> ResponseEntity.noContent()
  -> HTTP 204
```

HTTP 204 có nghĩa thao tác thành công và response không có body.

---

## 5. JWT, xác thực và phân quyền

### 5.1. Phân biệt Authentication và Authorization

- Authentication: xác định **bạn là ai**. Ví dụ token chứng minh request thuộc về user `admin`.
- Authorization: xác định **bạn được làm gì**. Ví dụ chỉ `ADMIN`, `MANAGER`, `WAREHOUSE` được tạo Product.

Đăng nhập thành công chưa có nghĩa được phép gọi mọi API.

### 5.2. Luồng đăng nhập

```text
POST /api/auth/login
        |
        v
SecurityConfig: permitAll
        |
        v
AuthController.login(LoginRequest)
        |
        v
AuthServiceImpl.login()
        |
        |-- UserRepository.findByUserName()
        |-- kiểm tra isActive
        |-- PasswordEncoder.matches(rawPassword, passwordHash)
        `-- JwtService.generateToken(user)
        |
        v
LoginResponse chứa access token
```

`PasswordEncoder.matches()` không giải mã BCrypt hash. Nó băm password người dùng vừa nhập theo thông tin salt trong hash rồi so sánh kết quả.

JWT được ký bằng secret key. Client có thể đọc payload, nhưng không thể sửa payload rồi tạo chữ ký hợp lệ nếu không biết secret.

Không nên đặt password hoặc dữ liệu bí mật vào JWT vì JWT payload chỉ được encode, không mặc định được mã hóa.

### 5.3. Luồng gọi API có token

Client gửi:

```http
Authorization: Bearer eyJ...
```

Sau đó:

```text
JwtAuthenticationFilter
  -> đọc Authorization header
  -> kiểm tra tiền tố "Bearer "
  -> cắt token bằng substring(7)
  -> JwtService.isTokenValid(token)
  -> JwtService.extractUsername(token)
  -> UserRepository.findByUserName(username)
  -> kiểm tra user tồn tại và isActive
  -> đổi role thành SimpleGrantedAuthority("ROLE_" + roleName)
  -> tạo UsernamePasswordAuthenticationToken
  -> lưu Authentication vào SecurityContextHolder
  -> cho request chạy tiếp
```

Ví dụ role trong database là `ADMIN`, authority được tạo thành `ROLE_ADMIN`.

Vì `hasRole("ADMIN")` tự thêm tiền tố `ROLE_`, nên:

```java
@PreAuthorize("hasRole('ADMIN')")
```

sẽ kiểm tra authority `ROLE_ADMIN`.

Tương tự:

```java
@PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'WAREHOUSE')")
```

chỉ cần user có một trong ba role đó là được phép chạy method.

### 5.4. Vì sao vẫn tìm User trong database sau khi token hợp lệ?

Token cho biết username và role tại thời điểm login. Nhưng trạng thái tài khoản có thể thay đổi sau đó.

Filter query User lại giúp phát hiện:

- User đã bị xóa.
- User đã bị khóa bằng `isActive = false`.
- Role hiện tại đã thay đổi.

Đổi lại, mỗi request tốn thêm một query database. Sau này có thể cân nhắc cache hoặc chiến lược khác, nhưng cách hiện tại rõ ràng và phù hợp cho giai đoạn học.

### 5.5. Vai trò của SecurityConfig

`SecurityConfig` là nơi lắp các phần bảo mật lại với nhau:

- Tắt CSRF cho REST API dùng JWT stateless.
- Không tạo HTTP session bằng `SessionCreationPolicy.STATELESS`.
- Mở `/api/auth/login` để user có thể đăng nhập.
- Mở Swagger/OpenAPI.
- Yêu cầu mọi endpoint còn lại phải authenticated.
- Gắn handler JSON cho 401 và 403.
- Đặt JWT filter trước filter username/password mặc định.
- Tạo `PasswordEncoder` thành bean dùng chung.
- Bật `@PreAuthorize` bằng `@EnableMethodSecurity`.

### 5.6. Vì sao PasswordEncoder là `@Bean`?

```java
@Bean
public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
}
```

Nếu Service tự viết `new BCryptPasswordEncoder()`, Service bị gắn chặt với BCrypt và tự quản lý dependency. Khi tạo bean:

- Spring quản lý object dùng chung.
- Service chỉ phụ thuộc vào interface `PasswordEncoder`.
- Dễ thay thuật toán hoặc cấu hình strength.
- Dễ mock khi unit test.

### 5.7. Cấu hình JWT

`JwtService` đọc:

```properties
jwt.secret=${JWT_SECRET}
jwt.expiration=86400000
```

`expiration` tính bằng millisecond. `86400000` tương đương 24 giờ.

Secret không nên ghi trực tiếp trong Git. Database password cũng vậy. Khi triển khai thật, nên dùng biến môi trường:

```properties
spring.datasource.password=${DB_PASSWORD}
jwt.secret=${JWT_SECRET}
```

Tài liệu chi tiết riêng về JWT nằm trong `docs/JWT_AUTH_FLOW.md`.

---

## 6. Xử lý lỗi thống nhất

### 6.1. Vì sao cần ApiErrorResponse?

Nếu mỗi nơi trả lỗi theo một kiểu, frontend phải đoán cấu trúc response. `ApiErrorResponse` tạo một hợp đồng chung, ví dụ:

```json
{
  "timestamp": "2026-07-18T10:00:00Z",
  "status": 404,
  "error": "Not Found",
  "message": "Không tìm thấy sản phẩm",
  "path": "/api/products/999"
}
```

Client luôn biết cần đọc field nào, bất kể lỗi đến từ Product, User hay Category.

### 6.2. Lỗi nghiệp vụ đi qua GlobalExceptionHandler

```text
ServiceImpl
  -> throw ResourceNotFoundException
  -> GlobalExceptionHandler.handleResourceNotFound()
  -> ApiErrorResponse + HTTP 404
```

Các nhóm hiện tại:

| Trường hợp | Exception/Handler | HTTP |
|---|---|---:|
| Dữ liệu request sai nghiệp vụ | `BadRequestException` | 400 |
| DTO vi phạm annotation validation | `MethodArgumentNotValidException` | 400 |
| Không tìm thấy dữ liệu | `ResourceNotFoundException` | 404 |
| Lỗi chưa dự đoán | `Exception` | 500 |

Handler 500 không nên trả nguyên `ex.getMessage()` ra client vì có thể làm lộ cấu trúc database, query hoặc thông tin nội bộ. Chi tiết lỗi nên được ghi log phía server.

### 6.3. Vì sao 401 và 403 không chỉ để trong GlobalExceptionHandler?

Nhiều lỗi security xảy ra trong filter chain **trước khi request vào Controller**. Vì vậy `@RestControllerAdvice` không phải lúc nào cũng bắt được chúng.

Spring Security dùng hai điểm xử lý riêng:

- `JwtAuthenticationEntryPoint`: request chưa được xác thực, trả 401.
- `JwtAccessDeniedHandler`: đã xác thực nhưng thiếu role, trả 403.

Hai class vẫn dùng chung `ApiErrorResponse`, nên hình dạng JSON nhất quán dù lỗi đến từ hai pipeline khác nhau.

### 6.4. Phân biệt nhanh các mã lỗi

| Mã | Ý nghĩa trong project |
|---:|---|
| 400 | Client gửi dữ liệu sai hoặc vi phạm nghiệp vụ |
| 401 | Chưa đăng nhập, token thiếu/sai/hết hạn |
| 403 | Đã đăng nhập nhưng role không được phép |
| 404 | Không tìm thấy tài nguyên |
| 500 | Lỗi không mong đợi phía server |

---

## 7. Jackson và ObjectMapper

Jackson chuyển đổi hai chiều:

```text
JSON request -> Java object
Java object  -> JSON response
```

`ObjectMapper` là công cụ chính của Jackson. Controller thường được Spring tự chuyển response thành JSON. Nhưng `JwtAuthenticationEntryPoint` và `JwtAccessDeniedHandler` tự ghi response ngay trong security filter chain nên cần gọi:

```java
objectMapper.writeValue(response.getWriter(), errorResponse);
```

`JacksonConfig` tạo `ObjectMapper` bean để các security handler có thể inject qua constructor.

---

## 8. application.properties và pom.xml

### 8.1. application.properties

File này chứa cấu hình thay đổi theo môi trường:

- Tên ứng dụng.
- URL, username và password database.
- Cách Hibernate quản lý schema.
- Có hiển thị SQL hay không.
- Port server.
- JWT secret và thời gian hết hạn.

Không nên hard-code những giá trị này trong Java vì khi chuyển từ máy local sang server, ta chỉ cần đổi cấu hình hoặc biến môi trường, không phải build lại logic Java.

### 8.2. pom.xml

Maven đọc `pom.xml` để biết:

- Project dùng Java phiên bản nào.
- Cần tải những dependency nào.
- Cách compile, test và đóng gói ứng dụng.

Các nhóm dependency chính của project:

| Dependency | Vai trò |
|---|---|
| Spring Web MVC | Xây REST Controller và xử lý HTTP |
| Spring Data JPA | Repository, ORM và làm việc với database |
| Spring Security | Xác thực và phân quyền |
| Spring Validation | `@Valid`, `@NotBlank`, `@Min`... |
| MySQL Connector | Kết nối MySQL |
| JJWT | Tạo, ký, đọc và validate JWT |
| Springdoc OpenAPI | Swagger UI và OpenAPI JSON |

---

## 9. Thứ tự nên code một module mới

Thứ tự dưới đây giúp tránh tình trạng viết Controller trước nhưng chưa biết dữ liệu và nghiệp vụ phía dưới:

```text
1. Thiết kế bảng/quan hệ database
2. Entity
3. Repository
4. Request DTO
5. Response DTO
6. Service interface
7. ServiceImpl
8. Controller
9. Phân quyền
10. Test bằng Swagger/Bruno
11. Unit test
```

Giải thích:

1. Database cho biết dữ liệu cần lưu và quan hệ giữa các bảng.
2. Entity ánh xạ cấu trúc đó sang Java.
3. Repository cung cấp thao tác truy vấn cần thiết.
4. Request DTO giới hạn và validate dữ liệu đầu vào.
5. Response DTO thiết kế dữ liệu đầu ra an toàn, dễ dùng.
6. Service interface định nghĩa use case.
7. ServiceImpl kết nối validation nghiệp vụ, Repository và mapping.
8. Controller mở use case thành endpoint HTTP.
9. `@PreAuthorize` giới hạn role được gọi endpoint.
10. Test thủ công xác nhận toàn bộ luồng tích hợp.
11. Unit test giữ hành vi ổn định khi code tiếp tục thay đổi.

Đây là **thứ tự viết code**, không phải thứ tự chạy runtime. Runtime luôn đi từ request qua Security, Controller, Service rồi Repository.

---

## 10. Cách debug theo từng lớp

Khi API lỗi, đừng sửa ngẫu nhiên. Hãy xác định request dừng ở đâu.

### Không vào được Controller

Kiểm tra:

- URL và HTTP method có đúng không?
- Endpoint có bị Security chặn không?
- Header có đúng `Authorization: Bearer <token>` không?
- Token còn hạn không?
- User còn active không?
- Role có phù hợp `@PreAuthorize` không?

### Vào Controller nhưng Service không chạy đúng

Kiểm tra:

- Request DTO có nhận đúng JSON không?
- `@Valid` có báo lỗi field nào không?
- Controller có gọi đúng method Service không?
- Kiểu return giữa Controller, Service và ServiceImpl có đồng nhất không?

### Service chạy nhưng truy vấn sai

Kiểm tra:

- Tên method Repository có khớp field Entity không?
- JPQL đang dùng tên Entity/field Java hay nhầm sang tên bảng/cột SQL?
- ID khóa ngoại có tồn tại không?
- Query parameter null có được query xử lý không?
- `sortBy` có đúng tên field Entity không?

### Database đúng nhưng JSON trả ra sai

Kiểm tra:

- `mapToResponse()` đã map đủ field chưa?
- Constructor của Response DTO có đúng thứ tự tham số không?
- Getter có tồn tại và trả đúng kiểu không?
- Có vô tình trả Entity thay cho Response DTO không?

### API trả 500

Kiểm tra log server để tìm exception gốc. Response 500 cố ý không lộ chi tiết cho client, nên chỉ nhìn JSON trong Bruno thường chưa đủ để biết nguyên nhân.

---

## 11. Những nguyên tắc project đang áp dụng

### Single Responsibility

Mỗi lớp có một nhóm trách nhiệm chính:

- Controller xử lý HTTP.
- Service xử lý nghiệp vụ.
- Repository xử lý truy vấn.
- DTO mô tả dữ liệu vào/ra.
- Entity mô tả dữ liệu lưu trữ.
- Security xử lý xác thực/phân quyền.
- Exception handler chuẩn hóa lỗi.

### Dependency Injection

Các object phụ thuộc được Spring truyền qua constructor. Class không tự `new` Repository, Service hay PasswordEncoder.

### Encapsulation

Entity không được phơi trực tiếp hoàn toàn ra API. Request/Response DTO quyết định dữ liệu nào được nhập và xuất.

### Separation of Concerns

JWT parsing không nằm trong Controller. SQL không nằm trong Service. HTTP status không nằm trong Repository. Mỗi mối quan tâm có đúng vị trí của nó.

### Fail Fast

Service kiểm tra dữ liệu không hợp lệ trước khi save. Nếu category không tồn tại hoặc SKU bị trùng, code ném exception sớm với message rõ ràng.

---

## 12. Những phần hiện tại đã có

- CRUD cho các module chính.
- Repository với derived query và JPQL filtering.
- Validation bằng DTO request.
- DTO response cho User, Product, login, auth/me và lỗi.
- Phân trang, sắp xếp, tìm kiếm và lọc.
- Đăng nhập BCrypt.
- Tạo và validate JWT.
- JWT authentication filter.
- Kiểm tra `isActive` trên mỗi request.
- Chuyển Role thành authority.
- Phân quyền method bằng `@PreAuthorize`.
- JSON thống nhất cho 401 và 403.
- Global exception handling cho 400, 404 và 500.
- Swagger/OpenAPI và kiểm thử qua Bruno.

---

## 13. Những phần nên làm tiếp

Thứ tự nâng cấp hợp lý:

1. Hoàn thiện Response DTO cho Category, Supplier và Role để không trả Entity trực tiếp.
2. Chuẩn hóa exception của AuthService, tránh dùng xen kẽ `ResponseStatusException` và custom exception.
3. Validate `page`, `size`, `sortBy`, `sortDir` để tránh query lỗi hoặc size quá lớn.
4. Chuẩn hóa HTTP response/status cho POST, PUT và DELETE.
5. Thêm logging phía server, nhất là lỗi 500 và hành động quan trọng.
6. Viết unit test cho ServiceImpl bằng JUnit và Mockito.
7. Viết integration test cho Controller/Security.
8. Thêm JPA relationship hoặc projection để giảm N+1 query khi map ProductResponse.
9. Thêm auditing như `createdAt`, `updatedAt`.
10. Dùng Liquibase để quản lý phiên bản database.
11. Đưa database password và JWT secret sang biến môi trường.
12. Thiết kế refresh token nếu ứng dụng cần phiên đăng nhập dài.
13. Docker hóa backend và MySQL, sau đó cấu hình Nginx.

Không cần làm tất cả cùng lúc. Nên hoàn thiện một vòng dọc nhỏ: code, test, kiểm tra lỗi, rồi mới chuyển sang phần tiếp theo.

---

## 14. Checklist khi thêm hoặc sửa một API

```text
[ ] URL và HTTP method có đúng REST convention không?
[ ] Request có dùng DTO và @Valid không?
[ ] Response có tránh trả field nhạy cảm không?
[ ] Controller có mỏng không?
[ ] Nghiệp vụ có nằm trong ServiceImpl không?
[ ] Repository có dùng đúng tên field Entity không?
[ ] Không tìm thấy dữ liệu có trả 404 không?
[ ] Dữ liệu sai nghiệp vụ có trả 400 không?
[ ] Endpoint có cần @PreAuthorize không?
[ ] 401 và 403 có đúng trường hợp không?
[ ] List lớn có phân trang không?
[ ] Có nguy cơ N+1 query không?
[ ] Swagger/Bruno đã test trường hợp thành công chưa?
[ ] Đã test request sai, ID không tồn tại và role không đủ quyền chưa?
[ ] Có unit test cho nghiệp vụ quan trọng chưa?
```

---

## 15. Tóm tắt dễ nhớ

```text
Request DTO  = Client được gửi gì?
Response DTO = API được trả gì?
Entity       = Database lưu gì?
Controller   = Endpoint nhận gì và gọi ai?
Service      = Hệ thống có nghiệp vụ gì?
ServiceImpl  = Nghiệp vụ được thực hiện thế nào?
Repository   = Dữ liệu được truy vấn thế nào?
Security     = Ai đang gọi và họ có quyền không?
Exception    = Khi sai thì trả lỗi gì?
Config       = Các thành phần Spring được lắp với nhau thế nào?
```

Một request bình thường có thể nhớ bằng câu:

```text
Security kiểm tra người gọi
-> Controller nhận request
-> Service xử lý luật nghiệp vụ
-> Repository làm việc với database
-> Mapper tạo Response DTO
-> Jackson trả JSON cho client
```

Đó là xương sống của project Supermarket API hiện tại.
