package com.logistics.scheduler.settlement;

import com.logistics.config.properties.PaymentProperties;
import com.logistics.config.properties.SettlementProperties;
import com.logistics.entity.SettlementBatch;
import com.logistics.entity.User;
import com.logistics.enums.SettlementStatus;
import com.logistics.repository.SettlementBatchRepository;
import com.logistics.repository.UserRepository;
import com.logistics.service.common.NotificationService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SettlementBatchWarningScheduler {

    private final SettlementBatchRepository batchRepository;
    private final NotificationService notificationService;
    private final UserRepository userRepository;
    private final SettlementProperties settlementProperties;
    private final PaymentProperties paymentProperties;

    @Scheduled(cron = "0 0 20 * * ?")
    // @Scheduled(cron = "0 * * * * ?")
    @Transactional
    public void scanUnpaidSettlementBatches() {

        tryUnlockShops();

        LocalDateTime now = LocalDateTime.now();

        // 48h: CẢNH BÁO
        LocalDateTime warningTime = now.minusHours(settlementProperties.getWarningOverHours());
        // LocalDateTime warningTime = now.minusMinutes(1);

        List<SettlementBatch> warningBatches = batchRepository.findByStatusInAndCreatedAtBefore(
                List.of(
                        SettlementStatus.PENDING,
                        SettlementStatus.FAILED),
                warningTime);

        for (SettlementBatch batch : warningBatches) {
            if (!batch.isWarningSent()) {
                User shop = batch.getShop();

                BigDecimal totalRemain = getTotalRemain(shop.getId());

                if (totalRemain.compareTo(BigDecimal.valueOf(paymentProperties.getMinAmount())) < 0) {
                    // Nợ quá nhỏ, không thể thanh toán VNPay → thông báo nhẹ, không cảnh báo
                    notificationService.create(
                            "Thông báo đối soát",
                            String.format(
                                    "Phiên đối soát %s có khoản nợ %s₫ thấp hơn mức tối thiểu thanh toán. Hệ thống sẽ tự động khấu trừ vào phiên đối soát tiếp theo.",
                                    batch.getCode(),
                                    totalRemain.toPlainString()),
                            "settlement_info",
                            shop.getId(),
                            null,
                            "settlements",
                            batch.getId()
                                    .toString());

                    batch.setWarningSent(true);
                    batchRepository.save(batch);
                    continue;
                }

                notificationService.create(
                        "Cảnh báo đối soát chưa thanh toán",
                        String.format(
                                "Phiên đối soát %s đã quá %s giờ nhưng chưa hoàn tất. "
                                        + "Vui lòng thanh toán sớm để tránh bị khóa tài khoản.",
                                batch.getCode(),
                                warningTime),
                        "settlement_warning",
                        shop.getId(),
                        null,
                        "settlements",
                        batch.getId()
                                .toString());

                batch.setWarningSent(true);
                batchRepository.save(batch);

            }
        }

        // 72h: KHÓA TÀI KHOẢN
        LocalDateTime lockTime = now.minusHours(settlementProperties.getLockOverHours());
        // LocalDateTime lockTime = now.minusMinutes(2);

        List<SettlementBatch> lockBatches = batchRepository.findByStatusInAndCreatedAtBefore(
                List.of(
                        SettlementStatus.PENDING,
                        SettlementStatus.FAILED),
                lockTime);

        for (SettlementBatch batch : lockBatches) {
            if (!batch.isLockedSent()) {
                User shop = batch.getShop();

                // Tính tổng nợ thực tế
                BigDecimal totalRemain = getTotalRemain(shop.getId());

                if (totalRemain.compareTo(BigDecimal.valueOf(paymentProperties.getMinAmount())) < 0) {
                    batch.setLockedSent(true);
                    batchRepository.save(batch);
                    continue;
                }

                if (!shop.getLocked()) {
                    shop.setLocked(true);
                    userRepository.save(shop);

                    notificationService.create(
                            "Tài khoản bị khóa do nợ đối soát",
                            String.format(
                                    "Phiên đối soát %s đã quá %s giờ nhưng chưa được thanh toán. "
                                            + "Tài khoản của bạn đã bị tạm khóa và không thể tạo thêm đơn hàng mới cho đến khi hoàn tất thanh toán.",
                                    batch.getCode(),
                                    lockTime),
                            "settlement_locked",
                            shop.getId(),
                            null,
                            "settlements",
                            batch.getId()
                                    .toString());
                }

                batch.setLockedSent(true);
                batchRepository.save(batch);
            }
        }
    }

    private BigDecimal getTotalRemain(Integer shopId) {
        List<SettlementBatch> debtBatches = batchRepository.findByShopIdAndStatusIn(
                shopId,
                List.of(SettlementStatus.PENDING, SettlementStatus.FAILED));

        return debtBatches.stream()
                .map(b -> b.getBalanceAmount()
                        .abs()
                        .subtract(b.getPaidAmount()))
                .filter(r -> r.compareTo(BigDecimal.ZERO) > 0)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private void tryUnlockShops() {
        List<User> lockedShops = userRepository.findByLockedTrue();

        for (User shop : lockedShops) {
            List<SettlementBatch> lockedBatches = batchRepository.findByShopAndLockedSentTrue(shop);
            if (lockedBatches.isEmpty()) continue;

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
    }
}