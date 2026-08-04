# KẾ HOẠCH ĐÀO TẠO BACKEND
### Backend Training Plan — Dành cho Developer mới
*Internal — TAS Dev Team*

---

## Tổng quan

Tài liệu này mô tả lộ trình đào tạo Backend dành cho developer mới. Chương trình được thiết kế bao gồm **36 ngày kỹ thuật** (hoặc **43 ngày** nếu tính cả bài tập thực hành), từng bước xây dựng nền tảng vững chắc từ môi trường, ngôn ngữ, framework đến triển khai thực tế.

### Tổng thời gian

- **Phần kỹ thuật (Technical):** 36 ngày
- **Phần kỹ thuật + Bài tập (Technical + Exercise):** 43 ngày
- **Lưu ý:** 1 ngày = 8 giờ làm việc

---

## PHẦN 0 — Setup Git & Kết nối GitLab

### 0.1 Cấu hình thông tin Git (git config)

Sau khi cài Git, mở Git Bash (hoặc Terminal) và chạy hai lệnh sau:

```bash
# Thay thế bằng tên và email GitLab của bạn
git config --global user.name "Nguyen Van A"
git config --global user.email "nguyenvana@company.com"

# Kiểm tra lại
git config --global --list
```

### 0.2 Tạo Personal Access Token trên GitLab

Để Git có thể xác thực với GitLab mà không cần nhập mật khẩu mỗi lần, bạn cần tạo một Personal Access Token (PAT). Thực hiện trên trình duyệt:

- Truy cập GitLab (https://gitlab.tasolutions.com.vn) và đăng nhập bằng tài khoản của bạn.
- Nhấn vào ảnh đại diện góc trên bên phải → chọn **Edit profile** → menu trái chọn **Access Tokens**.
- Nhập tên token (ví dụ: `git-clone-token`), tích chọn Scopes: `read_repository` và `write_repository`, rồi nhấn **Create personal access token**.
- Sao chép token vừa tạo và lưu vào nơi an toàn. Token chỉ hiển thị một lần duy nhất.

> **Lưu ý:** Không chia sẻ Personal Access Token với bất kỳ ai. Nếu bị lộ, hãy thu hồi ngay trên GitLab và tạo token mới.

### 0.3 Clone repository về máy

Trên GitLab, vào repository cần clone, nhấn nút **Clone** và sao chép đường dẫn HTTPS/SSH. Sau đó mở Git Bash tại thư mục lưu code và chạy lệnh:

```bash
# Cú pháp chung
git clone https://gitlab.tasolutions.com.vn/<group>/<repo>.git

# Ví dụ thực tế
git clone https://gitlab.tasolutions.com.vn/tasolutions/foryourenterprises.git
```

- Khi được hỏi **Username**: nhập username GitLab.
- Khi được hỏi **Password**: dán Personal Access Token (không phải mật khẩu đăng nhập thông thường).

### 0.4 Lưu thông tin xác thực để không phải nhập lại

Chạy lệnh sau để Git tự động ghi nhớ token (chọn lệnh phù hợp với hệ điều hành):

```bash
# Windows
git config --global credential.helper manager

# macOS
git config --global credential.helper osxkeychain

# Linux
git config --global credential.helper store
```

### 0.5 Các lệnh Git cơ bản hay dùng (git pull)

Sau khi clone xong, dưới đây là các lệnh Git bạn sẽ dùng thường xuyên khi làm việc hàng ngày:

```bash
# Lấy code mới nhất từ remote (hay dùng nhất - chạy mỗi buổi sáng)
git pull

# Kiểm tra trạng thái file đã thay đổi
git status

# Xem danh sách branch và đang ở branch nào
git branch -a

# Chuyển sang branch khác (ví dụ: develop)
git checkout develop

# Xem lịch sử commit gần nhất
git log --oneline -10
```

> **Lưu ý:** Trước khi bắt đầu làm việc mỗi ngày, hãy luôn chạy `git pull` đầu tiên để đảm bảo bạn đang làm việc trên phiên bản code mới nhất.

**Các lệnh khác cần nắm:**
`add`, `commit`, `push`, `branch`, `stash`, `log`, `rebase`, `cherry-pick`, `pull`, `clone`, `checkout`

**Tài liệu tham khảo:**
- Git: https://git-scm.com/downloads
- Git Flow: https://guides.github.com/introduction/flow
- Video tham khảo: https://www.youtube.com/watch?v=RGOj5yH7evk

---

## PHẦN 1 — Nền tảng & Môi trường

### 1. Setup Environment (1 ngày)

- Cài đặt JDK 17, IDE (IntelliJ IDEA), Maven (v3.9.12)
- Cấu hình môi trường phát triển cơ bản

### 2. Maven (0.5 ngày)

- Quản lý dependency qua `pom.xml`
- Các lệnh cơ bản: `mvn clean install`, `mvn package`, `mvn test`

---

## PHẦN 2 — Java Core & OOP

### 3. Java OOP (3 ngày)

**4 tính chất OOP:**
- Kế thừa (Inheritance)
- Đa hình (Polymorphism)
- Đóng gói (Encapsulation)
- Trừu tượng (Abstraction)

**Các khái niệm:**
- Class, Interface, Abstract class
- Overloading vs Overriding

### 4. S.O.L.I.D Principles (1 ngày)

- **S** – Single Responsibility Principle
- **O** – Open/Closed Principle
- **L** – Liskov Substitution Principle
- **I** – Interface Segregation Principle
- **D** – Dependency Inversion Principle

### 5. Java Core và Collections (4 ngày)

- Kiểu dữ liệu, String, Exception Handling, Generics
- Stream API, Lambda Expression, Optional
- Collections Framework: List, Set, Map, Queue

Video tham khảo: https://www.youtube.com/watch?v=grEKMHGYyns

### 6. Design Patterns (2 ngày)

- **Creational:** Singleton, Factory
- **Structural:** Adapter, Decorator
- **Behavioral:** Observer
- **Miscellaneous:** DAO, Dependency Injection, MVC

---

## PHẦN 3 — Spring Framework

### 7. Spring Core + Spring Boot (3 ngày)

- IoC Container, Bean, ApplicationContext
- Dependency Injection (`@Autowired`, `@Component`, `@Service`, `@Repository`)
- Spring Boot Auto-configuration, `application.properties`/`yml`
- RESTful project setup với Spring Boot

### 8. REST API với Spring (3 ngày)

- HTTP Methods: GET, POST, PUT, DELETE, PATCH
- `@RestController`, `@RequestMapping`, `@PathVariable`, `@RequestBody`
- HTTP Status Codes, Response wrapping
- Pagination & Filtering, Exception Handling (`@ControllerAdvice`)
- Swagger / OpenAPI Documentation

### 9. Spring Security + JWT (3 ngày)

- Authentication (Xác thực) và Authorization (Phân quyền)
- SecurityFilterChain, UserDetailsService
- JWT: tạo token, validate token, refresh token
- Role-based Access Control

---

## PHẦN 4 — Database & JPA

### 10. MySQL (2 ngày)

- Thiết kế CSDL quan hệ, chuẩn hóa (Normal Form)
- SQL: SELECT, INSERT, UPDATE, DELETE, JOIN, GROUP BY, HAVING
- Index, Transaction, Foreign Key

### 11. Spring JPA + Audit + DB Migration với Liquibase (3 ngày)

- ORM: `@Entity`, `@Table`, `@Column`, `@OneToMany`, `@ManyToMany`
- Spring Data JPA: Repository, JpaRepository, JPQL, Native Query
- Entity Auditing: `@CreatedDate`, `@LastModifiedDate`
- Liquibase: quản lý phiên bản schema CSDL

---

## PHẦN 5 — Testing & Quality

### 12. Unit Test (3 ngày)

- JUnit 5: `@Test`, `@BeforeEach`, `@AfterEach`, Assertions
- Mockito: `@Mock`, `@InjectMocks`, `when().thenReturn()`
- Test Coverage, viết test cho Service layer và Repository layer

---

## PHẦN 6 — Build & Deployment

### 13. Build & Deployment với Docker (4.5 ngày)

- Dockerfile và cú pháp cơ bản (`FROM`, `RUN`, `COPY`, `EXPOSE`, `CMD`)
- Build Docker Image từ Dockerfile
- docker-compose: docker network, docker service, docker volume
- Mount volume từ container ra physical path
- Set Environment Variable qua docker-compose
- Setup Docker Environment cho dự án thực tế

### 14. Nginx (2 ngày)

- **Cấu hình Nginx làm Reverse Proxy**
  - Hiểu cách Nginx đứng trước backend (ví dụ Spring Boot), nhận request từ client và forward vào server nội bộ.
- **Load Balancing cơ bản**
  - Cấu hình upstream để phân phối request đến nhiều backend (round robin, least connections), giúp tăng khả năng chịu tải.
- **Làm bài tập nhỏ với Nginx**
  - Triển khai:
    - 1 backend: test reverse proxy
    - 2–3 backend: test load balancing
    - Quan sát response thay đổi giữa các server

---

## BÀI TẬP THỰC HÀNH — 7 ngày

Xây dựng phần Backend cho ứng dụng quản lý (ví dụ: Quản lý Nhân Viên, Thư Viện, Sinh viên,...)

### A. Yêu cầu kỹ thuật bắt buộc

- Thiết kế ERD trước khi bắt đầu viết code
- Authentication & Authorization (Spring Security + JWT)
- Pagination & Filtering
- Search API
- Role-based Access Control (RBAC)
- Logging & Exception Handling
- Swagger (API Documentation)
- Unit Test
- Áp dụng Clean Architecture
- Triển khai bằng Docker

### B. Tính năng tùy chọn

- Upload file

---

*— Hết tài liệu —*
