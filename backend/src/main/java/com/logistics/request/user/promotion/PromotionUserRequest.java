package com.logistics.request.user.promotion;

import java.math.BigDecimal;

import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class PromotionUserRequest {
    private Integer page; 
    private Integer limit;
    private String search;
    private Integer serviceFee;
    private BigDecimal weight;
    private Integer serviceTypeId;   
}