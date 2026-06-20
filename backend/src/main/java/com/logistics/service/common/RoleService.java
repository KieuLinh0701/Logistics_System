package com.logistics.service.common;

import com.logistics.entity.PermissionGroup;
import com.logistics.entity.Role;
import com.logistics.exception.AppException;
import com.logistics.exception.enums.PermissionGroupErrorCode;
import com.logistics.repository.RoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RoleService {

    @Autowired
    private RoleRepository roleRepository;

    @Transactional(readOnly = true)
    public Role findByIdWithPermissionGroups(Integer roleId) {
        return roleRepository.findByIdWithPermissionGroups(roleId)
                .orElseThrow(() -> new AppException(PermissionGroupErrorCode.PERMISSION_GROUP_NOT_FOUND));
    }

    public List<String> getPermissionGroupCodes(Role role) {
        return role.getPermissionGroups().stream()
                .filter(PermissionGroup::getIsActive)
                .map(PermissionGroup::getCode)
                .distinct()
                .toList();
    }
}