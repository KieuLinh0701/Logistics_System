package com.logistics.mapper;

import com.logistics.dto.user.role.PermissionGroupDto;
import com.logistics.entity.PermissionGroup;
import java.util.List;
import java.util.stream.Collectors;

public class PermissionGroupMapper {

    public static PermissionGroupDto toPermissionGroupDto(PermissionGroup entity) {
        if (entity == null) return null;

        List<PermissionGroupDto> children = List.of();
        if (entity.getSubPermissions() != null) {
            children = entity.getSubPermissions().stream()
                    .map(PermissionGroupMapper::toPermissionGroupDto)
                    .toList();
        }

        return new PermissionGroupDto(
                entity.getId(),
                entity.getName(),
                entity.getDescription(),
                children);
    }

    public static List<PermissionGroupDto> toPermissionGroupListDto(List<PermissionGroup> entities) {
        if (entities == null) {
            return null;
        }
        return entities.stream()
                .map(PermissionGroupMapper::toPermissionGroupDto)
                .collect(Collectors.toList());
    }


}