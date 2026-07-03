package com.logistics.service.common.impl;

import com.logistics.dto.common.PublicOfficeInformationDto;
import com.logistics.dto.common.PublicOfficeSearchDto;
import com.logistics.entity.Office;
import com.logistics.enums.OfficeStatus;
import com.logistics.enums.OfficeType;
import com.logistics.exception.AppException;
import com.logistics.exception.enums.OfficeErrorCode;
import com.logistics.mapper.OfficeMapper;
import com.logistics.repository.AddressRepository;
import com.logistics.repository.OfficeRepository;
import com.logistics.request.common.office.PublicOfficeSearchRequest;
import com.logistics.service.common.OfficePublicService;
import com.logistics.specification.OfficeSpecification;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OfficePublicServiceImpl implements OfficePublicService {

    private final OfficeRepository officeRepository;

    private final AddressRepository addressRepository;

    @Override
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

    @Override
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

    @Override
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

    @Override
    public Boolean checkLocalOffices(int cityCode) {
            return hasLocalOffices(cityCode);
    }

    @Override
    public boolean hasLocalOffices(int cityCode) {
        Specification<Office> spec = Specification.<Office>unrestricted()
                .and(OfficeSpecification.status(OfficeStatus.ACTIVE.name()))
                .and(OfficeSpecification.city(cityCode));

        return officeRepository.exists(spec);
    }

    @Override
    public boolean isSameCity(int senderAddressId, int officeId) {

        Integer senderCity = addressRepository.findCityCodeById(senderAddressId);
        if (senderCity == null)
            return false;

        Integer officeCity = officeRepository.findCityCodeById(officeId);
        if (officeCity == null)
            return false;

        return senderCity.equals(officeCity);
    }

    @Override
    public Optional<Office> findById(Integer id) {
        return officeRepository.findById(id);
    }
}