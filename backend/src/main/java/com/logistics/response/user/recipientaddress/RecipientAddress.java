package com.logistics.response.user.recipientaddress;

import com.logistics.enums.RecipientAddressType;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

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