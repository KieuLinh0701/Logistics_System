package com.logistics.controller.user;

import com.logistics.request.user.recipientaddress.RecipientAddressUserRequest;
import com.logistics.request.user.recipientaddress.RecipientSuggestionRequest;
import com.logistics.request.user.recipientaddress.UserRecipientAddressSearchRequest;
import com.logistics.response.ApiResponse;
import com.logistics.response.ListResponse;
import com.logistics.response.user.recipientaddress.RecipientAddressResponse;
import com.logistics.response.user.recipientaddress.RecipientSuggestionAddressResponse;
import com.logistics.service.user.RecipientAddressUserService;
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
@RequestMapping("/api/user/recipient-addresses")
@Tag(name = "User - Recipient Address", description = "Quản lý danh bạ khách hàng nhận hàng: thêm, sửa, xóa, gợi ý thông tin và xuất báo cáo dữ liệu khách hàng")
public class RecipientAddressUserController {

    @Autowired
    private RecipientAddressUserService service;

    @GetMapping
    public ResponseEntity<ApiResponse<ListResponse<RecipientAddressResponse>>> list(
            HttpServletRequest request,
            @Valid UserRecipientAddressSearchRequest userRecipientAddressSearchRequest) {
        Integer userId = (Integer) request.getAttribute("currentUserId");

        return ResponseEntity.ok(ApiResponse.success(service.list(userId, userRecipientAddressSearchRequest)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<RecipientAddressResponse>> create(
            @Valid @RequestBody RecipientAddressUserRequest recipientAddressUserRequest,
            HttpServletRequest request) {
        Integer userId = (Integer) request.getAttribute("currentUserId");

        return ResponseEntity.ok(ApiResponse.success(service.create(userId, recipientAddressUserRequest)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<RecipientAddressResponse>> update(
            @PathVariable int id,
            @Valid @RequestBody RecipientAddressUserRequest recipientAddressUserRequest,
            HttpServletRequest request) {
        Integer userId = (Integer) request.getAttribute("currentUserId");

        return ResponseEntity.ok(ApiResponse.success(service.update(userId, id, recipientAddressUserRequest)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(
            @PathVariable int id,
            HttpServletRequest request) {
        Integer userId = (Integer) request.getAttribute("currentUserId");

        service.delete(userId, id);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @GetMapping("/suggestion")
    public ResponseEntity<ApiResponse<RecipientSuggestionAddressResponse>> getSuggestion(
            RecipientSuggestionRequest recipientSuggestionRequest,
            HttpServletRequest request
    ) {

        Integer userId = (Integer) request.getAttribute("currentUserId");

        return ResponseEntity.ok(ApiResponse.success(service.getRecipientSuggestion(userId, recipientSuggestionRequest)));
    }

    @GetMapping("/export")
    public ResponseEntity<byte[]> export(
            HttpServletRequest request,
            UserRecipientAddressSearchRequest userRecipientAddressSearchRequest) throws Exception {

        Integer userId = (Integer) request.getAttribute("currentUserId");
        byte[] data = service.export(userId, userRecipientAddressSearchRequest);

        String fileName = "UTE Logistics_Báo cáo khách hàng.xlsx";
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