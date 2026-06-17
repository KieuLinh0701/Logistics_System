package com.logistics.scheduler.settlementtransaction;

import com.logistics.constants.PaymentConstant;
import com.logistics.enums.SettlementTransactionStatus;
import com.logistics.enums.SettlementTransactionType;
import com.logistics.repository.SettlementTransactionRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import com.logistics.entity.SettlementTransaction;

@Service
@RequiredArgsConstructor
public class SettlementTransactionExpireScheduler {

    private final SettlementTransactionRepository transactionRepository;

    // Chạy mỗi 5 phút
    @Scheduled(cron = "0 */5 * * * ?")
    @Transactional
    public void expireStaleTransactions() {
        System.out.println("Scanning stale transactions: " + LocalDateTime.now());

        // Transaction PENDING quá n phút → FAILED
        LocalDateTime expiredBefore = LocalDateTime.now().minusMinutes(PaymentConstant.TRANSACTION_EXPIRE_MINUTES);

        List<SettlementTransaction> stale = transactionRepository
                .findByStatusAndTypeAndCreatedAtBefore(
                        SettlementTransactionStatus.PENDING,
                        SettlementTransactionType.SHOP_TO_SYSTEM,
                        expiredBefore);

        if (stale.isEmpty()) {
            System.out.println("No stale transactions found.");
            return;
        }

        stale.forEach(t -> {
            t.setStatus(SettlementTransactionStatus.FAILED);
            System.out.println("Expired transaction: " + t.getCode());
        });

        transactionRepository.saveAll(stale);

        System.out.println("Expired " + stale.size() + " transactions.");
    }
}
