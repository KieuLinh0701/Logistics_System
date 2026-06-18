package com.logistics.service.admin;

import java.time.LocalTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.logistics.request.admin.CreateOfficeRequest;
import com.logistics.request.admin.UpdateOfficeRequest;
import com.logistics.entity.Office;
import com.logistics.enums.OfficeStatus;
import com.logistics.enums.OfficeType;
import com.logistics.exception.AppException;
import com.logistics.exception.ErrorCode;
import com.logistics.exception.OfficeErrorCode;
import com.logistics.repository.OfficeRepository;
import com.logistics.response.Pagination;

@Service
public class OfficeAdminService {

    @Autowired
    private OfficeRepository officeRepository;

    public Map<String, Object> listOffices(int page, int limit, String search) {
        Pageable pageable = PageRequest.of(page - 1, limit, Sort.by("createdAt").descending());
        Page<Office> officePage;

        if (search != null && !search.trim().isEmpty()) {
            officePage = officeRepository.findByNameContainingIgnoreCaseOrCodeContainingIgnoreCase(search, search,
                    pageable);
        } else {
            officePage = officeRepository.findAll(pageable);
        }

        List<Map<String, Object>> offices = officePage.getContent().stream()
                .map(this::mapOffice)
                .collect(Collectors.toList());

        Pagination pagination = new Pagination(
                (int) officePage.getTotalElements(),
                page,
                limit,
                officePage.getTotalPages());

        Map<String, Object> result = new HashMap<>();
        result.put("data", offices);
        result.put("pagination", pagination);

        return result;
    }

    public Map<String, Object> getOfficeById(Integer officeId) {
        Office office = officeRepository.findById(officeId)
                .orElseThrow(() -> new AppException(OfficeErrorCode.NOT_FOUND));
        return mapOffice(office);
    }

    @Transactional
    public void createOffice(CreateOfficeRequest request) {
        String normalizedCode = normalizeCode(request.getCode());
        if (normalizedCode == null || normalizedCode.isBlank()) {
            throw new AppException(ErrorCode.BAD_REQUEST, "Mã bưu cục không được để trống");
        }

        if (officeRepository.existsByCode(normalizedCode)) {
            throw new AppException(OfficeErrorCode.CODE_EXISTED);
        }

        if (request.getPhoneNumber() != null && officeRepository.existsByPhoneNumber(request.getPhoneNumber())) {
            throw new AppException(OfficeErrorCode.PHONE_EXISTED);
        }

        if (request.getWardCode() == null || request.getCityCode() == null || request.getDetailAddress() == null) {
            throw new AppException(OfficeErrorCode.ADDRESS_REQUIRED);
        }

        if (request.getLatitude() == null || request.getLongitude() == null) {
            throw new AppException(OfficeErrorCode.COORDINATES_REQUIRED);
        }

        Office office = new Office();
        office.setCode(normalizedCode);
        office.setPostalCode(request.getPostalCode());
        office.setName(request.getName());
        office.setLatitude(request.getLatitude());
        office.setLongitude(request.getLongitude());
        office.setEmail(request.getEmail());
        office.setPhoneNumber(request.getPhoneNumber());
        office.setOpeningTime(request.getOpeningTime() != null ? request.getOpeningTime() : LocalTime.of(7, 0));
        office.setClosingTime(request.getClosingTime() != null ? request.getClosingTime() : LocalTime.of(17, 0));
        if (request.getType() != null) {
            office.setType(OfficeType.valueOf(request.getType().toUpperCase()));
        }
        if (request.getStatus() != null) {
            office.setStatus(OfficeStatus.valueOf(request.getStatus().toUpperCase()));
        }
        office.setCapacity(request.getCapacity());
        office.setNotes(request.getNotes());
        office.setCityCode(request.getCityCode());
        office.setWardCode(request.getWardCode());
        office.setDetail(request.getDetailAddress());

        office = officeRepository.save(office);
    }

    @Transactional
    public void updateOffice(Integer officeId, UpdateOfficeRequest request) {
        Office office = officeRepository.findById(officeId)
                .orElseThrow(() -> new AppException(OfficeErrorCode.NOT_FOUND));

        if (request.getCode() != null) {
            String normalizedCode = normalizeCode(request.getCode());
            if (!normalizedCode.equalsIgnoreCase(office.getCode())
                    && officeRepository.existsByCode(normalizedCode)) {
                throw new AppException(OfficeErrorCode.CODE_EXISTED);
            }
            office.setCode(normalizedCode);
        }

        if (request.getPhoneNumber() != null && !request.getPhoneNumber().equalsIgnoreCase(office.getPhoneNumber())
                && officeRepository.existsByPhoneNumber(request.getPhoneNumber())) {
            throw new AppException(OfficeErrorCode.PHONE_EXISTED);
        }

        if (request.getPostalCode() != null)
            office.setPostalCode(request.getPostalCode());
        if (request.getName() != null)
            office.setName(request.getName());
        if (request.getLatitude() != null)
            office.setLatitude(request.getLatitude());
        if (request.getLongitude() != null)
            office.setLongitude(request.getLongitude());
        if (request.getEmail() != null)
            office.setEmail(request.getEmail());
        if (request.getPhoneNumber() != null)
            office.setPhoneNumber(request.getPhoneNumber());
        if (request.getOpeningTime() != null)
            office.setOpeningTime(request.getOpeningTime());
        if (request.getClosingTime() != null)
            office.setClosingTime(request.getClosingTime());
        if (request.getType() != null)
            office.setType(OfficeType.valueOf(request.getType().toUpperCase()));
        if (request.getStatus() != null)
            office.setStatus(OfficeStatus.valueOf(request.getStatus().toUpperCase()));
        if (request.getCapacity() != null)
            office.setCapacity(request.getCapacity());
        if (request.getNotes() != null)
            office.setNotes(request.getNotes());

        if (request.getWardCode() != null)
            office.setWardCode(request.getWardCode());
        if (request.getCityCode() != null)
            office.setCityCode(request.getCityCode());
        if (request.getDetailAddress() != null)
            office.setDetail(request.getDetailAddress());

        office = officeRepository.save(office);
    }

    @Transactional
    public void deleteOffice(Integer officeId) {
        Office office = officeRepository.findById(officeId)
                .orElseThrow(() -> new AppException(OfficeErrorCode.NOT_FOUND));

        officeRepository.delete(office);
    }

    private Map<String, Object> mapOffice(Office office) {
        Map<String, Object> officeMap = new HashMap<>();
        officeMap.put("id", office.getId());
        officeMap.put("code", office.getCode());
        officeMap.put("postalCode", office.getPostalCode());
        officeMap.put("name", office.getName());
        officeMap.put("email", office.getEmail());
        officeMap.put("phoneNumber", office.getPhoneNumber());
        officeMap.put("type", office.getType() != null ? office.getType().name() : null);
        officeMap.put("status", office.getStatus() != null ? office.getStatus().name() : null);
        officeMap.put("latitude", office.getLatitude());
        officeMap.put("longitude", office.getLongitude());
        officeMap.put("openingTime", office.getOpeningTime());
        officeMap.put("closingTime", office.getClosingTime());
        officeMap.put("capacity", office.getCapacity());
        officeMap.put("notes", office.getNotes());
        officeMap.put("createdAt", office.getCreatedAt());
        officeMap.put("cityCode", office.getCityCode());
        officeMap.put("wardCode", office.getWardCode());
        officeMap.put("detail", office.getDetail());

        return officeMap;
    }

    private String normalizeCode(String code) {
        if (code == null) {
            return null;
        }
        return code.replace("_", "").trim().toUpperCase();
    }
}
