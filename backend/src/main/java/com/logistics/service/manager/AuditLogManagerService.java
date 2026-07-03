package com.logistics.service.manager;

import com.logistics.dto.manager.audit.ManagerAuditLogDto;
import com.logistics.request.manager.audit.AuditLogSearchRequest;
import com.logistics.response.ListResponse;

public interface AuditLogManagerService {
    ListResponse<ManagerAuditLogDto> list(int userId, AuditLogSearchRequest request);
    byte[] export(Integer userId, AuditLogSearchRequest request);
}