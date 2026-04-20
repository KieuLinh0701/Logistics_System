package com.logistics.websocket;

import java.security.Principal;

import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.stereotype.Controller;

import com.logistics.response.ApiResponse;
import com.logistics.request.chat.SendSupportMessageRequest;
import com.logistics.request.chat.SupportChatSendMessagePayload;
import com.logistics.dto.chat.SupportMessageDto;
import com.logistics.service.chat.SupportMessageService;

import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
public class SupportChatWebSocketController {

    private final SupportMessageService supportMessageService;

    @MessageMapping("/chat.sendMessage")
    public void sendMessage(SupportChatSendMessagePayload payload, Principal principal) {
        if (payload == null || payload.getTicketId() == null || payload.getSenderAccountId() == null) {
            return;
        }

        if (principal == null || principal.getName() == null) {
            return;
        }

        Integer principalAccountId;
        try {
            principalAccountId = Integer.parseInt(principal.getName());
        } catch (NumberFormatException e) {
            return;
        }

        if (!principalAccountId.equals(payload.getSenderAccountId())) {
            return;
        }

        SendSupportMessageRequest request = new SendSupportMessageRequest(
                payload.getMessage(),
                payload.getMessageType(),
                payload.getIsInternalNote());

        ApiResponse<SupportMessageDto> response = supportMessageService.sendMessage(
                payload.getTicketId(),
                payload.getSenderAccountId(),
                request);

        if (!response.isSuccess()) {
            // Keep silent for websocket send
        }
    }
}
