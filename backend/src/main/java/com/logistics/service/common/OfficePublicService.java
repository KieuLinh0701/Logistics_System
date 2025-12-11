package com.logistics.service.common;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import com.logistics.dto.common.PublicOfficeInformationDto;
import com.logistics.dto.common.PublicOfficeSearchDto;
import com.logistics.entity.Office;
import com.logistics.enums.OfficeStatus;
import com.logistics.enums.OfficeType;
import com.logistics.mapper.OfficeMapper;
import com.logistics.repository.AddressRepository;
import com.logistics.repository.OfficeRepository;
import com.logistics.request.common.office.PublicOfficeSearchRequest;
import com.logistics.response.ApiResponse;
import com.logistics.specification.OfficeSpecification;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class OfficePublicService {

    private final OfficeRepository officeRepository;

    private final AddressRepository addressRepository;

    public ApiResponse<List<PublicOfficeSearchDto>> searchOffices(PublicOfficeSearchRequest request) {
        try {
            Integer city = request.getCity();
            Integer ward = request.getWard();
            String search = request.getSearch();

            Specification<Office> spec = Specification.<Office>unrestricted()
                    .and(OfficeSpecification.status(OfficeStatus.ACTIVE.name()))
                    .and(OfficeSpecification.city(city))
                    .and(OfficeSpecification.ward(ward))
                    .and(OfficeSpecification.search(search));

            List<Office> offices = officeRepository.findAll(spec, Sort.by("name").ascending());

            List<PublicOfficeSearchDto> officeDtos = offices.stream()
                    .map(OfficeMapper::toPublicOfficeSearchDto)
                    .toList();

            return new ApiResponse<>(true, "Lấy danh sách bưu cục thành công", officeDtos);
        } catch (Exception e) {
            return new ApiResponse<>(false, "Lỗi khi lấy danh sách bưu cục: " + e.getMessage(), null);
        }
    }

    public ApiResponse<PublicOfficeInformationDto> getHeadOffice() {
        try {
            Specification<Office> spec = Specification.<Office>unrestricted()
                    .and(OfficeSpecification.status(OfficeStatus.ACTIVE.name()))
                    .and(OfficeSpecification.type(OfficeType.HEAD_OFFICE.name()));

            Optional<Office> officeOpt = officeRepository.findAll(spec, Sort.by("name").ascending())
                    .stream()
                    .findFirst();

            if (officeOpt.isPresent()) {
                PublicOfficeInformationDto officeDto = OfficeMapper.toPublicOfficeInformationDto(officeOpt.get());
                return new ApiResponse<>(true, "Lấy bưu cục chính thành công", officeDto);
            } else {
                return new ApiResponse<>(false, "Không tìm thấy bưu cục chính", null);
            }
        } catch (Exception e) {
            return new ApiResponse<>(false, "Lỗi khi lấy bưu cục chính: " + e.getMessage(), null);
        }
    }

    public ApiResponse<List<PublicOfficeInformationDto>> listLocalOffices(PublicOfficeSearchRequest request) {
        try {
            Integer city = request.getCity();
            Integer ward = request.getWard();

            Specification<Office> spec = Specification.<Office>unrestricted()
                    .and(OfficeSpecification.status(OfficeStatus.ACTIVE.name()))
                    .and(OfficeSpecification.city(city))
                    .and(OfficeSpecification.ward(ward));

            List<Office> offices = officeRepository.findAll(spec, Sort.by("name").ascending());

            if (offices.isEmpty() && city != null) {
                Specification<Office> specCityOnly = Specification.<Office>unrestricted()
                        .and(OfficeSpecification.status(OfficeStatus.ACTIVE.name()))
                        .and(OfficeSpecification.city(city));

                offices = officeRepository.findAll(specCityOnly, Sort.by("name").ascending());
            }

            List<PublicOfficeInformationDto> officeDtos = offices.stream()
                    .map(OfficeMapper::toPublicOfficeInformationDto)
                    .toList();

            return new ApiResponse<>(true, "Lấy danh sách bưu cục thành công", officeDtos);

        } catch (Exception e) {
            return new ApiResponse<>(false, "Lỗi khi lấy danh sách bưu cục: " + e.getMessage(), null);
        }
    }

    public ApiResponse<Boolean> checkLocalOffices(int cityCode) {
        try {
            boolean exists = hasLocalOffices(cityCode);
            return new ApiResponse<>(true, "Kiểm tra bưu cục thành công", exists);

        } catch (Exception e) {
            return new ApiResponse<>(false, "Lỗi khi kiểm tra bưu cục: " + e.getMessage(), false);
        }
    }

    public boolean hasLocalOffices(int cityCode) {
        Specification<Office> spec = Specification.<Office>unrestricted()
                .and(OfficeSpecification.status(OfficeStatus.ACTIVE.name()))
                .and(OfficeSpecification.city(cityCode));

        return officeRepository.exists(spec);
    }

    public boolean isSameCity(int senderAddressId, int officeId) {

        Integer senderCity = addressRepository.findCityCodeById(senderAddressId);
        if (senderCity == null)
            return false;

        Integer officeCity = officeRepository.findCityCodeById(officeId);
        if (officeCity == null)
            return false;

        return senderCity.equals(officeCity);
    }

    public Optional<Office> findById(Integer id) {
        return officeRepository.findById(id);
    }
}