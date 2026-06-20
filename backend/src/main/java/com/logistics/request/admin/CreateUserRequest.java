package com.logistics.request.admin;

import lombok.Data;

import java.util.List;

@Data
public class CreateUserRequest {
    private String email;
    private String password;
    private String firstName;
    private String lastName;
    private String phoneNumber;
    private List<Integer> roleIds;
    private Boolean isActive;
}




