package com.logistics.request.admin;

import java.util.List;
import lombok.Data;

@Data
public class UpdateUserRequest {
    private String firstName;
    private String lastName;
    private String phoneNumber;
    private String password;
    private List<Integer> roleIds;
    private Boolean isActive;
}




