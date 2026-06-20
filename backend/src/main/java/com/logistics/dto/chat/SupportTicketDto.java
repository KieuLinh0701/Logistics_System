package com.logistics.dto.chat;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SupportTicketDto {
    private Integer id;
    private String code;
    private String createdByName;
    private String createdByImage;
    private Integer createdByAccountId;
    private Integer assignedToAccountId;
    private String assignedToName;
    private Integer totalMessages;
    private String latestMessage;
    private Integer latestMessageSenderAccountId;
    private LocalDateTime latestMessageAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

}
