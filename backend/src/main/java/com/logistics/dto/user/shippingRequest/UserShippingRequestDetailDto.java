package com.logistics.dto.user.shippingRequest;

import java.time.LocalDateTime;
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
public class UserShippingRequestDetailDto {
    private Integer id;
    private String code;
    private String orderTrackingNumber;
    private String handlerName;
    private String handlerPhone;
    private String handlerEmail;
    private String requestType;
    private String requestContent;
    private String status;
    private String response;
    private LocalDateTime createdAt;
    private LocalDateTime responseAt;
    private List<ShippingRequestAttachmentDto> requestAttachments;
    private List<ShippingRequestAttachmentDto> responseAttachments;
}
