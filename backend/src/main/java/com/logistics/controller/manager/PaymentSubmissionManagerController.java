package com.logistics.controller.manager;

import com.logistics.audit.Audit;
import com.logistics.constants.AuditLogDescriptionConstant;
import com.logistics.dto.manager.paymentSubmission.ManagerPaymentSubmissionListDto;
import com.logistics.enums.AuditLogAction;
import com.logistics.enums.EntityType;
import com.logistics.request.SearchRequest;
import com.logistics.request.manager.ManagerPaymentSubmissionForm;
import com.logistics.response.ApiResponse;
import com.logistics.response.ListResponse;
import com.logistics.service.manager.PaymentSubmissionManagerService;
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
@RequestMapping("/api/manager/payment-submissions")
@Tag(name = "Manager - Payment Submission", description = "Quản lý chi tiết các khoản đối soát thanh toán và xuất báo cáo cho từng phiên đối soát")
public class PaymentSubmissionManagerController {

        @Autowired
        private PaymentSubmissionManagerService service;

        @GetMapping("/{id}")
        public ResponseEntity<ApiResponse<ListResponse<ManagerPaymentSubmissionListDto>>> list(
                        @PathVariable Integer id,
                        @Valid SearchRequest searchRequest,
                        HttpServletRequest request) {
                Integer userId = (Integer) request.getAttribute("currentUserId");

                ListResponse<ManagerPaymentSubmissionListDto> result = service.list(userId, id,
                                searchRequest);
                return ResponseEntity.ok(ApiResponse.success(result));
        }

        @PutMapping("/{id}")
        @Audit(
                entity = EntityType.PAYMENT_SUBMISSION,
                action = AuditLogAction.PROCESS,
                description = AuditLogDescriptionConstant.PAYMENT_SUBMISSION_PROCESSING,
                params = {"id"}
        )
        public ResponseEntity<ApiResponse<Void>> processing(@PathVariable Integer id,
                        @RequestBody @Valid ManagerPaymentSubmissionForm form,
                        HttpServletRequest request) {
                Integer userId = (Integer) request.getAttribute("currentUserId");

                service.processing(userId, id, form);
                return ResponseEntity.ok(ApiResponse.success(null));
        }

        @GetMapping("/{id}/export")
        @Audit(
                entity = EntityType.PAYMENT_SUBMISSION,
                action = AuditLogAction.EXPORT,
                description = AuditLogDescriptionConstant.PAYMENT_SUBMISSION_EXPORT
        )
        public ResponseEntity<byte[]> exportExcel(HttpServletRequest request,
                        @PathVariable Integer id,
                        SearchRequest searchRequest) throws Exception {

                Integer userId = (Integer) request.getAttribute("currentUserId");
                byte[] data = service.export(userId, id, searchRequest);

                String fileName = "UTE Logistics_Báo cáo đối soát.xlsx";
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