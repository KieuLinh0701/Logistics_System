package com.logistics.dto.chat;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

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
