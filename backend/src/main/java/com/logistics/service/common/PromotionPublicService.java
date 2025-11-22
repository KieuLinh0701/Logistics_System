package com.logistics.service.common;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import com.logistics.dto.PromotionDto;
import com.logistics.entity.Promotion;
import com.logistics.mapper.PromotionMapper;
import com.logistics.repository.PromotionRepository;
import com.logistics.request.common.promotion.PromotionPublicRequest;
import com.logistics.response.ApiResponse;
import com.logistics.response.ListResponse;
import com.logistics.response.Pagination;
import com.logistics.specification.PromotionSpecification;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PromotionPublicService {

    private final PromotionRepository repository;

    public ApiResponse<ListResponse<PromotionDto>> getActivePromotions(PromotionPublicRequest request) {
        try {
            int page = request.getPage() != null && request.getPage() > 0 ? request.getPage() - 1 : 0;
            int limit = request.getLimit() != null && request.getLimit() > 0 ? request.getLimit() : 5;

            Pageable pageable = PageRequest.of(page, limit, Sort.by("startDate").descending());

            Page<Promotion> promotionsPage = repository.findAll(PromotionSpecification.activeAndUsable(), pageable);

            List<PromotionDto> promotionDtos = promotionsPage.getContent().stream()
                    .map(PromotionMapper::toDto)
                    .toList();

            Pagination pagination = new Pagination(
                    (int) promotionsPage.getTotalElements(),
                    page + 1,
                    limit,
                    promotionsPage.getTotalPages());

            ListResponse<PromotionDto> response = new ListResponse<PromotionDto>(promotionDtos, pagination);

            return new ApiResponse<>(true, "Lấy danh sách khuyến mãi còn hiệu lực thành công", response);

        } catch (Exception e) {
            return new ApiResponse<>(false, "Lỗi khi lấy danh sách khuyến mãi: " + e.getMessage(), null);
        }
    }
}