package com.logistics.service.user;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.logistics.constants.PaymentConstant;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.logistics.entity.SettlementBatch;
import com.logistics.entity.SettlementTransaction;
import com.logistics.entity.User;
import com.logistics.enums.OrderPaymentStatus;
import com.logistics.enums.SettlementStatus;
import com.logistics.enums.SettlementTransactionStatus;
import com.logistics.enums.SettlementTransactionType;
import com.logistics.repository.OrderRepository;
import com.logistics.repository.SettlementBatchRepository;
import com.logistics.repository.SettlementTransactionRepository;
import com.logistics.repository.UserRepository;
import com.logistics.request.user.payment.UserPaymentCheck;
import com.logistics.response.ApiResponse;
import com.logistics.service.common.NotificationService;
import com.logistics.service.payment.VNPayService;
import com.logistics.utils.VNPayUtils;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

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

    @Value("${settlement.lock-over-hours}")
    private long lockOverHours;

    public ApiResponse<String> createVNPayURLForSettlements(
            Integer userId,
            HttpServletRequest request) {
        try {

            List<SettlementBatch> debtBatches = batchRepository.findByShopIdAndStatusIn(
                    userId,
                    List.of(SettlementStatus.PENDING, SettlementStatus.FAILED));

            String vnp_IpAddr = VNPayUtils.getClientIp(request);

            if (debtBatches.isEmpty()) {
                return new ApiResponse<>(false, "Không có khoản nợ nào cần thanh toán", null);
            }

            // Tính tổng còn nợ thực tế sau khấu trừ
            BigDecimal totalRemain = debtBatches.stream()
                    .map(b -> b.getBalanceAmount().abs().subtract(b.getPaidAmount()))
                    .filter(r -> r.compareTo(BigDecimal.ZERO) > 0)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            if (totalRemain.compareTo(BigDecimal.ZERO) <= 0) {
                return new ApiResponse<>(false, "Không có khoản nợ nào cần thanh toán", null);
            }

            if (totalRemain.compareTo(BigDecimal.valueOf(PaymentConstant.MIN_PAYMENT_AMOUNT)) < 0) {
                return new ApiResponse<>(false, "Tổng nợ phải từ 10.000₫ trở lên", null);
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
            String paymentUrl = vnPayService.createPaymentUrl(
                    String.join(",", transactionCodes),
                    String.join(",", settlementCodes),
                    null,
                    totalRemain,
                    vnp_IpAddr);

            return new ApiResponse<>(true, "Tạo link thanh toán thành công", paymentUrl);

        } catch (Exception e) {
            return new ApiResponse<>(false, e.getMessage(), null);
        }
    }

    @Transactional
    public ApiResponse<Boolean> handleVNPayReturn(UserPaymentCheck paymentCheck) {
        try {
            String transactionCodes = paymentCheck.getTransactionCode();
            List<String> codes = Arrays.stream(transactionCodes.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .toList();

            if (codes.isEmpty()) {
                return new ApiResponse<>(false, "Không có giao dịch để xử lý", false);
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
                return new ApiResponse<>(false, "Không tìm thấy giao dịch: " + String.join(", ", missing), false);
            }

            // Verify chữ ký 
            boolean isValidSignature = vnPayUtils.verifySignature(paymentCheck);
            if (!isValidSignature) {
                return new ApiResponse<>(false, "Chữ ký không hợp lệ", false);
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
                                .plusHours(lockOverHours)
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

            return new ApiResponse<>(isSuccess, isSuccess ? "Thanh toán thành công" : "Thanh toán thất bại",
                    isSuccess);
        } catch (Exception e) {
            return new ApiResponse<>(false, "Lỗi khi xử lý giao dịch: " + e.getMessage(), false);
        }
    }
}