package com.logistics.mapper;

import com.logistics.dto.BaseAuditLogDto;
import com.logistics.dto.admin.AdminAuditLogDto;
import com.logistics.dto.manager.audit.ManagerAuditLogDto;
import com.logistics.dto.user.audit.UserAuditLogDto;
import com.logistics.entity.AuditLog;
import com.logistics.entity.Office;
import com.logistics.entity.User;

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

    public static List<BaseAuditLogDto> toBaseAuditLogDtoList(List<AuditLog> auditLogList) {
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
                toUserManagerDto(entity.getUser()));
    }

    public static List<ManagerAuditLogDto> toManagerAuditLogDtoList(List<AuditLog> auditLogList) {
        return auditLogList.stream()
                .map(AuditLogMapper::toManagerAuditLogDto)
                .toList();
    }

    public static UserAuditLogDto toUserAuditLogDto(AuditLog entity, Integer shopId) {
        if (entity == null) {
            return null;
        }

        return new UserAuditLogDto(
                entity.getId(),
                entity.getEntity(),
                entity.getEntityId(),
                entity.getAction(),
                entity.getDescription(),
                entity.getStatus(),
                entity.getCreatedAt(),
                toUserUserDto(entity.getUser()));
    }

    public static List<UserAuditLogDto> toUserAuditLogDtoList(List<AuditLog> auditLogList, Integer shopId) {
        return auditLogList.stream()
                .map(x -> toUserAuditLogDto(x, shopId))
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
                toUserManagerDto(entity.getUser()),
                toOfficeDto(entity.getOffice()));
    }

    public static List<AdminAuditLogDto> toAdminAuditLogDtoList(List<AuditLog> auditLogList) {
        return auditLogList.stream()
                .map(AuditLogMapper::toAdminAuditLogDto)
                .toList();
    }

    public static ManagerAuditLogDto.User toUserManagerDto(User entity) {
        if (entity == null) {
            return null;
        }

        return new ManagerAuditLogDto.User(
                entity.getId(),
                entity.getFullName(),
                entity.getPhoneNumber()
        );
    }

    public static UserAuditLogDto.User toUserUserDto(User entity) {
        if (entity == null) {
            return null;
        }

        return new UserAuditLogDto.User(
                entity.getId(),
                entity.getFullName(),
                entity.getPhoneNumber()
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