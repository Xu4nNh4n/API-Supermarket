# NGINX CHO SPRING BOOT BACKEND

Tài liệu này giải thích cách dùng Nginx làm reverse proxy đứng trước Spring Boot, cách kết
hợp Nginx với Docker và nguyên lý load balancing nhiều backend instance.

---

## 1. Nginx là gì?

Nginx là web server và reverse proxy hiệu năng cao. Trong kiến trúc backend, Nginx thường
đứng trước ứng dụng Spring Boot.

```text
Client
  |
  v
Nginx :80/:443
  |
  v
Spring Boot :8080
  |
  v
MySQL :3306
```

Client chỉ biết địa chỉ Nginx. Spring Boot và MySQL có thể nằm trong network nội bộ.

---

## 2. Forward proxy và Reverse proxy

### Forward proxy

Forward proxy đại diện cho client đi ra Internet.

```text
Client -> Forward Proxy -> Internet
```

Server đích có thể không biết client thật.

### Reverse proxy

Reverse proxy đại diện cho server nhận request từ client.

```text
Client -> Reverse Proxy -> Backend
```

Client không cần biết backend thật chạy ở đâu hoặc có bao nhiêu instance.

Nginx trong tài liệu này đóng vai trò reverse proxy.

---

## 3. Tại sao không cho client gọi thẳng Spring Boot?

Nginx cung cấp một điểm vào thống nhất:

```text
Ẩn cổng và địa chỉ backend nội bộ.
Xử lý HTTPS/TLS.
Chuyển tiếp header đúng cách.
Giới hạn kích thước request.
Rate limiting.
Load balancing.
Ghi access log.
Phục vụ static file nếu cần.
```

Spring Boot vẫn chịu trách nhiệm:

```text
JWT authentication.
Role authorization.
Validation.
Business logic.
Database transaction.
```

Nginx không thay thế Spring Security.

---

## 4. Luồng request qua Nginx

```text
GET /api/products
Authorization: Bearer <access-token>
        |
        v
Nginx nhận request
        |
Giữ Authorization header
Thêm X-Forwarded-* headers
        |
        v
Spring Boot nhận /api/products
        |
JWT Filter xác thực token
        |
Controller xử lý
        |
Response quay lại qua Nginx
```

Nginx không được xóa `Authorization` header nếu backend dùng JWT.

---

## 5. Cấu trúc file Nginx

Có thể tạo:

```text
nginx/
├── nginx.conf
└── conf.d/
    └── supermarket.conf
```

Hoặc bắt đầu đơn giản với một file:

```text
nginx/nginx.conf
```

---

## 6. Cấu trúc nginx.conf

Ví dụ tối thiểu:

```nginx
events {
}

http {
    upstream supermarket_backend {
        server backend:8080;
    }

    server {
        listen 80;
        server_name localhost;

        location / {
            proxy_pass http://supermarket_backend;
        }
    }
}
```

Các block:

```text
events   -> cấu hình xử lý connection.
http     -> cấu hình HTTP chung.
upstream -> nhóm backend đích.
server   -> virtual server nhận request.
location -> quy tắc xử lý theo URL path.
```

---

## 7. Cấu hình reverse proxy đầy đủ hơn

```nginx
events {
    worker_connections 1024;
}

http {
    upstream supermarket_backend {
        server backend:8080;
    }

    server {
        listen 80;
        server_name localhost;

        client_max_body_size 10m;

        location / {
            proxy_pass http://supermarket_backend;
            proxy_http_version 1.1;

            proxy_set_header Host $host;
            proxy_set_header X-Real-IP $remote_addr;
            proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
            proxy_set_header X-Forwarded-Proto $scheme;

            proxy_connect_timeout 5s;
            proxy_send_timeout 30s;
            proxy_read_timeout 30s;
        }
    }
}
```

---

## 8. Giải thích các proxy header

### Host

```nginx
proxy_set_header Host $host;
```

Chuyển hostname client đã gọi cho backend.

### X-Real-IP

```nginx
proxy_set_header X-Real-IP $remote_addr;
```

Chứa IP client mà Nginx nhìn thấy.

### X-Forwarded-For

```nginx
proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
```

Giữ chuỗi proxy và IP gốc. Không nên tin header này từ Internet nếu request không đi qua
reverse proxy đáng tin cậy.

### X-Forwarded-Proto

```nginx
proxy_set_header X-Forwarded-Proto $scheme;
```

Cho backend biết client ban đầu dùng HTTP hay HTTPS.

---

## 9. Authorization header

Nginx mặc định thường chuyển tiếp header, nhưng có thể khai báo rõ:

```nginx
proxy_set_header Authorization $http_authorization;
```

Không ghi `$http_authorization` vào access log. Nó chứa JWT và có thể bị dùng để truy cập
API nếu bị lộ.

---

## 10. proxy_pass và dấu gạch chéo

Hai cấu hình có thể xử lý URI khác nhau:

```nginx
location /api/ {
    proxy_pass http://backend;
}
```

và:

```nginx
location /api/ {
    proxy_pass http://backend/;
}
```

Dấu `/` ở cuối `proxy_pass` có thể làm Nginx thay thế phần URI khớp với `location`.

Để giữ nguyên path cho SuperMarketAPI, cấu hình đơn giản:

```nginx
location / {
    proxy_pass http://supermarket_backend;
}
```

Request `/api/products` vẫn đến backend dưới dạng `/api/products`.

---

## 11. Kết hợp Nginx với Docker Compose

Kiến trúc:

```text
Host :80
   |
nginx container :80
   |
backend container :8080
   |
mysql container :3306
```

Thêm service:

```yaml
services:
  nginx:
    image: nginx:stable-alpine
    container_name: supermarket-nginx
    restart: unless-stopped
    ports:
      - "80:80"
    volumes:
      - ./nginx/nginx.conf:/etc/nginx/nginx.conf:ro
    depends_on:
      - backend
    networks:
      - supermarket-network
```

Trong `nginx.conf`, dùng tên service Docker:

```nginx
upstream supermarket_backend {
    server backend:8080;
}
```

Không dùng `localhost:8080`, vì localhost bên trong Nginx container là chính Nginx.

---

## 12. Có cần publish cổng backend không?

Khi Nginx là điểm vào duy nhất, backend không bắt buộc publish cổng ra host.

Thay vì:

```yaml
backend:
  ports:
    - "8080:8080"
```

Có thể dùng:

```yaml
backend:
  expose:
    - "8080"
```

`expose` mô tả cổng nội bộ cho các service trong network. Nginx vẫn gọi được
`backend:8080`, còn host không gọi trực tiếp `localhost:8080`.

Trong giai đoạn học/debug, có thể giữ port 8080 để so sánh gọi trực tiếp và gọi qua Nginx.

---

## 13. Kiểm tra reverse proxy

Gọi trực tiếp backend:

```text
http://localhost:8080/v3/api-docs
```

Gọi qua Nginx:

```text
http://localhost/v3/api-docs
```

Hai request nên trả cùng nội dung.

Login qua Nginx:

```http
POST http://localhost/api/auth/login
Content-Type: application/json

{
  "username": "admin",
  "password": "123456"
}
```

Sau đó dùng access token gọi API qua Nginx.

---

## 14. Access log và Error log của Nginx

Nginx có hai nhóm log chính:

```text
access log -> request, status, thời gian, IP.
error log  -> lỗi kết nối upstream, cấu hình, timeout.
```

Xem log container:

```powershell
docker compose logs -f nginx
```

Không cấu hình log format chứa Authorization header, cookie hoặc token.

---

## 15. Timeout

Các timeout phổ biến:

```nginx
proxy_connect_timeout 5s;
proxy_send_timeout 30s;
proxy_read_timeout 30s;
```

Ý nghĩa:

```text
connect -> thời gian Nginx chờ kết nối backend.
send    -> thời gian gửi request đến backend.
read    -> thời gian chờ backend trả dữ liệu.
```

Không đặt timeout quá dài để che giấu endpoint chậm. Cần tìm và tối ưu nguyên nhân ở backend
hoặc database.

---

## 16. Giới hạn kích thước request

```nginx
client_max_body_size 10m;
```

Nếu request lớn hơn giới hạn, Nginx trả `413 Request Entity Too Large` trước khi request đến
Spring Boot.

Với API JSON thông thường, không cần giới hạn quá lớn. Khi có upload file, chọn giới hạn dựa
trên yêu cầu nghiệp vụ.

---

## 17. Rate limiting

Rate limiting giới hạn số request trong một khoảng thời gian.

Ví dụ:

```nginx
http {
    limit_req_zone $binary_remote_addr zone=login_limit:10m rate=5r/m;

    server {
        location = /api/auth/login {
            limit_req zone=login_limit burst=5 nodelay;
            proxy_pass http://supermarket_backend;
        }
    }
}
```

Ý nghĩa ví dụ:

```text
Mỗi IP được khoảng 5 request login/phút.
burst cho phép một số request vượt ngắn hạn.
```

Rate limiting chỉ là một lớp bảo vệ. Vẫn cần chính sách khóa/chậm đăng nhập và giám sát phù
hợp ở backend. Cấu hình quá chặt có thể chặn người dùng hợp lệ dùng chung IP.

---

## 18. HTTPS và TLS termination

Trong production, client nên gọi HTTPS:

```text
Client --HTTPS--> Nginx --HTTP nội bộ--> Spring Boot
```

Nginx giữ certificate và giải mã TLS. Đây gọi là TLS termination.

Ví dụ cấu trúc khái niệm:

```nginx
server {
    listen 443 ssl;
    server_name api.example.com;

    ssl_certificate /etc/nginx/certs/fullchain.pem;
    ssl_certificate_key /etc/nginx/certs/privkey.pem;

    location / {
        proxy_pass http://supermarket_backend;
    }
}
```

Không commit private key vào Git hoặc Docker image.

---

## 19. Chuyển HTTP sang HTTPS

```nginx
server {
    listen 80;
    server_name api.example.com;

    return 301 https://$host$request_uri;
}
```

Server HTTPS nằm ở block khác. Chỉ bật redirect sau khi HTTPS đã cấu hình và kiểm tra thành
công.

---

## 20. Spring Boot và Forwarded headers

Khi Nginx kết thúc HTTPS nhưng gọi backend bằng HTTP, Spring cần hiểu giao thức gốc là HTTPS.

Có thể cấu hình:

```properties
server.forward-headers-strategy=framework
```

Chỉ tin forwarded headers khi ứng dụng thực sự đứng sau proxy đáng tin cậy. Nếu backend còn
được truy cập trực tiếp từ Internet, client có thể tự giả mạo các header này.

---

## 21. Load balancing là gì?

Load balancing phân phối request đến nhiều backend instance.

```text
                 +--> backend-1:8080
Client -> Nginx -+--> backend-2:8080
                 +--> backend-3:8080
```

Lợi ích:

```text
Tăng khả năng xử lý request.
Giảm phụ thuộc vào một instance.
Có thể bảo trì từng instance.
```

Load balancing không tự sửa ứng dụng có bottleneck ở MySQL hoặc query chậm.

---

## 22. Round Robin

Mặc định Nginx phân phối theo round robin:

```nginx
upstream supermarket_backend {
    server backend-1:8080;
    server backend-2:8080;
    server backend-3:8080;
}
```

```text
Request 1 -> backend-1
Request 2 -> backend-2
Request 3 -> backend-3
Request 4 -> backend-1
```

---

## 23. Least Connections

```nginx
upstream supermarket_backend {
    least_conn;

    server backend-1:8080;
    server backend-2:8080;
    server backend-3:8080;
}
```

Request mới được gửi đến backend đang có ít connection hoạt động hơn.

Phù hợp khi thời gian xử lý request chênh lệch đáng kể.

---

## 24. Weight

Backend mạnh hơn có thể nhận nhiều request hơn:

```nginx
upstream supermarket_backend {
    server backend-1:8080 weight=2;
    server backend-2:8080 weight=1;
}
```

`backend-1` được ưu tiên khoảng gấp đôi, nhưng phân phối thực tế còn phụ thuộc connection và
trạng thái backend.

---

## 25. Backup server

```nginx
upstream supermarket_backend {
    server backend-1:8080;
    server backend-2:8080 backup;
}
```

Backend backup chỉ được dùng khi các server chính không sẵn sàng.

---

## 26. JWT và nhiều backend instance

SuperMarketAPI dùng JWT stateless nên phù hợp với load balancing.

Điều kiện:

```text
Mọi backend dùng cùng JWT_SECRET.
Mọi backend kết nối cùng database hoặc dữ liệu đồng bộ.
Thời gian hệ thống giữa các máy không lệch đáng kể.
```

Access token tạo ở backend-1 phải được backend-2 xác thực được.

Refresh token lưu database nên backend nào cũng cần truy cập được bảng `refresh_tokens`.

---

## 27. Session và sticky session

Ứng dụng JWT stateless không cần lưu HTTP session đăng nhập trên một backend cụ thể. Vì vậy
thường không cần sticky session.

```text
Request 1 -> backend-1
Request 2 -> backend-2
```

Cả hai backend đều xác thực JWT độc lập bằng cùng secret.

Đây là một lợi ích của stateless authentication.

---

## 28. Khi backend bị lỗi

Nginx có thể thử upstream khác với một số loại lỗi, nhưng retry request thay đổi dữ liệu cần
cực kỳ cẩn thận.

Ví dụ retry `POST create order` có thể tạo dữ liệu hai lần nếu backend đầu đã xử lý nhưng
response bị mất.

Không bật retry rộng cho mọi request nếu nghiệp vụ chưa có idempotency.

---

## 29. CORS khi có Nginx

CORS là chính sách của browser khi frontend và API khác origin.

Có thể xử lý CORS tại Spring Boot hoặc Nginx. Nên chọn một nơi làm nguồn cấu hình chính để
tránh header bị lặp hoặc mâu thuẫn.

Nginx reverse proxy không tự động loại bỏ nhu cầu CORS. Nếu frontend và API được đưa về cùng
origin qua Nginx thì cấu hình có thể đơn giản hơn.

---

## 30. Swagger qua Nginx

Nếu proxy toàn bộ path:

```nginx
location / {
    proxy_pass http://supermarket_backend;
}
```

Các URL giữ nguyên:

```text
http://localhost/swagger-ui/index.html
http://localhost/v3/api-docs
```

Nếu đặt backend dưới prefix như `/backend/`, cần cấu hình thêm context path và OpenAPI để URL
không bị sai. Khi mới học nên giữ path nguyên bản.

---

## 31. Kiểm tra cấu hình Nginx

Trong container:

```powershell
docker compose exec nginx nginx -t
```

Nếu hợp lệ, có thể reload:

```powershell
docker compose exec nginx nginx -s reload
```

Với Compose local, tạo lại container cũng là cách đơn giản:

```powershell
docker compose up -d --force-recreate nginx
```

---

## 32. Các lỗi thường gặp

### 502 Bad Gateway

Nginx không kết nối được backend.

Kiểm tra:

```text
Tên service backend đúng không?
Backend có đang chạy không?
Cổng nội bộ có phải 8080 không?
Hai service có cùng network không?
```

### 504 Gateway Timeout

Nginx kết nối được nhưng backend trả response quá chậm hoặc không trả.

Kiểm tra query database, deadlock, timeout và log Spring Boot trước khi tăng timeout.

### 401 qua Nginx nhưng gọi trực tiếp thành công

Kiểm tra Authorization header có được chuyển tiếp không và access token có bị công cụ client
gửi sai không.

### URL bị mất `/api`

Kiểm tra dấu `/` ở `proxy_pass` và cách location rewrite URI.

### Connection refused

Backend chưa lắng nghe, sai hostname hoặc sai port.

### Swagger tải trang nhưng không gọi được API

Kiểm tra `/v3/api-docs`, forwarded headers và server URL trong OpenAPI.

---

## 33. Bảo mật Nginx cơ bản

- Chỉ expose cổng Nginx ra Internet.
- Dùng HTTPS trên production.
- Không log Authorization/cookie/token.
- Giới hạn request body.
- Cấu hình timeout hợp lý.
- Cân nhắc rate limit cho login.
- Cập nhật Nginx image định kỳ.
- Không để file private key trong Git.
- Không tin forwarded headers từ proxy không kiểm soát.
- Ẩn MySQL và backend trong private network khi có thể.

---

## 34. Checklist Reverse Proxy

- [ ] Backend chạy ổn khi gọi trực tiếp.
- [ ] Tạo `nginx/nginx.conf`.
- [ ] Thêm service Nginx vào Compose.
- [ ] Nginx và backend cùng network.
- [ ] `proxy_pass` dùng `backend:8080`, không dùng localhost.
- [ ] Chuyển tiếp Host và X-Forwarded headers.
- [ ] JWT Authorization header đến được backend.
- [ ] Login qua Nginx thành công.
- [ ] API bảo vệ qua Nginx thành công.
- [ ] Swagger/OpenAPI qua Nginx hoạt động.
- [ ] Backend không bắt buộc expose ra host.
- [ ] Log không chứa token.

---

## 35. Checklist Load Balancing

- [ ] Chạy ít nhất hai backend instance.
- [ ] Các instance dùng cùng JWT secret.
- [ ] Các instance kết nối cùng database test.
- [ ] Cấu hình nhiều server trong upstream.
- [ ] Gọi API nhiều lần và xác nhận request được phân phối.
- [ ] Dừng một backend và quan sát hành vi.
- [ ] Kiểm tra lỗi 502/504.
- [ ] Không phụ thuộc HTTP session cục bộ.
- [ ] Không bật retry nguy hiểm cho request ghi dữ liệu.

---

## 36. Thứ tự thực hành

```text
1. Hoàn thiện API và toàn bộ test.
2. Chạy backend + MySQL bằng Docker Compose.
3. Tạo nginx.conf tối thiểu.
4. Thêm Nginx vào cùng Docker network.
5. Test /v3/api-docs qua Nginx.
6. Test login và API JWT qua Nginx.
7. Ẩn cổng backend khỏi host.
8. Thêm timeout và giới hạn request body.
9. Tìm hiểu HTTPS.
10. Chạy hai backend instance.
11. Thử round robin và least_conn.
12. Sau cùng mới nghiên cứu rate limit và cấu hình production nâng cao.
```

