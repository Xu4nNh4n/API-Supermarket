package com.api.supermarket.repository;
import com.api.supermarket.entity.*;
import org.springframework.data.jpa.repository.*;
import java.util.List;
public interface RoleRepository extends JpaRepository<Role, Long> {
    //Kiểm tra trùng tên vai trò
    boolean existsByRoleName(String roleName);

    //Tìm kiếm vai trò theo tên
    List<Role> findByRoleNameContaining(String keyword);

}
