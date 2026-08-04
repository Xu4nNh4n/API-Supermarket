package com.api.supermarket.dto.request;

import jakarta.validation.constraints.*;

public class RoleRequest {
    @NotBlank(message = "Tên vai trò không được để trống")
    @Size(max = 50, message = "Tên vai trò tối đa 50 ký tự")
    private String roleName;

    @Size(max = 255, message = "Mô tả tối đa 255 ký tự")
    private String description;
    

    public String getRoleName() {
        return roleName;
    }
    public void setRoleName(String roleName) {
        this.roleName = roleName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
