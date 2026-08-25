# 🐳 GIÁO TRÌNH DOCKER & DOCKER COMPOSE TOÀN DIỆN CHO SPRING BOOT & MYSQL

> 🎯 **Mục tiêu**: Hiểu trọn vẹn từ bản chất của Containerization, giải phẫu chi tiết từng dòng trong `Dockerfile` và `docker-compose.yml`, làm chủ kỹ thuật tối ưu dung lượng (Multi-stage build), xử lý các lỗi đụng cổng/mất kết nối và cung cấp **Bộ Nhiệm Vụ Thực Hành (Project Quests)** để tự tay đóng gói bất kỳ dự án Backend nào!
> ⏱️ **Thời gian đọc**: ~20 phút.

---

## 📚 PHẦN 1: GIẢI PHÃ BẢN CHẤT LÝ THUYẾT (DEEP DIVE THEORY)

---

### 1. Vấn Đề Cốt Lõi: "Nó Chạy Trên Máy Tôi, Nhưng Lại Sập Trên Máy Bạn!"

Khi bạn viết code trên máy tính cá nhân (máy Local), ứng dụng phụ thuộc vào hàng tá yếu tố môi trường:
* Phiên bản JDK (máy bạn dùng Java 21, server công ty lại dùng Java 17).
* Hệ điều hành (bạn dùng Windows đường dẫn `D:\Code`, server dùng Linux `/var/app`).
* Cấu hình MySQL (múi giờ Timezone, bảng mã ký tự `utf8mb4_unicode_ci`, tài khoản root).
* Cổng mạng (Port) bị xung đột với phần mềm khác.

```text
[ CÁCH LÀM CŨ: Giao mã nguồn thô ]
Developer ──► Gửi file source code .java / .jar ──► Server / Đồng nghiệp
                                                          │
                                                          ▼ (SẬP ỨNG DỤNG)
                                           "Thiếu JDK 21! Lệch múi giờ MySQL! Đụng Port 3306!"

[ CÁCH LÀM VỚI DOCKER: Đóng gói toàn bộ "Ngôi nhà" ]
Developer ──► Đóng gói: [ Code + JDK 21 + Linux Alpine + Cấu hình ] thành 1 Image ──► Server
                                                                                          │
                                                                                          ▼ (CHẠY 100% NHẤT QUÁN)
```

---

### 2. So Sánh Máy Ảo (Virtual Machine) vs Docker Container

| Tiêu chí | Máy ảo (VMware / VirtualBox) | Docker Container |
| :--- | :--- | :--- |
| **Cơ chế hoạt động** | Cài một Hệ điều hành (Guest OS) riêng biệt nặng hàng chục GB. | **Chia sẻ chung nhân Linux (Host OS Kernel)** $\rightarrow$ chỉ chứa những gì ứng dụng cần. |
| **Dung lượng** | Rất nặng: 10GB – 30GB mỗi máy ảo. | Siêu nhẹ: **150MB – 300MB**. |
| **Thời gian khởi động** | Mất **1 đến 3 phút** để nạp hệ điều hành. | Mất **1 đến 2 giây** là sẵn sàng phục vụ. |
| **Hiệu năng CPU / RAM** | Chiếm dụng RAM cố định (bật 3 máy ảo là đơ máy). | Dùng bao nhiêu tốn bấy nhiêu, giải phóng RAM tức thì. |

---

### 3. Bốn Trụ Cột Cốt Lõi Của Docker (Tương Đương Trong Java)

```text
1. Dockerfile      ──► "Code thiết kế Class" (File văn bản chứa các bước nấu Image).
2. Docker Image    ──► "Class / Bản vẽ thiết kế" (File nhị phân chỉ đọc, chứa Code + Môi trường).
3. Docker Container──► "Object Instance" (Một thực thể đang chạy được sinh ra từ Image).
4. Docker Volume   ──► "Ổ cứng gắn ngoài" (Lưu dữ liệu MySQL bền vững, xóa Container không mất dữ liệu).
```

---

### 4. Giải Phẫu Từng Dòng Trong `Dockerfile` Multi-Stage Build

Kỹ thuật **Multi-stage Build** chia quá trình đóng gói thành 2 giai đoạn (Builder $\rightarrow$ Runner) giúp giảm kích thước Image từ **900MB xuống còn ~180MB**:

```dockerfile
# =========================================================================
# GIAI ĐOẠN 1 (BUILDER): Dùng Maven để biên dịch source code thành file JAR
# =========================================================================
FROM maven:3.9.6-eclipse-temurin-21 AS builder

# Thiết lập thư mục làm việc bên trong container
WORKDIR /app

# Tận dụng cơ chế Caching Layer của Docker:
# Chỉ copy pom.xml và tải thư viện trước. Nếu bạn sửa code Java mà không đổi pom.xml,
# Docker sẽ bỏ qua bước tải thư viện này (tiết kiệm 5-10 phút build mỗi lần!).
COPY pom.xml .
RUN mvn dependency:go-offline

# Copy toàn bộ mã nguồn vào và đóng gói thành file .jar (bỏ qua chạy test để build nhanh)
COPY src ./src
RUN mvn clean package -DskipTests

# =========================================================================
# GIAI ĐOẠN 2 (RUNNER): Dùng môi trường JRE siêu nhẹ để chạy file JAR
# =========================================================================
# eclipse-temurin:21-jre-alpine là bản Linux Alpine tối giản, chỉ có Java Runtime (không có Maven thừa thãi)
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Copy DUY NHẤT file .jar đã đóng gói thành công từ Giai đoạn 1 (builder) sang
COPY --from=builder /app/target/*.jar app.jar

# Khai báo cổng ứng dụng sẽ lắng nghe (để tài liệu hóa)
EXPOSE 8080

# Lệnh cố định thực thi khi Container khởi động
ENTRYPOINT ["java", "-jar", "app.jar"]
```

> 💡 **Phân biệt `ENTRYPOINT` vs `CMD`**:
> * `ENTRYPOINT`: Là câu lệnh thực thi **bắt buộc và cố định** của container (Ví dụ: `java -jar app.jar`).
> * `CMD`: Là các tham số mặc định truyền vào cho `ENTRYPOINT`, có thể bị ghi đè khi chạy lệnh `docker run`.

---

### 5. Giải Phẫu File Điều Phối `docker-compose.yml`

File `docker-compose.yml` giúp bạn kích hoạt cả cụm **Backend + Database** chỉ bằng đúng 1 câu lệnh `docker compose up`:

```yaml
version: '3.8'

services:
  # -------------------------------------------------------------
  # Dịch vụ 1: Database MySQL
  # -------------------------------------------------------------
  mysql:
    image: mysql:8.0
    container_name: supermarket_mysql
    restart: always # Tự động bật lại nếu container bị sập
    environment:
      MYSQL_ROOT_PASSWORD: rootpassword # Mật khẩu tài khoản root
      MYSQL_DATABASE: supermarket_db    # Tự động tạo sẵn Database này khi bật lần đầu
      MYSQL_USER: super_user
      MYSQL_PASSWORD: super_password
    ports:
      # Map cổng: "Cổng máy thật (Host) : Cổng bên trong Container"
      - "3307:3306" # Mẹo: Dùng 3307 ở ngoài để không bị đụng với MySQL cài sẵn trên máy bạn!
    volumes:
      # Gắn Volume để dữ liệu bảng và hóa đơn không bị mất khi tắt container
      - mysql_data:/var/lib/mysql
    networks:
      - supermarket_network
    healthcheck:
      # Kiểm tra khi nào MySQL thực sự sẵn sàng nhận kết nối
      test: ["CMD", "mysqladmin", "ping", "-h", "localhost"]
      interval: 10s
      timeout: 5s
      retries: 5

  # -------------------------------------------------------------
  # Dịch vụ 2: Ứng dụng Spring Boot Backend
  # -------------------------------------------------------------
  backend:
    build: . # Tự động tìm file Dockerfile ở thư mục hiện tại để build Image
    container_name: supermarket_backend
    restart: on-failure
    ports:
      - "8080:8080"
    depends_on:
      mysql:
        condition: service_healthy # Chờ MySQL khởi động xong hẳn mới bật Backend (Chống lỗi Crash!)
    environment:
      # ĐIỂM QUAN TRỌNG NHẤT: Trong mạng Docker, các container gọi nhau bằng TÊN SERVICE ("mysql"),
      # TUYỆT ĐỐI KHÔNG dùng "localhost" (vì localhost lúc này là chính bản thân container backend)!
      SPRING_DATASOURCE_URL: jdbc:mysql://mysql:3306/supermarket_db?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true
      SPRING_DATASOURCE_USERNAME: root
      SPRING_DATASOURCE_PASSWORD: rootpassword
      JWT_SECRET: day_la_chuoi_bi_mat_jwt_256_bit_sieu_bao_mat_supermarket_api_2026_tas
    networks:
      - supermarket_network

# Khai báo Volume và Network dùng chung
volumes:
  mysql_data:

networks:
  supermarket_network:
    driver: bridge
```

---

## 🛠️ PHẦN 2: HAI CHẾ ĐỘ LÀM VIỆC VỚI DOCKER TRONG THỰC TẾ

```text
┌────────────────────────────────────────────────────────────────────────────────────────┐
│ 💻 CHẾ ĐỘ 1: CHỈ CHẠY MYSQL BẰNG DOCKER (Khuyên dùng 90% thời gian khi đang Code)      │
│   ├── Lệnh chạy: `docker compose up -d mysql`                                          │
│   ├── Bạn mở IntelliJ / VS Code lên và bấm nút "Run / Debug" bình thường.              │
│   └── 👉 Ưu điểm: Sửa code Java là ăn ngay, đặt Breakpoint Debug từng dòng, không tốn  │
│       thời gian 2 phút chờ build lại Docker Image mỗi khi sửa một dấu chấm dấu phẩy!   │
├────────────────────────────────────────────────────────────────────────────────────────┤
│ 🚀 CHẾ ĐỘ 2: ĐÓNG GÓI TOÀN BỘ CẢ APP + MYSQL (Khuyên dùng khi Nộp bài / Deploy)        │
│   ├── Lệnh chạy: `docker compose up -d --build`                                        │
│   └── 👉 Ưu điểm: Hệ thống chạy khép kín 100%, độc lập với máy chủ, đem sang bất kỳ    │
│       máy tính nào (Linux/Mac/Windows) cũng chạy mượt mà không cần cài đặt gì thêm!    │
└────────────────────────────────────────────────────────────────────────────────────────┘
```

---

## 🚨 PHẦN 3: BẢNG BẮT BỆNH LỖI DOCKER THƯỜNG GẶP (TROUBLESHOOTING)

| Triệu chứng lỗi | Nguyên nhân cốt lõi | Cách xử lý dứt điểm |
| :--- | :--- | :--- |
| **`Communications link failure / Connection refused`** | Backend bật lên quá nhanh khi MySQL bên cạnh chưa kịp khởi động xong. | Thêm khối `healthcheck` vào service mysql và `depends_on: mysql: condition: service_healthy`. |
| **`Communications link failure` (khi dùng `localhost`)** | Trong `application.properties`, bạn để `jdbc:mysql://localhost:3306`. Bên trong container, `localhost` trỏ vào chính nó chứ không trỏ sang MySQL! | Đổi `localhost` thành tên service: `jdbc:mysql://mysql:3306/supermarket_db`. |
| **`Bind for 0.0.0.0:3306 failed: port is already allocated`** | Cổng 3306 trên máy thật của bạn đang bị MySQL Workbench / XAMPP chiếm dụng. | Đổi cổng ngoài trong compose: `ports: - "3307:3306"` (gọi vào 3307). |
| **`Virtualization support not detected` (Windows)** | Máy tính chưa bật ảo hóa Virtualization (VT-x / AMD-V) trong BIOS. | Khởi động lại máy, nhấn phím `F2` hoặc `Delete` vào BIOS $\rightarrow$ Bật `Intel Virtualization` / `SVM Mode`. |

---

## 🎮 PHẦN 4: BỘ NHIỆM VỤ THỰC HÀNH DỰ ÁN MỚI (DOCKER QUESTS)

---

### ⚔️ QUEST 1: Viết `Dockerfile` Multi-stage Tối Ưu
* **Nhiệm vụ**: Tạo file `Dockerfile` ở thư mục gốc project.
* **Yêu cầu**:
  1. Giai đoạn 1 dùng `maven:3.9.6-eclipse-temurin-21` để build file jar.
  2. Áp dụng layer cache: copy `pom.xml` và chạy `dependency:go-offline` trước.
  3. Giai đoạn 2 dùng `eclipse-temurin:21-jre-alpine` chỉ copy file jar sang chạy.

---

### ⚔️ QUEST 2: Thiết Lập Mạng & Volume Trong `docker-compose.yml`
* **Nhiệm vụ**: Viết file compose điều phối Backend và MySQL.
* **Yêu cầu**:
  1. Cấu hình volume `mysql_data` để lưu trữ dữ liệu an toàn.
  2. Cấu hình `healthcheck` kiểm tra trạng thái sống của MySQL trước khi cho Backend kết nối.
  3. Truyền biến môi trường `SPRING_DATASOURCE_URL` trỏ vào host `mysql:3306`.

---

### ⚔️ QUEST 3: Thực Hành Bộ 5 Lệnh Quản Trị Container Thần Thánh
* **Nhiệm vụ**: Mở Terminal thực hiện lần lượt các lệnh:
  1. `docker compose up -d --build` (Build và chạy ngầm toàn bộ dịch vụ).
  2. `docker compose ps` (Xem danh sách các container đang chạy và trạng thái sức khỏe).
  3. `docker compose logs -f backend` (Xem log trực tiếp của Spring Boot).
  4. `docker compose down` (Dừng và xóa container nhưng vẫn giữ nguyên dữ liệu CSDL).
  5. `docker compose down -v` (Dừng và xóa sạch cả dữ liệu Volume để làm mới từ đầu).

---

## 🏆 BOSS QUEST: THỬ THÁCH TRIỂN KHAI ĐỘC LẬP HOÀN TOÀN

* 🧪 **Quy trình kiểm thử**:
  1. Tắt toàn bộ IDE (IntelliJ/VS Code) và tắt toàn bộ MySQL Server cài trên máy tính thật của bạn.
  2. Mở Terminal tại thư mục dự án và gõ: `docker compose up -d --build`.
  3. Mở trình duyệt hoặc Postman gọi: `GET http://localhost:8080/api/products`.
  4. 👉 *Kỳ vọng: API trả về HTTP 200 OK với danh sách sản phẩm đầy đủ, chứng minh toàn bộ hệ sinh thái Spring Boot + MySQL đang vận hành 100% tự động trong Docker!*
