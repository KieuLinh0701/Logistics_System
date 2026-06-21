package com.logistics.dto.admin;

import com.logistics.dto.manager.audit.ManagerAuditLogDto;
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
public class AdminAuditLogDto extends ManagerAuditLogDto {
    private Office office;

    public AdminAuditLogDto(
            Integer id,
            EntityType entity,
            String entityId,
            AuditLogAction action,
            String description,
            AuditLogStatus status,
            LocalDateTime createdAt,
            User user,
            Role role,
            Office office) {
        super(id, entity, entityId, action, description, status, createdAt, user, role);
        this.office = office;
    }

    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Office {
        private Integer id;
        private String name;
        private String code;
        private String phoneNumber;
    }
}