package com.api.supermarket.controller;

import com.api.supermarket.service.*;
import com.api.supermarket.dto.request.SupplierRequest;
import com.api.supermarket.dto.response.PageResponse;
import com.api.supermarket.dto.response.SupplierResponse;

import jakarta.validation.*;

import java.util.List;

import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
@RestController
@RequestMapping("/api/suppliers")
public class SupplierController {
    private final SupplierService supplierService;

    public SupplierController(SupplierService supplierService){
        this.supplierService = supplierService;
    }

    @GetMapping
    public PageResponse<SupplierResponse> getAllSuppliers(
        @RequestParam(defaultValue = "0") int page, // Trang hiện tại, mặc định là 0 (trang đầu tiên)
        @RequestParam(defaultValue = "10") int size,  // Số lượng nhà cung cấp trên mỗi trang, mặc định là 10
        @RequestParam(defaultValue = "supplierId") String sortBy, // Trường để sắp xếp, mặc định là "supplierId"
        @RequestParam(defaultValue = "asc") String sortDir, // Hướng sắp xếp, mặc định là tăng dần
        @RequestParam(required = false) String keyword, // Từ khóa tìm kiếm theo tên nhà cung cấp
        @RequestParam(required = false) String phone, // Tìm kiếm theo số điện thoại
        @RequestParam(required = false) String email, // Tìm kiếm theo email
        @RequestParam(required = false) String address, // Tìm kiếm theo địa chỉ
        @RequestParam(required = false) Boolean active // Lọc theo trạng thái hoạt động
    ){
        return supplierService.filterSuppliers(
            page,
            size,
            sortBy,
            sortDir,
            keyword,
            phone,
            email,
            address,
            active
        );
    }

    @GetMapping("/{id}")
    public SupplierResponse getSupplierbyId(@PathVariable Long id){
        return supplierService.getSupplierbyId(id);
    }

    @GetMapping("/active")
    public List<SupplierResponse> getSuppliersByIsActiveTrue(){
        return supplierService.getSuppliersByIsActiveTrue();
    }

    @GetMapping("/search/{keyword}")
    public List<SupplierResponse> getSuppliersByNameContaining(@PathVariable String keyword){
        return supplierService.getSuppliersByNameContaining(keyword);
    }
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'WAREHOUSE')") // Chỉ cho phép ADMIN và MAN
    @PostMapping
    public SupplierResponse createSupplier(@Valid @RequestBody SupplierRequest request){
        return supplierService.createSupplier(request);
    }
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'WAREHOUSE')") // Chỉ cho phép
    @PutMapping("/{id}")
    public SupplierResponse updateSupplier(@PathVariable Long id, @Valid @RequestBody SupplierRequest request){
        return supplierService.updateSupplier(id, request);
    }
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'WAREHOUSE')") // Chỉ cho phép
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteSupplier (@PathVariable Long id){
        supplierService.deleteSupplier(id);
        return ResponseEntity.noContent().build();
    }
}
