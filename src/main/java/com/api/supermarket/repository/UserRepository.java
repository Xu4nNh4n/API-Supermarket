package com.api.supermarket.repository;
import com.api.supermarket.entity.User;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
public interface UserRepository extends JpaRepository<User, Long>{
    
    boolean existsByRoleId(Long RoleId);
    boolean existsByUserName(String userName);
    boolean existsByEmail(String email);
    
    Optional<User> findByUserName(String userName);
    
    List<User> findByIsActiveTrue();

    List<User> findByRoleId(Long roleId);

    List<User> findByFullNameContaining(String keyword);

    Optional<User> findByEmail(String email);

    @Query("""
            SELECT u FROM User u
            WHERE (:keyword IS NULL OR LOWER(u.userName) LIKE LOWER(CONCAT('%' , :keyword, '%')))
            AND (:roleName IS NULL OR LOWER(u.role.roleName) LIKE LOWER(CONCAT('%' , :roleName, '%')))
            AND (:fullName IS NULL OR LOWER(u.fullName) LIKE LOWER(CONCAT('%' , :fullName, '%')))
            AND (:email IS NULL OR u.email = :email)
            AND (:phone IS NULL OR u.phone = :phone)
            AND (:active IS NULL OR u.isActive = :active)
            """)
    Page<User> filterUsers(
        @Param("keyword") String keyword,
        @Param("roleName") String roleName,
        @Param("fullName") String fullName,
        @Param("email") String email,
        @Param("phone") String phone,
        @Param("active") Boolean active,
        Pageable pageable
    );
} 
