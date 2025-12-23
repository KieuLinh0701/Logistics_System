package com.logistics.request.user.address;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class AddressUserRequest {
    private int cityCode;
    private int wardCode;
    private String detail;

    @JsonProperty("isDefault")
    private boolean isDefault;
    
    private String name;
    private String phoneNumber;
}