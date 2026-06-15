package com.logistics.dto.user.employee;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class EmployeeByRoleIdListUserDto {
    private Integer id; // userId
    private String code;
    private String fullName;
    private Boolean isActive; // accountRole
    private String email;
    private String phoneNumber;
    private LocalDateTime updatedAt;
}
