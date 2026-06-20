package com.logistics.scheduler.settlementtransaction;

import com.logistics.config.properties.PaymentProperties;
import com.logistics.entity.SettlementTransaction;
import com.logistics.enums.SettlementTransactionStatus;
import com.logistics.enums.SettlementTransactionType;
import com.logistics.repository.SettlementTransactionRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class SettlementTransactionExpireScheduler {

    private final SettlementTransactionRepository transactionRepository;
    private final PaymentProperties paymentProperties;

    // Chạy mỗi 5 phút
    @Scheduled(cron = "0 */5 * * * ?")
    @Transactional
    public void expireStaleTransactions() {
        log.info("Scanning stale transactions: " + LocalDateTime.now());

        // Transaction PENDING quá n phút → FAILED
        LocalDateTime expiredBefore = LocalDateTime.now().minusMinutes(paymentProperties.getExpireMinutes());

        List<SettlementTransaction> stale = transactionRepository
                .findByStatusAndTypeAndCreatedAtBefore(
                        SettlementTransactionStatus.PENDING,
                        SettlementTransactionType.SHOP_TO_SYSTEM,
                        expiredBefore);

        if (stale.isEmpty()) {
            log.info("No stale transactions found.");
            return;
        }

        stale.forEach(t -> {
            t.setStatus(SettlementTransactionStatus.FAILED);
            log.info("Expired transaction: " + t.getCode());
        });

        transactionRepository.saveAll(stale);

        log.info("Expired " + stale.size() + " transactions.");
    }
}
