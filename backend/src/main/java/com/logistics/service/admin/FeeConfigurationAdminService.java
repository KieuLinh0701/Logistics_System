package com.logistics.service.admin;

import com.logistics.entity.FeeConfiguration;
import com.logistics.entity.ServiceType;
import com.logistics.enums.CodFeeType;
import com.logistics.enums.FeeType;
import com.logistics.exception.AppException;
import com.logistics.exception.enums.FeeConfigurationErrorCode;
import com.logistics.exception.enums.ServiceTypeErrorCode;
import com.logistics.repository.FeeConfigurationRepository;
import com.logistics.repository.ServiceTypeRepository;
import com.logistics.request.admin.CreateFeeConfigurationRequest;
import com.logistics.request.admin.UpdateFeeConfigurationRequest;
import com.logistics.response.Pagination;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class FeeConfigurationAdminService {

    @Autowired
    private FeeConfigurationRepository feeConfigurationRepository;

    @Autowired
    private ServiceTypeRepository serviceTypeRepository;

    public Map<String, Object> listFeeConfigurations(int page, int limit, String search, String feeType, Integer serviceTypeId, Boolean active) {
        Pageable pageable = PageRequest.of(page - 1, limit, Sort.by("createdAt").descending());

        Specification<FeeConfiguration> spec = Specification.where(null);

        if (search != null && !search.trim().isEmpty()) {
            spec = spec.and((root, query, cb) ->
                cb.like(cb.lower(root.get("notes")), "%" + search.toLowerCase() + "%")
            );
        }

        if (feeType != null && !feeType.trim().isEmpty()) {
            try {
                FeeType feeTypeEnum = FeeType.valueOf(feeType.toUpperCase());
                spec = spec.and((root, query, cb) -> cb.equal(root.get("feeType"), feeTypeEnum));
            } catch (IllegalArgumentException e) {
                // Invalid feeType filter, ignore
            }
        }

        if (serviceTypeId != null) {
            spec = spec.and((root, query, cb) ->
                cb.equal(root.get("serviceType").get("id"), serviceTypeId)
            );
        }

        if (active != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("active"), active));
        }

        Page<FeeConfiguration> feeConfigPage = feeConfigurationRepository.findAll(spec, pageable);

        List<Map<String, Object>> feeConfigs = feeConfigPage.getContent().stream()
                .map(this::mapFeeConfiguration)
                .collect(Collectors.toList());

        Pagination pagination = new Pagination(
                (int) feeConfigPage.getTotalElements(),
                page,
                limit,
                feeConfigPage.getTotalPages());

        Map<String, Object> result = new HashMap<>();
        result.put("data", feeConfigs);
        result.put("pagination", pagination);

        return result;
    }

    public Map<String, Object> getFeeConfigurationById(Integer id) {
        FeeConfiguration feeConfig = feeConfigurationRepository.findById(id)
                .orElseThrow(() -> new AppException(FeeConfigurationErrorCode.FEE_CONFIG_NOT_FOUND));
        return mapFeeConfiguration(feeConfig);
    }

    @Transactional
    public void createFeeConfiguration(CreateFeeConfigurationRequest request) {
        if (request.getFeeType() == null || request.getFeeType().isBlank()) {
            throw new AppException(FeeConfigurationErrorCode.FEE_CONFIG_FEE_TYPE_REQUIRED);
        }

        if (request.getCalculationType() == null || request.getCalculationType().isBlank()) {
            throw new AppException(FeeConfigurationErrorCode.FEE_CONFIG_CALCULATION_TYPE_REQUIRED);
        }

        if (request.getFeeValue() == null) {
            throw new AppException(FeeConfigurationErrorCode.FEE_CONFIG_FEE_VALUE_REQUIRED);
        }

        FeeConfiguration feeConfig = new FeeConfiguration();
        feeConfig.setFeeType(FeeType.valueOf(request.getFeeType().toUpperCase()));
        feeConfig.setCalculationType(CodFeeType.valueOf(request.getCalculationType().toUpperCase()));
        feeConfig.setFeeValue(request.getFeeValue());
        feeConfig.setMinOrderFee(request.getMinOrderFee());
        feeConfig.setMaxOrderFee(request.getMaxOrderFee());
        feeConfig.setActive(request.getActive() != null ? request.getActive() : true);
        feeConfig.setNotes(request.getNotes());

        if (request.getServiceTypeId() != null) {
            ServiceType serviceType = serviceTypeRepository.findById(request.getServiceTypeId())
                    .orElseThrow(() -> new AppException(ServiceTypeErrorCode.SERVICE_TYPE_NOT_FOUND));
            feeConfig.setServiceType(serviceType);
        }

        feeConfig = feeConfigurationRepository.save(feeConfig);
    }

    @Transactional
    public void updateFeeConfiguration(Integer id, UpdateFeeConfigurationRequest request) {
        FeeConfiguration feeConfig = feeConfigurationRepository.findById(id)
                .orElseThrow(() -> new AppException(FeeConfigurationErrorCode.FEE_CONFIG_NOT_FOUND));

        if (request.getFeeType() != null) {
            feeConfig.setFeeType(FeeType.valueOf(request.getFeeType().toUpperCase()));
        }
        if (request.getCalculationType() != null) {
            feeConfig.setCalculationType(CodFeeType.valueOf(request.getCalculationType().toUpperCase()));
        }
        if (request.getFeeValue() != null) {
            feeConfig.setFeeValue(request.getFeeValue());
        }
        if (request.getMinOrderFee() != null) {
            feeConfig.setMinOrderFee(request.getMinOrderFee());
        }
        if (request.getMaxOrderFee() != null) {
            feeConfig.setMaxOrderFee(request.getMaxOrderFee());
        }
        if (request.getActive() != null) {
            feeConfig.setActive(request.getActive());
        }
        if (request.getNotes() != null) {
            feeConfig.setNotes(request.getNotes());
        }

        if (request.getServiceTypeId() != null) {
            ServiceType serviceType = serviceTypeRepository.findById(request.getServiceTypeId())
                    .orElseThrow(() -> new AppException(ServiceTypeErrorCode.SERVICE_TYPE_NOT_FOUND));
            feeConfig.setServiceType(serviceType);
        }
        
        feeConfig = feeConfigurationRepository.save(feeConfig);
    }

    @Transactional
    public void deleteFeeConfiguration(Integer id) {
        FeeConfiguration feeConfig = feeConfigurationRepository.findById(id)
                .orElseThrow(() -> new AppException(FeeConfigurationErrorCode.FEE_CONFIG_NOT_FOUND));
        feeConfigurationRepository.delete(feeConfig);
    }

    private Map<String, Object> mapFeeConfiguration(FeeConfiguration feeConfig) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", feeConfig.getId());
        map.put("feeType", feeConfig.getFeeType().name());
        map.put("calculationType", feeConfig.getCalculationType().name());
        map.put("feeValue", feeConfig.getFeeValue());
        map.put("minOrderFee", feeConfig.getMinOrderFee());
        map.put("maxOrderFee", feeConfig.getMaxOrderFee());
        map.put("active", feeConfig.getActive());
        map.put("notes", feeConfig.getNotes());
        map.put("createdAt", feeConfig.getCreatedAt());
        map.put("updatedAt", feeConfig.getUpdatedAt());

        if (feeConfig.getServiceType() != null) {
            map.put("serviceTypeId", feeConfig.getServiceType().getId());
            map.put("serviceTypeName", feeConfig.getServiceType().getName());
        }

        return map;
    }
}
