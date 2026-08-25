package com.api.supermarket.service;

import java.util.List;

import com.api.supermarket.dto.request.SalesOrderRequest;
import com.api.supermarket.dto.response.PageResponse;
import com.api.supermarket.dto.response.SalesOrderResponse;

public interface SalesOrderService {

    // Lấy 1 hóa đơn cụ thể
    SalesOrderResponse getSalesOrderById(Long orderId);

    // Lấy lịch sử mua hàng của một khách hàng
    List<SalesOrderResponse> getSalesOrderHistory(Long customerId);

    // Lấy tất cả các hóa đơn
    PageResponse<SalesOrderResponse> getAllSalesOrders();

    // Tạo hóa đơn mới
    SalesOrderResponse createSalesOrder(SalesOrderRequest request);

    // Hủy hóa đơn
    SalesOrderResponse cancelSalesOrder(Long orderId);

    // Xác nhận thanh toán
    SalesOrderResponse paySalesOrder(Long orderId);

    // Tìm Kiếm theo mã in trên hóa đơn
    SalesOrderResponse getSalesOrderCode(String orderCode);

    // Lọc hóa đơn theo trạng thái
    PageResponse<SalesOrderResponse> filterSalesOrders(
            int page,
            int size,
            String sortBy,
            String sortDir,
            String status,
            String paymentMethod,
            String searchTerm);
}
