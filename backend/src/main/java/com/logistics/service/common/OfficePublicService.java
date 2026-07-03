package com.logistics.service.common;

import com.logistics.dto.common.PublicOfficeInformationDto;
import com.logistics.dto.common.PublicOfficeSearchDto;
import com.logistics.entity.Office;
import com.logistics.request.common.office.PublicOfficeSearchRequest;

import java.util.List;
import java.util.Optional;

public interface OfficePublicService {
    List<PublicOfficeSearchDto> searchOffices(PublicOfficeSearchRequest request);
    PublicOfficeInformationDto getHeadOffice();
    List<PublicOfficeInformationDto> listLocalOffices(PublicOfficeSearchRequest request);
    Boolean checkLocalOffices(int cityCode);
    boolean hasLocalOffices(int cityCode);
    boolean isSameCity(int senderAddressId, int officeId);
    Optional<Office> findById(Integer id);
}