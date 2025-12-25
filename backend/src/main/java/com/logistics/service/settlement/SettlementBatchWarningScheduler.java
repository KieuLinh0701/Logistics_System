package com.logistics.service.settlement;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.logistics.entity.SettlementBatch;
import com.logistics.entity.User;
import com.logistics.enums.SettlementStatus;
import com.logistics.repository.SettlementBatchRepository;
import com.logistics.repository.UserRepository;
import com.logistics.service.common.NotificationService;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class SettlementBatchWarningScheduler {

        private final SettlementBatchRepository batchRepository;
        private final NotificationService notificationService;
        private final UserRepository userRepository;

        @Scheduled(cron = "0 0 20 * * ?")
        // @Scheduled(cron = "0 * * * * ?") 
        @Transactional
        public void scanUnpaidSettlementBatches() {

                LocalDateTime now = LocalDateTime.now();

                // 48h: CẢNH BÁO
                LocalDateTime warningTime = now.minusHours(48);
                // LocalDateTime warningTime = now.minusMinutes(1);

                List<SettlementBatch> warningBatches = batchRepository.findByStatusInAndCreatedAtBefore(
                                List.of(
                                                SettlementStatus.PENDING,
                                                SettlementStatus.PARTIAL,
                                                SettlementStatus.FAILED),
                                warningTime);

                for (SettlementBatch batch : warningBatches) {
                        if (!batch.isWarningSent()) {
                                User shop = batch.getShop();

                                notificationService.create(
                                                "Cảnh báo đối soát chưa thanh toán",
                                                String.format(
                                                                "Phiên đối soát %s đã quá 48 giờ nhưng chưa hoàn tất. Vui lòng thanh toán sớm để tránh bị khóa tài khoản.",
                                                                batch.getCode()),
                                                "settlement_warning",
                                                shop.getId(),
                                                null,
                                                "settlements",
                                                batch.getId().toString());

                                batch.setWarningSent(true);
                                batchRepository.save(batch);
                        }
                }

                // 72h: KHÓA TÀI KHOẢN
                LocalDateTime lockTime = now.minusHours(72);
                // LocalDateTime lockTime = now.minusMinutes(2);

                List<SettlementBatch> lockBatches = batchRepository.findByStatusInAndCreatedAtBefore(
                                List.of(
                                                SettlementStatus.PENDING,
                                                SettlementStatus.PARTIAL,
                                                SettlementStatus.FAILED),
                                lockTime);

                for (SettlementBatch batch : lockBatches) {
                        if (!batch.isLockedSent()) {
                                User shop = batch.getShop();

                                if (!shop.getLocked()) {
                                        shop.setLocked(true);
                                        userRepository.save(shop);

                                        notificationService.create(
                                                        "Tài khoản bị khóa do nợ đối soát",
                                                        String.format(
                                                                        "Phiên đối soát %s đã quá 72 giờ nhưng chưa được thanh toán. "
                                                                                        + "Tài khoản của bạn đã bị tạm khóa và không thể tạo thêm đơn hàng mới cho đến khi hoàn tất thanh toán.",
                                                                        batch.getCode()),
                                                        "settlement_locked",
                                                        shop.getId(),
                                                        null,
                                                        "settlements",
                                                        batch.getId().toString());

                                        batch.setLockedSent(true);
                                        batchRepository.save(batch);
                                }
                        }
                }
        }
}