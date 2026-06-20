package com.logistics.dto.chat;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SupportAssignManagerOption {
    private Integer accountId;
    private String fullName;
    private String email;
    private String phone;
}
