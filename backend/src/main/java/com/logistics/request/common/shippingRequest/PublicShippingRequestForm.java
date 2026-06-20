package com.logistics.request.common.shippingRequest;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PublicShippingRequestForm {
    private String contactName;
    private String contactEmail;
    private String contactPhoneNumber;
    private String requestType;
    private String requestContent;
}
