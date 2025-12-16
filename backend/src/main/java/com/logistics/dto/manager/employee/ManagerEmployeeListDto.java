package com.logistics.dto.manager.employee;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ManagerEmployeeListDto {
    private Integer id;
    private String code;
    private String lastName;
    private String firstName;
    private String phoneNumber;
    private String role;
    private String email;
    private LocalDateTime hireDate;
    private String shift;
    private String status;
}
