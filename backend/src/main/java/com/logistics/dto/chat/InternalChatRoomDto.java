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
public class InternalChatRoomDto {
    private Integer id;
    private Integer employeeAccountId;
    private String employeeName;
    private String employeeRole;
    private String employeeAvatar;
    private Integer managerAccountId;
    private String managerName;
    private String managerAvatar;
    private Integer officeId;
    private String officeName;
    private String lastMessage;
    private LocalDateTime lastMessageAt;
    private Integer lastSenderAccountId;
    private Long unreadCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
