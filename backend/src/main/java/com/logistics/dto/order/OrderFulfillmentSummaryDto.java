package com.logistics.dto.order;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class OrderFulfillmentSummaryDto {
    private Long orderId;
    private String orderStatus;
    private Integer totalItems;
    private Integer deliveredItems;
    private Integer returnedItems;
    private Long expectedCOD;
    private Long collectedCOD;
    private Long returnedValue;
}
