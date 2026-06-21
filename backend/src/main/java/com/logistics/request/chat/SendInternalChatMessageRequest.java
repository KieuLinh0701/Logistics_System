package com.logistics.request.chat;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SendInternalChatMessageRequest {

    @NotBlank(message = "Tin nhắn không được để trống")
    private String message;
}
