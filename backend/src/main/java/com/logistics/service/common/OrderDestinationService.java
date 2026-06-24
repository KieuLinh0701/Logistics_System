package com.logistics.service.common;

import com.logistics.entity.Office;
import com.logistics.entity.Order;
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

        // 2. Fallback theo địa chỉ: Lấy cityCode từ Order
        Integer orderCityCode = order.getRecipientCityCode();

        System.out.println("orderCityCode" + orderCityCode);

        // Lấy cityCode/wardCode từ office
        Integer officeCityCode = office.getCityCode();

        System.out.println("officeCityCode" + officeCityCode);


        // 3. Nếu có đủ cityCode ở cả hai bên: so sánh cityCode
        if (orderCityCode != null && officeCityCode != null) {
            return orderCityCode.equals(officeCityCode);
        }

        return false;
    }
}
