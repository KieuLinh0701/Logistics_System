export interface PaymentRequest {
    settlementId: number;
    amount: number;
}

export interface PaymentCheck {
    transactionCode: string;       // vnp_TxnRef - mã giao dịch của bạn
    responseCode: string;          // vnp_ResponseCode - kết quả thanh toán
    referenceCode: string;         // vnp_TransactionNo - mã giao dịch VNPay
    secureHash: string;            // vnp_SecureHash - chữ ký
    amount?: string;               // vnp_Amount - số tiền
    bankCode?: string;             // vnp_BankCode - mã ngân hàng
    bankTranNo?: string;           // vnp_BankTranNo - mã giao dịch ngân hàng
    cardType?: string;             // vnp_CardType - loại thẻ
    orderInfo?: string;            // vnp_OrderInfo - thông tin đơn hàng
    payDate?: string;              // vnp_PayDate - ngày thanh toán
    tmnCode?: string;              // vnp_TmnCode - mã terminal
    transactionStatus?: string;    // vnp_TransactionStatus - trạng thái giao dịch
    secureHashType?: string;       // vnp_SecureHashType - loại hash (SHA256)
}

export interface PaymentsRequest {
    settlementIds: string;
    amount: number;
}