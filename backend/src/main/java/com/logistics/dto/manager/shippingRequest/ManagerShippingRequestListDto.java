package com.logistics.dto.manager.shippingRequest;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ManagerShippingRequestListDto {
    private Integer id;
    private String code;
    private String orderTrackingNumber;
    private String userCode;
    private String contactName;
    private String contactEmail;
    private String contactPhoneNumber;
    private Integer contactCityCode;
    private Integer contactWardCode;
    private String contactDetail;
    private String requestType;
    private String requestContent;
    private String status;
    private String response;
    private LocalDateTime createdAt;
    private LocalDateTime responseAt;
}
