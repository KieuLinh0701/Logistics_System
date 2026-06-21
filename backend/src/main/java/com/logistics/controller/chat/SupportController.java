package com.logistics.controller.chat;

import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.logistics.audit.Audit;
import com.logistics.constants.AuditLogDescriptionConstant;
import com.logistics.enums.AuditLogAction;
import com.logistics.enums.EntityType;
import com.logistics.exception.AppException;
import com.logistics.response.ApiResponse;
import com.logistics.request.chat.AssignTicketRequest;
import com.logistics.request.chat.CloseTicketRequest;
import com.logistics.request.chat.CreateSupportTicketRequest;
import com.logistics.request.chat.SendSupportMessageRequest;
import com.logistics.dto.chat.SupportMessageDto;
import com.logistics.dto.chat.SupportTicketDetailDto;
import com.logistics.dto.chat.SupportTicketDto;
import com.logistics.exception.enums.SupportTicketErrorCode;
import com.logistics.service.chat.SupportMessageService;
import com.logistics.service.chat.SupportTicketService;
import com.logistics.utils.SecurityUtils;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@Validated
@RequiredArgsConstructor
@RequestMapping("/api/support")
@Tag(name = "Support", description = "Hỗ trợ khách hàng và trò chuyện trực tuyến")
public class SupportController {

    private final SupportTicketService supportTicketService;
    private final SupportMessageService supportMessageService;

    @PostMapping("/tickets")
    @Audit(
            entity = EntityType.SUPPORT_TICKET,
            action = AuditLogAction.CREATE,
            description = AuditLogDescriptionConstant.SUPPORT_TICKET_CREATE
    )
    public ResponseEntity<ApiResponse<SupportTicketDto>> createTicket(@Valid @RequestBody CreateSupportTicketRequest request) {
        if (!SecurityUtils.hasRole("user")) {
            throw new AppException(SupportTicketErrorCode.SUPPORT_CREATE_TICKET_DENIED);
        }
        Integer accountId = SecurityUtils.getAuthenticatedAccountId();
        SupportTicketDto dto = supportTicketService.createTicket(accountId, request);
        return ResponseEntity.ok(ApiResponse.success("Tạo ticket thành công", dto));
    }

    @GetMapping("/tickets/my")
    public ResponseEntity<ApiResponse<List<SupportTicketDto>>> getMyTickets() {
        Integer accountId = SecurityUtils.getAuthenticatedAccountId();
        String roleName = SecurityUtils.getAuthenticatedUserRole() != null
                ? SecurityUtils.getAuthenticatedUserRole().getName() : null;
        List<SupportTicketDto> dto = supportTicketService.getMyTickets(accountId, roleName);
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách ticket thành công", dto));
    }

    @GetMapping("/tickets/{id}")
    public ResponseEntity<ApiResponse<SupportTicketDetailDto>> getTicketDetail(@PathVariable Integer id) {
        Integer accountId = SecurityUtils.getAuthenticatedAccountId();
        String roleName = SecurityUtils.getAuthenticatedUserRole() != null
                ? SecurityUtils.getAuthenticatedUserRole().getName() : null;
        SupportTicketDetailDto dto = supportTicketService.getTicketDetail(id, accountId, roleName);
        return ResponseEntity.ok(ApiResponse.success("Lấy chi tiết ticket thành công", dto));
    }

    @GetMapping("/tickets/{id}/messages")
    public ResponseEntity<ApiResponse<List<SupportMessageDto>>> getMessages(@PathVariable Integer id) {
        Integer accountId = SecurityUtils.getAuthenticatedAccountId();
        String roleName = SecurityUtils.getAuthenticatedUserRole() != null
                ? SecurityUtils.getAuthenticatedUserRole().getName() : null;
        List<SupportMessageDto> dto = supportMessageService.getMessages(id, accountId, roleName);
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách tin nhắn thành công", dto));
    }

    @PostMapping("/tickets/{id}/messages")
    @Audit(
            entity = EntityType.SUPPORT_MESSAGE,
            action = AuditLogAction.CREATE,
            description = AuditLogDescriptionConstant.SUPPORT_MESSAGE_CREATE,
            params = {"id"}
    )
    public ResponseEntity<ApiResponse<SupportMessageDto>> sendMessage(@PathVariable Integer id, @Valid @RequestBody SendSupportMessageRequest request) {
        Integer accountId = SecurityUtils.getAuthenticatedAccountId();
        SupportMessageDto dto = supportMessageService.sendMessage(id, accountId, request);
        return ResponseEntity.ok(ApiResponse.success("Gửi tin nhắn thành công", dto));
    }

    @PostMapping("/tickets/{id}/assign")
    @Audit(
            entity = EntityType.SUPPORT_TICKET,
            action = AuditLogAction.UPDATE_STATUS,
            description = AuditLogDescriptionConstant.SUPPORT_TICKET_ASSIGN,
            params = {"id"}
    )
    public ResponseEntity<ApiResponse<SupportTicketDto>> assignTicket(
            @PathVariable Integer id,
            @Valid @RequestBody AssignTicketRequest request) {
        if (!SecurityUtils.hasRole("admin")) {
            throw new AppException(SupportTicketErrorCode.SUPPORT_ASSIGN_DENIED);
        }
        Integer accountId = SecurityUtils.getAuthenticatedAccountId();
        SupportTicketDto dto = supportTicketService.assignTicket(id, accountId, request);
        return ResponseEntity.ok(ApiResponse.success("Phân công ticket thành công", dto));
    }

    @PostMapping("/tickets/{id}/close")
    @Audit(
            entity = EntityType.SUPPORT_TICKET,
            action = AuditLogAction.UPDATE_STATUS,
            description = AuditLogDescriptionConstant.SUPPORT_TICKET_CLOSE,
            params = {"id"}
    )
    public ResponseEntity<ApiResponse<SupportTicketDto>> closeTicket(
            @PathVariable Integer id,
            @RequestBody(required = false) CloseTicketRequest request) {
        Integer accountId = SecurityUtils.getAuthenticatedAccountId();
        String roleName = SecurityUtils.getAuthenticatedUserRole() != null
                ? SecurityUtils.getAuthenticatedUserRole().getName() : null;
        SupportTicketDto dto = supportTicketService.closeTicket(id, accountId, roleName, request);
        return ResponseEntity.ok(ApiResponse.success("Đã đánh dấu giải quyết ticket", dto));
    }

    @PostMapping("/tickets/{id}/force-close")
    @Audit(
            entity = EntityType.SUPPORT_TICKET,
            action = AuditLogAction.UPDATE_STATUS,
            description = AuditLogDescriptionConstant.SUPPORT_TICKET_FORCE_CLOSE,
            params = {"id"}
    )
    public ResponseEntity<ApiResponse<SupportTicketDto>> forceCloseTicket(
            @PathVariable Integer id,
            @RequestBody(required = false) CloseTicketRequest request) {
        if (!SecurityUtils.hasRole("admin")) {
            throw new AppException(SupportTicketErrorCode.SUPPORT_FORCE_CLOSE_DENIED);
        }
        Integer accountId = SecurityUtils.getAuthenticatedAccountId();
        SupportTicketDto dto = supportTicketService.forceCloseTicket(id, accountId, request);
        return ResponseEntity.ok(ApiResponse.success("Đã đóng ticket thành công", dto));
    }

    @PostMapping("/tickets/{id}/reopen")
    @Audit(
            entity = EntityType.SUPPORT_TICKET,
            action = AuditLogAction.UPDATE_STATUS,
            description = AuditLogDescriptionConstant.SUPPORT_TICKET_REOPEN,
            params = {"id"}
    )
    public ResponseEntity<ApiResponse<SupportTicketDto>> reopenTicket(@PathVariable Integer id) {
        Integer accountId = SecurityUtils.getAuthenticatedAccountId();
        String roleName = SecurityUtils.getAuthenticatedUserRole() != null
                ? SecurityUtils.getAuthenticatedUserRole().getName() : null;
        SupportTicketDto dto = supportTicketService.reopenTicket(id, accountId, roleName);
        return ResponseEntity.ok(ApiResponse.success("Đã mở lại ticket", dto));
    }

    @PostMapping("/messages/mark-read")
    @Audit(
            entity = EntityType.SUPPORT_MESSAGE,
            action = AuditLogAction.UPDATE,
            description = AuditLogDescriptionConstant.SUPPORT_MESSAGE_MARK_READ
    )
    public ResponseEntity<ApiResponse<Void>> markMessagesRead(@RequestBody Map<String, Integer> body) {
        Integer accountId = SecurityUtils.getAuthenticatedAccountId();
        Integer ticketId = body != null ? body.get("ticketId") : null;

        if (ticketId == null) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.failure("ticketId là bắt buộc"));
        }

        supportMessageService.markMessagesAsRead(ticketId, accountId);
        return ResponseEntity.ok(ApiResponse.success("Messages marked as read", null));
    }
}
