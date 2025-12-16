package com.logistics.request.manager.shippingRequest;

import java.util.List;

import org.springframework.web.multipart.MultipartFile;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ManagerShippingRequestForm { 
    private String status;
    private String response;
    private String oldAttachments;
    private List<MultipartFile> attachments;
}
