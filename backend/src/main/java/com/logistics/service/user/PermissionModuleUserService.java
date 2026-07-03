package com.logistics.service.user;

import com.logistics.dto.user.role.PermissionModuleDto;

import java.util.List;

public interface PermissionModuleUserService {
    List<PermissionModuleDto> activeList();
}