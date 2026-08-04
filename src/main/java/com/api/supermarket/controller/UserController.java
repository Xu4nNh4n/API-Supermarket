package com.api.supermarket.controller;

import java.util.List;
import java.util.Optional;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;

import com.api.supermarket.dto.response.PageResponse;
import com.api.supermarket.dto.response.UserResponse;
import com.api.supermarket.dto.request.CreateUserRequest;
import com.api.supermarket.dto.request.UpdateUserRequest;
import com.api.supermarket.service.UserService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/users")
public class UserController {
    private final UserService userService;
    
    public UserController(UserService userService) {
        this.userService = userService;
    }
    
    @PreAuthorize("hasRole('ADMIN')") // Chỉ cho phép ADMIN thực hiện các thao tác này
    @GetMapping
    public PageResponse<UserResponse> getAllUsers(
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "10") int size,
        @RequestParam(defaultValue = "userId") String sortBy,
        @RequestParam(defaultValue = "asc") String sortDir,
        @RequestParam(required = false) String keyword,
        @RequestParam(required = false) String roleName,
        @RequestParam(required = false) String fullName,
        @RequestParam(required = false) String email,
        @RequestParam(required = false) String phone,
        @RequestParam(required = false) Boolean active
    ) {
        return userService.filterUsers(page, size, sortBy, sortDir, keyword, roleName, fullName, email, phone, active);
    }

    @PreAuthorize("hasRole('ADMIN')") // Chỉ cho phép ADMIN thực hiện các thao tác này
    @GetMapping("/{id}")
    public UserResponse getUserById(@PathVariable Long id){
        return userService.getUserById(id);
    }

    @PreAuthorize("hasRole('ADMIN')") // Chỉ cho phép ADMIN thực hiện các thao tác này
    @PostMapping
    public UserResponse createUser(@Valid @RequestBody CreateUserRequest request){
        return userService.createUser(request);
    }

    @PreAuthorize("hasRole('ADMIN')") // Chỉ cho phép ADMIN thực hiện các thao tác này
    @PutMapping("/{id}")
    public UserResponse updateUser(@PathVariable Long id, @Valid @RequestBody UpdateUserRequest request){
        return userService.updateUser(id, request);
    }

    @PreAuthorize("hasRole('ADMIN')") // Chỉ cho phép ADMIN thực hiện các thao tác này
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id){
        userService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }

    @PreAuthorize("hasRole('ADMIN')") // Chỉ cho phép ADMIN thực hiện các thao tác này
    @GetMapping("/active")
    public List<UserResponse> getUsersByIsActiveTrue(){
        return userService.findByIsActiveTrue();
    }

    @PreAuthorize("hasRole('ADMIN')") // Chỉ cho phép ADMIN thực hiện các thao tác này
    @GetMapping("/search/username/{userName}")
    public Optional<UserResponse> getUserByUserName(@PathVariable String userName){
        return userService.findByUserName(userName);
    }

    @PreAuthorize("hasRole('ADMIN')") // Chỉ cho phép ADMIN thực hiện các thao tác này
    @GetMapping("/search/email/{email}")
    public Optional<UserResponse> getUserByEmail(@PathVariable String email){
        return userService.findByEmail(email);
    }

    @PreAuthorize("hasRole('ADMIN')") // Chỉ cho phép ADMIN thực hiện các thao tác này
    @GetMapping("/search/role/{roleId}")
    public List<UserResponse> getUsersByRoleId(@PathVariable Long roleId){
        return userService.findByRoleId(roleId);
    }

    @PreAuthorize("hasRole('ADMIN')") // Chỉ cho phép ADMIN thực hiện các thao tác này
    @GetMapping("/search/fullname/{keyword}")
    public List<UserResponse> getUsersByFullName(@PathVariable String keyword){
        return userService.findByFullNameContaining(keyword);
    }
}
