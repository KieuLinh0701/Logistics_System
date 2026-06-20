package com.logistics.request.user.promotion;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

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