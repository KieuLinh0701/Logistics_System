package com.logistics.dto.chat;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

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
    private String messageType;
    private String imageUrl;
    private Boolean isMine;
    private Boolean isRead;
    private LocalDateTime createdAt;
}
