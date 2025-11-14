package com.logistics.service;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import com.logistics.dto.OfficeDto;
import com.logistics.entity.Office;
import com.logistics.mapper.OfficeMapper;
import com.logistics.repository.OfficeRepository;
import com.logistics.request.office.OfficeSearchRequest;
import com.logistics.response.ApiResponse;
import com.logistics.response.Pagination;
import com.logistics.specification.OfficeSpecification;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class OfficeService {

    private final OfficeRepository officeRepository;

    public ApiResponse<List<OfficeDto>> searchOffices(OfficeSearchRequest request) {
        try {
            Integer city = request.getCity();
            Integer ward = request.getWard();
            String search = request.getSearch();

            Specification<Office> spec = Specification.<Office>unrestricted()
                    .and(OfficeSpecification.active())
                    .and(OfficeSpecification.city(city))
                    .and(OfficeSpecification.ward(ward))
                    .and(OfficeSpecification.search(search));

            List<Office> offices = officeRepository.findAll(spec, Sort.by("name").ascending());

            List<OfficeDto> officeDtos = offices.stream()
                    .map(OfficeMapper::toDto)
                    .toList();

            return new ApiResponse<>(true, "Lấy danh sách bưu cục thành công", officeDtos);
        } catch (Exception e) {
            return new ApiResponse<>(false, "Lỗi khi lấy danh sách bưu cục: " + e.getMessage(), null);
        }
    }
}