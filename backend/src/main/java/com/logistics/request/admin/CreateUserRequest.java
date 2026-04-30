package com.logistics.request.admin;

import java.util.List;
import lombok.Data;

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




