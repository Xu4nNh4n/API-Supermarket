package com.api.supermarket.service.Impl;
import com.api.supermarket.dto.request.RoleRequest;
import com.api.supermarket.dto.response.RoleResponse;
import com.api.supermarket.entity.*;
import com.api.supermarket.exception.BadRequestException;
import com.api.supermarket.exception.ResourceNotFoundException;
import com.api.supermarket.repository.*;
import com.api.supermarket.service.*;
import org.springframework.stereotype.*;
import java.util.*;
@Service
public class RoleServiceImpl implements RoleService {
    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    public RoleServiceImpl(RoleRepository roleRepository, UserRepository userRepository) {
        this.roleRepository = roleRepository;
        this.userRepository = userRepository;
    }

    private RoleResponse mapToResponse(Role role){
        return new RoleResponse(
            role.getRoleId(),
            role.getRoleName(),
            role.getDescription(),
            role.getCreateAt()
        );
    }

    @Override
    public List<RoleResponse> getALlRoles() {
        return roleRepository.findAll().stream().map(this::mapToResponse).toList();
    }

    @Override
    public RoleResponse getRoleById(Long id){
        return mapToResponse(roleRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy vai trò")));
    }

    @Override
    public RoleResponse createRole(RoleRequest request) {
        if (roleRepository.existsByRoleName(request.getRoleName())) {
            throw new BadRequestException("Tên vai trò đã tồn tại");
        }
        Role role = new Role();
        role.setRoleName(request.getRoleName());
        role.setDescription(request.getDescription());
        return mapToResponse(roleRepository.save(role));
    }

    @Override
    public RoleResponse updateRole(Long id, RoleRequest request) {
        Role role = roleRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy vai trò"));
        if (!role.getRoleName().equals(request.getRoleName()) && roleRepository.existsByRoleName(request.getRoleName())) {
            throw new BadRequestException("Tên vai trò đã tồn tại");
        }
        role.setRoleName(request.getRoleName());
        role.setDescription(request.getDescription());
        return mapToResponse(roleRepository.save(role));
    }

    @Override
    public void deleteRole(Long id) {
        if(userRepository.existsByRoleId(id)){
            throw new BadRequestException("Vai trò này đang được sử dụng bởi người dùng nên không thể xóa");
        }

        Role role = roleRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy vai trò"));
        roleRepository.delete(role);
    }

    @Override
    public List<RoleResponse> searchRolesByName(String keyword) {
        return roleRepository.findByRoleNameContaining(keyword).stream().map(this::mapToResponse).toList();
    }
    
}
