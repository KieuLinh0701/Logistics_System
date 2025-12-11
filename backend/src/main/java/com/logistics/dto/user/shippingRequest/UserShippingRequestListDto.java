package com.logistics.dto.user.shippingRequest;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class UserShippingRequestListDto {
    private Integer id;
    private String code;
    private String orderTrackingNumber;
    private String requestType;
    private String requestContent;
    private String status;
    private String response;
    private LocalDateTime createdAt;
    private LocalDateTime responseAt;
}
