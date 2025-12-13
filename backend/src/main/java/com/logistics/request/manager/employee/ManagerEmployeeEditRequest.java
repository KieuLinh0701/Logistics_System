package com.logistics.request.manager.employee;

import lombok.*;

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
