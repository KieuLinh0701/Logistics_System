package com.logistics.service.common;

import com.logistics.entity.Order;
import com.logistics.entity.Office;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class OrderDestinationService {

    public boolean isDestinationOffice(Order order, Office office) {
        if (order == null || office == null) {
            return false;
        }

        // 1. Ưu tiên chính: So sánh toOffice.id
        if (order.getToOffice() != null && order.getToOffice().getId() != null) {
            if (order.getToOffice().getId().equals(office.getId())) {
                return true;
            }
        }

        // 2. Fallback theo địa chỉ: Lấy cityCode/wardCode từ Order
        Integer orderCityCode = null;
        Integer orderWardCode = null;

        if (order.getRecipientAddress() != null) {
            orderCityCode = order.getRecipientAddress().getCityCode();
            orderWardCode = order.getRecipientAddress().getWardCode();
        }

        if (orderCityCode == null) {
            orderCityCode = order.getRecipientCityCode();
        }
        if (orderWardCode == null) {
            orderWardCode = order.getRecipientWardCode();
        }

        // Lấy cityCode/wardCode từ office
        Integer officeCityCode = office.getCityCode();
        Integer officeWardCode = office.getWardCode();

        // 3. Nếu có đủ wardCode ở cả hai bên: so sánh cả cityCode + wardCode
        if (orderWardCode != null && officeWardCode != null
                && orderCityCode != null && officeCityCode != null) {
            return orderCityCode.equals(officeCityCode) && orderWardCode.equals(officeWardCode);
        }

        // 4. Fallback cuối: chỉ so sánh cityCode
        if (orderCityCode != null && officeCityCode != null) {
            return orderCityCode.equals(officeCityCode);
        }

        return false;
    }
}
