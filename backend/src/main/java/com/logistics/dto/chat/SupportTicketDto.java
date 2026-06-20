package com.logistics.dto.chat;

import java.time.LocalDateTime;

import com.logistics.enums.SupportTicketStatus;

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

    private SupportTicketStatus status;
    private String subject;
    private String priority;
    private Integer officeId;
    private String officeName;
    private Integer unreadCount;
    private Boolean isAssigned;
    private LocalDateTime closedAt;
    private String closedByName;
}
