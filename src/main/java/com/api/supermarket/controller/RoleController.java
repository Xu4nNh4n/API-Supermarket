package com.api.supermarket.controller;
import com.api.supermarket.dto.request.RoleRequest;
import com.api.supermarket.dto.response.RoleResponse;
import com.api.supermarket.service.*;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;
@RestController
@RequestMapping("/api/roles")
public class RoleController {
    private final RoleService roleService;

    public RoleController(RoleService roleService) {
        this.roleService = roleService;
    }

    @PreAuthorize("hasRole('ADMIN')") // Chỉ cho phép ADMIN thực hiện các thao tác này
    @GetMapping
    public List<RoleResponse> getAllRoles() {
        return roleService.getALlRoles();
    }

    @PreAuthorize("hasRole('ADMIN')") // Chỉ cho phép ADMIN thực hiện các thao tác này
    @GetMapping("/{id}")
    public RoleResponse getRoleById(@PathVariable Long id) {
        return roleService.getRoleById(id);
    }
    
    @PreAuthorize("hasAnyRole('ADMIN')") // Chỉ cho phép ADMIN và MANAGER thực hiện các thao tác này
    @PostMapping
    public RoleResponse createRole(@Valid @RequestBody RoleRequest request) {
        return roleService.createRole(request);
    }

    @PreAuthorize("hasAnyRole('ADMIN')") // Chỉ cho phép ADMIN và MANAGER thực hiện các thao tác này
    @PutMapping("/{id}")
    public RoleResponse updateRole(@PathVariable Long id, @Valid @RequestBody RoleRequest request) {
        return roleService.updateRole(id, request);
    }

    @PreAuthorize("hasAnyRole('ADMIN')") // Chỉ cho phép ADMIN và MANAGER thực hiện các thao tác này
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteRole(@PathVariable Long id) {
        roleService.deleteRole(id);
        return ResponseEntity.noContent().build();
    }
    @PreAuthorize("hasAnyRole('ADMIN')") // Chỉ cho phép ADMIN và MANAGER thực hiện các thao tác này
    @GetMapping("/search/{keyword}")
    public List<RoleResponse> searchRolesByName(@PathVariable String keyword) {
        return roleService.searchRolesByName(keyword);
    }
}
