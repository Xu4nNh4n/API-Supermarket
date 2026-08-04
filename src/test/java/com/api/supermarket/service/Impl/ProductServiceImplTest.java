package com.api.supermarket.service.Impl;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.api.supermarket.dto.request.ProductRequest;
import com.api.supermarket.dto.response.ProductResponse;
import com.api.supermarket.entity.Category;
import com.api.supermarket.entity.Product;
import com.api.supermarket.entity.Supplier;
import com.api.supermarket.exception.BadRequestException;
import com.api.supermarket.exception.ResourceNotFoundException;
import com.api.supermarket.repository.CategoryRepository;
import com.api.supermarket.repository.ProductRepository;
import com.api.supermarket.repository.SupplierRepository;

@ExtendWith(MockitoExtension.class)
public class ProductServiceImplTest {
    @Mock
    private ProductRepository productRepository;
    
    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private SupplierRepository supplierRepository;

    @InjectMocks
    private ProductServiceImpl productService;

    private ProductRequest createRequest(){
        ProductRequest request = new ProductRequest();
        request.setProductName("Sản phẩm A");
        request.setSkuCode("SKU123");
        request.setPrice(new BigDecimal("100000.00"));
        request.setStockQuantity(50);
        request.setReorderPoint(10);
        request.setExpiryDate(LocalDate.of(2024, 12, 31));
        request.setImageUrl("http://example.com/image.jpg");
        request.setCategoryId(1L);
        request.setSupplierId(1L);
        return request;
    }

    private Product createProduct(){
        Product product = new Product();
        product.setProductId(1L);
        product.setProductName("Sản phẩm A");
        product.setSkuCode("SKU123");
        product.setPrice(new BigDecimal("100000.00"));
        product.setStockQuantity(50);
        product.setReorderPoint(10);
        product.setExpiryDate(LocalDate.of(2024, 12, 31));
        product.setImageUrl("http://example.com/image.jpg");
        product.setIsActive(true);
        product.setCategory(createCategory());
        product.setSupplier(createSupplier());
        product.setCreateAt(LocalDateTime.of(2026, 7, 28, 10, 0));
        return product;
    }

    private Category createCategory() {
        Category category = new Category();
        category.setCategoryId(1L);
        category.setCategoryName("Thực phẩm");
        return category;
    }

    private Supplier createSupplier() {
        Supplier supplier = new Supplier();
        supplier.setSupplierId(1L);
        supplier.setSupplierName("Nhà cung cấp A");
        return supplier;
    }

    @Test
    void createProductShouldSucceed(){
        ProductRequest request = createRequest();
        Category category = createCategory();
        Supplier supplier = createSupplier();

        when(productRepository.existsBySkuCode(request.getSkuCode())).thenReturn(false);
        when(supplierRepository.findById(1L)).thenReturn(Optional.of(supplier));
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));
        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> {
            Product product = invocation.getArgument(0);
            product.setProductId(1L);
            return product;
        });
        // Act
        ProductResponse product = productService.createProduct(request);
        // Assert
        assertEquals(1L, product.getProductId());
        assertEquals("Sản phẩm A", product.getProductName());
        assertEquals("SKU123", product.getSkuCode());
        assertEquals(new BigDecimal("100000.00"), product.getPrice());
        assertEquals(50, product.getStockQuantity());
        assertEquals(10, product.getReorderPoint());
        assertEquals(LocalDate.of(2024, 12, 31), product.getExpiryDate());
        assertEquals(1L, product.getCategoryId());
        assertEquals("Thực phẩm", product.getCategoryName());
        assertEquals(1L, product.getSupplierId());
        assertEquals("Nhà cung cấp A", product.getSupplierName());
        assertTrue(product.getIsActive());
        assertEquals("http://example.com/image.jpg", product.getImageUrl());
        verify(productRepository).save(any(Product.class));
    }

    @Test
    void duplicateSkuShouldThrowBadRequest(){
        ProductRequest request = createRequest();
        when(productRepository.existsBySkuCode(request.getSkuCode())).thenReturn(true);
        // Act & Assert
        assertThrows(BadRequestException.class, () -> productService.createProduct(request));
        verify(productRepository, never()).save(any());
    }

    @Test
    void categoryNotFoundShouldThrowResourceNotFound() {
        ProductRequest request = createRequest();

        when(productRepository.existsBySkuCode(request.getSkuCode()))
            .thenReturn(false);
        when(categoryRepository.findById(request.getCategoryId()))
            .thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(
            ResourceNotFoundException.class,
            () -> productService.createProduct(request)
        );

        assertEquals("Danh mục không tồn tại", exception.getMessage());
        verify(supplierRepository, never()).findById(any());
        verify(productRepository, never()).save(any());
    }

    @Test
    void supplierNotFoundShouldThrowResourceNotFound() {
        ProductRequest request = createRequest();

        when(productRepository.existsBySkuCode(request.getSkuCode()))
            .thenReturn(false);
        when(categoryRepository.findById(request.getCategoryId()))
            .thenReturn(Optional.of(createCategory()));
        when(supplierRepository.findById(request.getSupplierId()))
            .thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(
            ResourceNotFoundException.class,
            () -> productService.createProduct(request)
        );

        assertEquals("Nhà cung cấp không tồn tại", exception.getMessage());
        verify(productRepository, never()).save(any());
    }

    @Test
    void productNotFoundShouldThrowResourceNotFound() {
        when(productRepository.findById(99L))
            .thenReturn(Optional.empty());

        assertThrows(
            ResourceNotFoundException.class,
            () -> productService.getProductById(99L)
        );
    }

    @Test
    void updateWithoutIsActiveShouldKeepOldValue() {
        Product existingProduct = createProduct();
        existingProduct.setIsActive(false);

        ProductRequest request = createRequest();
        request.setIsActive(null);

        when(productRepository.findById(1L))
            .thenReturn(Optional.of(existingProduct));
        when(categoryRepository.findById(1L))
            .thenReturn(Optional.of(createCategory()));
        when(supplierRepository.findById(1L))
            .thenReturn(Optional.of(createSupplier()));
        when(productRepository.save(existingProduct))
            .thenReturn(existingProduct);

        ProductResponse response = productService.updateProduct(1L, request);

        assertFalse(response.getIsActive());
        verify(productRepository).save(existingProduct);
    }

    @Test
    void mapToProductResponseShouldReturnCorrectData() {
        Product product = createProduct();
        when(productRepository.findById(1L))
            .thenReturn(Optional.of(product));

        ProductResponse response = productService.getProductById(1L);

        assertEquals(product.getProductId(), response.getProductId());
        assertEquals(product.getProductName(), response.getProductName());
        assertEquals(product.getPrice(), response.getPrice());
        assertEquals(product.getCategory().getCategoryName(), response.getCategoryName());
        assertEquals(product.getSupplier().getSupplierName(), response.getSupplierName());
        assertEquals(product.getCreateAt(), response.getCreateAt());
    }

    @Test
    void deleteProductShouldSucceed() {
        Product product = createProduct();
        when(productRepository.findById(1L))
            .thenReturn(Optional.of(product));

        productService.deleteProduct(1L);

        verify(productRepository).delete(product);
    }
}
