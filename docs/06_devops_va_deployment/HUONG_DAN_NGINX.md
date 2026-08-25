# 🌐 GIÁO TRÌNH NGINX & CÂN BẰNG TẢI (REVERSE PROXY & LOAD BALANCING)

> 🎯 **Mục tiêu**: Làm chủ nguyên lý Nginx Reverse Proxy đứng trước Spring Boot, phân phối tải (Load Balancing) cho nhiều máy chủ, giải mã SSL và cung cấp **Bộ Nhiệm Vụ Thực Hành (Project Quests)** cho dự án mới!

---

## 📚 PHẦN 1: GIẢI PHÃ BẢN CHẤT LÝ THUYẾT (DEEP DIVE THEORY)

---

### 1. Phép Ẩn Dụ: Forward Proxy vs Reverse Proxy

```text
[ 1. FORWARD PROXY: Đại diện cho Client (Ví dụ: VPN lướt web ẩn danh) ]
Client ──► [ Forward Proxy / VPN ] ──► Internet / Website đích (Giấu IP của Client)

[ 2. REVERSE PROXY: Đại diện cho Server (Ví dụ: NGINX đứng trước Spring Boot) ]
Client ──► [ NGINX (Cổng 80/443) ] ──► [ Spring Boot 1 (:8080) ]
                                   ──► [ Spring Boot 2 (:8081) ] (Giấu IP của Server)
```

| Tiêu chí | Forward Proxy (VPN) | Reverse Proxy (NGINX) |
| :--- | :--- | :--- |
| **Đại diện cho ai?** | Đại diện cho **Client (Người dùng)** | Đại diện cho **Server (Hệ thống Backend)** |
| **Mục đích chính** | Vượt tường lửa, giấu IP người dùng | Bảo vệ server, cân bằng tải, giải mã HTTPS (SSL Termination) |
| **Vị trí đứng** | Đứng ở phía máy Client | Đứng làm cửa ngõ ở trước cụm Backend |

---

### 2. Ba Lợi Ích Lớn Nhất Khi Cho Nginx Đứng Trước Spring Boot

1. **Bảo Mật (Security Shield)**: Client không hề biết địa chỉ IP thật hay số cổng của Spring Boot và MySQL. Mọi tấn công DDoS/Quét cổng đều bị chặn lại ở Nginx.
2. **Cân Bằng Tải (Load Balancing)**: Khi có 1 triệu người truy cập, Nginx tự động chia đều request cho 3-5 container Spring Boot chạy song song $\rightarrow$ Không sợ bị nghẽn server.
3. **Giảm Tải CPU (SSL Termination)**: Nginx đảm nhận việc giải mã HTTPS nặng nề bằng ngôn ngữ C siêu tốc, sau đó chuyển tiếp HTTP nhẹ nhàng vào Spring Boot $\rightarrow$ Java tiết kiệm được 30% CPU.

---

### 3. Các Thuật Toán Cân Bằng Tải Cần Nhớ

* **Round Robin (Mặc định)**: Chia lần lượt từng máy (Request 1 vào Máy 1, Request 2 vào Máy 2, Request 3 vào Máy 1...).
* **Least Connections (`least_conn`)**: Chuyển request đến máy chủ nào đang rảnh nhất (có ít kết nối đang xử lý nhất).
* **IP Hash (`ip_hash`)**: Cùng 1 địa chỉ IP của người dùng sẽ luôn được gửi đến cùng 1 máy chủ backend (dùng khi backend có lưu Session).

---

## 🎮 PHẦN 2: BỘ NHIỆM VỤ THỰC HÀNH DỰ ÁN MỚI (NGINX QUESTS)

---

### ⚔️ QUEST 1: Viết File Cấu Hình `nginx.conf` Chuẩn
* **Mục tiêu**: Chuyển tiếp toàn bộ request từ cổng 80 vào Spring Boot cổng 8080.
* **Nhiệm vụ cần làm**:
  ```nginx
  events { worker_connections 1024; }

  http {
      upstream backend_servers {
          server backend:8080; # Tên service trong Docker Compose
      }

      server {
          listen 80;
          server_name localhost;

          location / {
              proxy_pass http://backend_servers;
              # Chuyển tiếp thông tin IP thật của Client vào Spring Boot
              proxy_set_header Host $host;
              proxy_set_header X-Real-IP $remote_addr;
              proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
              proxy_set_header X-Forwarded-Proto $scheme;
          }
      }
  }
  ```

---

### ⚔️ QUEST 2: Tích Hợp Nginx Vào `docker-compose.yml`
* **Mục tiêu**: Khởi động cụm Nginx + Spring Boot + MySQL cùng nhau.
* **Nhiệm vụ cần làm**:
  1. Thêm service `nginx`:
     * Image: `nginx:alpine`
     * Ports: `"80:80"`
     * Volumes: `./nginx.conf:/etc/nginx/nginx.conf:ro`
     * Depends_on: `backend`
  2. Ẩn cổng `8080` của service `backend` khỏi máy host (chỉ mở cổng nội bộ cho Nginx).

---

### ⚔️ QUEST 3: Cấu Hình Load Balancing Cho 2 Backend Instances
* **Mục tiêu**: Chạy 2 container Spring Boot cùng lúc và chia tải.
* **Nhiệm vụ cần làm**:
  1. Trong `nginx.conf`:
     ```nginx
     upstream backend_cluster {
         least_conn; # Ưu tiên máy rảnh
         server backend_1:8080;
         server backend_2:8080;
     }
     ```
  2. Chạy `docker compose up --scale backend=2 -d` để nhân bản 2 container Java!

---

## 🏆 BOSS QUEST: THỬ THÁCH KIỂM CHỨNG CÂN BẰNG TẢI

* 🧪 **Thử nghiệm**:
  1. Khởi động cụm 1 Nginx + 2 Container Spring Boot.
  2. Bật Postman gửi liên tục 10 request `GET http://localhost/api/products`.
  3. Mở log Docker: `docker compose logs -f`
  4. 👉 *Kỳ vọng: Bạn sẽ thấy 5 request được xử lý bởi `backend_1` và 5 request được xử lý bởi `backend_2` xen kẽ nhau hoàn hảo!*
