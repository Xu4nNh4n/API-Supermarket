# LOGGING TRONG SPRING BOOT

Tài liệu này giải thích cách ghi log trong ứng dụng backend, cách chọn thông tin cần log và
cách tránh làm lộ dữ liệu nhạy cảm.

---

## 1. Logging là gì?

Logging là quá trình ứng dụng ghi lại những sự kiện quan trọng trong lúc chạy.

Ví dụ:

```text
Ứng dụng khởi động.
Người dùng đăng nhập thất bại.
Một Product được tạo.
Database bị mất kết nối.
Một request phát sinh lỗi 500.
```

Log giúp trả lời các câu hỏi:

```text
Lỗi xảy ra lúc nào?
Request nào gây lỗi?
Lỗi xảy ra trong method nào?
Người dùng nào thực hiện thao tác?
Ứng dụng đã chạy đến bước nào trước khi lỗi?
```

Log không phải dữ liệu trả về cho client. Client nhận API response ngắn gọn, còn server
giữ stack trace chi tiết trong log.

---

## 2. Luồng log trong backend

```text
Client gửi request
        |
Controller nhận request
        |
Service xử lý nghiệp vụ
        |
Repository truy cập database
        |
Thành công hoặc exception
        |
Logger ghi sự kiện ra console/file/hệ thống thu thập log
```

Response và log có mục đích khác nhau:

```text
Response 500: "Internal server error"
Server log  : stack trace, exception type, path, thời điểm, request id
```

Không gửi stack trace cho client vì có thể làm lộ cấu trúc code, SQL hoặc thông tin hệ
thống.

---

## 3. SLF4J và Logback

Spring Boot thường sử dụng:

```text
SLF4J  -> API mà code Java gọi.
Logback -> công cụ thực sự ghi log.
```

Code chỉ phụ thuộc vào SLF4J:

```java
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
```

Ví dụ:

```java
@Service
public class ProductServiceImpl implements ProductService {

    private static final Logger log =
        LoggerFactory.getLogger(ProductServiceImpl.class);
}
```

Nếu dùng Lombok, có thể sử dụng:

```java
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class ProductServiceImpl {
}
```

Khi đang học, khai báo `Logger` thủ công giúp nhìn rõ cơ chế hơn.

---

## 4. Các mức log

Thứ tự từ chi tiết nhất đến nghiêm trọng nhất:

```text
TRACE
DEBUG
INFO
WARN
ERROR
```

### TRACE

Dùng cho thông tin cực kỳ chi tiết của luồng nội bộ. Thường chỉ bật khi điều tra lỗi khó.

```java
log.trace("Bắt đầu map Product entity sang ProductResponse");
```

### DEBUG

Dùng cho thông tin hỗ trợ developer kiểm tra chương trình.

```java
log.debug("Đang tìm Product với id={}", productId);
```

Không nên bật DEBUG quá rộng trên production vì log có thể rất nhiều.

### INFO

Dùng cho sự kiện nghiệp vụ hoặc vòng đời quan trọng nhưng bình thường.

```java
log.info("Tạo Product thành công, productId={}, sku={}", id, sku);
```

### WARN

Dùng khi có tình huống bất thường nhưng hệ thống vẫn tiếp tục hoạt động.

```java
log.warn("Đăng nhập thất bại, username={}", username);
```

### ERROR

Dùng cho lỗi làm request hoặc chức năng thất bại.

```java
log.error("Không thể xử lý request path={}", path, exception);
```

Truyền exception ở tham số cuối giúp logger ghi đầy đủ stack trace.

---

## 5. Parameterized logging

Nên dùng placeholder `{}`:

```java
log.info("Đã tạo Supplier id={}, email={}", supplierId, maskedEmail);
```

Không nên nối chuỗi:

```java
log.debug("Supplier id=" + supplierId);
```

Placeholder giúp code rõ hơn và tránh tạo chuỗi không cần thiết khi mức log đang tắt.

---

## 6. Những gì nên log

### Lỗi 500

Phải ghi exception và stack trace ở server:

```java
log.error(
    "Unhandled exception, method={}, path={}",
    request.getMethod(),
    request.getRequestURI(),
    ex
);
```

### Login thất bại

Có thể log username và nguyên nhân chung:

```java
log.warn("Đăng nhập thất bại, username={}", request.getUsername());
```

Không log password và không cần phân biệt công khai username sai hay password sai.

### Thao tác thay đổi dữ liệu

Nên log định danh của dữ liệu:

```text
Tạo Product: productId, skuCode.
Cập nhật Product: productId.
Xóa Supplier: supplierId.
Khóa User: userId.
Thu hồi Refresh Token: userId, refreshTokenId.
```

Không cần log toàn bộ request DTO.

### Lỗi tích hợp hoặc database

Log tên hệ thống/tác vụ và exception:

```java
log.error("Không thể kết nối database khi tạo Product", ex);
```

---

## 7. Những gì không được log

Không log:

```text
Password hoặc password hash.
JWT secret.
Database password.
Toàn bộ access token.
Toàn bộ refresh token.
Cookie xác thực.
Authorization header.
Thông tin cá nhân không cần thiết.
```

Ví dụ tuyệt đối không làm:

```java
log.info("Login request={}", request);
log.debug("JWT={}", accessToken);
log.debug("Authorization={}", request.getHeader("Authorization"));
```

DTO `LoginRequest` có thể tự sinh `toString()` chứa password. Vì vậy log toàn bộ object là
nguy hiểm.

---

## 8. Log token an toàn

Thông thường không cần log token. Nếu bắt buộc phải đối chiếu trong lúc điều tra, chỉ log
một fingerprint không thể dùng để xác thực.

Ví dụ:

```text
Không log raw refresh token.
Có thể log refreshTokenId trong database.
Có thể log vài ký tự đầu của token hash nếu thật sự cần.
```

Ưu tiên log định danh bản ghi:

```java
log.info(
    "Đã thu hồi refresh token, refreshTokenId={}, userId={}",
    token.getRefreshTokenId(),
    token.getUser().getUserId()
);
```

---

## 9. Logging trong GlobalExceptionHandler

Response lỗi 500 hiện nên giữ message chung:

```json
{
  "status": 500,
  "error": "Internal Server Error",
  "message": "Internal server error"
}
```

Nhưng server phải log exception thật:

```java
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log =
        LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleGeneralException(
            Exception ex,
            HttpServletRequest request
    ) {
        log.error(
            "Unhandled exception, method={}, path={}",
            request.getMethod(),
            request.getRequestURI(),
            ex
        );

        // Trả response chung cho client.
    }
}
```

Với lỗi nghiệp vụ 400/404, có thể dùng `WARN` hoặc không log nếu lỗi đã quá phổ biến. Không
nên ghi mọi lỗi validation thành `ERROR` vì client nhập sai không có nghĩa server hỏng.

---

## 10. Logging trong AuthService

Ví dụ tư duy:

```java
public LoginResponse login(LoginRequest request) {
    User user = userRepository.findByUserName(request.getUsername())
        .orElseThrow(() -> {
            log.warn(
                "Đăng nhập thất bại, username={}",
                request.getUsername()
            );
            return new UnauthorizedException(
                "Tên đăng nhập hoặc mật khẩu không đúng"
            );
        });

    // Không log request.getPassword().
}
```

Login thành công có thể log username/userId, nhưng cần cân nhắc chính sách dữ liệu:

```java
log.info("Đăng nhập thành công, userId={}", user.getUserId());
```

---

## 11. Logging CRUD

Ví dụ tạo Product:

```java
Product savedProduct = productRepository.save(product);

log.info(
    "Tạo Product thành công, productId={}, sku={}",
    savedProduct.getProductId(),
    savedProduct.getSkuCode()
);
```

Ví dụ xóa Supplier:

```java
supplierRepository.delete(supplier);

log.info("Xóa Supplier thành công, supplierId={}", id);
```

Log sau khi thao tác thành công phản ánh đúng kết quả hơn log trước khi gọi Repository.
Nếu method có transaction, commit có thể xảy ra sau khi method kết thúc; với nghiệp vụ quan
trọng có thể cần cơ chế audit chuyên biệt thay vì chỉ dựa vào log text.

---

## 12. Correlation ID và Request ID

Khi nhiều request chạy đồng thời, các dòng log có thể xen kẽ. Request ID giúp nhóm log của
cùng một request.

```text
Client request
    requestId=8f2a...
        |
Controller log requestId=8f2a...
        |
Service log requestId=8f2a...
        |
Exception log requestId=8f2a...
```

Có thể dùng MDC:

```java
import org.slf4j.MDC;

MDC.put("requestId", requestId);
try {
    filterChain.doFilter(request, response);
} finally {
    MDC.remove("requestId");
}
```

Phải xóa MDC trong `finally` vì thread có thể được tái sử dụng cho request khác.

Đây là phần nâng cao, nên làm sau khi logging cơ bản ổn định.

---

## 13. Cấu hình mức log

Trong `application.properties`:

```properties
logging.level.root=INFO
logging.level.com.api.supermarket=DEBUG
logging.level.org.hibernate.SQL=INFO
```

Production thường nên hạn chế DEBUG:

```properties
logging.level.root=INFO
logging.level.com.api.supermarket=INFO
logging.level.org.hibernate.SQL=WARN
```

Không bật log bind parameter SQL trên production nếu dữ liệu truy vấn có thể chứa thông tin
nhạy cảm.

---

## 14. Cấu hình file log

Có thể cấu hình đơn giản:

```properties
logging.file.name=logs/supermarket-api.log
```

Khi chạy bằng Docker, thường nên ghi log ra `stdout/stderr` để Docker hoặc hệ thống logging
thu thập. Không nhất thiết ghi file bên trong container.

```text
Ứng dụng -> stdout -> Docker logs -> hệ thống thu thập log
```

---

## 15. Log rotation

Nếu ghi log ra file, cần giới hạn kích thước và thời gian giữ log. Nếu không, log có thể
làm đầy ổ đĩa.

Khái niệm cần nắm:

```text
Max file size.
Số ngày lưu log.
Tổng dung lượng log.
Nén file log cũ.
```

Với hệ thống thực tế, có thể dùng `logback-spring.xml` để cấu hình rolling policy.

---

## 16. Logging khác Audit Log

Application log phục vụ vận hành và debug:

```text
Request lỗi.
Database timeout.
Ứng dụng khởi động.
```

Audit log phục vụ theo dõi hành động nghiệp vụ:

```text
Ai thay đổi giá Product?
Giá cũ và giá mới là bao nhiêu?
Ai khóa tài khoản?
Thao tác xảy ra lúc nào?
```

Audit log quan trọng có thể cần lưu trong bảng database riêng và không nên chỉ dựa vào file
log có thể bị xoay vòng hoặc xóa.

---

## 17. Kiểm thử logging

Không cần assert mọi dòng log trong unit test. Ưu tiên kiểm tra hành vi nghiệp vụ.

Các điểm nên kiểm tra thủ công/integration:

```text
Lỗi 500 có stack trace ở server.
Response 500 không chứa stack trace.
Login sai không log password.
Log không chứa Authorization header.
CRUD quan trọng có log id của bản ghi.
```

Có thể dùng công cụ bắt log trong test khi một yêu cầu logging là bắt buộc, nhưng không nên
làm test phụ thuộc quá nhiều vào câu chữ của log.

---

## 18. Checklist Logging

- [ ] Dùng SLF4J thay vì `System.out.println()`.
- [ ] Log stack trace cho lỗi 500.
- [ ] Response không trả stack trace.
- [ ] Login thất bại không log password.
- [ ] Không log JWT secret/access token/refresh token.
- [ ] Log ID cho thao tác create/update/delete quan trọng.
- [ ] Chọn đúng INFO/WARN/ERROR.
- [ ] Cấu hình mức log riêng cho local và production.
- [ ] Có chiến lược log rotation hoặc thu thập stdout.
- [ ] Cân nhắc request ID/MDC.

---

## 19. Thứ tự áp dụng vào SuperMarketAPI

```text
1. Thêm Logger vào GlobalExceptionHandler.
2. Ghi stack trace cho nhánh lỗi 500.
3. Thêm log login thất bại mà không log password.
4. Thêm log create/update/delete cho Product, User, Supplier.
5. Thêm log thu hồi Refresh Token bằng ID, không log token.
6. Cấu hình mức log local/production.
7. Kiểm tra log thủ công.
8. Sau đó mới nghiên cứu MDC và hệ thống tập trung log.
```

