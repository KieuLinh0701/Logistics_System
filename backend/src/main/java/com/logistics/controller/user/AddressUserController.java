package com.logistics.controller.user;

import com.logistics.dto.AddressDto;
import com.logistics.request.user.address.AddressUserRequest;
import com.logistics.response.ApiResponse;
import com.logistics.service.user.AddressUserService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/user/addresses")
public class AddressUserController {

    @Autowired
    private AddressUserService service;

    @GetMapping
    public ResponseEntity<ApiResponse<List<com.logistics.dto.AddressDto>>> list(HttpServletRequest request) {
        Integer userId = (Integer) request.getAttribute("currentUserId");
        
        return ResponseEntity.ok(service.list(userId));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<AddressDto>> create(@Valid @RequestBody AddressUserRequest addressRequest,
            HttpServletRequest request) {
        Integer userId = (Integer) request.getAttribute("currentUserId");

        return ResponseEntity.ok(service.create(userId, addressRequest));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<AddressDto>> update(@PathVariable int id,
            @Valid @RequestBody AddressUserRequest addressRequest,
            HttpServletRequest request) {
        Integer userId = (Integer) request.getAttribute("currentUserId");
        
        return ResponseEntity.ok(service.update(userId, id, addressRequest));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Boolean>> delete(@PathVariable int id,
            HttpServletRequest request) {
        Integer userId = (Integer) request.getAttribute("currentUserId");
        
        return ResponseEntity.ok(service.delete(userId, id));
    }

    @PatchMapping("/{id}/default")
    public ResponseEntity<ApiResponse<Boolean>> setDefault(@PathVariable int id,
            HttpServletRequest request) {
        Integer userId = (Integer) request.getAttribute("currentUserId");
        
        return ResponseEntity.ok(service.setDefault(userId, id));
    }
}