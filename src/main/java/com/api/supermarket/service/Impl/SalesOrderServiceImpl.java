package com.api.supermarket.service.Impl;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.api.supermarket.dto.request.OrderItemRequest;
import com.api.supermarket.dto.request.SalesOrderRequest;
import com.api.supermarket.dto.response.OrderItemResponse;
import com.api.supermarket.dto.response.PageResponse;
import com.api.supermarket.dto.response.ProductResponse;
import com.api.supermarket.dto.response.SalesOrderResponse;
import com.api.supermarket.entity.Customer;
import com.api.supermarket.entity.Product;
import com.api.supermarket.entity.SalesOrder;
import com.api.supermarket.entity.SalesOrderItem;
import com.api.supermarket.entity.User;
import com.api.supermarket.exception.BadRequestException;
import com.api.supermarket.exception.InsufficientStockException;
import com.api.supermarket.exception.ResourceNotFoundException;
import com.api.supermarket.repository.CustomerRepository;
import com.api.supermarket.repository.ProductRepository;
import com.api.supermarket.repository.SalesOrderItemRepository;
import com.api.supermarket.repository.SalesOrderRepository;
import com.api.supermarket.repository.UserRepository;
import com.api.supermarket.service.SalesOrderService;
import com.api.supermarket.validation.PaginationValidator;

/**
 * 🧾 SalesOrderServiceImpl - TRÁI TIM NGHIỆP VỤ BÁN HÀNG
 * 
 * 💡 TẠI SAO SERVICE NÀY PHỨC TẠP ("ĐIÊN") HƠN CÁC SERVICE KHÁC?
 * 1. Tiêm tới 5 Repository: Phải điều phối đồng thời SalesOrder, SalesOrderItem, Product, Customer, User.
 * 2. Tính toàn vẹn Kho hàng: Trừ kho ngay khi tạo đơn, ném lỗi rollback nếu thiếu hàng, hoàn kho khi hủy đơn.
 * 3. Bảo mật giá: Tuyệt đối không tin giá gửi từ Client, bắt buộc bốc giá gốc từ DB.
 * 4. Máy trạng thái (State Machine): Chặt chẽ PENDING -> PAID hoặc PENDING -> CANCELLED (cấm thanh toán đơn hủy).
 */
@Service
public class SalesOrderServiceImpl implements SalesOrderService {

    private final SalesOrderItemRepository salesOrderItemRepository;
    private final SalesOrderRepository salesOrderRepository;
    private final CustomerRepository customerRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;

    // Constructor Injection: Spring tự động tiêm đầy đủ 5 repository cần thiết
    public SalesOrderServiceImpl(
            SalesOrderRepository salesOrderRepository,
            CustomerRepository customerRepository,
            ProductRepository productRepository,
            SalesOrderItemRepository salesOrderItemRepository,
            UserRepository userRepository) {
        this.salesOrderRepository = salesOrderRepository;
        this.customerRepository = customerRepository;
        this.productRepository = productRepository;
        this.salesOrderItemRepository = salesOrderItemRepository;
        this.userRepository = userRepository;
    }

    /**
     * 🛠️ Helper Mapper: Chuyển đổi SalesOrder Entity sang SalesOrderResponse DTO
     * - An toàn Null-Safety: Nếu khách lẻ không có thông tin -> Hiển thị "Khách vãng lai".
     * - Lồng DTO chi tiết: Duyệt từng món hàng SalesOrderItem sang OrderItemResponse kèm ProductResponse.
     */
    private SalesOrderResponse mapToResponse(SalesOrder order, List<SalesOrderItem> items, Customer customer, User user) {
        List<OrderItemResponse> itemResponses = (items != null) ? items.stream().map(item -> {
            Product p = item.getProduct();
            ProductResponse productResponse = new ProductResponse(
                    p.getProductId(),
                    p.getProductName(),
                    p.getSkuCode(),
                    p.getPrice(),
                    p.getStockQuantity(),
                    p.getReorderPoint(),
                    p.getExpiryDate(),
                    p.getImageUrl(),
                    p.getIsActive(),
                    p.getCategory() != null ? p.getCategory().getCategoryId() : null,
                    p.getCategory() != null ? p.getCategory().getCategoryName() : null,
                    p.getSupplier() != null ? p.getSupplier().getSupplierId() : null,
                    p.getSupplier() != null ? p.getSupplier().getSupplierName() : null,
                    null);
            return new OrderItemResponse(
                    item.getOrderItemId(),
                    productResponse,
                    item.getQuantity(),
                    item.getUnitPrice(),
                    item.getSubtotal());
        }).toList() : new ArrayList<>();

        return new SalesOrderResponse(
                order.getOrderId(),
                order.getOrderCode(),
                order.getCustomer() != null ? order.getCustomer().getCustomerId() : null,
                order.getCustomer() != null ? order.getCustomer().getFullName() : "Khách vãng lai",
                order.getCustomer() != null ? order.getCustomer().getPhone() : null,
                order.getUser() != null ? order.getUser().getUserId() : null,
                order.getUser() != null ? order.getUser().getFullName() : null,
                order.getOrderType(),
                order.getPaymentMethod(),
                order.getStatus(),
                order.getNote(),
                order.getSubtotal(),
                order.getDiscountAmount(),
                order.getTotalAmount(),
                order.getCreatedAt(),
                order.getPaidAt(),
                itemResponses);
    }

    /**
     * 🔍 Lấy thông tin chi tiết 1 hóa đơn theo ID (kèm danh sách món hàng)
     */
    @Override
    public SalesOrderResponse getSalesOrderById(Long orderId) {
        SalesOrder order = salesOrderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy hóa đơn với ID: " + orderId));
        List<SalesOrderItem> items = salesOrderItemRepository.findBySalesOrder_OrderId(orderId);
        return mapToResponse(order, items, order.getCustomer(), order.getUser());
    }

    /**
     * 📜 Lấy lịch sử mua hàng của 1 khách hàng (sắp xếp mới nhất lên đầu)
     */
    @Override
    public List<SalesOrderResponse> getSalesOrderHistory(Long customerId) {
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy khách hàng với ID: " + customerId));
        List<SalesOrder> orders = salesOrderRepository.findByCustomer_CustomerIdOrderByCreatedAtDesc(customerId);

        return orders.stream().map(order -> {
            List<SalesOrderItem> items = salesOrderItemRepository.findBySalesOrder_OrderId(order.getOrderId());
            return mapToResponse(order, items, customer, order.getUser());
        }).toList();
    }

    /**
     * 🛒 TẠO HÓA ĐƠN MỚI (LUỒNG QUAN TRỌNG NHẤT HỆ THỐNG)
     * 
     * ⚠️ TẠI SAO PHẢI CÓ @Transactional?
     * Nếu đơn hàng có 5 sản phẩm, đến sản phẩm thứ 5 bị hết hàng hoặc đứt mạng:
     * -> @Transactional sẽ tự động ROLLBACK toàn bộ việc trừ kho của 4 sản phẩm trước đó!
     */
    @Override
    @Transactional
    public SalesOrderResponse createSalesOrder(SalesOrderRequest request) {
        // BƯỚC 1: Lấy Username của nhân viên thu ngân từ SecurityContext (Token JWT)
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String currentUsername = (auth != null && auth.getName() != null) ? auth.getName() : "admin";
        User user = userRepository.findByUserName(currentUsername)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng: " + currentUsername));

        // BƯỚC 2: Tìm khách hàng thành viên nếu có truyền customerId (nếu null -> khách vãng lai)
        Customer customer = null;
        if (request.getCustomerId() != null) {
            customer = customerRepository.findById(request.getCustomerId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Không tìm thấy khách hàng với ID: " + request.getCustomerId()));
        }

        // BƯỚC 3: Sinh mã hóa đơn tự động duy nhất (Ví dụ: HD20260826-153022)
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"));
        String orderCode = "HD" + timestamp;

        // BƯỚC 4: Khởi tạo Entity SalesOrder Master
        SalesOrder order = new SalesOrder();
        order.setOrderCode(orderCode);
        order.setOrderDate(LocalDateTime.now());
        order.setCustomer(customer);
        order.setUser(user);
        order.setOrderType(request.getOrderType());
        order.setPaymentMethod(request.getPaymentMethod());
        order.setNote(request.getNote());
        order.setStatus("PENDING"); // Mặc định đơn vừa tạo là PENDING (Chờ thanh toán)

        BigDecimal subtotal = BigDecimal.ZERO;
        List<SalesOrderItem> orderItems = new ArrayList<>();

        // BƯỚC 5: Duyệt từng món hàng trong Request -> Soi kho -> Trừ kho -> Tính tiền
        for (OrderItemRequest itemReq : request.getItems()) {
            Product product = productRepository.findById(itemReq.getProductId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Không tìm thấy sản phẩm với ID: " + itemReq.getProductId()));

            // 5.1. Soi tồn kho thực tế
            if (product.getStockQuantity() < itemReq.getQuantity()) {
                throw new InsufficientStockException(
                        "Sản phẩm '" + product.getProductName() + "' không đủ tồn kho! (Còn "
                                + product.getStockQuantity() + ", yêu cầu " + itemReq.getQuantity() + ")");
            }

            // 5.2. Trừ trực tiếp số lượng tồn kho của sản phẩm
            product.setStockQuantity(product.getStockQuantity() - itemReq.getQuantity());
            productRepository.save(product);

            // 5.3. Bốc đơn giá gốc từ Product trong DB (CHỐNG HACK SỬA GIÁ TỪ CLIENT)
            BigDecimal unitPrice = product.getPrice();
            BigDecimal itemSubtotal = unitPrice.multiply(BigDecimal.valueOf(itemReq.getQuantity()));
            subtotal = subtotal.add(itemSubtotal);

            // 5.4. Tạo bản ghi chi tiết SalesOrderItem
            SalesOrderItem orderItem = new SalesOrderItem();
            orderItem.setSalesOrder(order);
            orderItem.setProduct(product);
            orderItem.setQuantity(itemReq.getQuantity());
            orderItem.setUnitPrice(unitPrice);
            orderItem.setSubtotal(itemSubtotal);
            orderItems.add(orderItem);
        }

        // BƯỚC 6: Tính toán tổng tiền hóa đơn sau giảm giá
        BigDecimal discountAmount = BigDecimal.ZERO; // Sau này có thể tích hợp Voucher / Điểm thưởng
        BigDecimal totalAmount = subtotal.subtract(discountAmount);

        order.setSubtotal(subtotal);
        order.setDiscountAmount(discountAmount);
        order.setTotalAmount(totalAmount);

        // BƯỚC 7: Lưu SalesOrder và danh sách SalesOrderItem vào CSDL
        SalesOrder savedOrder = salesOrderRepository.save(order);
        List<SalesOrderItem> savedItems = salesOrderItemRepository.saveAll(orderItems);

        return mapToResponse(savedOrder, savedItems, customer, user);
    }

    /**
     * 💳 XÁC NHẬN THANH TOÁN HÓA ĐƠN (CHUYỂN SANG TRẠNG THÁI PAID)
     * - Kiểm tra State Machine: Chỉ đơn PENDING mới được thanh toán.
     * - Cấm thanh toán lại đơn đã PAID hoặc đơn đã bị CANCELLED.
     */
    @Override
    @Transactional
    public SalesOrderResponse paySalesOrder(Long orderId) {
        SalesOrder order = salesOrderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy hóa đơn với ID: " + orderId));

        if ("PAID".equalsIgnoreCase(order.getStatus())) {
            throw new BadRequestException("Hóa đơn này đã được thanh toán trước đó!");
        }

        if ("CANCELLED".equalsIgnoreCase(order.getStatus())) {
            throw new BadRequestException("Không thể thanh toán hóa đơn đã bị hủy!");
        }

        // Cập nhật trạng thái PAID và lưu mốc thời gian thanh toán thực tế
        order.setStatus("PAID");
        order.setPaidAt(LocalDateTime.now());
        SalesOrder updatedOrder = salesOrderRepository.save(order);

        List<SalesOrderItem> items = salesOrderItemRepository.findBySalesOrder_OrderId(orderId);
        return mapToResponse(updatedOrder, items, order.getCustomer(), order.getUser());
    }

    /**
     * ❌ HỦY HÓA ĐƠN & TỰ ĐỘNG HOÀN TRẢ LẠI TỒN KHO
     * - Kiểm tra State Machine: Không được hủy đơn đã thanh toán (PAID) hoặc đơn đã hủy trước đó.
     * - Duyệt toàn bộ món hàng trong đơn -> Cộng trả lại tồn kho vào bảng products.
     */
    @Override
    @Transactional
    public SalesOrderResponse cancelSalesOrder(Long orderId) {
        SalesOrder order = salesOrderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy hóa đơn với ID: " + orderId));

        if ("CANCELLED".equalsIgnoreCase(order.getStatus())) {
            throw new BadRequestException("Hóa đơn này đã bị hủy!");
        }

        if ("PAID".equalsIgnoreCase(order.getStatus())) {
            throw new BadRequestException("Không thể hủy hóa đơn đã được thanh toán!");
        }

        // Hoàn trả lại chính xác số lượng tồn kho từng món cho bảng Product
        List<SalesOrderItem> items = salesOrderItemRepository.findBySalesOrder_OrderId(orderId);
        for (SalesOrderItem item : items) {
            Product product = item.getProduct();
            if (product != null) {
                product.setStockQuantity(product.getStockQuantity() + item.getQuantity());
                productRepository.save(product);
            }
        }

        order.setStatus("CANCELLED");
        SalesOrder updatedOrder = salesOrderRepository.save(order);
        return mapToResponse(updatedOrder, items, order.getCustomer(), order.getUser());
    }

    /**
     * 🏷️ Tra cứu hóa đơn theo mã in trên giấy (Order Code)
     */
    @Override
    public SalesOrderResponse getSalesOrderCode(String orderCode) {
        SalesOrder order = salesOrderRepository.findByOrderCodeIgnoreCase(orderCode)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy hóa đơn với mã: " + orderCode));
        List<SalesOrderItem> items = salesOrderItemRepository.findBySalesOrder_OrderId(order.getOrderId());
        return mapToResponse(order, items, order.getCustomer(), order.getUser());
    }

    /**
     * 📄 Xem toàn bộ hóa đơn (Phân trang mặc định trang 0, 10 đơn, mới nhất)
     */
    @Override
    public PageResponse<SalesOrderResponse> getAllSalesOrders() {
        return filterSalesOrders(0, 10, "createdAt", "desc", null, null, null);
    }

    // Whitelist danh sách các cột được phép sắp xếp để chống SQL Injection
    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of(
            "orderId", "orderCode", "orderDate", "subtotal", "totalAmount", "status", "createdAt");

    /**
     * 🔎 Lọc và tìm kiếm nâng cao hóa đơn (Trạng thái, Phương thức, Từ khóa)
     */
    @Override
    public PageResponse<SalesOrderResponse> filterSalesOrders(
            int page, int size, String sortBy, String sortDir,
            String status, String paymentMethod, String searchTerm) {

        // Validate tham số phân trang và cột sắp xếp
        PaginationValidator.validate(page, size, sortBy, sortDir, ALLOWED_SORT_FIELDS);

        Sort sort = sortDir.equalsIgnoreCase("asc") ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<SalesOrder> orderPage = salesOrderRepository.filterSalesOrder(
                searchTerm, status, null, paymentMethod, searchTerm, searchTerm, pageable);

        List<SalesOrderResponse> content = orderPage.getContent().stream().map(order -> {
            List<SalesOrderItem> items = salesOrderItemRepository.findBySalesOrder_OrderId(order.getOrderId());
            return mapToResponse(order, items, order.getCustomer(), order.getUser());
        }).toList();

        return new PageResponse<>(
                content,
                orderPage.getNumber(),
                orderPage.getSize(),
                orderPage.getTotalElements(),
                orderPage.getTotalPages(),
                orderPage.isLast());
    }
}
