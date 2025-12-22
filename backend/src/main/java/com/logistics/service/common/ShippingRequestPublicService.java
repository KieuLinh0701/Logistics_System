package com.logistics.service.common;

import com.logistics.entity.ShippingRequest;
import com.logistics.enums.ShippingRequestType;
import com.logistics.repository.ShippingRequestRepository;
import com.logistics.request.common.shippingRequest.PublicShippingRequestForm;
import com.logistics.response.ApiResponse;
import com.logistics.utils.ShippingRequestUtils;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ShippingRequestPublicService {

    private final ShippingRequestRepository repository;

    public ApiResponse<Boolean> create(PublicShippingRequestForm request) {

        validateForm(request);

        ShippingRequestType type = ShippingRequestType.valueOf(request.getRequestType());

        if (!ShippingRequestUtils.canGuestCreateShippingRequest(type)) {
            return new ApiResponse<>(false, "Loại yêu cầu không hợp lệ", null);
        }

        ShippingRequest shippingRequest = new ShippingRequest();
        shippingRequest.setRequestType(type);
        shippingRequest.setContactName(request.getContactName());
        shippingRequest.setContactEmail(request.getContactEmail());
        shippingRequest.setContactPhoneNumber(request.getContactPhoneNumber());
        shippingRequest.setRequestContent(request.getRequestContent());

        shippingRequest = repository.save(shippingRequest);

        String code = ShippingRequestUtils.generateRequestCode(shippingRequest.getId());
        shippingRequest.setCode(code);
        repository.save(shippingRequest);

        return new ApiResponse<>(true, "Gửi liên hệ thành công", true);
    }

    private void validateForm(PublicShippingRequestForm request) {
        List<String> missing = new ArrayList<>();

        if (isBlank(request.getContactName())) {
            throw new RuntimeException("Tên");
        }

        if (isBlank(request.getContactEmail())) {
            throw new RuntimeException("Email");
        }

        if (isBlank(request.getContactPhoneNumber())) {
            throw new RuntimeException("Số điện thoại");
        }

        if (isBlank(request.getRequestType()))
            missing.add("Loại yêu cầu");

        if (isBlank(request.getRequestContent())) {
            throw new RuntimeException("Nội dung");
        }

        if (!missing.isEmpty())
            throw new RuntimeException("Thiếu thông tin: " + String.join(", ", missing));

        ShippingRequestType type;
        try {
            type = ShippingRequestType.valueOf(request.getRequestType());
        } catch (Exception e) {
            throw new RuntimeException("Loại yêu cầu không hợp lệ");
        }

        if (!isBlank(request.getRequestContent()) && request.getRequestContent().length() > 1000) {
            throw new RuntimeException("Nội dung yêu cầu không được vượt quá 1000 ký tự");
        }

    }

    private boolean isBlank(String s) {
        return s == null || s.isBlank();
    }

}