package com.api.supermarket.service;
import com.api.supermarket.dto.request.RoleRequest;
import com.api.supermarket.dto.response.RoleResponse;
import java.util.*;
public interface RoleService {
    //Lấy danh sách vai trò
    List<RoleResponse> getALlRoles();

    //Lấy 1 vai trò theo id
    RoleResponse getRoleById(Long id);

    //Thêm vai trò mới
    RoleResponse createRole(RoleRequest request);

    //Cập nhật vai trò
    RoleResponse updateRole(Long id, RoleRequest request);

    //Xóa vai trò
    void deleteRole(Long id);

    //Tìm kiếm vai trò theo tên
    List<RoleResponse> searchRolesByName(String keyword);
}
