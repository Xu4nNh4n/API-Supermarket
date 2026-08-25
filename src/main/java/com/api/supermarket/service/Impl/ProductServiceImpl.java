package com.api.supermarket.service.Impl;

import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import com.api.supermarket.dto.request.ProductRequest;
import com.api.supermarket.dto.response.PageResponse;
import com.api.supermarket.dto.response.ProductResponse;
import com.api.supermarket.entity.Category;
import com.api.supermarket.entity.Product;
import com.api.supermarket.entity.Supplier;
import com.api.supermarket.exception.BadRequestException;
import com.api.supermarket.exception.ResourceNotFoundException;
import com.api.supermarket.repository.CategoryRepository;
import com.api.supermarket.repository.ProductRepository;
import com.api.supermarket.repository.SupplierRepository;
import com.api.supermarket.service.ProductService;
import com.api.supermarket.validation.PaginationValidator;

@Service
public class ProductServiceImpl implements ProductService {
    // Repository dùng để thao tác trực tiếp với bảng product trong database.
    // Service cần repository để tách phần xử lý nghiệp vụ khỏi phần truy vấn dữ
    // liệu
    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final SupplierRepository supplierRepository;

    private static final Logger log = LoggerFactory.getLogger(ProductServiceImpl.class);

    // Constructor injection: Spring sẽ tự truyền ProductRepository vào đây.
    // Cách này giúp service dùng được repository mà không cần tự new object.
    public ProductServiceImpl(ProductRepository productRepository, CategoryRepository categoryRepository,
            SupplierRepository supplierRepository) {
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
            "createAt");

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
                product.getCategory() != null ? product.getCategory().getCategoryId() : null,
                product.getCategory() != null ? product.getCategory().getCategoryName() : null,
                product.getSupplier() != null ? product.getSupplier().getSupplierId() : null,
                product.getSupplier() != null ? product.getSupplier().getSupplierName() : null,
                product.getCreateAt());
    }

    // Các phương thức còn lại của ProductServiceImpl sẽ được triển khai ở đây, bao
    // gồm các phương thức CRUD và các phương thức tìm kiếm/lọc sản phẩm.
    // Get tất cả sản phẩm
    @Override
    public List<ProductResponse> getAllProducts() {
        return productRepository.findAll().stream().map(this::mapToResponse).toList();
    }

    // Get sản phẩm theo id
    @Override
    public ProductResponse getProductById(Long id) {
        log.debug("Loading product, productId={}", id);

        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy sản phẩm"));

        log.debug("Product found, productId={}", id);

        return mapToResponse(product);
    }

    // Thêm sản phẩm mới
    @Override
    public ProductResponse createProduct(ProductRequest request) {
        // Kiểm tra xem mã SKU đã tồn tại chưa
        if (productRepository.existsBySkuCode(request.getSkuCode())) {
            throw new BadRequestException("Mã SKU đã tồn tại");
        }
        log.debug("Creating product, productName={}, productSku={}", request.getProductName(), request.getSkuCode());
        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Danh mục không tồn tại"));
        Supplier supplier = supplierRepository.findById(request.getSupplierId())
                .orElseThrow(() -> new ResourceNotFoundException("Nhà cung cấp không tồn tại"));
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
        Product saveProduct = productRepository.save(product);
        log.info("Product created successfully, productId={}", saveProduct.getProductId());
        return mapToResponse(saveProduct);
    }
    // Cập nhật sản phẩm

    @Override
    public ProductResponse updateProduct(Long id, ProductRequest request) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy sản phẩm"));

        // Kiểm tra xem mã SKU đã tồn tại chưa (ngoại trừ sản phẩm hiện tại)
        if (!product.getSkuCode().equals(request.getSkuCode())
                && productRepository.existsBySkuCode(request.getSkuCode())) {
            throw new BadRequestException("Mã SKU đã tồn tại");
        }
        log.debug("Updating product, productId={}, productName={}, productSku={}", id, request.getProductName(),
                request.getSkuCode());

        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Danh mục không tồn tại"));
        Supplier supplier = supplierRepository.findById(request.getSupplierId())
                .orElseThrow(() -> new ResourceNotFoundException("Nhà cung cấp không tồn tại"));

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

        Product updatedProduct = productRepository.save(product);
        log.info("Product updated successfully, productId={}", updatedProduct.getProductId());
        return mapToResponse(updatedProduct);
    }

    // Xóa sản phẩm
    @Override
    public void deleteProduct(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy sản phẩm"));
        log.debug("Deleting product, productId={}, productName={}, productSku={}", id, product.getProductName(),
                product.getSkuCode());
        productRepository.delete(product);
        log.info("Product deleted successfully, productId={}", id);
    }

    // Lấy danh sách sản phẩm đang hoạt động
    @Override
    public List<ProductResponse> getActiveProducts() {
        return productRepository.findByIsActiveTrue().stream().map(this::mapToResponse).toList();
    }

    // Tìm kiếm sản phẩm theo tên
    @Override
    public List<ProductResponse> searchProductsByName(String keyword) {
        return productRepository.findByProductNameContaining(keyword).stream().map(this::mapToResponse).toList();
    }

    // Tìm kiếm sản phẩm theo mã SKU
    @Override
    public ProductResponse getProductBySkuCode(String skuCode) {
        Product product = productRepository.findBySkuCode(skuCode);
        if (product == null) {
            throw new ResourceNotFoundException("Không tìm thấy sản phẩm");
        }
        return mapToResponse(product);
    }

    // Kiểm tra mã SKU đã tồn tại chưa
    @Override
    public boolean isSkuCodeExists(String skuCode) {
        return productRepository.existsBySkuCode(skuCode);
    }

    // Lấy sản phẩm theo danh mục
    @Override
    public List<ProductResponse> getProductsByCategoryId(Long categoryId) {
        return productRepository.findByCategory_CategoryId(categoryId).stream().map(this::mapToResponse).toList();
    }

    // Lấy sản phẩm theo nhà cung cấp
    @Override
    public List<ProductResponse> getProductsBySupplierId(Long supplierId) {
        return productRepository.findBySupplier_SupplierId(supplierId).stream().map(this::mapToResponse).toList();
    }

    // Lấy sản phẩm theo danh mục và nhà cung cấp
    @Override
    public List<ProductResponse> getProductsByCategoryAndSupplier(Long categoryId, Long supplierId) {
        return productRepository.findByCategory_CategoryIdAndSupplier_SupplierId(categoryId, supplierId).stream()
                .map(this::mapToResponse).toList();
    }

    // Lấy sản phẩm sắp hết hạn (stockQuantity <= reorderPoint)
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
            Boolean active) {

        PaginationValidator.validate(page, size, sortBy, sortDir, ALLOWED_SORT_FIELDS);

        String sortProperty = switch (sortBy) {
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
                pageable);
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
                productPage.isLast());
    }
}
