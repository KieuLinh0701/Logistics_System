package com.logistics.service.financial.impl;

import com.logistics.entity.Order;
import com.logistics.enums.OrderPayerType;
import com.logistics.enums.OrderPaymentStatus;
import com.logistics.enums.PaymentSubmissionStatus;
import com.logistics.repository.OrderRepository;
import com.logistics.repository.PaymentSubmissionRepository;
import com.logistics.service.financial.FinancialValidationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class FinancialValidationServiceImpl implements FinancialValidationService {

    private final PaymentSubmissionRepository submissionRepository;
    private final OrderRepository orderRepository;

    @Override
    public boolean canMarkOrderPaid(Integer orderId) {
        if (orderId == null) return false;
        Order order = orderRepository.findById(orderId).orElse(null);
        if (order == null) return false;
        if (order.getPaymentStatus() == OrderPaymentStatus.PAID) return false;

        BigDecimal expectedServiceFee = computeExpectedServiceFee(order);
        if (expectedServiceFee == null || expectedServiceFee.compareTo(BigDecimal.ZERO) <= 0) {
            return false;
        }

        BigDecimal sumActual = submissionRepository.sumActualAmountByOrderIdAndStatusIn(orderId,
            List.of(PaymentSubmissionStatus.MATCHED, PaymentSubmissionStatus.ADJUSTED));
        if (sumActual == null) sumActual = BigDecimal.ZERO;
        return sumActual.compareTo(expectedServiceFee) >= 0;
    }

    private BigDecimal computeExpectedServiceFee(Order order) {
        if (order == null) return BigDecimal.ZERO;
        if (order.getPayer() == OrderPayerType.CUSTOMER) {
            Integer shippingFee = order.getShippingFee();
            return shippingFee != null ? BigDecimal.valueOf(shippingFee) : BigDecimal.ZERO;
        }
        return BigDecimal.ZERO;
    }

    @Override
    @Transactional
    public boolean markOrderPaidIfEligible(Order order) {
        if (order == null) return false;
        try {
            if (order.getPaymentStatus() == OrderPaymentStatus.PAID) return false;
            Integer orderId = order.getId();
            if (orderId == null) return false;
            if (canMarkOrderPaid(orderId)) {
                order.setPaymentStatus(OrderPaymentStatus.PAID);
                order.setPaidAt(LocalDateTime.now());
                orderRepository.save(order);
                return true;
            }
        } catch (Exception e) {
        }
        return false;
    }

    @Override
    @Transactional
    public void forceMarkPaid(Order order) {
        if (order == null) return;
        order.setPaymentStatus(OrderPaymentStatus.PAID);
        order.setPaidAt(LocalDateTime.now());
        orderRepository.save(order);
    }
}
