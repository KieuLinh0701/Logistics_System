package com.logistics.mapper;

import com.logistics.dto.user.employee.EmployeeByRoleIdListUserDto;
import com.logistics.dto.user.employee.EmployeeListUserDto;
import com.logistics.dto.user.employee.ShopWorkHistoryListUserDto;
import com.logistics.entity.Account;
import com.logistics.entity.AccountRole;
import com.logistics.entity.Role;
import com.logistics.entity.ShopWorkHistory;
import com.logistics.entity.User;

import java.util.List;
import java.util.stream.Collectors;

public class UserMapper {

    public static EmployeeByRoleIdListUserDto toEmployeeListUserDto(User entity, Integer roleId) {
        if (entity == null) return null;

        Account account = entity.getAccount();

        Boolean isActive = null;
        if (entity.getAccount() != null && entity.getAccount().getAccountRoles() != null) {
            isActive = entity.getAccount().getAccountRoles().stream()
                    .filter(ar -> ar.getRole() != null && ar.getRole().getId().equals(roleId))
                    .map(AccountRole::getIsActive)
                    .findFirst()
                    .orElse(null);
        }

        return new EmployeeByRoleIdListUserDto(
                entity.getId(),
                entity.getCode(),
                entity.getFullName(),
                isActive,
                (account != null) ? account.getEmail() : null,
                entity.getPhoneNumber(),
                entity.getUpdatedAt()
        );
    }

    public static List<EmployeeByRoleIdListUserDto> toEmployeeListUserDto(List<User> entities, Integer roleId) {
        if (entities == null) return null;
        return entities.stream()
                .map(e -> toEmployeeListUserDto(e, roleId))
                .toList();
    }

    public static List<EmployeeListUserDto> toEmployeeListDto(List<User> users) {
        return users.stream()
                .map(u -> mapToDto(u))
                .collect(Collectors.toList());
    }

    private static EmployeeListUserDto mapToDto(User u) {

        Account account = u.getAccount();

        return new EmployeeListUserDto(
                u.getId(),
                u.getCode(),
                u.getLastName(),
                u.getFirstName(),
                (account != null) ? account.getEmail() : null,
                u.getPhoneNumber(),
                u.getUpdatedAt()
        );
    }

    public static List<ShopWorkHistoryListUserDto> toShopWorkHistoryListDto(List<ShopWorkHistory> entities) {
        if (entities == null) return null;
        return entities.stream()
                .map(UserMapper::toShopWorkHistoryListDto)
                .toList();
    }

    public static ShopWorkHistoryListUserDto toShopWorkHistoryListDto(ShopWorkHistory entity) {
        if (entity == null) return null;

        Role role = entity.getRole();

        return new ShopWorkHistoryListUserDto(
                entity.getId(),
                role != null ? role.getName() : null,
                entity.getIsCurrent(),
                entity.getJoinedAt(),
                entity.getLeftAt(),
                entity.getNote()
        );
    }
}