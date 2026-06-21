package com.logistics.service.chat;

import com.logistics.dto.chat.SupportMessageDto;
import com.logistics.entity.SupportMessage;
import com.logistics.entity.SupportTicket;
import com.logistics.enums.SupportMessageSenderType;
import com.logistics.enums.SupportMessageType;
import com.logistics.enums.SupportTicketStatus;
import com.logistics.exception.AppException;
import com.logistics.exception.enums.SupportMessageErrorCode;
import com.logistics.exception.enums.SupportTicketErrorCode;
import com.logistics.repository.AccountRepository;
import com.logistics.repository.SupportMessageRepository;
import com.logistics.repository.SupportTicketRepository;
import com.logistics.request.chat.SendSupportMessageRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
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
    public SupportMessageDto sendMessage(Integer ticketId, Integer senderId,
            SendSupportMessageRequest request) {
        SupportTicket ticket = supportTicketRepository.findById(ticketId)
                .orElseThrow(() -> new AppException(SupportTicketErrorCode.SUPPORT_TICKET_NOT_FOUND));

        String roleName = resolveRoleNameByAccountId(senderId);

        if (!canAccessTicket(ticket, senderId, roleName)) {
            throw new AppException(SupportMessageErrorCode.SUPPORT_SEND_MESSAGE_DENIED);
        }

        if (ticket.getStatus() == SupportTicketStatus.CLOSED) {
            throw new AppException(SupportTicketErrorCode.SUPPORT_TICKET_CLOSED);
        }

        boolean wasResolved = ticket.getStatus() == SupportTicketStatus.RESOLVED;
        boolean isUserSender = !isManagerOrAdmin(roleName);

        if (wasResolved && isUserSender) {
            // Mở lại ticket
            String userName = accountRepository.findById(senderId)
                    .map(acc -> acc.getUser() != null ? acc.getUser().getFullName() : acc.getEmail())
                    .orElse("Khách hàng");

            ticket.setStatus(SupportTicketStatus.OPEN);
            ticket.setUpdatedAt(LocalDateTime.now());
            supportTicketRepository.save(ticket);

            // Tạo tin nhắn hệ thống khi mở lại
            createSystemMessage(ticketId,
                    "🔓 Khách hàng đã phản hồi, yêu cầu được mở lại.");
        }

        // Kiểm tra ghi chú nội bộ
        if (Boolean.TRUE.equals(request.getIsInternalNote()) && !isManagerOrAdmin(roleName)) {
            throw new AppException(SupportMessageErrorCode.SUPPORT_INTERNAL_NOTE_DENIED);
        }

        // Lưu tin nhắn
        SupportMessage saved = saveMessage(ticketId, senderId, request.getMessage(), request.getMessageType(),
                Boolean.TRUE.equals(request.getIsInternalNote()), false);

        // Cập nhật thời gian ticket
        ticket.setUpdatedAt(LocalDateTime.now());
        supportTicketRepository.save(ticket);

        // Gửi qua WebSocket
        SupportMessageDto dto = toMessageDto(saved);
        messagingTemplate.convertAndSend("/topic/support/" + ticketId, dto);

        // Kích hoạt bot cho tin nhắn từ user
        if (shouldTriggerAssistant(roleName, saved)) {
            supportAssistantService.handleAfterUserMessage(ticket, saved);
        }

        return dto;
    }

    @Transactional
    public SupportMessageDto sendMessage(Integer ticketId, Integer senderId, String roleName,
            SendSupportMessageRequest request) {
        SupportTicket ticket = supportTicketRepository.findById(ticketId)
                .orElseThrow(() -> new AppException(SupportTicketErrorCode.SUPPORT_TICKET_NOT_FOUND));

        if (!canAccessTicket(ticket, senderId, roleName)) {
            throw new AppException(SupportMessageErrorCode.SUPPORT_SEND_MESSAGE_DENIED);
        }

        // CHẶN: Không thể gửi tin nhắn đến ticket đã đóng
        if (ticket.getStatus() == SupportTicketStatus.CLOSED) {
            throw new AppException(SupportTicketErrorCode.SUPPORT_TICKET_CLOSED);
        }

        // XỬ LÝ: Ticket RESOLVED - phản hồi từ user sẽ mở lại
        boolean wasResolved = ticket.getStatus() == SupportTicketStatus.RESOLVED;
        boolean isUserSender = !isManagerOrAdmin(roleName);

        if (wasResolved && isUserSender) {
            ticket.setStatus(SupportTicketStatus.OPEN);
            ticket.setUpdatedAt(LocalDateTime.now());
            supportTicketRepository.save(ticket);

            createSystemMessage(ticketId,
                    "🔓 Khách hàng đã phản hồi, yêu cầu được mở lại.");
        }

        if (Boolean.TRUE.equals(request.getIsInternalNote()) && !isManagerOrAdmin(roleName)) {
            throw new AppException(SupportMessageErrorCode.SUPPORT_INTERNAL_NOTE_DENIED);
        }

        SupportMessage saved = saveMessage(ticketId, senderId, request.getMessage(), request.getMessageType(),
                Boolean.TRUE.equals(request.getIsInternalNote()), false);

        ticket.setUpdatedAt(LocalDateTime.now());
        supportTicketRepository.save(ticket);

        SupportMessageDto dto = toMessageDto(saved);
        messagingTemplate.convertAndSend("/topic/support/" + ticketId, dto);

        if (shouldTriggerAssistant(roleName, saved)) {
            supportAssistantService.handleAfterUserMessage(ticket, saved);
        }

        return dto;
    }

    public List<SupportMessageDto> getMessages(Integer ticketId, Integer accountId, String roleName) {
        SupportTicket ticket = supportTicketRepository.findById(ticketId)
                .orElseThrow(() -> new AppException(SupportTicketErrorCode.SUPPORT_TICKET_NOT_FOUND));

        if (!canAccessTicket(ticket, accountId, roleName)) {
            throw new AppException(SupportMessageErrorCode.SUPPORT_VIEW_MESSAGES_DENIED);
        }

        return supportMessageRepository.findByTicketIdOrderByCreatedAtAsc(ticketId)
                .stream()
                .filter(msg -> isManagerOrAdmin(roleName)
                        || msg.getMessageType() != SupportMessageType.SYSTEM
                        || msg.getSenderType() == SupportMessageSenderType.BOT)
                .filter(msg -> !Boolean.TRUE.equals(msg.getIsInternalNote()) || isManagerOrAdmin(roleName))
                .map(this::toMessageDto)
                .toList();
    }

    @Transactional
    public SupportMessage createInitialMessage(Integer ticketId, Integer senderAccountId, String message) {
        return saveMessage(ticketId, senderAccountId, message, SupportMessageType.TEXT, false, false);
    }

    @Transactional
    public void createSystemMessage(Integer ticketId, String message) {
        SupportMessage saved = saveMessage(ticketId, 0, message, SupportMessageType.SYSTEM, false, true);
        SupportMessageDto dto = toMessageDto(saved);
        messagingTemplate.convertAndSend("/topic/support/" + ticketId, dto);
    }

    @Transactional
    public SupportMessage createBotMessage(Integer ticketId, String message) {
        SupportMessage saved = supportBotMessageService.createBotMessage(ticketId, message);
        SupportMessageDto dto = toMessageDto(saved);
        messagingTemplate.convertAndSend("/topic/support/" + ticketId, dto);
        return saved;
    }

    @Transactional
    public void sendSystemBotMessage(Integer ticketId, SendSupportMessageRequest request, SupportMessageSenderType senderType) {
        if (request == null) {
            return;
        }
        supportBotMessageService.sendSystemBotMessage(ticketId, request.getMessage(), senderType);
    }

    
    // Đánh dấu tin nhắn đã đọc cho một ticket.
    @Transactional
    public void markMessagesAsRead(Integer ticketId, Integer accountId) {
        try {
            SupportTicket ticket = supportTicketRepository.findById(ticketId)
                    .orElse(null);

            if (ticket == null) {
                return;
            }

            String roleName = resolveRoleNameByAccountId(accountId);
            if (!canAccessTicket(ticket, accountId, roleName)) {
                return;
            }

            supportMessageRepository.markMessagesAsRead(ticketId, accountId);
        } catch (Exception e) {
            log.warn("Failed to mark messages as read for ticket {}: {}", ticketId, e.getMessage());
        }
    }

    // Đếm số tin nhắn chưa đọc cho một ticket.
    
    public int countUnreadByTicketId(Integer ticketId) {
        try {
            return (int) supportMessageRepository.countByTicketIdAndSenderAccountIdNotAndIsReadFalse(ticketId, 0);
        } catch (Exception e) {
            return 0;
        }
    }

    //Đếm tổng số tin nhắn cho một ticket.
    public int countMessagesByTicketId(Integer ticketId) {
        try {
            return supportMessageRepository.countByTicketId(ticketId);
        } catch (Exception e) {
            return 0;
        }
    }

    // Lấy thông tin tin nhắn mới nhất của một ticket
    public Optional<Map<String, Object>> getLatestMessageInfo(Integer ticketId) {
        try {
            return supportMessageRepository.findTopByTicketIdOrderByCreatedAtDesc(ticketId)
                    .map(msg -> {
                        Map<String, Object> info = new HashMap<>();
                        info.put("message", msg.getMessage());
                        info.put("createdAt", msg.getCreatedAt());
                        info.put("senderId", msg.getSenderAccountId());
                        return info;
                    });
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    private SupportMessage saveMessage(Integer ticketId, Integer senderId, String message,
            SupportMessageType messageType, boolean isInternalNote, boolean isRead) {
        SupportMessage entity = new SupportMessage();
        entity.setTicketId(ticketId);
        entity.setSenderAccountId(senderId);
        entity.setSenderType(resolveSenderType(senderId));
        entity.setMessage(message == null ? "" : message.trim());
        entity.setMessageType(messageType == null ? SupportMessageType.TEXT : messageType);
        entity.setIsInternalNote(isInternalNote);
        entity.setIsRead(isRead);

        return supportMessageRepository.save(entity);
    }

    private boolean canAccessTicket(SupportTicket ticket, Integer accountId, String roleName) {
        if (isManagerOrAdmin(roleName)) {
            if ("Admin".equalsIgnoreCase(roleName)) return true;

            if (Objects.equals(ticket.getAssignedToAccountId(), accountId)) {
                return true;
            }

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
                } catch (Exception e) {
                    log.warn("Failed to check office access for ticket {}: {}", ticket.getId(), e.getMessage());
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
        if (senderId == null || senderId == 0) {
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
        if (accountId == null) {
            return "User";
        }
        return accountRepository.findById(accountId)
                    .map(account -> {
                        var activeRoles = account.getAccountRoles() == null ? List.<String>of() : account.getAccountRoles().stream()
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
                senderImage,
                entity.getIsRead());
    }
}
