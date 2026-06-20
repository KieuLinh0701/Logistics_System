package com.logistics.request.chat;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CreateSupportTicketRequest {
    @Size(max = 255, message = "Tiêu đề không được vượt quá 255 ký tự")
    private String subject;

    @Size(max = 4000, message = "Nội dung ban đầu không được vượt quá 4000 ký tự")
    private String initialMessage;

    private Integer managerAccountId;

    @Size(max = 20, message = "Priority không hợp lệ")
    private String priority;
}
