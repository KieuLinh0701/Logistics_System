package com.logistics.service.chat;

import com.logistics.dto.chat.BotPreviewResponse;
import com.logistics.entity.SupportMessage;
import com.logistics.entity.SupportTicket;

public interface SupportAssistantService {
    void handleAfterUserMessage(SupportTicket ticket, SupportMessage userMessage);
    BotPreviewResponse previewMessage(String message);
}
