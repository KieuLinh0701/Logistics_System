package com.logistics.controller.manager;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.logistics.dto.manager.paymentSubmissionBatch.ManagerPaymentSubmissionBatchListDto;
import com.logistics.request.SearchRequest;
import com.logistics.request.manager.paymentSubmissionBatch.ManagerPaymentSubmissionBatchCreateForm;
import com.logistics.request.manager.paymentSubmissionBatch.ManagerPaymentSubmissionBatchEditForm;
import com.logistics.response.ApiResponse;
import com.logistics.response.ListResponse;
import com.logistics.service.manager.PaymentSubmissionBatchManagerService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;

@RestController
@RequestMapping("/api/manager/payment-submission-batchs")
public class PaymentSubmissionManagerBatchController {

        @Autowired
        private PaymentSubmissionBatchManagerService service;

        @GetMapping()
        public ResponseEntity<ApiResponse<ListResponse<ManagerPaymentSubmissionBatchListDto>>> list(
                        @Valid SearchRequest searchRequest,
                        HttpServletRequest request) {
                Integer userId = (Integer) request.getAttribute("currentUserId");

                ApiResponse<ListResponse<ManagerPaymentSubmissionBatchListDto>> result = service.list(userId,
                                searchRequest);
                return ResponseEntity.ok(result);
        }

        @PutMapping("/{id}")
        public ResponseEntity<ApiResponse<Boolean>> processing(@PathVariable Integer id,
                        @RequestBody @Valid ManagerPaymentSubmissionBatchEditForm form,
                        HttpServletRequest request) {
                Integer userId = (Integer) request.getAttribute("currentUserId");

                return ResponseEntity.ok(service.processing(userId, id, form));
        }

        @PostMapping
        public ResponseEntity<ApiResponse<Boolean>> create(
                        @RequestBody @Valid ManagerPaymentSubmissionBatchCreateForm form,
                        HttpServletRequest request) {
                Integer userId = (Integer) request.getAttribute("currentUserId");

                return ResponseEntity.ok(service.create(userId, form));
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