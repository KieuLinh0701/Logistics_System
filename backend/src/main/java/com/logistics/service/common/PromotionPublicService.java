package com.logistics.service.common;

import com.logistics.dto.common.PublicPromotionDto;
import com.logistics.entity.Promotion;
import com.logistics.mapper.PromotionMapper;
import com.logistics.repository.PromotionRepository;
import com.logistics.request.common.promotion.PromotionPublicRequest;
import com.logistics.response.ListResponse;
import com.logistics.response.Pagination;
import com.logistics.specification.PromotionSpecification;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PromotionPublicService {

    private final PromotionRepository repository;

    public ListResponse<PublicPromotionDto> getActivePromotions(PromotionPublicRequest request) {
            int page = request.getPage() != null && request.getPage() > 0 ? request.getPage() - 1 : 0;
            int limit = request.getLimit() != null && request.getLimit() > 0 ? request.getLimit() : 5;

            Pageable pageable = PageRequest.of(page, limit, Sort.by("startDate").descending());

            Page<Promotion> promotionsPage = repository.findAll(PromotionSpecification.activeAndUsable(), pageable);

            List<PublicPromotionDto> promotionDtos = promotionsPage.getContent().stream()
                    .map(PromotionMapper::toPublicPromotionDto)
                    .toList();

            Pagination pagination = new Pagination(
                    (int) promotionsPage.getTotalElements(),
                    page + 1,
                    limit,
                    promotionsPage.getTotalPages());

            ListResponse<PublicPromotionDto> response = new ListResponse<PublicPromotionDto>(promotionDtos, pagination);

            return response;
    }
}