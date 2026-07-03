package com.logistics.service.user;

import com.logistics.dto.user.role.RoleDetailUserDto;
import com.logistics.dto.user.role.RoleListUserDto;
import com.logistics.entity.Role;
import com.logistics.entity.User;
import com.logistics.request.user.role.RoleSearchUserRequest;
import com.logistics.request.user.role.RoleUserRequest;
import com.logistics.response.ListResponse;

import java.util.List;

public interface RoleUserService {
    ListResponse<RoleListUserDto> list(int userId, RoleSearchUserRequest request);
    List<RoleListUserDto> findAll(int userId);
    RoleDetailUserDto detail(int userId, int roleId);
    void create(int userId, RoleUserRequest request);
    void update(int userId, int roleId, RoleUserRequest request);
    void delete(int userId, int roleId);
    void checkOwnerPermission(User user, Role role);
    Role getRole(int roleId);
}