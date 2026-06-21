package com.logistics.controller.user;

import com.logistics.audit.Audit;
import com.logistics.constants.AuditLogDescriptionConstant;
import com.logistics.dto.BaseAuditLogDto;
import com.logistics.dto.user.employee.EmployeeByRoleIdListUserDto;
import com.logistics.dto.user.employee.EmployeeListUserDto;
import com.logistics.dto.user.employee.ShopWorkHistoryListUserDto;
import com.logistics.enums.AuditLogAction;
import com.logistics.enums.EntityType;
import com.logistics.request.manager.audit.AuditLogSearchRequest;
import com.logistics.request.user.employee.*;
import com.logistics.response.ApiResponse;
import com.logistics.response.ListResponse;
import com.logistics.service.user.EmployeeUserService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/api/user/employees")
@Tag(
        name = "User - Employee",
        description = "Quản lý nhân sự, cập nhật trạng thái, thông tin công việc và lịch sử làm việc của nhân viên tại bưu cục"
)
public class EmployeeUserController {

    @Autowired
    private EmployeeUserService service;

    @GetMapping("/{roleId}")
    public ResponseEntity<ApiResponse<ListResponse<EmployeeByRoleIdListUserDto>>> listByRoleId(
            @PathVariable Integer roleId,
            @Valid EmployeeByRoleIdSearchUserRequest employeeByRoleIdSearchUserRequest,
            HttpServletRequest request) {
        Integer userId = (Integer) request.getAttribute("currentUserId");

        return ResponseEntity.ok(ApiResponse.success(service.listByRoleId(userId, roleId, employeeByRoleIdSearchUserRequest)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<ListResponse<EmployeeListUserDto>>> list(
            @Valid EmployeeSearchUserRequest employeeSearchUserRequest,
            HttpServletRequest request) {
        Integer userId = (Integer) request.getAttribute("currentUserId");

        return ResponseEntity.ok(ApiResponse.success(service.list(userId, employeeSearchUserRequest)));
    }

    @PatchMapping("/{id}/active")
    @Audit(
            entity = EntityType.USER,
            action = AuditLogAction.UPDATE,
            description = AuditLogDescriptionConstant.EMPLOYEE_UPDATE_STATUS,
            params = {"id"}
    )
    public ResponseEntity<ApiResponse<Void>> updateIsActive(
            @PathVariable Integer id,
            @Valid @RequestBody UpdateIsActiveUserRequest updateIsActiveUserRequest,
            HttpServletRequest request) {
        Integer userId = (Integer) request.getAttribute("currentUserId");

        service.updateIsActive(userId, id, updateIsActiveUserRequest);
        return ResponseEntity.noContent().build();
    }

    @PostMapping
    @Audit(
            entity = EntityType.USER,
            action = AuditLogAction.CREATE,
            description = AuditLogDescriptionConstant.EMPLOYEE_CREATE
    )
    public ResponseEntity<ApiResponse<Void>> create(
            @Valid @RequestBody CreateEmployeeUserRequest createEmployeeUserRequest,
            HttpServletRequest request) {
        Integer userId = (Integer) request.getAttribute("currentUserId");

        service.createEmployee(userId, createEmployeeUserRequest);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}")
    @Audit(
            entity = EntityType.USER,
            action = AuditLogAction.UPDATE,
            description = AuditLogDescriptionConstant.EMPLOYEE_UPDATE,
            params = {"id"}
    )
    public ResponseEntity<ApiResponse<Void>> update(@PathVariable int id,
            @Valid @RequestBody UpdateEmployeeUserRequest updateEmployeeUserRequest,
            HttpServletRequest request) {
        Integer userId = (Integer) request.getAttribute("currentUserId");

        service.updateEmployee(userId, id, updateEmployeeUserRequest);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/work-history")
    public ResponseEntity<ApiResponse<ListResponse<ShopWorkHistoryListUserDto>>> listWorkHistory(
            @PathVariable int id,
            @Valid ShopWorkHistorySearchUserRequest searchRequest,
            HttpServletRequest request) {
        Integer userId = (Integer) request.getAttribute("currentUserId");

        return ResponseEntity.ok(ApiResponse.success(service.listWorkHistory(userId, id, searchRequest)));
    }

    @GetMapping("/{id}/logs")
    public ResponseEntity<ApiResponse<ListResponse<BaseAuditLogDto>>> listAuditLogsByUserId(
            @PathVariable Integer id,
            @Valid AuditLogSearchRequest auditLogSearchRequest,
            HttpServletRequest request) {

        Integer userId = (Integer) request.getAttribute("currentUserId");

        ListResponse<BaseAuditLogDto> result = service.listAuditLogsByUserId(userId, id, auditLogSearchRequest);
        return ResponseEntity.ok(ApiResponse.success(result));
    }


    @GetMapping("/{id}/logs/export")
    @Audit(
            entity = EntityType.AUDIT_LOG,
            action = AuditLogAction.EXPORT,
            description = AuditLogDescriptionConstant.AUDIT_LOG_EXPORT_BY_EMPLOYEE
    )
    public ResponseEntity<byte[]> exportAuditLogsByUserId(
            @PathVariable Integer id,
            AuditLogSearchRequest auditLogSearchRequest,
            HttpServletRequest request) throws Exception {

        Integer userId = (Integer) request.getAttribute("currentUserId");

        byte[] data = service.exportAuditLogsByUserId(userId, id, auditLogSearchRequest);

        String fileName = "UTE Logistics_Báo cáo lịch sử hoạt động của nhân viên.xlsx";
        String encodedFileName = URLEncoder.encode(fileName, StandardCharsets.UTF_8.toString())
                .replaceAll("\\+", "%20");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        headers.add(HttpHeaders.CONTENT_DISPOSITION,
                "attachment; filename*=UTF-8''" + encodedFileName);

        return ResponseEntity.ok()
                .headers(headers)
                .body(data);
    }
}