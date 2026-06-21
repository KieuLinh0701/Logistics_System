package com.logistics.controller.chat;

import java.security.Principal;

import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.stereotype.Controller;

import com.logistics.exception.AppException;
import com.logistics.request.chat.SendSupportMessageRequest;
import com.logistics.request.chat.SupportChatSendMessagePayload;
import com.logistics.service.chat.SupportMessageService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Controller
@RequiredArgsConstructor
@Slf4j
public class SupportChatWebSocketController {

    private final SupportMessageService supportMessageService;

    @MessageMapping("/chat.sendMessage")
    public void sendMessage(SupportChatSendMessagePayload payload, Principal principal) {
        if (payload == null || payload.getTicketId() == null || payload.getSenderAccountId() == null) {
            log.warn("Invalid support chat payload: ticketId or senderAccountId is null");
            return;
        }

        if (principal == null || principal.getName() == null) {
            log.warn("Invalid principal for support chat");
            return;
        }

        Integer principalAccountId;
        try {
            principalAccountId = Integer.parseInt(principal.getName());
        } catch (NumberFormatException e) {
            log.warn("Invalid principal name format: {}", principal.getName());
            return;
        }

        if (!principalAccountId.equals(payload.getSenderAccountId())) {
            log.warn("Principal accountId {} does not match senderAccountId {}", principalAccountId, payload.getSenderAccountId());
            return;
        }

        SendSupportMessageRequest request = new SendSupportMessageRequest(
                payload.getMessage(),
                payload.getMessageType(),
                payload.getIsInternalNote());

        try {
            supportMessageService.sendMessage(
                    payload.getTicketId(),
                    payload.getSenderAccountId(),
                    request);
        } catch (AppException e) {
            log.error("AppException in support chat: {} - {}", e.getErrorCode(), e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error in support chat WebSocket", e);
            throw e;
        }
    }
}
