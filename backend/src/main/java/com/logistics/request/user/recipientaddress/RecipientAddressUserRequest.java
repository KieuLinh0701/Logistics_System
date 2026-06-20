package com.logistics.request.user.recipientaddress;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class RecipientAddressUserRequest {
    private int cityCode;
    private int wardCode;
    private String detail;
    
    private String name;
    private String phoneNumber;

    private Double latitude;
    private Double longitude;
    private String cityName;
    private String wardName;
}