package com.logistics.service.user;

import com.logistics.config.properties.PaymentProperties;
import com.logistics.config.properties.SettlementProperties;
import com.logistics.entity.SettlementBatch;
import com.logistics.entity.SettlementTransaction;
import com.logistics.entity.User;
import com.logistics.enums.OrderPaymentStatus;
import com.logistics.enums.SettlementStatus;
import com.logistics.enums.SettlementTransactionStatus;
import com.logistics.enums.SettlementTransactionType;
import com.logistics.exception.AppException;
import com.logistics.exception.enums.SettlementBatchErrorCode;
import com.logistics.exception.enums.SettlementTransactionErrorCode;
import com.logistics.repository.OrderRepository;
import com.logistics.repository.SettlementBatchRepository;
import com.logistics.repository.SettlementTransactionRepository;
import com.logistics.repository.UserRepository;
import com.logistics.request.user.payment.UserPaymentCheck;
import com.logistics.service.common.NotificationService;
import com.logistics.service.payment.VNPayService;
import com.logistics.utils.VNPayUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PaymentUserService {

    private final SettlementBatchRepository batchRepository;

    private final SettlementTransactionRepository transactionRepository;

    private final OrderRepository orderRepository;

    private final UserRepository userRepository;

    private final VNPayService vnPayService;

    private final NotificationService notificationService;

    private final VNPayUtils vnPayUtils;

    private final UserUserService userService;

    private final SettlementProperties settlementProperties;

    private final PaymentProperties paymentProperties;

    public String createVNPayURLForSettlements(
            Integer userId,
            HttpServletRequest request) {
        Integer shopId = userService.getShopId(userId);

        List<SettlementBatch> debtBatches = batchRepository.findByShopIdAndStatusIn(
                shopId,
                List.of(SettlementStatus.PENDING, SettlementStatus.FAILED));

        String vnp_IpAddr = VNPayUtils.getClientIp(request);

        if (debtBatches.isEmpty()) {
            throw new AppException(SettlementBatchErrorCode.SETTLEMENT_NO_PENDING_DEBT);
        }

        // Tính tổng còn nợ thực tế sau khấu trừ
        BigDecimal totalRemain = debtBatches.stream()
                .map(b -> b.getBalanceAmount().abs().subtract(b.getPaidAmount()))
                .filter(r -> r.compareTo(BigDecimal.ZERO) > 0)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (totalRemain.compareTo(BigDecimal.ZERO) <= 0) {
            throw new AppException(SettlementBatchErrorCode.SETTLEMENT_NO_PENDING_DEBT);
        }

        long minPaymentAmount = paymentProperties.getMinAmount();
        if (totalRemain.compareTo(BigDecimal.valueOf(minPaymentAmount)) < 0) {
            String formattedAmount = String.format("%,d", minPaymentAmount);
            throw new AppException(SettlementBatchErrorCode.SETTLEMENT_MIN_PAYMENT_REQUIRED, formattedAmount);
        }

        // List để lưu transaction code và settlement code
        List<String> transactionCodes = new ArrayList<>();
        List<String> settlementCodes = new ArrayList<>();

        // Tạo transaction cho từng settlement
        for (SettlementBatch batch : debtBatches) {
            BigDecimal remain = batch.getBalanceAmount().abs().subtract(batch.getPaidAmount());
            if (remain.compareTo(BigDecimal.ZERO) <= 0) continue;

            SettlementTransaction transaction = new SettlementTransaction();
            transaction.setStatus(SettlementTransactionStatus.PENDING);
            transaction.setSettlementBatch(batch);
            transaction.setAmount(remain);
            transaction.setType(SettlementTransactionType.SHOP_TO_SYSTEM);
            transaction = transactionRepository.save(transaction);

            transactionCodes.add(transaction.getCode());
            settlementCodes.add(batch.getCode());
        }

        // Tạo link VNPay
        return vnPayService.createPaymentUrl(
                String.join(",", transactionCodes),
                String.join(",", settlementCodes),
                null,
                totalRemain,
                vnp_IpAddr);
    }

    @Transactional
    public boolean handleVNPayReturn(UserPaymentCheck paymentCheck) {
        String transactionCodes = paymentCheck.getTransactionCode();
        List<String> codes = Arrays.stream(transactionCodes.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();

        if (codes.isEmpty()) {
            throw new AppException(SettlementTransactionErrorCode.SETTLEMENT_TRANSACTION_PROCESSING_ERROR);
        }

        // Lấy tất cả transaction cùng lúc
        List<SettlementTransaction> transactions = transactionRepository.findAllByCodeIn(codes);

        // Nếu thiếu transaction nào thì báo lỗi
        if (transactions.size() != codes.size()) {
            List<String> foundCodes = transactions.stream()
                    .map(SettlementTransaction::getCode)
                    .toList();
            List<String> missing = codes.stream()
                    .filter(c -> !foundCodes.contains(c))
                    .toList();
            throw new AppException(SettlementTransactionErrorCode.SETTLEMENT_TRANSACTION_NOT_FOUND, String.join(", ", missing), false);
        }

        // Verify chữ ký
        boolean isValidSignature = vnPayUtils.verifySignature(paymentCheck);
        if (!isValidSignature) {
            throw new AppException(SettlementTransactionErrorCode.SETTLEMENT_TRANSACTION_INVALID_SIGNATURE);
        }

        boolean isSuccess = "00".equals(paymentCheck.getResponseCode());
        LocalDateTime now = LocalDateTime.now();

        User shop = transactions.get(0).getSettlementBatch().getShop();

        for (SettlementTransaction transaction : transactions) {
            transaction.setStatus(isSuccess
                    ? SettlementTransactionStatus.SUCCESS
                    : SettlementTransactionStatus.FAILED);
            transaction.setPaidAt(now);
            transaction.setReferenceCode(paymentCheck.getReferenceCode());
            transactionRepository.save(transaction);

            SettlementBatch batch = transaction.getSettlementBatch();

            if (isSuccess) {
                // Cập nhật paidAmount
                batch.setPaidAmount(batch.getPaidAmount().add(transaction.getAmount()));

                // Kiểm tra đã trả hết chưa
                BigDecimal remain = batch.getBalanceAmount().abs().subtract(batch.getPaidAmount());
                if (remain.compareTo(BigDecimal.ZERO) <= 0) {
                    batch.setStatus(SettlementStatus.COMPLETED);

                    // Cập nhật order → PAID
                    if (batch.getOrders() != null) {
                        batch.getOrders().forEach(order -> {
                            order.setCodStatus(com.logistics.enums.OrderCodStatus.TRANSFERRED);
                            if (order.getPaymentStatus() != OrderPaymentStatus.PAID) {
                                order.setPaymentStatus(OrderPaymentStatus.PAID);
                                order.setPaidAt(now);
                            }
                        });
                        orderRepository.saveAll(batch.getOrders());
                    }
                }
                // remain > 0 vẫn PENDING — không đổi status
            } else {
                batch.setStatus(SettlementStatus.FAILED);
            }

            batchRepository.save(batch);
        }

        // Xử lý mở khóa shop SAU KHI xử lý hết tất cả batch
        if (isSuccess && shop.getLocked()) {
            List<SettlementBatch> stillPending = batchRepository.findByShopAndStatusInOrderByCreatedAtAsc(
                    shop,
                    List.of(SettlementStatus.PENDING, SettlementStatus.FAILED));

            boolean hasOverHours = stillPending.stream()
                    .anyMatch(b -> b.getCreatedAt()
                            .plusHours(settlementProperties.getLockOverHours())
                            .isBefore(now));

            if (!hasOverHours) {
                shop.setLocked(false);
                userRepository.save(shop);

                notificationService.create(
                        "Tài khoản đã được mở khóa",
                        "Tất cả phiên đối soát đã được thanh toán. Tài khoản của bạn đã được mở khóa.",
                        "settlement_unlocked",
                        shop.getId(),
                        null,
                        "settlements",
                        null);
            }
        }

        return isSuccess;
    }
}