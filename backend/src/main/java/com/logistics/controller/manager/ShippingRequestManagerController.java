package com.logistics.controller.manager;

import com.logistics.audit.Audit;
import com.logistics.constants.AuditLogDescriptionConstant;
import com.logistics.dto.manager.shippingRequest.ManagerShippingRequestDetailDto;
import com.logistics.dto.manager.shippingRequest.ManagerShippingRequestListDto;
import com.logistics.enums.AuditLogAction;
import com.logistics.enums.EntityType;
import com.logistics.request.manager.shippingRequest.ManagerShippingRequestForm;
import com.logistics.request.manager.shippingRequest.ManagerShippingRequestSearchRequest;
import com.logistics.response.ApiResponse;
import com.logistics.response.ListResponse;
import com.logistics.service.manager.ShippingRequestManagerService;
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
@RequestMapping("/api/manager/shipping-requests")
@Tag(name = "Manager - Shipping Request", description = "Quản lý yêu cầu hỗ trợ và khiếu nại của khách hàng tại bưu cục")
public class ShippingRequestManagerController {

    @Autowired
    private ShippingRequestManagerService service;

    @GetMapping()
    public ResponseEntity<ApiResponse<ListResponse<ManagerShippingRequestListDto>>> list(
            @Valid ManagerShippingRequestSearchRequest managerShippingRequestSearchRequest,
            HttpServletRequest request) {
        Integer userId = (Integer) request.getAttribute("currentUserId");

        ListResponse<ManagerShippingRequestListDto> result = service.list(userId,
                managerShippingRequestSearchRequest);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ManagerShippingRequestDetailDto>> getShippingRequestById(
            @PathVariable Integer id,
            HttpServletRequest request) {
        Integer userId = (Integer) request.getAttribute("currentUserId");

        ManagerShippingRequestDetailDto result = service.getShippingRequestById(userId, id);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Audit(
            entity = EntityType.SHIPPING_REQUEST,
            action = AuditLogAction.PROCESS,
            description = AuditLogDescriptionConstant.SHIPPING_REQUEST_PROCESSING,
            params = {"id"}
    )
    public ResponseEntity<ApiResponse<Void>> processing(@PathVariable Integer id,
            @ModelAttribute ManagerShippingRequestForm managerShippingRequestForm,
            HttpServletRequest request) {
        Integer userId = (Integer) request.getAttribute("currentUserId");

        service.processing(userId, id, managerShippingRequestForm);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @GetMapping("/export")
    @Audit(
            entity = EntityType.SHIPPING_REQUEST,
            action = AuditLogAction.EXPORT,
            description = AuditLogDescriptionConstant.SHIPPING_REQUEST_EXPORT
    )
    public ResponseEntity<byte[]> export(
            HttpServletRequest request,
            ManagerShippingRequestSearchRequest managerShippingRequestSearchRequest) throws Exception {

        Integer userId = (Integer) request.getAttribute("currentUserId");
        byte[] data = service.export(userId, managerShippingRequestSearchRequest);

        String fileName = "UTE Logistics_Báo cáo danh sách yêu cầu hỗ trợ & khiếu nại.xlsx";
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