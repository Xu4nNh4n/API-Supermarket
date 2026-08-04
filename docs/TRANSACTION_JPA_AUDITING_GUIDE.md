# TRANSACTION VÀ JPA AUDITING TRONG SPRING BOOT

Tài liệu này giải thích hai nội dung:

1. Transaction dùng để bảo đảm nhiều thao tác database thành công hoặc thất bại cùng nhau.
2. JPA Auditing dùng để tự động quản lý thời điểm tạo và cập nhật Entity.

Hai nội dung có liên quan đến JPA nhưng giải quyết hai vấn đề khác nhau.

---

## 1. Transaction là gì?

Transaction là một nhóm thao tác database được xem như một đơn vị công việc duy nhất.

Ví dụ khi refresh token:

```text
1. Kiểm tra refresh token cũ.
2. Đánh dấu token cũ đã bị thu hồi.
3. Tạo refresh token mới.
4. Lưu refresh token mới.
```

Yêu cầu của nghiệp vụ:

```text
Tất cả bước thành công -> COMMIT.
Có một bước thất bại   -> ROLLBACK toàn bộ.
```

Nếu không có transaction, token cũ có thể đã bị thu hồi nhưng token mới lại chưa được
lưu. Người dùng sẽ bị mất phiên đăng nhập dù request refresh thất bại.

### Luồng hoạt động

```text
Service method được gọi
        |
Spring mở transaction
        |
Thực hiện các câu INSERT/UPDATE/DELETE
        |
Không có lỗi ----------------> COMMIT
        |
Có RuntimeException ---------> ROLLBACK
```

---

## 2. Sử dụng @Transactional

Import annotation của Spring:

```java
import org.springframework.transaction.annotation.Transactional;
```

Không nhầm với các annotation transaction thuộc package khác.

Ví dụ:

```java
@Service
public class RefreshTokenServiceImpl implements RefreshTokenService {

    @Override
    @Transactional
    public TokenResponse refresh(String rawToken) {
        // Thu hồi token cũ.
        // Tạo và lưu token mới.
        // Nếu có RuntimeException, cả hai thay đổi được rollback.
    }
}
```

### Nên đặt ở đâu?

Nên đặt `@Transactional` tại tầng Service vì Service chứa nghiệp vụ và điều phối nhiều
Repository.

```text
Controller -> nhận request, trả response.
Service    -> quản lý nghiệp vụ và transaction.
Repository -> thực hiện truy vấn database.
```

Không nên đặt transaction ở Controller vì Controller không nên sở hữu nghiệp vụ.

---

## 3. Transaction cho thao tác đọc

Với phương thức chỉ đọc, có thể sử dụng:

```java
@Transactional(readOnly = true)
public ProductResponse getProductById(Long id) {
    // Chỉ đọc dữ liệu.
}
```

`readOnly = true` mô tả rõ ý định của method và có thể giúp JPA tối ưu một số hoạt động
theo dõi thay đổi.

Không dùng `readOnly = true` cho method cần lưu, sửa hoặc xóa dữ liệu.

---

## 4. Khi nào transaction rollback?

Mặc định Spring rollback khi method ném:

```text
RuntimeException
Error
```

Các exception trong project như sau đều kế thừa `RuntimeException`:

```text
BadRequestException
ResourceNotFoundException
UnauthorizedException
```

Do đó chúng có thể kích hoạt rollback.

Với checked exception, cần cấu hình rõ:

```java
@Transactional(rollbackFor = Exception.class)
```

Không nên thêm `rollbackFor = Exception.class` vào mọi method nếu chưa thật sự cần.

---

## 5. Dirty checking

Trong transaction, Entity lấy từ Repository đang ở trạng thái managed.

Ví dụ:

```java
@Transactional
public void revoke(String rawToken) {
    RefreshToken token = refreshTokenRepository
        .findByTokenHash(hashToken(rawToken))
        .orElseThrow();

    token.setRevoked(true);
    token.setRevokedAt(Instant.now());
}
```

Dù không gọi `save(token)`, Hibernate vẫn phát hiện Entity đã thay đổi và chạy `UPDATE`
khi transaction commit. Cơ chế này gọi là dirty checking.

Gọi `save()` vẫn có thể được giữ lại để code dễ hiểu hơn, nhưng không bắt buộc với một
managed Entity trong transaction.

---

## 6. Transaction và quan hệ LAZY

`RefreshToken.user` đang sử dụng:

```java
@ManyToOne(fetch = FetchType.LAZY)
private User user;
```

`LAZY` nghĩa là User chưa chắc được tải ngay khi lấy RefreshToken. Khi code gọi:

```java
storedToken.getUser()
```

Hibernate có thể cần chạy thêm query. Transaction giữ persistence context còn hoạt động,
nhờ đó quan hệ LAZY có thể được tải trong Service.

Điều này đặc biệt quan trọng khi project đang cấu hình:

```properties
spring.jpa.open-in-view=false
```

Không nên dựa vào việc Controller truy cập quan hệ LAZY sau khi Service đã kết thúc.

---

## 7. Các lưu ý về @Transactional

### Method phải được gọi qua Spring Bean

Spring áp dụng transaction thông qua proxy. Một method trong cùng class gọi trực tiếp một
method `@Transactional` khác có thể không đi qua proxy.

```java
public void methodA() {
    methodB(); // self-invocation
}

@Transactional
public void methodB() {
}
```

Nếu `methodA()` đã có transaction thì `methodB()` vẫn tham gia transaction đang tồn tại.
Vấn đề xảy ra khi chỉ `methodB()` có annotation nhưng nó được gọi nội bộ từ method không có
transaction.

### Không nuốt exception

Ví dụ không nên làm:

```java
@Transactional
public void updateData() {
    try {
        repository.save(...);
    } catch (RuntimeException ex) {
        // Không throw lại.
    }
}
```

Spring không thấy exception thoát khỏi method nên có thể vẫn commit transaction.

---

## 8. JPA Auditing là gì?

JPA Auditing tự động gán các thông tin theo dõi Entity, phổ biến nhất là:

```text
createdAt -> thời điểm bản ghi được tạo.
updatedAt -> thời điểm bản ghi được cập nhật gần nhất.
```

Thay vì viết thủ công ở mọi Service:

```java
entity.setCreatedAt(Instant.now());
entity.setUpdatedAt(Instant.now());
```

Spring Data JPA có thể tự làm việc này.

---

## 9. Bật JPA Auditing

Tạo file:

```text
src/main/java/com/api/supermarket/config/JpaAuditingConfig.java
```

```java
package com.api.supermarket.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@Configuration
@EnableJpaAuditing
public class JpaAuditingConfig {
}
```

`@EnableJpaAuditing` kích hoạt cơ chế auditing trong Spring Data JPA.

---

## 10. Các annotation auditing

### @CreatedDate

Tự gán thời điểm Entity được insert lần đầu:

```java
@CreatedDate
@Column(name = "created_at", nullable = false, updatable = false)
private Instant createdAt;
```

### @LastModifiedDate

Tự gán lại thời điểm mỗi khi Entity được update:

```java
@LastModifiedDate
@Column(name = "updated_at")
private Instant updatedAt;
```

### @EntityListeners

Kết nối Entity với listener của auditing:

```java
@EntityListeners(AuditingEntityListener.class)
```

Nếu thiếu listener, `@CreatedDate` và `@LastModifiedDate` sẽ không tự hoạt động.

---

## 11. Ví dụ Entity sử dụng Auditing

```java
@Entity
@Table(name = "products")
@EntityListeners(AuditingEntityListener.class)
public class Product {

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private Instant updatedAt;
}
```

Luồng tạo mới:

```text
productRepository.save(product)
        |
AuditingEntityListener nhận sự kiện tạo Entity
        |
createdAt được gán
updatedAt được gán nếu cấu hình hỗ trợ
        |
Hibernate chạy INSERT
```

Luồng cập nhật:

```text
Thay đổi Product
        |
AuditingEntityListener nhận sự kiện update
        |
updatedAt được gán thời điểm mới
        |
Hibernate chạy UPDATE
```

---

## 12. Tạo BaseEntity để tránh lặp code

Nếu nhiều Entity đều có `createdAt` và `updatedAt`, có thể tạo:

```text
src/main/java/com/api/supermarket/entity/BaseEntity.java
```

```java
package com.api.supermarket.entity;

import java.time.Instant;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;

@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class BaseEntity {

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private Instant updatedAt;

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
```

Entity kế thừa:

```java
public class Product extends BaseEntity {
}
```

Chỉ dùng BaseEntity khi các bảng tương ứng đều có đủ cột `created_at` và `updated_at`.
Không nên ép mọi Entity kế thừa nếu schema của chúng không giống nhau.

---

## 13. JPA Auditing và timestamp do MySQL tự sinh

Project hiện có các field dạng:

```java
@Column(name = "created_at", insertable = false, updatable = false)
private LocalDateTime createAt;
```

Cấu hình này nói với Hibernate:

```text
Không gửi created_at khi INSERT.
Không gửi created_at khi UPDATE.
Database chịu trách nhiệm tạo giá trị.
```

Trong khi JPA Auditing cần Hibernate gửi giá trị `createdAt` khi insert. Vì vậy hai hướng
này xung đột nếu cùng áp dụng cho một field.

### Hướng A: Database quản lý timestamp

```sql
created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
```

Entity giữ `insertable = false`, `updatable = false`.

### Hướng B: JPA Auditing quản lý timestamp

```java
@CreatedDate
@Column(name = "created_at", nullable = false, updatable = false)
private Instant createdAt;
```

Không đặt `insertable = false` vì Hibernate cần insert giá trị.

Nên chọn một nguồn quản lý chính cho mỗi field. Nếu mục tiêu học phần này là JPA Auditing,
nên chọn hướng B và cập nhật Entity/schema một cách thống nhất.

---

## 14. Instant và LocalDateTime

### Instant

- Đại diện cho một thời điểm tuyệt đối theo UTC.
- Phù hợp cho token, log và hệ thống có nhiều múi giờ.
- RefreshToken hiện đang sử dụng `Instant`.

### LocalDateTime

- Không chứa thông tin múi giờ.
- Dễ đọc theo thời gian địa phương nhưng có thể mơ hồ khi triển khai nhiều khu vực.
- Các Entity cũ trong project đang dùng `LocalDateTime`.

Nên thống nhất một kiểu thời gian trong project. Với backend API, `Instant` thường là lựa
chọn rõ ràng hơn; client có thể chuyển UTC sang múi giờ hiển thị.

---

## 15. Áp dụng vào Refresh Token

`RefreshToken` có ba field thời gian khác nhau:

```text
createdAt -> thời điểm token được tạo.
expiresAt -> thời điểm token hết hạn.
revokedAt -> thời điểm token bị thu hồi.
```

Chỉ `createdAt` phù hợp để dùng `@CreatedDate`.

`expiresAt` là dữ liệu nghiệp vụ, phải được Service tính:

```java
token.setExpiresAt(
    Instant.now().plusMillis(refreshTokenExpiration)
);
```

`revokedAt` cũng là dữ liệu nghiệp vụ, phải được Service gán khi logout hoặc rotation:

```java
token.setRevoked(true);
token.setRevokedAt(Instant.now());
```

Không dùng `@LastModifiedDate` thay cho `revokedAt`, vì một token có thể được update vì lý
do khác ngoài thu hồi.

---

## 16. Ví dụ rollback trong Refresh Token

```java
@Transactional
public TokenResponse refresh(String rawToken) {
    RefreshToken oldToken = findAndValidate(rawToken);

    oldToken.setRevoked(true);
    oldToken.setRevokedAt(Instant.now());

    String newRefreshToken = createRefreshToken(oldToken.getUser());

    // Nếu tạo token mới lỗi, RuntimeException thoát khỏi method.
    // Transaction rollback nên oldToken không bị thu hồi trong database.

    String accessToken = jwtService.generateToken(oldToken.getUser());

    return new TokenResponse(
        accessToken,
        newRefreshToken,
        "Bearer",
        accessTokenExpiration
    );
}
```

Đây là ví dụ rõ nhất về lý do Refresh Token cần transaction.

---

## 17. Kiểm thử Transaction

Mục tiêu là chứng minh rollback thực sự xảy ra.

Ví dụ kịch bản:

```text
1. Lưu refresh token cũ với revoked=false.
2. Bắt đầu nghiệp vụ refresh.
3. Cho bước tạo token mới ném RuntimeException.
4. Đọc lại token cũ từ database.
5. Xác nhận revoked vẫn là false.
```

Không thể kiểm tra rollback chính xác chỉ bằng Mockito unit test vì Mockito không chạy
transaction/database thật. Nên dùng integration test với H2 hoặc database test riêng.

---

## 18. Kiểm thử JPA Auditing

### Kiểm tra createdAt

```text
1. Tạo Entity mà không set createdAt.
2. saveAndFlush().
3. clear EntityManager.
4. Đọc lại Entity.
5. createdAt phải khác null.
```

### Kiểm tra updatedAt

```text
1. Tạo Entity.
2. Lưu giá trị updatedAt ban đầu.
3. Thay đổi một field.
4. saveAndFlush().
5. Đọc lại Entity.
6. updatedAt mới phải sau updatedAt cũ.
```

`flush()` buộc Hibernate gửi SQL xuống database. `clear()` xóa cache để lần đọc sau lấy
dữ liệu thật từ database.

---

## 19. Checklist thực hành

### Transaction

- [ ] Hiểu COMMIT và ROLLBACK.
- [ ] Biết import `@Transactional` của Spring.
- [ ] Áp dụng transaction tại Service.
- [ ] Áp dụng cho `createRefreshToken`, `refresh` và `revoke`.
- [ ] Hiểu dirty checking.
- [ ] Hiểu ảnh hưởng của transaction với quan hệ LAZY.
- [ ] Viết integration test chứng minh rollback.

### JPA Auditing

- [ ] Tạo `JpaAuditingConfig`.
- [ ] Thêm `@EnableJpaAuditing`.
- [ ] Chọn database timestamp hoặc JPA Auditing làm nguồn quản lý.
- [ ] Thêm `@EntityListeners(AuditingEntityListener.class)`.
- [ ] Thêm `@CreatedDate`.
- [ ] Thêm `@LastModifiedDate` cho Entity có nhu cầu.
- [ ] Thống nhất `Instant` hoặc `LocalDateTime`.
- [ ] Viết integration test cho `createdAt` và `updatedAt`.

---

## 20. Thứ tự học và áp dụng trong project

```text
1. Hoàn thiện và kiểm thử Refresh Token.
2. Quan sát @Transactional trong refresh/revoke.
3. Viết test rollback cho refresh rotation.
4. Chọn chiến lược quản lý created_at/updated_at.
5. Bật JPA Auditing.
6. Thử Auditing trên một Entity trước.
7. Viết integration test.
8. Khi ổn định mới chuẩn hóa các Entity còn lại.
9. Dùng Liquibase để lưu thay đổi schema.
```

Không nên sửa toàn bộ Entity sang Auditing trong một lần. Làm một Entity, chạy test và
hiểu luồng trước sẽ giúp tránh lỗi schema trên diện rộng.

