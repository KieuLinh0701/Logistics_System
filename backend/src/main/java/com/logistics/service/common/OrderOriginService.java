package com.logistics.service.common;

import com.logistics.entity.Office;
import com.logistics.entity.Order;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Kiểm tra xem một office có phải là bưu cục gốc của đơn hay không.
 */
@Service
@RequiredArgsConstructor
public class OrderOriginService {

    /**
     * Kiểm tra office hiện tại có phải bưu cục gốc của đơn không.
     * Ưu tiên so sánh order.fromOffice.id với office.id.
     * Nếu fromOffice null thì fallback so sánh senderCityCode/senderWardCode.
     */
    public boolean isOriginOffice(Order order, Office office) {
        if (order == null || office == null) {
            return false;
        }

        // 1. Ưu tiên: So sánh fromOffice.id
        if (order.getFromOffice() != null && order.getFromOffice().getId() != null) {
            if (order.getFromOffice().getId().equals(office.getId())) {
                return true;
            }
        }

        // 2. Fallback theo địa chỉ người gửi: so sánh cityCode/wardCode
        Integer orderSenderCityCode = order.getSenderCityCode();
        Integer orderSenderWardCode = order.getSenderWardCode();

        Integer officeCityCode = office.getCityCode();
        Integer officeWardCode = office.getWardCode();

        if (orderSenderWardCode != null && officeWardCode != null
                && orderSenderCityCode != null && officeCityCode != null) {
            return orderSenderCityCode.equals(officeCityCode) && orderSenderWardCode.equals(officeWardCode);
        }

        if (orderSenderCityCode != null && officeCityCode != null) {
            return orderSenderCityCode.equals(officeCityCode);
        }

        return false;
    }
}
