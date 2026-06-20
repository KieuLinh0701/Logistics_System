package com.logistics.request.manager.shippingRequest;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

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
