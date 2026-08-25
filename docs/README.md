# 📚 THƯ VIỆN GIÁO TRÌNH & BỘ NHIỆM VỤ THỰC HÀNH BACKEND (SUPERMARKET API)

> *"Học lý thuyết để hiểu bản chất — Làm Quests thực hành để làm chủ kỹ năng trọn đời!"*

---

## 🗺️ BẠN ĐANG CẦN GÌ? (Tra Cứu Nhanh Theo Tình Huống & Bài Tập)

| 😵 Vấn đề bạn đang gặp | 📖 Giáo trình giải thích chi tiết | ⚔️ Bài tập thực hành (Quests) |
| :--- | :--- | :--- |
| **Mới bắt đầu**, muốn nắm bức tranh tổng thể | [01. Bản Đồ Tư Duy & Bí Kíp Toàn Diện](01_lo_trinh_va_kien_truc/01_GIAO_TRINH_BACKEND_TOAN_DIEN.md) | 5 Câu hỏi thử thách phỏng vấn |
| **Chuẩn bị tạo module mới**, chưa rõ quy trình | [Kiến Trúc & Quy Trình 6 Bước Chuẩn](01_lo_trinh_va_kien_truc/KIEN_TRUC_DU_AN_SUPERMARKET.md) | Quy trình Entity $\rightarrow$ DTO $\rightarrow$ Service $\rightarrow$ API |
| **Viết Service bị rối**, lỗi `mapToResponse`, stream | ⭐ [Cẩm Nang Sinh Tồn Tầng Service](05_nghiep_vu_va_dto/CAM_NANG_SINH_TON_TANG_SERVICE.md) | **3 Cấp độ Quests CRUD** + Bẫy null |
| **Không biết phân chia trường** Request vs Response | [Quy Tắc Thiết Kế DTO Chuyên Nghiệp](05_nghiep_vu_va_dto/QUY_TAC_THIET_KE_DTO.md) | **3 Quests DTO** + Boss Quest chống hack giá |
| **Tại sao Hóa đơn có Category**, giải mã DTO lồng nhau | [Giải Mã mapToResponse & Lồng DTO](05_nghiep_vu_va_dto/GIAI_MA_MAPTORESPONSE_VA_LONG_DTO.md) | **3 Quests Mapping** + Boss Quest constructor |
| **Quản lý Khách hàng**, tích điểm, tra cứu SĐT tại quầy | [Thiết Kế Module Khách Hàng (Customer)](05_nghiep_vu_va_dto/THIET_KE_MODULE_KHACH_HANG_CUSTOMER.md) | **3 Quests Customer** + Boss Quest check trùng |
| **Thiết kế luồng Bán hàng**, trừ kho, hoàn kho (5 Repo) | ⭐ [Thiết Kế Module Hóa Đơn & Bán Hàng](05_nghiep_vu_va_dto/THIET_KE_MODULE_HOA_DON_BAN_HANG.md) | **3 Quests Order** + Boss Quest hoàn kho |
| **Lỗi 401/403, JWT Token**, Refresh Token Rotation | [Hướng Dẫn Bảo Mật Spring Security & JWT](02_security_va_auth/HUONG_DAN_BAO_MAT_SPRING_SECURITY_VA_JWT.md) | **6 Quests Security** + Boss Quest phá hoại token |
| **Log bị lẫn lộn**, muốn truy vết lỗi bằng `traceId` | [Hướng Dẫn Logging SLF4J & MDC TraceID](03_logging_va_exception/HUONG_DAN_LOGGING_VA_MDC.md) | **4 Quests Logging** + Boss Quest truy vết lỗi |
| **Bắt lỗi hệ thống**, trả JSON ApiErrorResponse | [Hướng Dẫn Xử Lý Lỗi Global Exception](03_logging_va_exception/HUONG_DAN_XU_LY_LOI_EXCEPTION.md) | **4 Quests Exception** + Boss Quest bắt lỗi |
| **Thiết kế CSDL**, quan hệ 1-N, N-N, Index, DECIMAL | [Hướng Dẫn Thiết Kế CSDL MySQL & ERD](04_database_va_jpa/HUONG_DAN_CSDL_MYSQL.md) | **3 Quests Database** + Boss Quest EXPLAIN |
| **Quản lý phiên bản DB**, lỗi Checksum/Lock | [Hướng Dẫn Liquibase Migration](04_database_va_jpa/HUONG_DAN_LIQUIBASE.md) | **3 Quests Liquibase** + Boss Quest nâng cấp schema |
| **Giao dịch ACID**, `@Transactional`, JPA Auditing | [Hướng Dẫn Transaction & JPA Auditing](04_database_va_jpa/HUONG_DAN_TRANSACTION_VA_AUDITING.md) | **3 Quests Transaction** + Boss Quest giả lập mất điện |
| **Đóng gói Docker**, Multi-stage build, Compose | [Hướng Dẫn Docker & Docker Compose](06_devops_va_deployment/HUONG_DAN_DOCKER.md) | **3 Quests Docker** + Boss Quest chạy độc lập |
| **Nginx Reverse Proxy**, Cân bằng tải Load Balancing | [Hướng Dẫn Nginx & Cân Bằng Tải](06_devops_va_deployment/HUONG_DAN_NGINX.md) | **3 Quests Nginx** + Boss Quest kiểm chứng chia tải |

---

## 📂 MỤC LỤC CHI TIẾT THEO TỪNG THƯ MỤC CHUYÊN ĐỀ

### 📁 01. Lộ Trình & Kiến Trúc

* [01_GIAO_TRINH_BACKEND_TOAN_DIEN.md](01_lo_trinh_va_kien_truc/01_GIAO_TRINH_BACKEND_TOAN_DIEN.md) — Giáo trình tổng thể bao quát toàn bộ hệ thống (⏱️ 15 phút).
* [KIEN_TRUC_DU_AN_SUPERMARKET.md](01_lo_trinh_va_kien_truc/KIEN_TRUC_DU_AN_SUPERMARKET.md) — Bản đồ kiến trúc phân tầng & Quy trình 6 bước chuẩn (⏱️ 12 phút).
* [00_LO_TRINH_DAO_TAO_CONG_TY.md](01_lo_trinh_va_kien_truc/00_LO_TRINH_DAO_TAO_CONG_TY.md) — Kế hoạch đào tạo 43 ngày của công ty TAS (⏱️ 5 phút).
* [DANH_SACH_CONG_VIEC_CAN_LAM.md](01_lo_trinh_va_kien_truc/DANH_SACH_CONG_VIEC_CAN_LAM.md) — Checklist toàn bộ nhiệm vụ dự án (⏱️ 5 phút).

### 📁 02. Bảo Mật & Xác Thực (Security)

* [HUONG_DAN_BAO_MAT_SPRING_SECURITY_VA_JWT.md](02_security_va_auth/HUONG_DAN_BAO_MAT_SPRING_SECURITY_VA_JWT.md) — Luồng xác thực, Access/Refresh Token Rotation, BCrypt + **6 Quests** (⏱️ 15 phút).
* [HUONG_DAN_CHI_TIET_SPRING_SECURITY_VA_EXCEPTION.md](02_security_va_auth/HUONG_DAN_CHI_TIET_SPRING_SECURITY_VA_EXCEPTION.md) — Giải thích chi tiết 5 class Security trong code (⏱️ 10 phút).
* [HUONG_DAN_TAO_CHUOI_BI_MAT_JWT.md](02_security_va_auth/HUONG_DAN_TAO_CHUOI_BI_MAT_JWT.md) — Lệnh sinh JWT_SECRET 256-bit an toàn (⏱️ 2 phút).

### 📁 03. Logging & Xử Lý Ngoại Lệ (Exception)

* [HUONG_DAN_LOGGING_VA_MDC.md](03_logging_va_exception/HUONG_DAN_LOGGING_VA_MDC.md) — SLF4J, 4 cấp log, MDC traceId + **4 Quests** (⏱️ 12 phút).
* [HUONG_DAN_XU_LY_LOI_EXCEPTION.md](03_logging_va_exception/HUONG_DAN_XU_LY_LOI_EXCEPTION.md) — GlobalExceptionHandler, Custom Exceptions, HTTP Status Codes + **4 Quests** (⏱️ 12 phút).

### 📁 04. Cơ Sở Dữ Liệu, JPA & Liquibase

* [HUONG_DAN_CSDL_MYSQL.md](04_database_va_jpa/HUONG_DAN_CSDL_MYSQL.md) — Thiết kế ERD 3NF, Khóa ngoại, Indexing, DECIMAL + **3 Quests** (⏱️ 12 phút).
* [HUONG_DAN_TRANSACTION_VA_AUDITING.md](04_database_va_jpa/HUONG_DAN_TRANSACTION_VA_AUDITING.md) — Giao dịch ACID, Rollback tự động, JPA Auditing + **3 Quests** (⏱️ 12 phút).
* [HUONG_DAN_LIQUIBASE.md](04_database_va_jpa/HUONG_DAN_LIQUIBASE.md) — Database Migration bằng Code, MD5Sum Checksum + **3 Quests** (⏱️ 15 phút).

### 📁 05. Nghiệp Vụ & DTO ⭐

* [CAM_NANG_SINH_TON_TANG_SERVICE.md](05_nghiep_vu_va_dto/CAM_NANG_SINH_TON_TANG_SERVICE.md) — Biến số ít/nhiều (`order` vs `orders`), `stream().map()`, null-safety + **3 Cấp độ Quests** (⏱️ 15 phút).
* [QUY_TAC_THIET_KE_DTO.md](05_nghiep_vu_va_dto/QUY_TAC_THIET_KE_DTO.md) — Bộ 3 câu hỏi vàng phân loại DTO, Validation, chống hack giá + **3 Quests** (⏱️ 15 phút).
* [THIET_KE_MODULE_HOA_DON_BAN_HANG.md](05_nghiep_vu_va_dto/THIET_KE_MODULE_HOA_DON_BAN_HANG.md) — Giải mã 5 Repository, State Machine, trừ kho & hoàn kho + **3 Quests** (⏱️ 15 phút).
* [THIET_KE_MODULE_KHACH_HANG_CUSTOMER.md](05_nghiep_vu_va_dto/THIET_KE_MODULE_KHACH_HANG_CUSTOMER.md) — Quản lý khách hàng, tích điểm, tra cứu SĐT + **3 Quests** (⏱️ 10 phút).
* [GIAI_MA_MAPTORESPONSE_VA_LONG_DTO.md](05_nghiep_vu_va_dto/GIAI_MA_MAPTORESPONSE_VA_LONG_DTO.md) — Giải mã chuỗi domino Category trong Hóa đơn, MapStruct vs Thủ công + **3 Quests** (⏱️ 15 phút).

### 📁 06. DevOps & Triển Khai (Deployment)

* [HUONG_DAN_DOCKER.md](06_devops_va_deployment/HUONG_DAN_DOCKER.md) — Multi-stage build, Docker Compose, 2 chế độ làm việc + **3 Quests** (⏱️ 20 phút).
* [HUONG_DAN_NGINX.md](06_devops_va_deployment/HUONG_DAN_NGINX.md) — Reverse Proxy, 3 thuật toán Load Balancing, SSL Termination + **3 Quests** (⏱️ 15 phút).

---

### 💡 Lời Khuyên Cho Việc Học Tập & Ôn Luyện

1. **Đọc theo cặp**: Đọc phần **Lý thuyết** trong tài liệu $\rightarrow$ Đối chiếu ngay với file code tương ứng trong thư mục `src/`.
2. **Thực hành theo Quests**: Khi tự làm project mới, mở mục **Project Quests** của từng file ra và làm lần lượt từ Quest 1 đến Boss Quest.
3. **Thực hiện bài test Boss Quest**: Luôn dùng Postman/Bruno chạy bài test phá hoại để đảm bảo code của bạn không bao giờ bị sập trước dữ liệu sai của người dùng!
