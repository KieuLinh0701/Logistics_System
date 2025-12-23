package com.logistics.request.admin;

import lombok.Data;

@Data
public class CreateUserRequest {
    private String email;
    private String password;
    private String firstName;
    private String lastName;
    private String phoneNumber;
    private Integer roleId;
    private Boolean isActive;
}




