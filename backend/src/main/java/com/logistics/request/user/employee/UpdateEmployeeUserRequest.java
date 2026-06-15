package com.logistics.request.user.employee;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UpdateEmployeeUserRequest {
    private String firstName;
    private String lastName;
    private String phoneNumber;
}
