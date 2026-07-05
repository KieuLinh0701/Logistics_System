package com.logistics.service.financial;

import com.logistics.entity.Order;

public interface FinancialValidationService {
    boolean canMarkOrderPaid(Integer orderId);
    boolean markOrderPaidIfEligible(Order order);
    void forceMarkPaid(Order order);
}
