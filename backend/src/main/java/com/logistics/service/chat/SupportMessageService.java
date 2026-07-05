package com.logistics.service.chat;

import com.logistics.dto.chat.SupportMessageDto;
import com.logistics.entity.SupportMessage;
import com.logistics.enums.SupportMessageSenderType;
import com.logistics.request.chat.SendSupportMessageRequest;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface SupportMessageService {
    SupportMessageDto sendMessage(Integer ticketId, Integer senderId, SendSupportMessageRequest request);
    SupportMessageDto sendMessage(Integer ticketId, Integer senderId, String roleName, SendSupportMessageRequest request);
    List<SupportMessageDto> getMessages(Integer ticketId, Integer accountId, String roleName);
    SupportMessage createInitialMessage(Integer ticketId, Integer senderAccountId, String message);
    void createSystemMessage(Integer ticketId, String message);
    SupportMessage createBotMessage(Integer ticketId, String message);
    void sendSystemBotMessage(Integer ticketId, SendSupportMessageRequest request, SupportMessageSenderType senderType);
    void markMessagesAsRead(Integer ticketId, Integer accountId);
    int countUnreadByTicketId(Integer ticketId);
    int countMessagesByTicketId(Integer ticketId);
    Optional<Map<String, Object>> getLatestMessageInfo(Integer ticketId);
    SupportMessageDto sendImageMessage(Integer ticketId, Integer senderId, MultipartFile file);
}
