# CẨM NANG SINH TỒN TẦNG SERVICE: TỪ RỐI SANG RÕ

Tài liệu này dành riêng cho bạn, tổng hợp **tất cả** những điểm dễ nhầm lẫn nhất khi viết tầng Service trong Spring Boot. Mỗi phần đều kèm ví dụ thực tế từ chính dự án SuperMarketAPI, giải thích bằng ngôn ngữ đời thường nhất.

---

## MỤC LỤC

1. [Phân biệt biến số ít và số nhiều (order vs orders)](#1-phân-biệt-biến-số-ít-và-số-nhiều)
2. [Hiểu bản chất `.stream().map()` qua ví dụ đời thường](#2-hiểu-bản-chất-streammap)
3. [Khi nào gọi `order.getCustomer()` và tại sao lại gọi được?](#3-khi-nào-gọi-ordergetcustomer)
4. [Bản chất của `mapToResponse` - Tại sao phải truyền bao nhiêu tham số?](#4-bản-chất-của-maptoresponse)
5. [Quy tắc Constructor: Đếm tham số không bao giờ sai](#5-quy-tắc-constructor)
6. [Bảng tra cứu nhanh: Mỗi hàm Service cần gọi những gì?](#6-bảng-tra-cứu-nhanh)
7. [Mẹo ghi nhớ trọn đời](#7-mẹo-ghi-nhớ-trọn-đời)

---

## 1. Phân Biệt Biến Số Ít Và Số Nhiều

Đây là lỗi bạn vừa mắc phải và rất nhiều người mới cũng mắc:

```java
// ❌ SAI - orders là một DANH SÁCH (List), không có hàm getOrderId()
List<SalesOrder> orders = salesOrderRepository.findByCustomer_CustomerIdOrderByCreatedAtDesc(customerId);
orders.getOrderId(); // ← LỖI: List không có getOrderId()

// ✅ ĐÚNG - order (không có s) là 1 phần tử đơn lẻ bên trong danh sách
orders.stream().map(order -> {
    order.getOrderId(); // ← ĐÚNG: phần tử đơn lẻ SalesOrder có getOrderId()
});
```

### 🎯 Quy tắc vàng: Nhìn kiểu dữ liệu, không nhìn tên biến

| Kiểu dữ liệu | Tên biến nên đặt | Có thể gọi `.getXxx()` không? |
| :--- | :--- | :--- |
| `SalesOrder` (1 đối tượng) | `order` | ✅ CÓ - Gọi được `order.getOrderId()`, `order.getCustomer()`... |
| `List<SalesOrder>` (danh sách) | `orders` | ❌ KHÔNG - Chỉ gọi được `.size()`, `.get(0)`, `.stream()`, `.forEach()`... |
| `Product` (1 đối tượng) | `product` hoặc `p` | ✅ CÓ - Gọi được `product.getPrice()`, `product.getProductName()`... |
| `List<Product>` (danh sách) | `products` | ❌ KHÔNG - Chỉ gọi được các hàm của List |

### 🏠 Ví dụ đời thường:
* `orders` = **Cả cái tủ hồ sơ** chứa 50 tờ đơn hàng. Bạn không thể hỏi: *"Tủ hồ sơ ơi, mã đơn hàng của mày là gì?"* → Vô nghĩa!
* `order` = **1 tờ đơn hàng cụ thể** lấy ra từ tủ. Bạn mới hỏi được: *"Tờ đơn này, mã đơn hàng là gì?"* → `order.getOrderId()`

---

## 2. Hiểu Bản Chất `.stream().map()`

### ❓ Bạn đang rối vì `.stream().map()` trông lạ lẫm. Hãy quên code đi, nghĩ về đời thường:

Tưởng tượng bạn có một **rổ 5 quả cam** (List) và muốn biến chúng thành **5 ly nước cam** (List mới):

```text
[Cam 1] [Cam 2] [Cam 3] [Cam 4] [Cam 5]     ← Rổ cam (List<Cam>)
   ↓       ↓       ↓       ↓       ↓          ← .stream().map(cam -> vắtNước(cam))
[Ly 1]  [Ly 2]  [Ly 3]  [Ly 4]  [Ly 5]      ← Khay nước cam (List<NướcCam>)
```

### 📝 Áp dụng vào code thực tế:

```java
// Rổ cam = Danh sách các đơn hàng Entity từ Database
List<SalesOrder> orders = salesOrderRepository.findByCustomer_CustomerIdOrderByCreatedAtDesc(customerId);

// Vắt nước cam = Chuyển từng Entity thành Response DTO
List<SalesOrderResponse> result = orders.stream().map(order -> {
    // "order" ở đây là 1 quả cam được lấy ra khỏi rổ
    // Mỗi lần lặp, order sẽ lần lượt là: orders[0], orders[1], orders[2]...
    
    List<SalesOrderItem> items = salesOrderItemRepository.findBySalesOrder_OrderId(order.getOrderId());
    return mapToResponse(order, items, customer, order.getUser());
    
}).toList();
// .toList() = Thu gom tất cả ly nước cam vào 1 khay mới
```

### 🔑 Phân tích từng phần:

| Phần code | Ý nghĩa | Ví dụ đời thường |
| :--- | :--- | :--- |
| `orders.stream()` | Bắt đầu duyệt từng phần tử trong danh sách | Bắt đầu lấy từng quả cam trong rổ |
| `.map(order -> { ... })` | Với mỗi phần tử, làm gì đó và trả ra kết quả mới | Với mỗi quả cam, vắt thành 1 ly nước cam |
| `order` | Biến đại diện cho **1 phần tử đang được xử lý** | 1 quả cam đang cầm trên tay |
| `return mapToResponse(...)` | Kết quả chuyển đổi của 1 phần tử | 1 ly nước cam đã vắt xong |
| `.toList()` | Thu gom tất cả kết quả thành danh sách mới | Xếp tất cả ly nước cam vào khay |

### ⚡ Khi nào PHẢI dùng `.stream().map()`?
Khi bạn có một **danh sách kiểu A** và muốn biến thành **danh sách kiểu B**:

```text
List<SalesOrder>     → muốn thành → List<SalesOrderResponse>     → DÙNG .stream().map()
List<SalesOrderItem> → muốn thành → List<OrderItemResponse>      → DÙNG .stream().map()
List<Product>        → muốn thành → List<ProductResponse>        → DÙNG .stream().map()
```

### ⚡ Khi nào KHÔNG cần `.stream().map()`?
Khi bạn chỉ có **1 đối tượng đơn lẻ** (không phải List):

```text
SalesOrder order → muốn thành → SalesOrderResponse → Gọi thẳng mapToResponse(order, items...)
Product product  → muốn thành → ProductResponse    → Gọi thẳng mapToResponse(product)
```

---

## 3. Khi Nào Gọi `order.getCustomer()` Và Tại Sao Lại Gọi Được?

### ❓ Câu hỏi: "Tại sao từ `order` lại lấy được `Customer` và `User`?"

Câu trả lời nằm trong **Entity `SalesOrder.java`**:

```java
@Entity
public class SalesOrder {
    @Id
    private Long orderId;
    private String orderCode;
    private String status;
    
    @ManyToOne                         // ← Annotation này nói: "Nhiều Đơn hàng thuộc về 1 Khách"
    @JoinColumn(name = "customer_id")  // ← Nối bằng cột customer_id trong bảng sales_orders
    private Customer customer;         // ← Nhờ có dòng này, ta gọi được order.getCustomer()
    
    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;                 // ← Nhờ có dòng này, ta gọi được order.getUser()
}
```

### 🏠 Ví dụ đời thường:
Tờ đơn hàng giấy ở siêu thị có ghi:
* **Mã khách hàng**: KH005
* **Nhân viên thu ngân**: NV012

Khi JPA (Hibernate) đọc tờ đơn này từ Database, nó tự động:
1. Thấy `customer_id = 5` → Chạy sang bảng `customers` tìm khách có ID = 5 → Gán vào `order.customer`
2. Thấy `user_id = 12` → Chạy sang bảng `users` tìm nhân viên có ID = 12 → Gán vào `order.user`

Vì vậy khi bạn gọi `order.getCustomer().getFullName()`, thực chất là:

```text
order          → Tờ đơn hàng
.getCustomer() → Lấy ra thông tin khách hàng (đã được JPA tự động nối bảng)
.getFullName() → Lấy tên đầy đủ của khách hàng đó
```

### ⚠️ CẨN THẬN VỚI NULL:
Nếu đơn hàng cho **khách vãng lai** (không có tài khoản), `customer_id` trong Database sẽ là `NULL`:

```java
// ❌ SAI - Nếu customer là null → NullPointerException → Server sập ngay!
order.getCustomer().getFullName()

// ✅ ĐÚNG - Luôn kiểm tra null trước khi gọi
order.getCustomer() != null ? order.getCustomer().getFullName() : "Khách vãng lai"
```

### 📋 Bảng tra cứu: Từ Entity nào có thể gọi được gì?

| Từ Entity | Gọi được | Trả về kiểu | Lý do |
| :--- | :--- | :--- | :--- |
| `order.getCustomer()` | ✅ | `Customer` | Vì SalesOrder có `@ManyToOne Customer customer` |
| `order.getUser()` | ✅ | `User` | Vì SalesOrder có `@ManyToOne User user` |
| `order.getItems()` | ❌ Không có! | - | Vì SalesOrder **không khai báo** `@OneToMany List<SalesOrderItem>` |
| `product.getCategory()` | ✅ | `Category` | Vì Product có `@ManyToOne Category category` |
| `product.getSupplier()` | ✅ | `Supplier` | Vì Product có `@ManyToOne Supplier supplier` |
| `item.getProduct()` | ✅ | `Product` | Vì SalesOrderItem có `@ManyToOne Product product` |
| `item.getSalesOrder()` | ✅ | `SalesOrder` | Vì SalesOrderItem có `@ManyToOne SalesOrder salesOrder` |

> [!IMPORTANT]
> **Quy tắc**: Bạn chỉ gọi được `entity.getXxx()` khi trong class Entity đó có khai báo trường `private Xxx xxx;` kèm annotation `@ManyToOne` hoặc `@OneToMany`. Nếu không có → Không gọi được!

---

## 4. Bản Chất Của `mapToResponse`

### ❓ Tại sao phải truyền bao nhiêu tham số?

Câu trả lời duy nhất: **Mở file `XxxResponse.java` ra đếm constructor có bao nhiêu tham số, thì truyền bấy nhiêu.**

### 📝 Ví dụ cụ thể với `SalesOrderResponse`:

```java
// Bước 1: Mở file SalesOrderResponse.java, đếm constructor
public SalesOrderResponse(
    Long orderId,           // 1
    String orderCode,       // 2
    Long customerId,        // 3
    String customerName,    // 4
    String customerPhone,   // 5
    Long staffId,           // 6
    String staffName,       // 7
    String orderType,       // 8
    String paymentMethod,   // 9
    String status,          // 10
    String note,            // 11
    BigDecimal subtotal,    // 12
    BigDecimal discountAmount, // 13
    BigDecimal totalAmount, // 14
    LocalDateTime createdAt, // 15
    LocalDateTime paidAt,   // 16
    List<OrderItemResponse> items // 17
)
```

```java
// Bước 2: Truyền VÀO ĐÚNG THỨ TỰ, ĐÚNG KIỂU DỮ LIỆU
return new SalesOrderResponse(
    order.getOrderId(),                                                    // 1. Long
    order.getOrderCode(),                                                  // 2. String
    order.getCustomer() != null ? order.getCustomer().getCustomerId() : null, // 3. Long
    order.getCustomer() != null ? order.getCustomer().getFullName() : "Khách vãng lai", // 4. String
    order.getCustomer() != null ? order.getCustomer().getPhone() : null,   // 5. String
    order.getUser() != null ? order.getUser().getUserId() : null,          // 6. Long
    order.getUser() != null ? order.getUser().getFullName() : null,        // 7. String
    order.getOrderType(),                                                  // 8. String
    order.getPaymentMethod(),                                              // 9. String
    order.getStatus(),                                                     // 10. String
    order.getNote(),                                                       // 11. String
    order.getSubtotal(),                                                   // 12. BigDecimal
    order.getDiscountAmount(),                                             // 13. BigDecimal
    order.getTotalAmount(),                                                // 14. BigDecimal
    order.getCreatedAt(),                                                  // 15. LocalDateTime
    order.getPaidAt(),                                                     // 16. LocalDateTime
    itemResponses                                                          // 17. List<OrderItemResponse>
);
```

### 🔑 Mẹo tránh sai thứ tự:
Viết số thứ tự `// 1.`, `// 2.`... ở cuối mỗi dòng rồi đối chiếu với constructor, sai dòng nào thấy ngay!

---

## 5. Quy Tắc Constructor: Đếm Tham Số Không Bao Giờ Sai

### So sánh 3 cấp độ phức tạp:

```text
CẤP 1 - Entity đơn giản (Supplier): Constructor 7 tham số
    SupplierResponse(id, name, phone, email, address, isActive, createAt)
    → Gọi thẳng: new SupplierResponse(supplier.getXxx(), ...)
    → Không cần stream().map(), không cần kiểm tra null

CẤP 2 - Entity có 1 quan hệ cha (Product → Category + Supplier): Constructor 14 tham số
    ProductResponse(id, name, sku, price, stock, reorder, expiry, image, isActive,
                    categoryId, categoryName, supplierId, supplierName, createAt)
    → Cần kiểm tra null cho category và supplier

CẤP 3 - Entity cha-con phức tạp (SalesOrder → Customer + User + List<Items>): Constructor 17 tham số
    SalesOrderResponse(orderId, orderCode, customerId, customerName, customerPhone,
                       staffId, staffName, orderType, paymentMethod, status, note,
                       subtotal, discountAmount, totalAmount, createdAt, paidAt, items)
    → Cần kiểm tra null cho customer và user
    → Cần stream().map() để chuyển đổi danh sách items
```

---

## 6. Bảng Tra Cứu Nhanh: Mỗi Hàm Service Cần Gọi Những Gì?

### Các hàm trong `SalesOrderServiceImpl`:

| Hàm | Repository nào? | Trả về kiểu? | Cần stream().map()? | Cần @Transactional? |
| :--- | :--- | :--- | :---: | :---: |
| `getSalesOrderById(Long id)` | `salesOrderRepository.findById(id)` + `salesOrderItemRepository.findBySalesOrder_OrderId(id)` | `SalesOrderResponse` | ❌ Không (1 đơn) | ❌ Không (chỉ đọc) |
| `getSalesOrderCode(String code)` | `salesOrderRepository.findByOrderCodeIgnoreCase(code)` + items | `SalesOrderResponse` | ❌ Không (1 đơn) | ❌ Không (chỉ đọc) |
| `getSalesOrderHistory(Long custId)` | `salesOrderRepository.findByCustomer_CustomerIdOrderByCreatedAtDesc(custId)` + items từng đơn | `List<SalesOrderResponse>` | ✅ CÓ (nhiều đơn) | ❌ Không (chỉ đọc) |
| `getAllSalesOrders()` | Gọi lại `filterSalesOrders(...)` với tham số mặc định | `PageResponse<...>` | ✅ CÓ (nhiều đơn) | ❌ Không (chỉ đọc) |
| `filterSalesOrders(...)` | `salesOrderRepository.filterSalesOrder(...)` + items từng đơn | `PageResponse<...>` | ✅ CÓ (nhiều đơn) | ❌ Không (chỉ đọc) |
| `createSalesOrder(request)` | `productRepository`, `salesOrderRepository.save()`, `salesOrderItemRepository.saveAll()` | `SalesOrderResponse` | ❌ Không (1 đơn) | ✅ CÓ (ghi nhiều bảng) |
| `paySalesOrder(Long id)` | `salesOrderRepository.findById()` + `.save()` | `SalesOrderResponse` | ❌ Không (1 đơn) | ✅ CÓ (cập nhật status) |
| `cancelSalesOrder(Long id)` | `salesOrderRepository` + `productRepository` (hoàn kho) | `SalesOrderResponse` | ❌ Không (1 đơn) | ✅ CÓ (cập nhật nhiều bảng) |

### Quy tắc nhận biết nhanh:

```text
Trả về 1 đối tượng (SalesOrderResponse)     → Không cần stream().map()
Trả về danh sách (List<...> hoặc PageResponse) → CẦN stream().map()

Chỉ đọc dữ liệu (GET)     → Không cần @Transactional
Ghi/Sửa/Xóa dữ liệu (POST/PUT/DELETE) → CẦN @Transactional
```

---

## 7. Mẹo Ghi Nhớ Trọn Đời

### 🧠 MẸO 1: "Danh sách thì DUYỆT, đơn lẻ thì TRUYỀN"
```text
Nếu bạn có 1 cái  (SalesOrder order)  → Truyền thẳng vào mapToResponse(order, ...)
Nếu bạn có nhiều  (List<SalesOrder>)  → Dùng .stream().map() để duyệt từng cái
```

### 🧠 MẸO 2: "Muốn gọi getter, nhìn Entity có khai báo không"
```text
Muốn gọi order.getCustomer()?
  → Mở SalesOrder.java xem có dòng "private Customer customer" không
  → CÓ → Gọi được
  → KHÔNG → Không gọi được, phải dùng Repository tìm riêng
```

### 🧠 MẸO 3: "Constructor bao nhiêu tham số, truyền bấy nhiêu"
```text
Bước 1: Mở XxxResponse.java
Bước 2: Đếm tham số trong constructor
Bước 3: Ghi số thứ tự // 1, // 2, // 3... ở cuối mỗi dòng
Bước 4: Kiểm tra kiểu dữ liệu khớp (String, Long, BigDecimal...)
```

### 🧠 MẸO 4: "Có 's' thì là List, không 's' thì là 1 cái"
```text
orders  → List<SalesOrder>  → Không có .getOrderId()
order   → SalesOrder         → CÓ .getOrderId()

items   → List<SalesOrderItem> → Không có .getQuantity()
item    → SalesOrderItem        → CÓ .getQuantity()

products → List<Product> → Không có .getPrice()
product  → Product        → CÓ .getPrice()
```

### 🧠 MẸO 5: "Null thì sập, kiểm tra trước khi gọi"
```text
Mỗi khi gọi entity.getXxx().getYyy():
  → Hỏi: "getXxx() có thể trả về null không?"
  → Nếu CÓ THỂ null (customer, user có thể vắng mặt):
      entity.getXxx() != null ? entity.getXxx().getYyy() : giáTrịMặcĐịnh
  → Nếu CHẮC CHẮN không null (orderId, status luôn có):
      Gọi thẳng entity.getXxx()
```

---

## TÓM TẮT 1 TRANG: QUY TRÌNH VIẾT BẤT KỲ HÀM SERVICE NÀO

```text
BƯỚC 1: Đọc Interface Service → Hàm cần trả về kiểu gì?
         ├─ 1 đối tượng (XxxResponse)       → findById() + mapToResponse()
         └─ Danh sách (List<XxxResponse>)    → findAll/filter() + .stream().map() + mapToResponse()

BƯỚC 2: Hàm này CÓ GHI dữ liệu không? (save, update, delete)
         ├─ CÓ  → Thêm @Transactional
         └─ KHÔNG → Bỏ qua

BƯỚC 3: Viết mapToResponse()
         ├─ Mở XxxResponse.java đếm constructor
         ├─ Truyền đúng thứ tự, đúng kiểu
         ├─ Kiểm tra null cho quan hệ @ManyToOne
         └─ Nếu có danh sách con → dùng .stream().map() bên trong

BƯỚC 4: Compile thử → Sai ở đâu sửa ở đó!
```

---

## 🎮 BỘ NHIỆM VỤ THỰC HÀNH DỰ ÁN MỚI (SERVICE QUESTS)

> 🛠️ **Dùng khi bắt đầu dự án mới**: Mở các Quest dưới đây và thực hành viết lần lượt từ tầng Service đơn giản đến phức tạp:

### ⚔️ QUEST 1: Viết CRUD Cấp 1 (Entity Đơn Giản - Supplier)
* **Nhiệm vụ**: Tạo `SupplierServiceImpl`.
* **Yêu cầu**:
  1. Viết `createSupplier(request)`: Kiểm tra trùng email/phone bằng `existsBy...` $\rightarrow$ Lưu entity $\rightarrow$ `mapToResponse`.
  2. Viết `getSupplierById(id)`: Dùng `findById(id).orElseThrow(...)`.
  3. Viết `getAllSuppliers()`: Dùng `repository.findAll().stream().map(this::mapToResponse).toList()`.

### ⚔️ QUEST 2: Viết CRUD Cấp 2 (Entity Có Quan Hệ Cha - Product)
* **Nhiệm vụ**: Tạo `ProductServiceImpl`.
* **Yêu cầu**:
  1. Kiểm tra tồn tại của `CategoryId` và `SupplierId` trước khi gán vào `Product`.
  2. Trong `mapToResponse(product)`: Luôn kiểm tra `null` cho `product.getCategory()` và `product.getSupplier()`.
  3. Viết hàm phân trang `filterProducts(pageable)` với Whitelist sắp xếp an toàn `ALLOWED_SORT_FIELDS`.

### ⚔️ QUEST 3: Viết CRUD Cấp 3 (Entity Master-Detail Phức Tạp - SalesOrder)
* **Nhiệm vụ**: Tạo `SalesOrderServiceImpl`.
* **Yêu cầu**:
  1. Gắn `@Transactional` lên `createSalesOrder`.
  2. Duyệt từng món hàng trong Request: Kiểm tra số lượng tồn kho $\rightarrow$ Trừ kho `productRepository.save()` $\rightarrow$ Tính tổng tiền.
  3. Lưu `SalesOrder` và `SalesOrderItem`.
  4. Viết `getSalesOrderHistory(customerId)`: Dùng `orders.stream().map(order -> { ... }).toList()` để nạp kèm danh sách món hàng cho từng đơn.

### 🏆 BOSS QUEST: THỬ THÁCH BẪY NULL & QUAN HỆ TRỰC CHIẾN
* 🧪 **Thử thách**: Tạo một đơn hàng cho khách vãng lai (`customer = null`).
* 📋 **Yêu cầu**: Gọi API `GET /api/orders/{id}` để xem chi tiết $\rightarrow$ *Kỳ vọng: API trả về 200 OK với trường `customerName = "Khách vãng lai"`, hoàn toàn không bị văng NullPointerException!*

