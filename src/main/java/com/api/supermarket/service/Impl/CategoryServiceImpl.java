package com.api.supermarket.service.Impl;

import com.api.supermarket.dto.request.CategoryRequest;
import com.api.supermarket.dto.response.CategoryResponse;
import com.api.supermarket.dto.response.PageResponse;
import com.api.supermarket.entity.*;
import com.api.supermarket.exception.BadRequestException;
import com.api.supermarket.exception.ResourceNotFoundException;
import com.api.supermarket.repository.*;
import com.api.supermarket.service.*;
import com.api.supermarket.validation.PaginationValidator;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.*;
import java.util.*;
@Service
public class CategoryServiceImpl implements CategoryService{
    // Repository dùng để thao tác trực tiếp với bảng category trong database.
    // Service cần repository để tách phần xử lý nghiệp vụ khỏi phần truy vấn dữ liệu.
    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;

    // Constructor injection: Spring sẽ tự truyền CategoryRepository vào đây.
    // Cách này giúp service dùng được repository mà không cần tự new object.
    public CategoryServiceImpl(CategoryRepository categoryRepository, ProductRepository productRepository){
        this.categoryRepository = categoryRepository;
        this.productRepository = productRepository;
    }

    private CategoryResponse mapToResponse(Category category){
        return new CategoryResponse(
            category.getCategoryId(),
            category.getCategoryName(),
            category.getDescription(),
            category.getIsActive(),
            category.getCreateAt()
        );
    }
    // Danh sách các trường được phép sắp xếp trong API filterCategories.
    private static final Set<String> ALLOWED_SORT_FIELDS  = Set.of(
        "categoryId",
        "categoryName",
        "description",
        "isActive",
        "createAt"
    );
    @Override
    public List<CategoryResponse> getAllCategories(){
        // Lấy toàn bộ danh mục đang có trong database.
        return categoryRepository.findAll().stream().map(this::mapToResponse).toList();
    }

    @Override
    public CategoryResponse getCategoryById(Long id){
        // Tìm danh mục theo id; nếu không có thì báo lỗi để tránh xử lý dữ liệu null.
        Category category =  categoryRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy danh mục"));
        return mapToResponse(category);
    
    }

    @Override
    public CategoryResponse createCategory(CategoryRequest request){

        if(categoryRepository.existsByCategoryName(request.getCategoryName())){
            throw new BadRequestException("Tên danh mục đã tồn tại");
        }

        if(request.getCategoryName() == null || request.getCategoryName().isBlank()){
            throw new BadRequestException("Tên danh mục không được để trống");
        }
        
        // Tạo entity mới vì database lưu Category, không lưu trực tiếp CategoryRequest.
        Category category = new Category();

        // Gán dữ liệu từ request sang entity trước khi lưu vào database.
        category.setCategoryName(request.getCategoryName());
        category.setDescription(request.getDescription());
        category.setIsActive(request.getIsActive() == null ? true : request.getIsActive());

        // save() dùng để thêm danh mục mới vào database.
        return mapToResponse(categoryRepository.save(category));
    }
    
    @Override
    public CategoryResponse updateCategory(Long id, CategoryRequest request){
        Category category = categoryRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy danh mục này") );

        if(!category.getCategoryName().equals(request.getCategoryName()) && categoryRepository.existsByCategoryName(request.getCategoryName())){
            throw new BadRequestException("Tên danh mục đã tồn tại");
        }

        if(request.getCategoryName() == null || request.getCategoryName().isBlank()){
            throw new BadRequestException("Tên danh mục không được để trống");
        }
        // Lấy danh mục cũ theo id trước để biết đang sửa đúng bản ghi nào.

        // Cập nhật lại các field bằng dữ liệu mới từ request.
        category.setCategoryName(request.getCategoryName());
        category.setDescription(request.getDescription());
        category.setIsActive(request.getIsActive() != null ? request.getIsActive() : category.getIsActive());

        // save() ở đây dùng để lưu lại bản ghi đã được chỉnh sửa.
        return mapToResponse(categoryRepository.save(category));
    }

    @Override
    public void deleteCategory(Long id) {

        if(productRepository.existsByCategory_CategoryId(id)){
            throw new BadRequestException("Danh mục này có sản phẩm đang tồn tài nên không được xóa");
        }
    // Tìm danh mục trước khi xóa để nếu id sai thì có thể báo lỗi rõ ràng.
    Category category = categoryRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy danh mục"));

    // Xóa danh mục đã tìm thấy khỏi database.
    categoryRepository.delete(category);
    }

    @Override
    public List<CategoryResponse> getCategoriesByIsActiveTrue(){
        // Lọc ra những danh mục đang hoạt động, dùng cho màn hình chỉ hiển thị dữ liệu còn dùng.
        return categoryRepository.findByIsActiveTrue().stream().map(this::mapToResponse).toList();
    }

    @Override
    public List<CategoryResponse> getCategoriesByNameContaining(String keyword){
        // Tìm danh mục theo từ khóa trong tên, phục vụ chức năng search.
        return categoryRepository.findByCategoryNameContaining(keyword).stream().map(this::mapToResponse).toList();
    }

    @Override
    public PageResponse<CategoryResponse> filterCategories
    (
        int page,
        int size,
        String sortBy,
        String sortDir,
        String keyword,
        String description,
        Boolean active
    ){
        PaginationValidator.validate(page, size, sortBy, sortDir, ALLOWED_SORT_FIELDS);
        
        Sort sort = sortDir.equalsIgnoreCase("desc")
        ? Sort.by(sortBy).descending()
        : Sort.by(sortBy).ascending();


        Pageable pageable = PageRequest.of(page, size, sort);

        Page<Category> categoryPage = categoryRepository.filterCategories(
            keyword, 
            description, 
            active, 
            pageable);

        List<CategoryResponse> categories = categoryPage.getContent()
            .stream()
            .map(this::mapToResponse)
            .toList();
        return new PageResponse<>(
            categories,
            categoryPage.getNumber(),
            categoryPage.getSize(),
            categoryPage.getTotalElements(),
            categoryPage.getTotalPages(),
            categoryPage.isLast()
        );
    }
}
