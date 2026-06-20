package com.logistics.request.manager.employee;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ManagerEmployeeEditRequest {
    private String userFirstName;
    private String userLastName;
    private String userPhoneNumber;
    private String userRole;
    private String userEmail;
    private String hireDate;
    private String shift;
    private String status;
}
