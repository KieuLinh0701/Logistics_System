package com.logistics.service.common;

import java.util.List;
import java.util.Optional;

import com.logistics.exception.AppException;
import com.logistics.exception.enums.OfficeErrorCode;
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

    public List<PublicOfficeSearchDto> searchOffices(PublicOfficeSearchRequest request) {
            Integer city = request.getCity();
            Integer ward = request.getWard();
            String search = request.getSearch();

            Specification<Office> spec = Specification.<Office>unrestricted()
                    .and(OfficeSpecification.status(OfficeStatus.ACTIVE.name()))
                    .and(OfficeSpecification.city(city))
                    .and(OfficeSpecification.ward(ward))
                    .and(OfficeSpecification.search(search));

            List<Office> offices = officeRepository.findAll(spec, Sort.by("name").ascending());

            return offices.stream()
                    .map(OfficeMapper::toPublicOfficeSearchDto)
                    .toList();
    }

    public PublicOfficeInformationDto getHeadOffice() {
            Specification<Office> spec = Specification.<Office>unrestricted()
                    .and(OfficeSpecification.status(OfficeStatus.ACTIVE.name()))
                    .and(OfficeSpecification.type(OfficeType.HEAD_OFFICE.name()));

            Optional<Office> officeOpt = officeRepository.findAll(spec, Sort.by("name").ascending())
                    .stream()
                    .findFirst();

            if (officeOpt.isPresent()) {
                return OfficeMapper.toPublicOfficeInformationDto(officeOpt.get());
            } else {
                throw new AppException(OfficeErrorCode.OFFICE_HEAD_NOT_FOUND);
            }
    }

    public List<PublicOfficeInformationDto> listLocalOffices(PublicOfficeSearchRequest request) {
            Integer city = request.getCity();
            Integer ward = request.getWard();

            List<Office> offices;

            if (city != null && ward != null) {
                Specification<Office> spec = Specification.<Office>unrestricted()
                        .and(OfficeSpecification.status(OfficeStatus.ACTIVE.name()))
                        .and(OfficeSpecification.city(city))
                        .and(OfficeSpecification.ward(ward));

                offices = officeRepository.findAll(spec, Sort.by("name").ascending());

                if (offices.isEmpty()) {
                    Specification<Office> specCityOnly = Specification.<Office>unrestricted()
                            .and(OfficeSpecification.status(OfficeStatus.ACTIVE.name()))
                            .and(OfficeSpecification.city(city));
                    offices = officeRepository.findAll(specCityOnly, Sort.by("name").ascending());
                }
            }
            else if (city != null) {
                Specification<Office> specCityOnly = Specification.<Office>unrestricted()
                        .and(OfficeSpecification.status(OfficeStatus.ACTIVE.name()))
                        .and(OfficeSpecification.city(city));
                offices = officeRepository.findAll(specCityOnly, Sort.by("name").ascending());
            }
            else {
                offices = List.of();
            }

            return offices.stream()
                    .map(OfficeMapper::toPublicOfficeInformationDto)
                    .toList();
    }

    public Boolean checkLocalOffices(int cityCode) {
            return hasLocalOffices(cityCode);
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