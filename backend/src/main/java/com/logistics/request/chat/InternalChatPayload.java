package com.logistics.request.chat;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class InternalChatPayload {
    private Integer roomId;
    private Integer senderAccountId;
    private String message;
}
