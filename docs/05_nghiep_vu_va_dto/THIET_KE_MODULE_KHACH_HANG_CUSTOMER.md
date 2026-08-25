# 👥 GIÁO TRÌNH THIẾT KẾ MODULE KHÁCH HÀNG & TÍCH ĐIỂM (CUSTOMER ARCHITECTURE)

> 🎯 **Mục tiêu**: Nắm vững tư duy thiết kế Module Khách hàng (Customer): Quản lý thành viên, kiểm tra trùng lặp SĐT/Email an toàn với `Objects.equals()`, tích điểm thành viên khi mua hàng và cung cấp **Bộ Nhiệm Vụ Thực Hành (Project Quests)**!

---

## 📚 PHẦN 1: GIẢI MÃ BẢN CHẤT LÝ THUYẾT (DEEP DIVE THEORY)

### 1. Vai Trò Của Module Customer Trong Siêu Thị

Khác với tài khoản nhân viên (`User`) có tài khoản và mật khẩu đăng nhập, `Customer` đại diện cho khách mua sắm tại quầy:

* **Thu ngân tra cứu nhanh**: Tìm khách theo **Số điện thoại** ngay khi khách đọc tại quầy tính tiền.
* **Tích điểm thưởng (Loyalty Points)**: Mỗi lần mua hàng tích luỹ điểm để sau này giảm giá.
* **Khách vãng lai**: Nếu khách không muốn đăng ký thành viên $\rightarrow$ `customerId = null` (Hệ thống vẫn cho phép tạo đơn và hiển thị "Khách vãng lai").

```text
               ┌──────────────────────────────────────────────┐
               │              Quầy Thu Ngân POS               │
               └──────────────────────┬───────────────────────┘
                                      │
                   ┌──────────────────┴──────────────────┐
                   ▼                                     ▼
        [ Khách đọc Số Điện Thoại ]             [ Khách không đăng ký ]
                   │                                     │
                   ▼                                     ▼
      ┌─────────────────────────┐             ┌─────────────────────┐
      │ GET /api/customers/     │             │ customerId = null   │
      │       phone/0987654321  │             │ (Khách vãng lai)    │
      └────────────┬────────────┘             └──────────┬──────────┘
                   │                                     │
                   └──────────────────┬──────────────────┘
                                      ▼
                        ┌───────────────────────────┐
                        │   POST /api/orders (Mua)  │
                        └───────────────────────────┘
```

---

### 2. Kỹ Thuật Check Trùng An Toàn Khi Cập Nhật (`updateCustomer`)

* **Kịch bản thảm họa**: Khách hàng tên Tuấn có SĐT `0987654321`. Khi cập nhật đổi tên thành "Tuấn Anh" nhưng giữ nguyên SĐT cũ, nếu code kiểm tra ngây thơ `if (customerRepository.existsByPhone(phone))` $\rightarrow$ Hệ thống sẽ **báo lỗi sai** rằng *"SĐT đã tồn tại"* (vì chính Tuấn đang sở hữu số này!).
* **Giải pháp chuẩn**: Chỉ kiểm tra trùng khi SĐT gửi lên **khác** với SĐT hiện tại của khách hàng:

```java
// ✅ KỸ THUẬT SO SÁNH AN TOÀN:
if (request.getPhone() != null && !Objects.equals(customer.getPhone(), request.getPhone())) {
    if (customerRepository.existsByPhone(request.getPhone())) {
        throw new BadRequestException("Số điện thoại này đã thuộc về khách hàng khác!");
    }
}
```

---

## 🎮 PHẦN 2: BỘ NHIỆM VỤ THỰC HÀNH DỰ ÁN MỚI (CUSTOMER QUESTS)

### ⚔️ LEVEL 1: Thiết Kế Entity & Repository

* **Nhiệm vụ**:
  1. Tạo Entity [Customer.java](file:///d:/Code/code/Java/Backend/SuperMarketAPI/src/main/java/com/api/supermarket/entity/Customer.java) với các cột `fullName`, `phone`, `email`, `address`, `points`, `isActive`, `created_at`.
  2. Tạo Repository [CustomerRepository.java](file:///d:/Code/code/Java/Backend/SuperMarketAPI/src/main/java/com/api/supermarket/repository/CustomerRepository.java) với method `findByPhone(String phone)` và query tìm kiếm nâng cao.

---

### ⚔️ LEVEL 2: Xây Dựng Tầng Service

* **Nhiệm vụ**:
  1. Viết `createCustomer()`: Bắt buộc tên không rỗng, kiểm tra trùng SĐT và Email.
  2. Viết `getCustomerByPhone()`: Tìm nhanh khách theo SĐT, ném `ResourceNotFoundException` nếu chưa có.
  3. Viết `updateCustomer()`: Áp dụng `Objects.equals()` để kiểm tra trùng SĐT/Email an toàn.

---

### ⚔️ LEVEL 3: Mở 6 Endpoint REST Controller

* **Nhiệm vụ**: Xây dựng [CustomerController.java](file:///d:/Code/code/Java/Backend/SuperMarketAPI/src/main/java/com/api/supermarket/controller/CustomerController.java):
  * `POST /api/customers`: Trả về `201 CREATED`.
  * `GET /api/customers/{id}`: Xem chi tiết.
  * `GET /api/customers/phone/{phone}`: Tìm kiếm theo SĐT.
  * `PUT /api/customers/{id}`: Cập nhật thông tin.
  * `DELETE /api/customers/{id}`: Xóa khách (Phân quyền `hasRole('ADMIN')`).
  * `GET /api/customers`: Phân trang và tìm kiếm theo tên, SĐT, địa chỉ.

---

## 🏆 BOSS QUEST: THỬ THÁCH TÍCH HỢP KHÁCH HÀNG & HÓA ĐƠN

1. Tạo khách hàng mới tên "Nguyễn Văn An", SĐT `0912345678` bằng `POST /api/customers`.
2. Tạo hóa đơn mua hàng bằng `POST /api/orders` với `customerId` của anh An.
3. Kiểm tra xem hóa đơn trả về có tên "Nguyễn Văn An" và SĐT `0912345678` hay không.
4. Thử tạo hóa đơn khác với `customerId = null` $\rightarrow$ Kỳ vọng hóa đơn tự động hiển thị tên *"Khách vãng lai"*.
