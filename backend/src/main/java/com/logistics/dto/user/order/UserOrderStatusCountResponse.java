package com.logistics.dto.user.order;

import com.logistics.dto.AddressSummaryDto;
import com.logistics.enums.OrderStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class UserOrderStatusCountResponse {
    private String status;
    private long count;
}
