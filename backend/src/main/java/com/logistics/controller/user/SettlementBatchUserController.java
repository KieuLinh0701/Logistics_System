package com.logistics.controller.user;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.logistics.dto.user.UserSettlementBatchListDto;
import com.logistics.dto.user.UserSettlementOrderDto;
import com.logistics.dto.user.UserSettlementTransactionDto;
import com.logistics.request.SearchRequest;
import com.logistics.response.ApiResponse;
import com.logistics.response.ListResponse;
import com.logistics.service.user.SettlementBatchUserService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@RestController
@RequestMapping("/api/user/settlement-batchs")
public class SettlementBatchUserController {

        @Autowired
        private SettlementBatchUserService service;

        @GetMapping()
        public ResponseEntity<ApiResponse<ListResponse<UserSettlementBatchListDto>>> list(
                        @Valid SearchRequest searchRequest,
                        HttpServletRequest request) {
                Integer userId = (Integer) request.getAttribute("currentUserId");

                ApiResponse<ListResponse<UserSettlementBatchListDto>> result = service.list(userId,
                                searchRequest);
                return ResponseEntity.ok(result);
        }

        @GetMapping("/{id}/orders")
        public ResponseEntity<ApiResponse<ListResponse<UserSettlementOrderDto>>> getOrdersBySettlementBatchId(
                        @PathVariable Integer id,
                        @Valid SearchRequest searchRequest,
                        HttpServletRequest request) {
                Integer userId = (Integer) request.getAttribute("currentUserId");

                ApiResponse<ListResponse<UserSettlementOrderDto>> result = service.getOrdersBySettlementBatchId(
                                userId,
                                id,
                                searchRequest);
                return ResponseEntity.ok(result);
        } 

        @GetMapping("/{id}/transactions")
        public ResponseEntity<ApiResponse<List<UserSettlementTransactionDto>>> getSettlementTransactionsBySettlementBatchId(
                        @PathVariable Integer id,
                        @Valid SearchRequest searchRequest,
                        HttpServletRequest request) {
                Integer userId = (Integer) request.getAttribute("currentUserId");

                ApiResponse<List<UserSettlementTransactionDto>> result = service.getSettlementTransactionsBySettlementBatchId(
                                userId,
                                id);
                return ResponseEntity.ok(result);
        } 

        // @GetMapping("/export")
        // public ResponseEntity<byte[]> exportExcel(HttpServletRequest request,
        // SearchRequest searchRequest) throws Exception {

        // Integer userId = (Integer) request.getAttribute("currentUserId");
        // byte[] data = service.export(userId, searchRequest);

        // String fileName = "UTE Logistics_Báo cáo phiên đối soát.xlsx";
        // String encodedFileName = URLEncoder.encode(fileName,
        // StandardCharsets.UTF_8.toString())
        // .replaceAll("\\+", "%20");

        // HttpHeaders headers = new HttpHeaders();
        // headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        // headers.add(HttpHeaders.CONTENT_DISPOSITION,
        // "attachment; filename*=UTF-8''" + encodedFileName);

        // return ResponseEntity.ok()
        // .headers(headers)
        // .body(data);
        // }
}