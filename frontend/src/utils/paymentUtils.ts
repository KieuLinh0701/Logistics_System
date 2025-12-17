export interface PaymentResult {
  success: boolean; 
  message: string;  
}

export const PAYMENT_VNPAY_STATUS: Record<string, (code?: string) => PaymentResult> = {
  success: (code) => ({
    success: true,
    message: `Thanh toán thành công (Mã GD: ${code})`,
  }),
  failed: (code) => ({
    success: false,
    message: `Thanh toán thất bại (Mã GD: ${code})`,
  }),
  invalid_signature: () => ({
    success: false,
    message: "Giao dịch không hợp lệ (Sai chữ ký)",
  }),
  not_found: () => ({
    success: false,
    message: "Không tìm thấy giao dịch thanh toán",
  }),
  exception: () => ({
    success: false,
    message: "Có lỗi hệ thống khi xử lý thanh toán",
  }),
};

export const getPaymentStatus = (statusCode?: string, txnCode?: string): PaymentResult => {
  if (!statusCode) return { success: false, message: "Không xác định trạng thái" };

  const fn = PAYMENT_VNPAY_STATUS[statusCode];
  return fn ? fn(txnCode) : { success: false, message: `Trạng thái không xác định (${statusCode})` };
};