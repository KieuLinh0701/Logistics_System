package com.logistics.service.chat;

import com.logistics.entity.SupportMessage;
import com.logistics.enums.SupportMessageSenderType;

public interface SupportBotMessageService {
    SupportMessage createBotMessage(Integer ticketId, String message);
    void sendSystemBotMessage(Integer ticketId, String message, SupportMessageSenderType senderType);
}
