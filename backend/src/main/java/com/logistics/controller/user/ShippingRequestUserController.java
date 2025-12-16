package com.logistics.controller.user;

import org.springframework.beans.factory.annotation.Autowired;
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

@RestController
@RequestMapping("/api/user/shipping-requests")
public class ShippingRequestUserController {

    @Autowired
    private ShippingRequestUserService service;

    @GetMapping()
    public ResponseEntity<ApiResponse<ListResponse<UserShippingRequestListDto>>> list(
            @Valid UserShippingRequestSearchRequest userShippingRequestSearchRequest,
            HttpServletRequest request) {
        Integer userId = (Integer) request.getAttribute("currentUserId");
        
        ApiResponse<ListResponse<UserShippingRequestListDto>> result = service.list(userId,
                userShippingRequestSearchRequest);
        return ResponseEntity.ok(result);
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<Boolean>> create(@ModelAttribute UserShippingRequestForm userShippingRequestForm,
            HttpServletRequest request) {
        Integer userId = (Integer) request.getAttribute("currentUserId");
        
        return ResponseEntity.ok(service.create(userId, userShippingRequestForm));
    }

    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<Boolean>> update(@PathVariable Integer id,
            @ModelAttribute UserShippingRequestForm userShippingRequestForm,
            HttpServletRequest request) {
        Integer userId = (Integer) request.getAttribute("currentUserId");
        
        return ResponseEntity.ok(service.update(userId, id, userShippingRequestForm));
    } 

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<UserShippingRequestDetailDto>> getShippingRequestById(
            @PathVariable Integer id,
            HttpServletRequest request) {
        Integer userId = (Integer) request.getAttribute("currentUserId");
        
        ApiResponse<UserShippingRequestDetailDto> result = service.getShippingRequestById(userId, id);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/{id}/edit")
    public ResponseEntity<ApiResponse<UserShippingRequestEditDto>> getShippingRequestByIdForEdit(
            @PathVariable Integer id,
            HttpServletRequest request) {
        Integer userId = (Integer) request.getAttribute("currentUserId");
        
        ApiResponse<UserShippingRequestEditDto> result = service.getShippingRequestByIdForEdit(userId, id);
        return ResponseEntity.ok(result);
    }

    @PatchMapping("/{id}/cancel")
    public ResponseEntity<ApiResponse<Boolean>> cancel(
            @PathVariable Integer id,
            HttpServletRequest request) {
        Integer userId = (Integer) request.getAttribute("currentUserId");
        
        ApiResponse<Boolean> result = service.cancel(userId, id);
        return ResponseEntity.ok(result);
    }
}