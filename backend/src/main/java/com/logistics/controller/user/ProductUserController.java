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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/user/products")
public class ProductUserController {

    @Autowired
    private ProductUserService service;

    @GetMapping
    public ResponseEntity<ApiResponse<ListResponse<ProductDto>>> list(
            @Valid UserProductSearchRequest userProductSearchRequest,
            HttpServletRequest request) {
        Integer userId = (Integer) request.getAttribute("currentUserId");

        return ResponseEntity.ok(service.list(userId, userProductSearchRequest));
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<ProductDto>> create(@ModelAttribute UserProductForm userProductForm,
            HttpServletRequest request) {
        Integer userId = (Integer) request.getAttribute("currentUserId");

        return ResponseEntity.ok(service.create(userId, userProductForm));
    }

    @PutMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<ProductDto>> update(@ModelAttribute UserProductForm userProductForm,
            HttpServletRequest request) {

        Integer userId = (Integer) request.getAttribute("currentUserId");
        return ResponseEntity.ok(service.update(userId, userProductForm));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<ProductDto>> delete(@PathVariable Integer id,
            HttpServletRequest request) {
        Integer userId = (Integer) request.getAttribute("currentUserId");

        return ResponseEntity.ok(service.delete(userId, id));
    }

    @PostMapping(value = "/bulk", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<BulkResponse<ProductDto>> createBulk(@ModelAttribute UserBulkProductForm userBulkProductForm,
            HttpServletRequest request) {

        Integer userId = (Integer) request.getAttribute("currentUserId");

        return ResponseEntity.ok(service.createBulk(userId, userBulkProductForm));
    }

    @GetMapping("/active")
    public ResponseEntity<ApiResponse<ListResponse<ProductDto>>> getActiveAndInstockUserProducts(
            @Valid @ModelAttribute UserProductSearchRequest userProductSearchRequest,
            HttpServletRequest request) {
        Integer userId = (Integer) request.getAttribute("currentUserId");

        return ResponseEntity.ok(service.getActiveAndInstockUserProducts(userId, userProductSearchRequest));
    }
}
