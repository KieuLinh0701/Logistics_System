package com.logistics.controller.manager;

import com.logistics.audit.Audit;
import com.logistics.constants.AuditLogDescriptionConstant;
import com.logistics.dto.OfficeDto;
import com.logistics.enums.AuditLogAction;
import com.logistics.enums.EntityType;
import com.logistics.request.manager.ManagerOfficeEditRequest;
import com.logistics.response.ApiResponse;
import com.logistics.service.manager.OfficeManagerService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/manager/offices")
@Tag(name = "Manager - Office", description = "Quản lý thông tin bưu cục của quản lý")
public class OfficeManagerController {

    private final OfficeManagerService service;

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<OfficeDto>> getMyOffice(HttpServletRequest request) {
        Integer userId = (Integer) request.getAttribute("currentUserId");
        return ResponseEntity.ok(ApiResponse.success(service.getMyOffice(userId)));
    }

    @PutMapping("/me")
    @Audit(
            entity = EntityType.OFFICE,
            action = AuditLogAction.UPDATE,
            description = AuditLogDescriptionConstant.OFFICE_UPDATE
    )
    public ResponseEntity<ApiResponse<Boolean>> updateMyOffice(HttpServletRequest request,
    @RequestBody ManagerOfficeEditRequest managerOfficeEditRequest
    ) {
        Integer userId = (Integer) request.getAttribute("currentUserId");

        service.updateMyOffice(userId, managerOfficeEditRequest);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @GetMapping("/me/city-code")
    public ResponseEntity<ApiResponse<Integer>> getMyOfficeCityCode(HttpServletRequest request) {
        Integer userId = (Integer) request.getAttribute("currentUserId");
        return ResponseEntity.ok(ApiResponse.success(service.getMyOfficeCityCode(userId)));
    }
}
