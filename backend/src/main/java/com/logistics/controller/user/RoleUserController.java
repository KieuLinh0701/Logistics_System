package com.logistics.controller.user;

import com.logistics.dto.user.role.RoleDetailUserDto;
import com.logistics.dto.user.role.RoleListUserDto;
import com.logistics.request.user.role.RoleSearchUserRequest;
import com.logistics.request.user.role.RoleUserRequest;
import com.logistics.response.ApiResponse;
import com.logistics.response.ListResponse;
import com.logistics.service.user.RoleUserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/user/roles")
public class RoleUserController {

    @Autowired
    private RoleUserService service;

    @GetMapping
    public ResponseEntity<ApiResponse<ListResponse<RoleListUserDto>>> list(
            @Valid RoleSearchUserRequest roleSearchUserRequest,
            HttpServletRequest request) {
        Integer userId = (Integer) request.getAttribute("currentUserId");

        return ResponseEntity.ok(ApiResponse.success(service.list(userId, roleSearchUserRequest)));
    }

    @GetMapping("/all")
    public ResponseEntity<ApiResponse<List<RoleListUserDto>>> findAll(
            HttpServletRequest request) {
        Integer userId = (Integer) request.getAttribute("currentUserId");

        return ResponseEntity.ok(ApiResponse.success(service.findAll(userId)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<RoleDetailUserDto>> detail(
            @PathVariable int id,
            HttpServletRequest request) {
        Integer userId = (Integer) request.getAttribute("currentUserId");

        return ResponseEntity.ok(ApiResponse.success(service.detail(userId, id)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<Void>> create(
            @Valid @RequestBody RoleUserRequest roleUserRequest,
            HttpServletRequest request) {
        Integer userId = (Integer) request.getAttribute("currentUserId");

        service.create(userId, roleUserRequest);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> update(@PathVariable int id,
            @Valid @RequestBody RoleUserRequest roleUserRequest,
            HttpServletRequest request) {
        Integer userId = (Integer) request.getAttribute("currentUserId");

        service.update(userId, id, roleUserRequest);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable int id,
            HttpServletRequest request) {
        Integer userId = (Integer) request.getAttribute("currentUserId");

        service.delete(userId, id);
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}