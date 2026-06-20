package com.logistics.request.chat;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class BotPreviewRequest {
    @NotBlank(message = "Message is required")
    private String message;
}
