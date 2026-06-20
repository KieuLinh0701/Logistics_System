package com.logistics.dto.user.shippingRequest;

import com.logistics.dto.ShippingRequestAttachmentDto;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class UserShippingRequestDetailDto {
    private Integer id;
    private String code;
    private String orderTrackingNumber;
    private String handlerName;
    private String handlerPhoneNumber;
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
