# 🛒 SUPERMARKET MANAGEMENT RESTful API

[![Java](https://img.shields.io/badge/Java-21-orange.svg)](https://www.oracle.com/java/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.1.1-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![Security](https://img.shields.io/badge/Spring%20Security-JWT%20%2B%20Rotation-red.svg)](https://jwt.io/)
[![Database](https://img.shields.io/badge/MySQL-8.0-blue.svg)](https://www.mysql.com/)
[![Tests](https://img.shields.io/badge/Unit%20Tests-46%2F46%20Passed-success.svg)]()
[![License](https://img.shields.io/badge/License-MIT-green.svg)]()

> 🌟 **Hệ thống Backend Quản Lý Siêu Thị Toàn Diện** được xây dựng chuẩn kiến trúc Enterprise Layered Architecture với Spring Boot 4, Java 21, MySQL, JWT Stateless Authentication + Refresh Token Rotation, Liquibase Migration và hệ sinh thái Docker / Nginx.

---

## 🏛️ 1. TỔNG QUAN KIẾN TRÚC HỆ THỐNG (SYSTEM ARCHITECTURE)

```text
[ Client / Postman / Frontend ]
               │
               ▼ (HTTP Requests)
┌─────────────────────────────────────────────────────────────────┐
│                     MdcLoggingFilter (traceId)                  │
├─────────────────────────────────────────────────────────────────┤
│           JwtAuthenticationFilter (Bearer Token Verify)         │
├─────────────────────────────────────────────────────────────────┤
│            Controller Layer (@RestController / DTOs)            │
│   (Auth, Products, Categories, Suppliers, Customers, Orders)    │
├─────────────────────────────────────────────────────────────────┤
│      Service Layer (@Service / @Transactional / Business Rules) │
├─────────────────────────────────────────────────────────────────┤
│        Repository Layer (Spring Data JPA / Custom Queries)      │
├─────────────────────────────────────────────────────────────────┤
│          Database Layer (MySQL 8.0 / Liquibase Migration)       │
└─────────────────────────────────────────────────────────────────┘
```

---

## 🚀 2. CÔNG NGHỆ SỬ DỤNG (TECH STACK)

* **Ngôn ngữ lõi**: Java 21 (LTS)
* **Framework chính**: Spring Boot 4.1.1 (Spring MVC, Spring Data JPA, Spring Security, Validation, Actuator)
* **Xác thực & Bảo mật**:
  * JSON Web Token (jjwt 0.12.5) — Access Token (15 phút)
  * Refresh Token Rotation (7 ngày, băm SHA-256 lưu CSDL)
  * BCrypt Password Hashing
  * Role-Based Access Control (`ADMIN`, `STAFF`)
* **Cơ sở dữ liệu**: MySQL 8.0 + Liquibase Database Migration
* **Logging & Giám sát**: SLF4J + Logback + MDC ThreadLocal `traceId`
* **Xử lý lỗi**: `@RestControllerAdvice` + Bảng mã `ApiErrorResponse` chuẩn REST
* **Tài liệu API**: Springdoc OpenAPI / Swagger UI (`/swagger-ui.html`)
* **DevOps**: Docker Multi-stage Build + Docker Compose + Nginx Reverse Proxy / Load Balancing

---

## 📦 3. CÁC MODULE NGHIỆP VỤ CHÍNH

| STT | Module | Tính năng & Trách nhiệm |
| :---: | :--- | :--- |
| **1** | **Auth & Security** | Đăng nhập BCrypt, Cấp JWT Access/Refresh Token, Đổi Token (Rotation), Xem thông tin cá nhân (`/api/auth/me`). |
| **2** | **Sản phẩm (Products)** | CRUD sản phẩm, Tự động cảnh báo sắp hết hàng (`stockQuantity <= reorderPoint`), Lọc đa tiêu chí, Whitelist Sort. |
| **3** | **Danh mục & NCC** | Quản lý `Category` & `Supplier`, Chặn xóa khi có sản phẩm ràng buộc. |
| **4** | **Khách hàng (Customer)** | Quản lý thành viên, Tích điểm thưởng (`points`), Tra cứu nhanh tại quầy theo SĐT, Check trùng an toàn (`Objects.equals`). |
| **5** | **Hóa đơn (Sales Order)** | **Trái tim hệ thống**: Trừ kho tự động khi tạo đơn $\rightarrow$ Chống sửa giá (bốc giá từ DB) $\rightarrow$ Xác nhận thanh toán (`PAID`) $\rightarrow$ Hủy đơn **tự động hoàn trả kho** (`CANCELLED`). |
| **6** | **Người dùng (User & Role)** | Phân quyền nhân viên thu ngân (`STAFF`) và quản trị viên (`ADMIN`), Khóa tài khoản (`isActive`). |

---

## ⚡ 4. HƯỚNG DẪN CÀI ĐẶT & CHẠY ỨNG DỤNG

### Cách 1: Chạy trực tiếp trên máy (Local Run)

1. **Clone dự án & chuẩn bị CSDL**:

   ```powershell
   git clone https://github.com/Xu4nNh4n/API-Supermarket.git
   cd SuperMarketAPI
   ```

2. **Cấu hình biến môi trường** (hoặc sửa file `src/main/resources/application.yaml`):

   ```properties
   DB_URL=jdbc:mysql://localhost:3306/supermarket_db
   DB_USERNAME=root
   DB_PASSWORD=your_password
   JWT_SECRET=c3VwZXJtYXJrZXRfc2VjcmV0X2tleV8yNTZiaXRzX2V4YW1wbGVfMTIzNDU2Nzg5
   ```

3. **Biên dịch và Chạy**:

   ```powershell
   .\mvnw.cmd clean spring-boot:run
   ```

4. **Truy cập Swagger UI**: [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)

---

### Cách 2: Chạy đóng gói Docker Compose (1 Lệnh duy nhất)

```powershell
docker compose up -d --build
```

Hệ thống sẽ tự động khởi tạo Container **MySQL 8.0**, chạy migrate schema **Liquibase**, và khởi chạy **Backend API** trên cổng `8080`!

---

## 🧪 5. KIỂM THỬ (UNIT TESTS)

Dự án đạt độ phủ kiểm thử cao với **46 bài Unit Test** (Mockito + JUnit 5) pass 100%:

```powershell
.\mvnw.cmd test -Dtest=*ServiceImplTest
```

```text
[INFO] Running CategoryServiceImplTest    - Tests run: 7,  Failures: 0, Errors: 0
[INFO] Running CustomerServiceImplTest    - Tests run: 8,  Failures: 0, Errors: 0
[INFO] Running ProductServiceImplTest     - Tests run: 8,  Failures: 0, Errors: 0
[INFO] Running SalesOrderServiceImplTest  - Tests run: 6,  Failures: 0, Errors: 0
[INFO] Running SupplierServiceImplTest    - Tests run: 7,  Failures: 0, Errors: 0
[INFO] Running UserServiceImplTest        - Tests run: 10, Failures: 0, Errors: 0
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS - Total Tests: 46, Failures: 0, Errors: 0 (100% PASS)
```

---

## 📚 6. THƯ VIỆN GIÁO TRÌNH & BỘ BÀI TẬP THỰC HÀNH (DOCS)

Dự án đi kèm bộ **18 tài liệu kỹ thuật chuyên sâu** kèm **Bộ Nhiệm Vụ Thực Hành (Project Quests)** tại thư mục [`docs/`](docs/README.md):

* 🗺️ [01. Lộ Trình & Bản Đồ Tư Duy Toàn Diện](docs/01_lo_trinh_va_kien_truc/01_GIAO_TRINH_BACKEND_TOAN_DIEN.md)
* 📐 [02. Bản Đồ Kiến Trúc & Quy Trình 6 Bước Chuẩn](docs/01_lo_trinh_va_kien_truc/KIEN_TRUC_DU_AN_SUPERMARKET.md)
* 🛡️ [03. Hướng Dẫn Bảo Mật Spring Security & JWT](docs/02_security_va_auth/HUONG_DAN_BAO_MAT_SPRING_SECURITY_VA_JWT.md)
* 🪵 [04. Hướng Dẫn Logging SLF4J & MDC TraceID](docs/03_logging_va_exception/HUONG_DAN_LOGGING_VA_MDC.md)
* 🚨 [05. Hướng Dẫn Xử Lý Ngoại Lệ Global Exception](docs/03_logging_va_exception/HUONG_DAN_XU_LY_LOI_EXCEPTION.md)
* 🗄️ [06. Hướng Dẫn Thiết Kế CSDL MySQL & ERD](docs/04_database_va_jpa/HUONG_DAN_CSDL_MYSQL.md)
* 🔄 [07. Hướng Dẫn Transaction ACID & Auditing](docs/04_database_va_jpa/HUONG_DAN_TRANSACTION_VA_AUDITING.md)
* 🗃️ [08. Hướng Dẫn Liquibase Database Migration](docs/04_database_va_jpa/HUONG_DAN_LIQUIBASE.md)
* ⭐ [09. Cẩm Nang Sinh Tồn Tầng Service (Stream/Null-safety)](docs/05_nghiep_vu_va_dto/CAM_NANG_SINH_TON_TANG_SERVICE.md)
* 📋 [10. Quy Tắc Thiết Kế DTO Chống Gian Lận Giá](docs/05_nghiep_vu_va_dto/QUY_TAC_THIET_KE_DTO.md)
* 🧾 [11. Thiết Kế Module Hóa Đơn & Vòng Đời Bán Hàng (5 Repo)](docs/05_nghiep_vu_va_dto/THIET_KE_MODULE_HOA_DON_BAN_HANG.md)
* 👥 [12. Thiết Kế Module Khách Hàng & Tích Điểm (Customer)](docs/05_nghiep_vu_va_dto/THIET_KE_MODULE_KHACH_HANG_CUSTOMER.md)
* 🐳 [13. Hướng Dẫn Đóng Gói Docker & Docker Compose](docs/06_devops_va_deployment/HUONG_DAN_DOCKER.md)
* 🌐 [14. Hướng Dẫn Cấu Hình Nginx Reverse Proxy & Load Balancing](docs/06_devops_va_deployment/HUONG_DAN_NGINX.md)

---

## 👨‍💻 Tác giả

* **Repository**: [Xu4nNh4n/API-Supermarket](https://github.com/Xu4nNh4n/API-Supermarket)
* **Phiên bản**: `0.0.1-SNAPSHOT` (Production Ready)
