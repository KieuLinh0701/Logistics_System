package com.logistics.service.user;

import com.logistics.dto.user.audit.UserAuditLogDto;
import com.logistics.request.manager.audit.AuditLogSearchRequest;
import com.logistics.response.ListResponse;

public interface AuditLogUserService {
    ListResponse<UserAuditLogDto> list(int userId, AuditLogSearchRequest request);
    byte[] export(Integer userId, AuditLogSearchRequest request);
}