package com.logistics.service.common;

import com.logistics.entity.Role;

import java.util.List;

public interface RoleService {
    Role findByIdWithPermissionGroups(Integer roleId);
    List<String> getPermissionGroupCodes(Role role);
}