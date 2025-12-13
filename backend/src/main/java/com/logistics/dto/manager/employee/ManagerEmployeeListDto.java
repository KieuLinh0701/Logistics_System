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
    private String userLastName;
    private String userFirstName;
    private String userPhoneNumber;
    private Integer userCityCode;
    private Integer userWardCode;
    private String userDetail;
    private String userRole;
    private String userEmail;
    private LocalDateTime hireDate;
    private String shift;
    private String status;
}
