package com.logistics.request.chat;

import jakarta.validation.constraints.NotBlank;
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
    @NotBlank(message = "Nội dung ban đầu không được để trống")
    @Size(max = 4000, message = "Nội dung ban đầu không được vượt quá 4000 ký tự")
    private String initialMessage;

    private Integer managerAccountId;
}
