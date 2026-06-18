package com.logistics.service.admin;

import com.logistics.entity.Office;
import com.logistics.entity.ShippingRequest;
import com.logistics.enums.ShippingRequestStatus;
import com.logistics.exception.AppException;
import com.logistics.exception.ShippingRequestErrorCode;
import com.logistics.exception.OfficeErrorCode;
import com.logistics.repository.OfficeRepository;
import com.logistics.repository.ShippingRequestRepository;
import com.logistics.service.common.NotificationService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.ArrayList;
import java.util.HashMap;

@Service
public class ShippingRequestAdminService {

    @Autowired
    private ShippingRequestRepository shippingRequestRepository;
    @Autowired
    private OfficeRepository officeRepository;
    @Autowired
    private NotificationService notificationService;

    public List<Map<String, Object>> listAll() {
        List<ShippingRequest> list = shippingRequestRepository.findAll();

        List<Map<String, Object>> out = new ArrayList<>();
        if (list != null) {
            for (ShippingRequest r : list) {
                Map<String, Object> m = new HashMap<>();
                m.put("id", r.getId());
                m.put("code", r.getCode());
                m.put("requestType", r.getRequestType() != null ? r.getRequestType().name() : null);
                m.put("requestContent", r.getRequestContent());
                m.put("status", r.getStatus() != null ? r.getStatus().name() : null);
                m.put("paidAt", r.getPaidAt());
                m.put("contactName", r.getContactName());
                m.put("contactPhoneNumber", r.getContactPhoneNumber());
                m.put("contactEmail", r.getContactEmail());
                m.put("response", r.getResponse());
                m.put("responseAt", r.getResponseAt());

                if (r.getOrder() != null) {
                    m.put("orderTrackingNumber", r.getOrder().getTrackingNumber());
                } else {
                    m.put("orderTrackingNumber", null);
                }
                if (r.getOffice() != null) {
                    Map<String, Object> o = new HashMap<>();
                    o.put("id", r.getOffice().getId());
                    o.put("name", r.getOffice().getName());
                    m.put("office", o);
                } else {
                    m.put("office", null);
                }
                if (r.getUser() != null) {
                    Map<String, Object> u = new HashMap<>();
                    u.put("id", r.getUser().getId());
                    u.put("firstName", r.getUser().getFirstName());
                    u.put("lastName", r.getUser().getLastName());
                    m.put("user", u);
                } else {
                    m.put("user", null);
                }
                out.add(m);
            }
        }

        return out;
    }

    public ShippingRequest detail(Integer id) {
        Optional<ShippingRequest> req = shippingRequestRepository.findById(id);
        return req.orElseThrow(() -> new AppException(ShippingRequestErrorCode.NOT_FOUND));
    }

    public void assignOffice(Integer requestId, Integer officeId) {
        ShippingRequest req = shippingRequestRepository.findById(requestId)
                .orElseThrow(() -> new AppException(ShippingRequestErrorCode.NOT_FOUND));
        Office office = officeRepository.findById(officeId)
                .orElseThrow(() -> new AppException(OfficeErrorCode.NOT_FOUND));
        req.setOffice(office);
        req.setStatus(ShippingRequestStatus.PROCESSING);
        shippingRequestRepository.save(req);
        notificationService.notifyOfficeManagerOnShippingRequestAssigned(office, req);
    }

    public void updateStatus(Integer requestId, ShippingRequestStatus status) {
        ShippingRequest req = shippingRequestRepository.findById(requestId)
                .orElseThrow(() -> new AppException(ShippingRequestErrorCode.NOT_FOUND));
        req.setStatus(status);
        shippingRequestRepository.save(req);
    }
}
