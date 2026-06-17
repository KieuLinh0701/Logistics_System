package com.logistics.request.user.employee;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CreateEmployeeUserRequest {
    private String email;
    private String firstName;
    private String lastName;
    private String phoneNumber;
    private Integer roleId;
}
