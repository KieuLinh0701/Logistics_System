package com.logistics.service.common.impl;

import com.logistics.entity.PermissionGroup;
import com.logistics.entity.Role;
import com.logistics.exception.AppException;
import com.logistics.exception.enums.PermissionGroupErrorCode;
import com.logistics.repository.RoleRepository;
import com.logistics.service.common.RoleService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RoleServiceImpl implements RoleService {

    private final RoleRepository roleRepository;

    @Override
    @Transactional(readOnly = true)
    public Role findByIdWithPermissionGroups(Integer roleId) {
        return roleRepository.findByIdWithPermissionGroups(roleId)
                .orElseThrow(() -> new AppException(PermissionGroupErrorCode.PERMISSION_GROUP_NOT_FOUND));
    }

    @Override
    public List<String> getPermissionGroupCodes(Role role) {
        return role.getPermissionGroups().stream()
                .filter(PermissionGroup::getIsActive)
                .map(PermissionGroup::getCode)
                .distinct()
                .toList();
    }
}