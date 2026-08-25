package com.api.supermarket.controller;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.api.supermarket.dto.request.SalesOrderRequest;
import com.api.supermarket.dto.response.PageResponse;
import com.api.supermarket.dto.response.SalesOrderResponse;
import com.api.supermarket.service.SalesOrderService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/orders")
public class SalesOrderController {

    private final SalesOrderService salesOrderService;
    private static final Logger log = LoggerFactory.getLogger(SalesOrderController.class);

    public SalesOrderController(SalesOrderService salesOrderService) {
        this.salesOrderService = salesOrderService;
    }

    // 1. Tạo hóa đơn mới (POST /api/orders)
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ResponseEntity<SalesOrderResponse> createSalesOrder(@Valid @RequestBody SalesOrderRequest request) {
        log.info("Nhận yêu cầu tạo hóa đơn mới từ thu ngân");
        SalesOrderResponse response = salesOrderService.createSalesOrder(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // 2. Lấy chi tiết hóa đơn theo ID (GET /api/orders/{id})
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ResponseEntity<SalesOrderResponse> getSalesOrderById(@PathVariable Long id) {
        log.debug("Xem chi tiết hóa đơn, orderId = {}", id);
        return ResponseEntity.ok(salesOrderService.getSalesOrderById(id));
    }

    // 3. Tra cứu hóa đơn theo mã code in trên giấy (GET
    // /api/orders/code/{orderCode})
    @GetMapping("/code/{orderCode}")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ResponseEntity<SalesOrderResponse> getSalesOrderByCode(@PathVariable String orderCode) {
        log.debug("Tra cứu hóa đơn theo mã: {}", orderCode);
        return ResponseEntity.ok(salesOrderService.getSalesOrderCode(orderCode));
    }

    // 4. Xem lịch sử mua hàng của khách hàng (GET
    // /api/orders/customer/{customerId})
    @GetMapping("/customer/{customerId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ResponseEntity<List<SalesOrderResponse>> getSalesOrderHistory(@PathVariable Long customerId) {
        log.debug("Xem lịch sử mua hàng của khách hàng ID: {}", customerId);
        return ResponseEntity.ok(salesOrderService.getSalesOrderHistory(customerId));
    }

    // 5. Phân trang và lọc hóa đơn (GET /api/orders)
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ResponseEntity<PageResponse<SalesOrderResponse>> filterSalesOrders(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String paymentMethod,
            @RequestParam(required = false) String searchTerm) {
        PageResponse<SalesOrderResponse> response = salesOrderService.filterSalesOrders(page, size, sortBy, sortDir,
                status, paymentMethod, searchTerm);
        return ResponseEntity.ok(response);
    }

    // 6. Xác nhận thanh toán hóa đơn (PUT /api/orders/{id}/pay)
    @PutMapping("/{id}/pay")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ResponseEntity<SalesOrderResponse> paySalesOrder(@PathVariable Long id) {
        log.info("Xác nhận thanh toán cho hóa đơn ID: {}", id);
        return ResponseEntity.ok(salesOrderService.paySalesOrder(id));
    }

    // 7. Hủy hóa đơn & hoàn kho (PUT /api/orders/{id}/cancel)
    @PutMapping("/{id}/cancel")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ResponseEntity<SalesOrderResponse> cancelSalesOrder(@PathVariable Long id) {
        log.info("Yêu cầu hủy hóa đơn ID: {}", id);
        return ResponseEntity.ok(salesOrderService.cancelSalesOrder(id));
    }
}
