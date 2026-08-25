# 🔑 HƯỚNG DẪN TẠO VÀ QUẢN LÝ CHUỖI BÍ MẬT JWT (JWT SECRET KEY & ENV)

> 🎯 **Mục tiêu**: Hiểu tại sao thuật toán HMAC-SHA256 bắt buộc `JWT_SECRET` phải đủ 256-bit (tối thiểu 32 bytes), cách sinh chuỗi bí mật chuẩn quân đội bằng PowerShell/Linux/Java, cách nạp biến môi trường và cung cấp **Bộ Nhiệm Vụ Thực Hành (Project Quests)**!
> ⏱️ **Thời gian đọc**: ~12 phút.

---

## 📚 PHẦN 1: GIẢI PHÃ BẢN CHẤT LÝ THUYẾT (DEEP DIVE THEORY)

---

### 1. Tại Sao Chuỗi Bí Mật JWT Bắt Buộc Phải Đủ 256-bit?

Thuật toán mã hóa chữ ký số `HS256` (HMAC with SHA-256) sử dụng một chuỗi khóa bí mật (`SECRET_KEY`) để ký và xác thực tính toàn vẹn của Token:

```text
HMAC-SHA256: Cần khóa có kích thước tối thiểu = 256 bits = 32 bytes = 32 ký tự ASCII
```

### 💥 Thảm Họa Khi Đặt Khóa Bí Mật Quá Ngắn / Quá Yếu:
* Nếu bạn đặt `JWT_SECRET="123456"` hoặc `"mysecretkey"`:
  1. Kẻ xấu lấy được 1 chuỗi JWT hợp lệ từ trình duyệt.
  2. Dùng các công cụ bẻ khóa siêu tốc (như `hashcat` hoặc `john the ripper`) chạy trên card màn hình GPU.
  3. Chỉ mất **3 giây** để máy tính dò ra chuỗi `"123456"`.
  4. 👉 *Hacker lập tức tự tạo hàng triệu Token Admin giả mạo để kiểm soát toàn bộ server của bạn!*

---

### 2. Bốn Cách Sinh Chuỗi Bí Mật 256-bit Chuẩn An Toàn

#### 🔹 Cách 1: Sinh Bằng PowerShell Trên Windows (Khuyên Dùng)
Mở PowerShell và dán đoạn lệnh sau:
```powershell
$bytes = New-Object byte[] 32
[Security.Cryptography.RandomNumberGenerator]::Fill($bytes)
[Convert]::ToBase64String($bytes)
```
👉 *Kết quả in ra*: Một chuỗi Base64 256-bit siêu bảo mật, ví dụ: `d8fA+9K1xL3mQ7vP0wZ5tN8sR2uY4cX6bE1gH9jK3mU=`

#### 🔹 Cách 2: Sinh Bằng Linux / Git Bash / Mac OS
Mở Git Bash hoặc Linux Terminal gõ:
```bash
openssl rand -base64 32
```

#### 🔹 Cách 3: Sinh Trực Tiếp Bằng Mã Java
```java
import java.security.SecureRandom;
import java.util.Base64;

public class SecretGenerator {
    public static void main(String[] args) {
        byte[] bytes = new byte[32];
        new SecureRandom().nextBytes(bytes);
        System.out.println("JWT Secret: " + Base64.getEncoder().encodeToString(bytes));
    }
}
```

---

## ⚙️ PHẦN 2: CÁCH NẠP BIẾN MÔI TRƯỜNG AN TOÀN TRONG THỰC TẾ

> ⚠️ **Quy tắc bảo mật**: **TUYỆT ĐỐI KHÔNG** gõ trực tiếp chuỗi bí mật vào file `application.properties` rồi đẩy lên GitHub công khai! Luôn sử dụng biến môi trường (Environment Variable).

### 1. Khai Báo Trong `application.properties` Bằng Cú Pháp Placeholder:
```properties
# Đọc từ biến môi trường JWT_SECRET, nếu không có thì dùng giá trị mặc định sau dấu hai chấm
jwt.secret=${JWT_SECRET:day_la_chuoi_mac_dinh_chi_dung_cho_moi_truong_local_dev_32_ky_tu_tro_len}
jwt.expiration=900000 # 15 phút (tính bằng mili-giây)
```

### 2. Cách Set Biến Môi Trường Khi Chạy Lệnh Bằng PowerShell:
```powershell
$env:JWT_SECRET="d8fA+9K1xL3mQ7vP0wZ5tN8sR2uY4cX6bE1gH9jK3mU="
$env:DB_PASSWORD="mat-khau-mysql-that"
.\mvnw.cmd spring-boot:run
```

### 3. Cách Set Trong File `docker-compose.yml`:
```yaml
services:
  backend:
    environment:
      - JWT_SECRET=d8fA+9K1xL3mQ7vP0wZ5tN8sR2uY4cX6bE1gH9jK3mU=
```

---

## 🎮 PHẦN 3: BỘ NHIỆM VỤ THỰC HÀNH DỰ ÁN MỚI (SECRET QUESTS)

---

### ⚔️ QUEST 1: Sinh Khóa Bí Mật & Tạo File `.env`
* **Nhiệm vụ cần làm**:
  1. Mở PowerShell sinh 1 chuỗi Base64 256-bit ngẫu nhiên.
  2. Tạo file `.env` ở thư mục gốc (không commit file này lên Git!).
  3. Thêm file `.env` vào file `.gitignore`.

---

### ⚔️ QUEST 2: Đọc Khóa Bí Mật Trong `JwtService.java`
* **Nhiệm vụ cần làm**:
  1. Dùng `@Value("${jwt.secret}") private String secretKey;`.
  2. Chuyển chuỗi Base64 thành khóa ký số `SecretKey`:
     ```java
     private SecretKey getSigningKey() {
         byte[] keyBytes = Decoders.BASE64.decode(secretKey);
         return Keys.hmacShaKeyFor(keyBytes);
     }
     ```

---

## 🏆 BOSS QUEST: THỬ THÁCH BẢO VỆ MÃ NGUỒN TRÊN GIT

* 🧪 **Thử nghiệm**:
  1. Kiểm tra file `.gitignore` xem đã có dòng `.env` và `application-local.properties` chưa.
  2. Chạy lệnh: `git status` $\rightarrow$ *Kỳ vọng: File chứa mật khẩu không bao giờ xuất hiện trong danh sách commit của Git!*