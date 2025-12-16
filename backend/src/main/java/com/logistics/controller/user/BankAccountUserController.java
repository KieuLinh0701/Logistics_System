package com.logistics.controller.user;

import java.util.List;

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
        
        return ResponseEntity.ok(service.list(userId));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<BankAccountDto>> create(@RequestBody BankAccountRequest bankAccountRequest,
            HttpServletRequest request) {
        Integer userId = (Integer) request.getAttribute("currentUserId");
        
        return ResponseEntity.ok(service.create(userId, bankAccountRequest));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<BankAccountDto>> update(@PathVariable Integer id,
            @RequestBody BankAccountRequest bankAccountRequest,
            HttpServletRequest request) {

        Integer userId = (Integer) request.getAttribute("currentUserId");
        
        return ResponseEntity.ok(service.update(userId, id, bankAccountRequest));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Boolean>> delete(@PathVariable Integer id,
            HttpServletRequest request) {
        Integer userId = (Integer) request.getAttribute("currentUserId");
        
        return ResponseEntity.ok(service.delete(userId, id));
    }

    @PatchMapping("/{id}/default")
    public ResponseEntity<ApiResponse<Boolean>> setDefault(@PathVariable Integer id,
            HttpServletRequest request) {
        Integer userId = (Integer) request.getAttribute("currentUserId");
        
        return ResponseEntity.ok(service.setDefault(userId, id));
    }
}