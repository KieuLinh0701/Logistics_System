package com.logistics.service.user;

import com.logistics.dto.BaseAuditLogDto;
import com.logistics.dto.user.employee.EmployeeByRoleIdListUserDto;
import com.logistics.dto.user.employee.EmployeeListUserDto;
import com.logistics.dto.user.employee.ShopWorkHistoryListUserDto;
import com.logistics.request.manager.audit.AuditLogSearchRequest;
import com.logistics.request.user.employee.*;
import com.logistics.response.ListResponse;

public interface EmployeeUserService {
    ListResponse<EmployeeByRoleIdListUserDto> listByRoleId(int userId, int roleId, EmployeeByRoleIdSearchUserRequest request);
    ListResponse<EmployeeListUserDto> list(int userId, EmployeeSearchUserRequest request);
    void updateIsActive(int userId, int id, UpdateIsActiveUserRequest request);
    void createEmployee(int userId, CreateEmployeeUserRequest request);
    void updateEmployee(int userId, int id, UpdateEmployeeUserRequest request);
    ListResponse<ShopWorkHistoryListUserDto> listWorkHistory(int userId, int targetUserId, ShopWorkHistorySearchUserRequest request);
    ListResponse<BaseAuditLogDto> listAuditLogsByUserId(Integer userId, Integer employeeId, AuditLogSearchRequest request);
    byte[] exportAuditLogsByUserId(Integer userId, Integer employeeId, AuditLogSearchRequest request);
}