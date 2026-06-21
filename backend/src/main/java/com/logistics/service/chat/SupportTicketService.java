package com.logistics.service.chat;

import com.logistics.dto.chat.SupportTicketDetailDto;
import com.logistics.dto.chat.SupportTicketDto;
import com.logistics.entity.SupportMessage;
import com.logistics.entity.SupportTicket;
import com.logistics.enums.SupportTicketStatus;
import com.logistics.exception.AppException;
import com.logistics.exception.enums.SupportTicketErrorCode;
import com.logistics.repository.AccountRepository;
import com.logistics.repository.OfficeRepository;
import com.logistics.repository.SupportTicketRepository;
import com.logistics.request.chat.AssignTicketRequest;
import com.logistics.request.chat.CloseTicketRequest;
import com.logistics.request.chat.CreateSupportTicketRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
@Slf4j
public class SupportTicketService {
    private final SupportTicketRepository supportTicketRepository;
    private final AccountRepository accountRepository;
    private final OfficeRepository officeRepository;
    private final SupportMessageService supportMessageService;
    private final SupportAssistantService supportAssistantService;

    // Tạo ticket hỗ trợ mới
    @Transactional
    public SupportTicketDto createTicket(Integer accountId, CreateSupportTicketRequest request) {
        SupportTicket ticket = new SupportTicket();
        ticket.setCreatedByAccountId(accountId);
        ticket.setCode("SUP-TMP");
        ticket.setStatus(SupportTicketStatus.OPEN);
        ticket.setAssignedToAccountId(null);

        String subject = request.getSubject();
        if (subject == null || subject.isBlank()) {
            String initialMsg = request.getInitialMessage();
            if (initialMsg != null && !initialMsg.isBlank()) {
                subject = initialMsg.length() > 80 ? initialMsg.substring(0, 80) : initialMsg;
            } else {
                subject = "Yêu cầu hỗ trợ";
            }
        }
        ticket.setSubject(subject);

        String priority = request.getPriority();
        if (priority == null || priority.isBlank()) {
            priority = "NORMAL";
        }
        ticket.setPriority(priority);

        ticket = supportTicketRepository.save(ticket);

        String finalCode = String.format("SUP-%06d", ticket.getId());
        ticket.setCode(finalCode);
        supportTicketRepository.save(ticket);

        supportMessageService.createSystemMessage(ticket.getId(),
                "Yêu cầu hỗ trợ đã được tạo và đang chờ CSKH tiếp nhận.");

        if (request.getInitialMessage() != null && !request.getInitialMessage().isBlank()) {
            SupportMessage initialMessage = supportMessageService.createInitialMessage(
                    ticket.getId(), accountId, request.getInitialMessage());
            if (initialMessage != null) {
                supportAssistantService.handleAfterUserMessage(ticket, initialMessage);
            }
        }

        return toTicketDto(ticket);
    }

    public List<SupportTicketDto> getMyTickets(Integer accountId, String roleName) {
        List<SupportTicket> tickets;
        if (roleName != null && roleName.equalsIgnoreCase("Admin")) {
            tickets = supportTicketRepository.findAllByOrderByUpdatedAtDesc();
        } else if (roleName != null && roleName.equalsIgnoreCase("Manager")) {
            tickets = supportTicketRepository.findByAssignedToAccountIdOrderByUpdatedAtDesc(accountId);
        } else {
            tickets = supportTicketRepository.findByCreatedByAccountIdOrderByCreatedAtDesc(accountId);
        }

        return tickets.stream().map(this::toTicketDto).toList();
    }

    public SupportTicketDetailDto getTicketDetail(Integer id, Integer accountId, String roleName) {
        SupportTicket ticket = supportTicketRepository.findById(id)
                .orElseThrow(() -> new AppException(SupportTicketErrorCode.SUPPORT_TICKET_NOT_FOUND));

        boolean allowed = canAccessTicket(ticket, accountId, roleName);
        if (!allowed) {
            throw new AppException(SupportTicketErrorCode.SUPPORT_TICKET_ACCESS_DENIED);
        }

        SupportTicketDetailDto dto = new SupportTicketDetailDto();
        dto.setTicket(toTicketDto(ticket));
        return dto;
    }

    // Phân công ticket cho quản lý xử lý.
    @Transactional
    public SupportTicketDto assignTicket(Integer ticketId, Integer accountId, AssignTicketRequest request) {
        SupportTicket ticket = supportTicketRepository.findById(ticketId)
                .orElseThrow(() -> new AppException(SupportTicketErrorCode.SUPPORT_TICKET_NOT_FOUND));

        if (request.getAssigneeAccountId() != null) {
            ticket.setAssignedToAccountId(request.getAssigneeAccountId());
        }
        if (request.getOfficeId() != null) {
            ticket.setOfficeId(request.getOfficeId());
        }
        ticket.setStatus(SupportTicketStatus.ASSIGNED);
        ticket.setUpdatedAt(LocalDateTime.now());
        supportTicketRepository.save(ticket);

        String assigneeName = null;
        if (request.getAssigneeAccountId() != null) {
            assigneeName = accountRepository.findById(request.getAssigneeAccountId())
                    .map(acc -> acc.getUser() != null ? acc.getUser().getFullName() : acc.getEmail())
                    .orElse("Manager #" + request.getAssigneeAccountId());
        }

        String systemMsg;
        if (request.getNote() != null && !request.getNote().isBlank()) {
            systemMsg = String.format("Admin đã chuyển yêu cầu cho %s xử lý. Ghi chú: %s",
                    assigneeName != null ? assigneeName : "Manager",
                    request.getNote());
        } else {
            systemMsg = String.format("Admin đã chuyển yêu cầu cho %s xử lý.",
                    assigneeName != null ? assigneeName : "Manager");
        }
        supportMessageService.createSystemMessage(ticket.getId(), systemMsg);

        return toTicketDto(ticket);
    }

    // Đánh dấu ticket là RESOLVED (Manager/Admin có thể sử dụng).
    @Transactional
    public SupportTicketDto closeTicket(Integer ticketId, Integer accountId, String roleName,
            CloseTicketRequest request) {
        SupportTicket ticket = supportTicketRepository.findById(ticketId)
                .orElseThrow(() -> new AppException(SupportTicketErrorCode.SUPPORT_TICKET_NOT_FOUND));

        if (ticket.getStatus() == SupportTicketStatus.CLOSED) {
            throw new AppException(SupportTicketErrorCode.SUPPORT_TICKET_ALREADY_CLOSED);
        }

        boolean canClose = false;
        if (roleName != null && roleName.equalsIgnoreCase("Admin")) {
            canClose = true;
        } else if (roleName != null && roleName.equalsIgnoreCase("Manager")
                && ticket.getAssignedToAccountId() != null
                && ticket.getAssignedToAccountId().equals(accountId)) {
            canClose = true;
        }

        if (!canClose) {
            throw new AppException(SupportTicketErrorCode.SUPPORT_CLOSE_DENIED);
        }

        // Lấy tên người thực hiện
        String actorName = accountRepository.findById(accountId)
                .map(acc -> acc.getUser() != null ? acc.getUser().getFullName() : acc.getEmail())
                .orElse("User #" + accountId);

        // Cập nhật trạng thái ticket thành RESOLVED (không phải CLOSED)
        ticket.setStatus(SupportTicketStatus.RESOLVED);
        ticket.setUpdatedAt(LocalDateTime.now());
        supportTicketRepository.save(ticket);

        // Tạo tin nhắn hệ thống
        String noteText = (request != null && request.getNote() != null && !request.getNote().isBlank())
                ? ". Ghi chú: " + request.getNote()
                : "";
        String systemMsg = String.format("✅ %s đã đánh dấu yêu cầu là đã giải quyết.%s",
                actorName, noteText);
        supportMessageService.createSystemMessage(ticket.getId(), systemMsg);

        return toTicketDto(ticket);
    }

    // Đóng ticket vĩnh viễn - đóng ticket hoàn toàn.
    @Transactional
    public SupportTicketDto forceCloseTicket(Integer ticketId, Integer accountId, CloseTicketRequest request) {
        SupportTicket ticket = supportTicketRepository.findById(ticketId)
                .orElseThrow(() -> new AppException(SupportTicketErrorCode.SUPPORT_TICKET_NOT_FOUND));

        if (ticket.getStatus() == SupportTicketStatus.CLOSED) {
            throw new AppException(SupportTicketErrorCode.SUPPORT_TICKET_ALREADY_CLOSED);
        }

        String actorName = accountRepository.findById(accountId)
                .map(acc -> acc.getUser() != null ? acc.getUser().getFullName() : acc.getEmail())
                .orElse("Admin #" + accountId);

        // Cập nhật trạng thái ticket thành CLOSED
        ticket.setStatus(SupportTicketStatus.CLOSED);
        ticket.setClosedAt(LocalDateTime.now());
        ticket.setClosedByAccountId(accountId);
        ticket.setClosedByName(actorName);
        ticket.setUpdatedAt(LocalDateTime.now());
        supportTicketRepository.save(ticket);

        // Tạo tin nhắn hệ thống
        String noteText = (request != null && request.getNote() != null && !request.getNote().isBlank())
                ? ". Ghi chú: " + request.getNote()
                : "";
        String systemMsg = String.format("🔒 %s đã đóng yêu cầu hỗ trợ.%s",
                actorName, noteText);
        supportMessageService.createSystemMessage(ticket.getId(), systemMsg);

        return toTicketDto(ticket);
    }

    // Mở lại ticket - từ CLOSED hoặc RESOLVED về OPEN.
    @Transactional
    public SupportTicketDto reopenTicket(Integer ticketId, Integer accountId, String roleName) {
        SupportTicket ticket = supportTicketRepository.findById(ticketId)
                .orElseThrow(() -> new AppException(SupportTicketErrorCode.SUPPORT_TICKET_NOT_FOUND));

        // Có thể mở lại từ CLOSED hoặc RESOLVED
        if (ticket.getStatus() != SupportTicketStatus.CLOSED
                && ticket.getStatus() != SupportTicketStatus.RESOLVED) {
            throw new AppException(SupportTicketErrorCode.SUPPORT_REOPEN_INVALID_STATUS);
        }

        boolean canReopen = false;
        if (roleName != null && roleName.equalsIgnoreCase("Admin")) {
            canReopen = true;
        } else if (Objects.equals(ticket.getCreatedByAccountId(), accountId)) {
            canReopen = true;
        } else if (roleName != null && roleName.equalsIgnoreCase("Manager")
                && ticket.getAssignedToAccountId() != null
                && ticket.getAssignedToAccountId().equals(accountId)) {
            canReopen = true;
        }

        if (!canReopen) {
            throw new AppException(SupportTicketErrorCode.SUPPORT_REOPEN_DENIED);
        }

        String actorName = accountRepository.findById(accountId)
                .map(acc -> acc.getUser() != null ? acc.getUser().getFullName() : acc.getEmail())
                .orElse("User #" + accountId);

        // Đặt lại trạng thái thành OPEN
        ticket.setStatus(SupportTicketStatus.OPEN);
        ticket.setClosedAt(null);
        ticket.setClosedByAccountId(null);
        ticket.setClosedByName(null);
        ticket.setUpdatedAt(LocalDateTime.now());
        supportTicketRepository.save(ticket);

        String systemMsg = String.format("🔓 %s đã mở lại yêu cầu hỗ trợ.", actorName);
        supportMessageService.createSystemMessage(ticket.getId(), systemMsg);

        return toTicketDto(ticket);
    }

    private SupportTicketDto toTicketDto(SupportTicket t) {
        SupportTicketDto dto = new SupportTicketDto();
        dto.setId(t.getId());
        dto.setCode(t.getCode());
        dto.setCreatedByAccountId(t.getCreatedByAccountId());
        dto.setAssignedToAccountId(t.getAssignedToAccountId());
        dto.setCreatedAt(t.getCreatedAt());
        dto.setUpdatedAt(t.getUpdatedAt());

        dto.setStatus(t.getStatus());
        dto.setSubject(t.getSubject());
        dto.setPriority(t.getPriority());
        dto.setOfficeId(t.getOfficeId());
        dto.setClosedAt(t.getClosedAt());
        dto.setClosedByName(t.getClosedByName());
        dto.setIsAssigned(t.getAssignedToAccountId() != null);

        try {
            supportMessageService.getLatestMessageInfo(t.getId()).ifPresent(info -> {
                dto.setLatestMessage((String) info.get("message"));
                dto.setLatestMessageAt((LocalDateTime) info.get("createdAt"));
                dto.setLatestMessageSenderAccountId((Integer) info.get("senderId"));
            });

            dto.setTotalMessages(supportMessageService.countMessagesByTicketId(t.getId()));
            dto.setUnreadCount(supportMessageService.countUnreadByTicketId(t.getId()));
        } catch (Exception e) {
            log.warn("Failed to load message info for ticket {}: {}", t.getId(), e.getMessage());
        }

        try {
            if (t.getCreatedByAccountId() != null) {
                accountRepository.findById(t.getCreatedByAccountId()).ifPresent(acc -> {
                    if (acc.getUser() != null) dto.setCreatedByName(acc.getUser().getFullName());
                    dto.setCreatedByImage(acc.getUser() != null ? acc.getUser().getImages() : null);
                });
            }
            if (t.getAssignedToAccountId() != null) {
                accountRepository.findById(t.getAssignedToAccountId()).ifPresent(acc ->
                        dto.setAssignedToName(acc.getUser() != null ? acc.getUser().getFullName() : acc.getEmail()));
            }
            if (t.getOfficeId() != null) {
                officeRepository.findById(t.getOfficeId()).ifPresent(office ->
                        dto.setOfficeName(office.getName()));
            }
        } catch (Exception e) {
            log.warn("Failed to load related entity names for ticket {}: {}", t.getId(), e.getMessage());
        }

        return dto;
    }

    private boolean canAccessTicket(SupportTicket ticket, Integer accountId, String roleName) {
        if (roleName != null && roleName.equalsIgnoreCase("Admin")) {
            return true;
        }
        if (roleName != null && roleName.equalsIgnoreCase("Manager")
                && ticket.getAssignedToAccountId() != null
                && ticket.getAssignedToAccountId().equals(accountId)) {
            return true;
        }
        return Objects.equals(ticket.getCreatedByAccountId(), accountId);
    }
}
