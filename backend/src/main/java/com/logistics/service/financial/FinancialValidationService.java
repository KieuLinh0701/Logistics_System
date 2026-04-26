package com.logistics.service.financial;

import java.math.BigDecimal;
import java.util.List;
import java.time.LocalDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.logistics.entity.Order;
import com.logistics.enums.PaymentSubmissionStatus;
import com.logistics.enums.OrderPaymentStatus;
import com.logistics.repository.OrderRepository;
import com.logistics.repository.PaymentSubmissionRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class FinancialValidationService {

    

    private final PaymentSubmissionRepository submissionRepository;
    private final OrderRepository orderRepository;

    public boolean canMarkOrderPaid(Integer orderId) {
        if (orderId == null) return false;
        BigDecimal sumActual = submissionRepository.sumActualAmountByOrderIdAndStatusIn(orderId,
            List.of(PaymentSubmissionStatus.MATCHED, PaymentSubmissionStatus.ADJUSTED));
        if (sumActual == null) sumActual = BigDecimal.ZERO;
        BigDecimal expected = orderRepository.findById(orderId)
            .map(o -> o.getCod() != null ? BigDecimal.valueOf(o.getCod()) : BigDecimal.ZERO)
            .orElse(BigDecimal.ZERO);
        return expected.compareTo(BigDecimal.ZERO) > 0 && sumActual.compareTo(expected) >= 0;
    }

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

    @Transactional
    public void forceMarkPaid(Order order) {
        if (order == null) return;
        order.setPaymentStatus(OrderPaymentStatus.PAID);
        order.setPaidAt(LocalDateTime.now());
        orderRepository.save(order);
        
    }
}
