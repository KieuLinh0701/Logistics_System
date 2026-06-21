package com.logistics.dto.manager.audit;

import com.logistics.dto.BaseAuditLogDto;
import com.logistics.enums.AuditLogAction;
import com.logistics.enums.AuditLogStatus;
import com.logistics.enums.EntityType;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
public class ManagerAuditLogDto extends BaseAuditLogDto {
    private User user;

    public ManagerAuditLogDto(
            Integer id,
            EntityType entity,
            String entityId,
            AuditLogAction action,
            String description,
            AuditLogStatus status,
            LocalDateTime createdAt,
            User user) {
        super(id, entity, entityId, action, description, status, createdAt);
        this.user = user;
    }

    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    public static class User {
        private Integer id;
        private String fullName;
        private String phoneNumber;
    }
}