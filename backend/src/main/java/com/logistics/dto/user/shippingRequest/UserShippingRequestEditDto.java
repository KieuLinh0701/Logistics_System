package com.logistics.dto.user.shippingRequest;

import java.util.List;

import com.logistics.dto.ShippingRequestAttachmentDto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class UserShippingRequestEditDto {
    private Integer id;
    private String code;
    private String orderTrackingNumber;
    private String requestType;
    private String requestContent;
    private List<ShippingRequestAttachmentDto> requestAttachments;
}
