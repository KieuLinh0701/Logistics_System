package com.logistics.scheduler.settlement;

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
                    List.of(
                            OrderStatus.DELIVERED,
                            OrderStatus.RETURNED
                    ));

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
                List<PaymentSubmission> submissions = order.getPaymentSubmissions();
                PaymentSubmission ps = null;
                if (submissions != null && !submissions.isEmpty()) {
                    ps = submissions.stream()
                            .filter(s -> s.getStatus() == PaymentSubmissionStatus.MATCHED
                                    || s.getStatus() == PaymentSubmissionStatus.ADJUSTED)
                            .findFirst()
                            .orElse(submissions.get(0));
                }

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

                BigDecimal codAmount;
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

            // Lấy tất cả batch PENDING/FAILED cũ của shop
            List<SettlementBatch> oldDebtBatches = batchRepository.findByShopAndStatusInOrderByCreatedAtAsc(
                    shop,
                    List.of(
                            SettlementStatus.PENDING,
                            SettlementStatus.FAILED
                    ));

            // Tính tổng nợ cũ còn lại (đã trừ paidAmount)
            BigDecimal totalOldDebt = oldDebtBatches.stream()
                    .map(b -> b.getBalanceAmount().abs().subtract(b.getPaidAmount()))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            // Net = batch mới + khấu trừ nợ cũ
            BigDecimal net = totalCOD.subtract(totalOldDebt);

            // Nếu tổng COD > 0 thì tạo transaction giả lập SYSTEM -> SHOP
            SettlementTransaction transaction = null;
            if (net.compareTo(BigDecimal.ZERO) > 0) {
                // Hệ thống chuyển tiền cho shop phần chênh lệch
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

                // Batch mới COMPLETED
                batch.setPaidAmount(totalCOD);
                batch.setStatus(SettlementStatus.COMPLETED);
                batchRepository.save(batch);

                // Mark tất cả batch nợ cũ COMPLETED vì đã khấu trừ hết
                for (SettlementBatch old : oldDebtBatches) {
                    old.setPaidAmount(old.getBalanceAmount().abs());
                    old.setStatus(SettlementStatus.COMPLETED); // ------ CHỖ NÀY SAO K PHỈ LÀ KHẤU TRỪ NHỈ
                    batchRepository.save(old);

                    // Cập nhật order của batch cũ → PAID
                    updateOrdersCompleted(old.getOrders());
                }

                // Cập nhật order của batch mới → PAID
                updateOrdersCompleted(orders);
            } else if (net.compareTo(BigDecimal.ZERO) < 0) {
                // Shop vẫn còn nợ sau khi khấu trừ
                // Batch mới dùng để khấu trừ nợ cũ trước
                BigDecimal remaining = totalCOD; // phần batch mới dùng để khấu trừ
                for (SettlementBatch old : oldDebtBatches) {
                    if (remaining.compareTo(BigDecimal.ZERO) <= 0) break;

                    BigDecimal oldRemain = old.getBalanceAmount().abs().subtract(old.getPaidAmount());
                    if (oldRemain.compareTo(BigDecimal.ZERO) <= 0) continue;

                    if (remaining.compareTo(oldRemain) >= 0) {
                        // Khấu trừ hết batch này
                        old.setPaidAmount(old.getBalanceAmount().abs());
                        old.setStatus(SettlementStatus.COMPLETED);
                        remaining = remaining.subtract(oldRemain);

                        // Cập nhật order của batch cũ → PAID
                        updateOrdersCompleted(old.getOrders());
                    } else {
                        // Khấu trừ 1 phần
                        old.setPaidAmount(old.getPaidAmount()
                                .add(remaining));
                        remaining = BigDecimal.ZERO;
                    }
                    batchRepository.save(old);
                }
                // Batch mới PENDING — shop còn nợ |net|
                batch.setPaidAmount(BigDecimal.ZERO);
                batch.setStatus(SettlementStatus.PENDING);
                batchRepository.save(batch);
            } else {
                batch.setPaidAmount(totalCOD);
                batch.setStatus(SettlementStatus.COMPLETED);
                batchRepository.save(batch);

                for (SettlementBatch old : oldDebtBatches) {
                    old.setPaidAmount(old.getBalanceAmount().abs());
                    old.setStatus(SettlementStatus.COMPLETED);
                    batchRepository.save(old);
                    updateOrdersCompleted(old.getOrders());
                }

                updateOrdersCompleted(orders);
            }

            String notifMessage = net.compareTo(BigDecimal.ZERO) > 0
                    ? String.format(
                            "Phiên đối soát #%s đã hoàn thành. Hệ thống đã chuyển %s₫ vào tài khoản của bạn.",
                            batch.getCode(),
                            net.toPlainString())
                    : net.compareTo(BigDecimal.ZERO) < 0
                            ? String.format(
                                    "Phiên đối soát #%s đã được tạo. Bạn còn nợ %s₫ sau khi khấu trừ. Vui lòng thanh toán sớm.",
                                    batch.getCode(),
                                    net.abs().toPlainString())
                            : String.format(
                                    "Phiên đối soát #%s đã được bù trừ hoàn toàn.",
                                    batch.getCode());

            notificationService.create(
                    "Phiên đối soát theo lịch đã được tạo",
                    notifMessage,
                    "settlement_batch",
                    shop.getId(),
                    null,
                    "settlements",
                    batch.getId().toString());

            System.out.println("Settlement batch for shop " + shop.getId());
        }

        System.out.println("Finished creating automatic settlement batch.");
    }

    private void updateOrdersCompleted(List<Order> orders) {
        if (orders == null) return;
        LocalDateTime now = LocalDateTime.now();
        for (Order order : orders) {
            order.setCodStatus(OrderCodStatus.TRANSFERRED);
            if (order.getPaymentStatus() != OrderPaymentStatus.PAID) {
                order.setPaymentStatus(OrderPaymentStatus.PAID);
                order.setPaidAt(now);
            }
            orderRepository.save(order);
        }
    }
}