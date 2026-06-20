package com.logistics.controller.user;

import com.logistics.audit.Audit;
import com.logistics.constants.AuditLogDescriptionConstant;
import com.logistics.dto.BankAccountDto;
import com.logistics.enums.AuditLogAction;
import com.logistics.enums.EntityType;
import com.logistics.request.user.bankAccount.BankAccountRequest;
import com.logistics.response.ApiResponse;
import com.logistics.service.user.BankAccountUserService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/user/bank-accounts")
@Tag(name = "User - Bank Account", description = "Quản lý thông tin tài khoản ngân hàng của người dùng để thực hiện giao dịch thanh toán")
public class BankAccountUserController {

    @Autowired
    private BankAccountUserService service;

    @GetMapping
    public ResponseEntity<ApiResponse<List<BankAccountDto>>> list(HttpServletRequest request) {
        Integer userId = (Integer) request.getAttribute("currentUserId");

        return ResponseEntity.ok(ApiResponse.success(service.list(userId)));
    }

    @PostMapping
    @Audit(
            entity = EntityType.BANK_ACCOUNT,
            action = AuditLogAction.CREATE,
            description = AuditLogDescriptionConstant.BANK_ACCOUNT_CREATE
    )
    public ResponseEntity<ApiResponse<BankAccountDto>> create(
            @Valid @RequestBody BankAccountRequest bankAccountRequest,
            HttpServletRequest request) {
        Integer userId = (Integer) request.getAttribute("currentUserId");

        return ResponseEntity.ok(ApiResponse.success(service.create(userId, bankAccountRequest)));
    }

    @PutMapping("/{id}")
    @Audit(
            entity = EntityType.BANK_ACCOUNT,
            action = AuditLogAction.UPDATE,
            description = AuditLogDescriptionConstant.BANK_ACCOUNT_UPDATE,
            params = {"id"}
    )
    public ResponseEntity<ApiResponse<BankAccountDto>> update(
            @PathVariable Integer id,
            @Valid @RequestBody BankAccountRequest bankAccountRequest,
            HttpServletRequest request) {

        Integer userId = (Integer) request.getAttribute("currentUserId");

        return ResponseEntity.ok(ApiResponse.success(service.update(userId, id, bankAccountRequest)));
    }

    @DeleteMapping("/{id}")
    @Audit(
            entity = EntityType.BANK_ACCOUNT,
            action = AuditLogAction.DELETE,
            description = AuditLogDescriptionConstant.BANK_ACCOUNT_DELETE,
            params = {"id"}
    )
    public ResponseEntity<ApiResponse<Void>> delete(
            @PathVariable Integer id,
            HttpServletRequest request) {
        Integer userId = (Integer) request.getAttribute("currentUserId");

        service.delete(userId, id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/default")
    @Audit(
            entity = EntityType.BANK_ACCOUNT,
            action = AuditLogAction.UPDATE,
            description = AuditLogDescriptionConstant.BANK_ACCOUNT_SET_DEFAULT,
            params = {"id"}
    )
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