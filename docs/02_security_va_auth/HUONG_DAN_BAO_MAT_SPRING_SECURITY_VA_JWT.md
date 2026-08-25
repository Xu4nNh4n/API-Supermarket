# 🛡️ GIÁO TRÌNH BẢO MẬT TOÀN DIỆN: SPRING SECURITY, JWT & REFRESH TOKEN ROTATION

> 🎯 **Mục tiêu**: Nắm vững lý thuyết cốt lõi về bảo mật Backend (JWT, Filter Chain, BCrypt, Token Rotation) và cung cấp **Bộ Nhiệm Vụ Thực Hành (Project Quests)** để bạn có thể tự tay xây dựng lại toàn bộ hệ thống bảo mật này từ con số 0 trong bất kỳ dự án mới nào!

---

## 📚 PHẦN 1: GIẢI PHÃ BẢN CHẤT LÝ THUYẾT (DEEP DIVE THEORY)

---

### 1. Giải Phẫu Cấu Trúc Chuỗi JWT (JSON Web Token)

Một chuỗi JWT trông như thế này: `aaaaaa.bbbbbb.cccccc` (gồm 3 phần ngăn cách bởi dấu chấm `.`):

```text
[ Header (Đỏ) ]  .  [ Payload (Tím) ]  .  [ Signature (Xanh) ]
   Thuật toán &           Dữ liệu User           Chữ ký số bí mật
     Loại token          (username, roles)      (Chống giả mạo)
```

```text
1. Header    : {"alg": "HS256", "typ": "JWT"} ──► Mã hóa Base64Url
2. Payload   : {"sub": "admin", "role": "ROLE_ADMIN", "exp": 1771234567} ──► Mã hóa Base64Url
3. Signature : HMACSHA256( Base64(Header) + "." + Base64(Payload), SECRET_KEY )
```

#### ❓ Câu hỏi tư duy: *Payload chỉ là Base64 (ai cũng dịch ngược ra đọc được), vậy tại sao hacker không sửa `"role": "USER"` thành `"role": "ADMIN"`?*
> 💡 **Bản chất**: Nếu hacker sửa Payload $\rightarrow$ Chữ ký số `Signature` tính lại sẽ bị **lệch hoàn toàn** với chữ ký ban đầu. Muốn tạo chữ ký mới hợp lệ, hacker bắt buộc phải có `SECRET_KEY` (chuỗi bí mật chỉ nằm trên Server). Do đó, Server phát hiện bị giả mạo ngay lập tức và từ chối token!

---

### 2. Vòng Đời Request Đi Qua Security Filter Chain

Trong Spring Security, mọi request đều phải đi qua một **dây chuyền các trạm kiểm soát (Filter Chain)** trước khi chạm tới Controller:

```text
[ Client Request ]
       │  (Header: Authorization: Bearer <jwt_token>)
       ▼
[ MdcLoggingFilter ] ──► Gắn traceId vào ThreadLocal
       │
       ▼
[ JwtAuthenticationFilter ] ── (Trạm soi vé JWT)
       │
       ├── 1. Lấy chuỗi token từ header "Authorization"
       ├── 2. jwtService.isTokenValid(token): Kiểm tra chữ ký & ngày hết hạn
       ├── 3. jwtService.extractUsername(token): Lấy tên đăng nhập
       ├── 4. userDetailsService.loadUserByUsername(): Lấy thông tin & Quyền (Authorities)
       └── 5. Nạp Authentication vào: SecurityContextHolder.getContext().setAuthentication(...)
       │
       ▼
[ AuthorizationFilter ] ── (Trạm kiểm tra Quyền hạn)
       │
       ├── Có quyền (Khớp role ADMIN/STAFF) ──► Cho vào Controller
       ├── Chưa xác thực (Không token / token hỏng) ──► Ném vào: JwtAuthenticationEntryPoint (401)
       └── Thiếu quyền (Role USER đòi vào trang ADMIN) ──► Ném vào: JwtAccessDeniedHandler (403)
```

---

### 3. Cơ Chế Refresh Token Rotation (Đổi Vé Mới - Hủy Vé Cũ)

* **Access Token (JWT)**: Sống ngắn (**15 phút**), lưu trên RAM của Client. Dùng gọi API hàng ngày.
* **Refresh Token**: Sống dài (**7 ngày**), lưu an toàn ở Client (HttpOnly Cookie hoặc Secure Storage).

```text
[ QUY TRÌNH XOAY VÒNG TOKEN (ROTATION) ]

Client: POST /api/auth/refresh-token (Gửi Refresh Token cũ: "token-AAA")
                   │
                   ▼
       Server băm SHA-256("token-AAA") 
                   │
                   ▼
     Tìm trong bảng MySQL `refresh_tokens`
                   │
    ┌──────────────┴────────────────────────────────────────┐
    ▼                                                       ▼
[ TÌM THẤY & HỢP LỆ ]                               [ PHÁT HIỆN BỊ DÙNG LẠI / THU HỒI ]
  1. Đánh dấu token-AAA: is_revoked = true            1. Báo động bảo mật (Token Replay Attack)!
  2. Sinh Refresh Token MỚI ("token-BBB")             2. Thu hồi toàn bộ token của User này.
  3. Sinh Access Token MỚI                            3. Bắt buộc người dùng phải đăng nhập lại.
  4. Băm SHA-256("token-BBB") lưu vào DB
  5. Trả cặp token MỚI về cho Client
```

---

### 4. Tại Sao Phải Băm Mật Khẩu Bằng BCrypt?

* **One-way Hash (Băm 1 chiều)**: Không có thuật toán nào giải mã ngược từ chuỗi băm `$2a$10$...` về lại `123456`.
* **Salt ngẫu nhiên**: Mỗi lần băm, BCrypt tự sinh 1 chuỗi muối (Salt) ngẫu nhiên. Cùng mật khẩu `123456`, băm 10 lần ra 10 chuỗi khác nhau $\rightarrow$ Chống hoàn toàn hình thức tấn công dò từ điển (Rainbow Table).
* **So khớp mật khẩu**: Khi đăng nhập, ta dùng `passwordEncoder.matches(rawPassword, encodedPassword)` chứ không bao giờ so sánh bằng `equals()`.

---

## 🎮 PHẦN 2: BỘ NHIỆM VỤ THỰC HÀNH DỰ ÁN MỚI (PROJECT QUESTS)

> 🛠️ **Hướng dẫn sử dụng**: Khi bạn bắt đầu một project Backend mới từ con số 0, hãy làm lần lượt **6 Quest** dưới đây để tự tay dựng hoàn chỉnh hệ thống bảo mật cấp doanh nghiệp!

---

### ⚔️ QUEST 1: Thiết Kế Bảng CSDL & Entity
* **Mục tiêu**: Chuẩn bị bảng lưu User, Role và Refresh Token.
* **Nhiệm vụ cần làm**:
  1. Tạo bảng `roles` (`role_id`, `role_name`: ROLE_ADMIN, ROLE_USER).
  2. Tạo bảng `users` (`user_id`, `username`, `password_hash`, `role_id`, `is_active`).
  3. Tạo bảng `refresh_tokens` (`id`, `token_hash VARCHAR(64)`, `user_id`, `expiry_date`, `is_revoked`, `created_at`).
  4. Viết các file Entity Java tương ứng với `@ManyToOne` giữa `User` $\rightarrow$ `Role`, và `RefreshToken` $\rightarrow$ `User`.

---

### ⚔️ QUEST 2: Xây Dựng Tiện Ích `JwtService`
* **Mục tiêu**: Tạo class chuyên xử lý sinh và giải mã Token.
* **Nhiệm vụ cần làm**:
  1. Khai báo `JWT_SECRET` (256-bit) và `EXPIRATION_TIME` (15 phút) trong `application.properties`.
  2. Viết hàm `generateAccessToken(UserDetails user)`: Đính kèm `username` và danh sách `roles` vào Claims.
  3. Viết hàm `extractUsername(String token)`: Đọc subject từ Token.
  4. Viết hàm `isTokenValid(String token, UserDetails user)`: Kiểm tra đúng chữ ký và chưa hết hạn.

---

### ⚔️ QUEST 3: Xây Dựng `JwtAuthenticationFilter` (Trạm Soi Vé)
* **Mục tiêu**: Đứng trước mọi request để kiểm tra token.
* **Nhiệm vụ cần làm**:
  1. Kế thừa `OncePerRequestFilter`.
  2. Đọc header `Authorization`. Nếu không bắt đầu bằng `"Bearer "` $\rightarrow$ Cho request đi tiếp (`filterChain.doFilter`).
  3. Cắt chuỗi lấy JWT $\rightarrow$ Rút `username` $\rightarrow$ Nạp User từ Database.
  4. Nếu token hợp lệ $\rightarrow$ Tạo `UsernamePasswordAuthenticationToken` và gán vào `SecurityContextHolder.getContext().setAuthentication(...)`.

---

### ⚔️ QUEST 4: Xử Lý Ngoại Lệ Bảo Mật (401 & 403 Handlers)
* **Mục tiêu**: Trả về JSON lỗi đẹp mắt thay vì trang trắng HTML mặc định của Tomcat.
* **Nhiệm vụ cần làm**:
  1. Tạo `JwtAuthenticationEntryPoint implements AuthenticationEntryPoint`: Bắt lỗi 401 Unauthorized $\rightarrow$ ghi JSON `{"status": 401, "message": "Chưa đăng nhập hoặc token hết hạn"}` ra `response.getWriter()`.
  2. Tạo `JwtAccessDeniedHandler implements AccessDeniedHandler`: Bắt lỗi 403 Forbidden $\rightarrow$ ghi JSON `{"status": 403, "message": "Bạn không có quyền thực hiện thao tác này"}`.

---

### ⚔️ QUEST 5: Cấu Hình `SecurityConfig` (Trái Tim Hệ Thống)
* **Mục tiêu**: Ráp tất cả các thành phần lại với nhau.
* **Nhiệm vụ cần làm**:
  1. Tắt CSRF (`csrf.disable()`) vì dùng JWT (Stateless).
  2. Đặt `SessionCreationPolicy.STATELESS` (không dùng HttpSession).
  3. Phân quyền Endpoint:
     * `.requestMatchers("/api/auth/**").permitAll()` (Đăng nhập, đăng ký cho qua).
     * `.requestMatchers("/api/admin/**").hasRole("ADMIN")` (Chỉ Admin).
     * `.anyRequest().authenticated()` (Các API còn lại phải đăng nhập).
  4. Gắn `JwtAuthenticationFilter` đứng trước `UsernamePasswordAuthenticationFilter`.
  5. Đăng ký Bean `PasswordEncoder = new BCryptPasswordEncoder()`.

---

### ⚔️ QUEST 6: Xử Lý Logic Đăng Nhập & Xoay Vòng Refresh Token
* **Mục tiêu**: Hoàn thành API Login, Refresh Token và Logout.
* **Nhiệm vụ cần làm**:
  1. Viết `login(LoginRequest)`: So khớp password qua BCrypt $\rightarrow$ Sinh Access Token + Refresh Token $\rightarrow$ Băm SHA-256 Refresh Token lưu vào MySQL.
  2. Viết `refreshToken(RefreshTokenRequest)`: Băm chuỗi gửi lên $\rightarrow$ Tìm trong DB $\rightarrow$ Thu hồi token cũ (`isRevoked = true`) $\rightarrow$ Cấp phát cặp token mới.
  3. Viết `logout(LogoutRequest)`: Tìm token trong DB $\rightarrow$ Gán `isRevoked = true` để vô hiệu hóa vĩnh viễn.

---

## 🏆 BOSS QUEST: THỬ THÁCH THỰC CHIẾN TRÊN POSTMAN / BRUNO

Sau khi hoàn thành 6 Quest trên, hãy thực hiện **3 bài test thử nghiệm phá hoại** để kiểm chứng độ vững chắc của hệ thống:

* 🧪 **Test 1 (Giả mạo Token)**: Lấy Access Token hợp lệ, dùng tool sửa đổi 1 ký tự bất kỳ ở phần giữa rồi gửi lên $\rightarrow$ *Kết quả mong đợi: Bắn lỗi 401 Unauthorized tức thì.*
* 🧪 **Test 2 (Token hết hạn)**: Chờ qua 15 phút hoặc chỉnh thời gian hết hạn còn 10 giây $\rightarrow$ Gửi request $\rightarrow$ *Kết quả mong đợi: 401 Unauthorized.*
* 🧪 **Test 3 (Tấn công Replay Token)**: Dùng Refresh Token cũ đã bị xoay vòng (đã `is_revoked = true`) để xin token mới lần nữa $\rightarrow$ *Kết quả mong đợi: Hệ thống phát hiện gian lận và từ chối cấp token!*

---

*Lưu tài liệu này làm cẩm nang chuẩn. Bất kỳ khi nào làm dự án mới, chỉ cần mở Quest 1 đến Quest 6 ra làm theo từng bước là bạn sẽ có ngay một hệ thống bảo mật hoàn hảo!*
