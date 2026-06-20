package com.logistics.request.common.auth;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ChooseRoleRequest {
    private String tempToken;
    private String roleName;

    @Override
    public String toString() {
        return "ChooseRoleRequest{tempToken='******', roleName='" + roleName + "'}";
    }
}