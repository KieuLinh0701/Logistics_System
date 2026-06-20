package com.logistics.controller.chat;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.logistics.dto.chat.BotPreviewResponse;
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
        try {
            BotPreviewResponse response = supportAssistantService.previewMessage(request.getMessage());
            return ResponseEntity.ok(ApiResponse.success("Preview generated", response));
        } catch (Exception e) {
            log.error("Error in previewBotMessage", e);
            BotPreviewResponse fallback = new BotPreviewResponse(
                    "ERROR",
                    "Mình chưa xử lý được tin nhắn này. Bạn có thể tạo yêu cầu hỗ trợ để CSKH kiểm tra.",
                    true,
                    true
            );
            return ResponseEntity.ok(ApiResponse.success("Fallback response", fallback));
        }
    }
}
