package com.logistics.service.settlement;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.logistics.entity.*;
import com.logistics.enums.*;
import com.logistics.repository.*;
import com.logistics.service.common.NotificationService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class SettlementBatchSchedulerService {

    private final OrderRepository orderRepository;
    private final SettlementBatchRepository batchRepository;
    private final SettlementTransactionRepository transactionRepository;
    private final BankAccountRepository bankAccountRepository;
    private final UserSettlementScheduleRepository scheduleRepository;
    private final NotificationService notificationService;

    // @Scheduled(cron = "0 * * * * ?") 
    @Scheduled(cron = "0 0 20 * * ?") // 20:00 mỗi ngày
    @Transactional
    public void createDailySettlementBatch() {
        System.out.println("Start creating automatic settlement batch: " + LocalDateTime.now());

        // Lấy tất cả user có lịch đối soát hôm nay
        DayOfWeek today = LocalDate.now().getDayOfWeek();
        WeekDay weekDay = WeekDay.valueOf(today.name());
        List<UserSettlementSchedule> schedulesToday = scheduleRepository.findAllWithScheduleToday(weekDay);

        for (UserSettlementSchedule schedule : schedulesToday) {
            User shop = schedule.getUser();

            // Lấy các đơn hàng DELIVERED / RETURNED mà chưa có settlementBatch
            List<Order> orders = orderRepository.findByUserAndSettlementBatchIsNullAndStatusIn(
                    shop,
                    List.of(OrderStatus.DELIVERED, OrderStatus.RETURNED));

            if (orders.isEmpty())
                continue;

            // Tạo settlement batch
            SettlementBatch batch = new SettlementBatch();
            batch.setShop(shop);
            batch.setStatus(SettlementStatus.PENDING);
            batch.setBalanceAmount(BigDecimal.ZERO);
            batchRepository.save(batch);

            BigDecimal totalCOD = BigDecimal.ZERO;

            for (Order order : orders) {
                PaymentSubmission ps = order.getPaymentSubmission();

                if (ps == null && order.getCod() == 0 && order.getPayer() == OrderPayerType.SHOP) {
                    continue;
                }

                boolean validCOD = true;
                if (ps != null) {
                    if (!(ps.getStatus() == PaymentSubmissionStatus.MATCHED
                            || ps.getStatus() == PaymentSubmissionStatus.ADJUSTED)) {
                        validCOD = false;
                    }
                }

                if (!validCOD)
                    continue;

                BigDecimal codAmount = BigDecimal.valueOf(order.getCod());
                if (order.getStatus() == OrderStatus.RETURNED &&
                        order.getPaymentStatus() == OrderPaymentStatus.UNPAID) {
                    codAmount = BigDecimal.valueOf(-order.getTotalFee());
                } else if (order.getStatus() == OrderStatus.RETURNED &&
                        order.getPaymentStatus() == OrderPaymentStatus.PAID &&
                        order.getPayer() == OrderPayerType.CUSTOMER) {
                    codAmount = BigDecimal.ZERO;
                } else if (order.getStatus() == OrderStatus.DELIVERED &&
                        order.getPaymentStatus() == OrderPaymentStatus.PAID &&
                        order.getPayer() == OrderPayerType.CUSTOMER) {
                    codAmount = BigDecimal.valueOf(order.getCod());
                } else {
                    codAmount = BigDecimal.valueOf(order.getCod() - order.getTotalFee());
                }

                totalCOD = totalCOD.add(codAmount);

                order.setSettlementBatch(batch);
                orderRepository.save(order);
            }

            batch.setBalanceAmount(totalCOD);
            batchRepository.save(batch);

            // Nếu tổng COD > 0 thì tạo transaction giả lập SYSTEM -> SHOP
            SettlementTransaction transaction = null;
            if (totalCOD.compareTo(BigDecimal.ZERO) > 0) {
                BankAccount defaultBank = bankAccountRepository.findDefaultByUser(shop);

                transaction = new SettlementTransaction();
                transaction.setSettlementBatch(batch);
                transaction.setAmount(totalCOD);
                transaction.setType(SettlementTransactionType.SYSTEM_TO_SHOP);
                transaction.setStatus(SettlementTransactionStatus.SUCCESS);
                transaction.setBankName(defaultBank.getBankName());
                transaction.setAccountNumber(defaultBank.getAccountNumber());
                transaction.setAccountName(defaultBank.getAccountName());
                transaction.setPaidAt(LocalDateTime.now());

                transactionRepository.save(transaction);
            }

            if (totalCOD.compareTo(BigDecimal.ZERO) > 0) {
                if (transaction != null
                        && transaction.getStatus() == SettlementTransactionStatus.SUCCESS) {
                    batch.setStatus(SettlementStatus.COMPLETED);
                } else {
                    batch.setStatus(SettlementStatus.FAILED);
                }
            }

            for (Order order : orders) {
                if (order.getSettlementBatch() != null && order.getSettlementBatch().getId().equals(batch.getId())) {

                    order.setCodStatus(OrderCodStatus.TRANSFERRED);

                    if (order.getPaymentStatus() != OrderPaymentStatus.PAID &&
                            ((transaction != null && transaction.getStatus() == SettlementTransactionStatus.SUCCESS)
                                    || totalCOD.compareTo(BigDecimal.ZERO) >= 0)) {
                        order.setPaymentStatus(OrderPaymentStatus.PAID);
                        order.setPaidAt(LocalDateTime.now());
                    }

                    orderRepository.save(order);
                }
            }

            notificationService.create(
                    "Phiên đối soát theo lịch đã được tạo",
                    String.format(
                            "Phiên đối soát #%s của bạn đã được tạo. Tổng COD và phí dịch vụ đã được ghi nhận. Vui lòng xem chi tiết trong hệ thống.",
                            batch.getCode()),
                    "settlement_batch",
                    shop.getId(),
                    null,
                    "settlements",
                    batch.getId().toString());

            System.out.println("Settlement batch for shop " + shop.getId());
        }

        System.out.println("Finished creating automatic settlement batch.");
    }
}