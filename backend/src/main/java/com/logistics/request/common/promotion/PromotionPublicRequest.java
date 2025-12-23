package com.logistics.request.common.promotion;

import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class PromotionPublicRequest {
    private Integer page;
    private Integer limit;
}