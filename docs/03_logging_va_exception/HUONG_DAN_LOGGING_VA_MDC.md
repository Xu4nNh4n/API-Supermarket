# 📘 GIÁO TRÌNH LOGGING CHUYÊN NGHIỆP: SLF4J, LOGBACK & MDC TRACE-ID

> 🎯 **Mục tiêu**: Làm chủ tư duy ghi log cấp doanh nghiệp (Enterprise Logging) và cung cấp **Bộ Nhiệm Vụ Thực Hành (Project Quests)** để tự tay cấu hình hệ thống truy vết `traceId` từ con số 0 trong dự án mới!

---

## 📚 PHẦN 1: GIẢI PHÃ BẢN CHẤT LÝ THUYẾT (DEEP DIVE THEORY)

---

### 1. Dòng Chảy Của Logging Trong Ứng Dụng Backend

```text
[ Client gửi Request ]
         │
         ▼
[ MdcLoggingFilter ] ──► (1. Sinh traceId ngẫu nhiên dạng UUID)
         │           ──► (2. Nạp traceId vào MDC ThreadLocal)
         ▼
[ Controller / Service / Repo ] ──► (3. log.info / log.error tự động có [traceId=...])
         │
         ▼
[ GlobalExceptionHandler ] ──► (4. Đính kèm traceId vào JSON lỗi trả về cho Client)
         │
         ▼
[ Filter kết thúc ] ──► (5. Khối finally: MDC.clear() dọn sạch bộ nhớ)
```

---

### 2. So Sánh `System.out.println()` vs `SLF4J Logger`

| Tiêu chí | `System.out.println()` (Nghiệp dư) | `SLF4J Logger` (Chuẩn Doanh Nghiệp) |
| :--- | :--- | :--- |
| **Lưu trữ** | ❌ Chỉ in ra Console, mất khi tắt app | ✅ Tự động ghi file theo ngày (`app-2026-08-21.log`), nén zip lưu nhiều năm |
| **Hiệu năng** | ❌ Chặn luồng (Blocking I/O), làm chậm server | ✅ Hỗ trợ Async Logging (Bất đồng bộ), tốc độ siêu nhanh |
| **Phân cấp Level** | ❌ Không có, muốn tắt phải xóa code | ✅ Bật/tắt linh hoạt (DEV bật `DEBUG`, PROD chỉ bật `INFO/ERROR`) |
| **Định dạng Log** | ❌ Chỉ có dòng chữ thô sơ | ✅ Tự động gắn: `[Thời gian] [Thread] [traceId] [Tên Class] [Level]` |

---

### 3. Khi Nào Dùng 4 Cấp Độ Log (Log Levels)?

```text
  ERROR  ▲  Lỗi nghiêm trọng (Sập DB, lỗi hệ thống, thanh toán thất bại) ──► Dev phải vào sửa ngay!
         │
  WARN   │  Cảnh báo bất thường (Kho sắp hết hàng, đăng nhập sai 1 lần) ──► Cần theo dõi
         │
  INFO   │  Hành động quan trọng thành công (Tạo đơn hàng #123, Đăng nhập thành công)
         │
  DEBUG  ▼  Chi tiết phục vụ lúc viết code (Giá trị biến, dữ liệu trung gian)
```

---

### 4. Bí Quyết MDC (Mapped Diagnostic Context) & Trace-ID

Khi có hàng ngàn người cùng gọi API trong 1 giây, console sẽ bị lẫn lộn giữa các Thread. Nhờ có `traceId`, mỗi request được đóng một "con dấu định danh":

```text
[traceId=9a1b-c3d4] [SalesOrderServiceImpl] Bắt đầu tạo đơn cho khách A
[traceId=8e7f-b1a2] [SalesOrderServiceImpl] Bắt đầu tạo đơn cho khách B
[traceId=9a1b-c3d4] [SalesOrderServiceImpl] Trừ kho thành công đơn khách A
[traceId=7c6d-e5f4] [SalesOrderServiceImpl] Lỗi: Khách C không đủ tiền!
```

---

## 🎮 PHẦN 2: BỘ NHIỆM VỤ THỰC HÀNH DỰ ÁN MỚI (PROJECT QUESTS)

---

### ⚔️ QUEST 1: Cấu Hình File `logback-spring.xml`
* **Mục tiêu**: Thiết lập mẫu format log in ra Console và ghi ra file.
* **Nhiệm vụ cần làm**:
  1. Tạo file `src/main/resources/logback-spring.xml`.
  2. Cấu hình Console Pattern có chứa `[%X{traceId}]`:
     ```xml
     <pattern>%d{yyyy-MM-dd HH:mm:ss} [%X{traceId}] %-5level %logger{36} - %msg%n</pattern>
     ```
  3. Cấu hình RollingFileAppender để tự động cắt file log mỗi ngày 1 file.

---

### ⚔️ QUEST 2: Xây Dựng `MdcLoggingFilter`
* **Mục tiêu**: Tự động sinh và dọn dẹp `traceId` cho mọi request.
* **Nhiệm vụ cần làm**:
  1. Tạo class `MdcLoggingFilter extends OncePerRequestFilter`.
  2. Sinh chuỗi `String traceId = UUID.randomUUID().toString();`
  3. Gán vào MDC: `MDC.put("traceId", traceId);`
  4. Đặt `filterChain.doFilter(request, response)` trong khối `try`.
  5. Đặt `MDC.clear()` trong khối `finally` để giải phóng ThreadLocal.

---

### ⚔️ QUEST 3: Tích Hợp `traceId` Vào JSON Lỗi `ApiErrorResponse`
* **Mục tiêu**: Trả `traceId` về cho Frontend khi có lỗi.
* **Nhiệm vụ cần làm**:
  1. Thêm trường `private String traceId;` vào `ApiErrorResponse.java`.
  2. Trong `GlobalExceptionHandler`, mỗi khi bắt Exception $\rightarrow$ lấy `MDC.get("traceId")` điền vào response.

---

### ⚔️ QUEST 4: Áp Dụng `@Slf4j` Chuẩn Trong Service
* **Mục tiêu**: Ghi log đúng cú pháp và đúng level.
* **Nhiệm vụ cần làm**:
  1. Gắn `@Slf4j` lên đầu ServiceImpl.
  2. Dùng placeholder `{}`: `log.info("Tạo hóa đơn ID: {} thành công", order.getId());` (Không dùng cộng chuỗi `+`).
  3. Dùng `log.warn(...)` cho lỗi nghiệp vụ và `log.error(...)` cho Exception nghiêm trọng.

---

## 🏆 BOSS QUEST: THỬ THÁCH TRUY VẾT LỖI THỰC CHIẾN

* 🧪 **Thử thách**: Gọi API tạo đơn hàng với dữ liệu sai để server bắn lỗi 400.
* 📋 **Yêu cầu**:
  1. Nhìn vào JSON trả về trên Postman/Bruno, copy mã `traceId`.
  2. Mở file log của Server, nhấn `Ctrl + F` dán mã `traceId` vào $\rightarrow$ *Quan sát toàn bộ các dòng log từ lúc request vào đến lúc văng lỗi!*
