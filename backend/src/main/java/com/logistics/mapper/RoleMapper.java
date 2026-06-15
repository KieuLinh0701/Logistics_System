package com.logistics.mapper;

import com.logistics.dto.ProductDto;
import com.logistics.dto.user.role.RoleDetailUserDto;
import com.logistics.dto.user.role.RoleListUserDto;
import com.logistics.entity.PermissionGroup;
import com.logistics.entity.Product;
import com.logistics.entity.Role;

import java.util.List;

public class RoleMapper {

    public static RoleListUserDto toRoleListUserDto(Role entity) {
        if (entity == null) {
            return null;
        }

        return new RoleListUserDto(
                entity.getId(),
                entity.getName(),
                entity.getDescription(),
                entity.getCreatedAt(),
                entity.getUpdatedAt());
    }

    public static RoleDetailUserDto toRoleDetailUserDto(Role entity) {
        if (entity == null) {
            return null;
        }

        return new RoleDetailUserDto(
                entity.getId(),
                entity.getName(),
                entity.getDescription(),
                getPermissionGroupIds(entity));
    }

    private static List<Integer> getPermissionGroupIds(Role entity) {
        if (entity.getPermissionGroups() == null) {
            return null;
        }
        return entity.getPermissionGroups().stream()
                .map(PermissionGroup::getId)
                .toList();
    }
}