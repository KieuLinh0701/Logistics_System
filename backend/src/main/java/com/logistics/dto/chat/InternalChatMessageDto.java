package com.logistics.dto.chat;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InternalChatMessageDto {
    private Integer id;
    private Integer roomId;
    private Integer senderAccountId;
    private String senderName;
    private String senderRole;
    private String senderAvatar;
    private String message;
    private Boolean isMine;
    private Boolean isRead;
    private LocalDateTime createdAt;
}
