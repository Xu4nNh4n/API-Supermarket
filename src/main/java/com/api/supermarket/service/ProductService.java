package com.api.supermarket.service;
import com.api.supermarket.dto.request.ProductRequest;
import com.api.supermarket.dto.response.PageResponse;
import com.api.supermarket.dto.response.ProductResponse;
import java.util.*;

public interface ProductService {
    //Lấy danh sách sản phẩm
    List<ProductResponse> getAllProducts();

    //Lấy 1 sản phẩm theo id
    ProductResponse getProductById(Long id);

    //Thêm sản phẩm mới
    ProductResponse createProduct(ProductRequest request);

    //Cập nhật sản phẩm
    ProductResponse updateProduct(Long id, ProductRequest request);

    //Xóa sản phẩm
    void deleteProduct(Long id);

    //Tìm kiếm sản phẩm theo tên
    List<ProductResponse> searchProductsByName(String keyword);

    //Lọc sản phẩm đang hoạt động
    List<ProductResponse> getActiveProducts();

    //Lọc sản phẩm theo danh mục
    List<ProductResponse> getProductsByCategoryId(Long categoryId);

    //Lọc sản phẩm theo nhà cung cấp
    List<ProductResponse> getProductsBySupplierId(Long supplierId);

    //Lọc sản phẩm theo danh mục và nhà cung cấp
    List<ProductResponse> getProductsByCategoryAndSupplier(Long categoryId, Long supplierId);

    //Tìm sản phẩm theo mã sku
    ProductResponse getProductBySkuCode(String skuCode);

    //Kiểm tra trùng mã sku
    boolean isSkuCodeExists(String skuCode);

    //Lấy sản phẩm sắp hết hạn (stockQuantity <= reorderPoint)
    List<ProductResponse> getLowStockProducts();

    //Lấy danh sách sản phẩm với phân trang``   

    PageResponse<ProductResponse> filterProducts(
        int page,
        int size,
        String sortBy,
        String sortDir,
        String keyword,
        Long categoryId,
        Long supplierId,
        Boolean active
    );
}
