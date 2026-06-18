package com.logistics.controller.user;

import java.util.List;

import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.logistics.dto.BankAccountDto;
import com.logistics.request.user.bankAccount.BankAccountRequest;
import com.logistics.response.ApiResponse;
import com.logistics.service.user.BankAccountUserService;

import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/api/user/bank-accounts")
public class BankAccountUserController {

    @Autowired
    private BankAccountUserService service;

    @GetMapping
    public ResponseEntity<ApiResponse<List<BankAccountDto>>> list(HttpServletRequest request) {
        Integer userId = (Integer) request.getAttribute("currentUserId");

        return ResponseEntity.ok(ApiResponse.success(service.list(userId)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<BankAccountDto>> create(
            @Valid @RequestBody BankAccountRequest bankAccountRequest,
            HttpServletRequest request) {
        Integer userId = (Integer) request.getAttribute("currentUserId");

        return ResponseEntity.ok(ApiResponse.success(service.create(userId, bankAccountRequest)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<BankAccountDto>> update(
            @PathVariable Integer id,
            @Valid @RequestBody BankAccountRequest bankAccountRequest,
            HttpServletRequest request) {

        Integer userId = (Integer) request.getAttribute("currentUserId");

        return ResponseEntity.ok(ApiResponse.success(service.update(userId, id, bankAccountRequest)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(
            @PathVariable Integer id,
            HttpServletRequest request) {
        Integer userId = (Integer) request.getAttribute("currentUserId");

        service.delete(userId, id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/default")
    public ResponseEntity<ApiResponse<Void>> setDefault(
            @PathVariable Integer id,
            HttpServletRequest request) {
        Integer userId = (Integer) request.getAttribute("currentUserId");

        service.setDefault(userId, id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/exists")
    public ResponseEntity<ApiResponse<Boolean>> hasBankAccount(HttpServletRequest request) {
        Integer userId = (Integer) request.getAttribute("currentUserId");
        
        return ResponseEntity.ok(ApiResponse.success(service.hasBankAccount(userId)));
    }

}