package com.logistics.controller.user;

import com.logistics.dto.ProductDto;
import com.logistics.request.user.product.UserBulkProductForm;
import com.logistics.request.user.product.UserProductForm;
import com.logistics.request.user.product.UserProductSearchRequest;
import com.logistics.response.ApiResponse;
import com.logistics.response.BulkResponse;
import com.logistics.response.ListResponse;
import com.logistics.service.user.ProductUserService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/user/products")
public class ProductUserController {

    private final ProductUserService service;

    @GetMapping
    public ResponseEntity<ApiResponse<ListResponse<ProductDto>>> list(
            @Valid UserProductSearchRequest userProductSearchRequest,
            HttpServletRequest request) {
        Integer userId = (Integer) request.getAttribute("currentUserId");

        return ResponseEntity.ok(ApiResponse.success(service.list(userId, userProductSearchRequest)));
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<ProductDto>> create(
            @Valid @ModelAttribute UserProductForm userProductForm,
            HttpServletRequest request) {
        Integer userId = (Integer) request.getAttribute("currentUserId");

        return ResponseEntity.ok(ApiResponse.success(service.create(userId, userProductForm)));
    }

    @PutMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<ProductDto>> update(
            @Valid @ModelAttribute UserProductForm userProductForm,
            HttpServletRequest request) {

        Integer userId = (Integer) request.getAttribute("currentUserId");
        return ResponseEntity.ok(ApiResponse.success(service.update(userId, userProductForm)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<ProductDto>> delete(@PathVariable Integer id,
            HttpServletRequest request) {
        Integer userId = (Integer) request.getAttribute("currentUserId");

        return ResponseEntity.ok(ApiResponse.success(service.delete(userId, id)));
    }

    @PostMapping(value = "/bulk", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<BulkResponse<ProductDto>> createBulk(
            @Valid @ModelAttribute UserBulkProductForm userBulkProductForm,
            HttpServletRequest request) {

        Integer userId = (Integer) request.getAttribute("currentUserId");

        return ResponseEntity.ok(service.createBulk(userId, userBulkProductForm));
    }

    @GetMapping("/active")
    public ResponseEntity<ApiResponse<ListResponse<ProductDto>>> getActiveAndInstockUserProducts(
            @Valid @ModelAttribute UserProductSearchRequest userProductSearchRequest,
            HttpServletRequest request) {
        Integer userId = (Integer) request.getAttribute("currentUserId");

        return ResponseEntity.ok(ApiResponse.success(service.getActiveAndInstockUserProducts(userId, userProductSearchRequest)));
    }

    @GetMapping("/export")
    public ResponseEntity<byte[]> export(
            HttpServletRequest request,
            UserProductSearchRequest userProductSearchRequest) throws Exception {

        Integer userId = (Integer) request.getAttribute("currentUserId");
        byte[] data = service.export(userId, userProductSearchRequest);

        String fileName = "UTE Logistics_Báo cáo sản phẩm.xlsx";
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
