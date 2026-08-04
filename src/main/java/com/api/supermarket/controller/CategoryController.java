package com.api.supermarket.controller;

import com.api.supermarket.dto.request.CategoryRequest;
import com.api.supermarket.dto.response.CategoryResponse;
import com.api.supermarket.dto.response.PageResponse;
import com.api.supermarket.service.*;
import jakarta.validation.*;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.*;
@RestController
@RequestMapping("/api/categories")
public class CategoryController {
    private final CategoryService categoryService;

    public CategoryController(CategoryService categoryService){
        this.categoryService = categoryService;
    }
    @GetMapping
    public PageResponse<CategoryResponse> getAllCategories(
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "10") int size,
        @RequestParam(defaultValue = "categoryId") String sortBy,
        @RequestParam(defaultValue = "asc") String sortDir,
        @RequestParam(required = false) String keyword,
        @RequestParam(required = false) String description,
        @RequestParam(required = false) Boolean active
    ){
        return categoryService.filterCategories(page, size, sortBy, sortDir, keyword, description, active);
    }

    //lấy theo id
    @GetMapping("/{id}")
    public CategoryResponse getCategoryById(@PathVariable Long id){
        return categoryService.getCategoryById(id);
    }
    //lấy theo Active
    @GetMapping("/active")
    public List<CategoryResponse> getCategoriesByIsActiveTrue(){
        return categoryService.getCategoriesByIsActiveTrue();
    }
    //Lấy theo tên
    @GetMapping("/search/{name}")
    public List<CategoryResponse> getCategoriesByName(@PathVariable String name){
        return categoryService.getCategoriesByNameContaining(name);
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')") // Chỉ cho phép ADMIN và MANAGER thực hiện các thao tác này
    @PostMapping
    public CategoryResponse createCategory(@Valid @RequestBody CategoryRequest request){
        return categoryService.createCategory(request);
    }
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')") // Chỉ cho phép ADMIN và MANAGER thực hiện các thao tác này
    @PutMapping("/{id}")
    public CategoryResponse updateCategory(@PathVariable Long id, @Valid @RequestBody CategoryRequest request){
        return categoryService.updateCategory(id, request);
    }
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')") // Chỉ cho phép ADMIN và MANAGER thực hiện các thao tác này
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCategory(@PathVariable Long id){
        categoryService.deleteCategory(id);
        return ResponseEntity.noContent().build();
    }
}
