package com.logistics.request.user.shippingRequest;

import com.logistics.enums.ShippingRequestType;
import com.logistics.utils.ShippingRequestUtils;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public record UserShippingRequestForm (
        @NotNull(message = "Loại yêu cầu không được để trống")
        ShippingRequestType requestType,

        String trackingNumber,

        @Size(max = 1000, message = "Nội dung yêu cầu không được vượt quá 1000 ký tự")
        String requestContent,

        String oldAttachments,
        List<MultipartFile> attachments
) {
    public UserShippingRequestForm {

        // Logic check của bạn: Nội dung không được để trống tùy theo loại
        if (!ShippingRequestUtils.canUserEmptyContentRequest(requestType) && (requestContent == null || requestContent.isBlank())) {
            throw new IllegalArgumentException("Nội dung yêu cầu không được để trống");
        }

        // Logic check: Mã đơn hàng không được để trống tùy theo loại
        if (!ShippingRequestUtils.canUserEmptyTrackingNumber(requestType) && (trackingNumber == null || trackingNumber.isBlank())) {
            throw new IllegalArgumentException("Mã đơn hàng không được để trống");
        }
    }
}
