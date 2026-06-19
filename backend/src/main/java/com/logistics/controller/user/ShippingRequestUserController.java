package com.logistics.controller.user;

import com.logistics.request.SearchRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.logistics.dto.user.shippingRequest.UserShippingRequestDetailDto;
import com.logistics.dto.user.shippingRequest.UserShippingRequestEditDto;
import com.logistics.dto.user.shippingRequest.UserShippingRequestListDto;
import com.logistics.request.user.shippingRequest.UserShippingRequestForm;
import com.logistics.request.user.shippingRequest.UserShippingRequestSearchRequest;
import com.logistics.response.ApiResponse;
import com.logistics.response.ListResponse;
import com.logistics.service.user.ShippingRequestUserService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/user/shipping-requests")
public class ShippingRequestUserController {

    private final ShippingRequestUserService service;

    @GetMapping()
    public ResponseEntity<ApiResponse<ListResponse<UserShippingRequestListDto>>> list(
            @Valid UserShippingRequestSearchRequest userShippingRequestSearchRequest,
            HttpServletRequest request) {
        Integer userId = (Integer) request.getAttribute("currentUserId");

        ListResponse<UserShippingRequestListDto> result = service.list(userId,
                userShippingRequestSearchRequest);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<Void>> create(
            @Valid @ModelAttribute UserShippingRequestForm userShippingRequestForm,
            HttpServletRequest request) {
        Integer userId = (Integer) request.getAttribute("currentUserId");

        service.create(userId, userShippingRequestForm);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<Boolean>> update(
            @PathVariable Integer id,
            @Valid @ModelAttribute UserShippingRequestForm userShippingRequestForm,
            HttpServletRequest request) {
        Integer userId = (Integer) request.getAttribute("currentUserId");

        service.update(userId, id, userShippingRequestForm);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<UserShippingRequestDetailDto>> getShippingRequestById(
            @PathVariable Integer id,
            HttpServletRequest request) {
        Integer userId = (Integer) request.getAttribute("currentUserId");

        UserShippingRequestDetailDto result = service.getShippingRequestById(userId, id);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @GetMapping("/{id}/edit")
    public ResponseEntity<ApiResponse<UserShippingRequestEditDto>> getShippingRequestByIdForEdit(
            @PathVariable Integer id,
            HttpServletRequest request) {
        Integer userId = (Integer) request.getAttribute("currentUserId");

        UserShippingRequestEditDto result = service.getShippingRequestByIdForEdit(userId, id);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @PatchMapping("/{id}/cancel")
    public ResponseEntity<ApiResponse<Void>> cancel(
            @PathVariable Integer id,
            HttpServletRequest request) {
        Integer userId = (Integer) request.getAttribute("currentUserId");

        service.cancel(userId, id);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @GetMapping("/export")
    public ResponseEntity<byte[]> export(
            HttpServletRequest request,
            UserShippingRequestSearchRequest userShippingRequestSearchRequest) throws Exception {

        Integer userId = (Integer) request.getAttribute("currentUserId");
        byte[] data = service.export(userId, userShippingRequestSearchRequest);

        String fileName = "UTE Logistics_Báo cáo hỗ trợ và khiếu nại.xlsx";
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