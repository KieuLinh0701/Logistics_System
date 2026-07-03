package com.logistics.service.manager;

import com.logistics.dto.BaseAuditLogDto;
import com.logistics.dto.manager.employee.ManagerEmployeeListDto;
import com.logistics.dto.manager.employee.ManagerEmployeeListWithShipperAssignmentDto;
import com.logistics.dto.manager.employee.ManagerEmployeePerformanceDto;
import com.logistics.entity.Office;
import com.logistics.request.SearchRequest;
import com.logistics.request.manager.audit.AuditLogSearchRequest;
import com.logistics.request.manager.employee.ManagerEmployeeEditRequest;
import com.logistics.request.manager.employee.ManagerEmployeeSearchRequest;
import com.logistics.response.ListResponse;

import java.util.List;

public interface EmployeeManagerService {
    Office getManagedOfficeByUserId(Integer userId);
    ListResponse<ManagerEmployeeListDto> list(int userId, ManagerEmployeeSearchRequest request);
    byte[] export(int userId, ManagerEmployeeSearchRequest request);
    ListResponse<ManagerEmployeePerformanceDto> getEmployeePerformance(int userId, SearchRequest request);
    String createEmployee(int creatorUserId, ManagerEmployeeEditRequest req);
    String updateEmployee(int editorUserId, int employeeId, ManagerEmployeeEditRequest req);
    ListResponse<ManagerEmployeeListWithShipperAssignmentDto> getActiveShippersWithActiveAssignments(int userId, ManagerEmployeeSearchRequest request);
    ListResponse<ManagerEmployeeListDto> getActiveShippers(
            int userId,
            ManagerEmployeeSearchRequest request);
    ListResponse<ManagerEmployeeListDto> getActiveEmployeesByShipmentType(
            int userId,
            SearchRequest request);
    byte[] exportPerformance(Integer userId, SearchRequest request);
    List<ManagerEmployeePerformanceDto> getEmployeePerformanceForExport(
            int userId,
            SearchRequest request);
    byte[] exportActiveShippersWithActiveAssignments(int userId, ManagerEmployeeSearchRequest request);
    ListResponse<BaseAuditLogDto> listAuditLogsByUserId(
            Integer userId,
            Integer employeeId,
            AuditLogSearchRequest request);
    byte[] exportAuditLogsByUserId(
            Integer userId,
            Integer employeeId,
            AuditLogSearchRequest request);
}