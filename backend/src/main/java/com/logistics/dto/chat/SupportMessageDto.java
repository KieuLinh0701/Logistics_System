package com.logistics.dto.chat;

import com.logistics.enums.SupportMessageSenderType;
import com.logistics.enums.SupportMessageType;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SupportMessageDto {
    private Integer id;
    private Integer ticketId;
    private Integer senderAccountId;
    private SupportMessageSenderType senderType;
    private String senderLabel;
    private Boolean isBotMessage;
    private String message;
    private SupportMessageType messageType;
    private Boolean isInternalNote;
    private LocalDateTime createdAt;
    private String senderName;
    private String senderImage;
    private Boolean isRead;
}
