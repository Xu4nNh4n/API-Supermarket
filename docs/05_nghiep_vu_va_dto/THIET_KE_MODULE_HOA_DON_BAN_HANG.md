# 🧾 GIÁO TRÌNH CHUYÊN SÂU: THIẾT KẾ MODULE HÓA ĐƠN BÁN HÀNG (SALES ORDER SERVICE)

> 🎯 **Mục tiêu**: Giải mã toàn bộ kiến trúc "phức tạp nhất hệ thống" của tầng Service Hóa Đơn: Điều phối 5 Repository, quản lý tính toàn vẹn tồn kho, chống gian lận giá, và kiểm soát State Machine `@Transactional`.

---

## 📚 PHẦN 1: GIẢI MÃ TẠI SAO SERVICE NÀY LẠI "ĐIÊN" HƠN BÌNH THƯỜNG?

Trong các module thông thường (`Category`, `Supplier`, `Product`), một Service chỉ làm việc với **1 Repository duy nhất** để thực hiện CRUD đơn giản.

Nhưng với **`SalesOrderServiceImpl`**, một thao tác bấm "Tạo đơn hàng" phải chạm vào **5 Repository cùng lúc**:

```text
                                  ┌───────────────────────────┐
                                  │   SalesOrderServiceImpl   │
                                  └─────────────┬─────────────┘
                                                │
         ┌──────────────────┬───────────────────┼───────────────────┬──────────────────┐
         ▼                  ▼                   ▼                   ▼                  ▼
┌─────────────────┐┌─────────────────┐┌───────────────────┐┌─────────────────┐┌─────────────────┐
│ UserRepository  ││CustomerRepository││ ProductRepository ││ SalesOrderRepo  ││SalesOrderItemRepo│
│ (Lấy Thu Ngân)  ││(Soi Khách Mua)  ││(Soi & Trừ Tồn Kho)││(Lưu Đơn Master) ││ (Lưu Chi Tiết)  │
└─────────────────┘└─────────────────┘└───────────────────┘└─────────────────┘└─────────────────┘
```

---

## ⚙️ PHẦN 2: 4 QUY TẮC SỐNG CÒN TRONG BÁN HÀNG

### 1. Quy Tắc @Transactional & Rollback Tức Thì

* **Kịch bản thảm họa**: Đơn hàng có 5 sản phẩm. Hệ thống đã trừ kho thành công 4 sản phẩm đầu, nhưng đến sản phẩm thứ 5 thì bị lỗi đứt mạng hoặc hết hàng.
* **Nếu không có `@Transactional`**: 4 món đầu bị mất hàng trong kho mà hóa đơn thì không được tạo $\rightarrow$ **Mất cân bằng kho thực tế và sổ sách!**
* **Giải pháp**: Gắn `@Transactional` lên method `createSalesOrder`, `paySalesOrder`, `cancelSalesOrder`. Nếu có bất kỳ lỗi nào ném ra $\rightarrow$ Spring tự động Rollback 100% về nguyên trạng!

---

### 2. Quy Tắc Chống Hack Sửa Giá (Price Tampering)

* **Kịch bản thảm họa**: Hacker sửa Request JSON gửi lên `unitPrice: 1000đ` cho chiếc Tivi giá 20 triệu.
* **Giải pháp**: Client **tuyệt đối không được gửi giá** trong `OrderItemRequest`. Server lấy `productId` $\rightarrow$ bốc giá niêm yết trong bảng `products` của CSDL để tính tiền:

```text
[ Client gửi lên ]               [ Server xử lý an toàn ]
{                                ┌────────────────────────────────────────┐
  "productId": 1,         ───►   │ productRepository.findById(1)          │
  "quantity": 2                  │ BigDecimal price = product.getPrice(); │ ──► Bốc giá từ DB!
}                                └────────────────────────────────────────┘
```

---

### 3. Vòng Đời Máy Trạng Thái (State Machine Lifecycle)

```text
                [ POST /api/orders (Tạo đơn hàng) ]
                                 │
                                 ▼
                     ┌───────────────────────┐
                     │   status = PENDING    │ ───► Đã trừ kho, chờ trả tiền
                     └───────────────────────┘
                                 │
                 ┌───────────────┴───────────────┐
                 │                               │
    [ PUT /api/orders/{id}/pay ]    [ PUT /api/orders/{id}/cancel ]
                 │                               │
                 ▼                               ▼
     ┌───────────────────────┐       ┌───────────────────────┐
     │    status = PAID      │       │  status = CANCELLED   │ ───► Tự động HOÀN KHO!
     │  (Ghi nhận paid_at)   │       │ (Cấm sửa, cấm xóa)    │
     └───────────────────────┘       └───────────────────────┘
```

* **Quy tắc chặn lỗi (Guard Clauses)**:
  * Đơn đang `PAID` $\rightarrow$ Không được bấm thanh toán lần 2 (`BadRequestException`).
  * Đơn đã `CANCELLED` $\rightarrow$ Không thể thanh toán lại.
  * Đơn đã `PAID` $\rightarrow$ Không được phép bấm hủy đơn tùy tiện (phải thông qua quy trình trả hàng).

---

## 🎮 PHẦN 3: BỘ NHIỆM VỤ THỰC HÀNH DỰ ÁN MỚI (QUESTS)

### ⚔️ LEVEL 1: Xây Dựng Luồng Trừ Kho (`createSalesOrder`)

* **Mục tiêu**: Viết logic duyệt vòng lặp `for`, kiểm tra tồn kho và trừ số lượng.
* **Checklist thực hiện**:
  * [ ] Lấy `Authentication` từ `SecurityContextHolder` để xác định nhân viên thu ngân.
  * [ ] Duyệt từng `item` trong `request.getItems()`.
  * [ ] Nếu `stockQuantity < requestedQuantity` $\rightarrow$ Ném `InsufficientStockException`.
  * [ ] Cập nhật `stockQuantity = stockQuantity - requestedQuantity` và lưu `productRepository.save()`.

---

### ⚔️ LEVEL 2: Xây Dựng Luồng Hoàn Kho Khi Hủy Đơn (`cancelSalesOrder`)

* **Mục tiêu**: Đảm bảo kho hàng luôn chính xác tuyệt đối khi khách đổi ý hủy đơn.
* **Checklist thực hiện**:
  * [ ] Tìm đơn theo `orderId`.
  * [ ] Kiểm tra nếu đơn đang `CANCELLED` hoặc `PAID` $\rightarrow$ Chặn bằng `BadRequestException`.
  * [ ] Lấy danh sách món từ `salesOrderItemRepository.findBySalesOrder_OrderId(orderId)`.
  * [ ] Cộng trả lại `stockQuantity = stockQuantity + item.getQuantity()`.
  * [ ] Đổi `status = CANCELLED` và lưu lại.

---

### ⚔️ LEVEL 3: Mở 7 Endpoint REST Controller Chuẩn Mã HTTP

* **Mục tiêu**: Xây dựng [SalesOrderController.java](file:///d:/Code/code/Java/Backend/SuperMarketAPI/src/main/java/com/api/supermarket/controller/SalesOrderController.java).
* **Checklist thực hiện**:
  * [ ] `POST /api/orders`: Trả về HTTP `201 CREATED`.
  * [ ] `GET /api/orders/{id}`: Trả về HTTP `200 OK`.
  * [ ] `GET /api/orders/code/{orderCode}`: Tra cứu mã hóa đơn trên giấy.
  * [ ] `GET /api/orders/customer/{customerId}`: Lịch sử mua của khách.
  * [ ] `GET /api/orders`: Phân trang & lọc theo trạng thái, từ khóa.
  * [ ] `PUT /api/orders/{id}/pay`: Xác nhận thanh toán.
  * [ ] `PUT /api/orders/{id}/cancel`: Hủy hóa đơn.

---

## 🏆 BOSS QUEST: KIỂM THỬ KỊCH BẢN MUA BÁN THỰC TẾ TRÊN POSTMAN

```text
[BƯỚC 1: Login Admin] ──► Nhận JWT Bearer Token
          │
[BƯỚC 2: Kiểm tra kho] ─► Sản phẩm ID 1 đang có stockQuantity = 10
          │
[BƯỚC 3: Tạo đơn hàng] ─► Mua 4 cái (POST /api/orders) ──► Tồn kho giảm còn 6
          │
[BƯỚC 4: Hủy đơn hàng] ─► Hủy đơn (PUT /api/orders/1/cancel) ──► Tồn kho TỰ ĐỘNG hoàn lại 10!
```
