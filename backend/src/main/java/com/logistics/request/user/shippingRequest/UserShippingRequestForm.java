package com.logistics.request.user.shippingRequest;

import java.util.List;

import org.springframework.web.multipart.MultipartFile;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UserShippingRequestForm { 
    private String requestType;
    private String trackingNumber;
    private String requestContent;

    private String oldAttachments;
    private List<MultipartFile> attachments;
}
