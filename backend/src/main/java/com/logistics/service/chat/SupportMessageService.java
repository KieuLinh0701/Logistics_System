package com.logistics.service.chat;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.logistics.repository.AccountRepository;
import com.logistics.dto.chat.SupportMessageDto;
import com.logistics.entity.SupportMessage;
import com.logistics.entity.SupportTicket;
import com.logistics.enums.SupportMessageSenderType;
import com.logistics.enums.SupportMessageType;
import com.logistics.repository.SupportMessageRepository;
import com.logistics.repository.SupportTicketRepository;
import com.logistics.request.chat.SendSupportMessageRequest;
import com.logistics.response.ApiResponse;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class SupportMessageService {

    private final SupportTicketRepository supportTicketRepository;
    private final SupportMessageRepository supportMessageRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final AccountRepository accountRepository;
    private final com.logistics.repository.OrderRepository orderRepository;
    private final com.logistics.repository.EmployeeRepository employeeRepository;
    private final SupportAssistantService supportAssistantService;
    private final SupportBotMessageService supportBotMessageService;

    @Transactional
    public ApiResponse<SupportMessageDto> sendMessage(Integer ticketId, Integer senderId, String roleName,
            SendSupportMessageRequest request) {
        try {
            SupportTicket ticket = supportTicketRepository.findById(ticketId)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy ticket hỗ trợ"));

            if (!canAccessTicket(ticket, senderId, roleName)) {
                return new ApiResponse<>(false, "Bạn không có quyền chat trong ticket này", null);
            }

            if (Boolean.TRUE.equals(request.getIsInternalNote()) && !isManagerOrAdmin(roleName)) {
                return new ApiResponse<>(false, "Chỉ Manager/Admin mới được gửi ghi chú nội bộ", null);
            }

            SupportMessage saved = saveMessage(ticketId, senderId, request.getMessage(), request.getMessageType(),
                    Boolean.TRUE.equals(request.getIsInternalNote()));

            ticket.setUpdatedAt(LocalDateTime.now());
            supportTicketRepository.save(ticket);

            SupportMessageDto dto = toMessageDto(saved);
            messagingTemplate.convertAndSend("/topic/support/" + ticketId, dto);

            if (shouldTriggerAssistant(roleName, saved)) {
                supportAssistantService.handleAfterUserMessage(ticket, saved);
            }

            return new ApiResponse<>(true, "Gửi tin nhắn thành công", dto);
        } catch (Exception e) {
            return new ApiResponse<>(false, "Gửi tin nhắn thất bại: " + e.getMessage(), null);
        }
    }

    @Transactional
    public ApiResponse<SupportMessageDto> sendMessage(Integer ticketId, Integer senderId, SendSupportMessageRequest request) {
        String resolvedRoleName = resolveRoleNameByAccountId(senderId);
        return sendMessage(ticketId, senderId, resolvedRoleName, request);
    }

    @Transactional(readOnly = true)
    public ApiResponse<List<SupportMessageDto>> getMessages(Integer ticketId, Integer accountId, String roleName) {
        try {
            SupportTicket ticket = supportTicketRepository.findById(ticketId)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy ticket hỗ trợ"));

            if (!canAccessTicket(ticket, accountId, roleName)) {
                return new ApiResponse<>(false, "Bạn không có quyền xem tin nhắn của ticket này", null);
            }

            List<SupportMessageDto> data = supportMessageRepository.findByTicketIdOrderByCreatedAtAsc(ticketId)
                    .stream()
                    .filter(msg -> isManagerOrAdmin(roleName)
                            || msg.getMessageType() != SupportMessageType.SYSTEM
                            || msg.getSenderType() == SupportMessageSenderType.BOT)
                    .filter(msg -> !Boolean.TRUE.equals(msg.getIsInternalNote()) || isManagerOrAdmin(roleName))
                    .map(this::toMessageDto)
                    .toList();

            return new ApiResponse<>(true, "Lấy danh sách tin nhắn thành công", data);
        } catch (Exception e) {
            return new ApiResponse<>(false, "Lấy danh sách tin nhắn thất bại: " + e.getMessage(), null);
        }
    }

    @Transactional
    public void createInitialMessage(Integer ticketId, Integer senderAccountId, String message) {
        saveMessage(ticketId, senderAccountId, message, SupportMessageType.TEXT, false);
    }

    @Transactional
    public void createSystemMessage(Integer ticketId, String message) {
        saveMessage(ticketId, 0, message, SupportMessageType.SYSTEM, false);
    }

    @Transactional
    public SupportMessage createBotMessage(Integer ticketId, String message) {
        return supportBotMessageService.createBotMessage(ticketId, message);
    }

    @Transactional
    public void sendSystemBotMessage(Integer ticketId, SendSupportMessageRequest request, SupportMessageSenderType senderType) {
        if (request == null) {
            return;
        }
        supportBotMessageService.sendSystemBotMessage(ticketId, request.getMessage(), senderType);
    }

    private SupportMessage saveMessage(Integer ticketId, Integer senderId, String message,
            SupportMessageType messageType, boolean isInternalNote) {
        SupportMessage entity = new SupportMessage();
        entity.setTicketId(ticketId);
        entity.setSenderAccountId(senderId);
        entity.setSenderType(resolveSenderType(senderId));
        entity.setMessage(message == null ? "" : message.trim());
        entity.setMessageType(messageType == null ? SupportMessageType.TEXT : messageType);
        entity.setIsInternalNote(isInternalNote);

        return supportMessageRepository.save(entity);
    }

    private boolean canAccessTicket(SupportTicket ticket, Integer accountId, String roleName) {
        if (isManagerOrAdmin(roleName)) {
            // Manager hoặc Admin: Admin xem được tất cả 
            if ("Admin".equalsIgnoreCase(roleName)) return true;

            // Manager: cho phép nếu ticket được gán cho họ
            if (Objects.equals(ticket.getAssignedToAccountId(), accountId)) {
                return true;
            }

            // Hoặc nếu ticket liên quan tới recipientaddress mà bưu cục (office) thuộc quyền quản lý của manager
            if (ticket.getRelatedType() != null && "ORDER".equalsIgnoreCase(ticket.getRelatedType()) && ticket.getRelatedId() != null) {
                try {
                    var ordOpt = orderRepository.findById(ticket.getRelatedId());
                    if (ordOpt.isPresent()) {
                        var o = ordOpt.get();
                        List<Integer> officeIds = employeeRepository.findAllByAccountId(accountId).stream()
                                .map(e -> e.getOffice() != null ? e.getOffice().getId() : null)
                                .filter(id -> id != null)
                                .toList();

                        Integer fromId = o.getFromOffice() != null ? o.getFromOffice().getId() : null;
                        Integer toId = o.getToOffice() != null ? o.getToOffice().getId() : null;

                        if ((fromId != null && officeIds.contains(fromId)) || (toId != null && officeIds.contains(toId))) {
                            return true;
                        }
                    }
                } catch (Exception ignore) {
                }
            }

            return false;
        }

        return Objects.equals(ticket.getCreatedByAccountId(), accountId);
    }

    private boolean isManagerOrAdmin(String roleName) {
        return "Manager".equalsIgnoreCase(roleName) || "Admin".equalsIgnoreCase(roleName);
    }

    private boolean shouldTriggerAssistant(String roleName, SupportMessage saved) {
        if (saved == null) {
            return false;
        }
        if (saved.getSenderType() == SupportMessageSenderType.BOT || saved.getSenderType() == SupportMessageSenderType.SYSTEM) {
            return false;
        }
        return !isManagerOrAdmin(roleName) && saved.getSenderType() == SupportMessageSenderType.USER;
    }

    private SupportMessageSenderType resolveSenderType(Integer senderId) {
        if (senderId == null) {
            return SupportMessageSenderType.SYSTEM;
        }
        if (senderId == 0) {
            return SupportMessageSenderType.SYSTEM;
        }
        return accountRepository.findById(senderId)
                .map(account -> {
                    var activeRoles = account.getAccountRoles() == null ? List.<String>of() : account.getAccountRoles().stream()
                            .filter(ar -> Boolean.TRUE.equals(ar.getIsActive()) && ar.getRole() != null)
                            .map(ar -> ar.getRole().getName())
                            .toList();
                    if (activeRoles.stream().anyMatch(r -> "Admin".equalsIgnoreCase(r))) {
                        return SupportMessageSenderType.ADMIN;
                    }
                    if (activeRoles.stream().anyMatch(r -> "Manager".equalsIgnoreCase(r))) {
                        return SupportMessageSenderType.MANAGER;
                    }
                    return SupportMessageSenderType.USER;
                })
                .orElse(SupportMessageSenderType.USER);
    }

    private String resolveRoleNameByAccountId(Integer accountId) {
        return accountRepository.findById(accountId)
                    .map(account -> {
                        var activeRoles = account.getAccountRoles().stream()
                                .filter(ar -> Boolean.TRUE.equals(ar.getIsActive()) && ar.getRole() != null)
                                .map(ar -> ar.getRole().getName())
                                .toList();

                        if (activeRoles.stream().anyMatch(r -> "Admin".equalsIgnoreCase(r))) {
                            return "Admin";
                        }
                        if (activeRoles.stream().anyMatch(r -> "Manager".equalsIgnoreCase(r))) {
                            return "Manager";
                        }
                        
                        return activeRoles.stream().findFirst().orElse("User");
                })
            .orElse("User");
    }

    private SupportMessageDto toMessageDto(SupportMessage entity) {
        String senderName = null;
        String senderImage = null;

        Integer senderId = entity.getSenderAccountId();
        if (senderId != null && senderId > 0) {
            var opt = accountRepository.findById(senderId);
            if (opt.isPresent()) {
                var acc = opt.get();
                if (acc.getUser() != null) {
                    senderName = acc.getUser().getFullName();
                    senderImage = acc.getUser().getImages();
                } else {
                    senderName = acc.getEmail();
                }
            }
        } else if (senderId != null && senderId == 0) {
            senderName = "System";
        }

        boolean isBotMessage = entity.getSenderType() == SupportMessageSenderType.BOT;
        String senderLabel;
        if (isBotMessage) {
            senderLabel = "Trợ lý logistics";
        } else if (entity.getSenderType() == SupportMessageSenderType.SYSTEM) {
            senderLabel = "System";
        } else if (entity.getSenderType() == SupportMessageSenderType.MANAGER) {
            senderLabel = "Manager";
        } else if (entity.getSenderType() == SupportMessageSenderType.ADMIN) {
            senderLabel = "Admin";
        } else {
            senderLabel = senderName;
        }

        return new SupportMessageDto(
                entity.getId(),
                entity.getTicketId(),
                entity.getSenderAccountId(),
                entity.getSenderType(),
                senderLabel,
                isBotMessage,
                entity.getMessage(),
                entity.getMessageType(),
                entity.getIsInternalNote(),
                entity.getCreatedAt(),
                senderName,
                senderImage);
    }
}
