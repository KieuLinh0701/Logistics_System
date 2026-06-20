package com.logistics.controller.user;

import com.logistics.dto.AddressDto;
import com.logistics.request.user.address.AddressUserRequest;
import com.logistics.response.ApiResponse;
import com.logistics.service.user.AddressUserService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/user/addresses")
@Tag(name = "User - Address", description = "Quản lý sổ địa chỉ của người dùng (thêm, sửa, xóa và đặt làm địa chỉ mặc định)")
public class AddressUserController {

    @Autowired
    private AddressUserService service;

    @GetMapping
    public ResponseEntity<ApiResponse<List<AddressDto>>> list(
            HttpServletRequest request) {
        Integer userId = (Integer) request.getAttribute("currentUserId");
        
        return ResponseEntity.ok(ApiResponse.success(service.list(userId)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<AddressDto>> create(
            @Valid @RequestBody AddressUserRequest addressRequest,
            HttpServletRequest request) {
        Integer userId = (Integer) request.getAttribute("currentUserId");

        return ResponseEntity.ok(ApiResponse.success(service.create(userId, addressRequest)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<AddressDto>> update(@PathVariable int id,
            @Valid @RequestBody AddressUserRequest addressRequest,
            HttpServletRequest request) {
        Integer userId = (Integer) request.getAttribute("currentUserId");
        
        return ResponseEntity.ok(ApiResponse.success(service.update(userId, id, addressRequest)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(
            @PathVariable int id,
            HttpServletRequest request) {
        Integer userId = (Integer) request.getAttribute("currentUserId");

        service.delete(userId, id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/default")
    public ResponseEntity<ApiResponse<Void>> setDefault(
            @PathVariable int id,
            HttpServletRequest request) {
        Integer userId = (Integer) request.getAttribute("currentUserId");

        service.setDefault(userId, id);
        return ResponseEntity.noContent().build();
    }
}