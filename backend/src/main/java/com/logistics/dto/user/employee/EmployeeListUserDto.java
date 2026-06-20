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
public class EmployeeListUserDto {
        private Integer id;
        private String code;
        private String lastName;
        private String firstName;
        private String email;
        private String phoneNumber;
        private LocalDateTime updatedAt;
}
