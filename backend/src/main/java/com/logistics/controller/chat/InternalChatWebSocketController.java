package com.logistics.controller.chat;

import java.security.Principal;

import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Controller;

import com.logistics.exception.AppException;
import com.logistics.request.chat.InternalChatPayload;
import com.logistics.service.chat.InternalChatService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Controller
@RequiredArgsConstructor
@Slf4j
public class InternalChatWebSocketController {

    private final InternalChatService internalChatService;

    @MessageMapping("/internal-chat.sendMessage")
    public void sendMessage(@Payload InternalChatPayload payload, Principal principal) {
        if (payload == null || payload.getRoomId() == null || payload.getSenderAccountId() == null) {
            log.warn("Invalid payload: roomId or senderAccountId is null");
            return;
        }

        if (principal == null || principal.getName() == null) {
            log.warn("Invalid principal for internal chat");
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

        try {
            internalChatService.sendMessage(payload.getRoomId(), payload.getMessage());
        } catch (AppException e) {
            log.error("AppException in internal chat: {} - {}", e.getErrorCode(), e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error in internal chat WebSocket", e);
            throw e;
        }
    }
}
