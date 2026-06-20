package com.logistics.service.chat;

import java.time.LocalDateTime;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.logistics.dto.chat.SupportMessageDto;
import com.logistics.entity.SupportMessage;
import com.logistics.entity.SupportTicket;
import com.logistics.enums.SupportMessageSenderType;
import com.logistics.enums.SupportMessageType;
import com.logistics.repository.AccountRepository;
import com.logistics.repository.SupportMessageRepository;
import com.logistics.repository.SupportTicketRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class SupportBotMessageService {

    private final SupportMessageRepository supportMessageRepository;
    private final SupportTicketRepository supportTicketRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final AccountRepository accountRepository;

    @Transactional
    public SupportMessage createBotMessage(Integer ticketId, String message) {
        SupportMessage entity = new SupportMessage();
        entity.setTicketId(ticketId);
        entity.setSenderAccountId(0);
        entity.setSenderType(SupportMessageSenderType.BOT);
        entity.setMessage(message == null ? "" : message.trim());
        entity.setMessageType(SupportMessageType.SYSTEM);
        entity.setIsInternalNote(false);
        entity.setIsRead(false);

        SupportMessage saved = supportMessageRepository.save(entity);
        updateTicketUpdatedAt(ticketId);
        messagingTemplate.convertAndSend("/topic/support/" + ticketId, toMessageDto(saved));
        return saved;
    }

    @Transactional
    public void sendSystemBotMessage(Integer ticketId, String message, SupportMessageSenderType senderType) {
        SupportMessage entity = new SupportMessage();
        entity.setTicketId(ticketId);
        entity.setSenderAccountId(0);
        entity.setSenderType(senderType == null ? SupportMessageSenderType.SYSTEM : senderType);
        entity.setMessage(message == null ? "" : message.trim());
        entity.setMessageType(SupportMessageType.SYSTEM);
        entity.setIsInternalNote(false);
        entity.setIsRead(false);

        SupportMessage saved = supportMessageRepository.save(entity);
        updateTicketUpdatedAt(ticketId);
        messagingTemplate.convertAndSend("/topic/support/" + ticketId, toMessageDto(saved));
    }

    private void updateTicketUpdatedAt(Integer ticketId) {
        supportTicketRepository.findById(ticketId).ifPresent(ticket -> {
            ticket.setUpdatedAt(LocalDateTime.now());
            supportTicketRepository.save(ticket);
        });
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
            senderName = entity.getSenderType() == SupportMessageSenderType.BOT ? "Trợ lý logistics" : "System";
        }

        boolean isBotMessage = entity.getSenderType() == SupportMessageSenderType.BOT;
        String senderLabel = isBotMessage ? "Trợ lý logistics" : (entity.getSenderType() == SupportMessageSenderType.SYSTEM ? "System" : senderName);

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
