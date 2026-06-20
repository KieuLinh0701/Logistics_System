package com.logistics.controller.chat;

import com.logistics.dto.chat.SupportMessageDto;
import com.logistics.dto.chat.SupportTicketDetailDto;
import com.logistics.dto.chat.SupportTicketDto;
import com.logistics.request.chat.CreateSupportTicketRequest;
import com.logistics.request.chat.SendSupportMessageRequest;
import com.logistics.response.ApiResponse;
import com.logistics.service.chat.SupportMessageService;
import com.logistics.service.chat.SupportTicketService;
import com.logistics.utils.SecurityUtils;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Objects;

@RestController
@Validated
@RequiredArgsConstructor
@RequestMapping("/api/support")
@Tag(name = "Support", description = "Hỗ trợ khách hàng và trò chuyện trực tuyến")
public class SupportController {

    private final SupportTicketService supportTicketService;
    private final SupportMessageService supportMessageService;

    @PostMapping("/tickets")
    public ResponseEntity<ApiResponse<SupportTicketDto>> createTicket(@Valid @RequestBody CreateSupportTicketRequest request) {
        Integer accountId = SecurityUtils.getAuthenticatedAccountId();
        String roleName = Objects.requireNonNull(SecurityUtils.getAuthenticatedUserRole())
                .getName();
        return ResponseEntity.ok(supportTicketService.createTicket(accountId, roleName, request));
    }

    @GetMapping("/tickets/my")
    public ResponseEntity<ApiResponse<List<SupportTicketDto>>> getMyTickets() {
        Integer accountId = SecurityUtils.getAuthenticatedAccountId();
        String roleName = Objects.requireNonNull(SecurityUtils.getAuthenticatedUserRole())
                .getName();
        return ResponseEntity.ok(supportTicketService.getMyTickets(accountId, roleName));
    }

    @GetMapping("/tickets/{id}")
    public ResponseEntity<ApiResponse<SupportTicketDetailDto>> getTicketDetail(@PathVariable Integer id) {
        Integer accountId = SecurityUtils.getAuthenticatedAccountId();
        String roleName = Objects.requireNonNull(SecurityUtils.getAuthenticatedUserRole())
                .getName();
        return ResponseEntity.ok(supportTicketService.getTicketDetail(id, accountId, roleName));
    }

    @GetMapping("/tickets/{id}/messages")
    public ResponseEntity<ApiResponse<List<SupportMessageDto>>> getMessages(@PathVariable Integer id) {
        Integer accountId = SecurityUtils.getAuthenticatedAccountId();
        String roleName = Objects.requireNonNull(SecurityUtils.getAuthenticatedUserRole())
                .getName();
        return ResponseEntity.ok(supportMessageService.getMessages(id, accountId, roleName));
    }

    @PostMapping("/tickets/{id}/messages")
    public ResponseEntity<ApiResponse<SupportMessageDto>> sendMessage(@PathVariable Integer id, @Valid @RequestBody SendSupportMessageRequest request) {
        Integer accountId = SecurityUtils.getAuthenticatedAccountId();
        return ResponseEntity.ok(supportMessageService.sendMessage(id, accountId, request));
    }

}
