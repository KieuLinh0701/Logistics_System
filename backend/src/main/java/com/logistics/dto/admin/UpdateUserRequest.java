package com.logistics.dto.admin;

import lombok.Data;

@Data
public class UpdateUserRequest {
    private String firstName;
    private String lastName;
    private String phoneNumber;
    private String password;
    private Integer roleId;
    private Boolean isActive;
}



