package com.logistics.request.promotion;

import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class PublicPromotionRequest {
    private Integer page;
    private Integer limit;
}