package com.api.supermarket.service;
import com.api.supermarket.dto.request.CategoryRequest;
import com.api.supermarket.dto.response.CategoryResponse;
import com.api.supermarket.dto.response.PageResponse;
import java.util.*;
public interface CategoryService {
    //Lấy danh sách danh mục
    List<CategoryResponse> getAllCategories();

    //Lấy 1 danh mục theo id
    CategoryResponse getCategoryById(Long id);

    //Thêm danh mục mới
    CategoryResponse createCategory(CategoryRequest request);

    //Cập nhật danh mục
    CategoryResponse updateCategory(Long id, CategoryRequest request);

    //Xóa danh mục
    void deleteCategory(Long id);
    
    //lấy danh mục đang hoạt động
    List<CategoryResponse> getCategoriesByIsActiveTrue();

    //Tìm danh mục theo tên
    List<CategoryResponse> getCategoriesByNameContaining(String keyword);

    PageResponse<CategoryResponse> filterCategories(
        int page,
        int size,
        String sortBy,
        String sortDir,
        String keyword,
        String description,
        Boolean active
    );
}
