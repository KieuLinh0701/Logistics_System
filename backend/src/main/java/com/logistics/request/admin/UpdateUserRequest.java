package com.logistics.request.admin;

import lombok.Data;

import java.util.List;

@Data
public class UpdateUserRequest {
    private String firstName;
    private String lastName;
    private String phoneNumber;
    private String password;
    private List<Integer> roleIds;
    private Boolean isActive;
}




