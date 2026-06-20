package com.logistics.request.chat;

import com.logistics.enums.SupportMessageType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SupportChatSendMessagePayload {
    @NotNull(message = "Ticket ID không được để trống")
    private Integer ticketId;

    @NotNull(message = "Sender account ID không được để trống")
    private Integer senderAccountId;

    @NotBlank(message = "Nội dung tin nhắn không được để trống")
    @Size(max = 4000, message = "Nội dung tin nhắn không được vượt quá 4000 ký tự")
    private String message;

    @NotNull(message = "Loại tin nhắn không được để trống")
    private SupportMessageType messageType = SupportMessageType.TEXT;

    private Boolean isInternalNote = false;
}
