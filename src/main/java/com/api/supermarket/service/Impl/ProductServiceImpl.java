package com.api.supermarket.service.Impl;

import com.api.supermarket.dto.request.ProductRequest;
import com.api.supermarket.dto.response.PageResponse;
import com.api.supermarket.dto.response.ProductResponse;
import com.api.supermarket.entity.*;
import com.api.supermarket.exception.BadRequestException;
import com.api.supermarket.exception.ResourceNotFoundException;
import com.api.supermarket.repository.*;
import com.api.supermarket.service.*;
import com.api.supermarket.validation.PaginationValidator;

import org.springframework.stereotype.*;
import java.util.*;

import org.springframework.data.domain.Page; // Page là một interface trong Spring Data JPA, đại diện cho một trang dữ liệu được phân trang. Nó chứa thông tin về nội dung của trang, số trang hiện tại, tổng số trang, tổng số phần tử và các thông tin phân trang khác.
import org.springframework.data.domain.PageRequest; // PageRequest là một lớp trong Spring Data JPA, dùng để tạo đối tượng Pageable với thông tin về số trang, kích thước trang và sắp xếp dữ liệu.
import org.springframework.data.domain.Pageable; // Pageeable là một interface trong Spring Data JPA, dùng để xác định thông tin phân trang và sắp xếp dữ liệu khi truy vấn cơ sở dữ liệu.
import org.springframework.data.domain.Sort; // Sort là một lớp trong Spring Data JPA, dùng để xác định thông tin sắp xếp dữ liệu khi truy vấn cơ sở dữ liệu.
@Service
public class ProductServiceImpl implements ProductService {
    // Repository dùng để thao tác trực tiếp với bảng product trong database.
    // Service cần repository để tách phần xử lý nghiệp vụ khỏi phần truy vấn dữ liệu
    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final SupplierRepository supplierRepository;

    // Constructor injection: Spring sẽ tự truyền ProductRepository vào đây.
    // Cách này giúp service dùng được repository mà không cần tự new object.
    public ProductServiceImpl(ProductRepository productRepository, CategoryRepository categoryRepository, SupplierRepository supplierRepository){
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
        this.supplierRepository = supplierRepository;
    }

    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of(
        "productId",
        "productName",
        "skuCode",
        "categoryId",
        "supplierId",
        "price",
        "stockQuantity",
        "isActive",
        "createAt"
    );

    private ProductResponse mapToResponse(Product product) {
        return new ProductResponse(
            product.getProductId(),
            product.getProductName(),
            product.getSkuCode(),
            product.getPrice(),
            product.getStockQuantity(),
            product.getReorderPoint(),
            product.getExpiryDate(),
            product.getImageUrl(),
            product.getIsActive(),
            product.getCategory().getCategoryId(),
            product.getCategory().getCategoryName(),
            product.getSupplier().getSupplierId(),
            product.getSupplier().getSupplierName(),
            product.getCreateAt()
        );
    }

    // Các phương thức còn lại của ProductServiceImpl sẽ được triển khai ở đây, bao gồm các phương thức CRUD và các phương thức tìm kiếm/lọc sản phẩm.
    //Get tất cả sản phẩm
    @Override
    public List<ProductResponse> getAllProducts() {
        return productRepository.findAll().stream().map(this::mapToResponse).toList();
    }
    //Get sản phẩm theo id
    @Override
    public ProductResponse getProductById(Long id) {
        Product product = productRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy sản phẩm"));
        return mapToResponse(product);
    }
    //Thêm sản phẩm mới
    @Override
    public ProductResponse createProduct(ProductRequest request) {
        // Kiểm tra xem mã SKU đã tồn tại chưa
        if (productRepository.existsBySkuCode(request.getSkuCode())) {
            throw new BadRequestException("Mã SKU đã tồn tại");
        }
        Category category = categoryRepository.findById(request.getCategoryId()).orElseThrow(() -> new ResourceNotFoundException("Danh mục không tồn tại"));
        Supplier supplier = supplierRepository.findById(request.getSupplierId()).orElseThrow(() -> new ResourceNotFoundException("Nhà cung cấp không tồn tại"));
        // Tạo entity mới vì database lưu Product, không lưu trực tiếp ProductRequest.
        Product product = new Product();
        product.setSkuCode(request.getSkuCode());
        product.setProductName(request.getProductName());
        product.setPrice(request.getPrice());
        product.setStockQuantity(request.getStockQuantity());
        if (request.getReorderPoint() != null) {
            product.setReorderPoint(request.getReorderPoint());
        }
        product.setExpiryDate(request.getExpiryDate());
        product.setImageUrl(request.getImageUrl());
        product.setIsActive(request.getIsActive() == null || request.getIsActive());
        product.setCategory(category);
        product.setSupplier(supplier);

        return mapToResponse(productRepository.save(product));
    }
    //Cập nhật sản phẩm

    @Override
    public ProductResponse updateProduct(Long id, ProductRequest request) {
        Product product = productRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy sản phẩm"));

        // Kiểm tra xem mã SKU đã tồn tại chưa (ngoại trừ sản phẩm hiện tại)
        if (!product.getSkuCode().equals(request.getSkuCode()) && productRepository.existsBySkuCode(request.getSkuCode())) {
            throw new BadRequestException("Mã SKU đã tồn tại");
        }

        Category category = categoryRepository.findById(request.getCategoryId()).orElseThrow(() -> new ResourceNotFoundException("Danh mục không tồn tại"));
        Supplier supplier = supplierRepository.findById(request.getSupplierId()).orElseThrow(() -> new ResourceNotFoundException("Nhà cung cấp không tồn tại"));

        // Cập nhật thông tin sản phẩm
        product.setSkuCode(request.getSkuCode());
        product.setProductName(request.getProductName());
        product.setPrice(request.getPrice());
        product.setStockQuantity(request.getStockQuantity());
        if (request.getReorderPoint() != null) {
            product.setReorderPoint(request.getReorderPoint());
        }
        product.setExpiryDate(request.getExpiryDate());
        product.setImageUrl(request.getImageUrl());
        product.setIsActive(request.getIsActive() != null ? request.getIsActive() : product.getIsActive());
        product.setCategory(category);
        product.setSupplier(supplier);

        return mapToResponse(productRepository.save(product));
    }

    //Xóa sản phẩm
    @Override
    public void deleteProduct(Long id) {
        Product product = productRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy sản phẩm"));
        productRepository.delete(product);
    }

    //Lấy danh sách sản phẩm đang hoạt động
    @Override
    public List<ProductResponse> getActiveProducts() {
        return productRepository.findByIsActiveTrue().stream().map(this::mapToResponse).toList();
    }
    //Tìm kiếm sản phẩm theo tên
    @Override
    public List<ProductResponse> searchProductsByName(String keyword) {
        return productRepository.findByProductNameContaining(keyword).stream().map(this::mapToResponse).toList();
    }

    //Tìm kiếm sản phẩm theo mã SKU
    @Override
    public ProductResponse getProductBySkuCode(String skuCode) {
        Product product = productRepository.findBySkuCode(skuCode);
        if (product == null) {
            throw new ResourceNotFoundException("Không tìm thấy sản phẩm");
        }
        return mapToResponse(product);
    }

    //Kiểm tra mã SKU đã tồn tại chưa
    @Override
    public boolean isSkuCodeExists(String skuCode) {
        return productRepository.existsBySkuCode(skuCode);
    }

    //Lấy sản phẩm theo danh mục
    @Override
    public List<ProductResponse> getProductsByCategoryId(Long categoryId) {
        return productRepository.findByCategory_CategoryId(categoryId).stream().map(this::mapToResponse).toList();
    }

    //Lấy sản phẩm theo nhà cung cấp
    @Override
    public List<ProductResponse> getProductsBySupplierId(Long supplierId) {
        return productRepository.findBySupplier_SupplierId(supplierId).stream().map(this::mapToResponse).toList();
    }

    //Lấy sản phẩm theo danh mục và nhà cung cấp
    @Override
    public List<ProductResponse> getProductsByCategoryAndSupplier(Long categoryId, Long supplierId) {
        return productRepository.findByCategory_CategoryIdAndSupplier_SupplierId(categoryId, supplierId).stream().map(this::mapToResponse).toList();
    }

    //Lấy sản phẩm sắp hết hạn (stockQuantity <= reorderPoint)
    @Override
    public List<ProductResponse> getLowStockProducts() {
        return productRepository.findLowStockProducts().stream().map(this::mapToResponse).toList();
    }

        @Override
        public PageResponse<ProductResponse> filterProducts(
            int page,
            int size,
            String sortBy,
            String sortDir,
            String keyword,
            Long categoryId,
            Long supplierId,
            Boolean active
        ) {

            PaginationValidator.validate(page, size, sortBy, sortDir, ALLOWED_SORT_FIELDS);

            String sortProperty = switch (sortBy){
                case "categoryId" -> "category.categoryId";
                case "supplierId" -> "supplier.supplierId";
                default -> sortBy;
            };
            
            Sort sort = sortDir.equalsIgnoreCase("desc") 
            ? Sort.by(sortProperty).descending()
            : Sort.by(sortProperty).ascending();
            Pageable pageable = PageRequest.of(page, size, sort);

            Page<Product> productPage = productRepository.filterProducts(
            keyword,
            categoryId,
            supplierId,
            active,
            pageable
            );
            List<ProductResponse> products = productPage.getContent()
                .stream()
                .map(this::mapToResponse)
                .toList();

            return new PageResponse<>(
                products,
                productPage.getNumber(),
                productPage.getSize(),
                productPage.getTotalElements(),
                productPage.getTotalPages(),
                productPage.isLast()
            );
        }
}
