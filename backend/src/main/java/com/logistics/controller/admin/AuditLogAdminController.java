package com.logistics.controller.admin;

import com.logistics.audit.Audit;
import com.logistics.constants.AuditLogDescriptionConstant;
import com.logistics.dto.admin.AdminAuditLogDto;
import com.logistics.enums.AuditLogAction;
import com.logistics.enums.EntityType;
import com.logistics.request.manager.audit.AuditLogSearchRequest;
import com.logistics.response.ApiResponse;
import com.logistics.response.ListResponse;
import com.logistics.service.admin.AuditLogAdminService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/admin/logs")
@Tag(name = "Admin - Audit Logs", description = "Quản lý và theo dõi nhật ký hoạt động hệ thống, hỗ trợ xuất báo cáo")
public class AuditLogAdminController {

    private final AuditLogAdminService service;

    @GetMapping
    public ResponseEntity<ApiResponse<ListResponse<AdminAuditLogDto>>> list(
            @Valid AuditLogSearchRequest auditLogSearchRequest) {

        ListResponse<AdminAuditLogDto> result = service.list(auditLogSearchRequest);
        return ResponseEntity.ok(ApiResponse.success(result));
    }


    @GetMapping("/export")
    @Audit(
            entity = EntityType.AUDIT_LOG,
            action = AuditLogAction.EXPORT,
            description = AuditLogDescriptionConstant.AUDIT_LOG_EXPORT
    )
    public ResponseEntity<byte[]> export(AuditLogSearchRequest auditLogSearchRequest) throws Exception {

        byte[] data = service.export(auditLogSearchRequest);

        String fileName = "UTE Logistics_Báo cáo lịch sử hoạt động của hệ thống.xlsx";
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