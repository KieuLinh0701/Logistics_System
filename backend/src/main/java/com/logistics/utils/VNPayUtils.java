package com.logistics.utils;

import com.logistics.config.VNPayConfig;
import com.logistics.exception.AppException;
import com.logistics.exception.enums.PaymentErrorCode;
import com.logistics.request.user.payment.UserPaymentCheck;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Component
public class VNPayUtils {

    private final VNPayConfig config;

    public VNPayUtils(VNPayConfig config) {
        this.config = config;
    }

    // --- Hash tất cả field từ Map (dùng verify chữ ký)
    public String hashAllFields(Map<String, String> fields) {
        List<String> fieldNames = new ArrayList<>(fields.keySet());
        Collections.sort(fieldNames); // sort alphabetically

        StringBuilder sb = new StringBuilder();
        Iterator<String> itr = fieldNames.iterator();

        while (itr.hasNext()) {
            String fieldName = itr.next();
            String fieldValue = fields.get(fieldName);

            if (fieldValue != null && !fieldValue.isEmpty()) {
                sb.append(fieldName)
                        .append("=")
                        .append(URLEncoder.encode(fieldValue, StandardCharsets.US_ASCII));

                if (itr.hasNext())
                    sb.append("&");
            }
        }

        return hmacSHA512(config.getSecretKey(), sb.toString());
    }

    // --- HMAC SHA512
    public String hmacSHA512(String key, String data) {
        try {
            Mac hmac512 = Mac.getInstance("HmacSHA512");
            SecretKeySpec secretKey = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA512");
            hmac512.init(secretKey);

            byte[] result = hmac512.doFinal(data.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();

            for (byte b : result) {
                sb.append(String.format("%02x", b & 0xff));
            }
            return sb.toString();
        } catch (Exception e) {
            throw new AppException(PaymentErrorCode.PAYMENT_SIGNATURE_ERROR);
        }
    }

    // --- Verify chữ ký callback từ VNPay
    public boolean verifySignature(UserPaymentCheck check) {
        Map<String, String> vnp_Params = new HashMap<>();
        vnp_Params.put("vnp_Amount", check.getAmount());
        vnp_Params.put("vnp_BankCode", check.getBankCode());
        vnp_Params.put("vnp_BankTranNo", check.getBankTranNo());
        vnp_Params.put("vnp_CardType", check.getCardType());
        vnp_Params.put("vnp_OrderInfo", check.getOrderInfo());
        vnp_Params.put("vnp_PayDate", check.getPayDate());
        vnp_Params.put("vnp_ResponseCode", check.getResponseCode());
        vnp_Params.put("vnp_TmnCode", check.getTmnCode());
        vnp_Params.put("vnp_TransactionNo", check.getReferenceCode());
        vnp_Params.put("vnp_TransactionStatus", check.getTransactionStatus());
        vnp_Params.put("vnp_TxnRef", check.getTransactionCode());

        // --- tính hash từ map đúng
        String calculatedHash = hashAllFields(vnp_Params);

        // --- so sánh chữ ký trả về
        return calculatedHash.equalsIgnoreCase(check.getSecureHash());
    }

    public static String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");

        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_CLIENT_IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_X_FORWARDED_FOR");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }

        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }

        return ip;
    }
}