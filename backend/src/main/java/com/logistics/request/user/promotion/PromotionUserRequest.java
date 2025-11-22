package com.logistics.request.user.promotion;

import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class PromotionUserRequest {
    private Integer page;
    private Integer limit;
    private String search;
    private String shippingFee;
}
