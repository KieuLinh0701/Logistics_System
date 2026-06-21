package com.logistics.controller.chat;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.logistics.dto.chat.BotPreviewResponse;
import com.logistics.exception.AppException;
import com.logistics.exception.enums.SupportMessageErrorCode;
import com.logistics.request.chat.BotPreviewRequest;
import com.logistics.response.ApiResponse;
import com.logistics.service.chat.SupportAssistantService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/support/bot")
@RequiredArgsConstructor
@Slf4j
public class SupportBotController {

    private final SupportAssistantService supportAssistantService;

    @PostMapping("/preview")
    public ResponseEntity<ApiResponse<BotPreviewResponse>> previewBotMessage(
            @Valid @RequestBody BotPreviewRequest request) {
        BotPreviewResponse response = supportAssistantService.previewMessage(request.getMessage());
        return ResponseEntity.ok(ApiResponse.success("Preview generated", response));
    }
}
