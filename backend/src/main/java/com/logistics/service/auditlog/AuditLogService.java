package com.logistics.service.auditlog;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.logistics.entity.AuditLog;
import com.logistics.entity.User;
import com.logistics.enums.AuditLogAction;
import com.logistics.enums.AuditLogStatus;
import com.logistics.enums.EntityType;
import com.logistics.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;
    private final ObjectMapper objectMapper;

    @Async
    public void log(User user,
                    AuditLogAction action,
                    EntityType entityType,
                    String entityId,
                    String entityCode,
                    Object payloadBefore,
                    Object payloadAfter,
                    String description) {
        try {
            AuditLog auditLog = AuditLog.builder()
                    .user(user)
                    .action(action)
                    .entity(entityType)
                    .entityId(entityId)
                    .entityCode(entityCode)
                    .payloadBefore(toJson(payloadBefore))
                    .payloadAfter(toJson(payloadAfter))
                    .description(description)
                    .status(AuditLogStatus.SUCCESS)
                    .build();

            auditLogRepository.save(auditLog);
        } catch (Exception e) {
            log.error("Failed to write audit log: {}", e.getMessage());
        }
    }

    @Async
    public void logFailure(User user,
                           AuditLogAction action,
                           EntityType entityType,
                           String entityId,
                           AuditLogStatus status,
                           String errorMessage) {
        try {
            AuditLog auditLog = AuditLog.builder()
                    .user(user)
                    .action(action)
                    .entity(entityType)
                    .entityId(entityId)
                    .status(status)
                    .errorMessage(errorMessage)
                    .build();

            auditLogRepository.save(auditLog);
        } catch (Exception e) {
            log.error("Failed to write failure audit log: {}", e.getMessage());
        }
    }

    private String toJson(Object obj) {
        if (obj == null) return null;
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            return obj.toString();
        }
    }
}