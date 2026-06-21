package com.logistics.controller.manager;

import com.logistics.audit.Audit;
import com.logistics.constants.AuditLogDescriptionConstant;
import com.logistics.dto.manager.audit.ManagerAuditLogDto;
import com.logistics.enums.AuditLogAction;
import com.logistics.enums.EntityType;
import com.logistics.request.manager.audit.AuditLogSearchRequest;
import com.logistics.response.ApiResponse;
import com.logistics.response.ListResponse;
import com.logistics.service.manager.AuditLogManagerService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
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
@RequestMapping("/api/manager/logs")
@Tag(name = "Manager - Audit Logs", description = "Quản lý và theo dõi nhật ký hoạt động bưu cục, hỗ trợ xuất báo cáo")
public class AuditLogManagerController {

    private final AuditLogManagerService service;

    @GetMapping
    public ResponseEntity<ApiResponse<ListResponse<ManagerAuditLogDto>>> list(
            @Valid AuditLogSearchRequest auditLogSearchRequest,
            HttpServletRequest request) {
        Integer userId = (Integer) request.getAttribute("currentUserId");

        ListResponse<ManagerAuditLogDto> result = service.list(userId, auditLogSearchRequest);
        return ResponseEntity.ok(ApiResponse.success(result));
    }


    @GetMapping("/export")
    @Audit(
            entity = EntityType.AUDIT_LOG,
            action = AuditLogAction.EXPORT,
            description = AuditLogDescriptionConstant.AUDIT_LOG_EXPORT
    )
    public ResponseEntity<byte[]> export(HttpServletRequest request,
                                         AuditLogSearchRequest auditLogSearchRequest) throws Exception {

        Integer userId = (Integer) request.getAttribute("currentUserId");
        byte[] data = service.export(userId, auditLogSearchRequest);

        String fileName = "UTE Logistics_Báo cáo lịch sử hoạt động của bưu cục.xlsx";
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