package com.logistics.mapper;

import com.logistics.dto.*;
import com.logistics.dto.admin.AdminAuditLogDto;
import com.logistics.dto.manager.audit.ManagerAuditLogDto;
import com.logistics.entity.*;
import com.logistics.enums.EmployeeStatus;

import java.util.List;

public class AuditLogMapper {

    public static BaseAuditLogDto toBaseAuditLogDto(AuditLog entity) {
        if (entity == null) {
            return null;
        }

        return new BaseAuditLogDto(
                entity.getId(),
                entity.getEntity(),
                entity.getEntityId(),
                entity.getAction(),
                entity.getDescription(),
                entity.getStatus(),
                entity.getCreatedAt());
    }

    public List<BaseAuditLogDto> toBaseAuditLogDtoList(List<AuditLog> auditLogList) {
        return auditLogList.stream()
                .map(AuditLogMapper::toBaseAuditLogDto)
                .toList();
    }

    public static ManagerAuditLogDto toManagerAuditLogDto(AuditLog entity) {
        if (entity == null) {
            return null;
        }

        return new ManagerAuditLogDto(
                entity.getId(),
                entity.getEntity(),
                entity.getEntityId(),
                entity.getAction(),
                entity.getDescription(),
                entity.getStatus(),
                entity.getCreatedAt(),
                toUserDto(entity.getUser()),
                toRoleDto(entity.getUser() != null ? entity.getUser().getEmployees() : List.of()));
    }

    public static List<ManagerAuditLogDto> toManagerAuditLogDtoList(List<AuditLog> auditLogList) {
        return auditLogList.stream()
                .map(AuditLogMapper::toManagerAuditLogDto)
                .toList();
    }

    public static AdminAuditLogDto toAdminAuditLogDto(AuditLog entity) {
        if (entity == null) {
            return null;
        }

        return new AdminAuditLogDto(
                entity.getId(),
                entity.getEntity(),
                entity.getEntityId(),
                entity.getAction(),
                entity.getDescription(),
                entity.getStatus(),
                entity.getCreatedAt(),
                toUserDto(entity.getUser()),
                toRoleDto(entity.getUser() != null ? entity.getUser().getEmployees() : List.of()),
                toOfficeDto(entity.getOffice()));
    }

    public List<AdminAuditLogDto> toAdminAuditLogDtoList(List<AuditLog> auditLogList) {
        return auditLogList.stream()
                .map(AuditLogMapper::toAdminAuditLogDto)
                .toList();
    }

    public static ManagerAuditLogDto.User toUserDto(User entity) {
        if (entity == null) {
            return null;
        }

        return new ManagerAuditLogDto.User(
                entity.getId(),
                entity.getFullName(),
                entity.getPhoneNumber()
        );
    }

    public static ManagerAuditLogDto.Role toRoleDto(List<Employee> employees) {
        if (employees.isEmpty()) {
            return null;
        }

        Employee employee = employees.stream()
                .filter(e -> e.getStatus().equals(EmployeeStatus.ACTIVE))
                .findFirst()
                .orElse(null);

        if (employee == null || employee.getAccountRole() == null || employee.getAccountRole().getRole() == null) {
            return null;
        }

        return new ManagerAuditLogDto.Role(
                employee.getAccountRole().getRole().getId(),
                employee.getAccountRole().getRole().getName()
        );
    }

    public static AdminAuditLogDto.Office toOfficeDto(Office entity) {
        if (entity == null) {
            return null;
        }

        return new AdminAuditLogDto.Office(
                entity.getId(),
                entity.getName(),
                entity.getCode(),
                entity.getPhoneNumber()
        );
    }
}