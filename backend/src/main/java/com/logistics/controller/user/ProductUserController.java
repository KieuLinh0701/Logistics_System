package com.logistics.controller.user;

import com.logistics.dto.ProductDto;
import com.logistics.request.user.product.UserBulkProductForm;
import com.logistics.request.user.product.UserProductForm;
import com.logistics.request.user.product.UserProductSearchRequest;
import com.logistics.response.ApiResponse;
import com.logistics.response.BulkResponse;
import com.logistics.response.ListResponse;
import com.logistics.service.user.ProductUserService;
import com.logistics.utils.SecurityUtils;

import jakarta.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/user/products")
public class ProductUserController {

    @Autowired
    private ProductUserService service;

    private boolean isNotPermitRole() {
        return !SecurityUtils.hasRole("user");
    }

    @GetMapping
    public ResponseEntity<ApiResponse<ListResponse<ProductDto>>> list(@Valid UserProductSearchRequest request) {
        if (isNotPermitRole()) {
            return ResponseEntity.status(403).body(new ApiResponse<>(false, "Không có quyền truy cập", null));
        }

        Integer userId;
        try {
            userId = SecurityUtils.getAuthenticatedUserId();
        } catch (RuntimeException e) {
            ApiResponse<ListResponse<ProductDto>> response = new ApiResponse<>(false, e.getMessage(), null);
            return ResponseEntity.status(401).body(response);
        }

        return ResponseEntity.ok(service.list(userId, request));
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<ProductDto>> create(@ModelAttribute UserProductForm request) {
        System.out.println(isNotPermitRole());
        if (isNotPermitRole()) {
            System.out.println(isNotPermitRole());
            return ResponseEntity.status(403).body(new ApiResponse<>(false, "Không có quyền truy cập", null));
        }

        Integer userId;
        try {
            userId = SecurityUtils.getAuthenticatedUserId();
        } catch (RuntimeException e) {
            ApiResponse<ProductDto> response = new ApiResponse<>(false, e.getMessage(), null);
            return ResponseEntity.status(401).body(response);
        }

        return ResponseEntity.ok(service.create(userId, request));
    }

    @PutMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<ProductDto>> update(@ModelAttribute UserProductForm request) {
        System.out.println(isNotPermitRole());
        if (isNotPermitRole()) {
            return ResponseEntity.status(403).body(new ApiResponse<>(false, "Không có quyền truy cập", null));
        }

        Integer userId;
        try {
            userId = SecurityUtils.getAuthenticatedUserId();
        } catch (RuntimeException e) {
            ApiResponse<ProductDto> response = new ApiResponse<>(false, e.getMessage(), null);
            return ResponseEntity.status(401).body(response);
        }

        return ResponseEntity.ok(service.update(userId, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<ProductDto>> delete(@PathVariable Integer id) {
        if (isNotPermitRole()) {
            return ResponseEntity.status(403).body(new ApiResponse<>(false, "Không có quyền truy cập", null));
        }

        Integer userId;
        try {
            userId = SecurityUtils.getAuthenticatedUserId();
        } catch (RuntimeException e) {
            ApiResponse<ProductDto> response = new ApiResponse<>(false, e.getMessage(), null);
            return ResponseEntity.status(401).body(response);
        }

        return ResponseEntity.ok(service.delete(userId, id));
    }

    @PostMapping(value = "/bulk", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<BulkResponse<ProductDto>> createBulk(@ModelAttribute UserBulkProductForm request) {

        if (isNotPermitRole()) {
            BulkResponse<ProductDto> response = new BulkResponse<>(
                false, "Không có quyền truy cập", 0, 0, null);
            return ResponseEntity.status(403).body(response);
        }

        Integer userId;
        try {
            userId = SecurityUtils.getAuthenticatedUserId();
        } catch (RuntimeException e) {
            BulkResponse<ProductDto> response = new BulkResponse<>(
                false, e.getMessage(), 0, 0, null);
            return ResponseEntity.status(401).body(response);
        }

        return ResponseEntity.ok(service.createBulk(userId, request));
    }
}
