package com.logistics.service.admin;

import com.logistics.dto.BaseAuditLogDto;
import com.logistics.request.admin.CreateUserRequest;
import com.logistics.request.admin.UpdateUserRequest;
import com.logistics.request.manager.audit.AuditLogSearchRequest;
import com.logistics.response.ListResponse;

import java.util.Map;

public interface UserAdminService {

    Map<String, Object> listUsers(int page, int limit, String search, String status, String roleName);

    Map<String, Object> getUserById(Integer userId);

    void createUser(CreateUserRequest request);

    void updateUser(Integer userId, UpdateUserRequest request);

    void deleteUser(Integer userId);

    ListResponse<BaseAuditLogDto> listAuditLogsByUserId(Integer userId, AuditLogSearchRequest request);

    byte[] export(Integer userId, AuditLogSearchRequest request);
}
