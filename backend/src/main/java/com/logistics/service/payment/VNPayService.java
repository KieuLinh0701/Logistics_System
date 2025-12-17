package com.logistics.service.payment;

import com.logistics.config.VNPayConfig;
import com.logistics.utils.VNPayUtils;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;

@Service
@RequiredArgsConstructor
public class VNPayService {

    private final VNPayConfig config;
    private final VNPayUtils vnPayUtil;

    public String createPaymentUrl(String transactionCode, 
    String settlementCode, Integer settlementId,
    BigDecimal amount, String ip) {
        try {
            String vnp_Version = "2.1.0";
            String vnp_Command = "pay";
            String vnp_OrderType = "other";

            long vnpAmount = amount.multiply(BigDecimal.valueOf(100)).longValue();
            String vnp_TxnRef = transactionCode;
            String vnp_IpAddr = ip; 

            Map<String, String> vnp_Params = new HashMap<>();
            vnp_Params.put("vnp_Version", vnp_Version);
            vnp_Params.put("vnp_Command", vnp_Command);
            vnp_Params.put("vnp_TmnCode", config.getVnp_TmnCode());
            vnp_Params.put("vnp_Amount", String.valueOf(vnpAmount));
            vnp_Params.put("vnp_CurrCode", "VND");
            vnp_Params.put("vnp_TxnRef", vnp_TxnRef);
            vnp_Params.put("vnp_OrderInfo", "Thanh toan phien doi soat: " + settlementCode);
            vnp_Params.put("vnp_OrderType", vnp_OrderType);
            vnp_Params.put("vnp_Locale", "vn");
            vnp_Params.put("vnp_ReturnUrl", config.getVnp_ReturnUrl());
            vnp_Params.put("vnp_IpAddr", vnp_IpAddr);
 
            Calendar cld = Calendar.getInstance(TimeZone.getTimeZone("Etc/GMT+7"));
            SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmss");

            vnp_Params.put("vnp_CreateDate", formatter.format(cld.getTime()));
            cld.add(Calendar.MINUTE, 15);
            vnp_Params.put("vnp_ExpireDate", formatter.format(cld.getTime()));

            List<String> fieldNames = new ArrayList<>(vnp_Params.keySet());
            Collections.sort(fieldNames); 

            StringBuilder hashData = new StringBuilder();
            StringBuilder query = new StringBuilder();

            for (int i = 0; i < fieldNames.size(); i++) {
                String name = fieldNames.get(i);
                String value = vnp_Params.get(name);

                if (value != null && !value.isEmpty()) {
                    hashData.append(name).append('=')
                            .append(URLEncoder.encode(value, StandardCharsets.US_ASCII));

                    query.append(URLEncoder.encode(name, StandardCharsets.US_ASCII))
                            .append('=')
                            .append(URLEncoder.encode(value, StandardCharsets.US_ASCII));

                    if (i < fieldNames.size() - 1) {
                        hashData.append('&');
                        query.append('&');
                    }
                }
            }

            String secureHash = vnPayUtil.hmacSHA512(
                    config.getSecretKey(),
                    hashData.toString()
            );

            return config.getVnp_PayUrl()
                    + "?"
                    + query
                    + "&vnp_SecureHash="
                    + secureHash;

        } catch (Exception e) {
            throw new RuntimeException("Không tạo được link VNPay", e);
        }
    }
}