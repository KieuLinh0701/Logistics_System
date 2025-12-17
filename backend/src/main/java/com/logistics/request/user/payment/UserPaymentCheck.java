package com.logistics.request.user.payment;

import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class UserPaymentCheck {
    private String transactionCode; // vnp_TxnRef - mã giao dịch của bạn
    private String responseCode; // vnp_ResponseCode - kết quả thanh toán
    private String referenceCode; // vnp_TransactionNo - mã giao dịch VNPay
    private String secureHash; // vnp_SecureHash - chữ ký

    private String amount; // vnp_Amount - số tiền
    private String bankCode; // vnp_BankCode - mã ngân hàng
    private String bankTranNo; // vnp_BankTranNo - mã giao dịch ngân hàng
    private String cardType; // vnp_CardType - loại thẻ
    private String orderInfo; // vnp_OrderInfo - thông tin đơn hàng
    private String payDate; // vnp_PayDate - ngày thanh toán
    private String tmnCode; // vnp_TmnCode - mã terminal
    private String transactionStatus; // vnp_TransactionStatus - trạng thái giao dịch
    private String secureHashType; // vnp_SecureHashType - loại hash (SHA256)
}
