package com.logistics.service.admin;

import com.logistics.entity.Promotion;
import com.logistics.entity.ServiceType;
import com.logistics.entity.User;
import com.logistics.entity.UserPromotion;
import com.logistics.enums.PromotionDiscountType;
import com.logistics.enums.PromotionStatus;
import com.logistics.exception.AppException;
import com.logistics.exception.enums.PromotionErrorCode;
import com.logistics.repository.PromotionRepository;
import com.logistics.repository.ServiceTypeRepository;
import com.logistics.repository.UserPromotionRepository;
import com.logistics.repository.UserRepository;
import com.logistics.request.admin.CreatePromotionRequest;
import com.logistics.request.admin.UpdatePromotionRequest;
import com.logistics.response.Pagination;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class PromotionAdminService {

    @Autowired
    private PromotionRepository promotionRepository;

    @Autowired
    private ServiceTypeRepository serviceTypeRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserPromotionRepository userPromotionRepository;

    public Map<String, Object> listPromotions(int page, int limit, String search, String status, Boolean isGlobal) {
        Pageable pageable = PageRequest.of(page - 1, limit, Sort.by("createdAt").descending());

        Specification<Promotion> spec = Specification.where(null);

        if (search != null && !search.trim().isEmpty()) {
            spec = spec.and((root, query, cb) ->
                cb.or(
                    cb.like(cb.lower(root.get("code")), "%" + search.toLowerCase() + "%"),
                    cb.like(cb.lower(root.get("title")), "%" + search.toLowerCase() + "%"),
                    cb.like(cb.lower(root.get("description")), "%" + search.toLowerCase() + "%")
                )
            );
        }

        if (status != null && !status.trim().isEmpty()) {
            try {
                PromotionStatus statusEnum = PromotionStatus.valueOf(status.toUpperCase());
                spec = spec.and((root, query, cb) -> cb.equal(root.get("status"), statusEnum));
            } catch (IllegalArgumentException e) {
                // Invalid status filter, ignore
            }
        }

        if (isGlobal != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("isGlobal"), isGlobal));
        }

        Page<Promotion> promotionPage = promotionRepository.findAll(spec, pageable);

        List<Map<String, Object>> promotions = promotionPage.getContent().stream()
                .map(this::mapPromotion)
                .collect(Collectors.toList());

        Pagination pagination = new Pagination(
                (int) promotionPage.getTotalElements(),
                page,
                limit,
                promotionPage.getTotalPages());

        Map<String, Object> result = new HashMap<>();
        result.put("data", promotions);
        result.put("pagination", pagination);

        return result;
    }

    public Map<String, Object> getPromotionById(Integer promotionId) {
        Promotion promotion = promotionRepository.findById(promotionId)
                .orElseThrow(() -> new AppException(PromotionErrorCode.PROMOTION_NOT_FOUND));
        return mapPromotion(promotion);
    }

    @Transactional
    public void createPromotion(CreatePromotionRequest request) {
        if (request.getCode() == null || request.getCode().isBlank()) {
            throw new AppException(PromotionErrorCode.PROMOTION_CODE_REQUIRED);
        }

        String normalizedCode = request.getCode().toUpperCase().trim();
        if (promotionRepository.existsByCode(normalizedCode)) {
            throw new AppException(PromotionErrorCode.PROMOTION_CODE_EXISTED);
        }

        if (request.getStartDate() != null && request.getEndDate() != null
                && request.getEndDate().isBefore(request.getStartDate())) {
            throw new AppException(PromotionErrorCode.PROMOTION_INVALID_DATE_RANGE);
        }

        Promotion promotion = new Promotion();
        promotion.setCode(normalizedCode);
        promotion.setTitle(request.getTitle());
        promotion.setDescription(request.getDescription());
        promotion.setDiscountType(
                request.getDiscountType() != null
                        ? PromotionDiscountType.valueOf(request.getDiscountType().toUpperCase())
                        : PromotionDiscountType.PERCENTAGE);
        promotion.setDiscountValue(request.getDiscountValue());
        promotion.setIsGlobal(request.getIsGlobal() != null ? request.getIsGlobal() : true);
        promotion.setMaxDiscountAmount(request.getMaxDiscountAmount());
        promotion.setStartDate(request.getStartDate());
        promotion.setEndDate(request.getEndDate());

        promotion.setMinOrderValue(request.getMinOrderValue());
        promotion.setMinWeight(request.getMinWeight());
        promotion.setMaxWeight(request.getMaxWeight());
        promotion.setMinOrdersCount(request.getMinOrdersCount());
        promotion.setFirstTimeUser(request.getFirstTimeUser() != null ? request.getFirstTimeUser() : false);
        promotion.setValidMonthsAfterJoin(request.getValidMonthsAfterJoin());
        promotion.setValidYearsAfterJoin(request.getValidYearsAfterJoin());

        promotion.setUsageLimit(request.getUsageLimit());
        promotion.setMaxUsagePerUser(request.getMaxUsagePerUser());
        promotion.setDailyUsageLimitGlobal(request.getDailyUsageLimitGlobal());
        promotion.setDailyUsageLimitPerUser(request.getDailyUsageLimitPerUser());
        promotion.setUsedCount(0);

        promotion.setStatus(
                request.getStatus() != null
                        ? PromotionStatus.valueOf(request.getStatus().toUpperCase())
                        : PromotionStatus.ACTIVE);

        if (request.getServiceTypeIds() != null && !request.getServiceTypeIds().isEmpty()) {
            List<ServiceType> serviceTypes = serviceTypeRepository.findAllById(request.getServiceTypeIds());
            promotion.setServiceTypes(serviceTypes);
        }

        promotion = promotionRepository.save(promotion);

        if (!promotion.getIsGlobal() && request.getUserIds() != null) {
            List<UserPromotion> managedLinks = promotion.getUserPromotions();
            if (managedLinks == null) {
                managedLinks = new ArrayList<>();
                promotion.setUserPromotions(managedLinks);
            }
            managedLinks.clear();
            if (!request.getUserIds().isEmpty()) {
                List<User> users = userRepository.findAllById(request.getUserIds());
                for (User user : users) {
                    UserPromotion userPromotion = new UserPromotion();
                    userPromotion.setUser(user);
                    userPromotion.setPromotion(promotion);
                    userPromotion.setUsedCount(0);
                    managedLinks.add(userPromotion);
                }
            }
        }

        promotionRepository.save(promotion);
    }

    @Transactional
    public void updatePromotion(Integer promotionId, UpdatePromotionRequest request) {
        Promotion promotion = promotionRepository.findById(promotionId)
                .orElseThrow(() -> new AppException(PromotionErrorCode.PROMOTION_NOT_FOUND));

        if (request.getCode() != null && !request.getCode().trim().isEmpty()) {
            String normalizedCode = request.getCode().toUpperCase().trim();
            if (!normalizedCode.equals(promotion.getCode()) && promotionRepository.existsByCode(normalizedCode)) {
                throw new AppException(PromotionErrorCode.PROMOTION_CODE_EXISTED);
            }
            promotion.setCode(normalizedCode);
        }

        if (request.getStartDate() != null && request.getEndDate() != null
                && request.getEndDate().isBefore(request.getStartDate())) {
            throw new AppException(PromotionErrorCode.PROMOTION_INVALID_DATE_RANGE);
        }

        if (request.getTitle() != null) promotion.setTitle(request.getTitle());
        if (request.getDescription() != null) promotion.setDescription(request.getDescription());
        if (request.getDiscountType() != null)
            promotion.setDiscountType(PromotionDiscountType.valueOf(request.getDiscountType().toUpperCase()));
        if (request.getDiscountValue() != null) promotion.setDiscountValue(request.getDiscountValue());
        if (request.getIsGlobal() != null) promotion.setIsGlobal(request.getIsGlobal());
        if (request.getMaxDiscountAmount() != null) promotion.setMaxDiscountAmount(request.getMaxDiscountAmount());
        if (request.getStartDate() != null) promotion.setStartDate(request.getStartDate());
        if (request.getEndDate() != null) promotion.setEndDate(request.getEndDate());

        if (request.getMinOrderValue() != null) promotion.setMinOrderValue(request.getMinOrderValue());
        if (request.getMinWeight() != null) promotion.setMinWeight(request.getMinWeight());
        if (request.getMaxWeight() != null) promotion.setMaxWeight(request.getMaxWeight());
        if (request.getMinOrdersCount() != null) promotion.setMinOrdersCount(request.getMinOrdersCount());
        if (request.getFirstTimeUser() != null) promotion.setFirstTimeUser(request.getFirstTimeUser());
        if (request.getValidMonthsAfterJoin() != null) promotion.setValidMonthsAfterJoin(request.getValidMonthsAfterJoin());
        if (request.getValidYearsAfterJoin() != null) promotion.setValidYearsAfterJoin(request.getValidYearsAfterJoin());

        if (request.getUsageLimit() != null) promotion.setUsageLimit(request.getUsageLimit());
        if (request.getMaxUsagePerUser() != null) promotion.setMaxUsagePerUser(request.getMaxUsagePerUser());
        if (request.getDailyUsageLimitGlobal() != null) promotion.setDailyUsageLimitGlobal(request.getDailyUsageLimitGlobal());
        if (request.getDailyUsageLimitPerUser() != null) promotion.setDailyUsageLimitPerUser(request.getDailyUsageLimitPerUser());
        if (request.getStatus() != null)
            promotion.setStatus(PromotionStatus.valueOf(request.getStatus().toUpperCase()));

        if (request.getServiceTypeIds() != null) {
            if (request.getServiceTypeIds().isEmpty()) {
                promotion.setServiceTypes(new ArrayList<>());
            } else {
                List<ServiceType> serviceTypes = serviceTypeRepository.findAllById(request.getServiceTypeIds());
                promotion.setServiceTypes(serviceTypes);
            }
        }

        promotion = promotionRepository.save(promotion);

        List<UserPromotion> managedLinks = promotion.getUserPromotions();
        if (managedLinks == null) {
            managedLinks = new ArrayList<>();
            promotion.setUserPromotions(managedLinks);
        }

        boolean clearLinks = promotion.getIsGlobal()
                || (request.getUserIds() != null && request.getUserIds().isEmpty());

        if (clearLinks && !managedLinks.isEmpty()) {
            managedLinks.clear();
        }

        if (!promotion.getIsGlobal() && request.getUserIds() != null && !request.getUserIds().isEmpty()) {
            managedLinks.clear();
            List<User> users = userRepository.findAllById(request.getUserIds());
            for (User user : users) {
                UserPromotion userPromotion = new UserPromotion();
                userPromotion.setUser(user);
                userPromotion.setPromotion(promotion);
                userPromotion.setUsedCount(0);
                managedLinks.add(userPromotion);
            }
        }

        promotionRepository.save(promotion);
    }

    @Transactional
    public void deletePromotion(Integer promotionId) {
        Promotion promotion = promotionRepository.findById(promotionId)
                .orElseThrow(() -> new AppException(PromotionErrorCode.PROMOTION_NOT_FOUND));
        promotionRepository.delete(promotion);
    }

    private Map<String, Object> mapPromotion(Promotion promotion) {
        Map<String, Object> promotionMap = new HashMap<>();
        promotionMap.put("id", promotion.getId());
        promotionMap.put("code", promotion.getCode());
        promotionMap.put("title", promotion.getTitle());
        promotionMap.put("description", promotion.getDescription());
        promotionMap.put("discountType", promotion.getDiscountType().name());
        promotionMap.put("discountValue", promotion.getDiscountValue());
        promotionMap.put("isGlobal", promotion.getIsGlobal());
        promotionMap.put("maxDiscountAmount", promotion.getMaxDiscountAmount());
        promotionMap.put("startDate", promotion.getStartDate());
        promotionMap.put("endDate", promotion.getEndDate());

        promotionMap.put("minOrderValue", promotion.getMinOrderValue());
        promotionMap.put("minWeight", promotion.getMinWeight());
        promotionMap.put("maxWeight", promotion.getMaxWeight());
        promotionMap.put("minOrdersCount", promotion.getMinOrdersCount());
        promotionMap.put("firstTimeUser", promotion.getFirstTimeUser());
        promotionMap.put("validMonthsAfterJoin", promotion.getValidMonthsAfterJoin());
        promotionMap.put("validYearsAfterJoin", promotion.getValidYearsAfterJoin());

        if (promotion.getServiceTypes() != null) {
            promotionMap.put("serviceTypeIds", promotion.getServiceTypes().stream()
                    .map(ServiceType::getId)
                    .collect(Collectors.toList()));
        }

        promotionMap.put("usageLimit", promotion.getUsageLimit());
        promotionMap.put("maxUsagePerUser", promotion.getMaxUsagePerUser());
        promotionMap.put("dailyUsageLimitGlobal", promotion.getDailyUsageLimitGlobal());
        promotionMap.put("dailyUsageLimitPerUser", promotion.getDailyUsageLimitPerUser());
        promotionMap.put("usedCount", promotion.getUsedCount());
        promotionMap.put("status", promotion.getStatus().name());
        promotionMap.put("createdAt", promotion.getCreatedAt());
        promotionMap.put("updatedAt", promotion.getUpdatedAt());

        if (!promotion.getIsGlobal() && promotion.getUserPromotions() != null) {
            promotionMap.put("userIds", promotion.getUserPromotions().stream()
                    .filter(up -> up.getUser() != null && up.getUser().getId() != null)
                    .map(up -> up.getUser().getId())
                    .collect(Collectors.toList()));
        }

        return promotionMap;
    }
}
