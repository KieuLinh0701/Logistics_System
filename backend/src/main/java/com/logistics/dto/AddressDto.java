package com.logistics.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class AddressDto {
    private Integer id;
    private int wardCode;
    private int cityCode;
    private String detail;

    @JsonProperty("isDefault")
    private boolean isDefault;

    private String name;

    private String phoneNumber;

    private String fullAddress;
    private String cityName;
    private String wardName;
    private Double latitude;
    private Double longitude;
}