package com.api.supermarket.controller;

import com.api.supermarket.dto.response.PageResponse;
import com.api.supermarket.dto.response.ProductResponse;
import com.api.supermarket.dto.request.ProductRequest;
import com.api.supermarket.service.ProductService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
@RestController
@RequestMapping("/api/products")
public class ProductController {
    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }


    @GetMapping
    public PageResponse<ProductResponse> getAllProducts(
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "10") int size,
        @RequestParam(defaultValue = "productId") String sortBy,
        @RequestParam(defaultValue = "asc") String sortDir,
        @RequestParam(required = false) String keyword,
        @RequestParam(required = false) Long categoryId,
        @RequestParam(required = false) Long supplierId,
        @RequestParam(required = false) Boolean active
    ){
        return productService.filterProducts(page, size, sortBy, sortDir, keyword, categoryId, supplierId, active);
    }

    @GetMapping("/{id}")
    public ProductResponse getProductById(@PathVariable Long id) {
        return productService.getProductById(id);
    }

    @GetMapping("/active")
    public List<ProductResponse> getActiveProducts() {
        return productService.getActiveProducts();
    }

    @GetMapping("/search/{keyword}")
    public List<ProductResponse> searchProductsByName(@PathVariable String keyword) {
        return productService.searchProductsByName(keyword);
    }

    @GetMapping("/sku/{skuCode}")
    public ProductResponse getProductBySkuCode(@PathVariable String skuCode) {
        return productService.getProductBySkuCode(skuCode);
    }

    @GetMapping("/sku/{skuCode}/exists")
    public boolean isSkuCodeExists(@PathVariable String skuCode) {
        return productService.isSkuCodeExists(skuCode);
    }

    @GetMapping("/category/{categoryId}")
    public List<ProductResponse> getProductsByCategoryId(@PathVariable Long categoryId) {
        return productService.getProductsByCategoryId(categoryId);
    }

    @GetMapping("/supplier/{supplierId}")
    public List<ProductResponse> getProductsBySupplierId(@PathVariable Long supplierId) {
        return productService.getProductsBySupplierId(supplierId);
    }

    @GetMapping("/category/{categoryId}/supplier/{supplierId}")
    public List<ProductResponse> getProductsByCategoryAndSupplier(@PathVariable Long categoryId, @PathVariable Long supplierId) {
        return productService.getProductsByCategoryAndSupplier(categoryId, supplierId);
    }

    @GetMapping("/low-stock")
    public List<ProductResponse> getLowStockProducts() {
        return productService.getLowStockProducts();
    }
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'WAREHOUSE')") // Chỉ cho phép ADMIN và MAN
    @PostMapping
    public ProductResponse createProduct(@Valid @RequestBody ProductRequest request) {
        return productService.createProduct(request);
    }
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'WAREHOUSE')") // Chỉ cho phép ADMIN và MANAGER thực hiện các thao tác này
    @PutMapping("/{id}")
    public ProductResponse updateProduct(@PathVariable Long id, @Valid @RequestBody ProductRequest request) {
        return productService.updateProduct(id, request);
    }
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'WAREHOUSE')") // Chỉ cho phép ADMIN và MANAGER thực hiện các thao tác này
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProduct(@PathVariable Long id) {
        productService.deleteProduct(id);
        return ResponseEntity.noContent().build();
    }
}
