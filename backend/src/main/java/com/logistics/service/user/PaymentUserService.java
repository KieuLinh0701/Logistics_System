package com.logistics.service.user;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.logistics.entity.SettlementBatch;
import com.logistics.entity.SettlementTransaction;
import com.logistics.enums.SettlementStatus;
import com.logistics.enums.SettlementTransactionStatus;
import com.logistics.enums.SettlementTransactionType;
import com.logistics.repository.OrderRepository;
import com.logistics.repository.SettlementBatchRepository;
import com.logistics.repository.SettlementTransactionRepository;
import com.logistics.request.user.payment.UserPaymentCheck;
import com.logistics.request.user.payment.UserPaymentRequest;
import com.logistics.response.ApiResponse;
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

    private final VNPayService vnPayService;

    private final VNPayUtils vnPayUtils;

    public ApiResponse<String> createVNPayURL(Integer userId, UserPaymentRequest paymentRequest,
            HttpServletRequest request) {
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

            if (paymentRequest.getAmount().compareTo(remainAmount) > 0) {
                throw new RuntimeException("Số tiền thanh toán vượt quá số tiền cần trả");
            }

            SettlementTransaction transaction = new SettlementTransaction();
            transaction.setStatus(SettlementTransactionStatus.PENDING);
            transaction.setSettlementBatch(settlement);
            transaction.setAmount(paymentRequest.getAmount());
            transaction.setType(SettlementTransactionType.SHOP_TO_SYSTEM);
            transaction = transactionRepository.save(transaction);

            String paymentUrl = vnPayService.createPaymentUrl(
                    transaction.getCode(),
                    settlement.getCode(),
                    settlement.getId(),
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
            System.out.println("===== DEBUG UserPaymentCheck =====");
            System.out.println("transactionCode: " + paymentCheck.getTransactionCode());
            System.out.println("responseCode: " + paymentCheck.getResponseCode());
            System.out.println("referenceCode: " + paymentCheck.getReferenceCode());
            System.out.println("secureHash: " + paymentCheck.getSecureHash());
            System.out.println("amount: " + paymentCheck.getAmount());
            System.out.println("bankCode: " + paymentCheck.getBankCode());
            System.out.println("bankTranNo: " + paymentCheck.getBankTranNo());
            System.out.println("cardType: " + paymentCheck.getCardType());
            System.out.println("orderInfo: " + paymentCheck.getOrderInfo());
            System.out.println("payDate: " + paymentCheck.getPayDate());
            System.out.println("tmnCode: " + paymentCheck.getTmnCode());
            System.out.println("transactionStatus: " + paymentCheck.getTransactionStatus());
            System.out.println("secureHashType: " + paymentCheck.getSecureHashType());
            System.out.println("==================================");

            // 1. Lấy transaction trước
            String transactionCode = paymentCheck.getTransactionCode();
            SettlementTransaction transaction = transactionRepository
                    .findByCode(transactionCode)
                    .orElse(null);

            if (transaction == null) {
                return new ApiResponse<>(false, "Giao dịch không tồn tại", false);
            }

            // 2. Verify chữ ký
            boolean isValidSignature = vnPayUtils.verifySignature(paymentCheck);

            boolean isSuccess = "00".equals(paymentCheck.getResponseCode()) && isValidSignature;

            // 3. Cập nhật transaction
            transaction.setStatus(isSuccess
                    ? SettlementTransactionStatus.SUCCESS
                    : SettlementTransactionStatus.FAILED);
            transaction.setPaidAt(LocalDateTime.now());
            transaction.setReferenceCode(paymentCheck.getReferenceCode());
            transactionRepository.save(transaction);

            // 4. Cập nhật settlement batch
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
                // Nếu chữ ký không hợp lệ hoặc giao dịch thất bại, check tổng thành công
                settlement.setStatus(totalPaid.compareTo(BigDecimal.ZERO) > 0
                        ? SettlementStatus.PARTIAL
                        : SettlementStatus.FAILED);
            }
            batchRepository.save(settlement);

            // 5. Trả về kết quả
            if (!isValidSignature) {
                return new ApiResponse<>(false, "Chữ ký không hợp lệ", false);
            }
            return new ApiResponse<>(true, isSuccess ? "Thanh toán thành công" : "Thanh toán thất bại", isSuccess);

        } catch (Exception e) {
            return new ApiResponse<>(false, "Lỗi khi xử lý giao dịch: " + e.getMessage(), false);
        }
    }

}