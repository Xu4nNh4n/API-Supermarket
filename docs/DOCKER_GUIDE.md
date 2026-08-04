# DOCKER CHO SPRING BOOT VÀ MYSQL

Tài liệu này giải thích lý thuyết Docker và cách đóng gói SuperMarketAPI cùng MySQL thành
các container có thể chạy nhất quán trên nhiều môi trường.

---

## 1. Vấn đề Docker giải quyết

Khi chạy ứng dụng trực tiếp trên máy, mỗi máy có thể khác nhau:

```text
Phiên bản Java khác nhau.
Phiên bản MySQL khác nhau.
Biến môi trường khác nhau.
Cổng đang được sử dụng.
Cách cài đặt dependency khác nhau.
```

Docker đóng gói ứng dụng và môi trường chạy thành image.

```text
Source code + Java runtime + cấu hình chạy
                    |
                    v
               Docker Image
                    |
                    v
              Docker Container
```

Container giúp môi trường local, test và server gần giống nhau hơn.

---

## 2. Image và Container

### Docker Image

Image là bản mẫu chỉ đọc dùng để tạo container.

Ví dụ:

```text
mysql:8.4
eclipse-temurin:21-jre
supermarket-api:1.0
```

### Docker Container

Container là một instance đang chạy của image.

```text
Một image supermarket-api
        |
        +-- container backend-1
        +-- container backend-2
        +-- container backend-3
```

Xóa container không có nghĩa xóa image. Dữ liệu bên trong container có thể mất nếu không
được lưu bằng volume.

---

## 3. Container khác máy ảo thế nào?

Máy ảo thường chứa cả hệ điều hành khách hoàn chỉnh. Container dùng chung kernel của host
nhưng có filesystem, process và network riêng.

```text
Virtual Machine: nặng hơn, khởi động chậm hơn, cách ly mạnh.
Container      : nhẹ hơn, khởi động nhanh, phù hợp triển khai service.
```

Container không phải cơ chế bảo mật tuyệt đối. Vẫn cần cập nhật image, hạn chế quyền và bảo
vệ secret.

---

## 4. Dockerfile là gì?

`Dockerfile` là công thức dùng để build image.

Các lệnh phổ biến:

```text
FROM       -> chọn image nền.
WORKDIR    -> thư mục làm việc trong image.
COPY       -> sao chép file vào image.
RUN        -> chạy lệnh khi build image.
ENV        -> khai báo biến môi trường mặc định.
EXPOSE     -> mô tả cổng ứng dụng sử dụng.
ENTRYPOINT -> lệnh chạy khi container khởi động.
```

---

## 5. Build JAR trước khi chạy

Spring Boot thường được đóng gói thành file JAR:

```powershell
.\mvnw.cmd clean package
```

Nếu test thành công, file được tạo trong:

```text
target/supermarket-0.0.1-SNAPSHOT.jar
```

Chạy trực tiếp:

```powershell
java -jar target/supermarket-0.0.1-SNAPSHOT.jar
```

Docker runtime cuối cùng chỉ cần Java và file JAR, không cần source code hoặc Maven.

---

## 6. Multi-stage build

Multi-stage build chia Dockerfile thành hai giai đoạn:

```text
Giai đoạn build   -> JDK + Maven + source code -> tạo JAR.
Giai đoạn runtime -> JRE + JAR                 -> chạy ứng dụng.
```

Image runtime nhỏ hơn vì không chứa Maven, source và compiler.

Ví dụ `Dockerfile`:

```dockerfile
# Giai đoạn 1: build ứng dụng.
FROM eclipse-temurin:21-jdk AS builder

WORKDIR /app

COPY .mvn .mvn
COPY mvnw pom.xml ./

RUN chmod +x mvnw
RUN ./mvnw -B -DskipTests dependency:go-offline

COPY src src

RUN ./mvnw -B -DskipTests clean package

# Giai đoạn 2: chỉ giữ Java runtime và file JAR.
FROM eclipse-temurin:21-jre

WORKDIR /app

# Tạo user riêng, tránh chạy ứng dụng bằng root.
RUN useradd --system --create-home spring

COPY --from=builder /app/target/*.jar app.jar

USER spring

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
```

Tên/tag image nền chỉ là ví dụ. Trong dự án thật nên pin tag đã được đội ngũ kiểm tra.

---

## 7. Tại sao không chạy bằng root?

Process trong container mặc định có thể chạy bằng user root. Nếu ứng dụng bị khai thác,
quyền root làm tăng phạm vi ảnh hưởng bên trong container.

```dockerfile
RUN useradd --system --create-home spring
USER spring
```

Đây là một lớp giảm thiểu rủi ro, không thay thế các biện pháp bảo mật khác.

---

## 8. .dockerignore

Tạo file `.dockerignore` để không gửi file thừa vào build context:

```text
target/
.git/
.idea/
.vscode/
Bruno/
docs/
*.log
.env
```

Lợi ích:

- Build nhanh hơn.
- Image cache ổn định hơn.
- Giảm nguy cơ sao chép secret hoặc file không cần thiết.

Không đưa `.env` vào image.

---

## 9. Build Docker image

Tại thư mục có Dockerfile:

```powershell
docker build -t supermarket-api:1.0 .
```

Ý nghĩa:

```text
docker build          -> build image.
-t supermarket-api:1.0 -> đặt tên và tag.
.                     -> build context là thư mục hiện tại.
```

Kiểm tra:

```powershell
docker images
```

---

## 10. Chạy một container backend

Ví dụ:

```powershell
docker run --name supermarket-backend `
  -p 8080:8080 `
  -e DB_PASSWORD=your-password `
  -e JWT_SECRET=your-long-random-secret `
  supermarket-api:1.0
```

Ánh xạ cổng:

```text
localhost:8080 -> container:8080
```

Backend vẫn cần kết nối MySQL. Nếu MySQL chạy container khác, không dùng `localhost` trong
backend container để trỏ đến MySQL container.

---

## 11. localhost bên trong container

Trong backend container:

```text
localhost = chính backend container.
```

Nó không phải máy Windows và không phải MySQL container.

Nếu service MySQL trong Compose có tên `mysql`, URL phải là:

```text
jdbc:mysql://mysql:3306/supermarket_api
```

Docker DNS tự phân giải tên service `mysql` thành địa chỉ container MySQL.

---

## 12. Docker Network

Network cho phép container giao tiếp với nhau bằng tên service.

```text
supermarket-network
    |
    +-- backend:8080
    +-- mysql:3306
    +-- nginx:80
```

Không cần publish cổng MySQL ra host nếu chỉ backend cần truy cập. Khi học và dùng Workbench,
có thể publish `3307:3306` để tránh trùng MySQL local.

---

## 13. Docker Volume

Filesystem ghi được của container không nên dùng để lưu dữ liệu database lâu dài.

```text
Không có volume:
Xóa MySQL container -> có thể mất database.

Có volume:
Xóa và tạo lại container -> dữ liệu vẫn còn trong volume.
```

Ví dụ:

```yaml
volumes:
  mysql_data:
```

Gắn vào MySQL:

```yaml
volumes:
  - mysql_data:/var/lib/mysql
```

Volume không thay thế backup. Xóa volume vẫn có thể làm mất dữ liệu.

---

## 14. Docker Compose là gì?

Compose mô tả nhiều container trong một file YAML.

```text
docker-compose.yml
    |
    +-- backend
    +-- mysql
    +-- network
    +-- volume
    +-- environment variables
```

Thay vì chạy nhiều lệnh `docker run`, dùng:

```powershell
docker compose up -d
```

---

## 15. Ví dụ docker-compose.yml

```yaml
services:
  mysql:
    image: mysql:8.4
    container_name: supermarket-mysql
    restart: unless-stopped
    environment:
      MYSQL_DATABASE: supermarket_api
      MYSQL_ROOT_PASSWORD: ${DB_PASSWORD}
    volumes:
      - mysql_data:/var/lib/mysql
    ports:
      - "3307:3306"
    healthcheck:
      test:
        [
          "CMD",
          "mysqladmin",
          "ping",
          "-h",
          "localhost",
          "-p${DB_PASSWORD}"
        ]
      interval: 10s
      timeout: 5s
      retries: 10
    networks:
      - supermarket-network

  backend:
    build:
      context: .
      dockerfile: Dockerfile
    container_name: supermarket-backend
    restart: unless-stopped
    environment:
      SPRING_DATASOURCE_URL: >-
        jdbc:mysql://mysql:3306/supermarket_api?allowPublicKeyRetrieval=true&useSSL=false&serverTimezone=UTC
      SPRING_DATASOURCE_USERNAME: root
      DB_PASSWORD: ${DB_PASSWORD}
      JWT_SECRET: ${JWT_SECRET}
    ports:
      - "8080:8080"
    depends_on:
      mysql:
        condition: service_healthy
    networks:
      - supermarket-network

volumes:
  mysql_data:

networks:
  supermarket-network:
    driver: bridge
```

`depends_on` với healthcheck giúp backend chờ MySQL sẵn sàng nhận kết nối, thay vì chỉ chờ
container MySQL vừa được tạo.

---

## 16. File .env của Docker Compose

Tạo `.env` cạnh `docker-compose.yml`:

```dotenv
DB_PASSWORD=your-local-docker-password
JWT_SECRET=your-random-secret-at-least-32-bytes
```

Thêm `.env` vào `.gitignore`:

```gitignore
.env
```

Docker Compose tự đọc `.env` để thay `${DB_PASSWORD}` và `${JWT_SECRET}` trong file Compose.

Spring Boot không tự đọc `.env` khi chạy trực tiếp bằng Maven. Đây là hai cơ chế khác nhau.

Không commit secret thật lên Git.

---

## 17. Biến môi trường ghi đè application.properties

Spring Boot cho phép biến môi trường ghi đè property.

```text
spring.datasource.url
        |
SPRING_DATASOURCE_URL
```

Project đang dùng:

```properties
spring.datasource.password=${DB_PASSWORD}
jwt.secret=${JWT_SECRET}
```

Compose truyền:

```yaml
environment:
  DB_PASSWORD: ${DB_PASSWORD}
  JWT_SECRET: ${JWT_SECRET}
```

Không bake password/secret vào Dockerfile hoặc image.

---

## 18. Các lệnh Docker Compose

Khởi động và build:

```powershell
docker compose up -d --build
```

Xem container:

```powershell
docker compose ps
```

Xem log backend:

```powershell
docker compose logs -f backend
```

Xem log MySQL:

```powershell
docker compose logs -f mysql
```

Dừng và xóa container/network:

```powershell
docker compose down
```

Dừng và xóa cả volume dữ liệu:

```powershell
docker compose down -v
```

Lệnh `down -v` xóa dữ liệu MySQL trong volume. Chỉ dùng khi chắc chắn dữ liệu không cần giữ.

---

## 19. Image layer và cache

Mỗi lệnh phù hợp trong Dockerfile tạo một layer. Docker có thể tái sử dụng layer cũ.

Vì `pom.xml` thay đổi ít hơn source, nên copy dependency trước:

```dockerfile
COPY .mvn .mvn
COPY mvnw pom.xml ./
RUN ./mvnw -B -DskipTests dependency:go-offline

COPY src src
RUN ./mvnw -B -DskipTests clean package
```

Khi chỉ sửa Java source, Docker có thể giữ cache dependency Maven.

---

## 20. Có nên bỏ qua test khi build image?

Dockerfile ví dụ dùng:

```text
-DskipTests
```

Điều này giúp build image nhanh nhưng không có nghĩa được bỏ test trong quy trình triển khai.

Quy trình phù hợp:

```text
1. CI chạy clean test.
2. Test xanh.
3. Build Docker image với -DskipTests để không chạy lại.
```

Khi học local, có thể chạy test trước bằng Maven rồi mới build image.

---

## 21. Liquibase trong Docker

Khi backend container khởi động và Liquibase được bật:

```text
MySQL healthy
      |
Backend kết nối MySQL
      |
Liquibase chạy migration
      |
JPA khởi tạo
      |
API sẵn sàng
```

Không cần container Liquibase riêng ở mức cơ bản. Spring Boot có thể tự chạy migration.

Với nhiều backend instance, Liquibase dùng `DATABASECHANGELOGLOCK` để tránh chạy migration
đồng thời.

---

## 22. Healthcheck

Healthcheck xác định service có thực sự sẵn sàng hay chỉ có process đang chạy.

MySQL dùng:

```text
mysqladmin ping
```

Backend có thể dùng Spring Boot Actuator trong tương lai:

```text
GET /actuator/health
```

Không nên dùng endpoint nghiệp vụ phức tạp làm healthcheck.

---

## 23. Log trong Docker

Ứng dụng nên ghi log ra console:

```text
Spring Boot stdout
        |
Docker logging driver
        |
docker compose logs
```

Không log password, secret hoặc token chỉ vì container log được xem là nội bộ.

---

## 24. Các lỗi thường gặp

### Backend không kết nối được MySQL

Kiểm tra:

```text
URL có dùng host mysql thay vì localhost không?
MySQL đã healthy chưa?
Password hai service có giống nhau không?
Database name có đúng không?
```

### Port already allocated

Host đã có process dùng cổng 8080 hoặc 3307. Đổi phần bên trái:

```yaml
ports:
  - "8081:8080"
```

### Access denied for user

Volume MySQL cũ có thể đã được khởi tạo bằng password cũ. Đổi biến môi trường không tự đổi
password trong database đã tồn tại.

### Backend chạy trước MySQL

Thêm healthcheck và `depends_on.condition: service_healthy`.

### Không tìm thấy JAR

Kiểm tra build Maven thành công và pattern:

```dockerfile
COPY --from=builder /app/target/*.jar app.jar
```

### mvnw permission denied

Trong Linux build stage cần:

```dockerfile
RUN chmod +x mvnw
```

---

## 25. Bảo mật Docker cơ bản

- Chạy backend bằng non-root user.
- Không ghi secret trong Dockerfile.
- Không commit `.env`.
- Không publish cổng MySQL nếu không cần truy cập từ host.
- Pin image tag đã kiểm tra.
- Cập nhật image nền định kỳ.
- Không mount Docker socket vào container backend.
- Giới hạn quyền và tài nguyên khi triển khai thật.
- Dùng secret manager khi môi trường hỗ trợ.

---

## 26. Checklist Docker

- [ ] Toàn bộ Maven test chạy thành công.
- [ ] Build JAR thành công.
- [ ] Tạo `.dockerignore`.
- [ ] Tạo multi-stage Dockerfile.
- [ ] Image dùng Java 21.
- [ ] Runtime chạy bằng non-root user.
- [ ] Build image thành công.
- [ ] Tạo Compose cho backend và MySQL.
- [ ] Dùng service name `mysql` trong JDBC URL.
- [ ] Truyền `DB_PASSWORD` và `JWT_SECRET` qua environment.
- [ ] Tạo volume cho MySQL.
- [ ] Thêm MySQL healthcheck.
- [ ] Login/API hoạt động trong container.
- [ ] Dữ liệu còn sau khi tạo lại container.
- [ ] `.env` không được commit.

---

## 27. Thứ tự thực hành

```text
1. Chạy clean test trên máy.
2. Chạy JAR trực tiếp bằng java -jar.
3. Tạo .dockerignore.
4. Tạo Dockerfile.
5. Build và chạy backend image.
6. Tạo MySQL container.
7. Tạo docker-compose.yml.
8. Truyền biến môi trường.
9. Kiểm tra volume.
10. Kiểm tra Liquibase migration.
11. Thêm Nginx sau khi backend + MySQL đã ổn định.
```

