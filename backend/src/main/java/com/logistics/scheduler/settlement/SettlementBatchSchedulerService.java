package com.logistics.scheduler.settlement;

import com.logistics.entity.*;
import com.logistics.enums.*;
import com.logistics.repository.*;
import com.logistics.service.common.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class SettlementBatchSchedulerService {

    private final OrderRepository orderRepository;
    private final SettlementBatchRepository batchRepository;
    private final SettlementTransactionRepository transactionRepository;
    private final BankAccountRepository bankAccountRepository;
    private final UserSettlementScheduleRepository scheduleRepository;
    private final NotificationService notificationService;
    private final UserRepository userRepository;

     @Scheduled(cron = "0 * * * * ?")
//    @Scheduled(cron = "0 0 20 * * ?") // 20:00 mỗi ngày
    @Transactional
    public void createDailySettlementBatch() {
        log.info("Start creating automatic settlement batch: " + LocalDateTime.now());

        // Lấy tất cả user có lịch đối soát hôm nay
        DayOfWeek today = LocalDate.now().getDayOfWeek();
        WeekDay weekDay = WeekDay.valueOf(today.name());
        List<UserSettlementSchedule> schedulesToday = scheduleRepository.findAllWithScheduleToday(weekDay);

        for (UserSettlementSchedule schedule : schedulesToday) {
            User shop = schedule.getUser();

            // Lấy các đơn hàng DELIVERED / RETURNED / RETURN_FAILED_FINAL mà chưa có settlementBatch
            List<Order> orders = orderRepository.findByUserAndSettlementBatchIsNullAndStatusIn(
                    shop,
                    List.of(
                            OrderStatus.DELIVERED,
                            OrderStatus.RETURNED,
                            OrderStatus.RETURN_FAILED_FINAL
                    ));

            if (orders.isEmpty())
                continue;

            BigDecimal totalCOD = BigDecimal.ZERO;
            List<Order> attachedOrders = new java.util.ArrayList<>();

            for (Order order : orders) {
                List<PaymentSubmission> submissions = order.getPaymentSubmissions();
                PaymentSubmission ps = null;
                if (submissions != null && !submissions.isEmpty()) {
                    ps = submissions.stream()
                            .filter(s -> s.getStatus() == PaymentSubmissionStatus.MATCHED
                                    || s.getStatus() == PaymentSubmissionStatus.ADJUSTED)
                            .findFirst()
                            .orElse(submissions.getFirst());
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
                if ((order.getStatus() == OrderStatus.RETURNED || order.getStatus() == OrderStatus.RETURN_FAILED_FINAL) &&
                        order.getPaymentStatus() == OrderPaymentStatus.UNPAID) {
                    codAmount = BigDecimal.valueOf(-order.getTotalFee());
                } else if ((order.getStatus() == OrderStatus.RETURNED || order.getStatus() == OrderStatus.RETURN_FAILED_FINAL) &&
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
                attachedOrders.add(order);
            }

            if (attachedOrders.isEmpty())
                continue;

            // Tạo settlement batch
            SettlementBatch batch = new SettlementBatch();
            batch.setShop(shop);
            batch.setStatus(SettlementStatus.PENDING);
            batch.setBalanceAmount(totalCOD);
            batchRepository.save(batch);

            for (Order order : attachedOrders) {
                order.setSettlementBatch(batch);
                orderRepository.save(order);
            }

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
                transaction.setAmount(net);
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
                    old.setStatus(SettlementStatus.COMPLETED);
                    batchRepository.save(old);

                    // Cập nhật order của batch cũ → PAID
                    updateOrdersCompleted(old.getOrders());
                }

                if (totalOldDebt.compareTo(BigDecimal.ZERO) > 0) {
                    createOffsetTransaction(batch, totalOldDebt);
                }

                // Cập nhật order của batch mới → PAID
                updateOrdersCompleted(attachedOrders);
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
                BigDecimal consumed = totalCOD.subtract(remaining);
                // Batch mới PENDING — shop còn nợ |net|
                batch.setPaidAmount(consumed);
                if (totalCOD.compareTo(BigDecimal.ZERO) > 0) {
                    // Batch này tự nó là khoản credit, đã dùng hết để khấu trừ nợ cũ → coi như resolved
                    batch.setStatus(SettlementStatus.COMPLETED);
                    updateOrdersCompleted(attachedOrders);
                } else {
                    // Batch này tự nó là khoản nợ mới (totalCOD <= 0) → giữ PENDING cho kỳ sau
                    batch.setStatus(SettlementStatus.PENDING);
                }
                batchRepository.save(batch);

                if (consumed.compareTo(BigDecimal.ZERO) > 0) {
                    createOffsetTransaction(batch, consumed);
                }
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

                updateOrdersCompleted(attachedOrders);

                if (totalCOD.compareTo(BigDecimal.ZERO) > 0) {
                    createOffsetTransaction(batch, totalCOD);
                }
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

            tryUnlockShop(shop);

            log.info("Settlement batch for shop " + shop.getId());
        }

        log.info("Finished creating automatic settlement batch.");
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

    private void tryUnlockShop(User shop) {
        if (!shop.getLocked()) return;

        // Chỉ xét các batch đã từng trigger lock
        List<SettlementBatch> lockedBatches = batchRepository.findByShopAndLockedSentTrue(shop);
        if (lockedBatches.isEmpty()) return;

        boolean allResolved = lockedBatches.stream()
                .allMatch(b -> b.getStatus() == SettlementStatus.COMPLETED);

        if (allResolved) {
            shop.setLocked(false);
            userRepository.save(shop);

            notificationService.create(
                    "Tài khoản đã được mở khóa",
                    "Các khoản nợ gây khóa tài khoản đã được xử lý. Tài khoản của bạn đã được mở khóa.",
                    "settlement_unlocked",
                    shop.getId(),
                    null,
                    "settlements",
                    null);
        }
    }

    private void createOffsetTransaction(SettlementBatch batch, BigDecimal amount) {
        if (amount.compareTo(BigDecimal.ZERO) <= 0) return;

        SettlementTransaction offsetTx = new SettlementTransaction();
        offsetTx.setSettlementBatch(batch);
        offsetTx.setAmount(amount);
        offsetTx.setType(SettlementTransactionType.OFFSET);
        offsetTx.setStatus(SettlementTransactionStatus.SUCCESS);
        offsetTx.setPaidAt(LocalDateTime.now());
        transactionRepository.save(offsetTx);
    }
}