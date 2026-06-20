package com.logistics.response.user.recipientaddress;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class RecipientAddress {
    private Integer id;
    private String name;
    private String phoneNumber;
    private String fullAddress;
    private Integer cityCode;
    private String cityName;
    private Integer wardCode;
    private String wardName;
    private String detail;
    private Double latitude;
    private Double longitude;
}