package com.logistics.mapper;

import com.logistics.dto.user.role.PermissionGroupDto;
import com.logistics.dto.user.role.PermissionModuleDto;
import com.logistics.entity.PermissionModule;

import java.util.List;
import java.util.stream.Collectors;

public class PermissionModuleMapper {

    public static PermissionModuleDto toPermissionModuleDto(PermissionModule entity) {
        if (entity == null) return null;

        List<PermissionGroupDto> groups = entity.getPermissionGroups() == null
                ? List.of()
                : entity.getPermissionGroups().stream()
                        .map(PermissionGroupMapper::toPermissionGroupDto)
                        .toList();

        return new PermissionModuleDto(
                entity.getId(),
                entity.getName(),
                entity.getDescription(),
                groups);
    }

    public static List<PermissionModuleDto> toPermissionModuleListDto(List<PermissionModule> entities) {
        if (entities == null) {
            return null;
        }
        return entities.stream()
                .map(PermissionModuleMapper::toPermissionModuleDto)
                .collect(Collectors.toList());
    }
}