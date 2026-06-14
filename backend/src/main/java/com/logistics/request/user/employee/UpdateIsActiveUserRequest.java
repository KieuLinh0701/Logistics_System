package com.logistics.request.user.employee;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class UpdateIsActiveUserRequest {
    private Boolean isActive;
    private Integer roleId;
}
