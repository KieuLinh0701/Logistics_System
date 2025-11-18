package com.logistics.response;

import java.util.List;

import com.logistics.dto.PromotionDto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PromotionResponse {
    private List<PromotionDto> promotions;
    private Pagination pagination;
}