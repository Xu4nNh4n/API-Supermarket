# 🛡️ CẨM NANG GIẢI PHẪU 5 CLASS BẢO MẬT & XỬ LÝ LỖI TRONG SPRING SECURITY

> 🎯 **Mục tiêu**: Giải phẫu chi tiết mã nguồn 5 class cốt lõi trong gói `security/` và `config/`, hiểu cách `SecurityContextHolder` lưu trữ phiên làm việc của User trong ThreadLocal, xử lý dứt điểm các lỗi CORS/401/403 và cung cấp **Bộ Nhiệm Vụ Thực Hành (Project Quests)**!
> ⏱️ **Thời gian đọc**: ~18 phút.

---

## 📚 PHẦN 1: GIẢI PHÃ BẢN CHẤT LÝ THUYẾT (DEEP DIVE THEORY)

---

### 1. Phân Biệt Cốt Lõi: Authentication vs Authorization

```text
┌────────────────────────────────────────────────────────────────────────────────────────┐
│ 1. AUTHENTICATION (Xác thực - "BẠN LÀ AI?"):                                           │
│   ├── Kiểm tra định danh: Nhập username + password.                                    │
│   └── Đúng thông tin ──► Cấp JWT Token (Tương đương Căn cước công dân / Vé vào cổng).  │
├────────────────────────────────────────────────────────────────────────────────────────┤
│ 2. AUTHORIZATION (Phân quyền - "BẠN ĐƯỢC PHÉP LÀM GÌ?"):                               │
│   ├── Kiểm tra vai trò (Role): Sau khi đã có thẻ vào cổng.                            │
│   ├── Nếu Role = ROLE_ADMIN  ──► Được xóa sản phẩm, xem báo cáo doanh thu.             │
│   └── Nếu Role = ROLE_STAFF  ──► Chỉ được quét mã tính tiền, không được xóa dữ liệu.   │
└────────────────────────────────────────────────────────────────────────────────────────┘
```

---

### 2. Giải Phẫu Chi Tiết 5 Class Trọng Yếu Trong Dự Án

```text
com.api.supermarket/
├── config/
│   ├── SecurityConfig.java              ──► [BỘ NÃO CẤU HÌNH]: Điều phối toàn bộ luồng bảo mật
│   └── MdcLoggingFilter.java            ──► [BỘ GÁC CỔNG LOG]: Gắn traceId vào ThreadLocal
└── security/
    ├── JwtService.java                  ──► [MÁY IN & SOI TOKEN]: Tạo chữ ký số và giải mã JWT
    ├── JwtAuthenticationFilter.java     ──► [NGƯỜI SOI VÉ]: Đọc header Authorization ở cửa vào
    ├── JwtAuthenticationEntryPoint.java ──► [XỬ LÝ 401]: Trả JSON khi chưa đăng nhập / token hỏng
    └── JwtAccessDeniedHandler.java      ──► [XỬ LÝ 403]: Trả JSON khi sai quyền hạn (Role)
```

---

### 3. Chi Tiết Từng Class & Phương Thức Hoạt Động

#### 1️⃣ `JwtService.java` (Tiện ích Token)
* `generateAccessToken(UserDetails user)`: Đóng gói `username`, `roles` và hạn dùng 15 phút vào Payload, sau đó ký số bằng thuật toán `HMAC-SHA256` với `JWT_SECRET`.
* `extractUsername(String token)`: Đọc và giải mã Claims để lấy ra tên tài khoản.
* `isTokenValid(String token, UserDetails user)`: Kiểm tra 2 điều kiện: Chữ ký số khớp 100% VÀ token chưa vượt quá thời gian hết hạn (`exp`).

#### 2️⃣ `JwtAuthenticationFilter.java` (Trạm kiểm soát)
* Kế thừa `OncePerRequestFilter` (đảm bảo mỗi request chỉ chạy qua Filter này đúng 1 lần).
* Trích xuất token từ chuỗi `Bearer eyJhbGciOi...`
* Khi token hợp lệ $\rightarrow$ Nạp đối tượng `UsernamePasswordAuthenticationToken` vào **`SecurityContextHolder.getContext().setAuthentication(auth)`**.
* 👉 *Nhờ có bước này, tất cả các Controller và Service phía sau đều có thể gọi `SecurityContextHolder.getContext().getAuthentication().getName()` để biết ai đang thao tác!*

#### 3️⃣ `JwtAuthenticationEntryPoint.java` (Trạm chặn lỗi 401 Unauthorized)
* Được gọi khi: Client gọi API bảo vệ nhưng không gửi kèm Token hoặc Token bị sửa đổi / hết hạn.
* Hành động: Thay vì để Tomcat trả về trang HTML lỗi màu trắng, class này ghi thẳng chuỗi JSON `ApiErrorResponse` (HTTP 401) ra luồng `HttpServletResponse`.

#### 4️⃣ `JwtAccessDeniedHandler.java` (Trạm chặn lỗi 403 Forbidden)
* Được gọi khi: Client đã đăng nhập thành công (Token chuẩn), nhưng Role của User không đủ quyền (Ví dụ: `ROLE_STAFF` cố tình gọi API xóa tài khoản của Admin).
* Hành động: Trả về JSON `ApiErrorResponse` với mã **`403 Forbidden`**.

#### 5️⃣ `SecurityConfig.java` (Trung tâm điều phối)
* Cấu hình `csrf.disable()` (vì dùng JWT Stateless).
* Cấu hình `sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS)`.
* Đăng ký `JwtAuthenticationFilter` đứng trước `UsernamePasswordAuthenticationFilter`.

---

## 🚨 PHẦN 2: BẢNG BẮT BỆNH LỖI SECURITY THƯỜNG GẶP (TROUBLESHOOTING)

| Triệu chứng lỗi | Nguyên nhân cốt lõi | Cách xử lý dứt điểm |
| :--- | :--- | :--- |
| **Lỗi CORS (`Blocked by CORS policy`)** | Trình duyệt chặn request từ Frontend (port 3000/5173) gọi sang Backend (port 8080). | Thêm cấu hình `CorsConfiguration` trong `SecurityConfig` cho phép `allowedOrigins("http://localhost:5173")` và `allowedMethods("*")`. |
| **Gọi API công khai vẫn bị 401** | Quên cấu hình `.requestMatchers("/api/auth/**").permitAll()` trong `SecurityConfig`. | Thêm endpoint cần mở công khai vào danh sách `permitAll()`. |
| **Token còn hạn nhưng vẫn báo lỗi chữ ký (Signature)** | `JWT_SECRET` trên máy server bị thay đổi hoặc không đủ 256-bit (tối thiểu 32 ký tự). | Kiểm tra lại chuỗi bí mật trong file `.env` hoặc `application.properties`. |

---

## 🎮 PHẦN 3: BỘ NHIỆM VỤ THỰC HÀNH DỰ ÁN MỚI (SECURITY CLASSES QUESTS)

---

### ⚔️ QUEST 1: Viết `JwtAuthenticationEntryPoint` Chuẩn JSON
* **Nhiệm vụ**: Tạo class xử lý lỗi 401 trả về JSON đồng bộ với `ApiErrorResponse`.
* **Yêu cầu**:
  ```java
  @Component
  public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {
      @Override
      public void commence(HttpServletRequest request, HttpServletResponse response,
                           AuthenticationException authException) throws IOException {
          response.setContentType("application/json;charset=UTF-8");
          response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
          
          ApiErrorResponse error = new ApiErrorResponse(
              LocalDateTime.now(),
              401,
              "Unauthorized",
              "Bạn chưa đăng nhập hoặc token đã hết hạn!",
              (String) request.getAttribute("traceId"),
              request.getRequestURI()
          );
          response.getWriter().write(new ObjectMapper().writeValueAsString(error));
      }
  }
  ```

---

### ⚔️ QUEST 2: Viết `JwtAccessDeniedHandler` Cho Lỗi 403
* **Nhiệm vụ**: Tạo class xử lý lỗi thiếu quyền.
* **Yêu cầu**:
  1. Kế thừa `AccessDeniedHandler`.
  2. Trả về `HttpServletResponse.SC_FORBIDDEN` (403) kèm thông điệp "Bạn không có quyền thực hiện chức năng này!".

---

### ⚔️ QUEST 3: Ráp Nối Handlers Vào `SecurityConfig`
* **Nhiệm vụ**: Khai báo 2 Handler trên vào chuỗi lọc của Spring Security:
  ```java
  http.exceptionHandling(ex -> ex
      .authenticationEntryPoint(jwtAuthenticationEntryPoint)
      .accessDeniedHandler(jwtAccessDeniedHandler)
  );
  ```

---

## 🏆 BOSS QUEST: THỬ THÁCH BẮT ĐÚNG MÃ LỖI 401 VÀ 403

* 🧪 **Quy trình kiểm thử**:
  1. Gọi `GET /api/orders` không gửi Token $\rightarrow$ *Kỳ vọng: Nhận đúng JSON 401 từ `JwtAuthenticationEntryPoint`.*
  2. Đăng nhập bằng tài khoản `STAFF`, lấy Token gắn vào Header $\rightarrow$ Gọi `DELETE /api/users/1` (API của Admin) $\rightarrow$ *Kỳ vọng: Nhận đúng JSON 403 từ `JwtAccessDeniedHandler`!*
