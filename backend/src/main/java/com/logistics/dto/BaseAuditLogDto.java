package com.logistics.dto;

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
@AllArgsConstructor
@NoArgsConstructor
public class BaseAuditLogDto {
    private Integer id;
    private EntityType entity;
    private String entityId;
    private AuditLogAction action;
    private String description;
    private AuditLogStatus status;
    private LocalDateTime createdAt;
}