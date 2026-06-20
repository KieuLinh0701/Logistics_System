package com.logistics.controller.manager;

import com.logistics.dto.manager.paymentSubmissionBatch.ManagerPaymentSubmissionBatchListDto;
import com.logistics.request.SearchRequest;
import com.logistics.request.manager.paymentSubmissionBatch.ManagerPaymentSubmissionBatchEditForm;
import com.logistics.response.ApiResponse;
import com.logistics.response.ListResponse;
import com.logistics.service.manager.PaymentSubmissionBatchManagerService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/manager/payment-submission-batchs")
@Tag(name = "Manager - Payment Submission Batch", description = "Quản lý các phiên đối soát thanh toán và xuất báo cáo tại bưu cục")
public class PaymentSubmissionBatchManagerController {

    private final PaymentSubmissionBatchManagerService service;

    @GetMapping()
    public ResponseEntity<ApiResponse<ListResponse<ManagerPaymentSubmissionBatchListDto>>> list(
            @Valid SearchRequest searchRequest,
            HttpServletRequest request) {
        Integer userId = (Integer) request.getAttribute("currentUserId");

        ListResponse<ManagerPaymentSubmissionBatchListDto> result = service.list(
                userId,
                searchRequest);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> processing(@PathVariable Integer id,
                                                        @RequestBody @Valid ManagerPaymentSubmissionBatchEditForm form,
                                                        HttpServletRequest request) {
        Integer userId = (Integer) request.getAttribute("currentUserId");

        service.processing(userId, id, form);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @GetMapping("/export")
    public ResponseEntity<byte[]> exportExcel(HttpServletRequest request,
                                              SearchRequest searchRequest) throws Exception {

        Integer userId = (Integer) request.getAttribute("currentUserId");
        byte[] data = service.export(userId, searchRequest);

        String fileName = "UTE Logistics_Báo cáo phiên đối soát.xlsx";
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