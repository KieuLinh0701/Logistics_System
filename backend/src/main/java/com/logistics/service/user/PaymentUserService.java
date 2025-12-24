package com.logistics.service.user;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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
import com.logistics.request.user.payment.UserPaymentRequest;
import com.logistics.request.user.payment.UserPaymentsRequest;
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

    private static final BigDecimal MIN_PAYMENT_AMOUNT = new BigDecimal("10000");

    public ApiResponse<String> createVNPayURL(Integer userId, UserPaymentRequest paymentRequest,
            HttpServletRequest request, boolean isDetail) {
        try {
            System.out.println("id" + paymentRequest.getSettlementId());
            System.out.println("amount" + paymentRequest.getAmount());
            String vnp_IpAddr = VNPayUtils.getClientIp(request);

            SettlementBatch settlement = batchRepository
                    .findByIdAndShop_Id(paymentRequest.getSettlementId(), userId)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy phiên đối soát"));
            ;

            if (settlement.getStatus() == SettlementStatus.COMPLETED) {
                throw new RuntimeException("Phiên đối soát đã hoàn thành");
            }

            BigDecimal balance = settlement.getBalanceAmount();
            if (balance.compareTo(BigDecimal.ZERO) >= 0) {
                throw new RuntimeException("Phiên đối soát không có khoản cần thanh toán");
            }

            BigDecimal paidAmount = transactionRepository.sumAmountByBatchAndStatus(
                    settlement.getId(),
                    SettlementTransactionStatus.SUCCESS);

            BigDecimal remainAmount = balance.abs().subtract(paidAmount);

            if (remainAmount.compareTo(BigDecimal.ZERO) <= 0) {
                throw new RuntimeException("Phiên đối soát đã được thanh toán đủ");
            }

            validatePartialPayment(remainAmount, paymentRequest.getAmount());

            SettlementTransaction transaction = new SettlementTransaction();
            transaction.setStatus(SettlementTransactionStatus.PENDING);
            transaction.setSettlementBatch(settlement);
            transaction.setAmount(paymentRequest.getAmount());
            transaction.setType(SettlementTransactionType.SHOP_TO_SYSTEM);
            transaction = transactionRepository.save(transaction);

            String paymentUrl = vnPayService.createPaymentUrl(
                    transaction.getCode(),
                    settlement.getCode(),
                    isDetail ? settlement.getId() : null,
                    paymentRequest.getAmount(),
                    vnp_IpAddr);

            return new ApiResponse<>(true, "Tạo link thanh toán thành công", paymentUrl);

        } catch (Exception e) {
            return new ApiResponse<>(false, e.getMessage(), null);
        }
    }

    public ApiResponse<String> createVNPayURLForSettlements(Integer userId, UserPaymentsRequest paymentRequest,
            HttpServletRequest request) {
        try {
            String settlementIdStr = paymentRequest.getSettlementIds();
            List<Integer> settlementIds = Arrays.stream(settlementIdStr.split(","))
                    .map(Integer::parseInt)
                    .collect(Collectors.toList());

            String vnp_IpAddr = VNPayUtils.getClientIp(request);

            // Lấy tất cả settlements theo list id và user
            List<SettlementBatch> settlements = batchRepository.findAllByIdInAndShop_Id(settlementIds, userId);
            if (settlements.size() != settlementIds.size()) {
                throw new RuntimeException("Không tìm thấy một số phiên đối soát");
            }

            // Tính tổng phần còn lại của tất cả settlement
            BigDecimal totalRemainAmount = BigDecimal.ZERO;
            Map<Integer, BigDecimal> remainAmounts = new HashMap<>();
            for (SettlementBatch settlement : settlements) {
                if (settlement.getStatus() == SettlementStatus.COMPLETED) {
                    throw new RuntimeException("Một trong các phiên đối soát đã hoàn thành");
                }

                BigDecimal balance = settlement.getBalanceAmount();
                if (balance.compareTo(BigDecimal.ZERO) >= 0) {
                    throw new RuntimeException("Một trong các phiên đối soát không có khoản cần thanh toán");
                }

                BigDecimal paidAmount = transactionRepository.sumAmountByBatchAndStatus(
                        settlement.getId(),
                        SettlementTransactionStatus.SUCCESS);

                BigDecimal remainAmount = balance.abs().subtract(paidAmount);

                if (remainAmount.compareTo(BigDecimal.ZERO) <= 0) {
                    throw new RuntimeException("Một trong các phiên đối soát đã được thanh toán đủ");
                }

                remainAmounts.put(settlement.getId(), remainAmount);
                totalRemainAmount = totalRemainAmount.add(remainAmount);
            }

            // Kiểm tra tổng số tiền thanh toán
            if (paymentRequest.getAmount().compareTo(totalRemainAmount) != 0) {
                throw new RuntimeException(
                        "Tổng số tiền thanh toán phải bằng tổng các phần còn lại của các phiên đối soát");
            }

            // List để lưu transaction code và settlement code
            List<String> transactionCodes = new ArrayList<>();
            List<String> settlementCodes = new ArrayList<>();

            // Tạo transaction cho từng settlement
            for (SettlementBatch settlement : settlements) {
                BigDecimal remainAmount = remainAmounts.get(settlement.getId());
                validatePartialPayment(remainAmount, remainAmount); // mỗi settlement sẽ thanh toán phần còn lại

                SettlementTransaction transaction = new SettlementTransaction();
                transaction.setStatus(SettlementTransactionStatus.PENDING);
                transaction.setSettlementBatch(settlement);
                transaction.setAmount(remainAmount);
                transaction.setType(SettlementTransactionType.SHOP_TO_SYSTEM);
                transaction = transactionRepository.save(transaction);

                transactionCodes.add(transaction.getCode());
                settlementCodes.add(settlement.getCode());
            }

            // Chuyển list transaction code và settlement code thành chuỗi ngăn cách dấu
            // phẩy
            String allTransactionCodes = String.join(",", transactionCodes);
            String allSettlementCodes = String.join(",", settlementCodes);

            // Tạo link VNPay
            String paymentUrl = vnPayService.createPaymentUrl(
                    allTransactionCodes,
                    allSettlementCodes,
                    null,
                    paymentRequest.getAmount(),
                    vnp_IpAddr);

            return new ApiResponse<>(true, "Tạo link thanh toán thành công", paymentUrl);

        } catch (Exception e) {
            return new ApiResponse<>(false, e.getMessage(), null);
        }
    }

    @Transactional
    public ApiResponse<Boolean> handleVNPayReturn(Integer userId, UserPaymentCheck paymentCheck) {
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

            boolean isSuccess = "00".equals(paymentCheck.getResponseCode()) && isValidSignature;
            LocalDateTime now = LocalDateTime.now();

            for (SettlementTransaction transaction : transactions) {
                transaction.setStatus(isSuccess
                        ? SettlementTransactionStatus.SUCCESS
                        : SettlementTransactionStatus.FAILED);
                transaction.setPaidAt(now);
                transaction.setReferenceCode(paymentCheck.getReferenceCode());
                transactionRepository.save(transaction);

                SettlementBatch settlement = transaction.getSettlementBatch();
                BigDecimal totalPaid = transactionRepository.sumAmountByBatchAndStatus(
                        settlement.getId(),
                        SettlementTransactionStatus.SUCCESS);
                BigDecimal needToPay = settlement.getBalanceAmount().abs();

                if (isSuccess) {
                    settlement.setStatus(totalPaid.compareTo(needToPay) >= 0
                            ? SettlementStatus.COMPLETED
                            : SettlementStatus.PARTIAL);
                } else {
                    settlement.setStatus(totalPaid.compareTo(BigDecimal.ZERO) > 0
                            ? SettlementStatus.PARTIAL
                            : SettlementStatus.FAILED);
                }
                batchRepository.save(settlement);

                // Xử lý đơn hàng nếu batch hoàn tất
                if (settlement.getStatus() == SettlementStatus.COMPLETED && settlement.getOrders() != null) {
                    settlement.getOrders().forEach(order -> {
                        if (order.getPaymentStatus() != OrderPaymentStatus.PAID) {
                            order.setPaymentStatus(OrderPaymentStatus.PAID);
                            order.setPaidAt(now);
                        }
                    });
                    orderRepository.saveAll(settlement.getOrders());

                    // Xử lý mở khóa shop
                    User shop = settlement.getShop();
                    if (shop.getLocked()) {
                        List<SettlementBatch> unsettledBatches = batchRepository.findByShopAndStatusIn(
                                shop,
                                List.of(SettlementStatus.PENDING, SettlementStatus.PARTIAL, SettlementStatus.FAILED));
                        unsettledBatches.removeIf(b -> b.getId().equals(settlement.getId()));

                        boolean hasOtherOverHours = unsettledBatches.stream()
                                .anyMatch(b -> b.getCreatedAt().plusHours(lockOverHours).isBefore(now));

                        if (!hasOtherOverHours) {
                            shop.setLocked(false);
                            userRepository.save(shop);

                            notificationService.create(
                                    "Tài khoản đã được mở khóa",
                                    String.format(
                                            "Phiên đối soát %s đã được thanh toán thành công. " +
                                                    "Tài khoản của bạn đã được mở khóa và có thể tiếp tục tạo đơn hàng bình thường.",
                                            settlement.getCode()),
                                    "settlement_unlocked",
                                    shop.getId(),
                                    null,
                                    "settlements",
                                    settlement.getId().toString());
                        }
                    }
                }
            }

            return new ApiResponse<>(isSuccess, isSuccess ? "Thanh toán thành công" : "Thanh toán thất bại", isSuccess);

        } catch (Exception e) {
            return new ApiResponse<>(false, "Lỗi khi xử lý giao dịch: " + e.getMessage(), false);
        }
    }

    private void validatePartialPayment(BigDecimal remainAmount, BigDecimal payAmount) {

        // 1. Mỗi lần thanh toán phải >= 10.000
        if (payAmount.compareTo(MIN_PAYMENT_AMOUNT) < 0) {
            throw new RuntimeException("Số tiền thanh toán tối thiểu là 10.000 VNĐ");
        }

        // 2. Không cho trả vượt
        if (payAmount.compareTo(remainAmount) > 0) {
            throw new RuntimeException("Số tiền thanh toán vượt quá số tiền cần trả");
        }

        BigDecimal afterPay = remainAmount.subtract(payAmount);

        // 3. Không cho phép còn lại < 10.000 (trừ khi = 0)
        if (afterPay.compareTo(BigDecimal.ZERO) > 0
                && afterPay.compareTo(MIN_PAYMENT_AMOUNT) < 0) {
            throw new RuntimeException(
                    "Số tiền còn lại của phiên đối soát phải từ 10.000 VNĐ trở lên");
        }
    }

}