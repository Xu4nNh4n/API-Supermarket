package com.api.supermarket.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.api.supermarket.dto.request.CustomerRequest;
import com.api.supermarket.dto.response.CustomerResponse;
import com.api.supermarket.dto.response.PageResponse;
import com.api.supermarket.service.CustomerService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/customers")
public class CustomerController {

    private final CustomerService customerService;
    private static final Logger log = LoggerFactory.getLogger(CustomerController.class);

    public CustomerController(CustomerService customerService) {
        this.customerService = customerService;
    }

    // 1. Tạo khách hàng mới (POST /api/customers)
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ResponseEntity<CustomerResponse> createCustomer(@Valid @RequestBody CustomerRequest request) {
        log.info("Nhận yêu cầu tạo khách hàng mới: {}", request.getFullName());
        CustomerResponse response = customerService.createCustomer(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // 2. Lấy chi tiết khách hàng theo ID (GET /api/customers/{id})
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ResponseEntity<CustomerResponse> getCustomerById(@PathVariable Long id) {
        log.debug("Xem chi tiết khách hàng, ID = {}", id);
        return ResponseEntity.ok(customerService.getCustomerById(id));
    }

    // 3. Tìm khách hàng theo số điện thoại (GET /api/customers/phone/{phone})
    @GetMapping("/phone/{phone}")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ResponseEntity<CustomerResponse> getCustomerByPhone(@PathVariable String phone) {
        log.debug("Tìm khách hàng theo SĐT: {}", phone);
        return ResponseEntity.ok(customerService.getCustomerByPhone(phone));
    }

    // 4. Cập nhật thông tin khách hàng (PUT /api/customers/{id})
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ResponseEntity<CustomerResponse> updateCustomer(
            @PathVariable Long id,
            @Valid @RequestBody CustomerRequest request) {
        log.info("Cập nhật thông tin khách hàng ID = {}", id);
        return ResponseEntity.ok(customerService.updateCustomer(id, request));
    }

    // 5. Xóa khách hàng (DELETE /api/customers/{id})
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteCustomer(@PathVariable Long id) {
        log.info("Xóa khách hàng ID = {}", id);
        customerService.deleteCustomer(id);
        return ResponseEntity.noContent().build();
    }

    // 6. Phân trang và tìm kiếm khách hàng (GET /api/customers)
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ResponseEntity<PageResponse<CustomerResponse>> filterCustomers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "customerId") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String phone,
            @RequestParam(required = false) String email,
            @RequestParam(required = false) String address,
            @RequestParam(required = false) Boolean active) {
        log.debug("Lọc danh sách khách hàng, page={}, size={}, keyword={}", page, size, keyword);
        PageResponse<CustomerResponse> response = customerService.filterCustomers(
                page, size, sortBy, sortDir, keyword, phone, email, address, active);
        return ResponseEntity.ok(response);
    }
}
