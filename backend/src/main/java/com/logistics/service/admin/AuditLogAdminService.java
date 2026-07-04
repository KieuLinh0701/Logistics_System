package com.logistics.service.admin;

import com.logistics.dto.admin.AdminAuditLogDto;
import com.logistics.request.manager.audit.AuditLogSearchRequest;
import com.logistics.response.ListResponse;

public interface AuditLogAdminService {

    ListResponse<AdminAuditLogDto> list(AuditLogSearchRequest request);

    byte[] export(AuditLogSearchRequest request);
}
