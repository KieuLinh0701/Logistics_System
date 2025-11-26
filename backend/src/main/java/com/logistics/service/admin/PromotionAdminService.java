package com.logistics.service.admin;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.logistics.entity.Promotion;
import com.logistics.entity.ServiceType;
import com.logistics.entity.User;
import com.logistics.entity.UserPromotion;
import com.logistics.enums.PromotionDiscountType;
import com.logistics.enums.PromotionStatus;
import com.logistics.repository.PromotionRepository;
import com.logistics.repository.ServiceTypeRepository;
import com.logistics.repository.UserPromotionRepository;
import com.logistics.repository.UserRepository;
import com.logistics.request.admin.CreatePromotionRequest;
import com.logistics.request.admin.UpdatePromotionRequest;
import com.logistics.response.ApiResponse;
import com.logistics.response.Pagination;
import com.logistics.specification.PromotionSpecification;

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

    public ApiResponse<Map<String, Object>> listPromotions(int page, int limit, String search, String status, Boolean isGlobal) {
        try {
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
                    // Invalid status, ignore
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

            return new ApiResponse<>(true, "Lấy danh sách khuyến mãi thành công", result);
        } catch (Exception e) {
            return new ApiResponse<>(false, "Lỗi: " + e.getMessage(), null);
        }
    }

    public ApiResponse<Map<String, Object>> getPromotionById(Integer promotionId) {
        try {
            Promotion promotion = promotionRepository.findById(promotionId)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy khuyến mãi"));
            return new ApiResponse<>(true, "Lấy thông tin khuyến mãi thành công", mapPromotion(promotion));
        } catch (Exception e) {
            return new ApiResponse<>(false, e.getMessage(), null);
        }
    }

    @Transactional
    public ApiResponse<Map<String, Object>> createPromotion(CreatePromotionRequest request) {
        try {
            if (request.getCode() == null || request.getCode().isBlank()) {
                return new ApiResponse<>(false, "Mã khuyến mãi không được để trống", null);
            }
            
            String normalizedCode = request.getCode().toUpperCase().trim();
            if (promotionRepository.existsByCode(normalizedCode)) {
                return new ApiResponse<>(false, "Mã khuyến mãi đã tồn tại", null);
            }

            if (request.getStartDate() != null && request.getEndDate() != null 
                    && request.getEndDate().isBefore(request.getStartDate())) {
                return new ApiResponse<>(false, "Ngày kết thúc phải sau ngày bắt đầu", null);
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
            
            // Điều kiện áp dụng
            promotion.setMinOrderValue(request.getMinOrderValue());
            promotion.setMinWeight(request.getMinWeight());
            promotion.setMaxWeight(request.getMaxWeight());
            promotion.setMinOrdersCount(request.getMinOrdersCount());
            promotion.setFirstTimeUser(request.getFirstTimeUser() != null ? request.getFirstTimeUser() : false);
            promotion.setValidMonthsAfterJoin(request.getValidMonthsAfterJoin());
            promotion.setValidYearsAfterJoin(request.getValidYearsAfterJoin());
            
            // Giới hạn sử dụng
            promotion.setUsageLimit(request.getUsageLimit());
            promotion.setMaxUsagePerUser(request.getMaxUsagePerUser());
            promotion.setDailyUsageLimitGlobal(request.getDailyUsageLimitGlobal());
            promotion.setDailyUsageLimitPerUser(request.getDailyUsageLimitPerUser());
            promotion.setUsedCount(0);
            
            promotion.setStatus(
                    request.getStatus() != null 
                            ? PromotionStatus.valueOf(request.getStatus().toUpperCase())
                            : PromotionStatus.ACTIVE);

            // Xử lý ServiceTypes
            if (request.getServiceTypeIds() != null && !request.getServiceTypeIds().isEmpty()) {
                List<ServiceType> serviceTypes = serviceTypeRepository.findAllById(request.getServiceTypeIds());
                promotion.setServiceTypes(serviceTypes);
            }

            promotion = promotionRepository.save(promotion);

            // Nếu không phải global, gán cho các user được chỉ định
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

            return new ApiResponse<>(true, "Tạo khuyến mãi thành công", mapPromotion(promotion));
        } catch (IllegalArgumentException e) {
            return new ApiResponse<>(false, "Giá trị enum không hợp lệ: " + e.getMessage(), null);
        } catch (Exception e) {
            return new ApiResponse<>(false, e.getMessage(), null);
        }
    }

    @Transactional
    public ApiResponse<Map<String, Object>> updatePromotion(Integer promotionId, UpdatePromotionRequest request) {
        try {
            Promotion promotion = promotionRepository.findById(promotionId)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy khuyến mãi"));

            if (request.getCode() != null && !request.getCode().trim().isEmpty()) {
                String normalizedCode = request.getCode().toUpperCase().trim();
                if (!normalizedCode.equals(promotion.getCode()) && promotionRepository.existsByCode(normalizedCode)) {
                    return new ApiResponse<>(false, "Mã khuyến mãi đã tồn tại", null);
                }
                promotion.setCode(normalizedCode);
            }

            if (request.getStartDate() != null && request.getEndDate() != null 
                    && request.getEndDate().isBefore(request.getStartDate())) {
                return new ApiResponse<>(false, "Ngày kết thúc phải sau ngày bắt đầu", null);
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
            
            // Điều kiện áp dụng
            if (request.getMinOrderValue() != null) promotion.setMinOrderValue(request.getMinOrderValue());
            if (request.getMinWeight() != null) promotion.setMinWeight(request.getMinWeight());
            if (request.getMaxWeight() != null) promotion.setMaxWeight(request.getMaxWeight());
            if (request.getMinOrdersCount() != null) promotion.setMinOrdersCount(request.getMinOrdersCount());
            if (request.getFirstTimeUser() != null) promotion.setFirstTimeUser(request.getFirstTimeUser());
            if (request.getValidMonthsAfterJoin() != null) promotion.setValidMonthsAfterJoin(request.getValidMonthsAfterJoin());
            if (request.getValidYearsAfterJoin() != null) promotion.setValidYearsAfterJoin(request.getValidYearsAfterJoin());
            
            // Giới hạn sử dụng
            if (request.getUsageLimit() != null) promotion.setUsageLimit(request.getUsageLimit());
            if (request.getMaxUsagePerUser() != null) promotion.setMaxUsagePerUser(request.getMaxUsagePerUser());
            if (request.getDailyUsageLimitGlobal() != null) promotion.setDailyUsageLimitGlobal(request.getDailyUsageLimitGlobal());
            if (request.getDailyUsageLimitPerUser() != null) promotion.setDailyUsageLimitPerUser(request.getDailyUsageLimitPerUser());
            if (request.getStatus() != null) 
                promotion.setStatus(PromotionStatus.valueOf(request.getStatus().toUpperCase()));

            // Xử lý ServiceTypes
            if (request.getServiceTypeIds() != null) {
                if (request.getServiceTypeIds().isEmpty()) {
                    promotion.setServiceTypes(new ArrayList<>());
                } else {
                    List<ServiceType> serviceTypes = serviceTypeRepository.findAllById(request.getServiceTypeIds());
                    promotion.setServiceTypes(serviceTypes);
                }
            }

            promotion = promotionRepository.save(promotion);

            // Xử lý UserPromotions nếu không phải global
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

            return new ApiResponse<>(true, "Cập nhật khuyến mãi thành công", mapPromotion(promotion));
        } catch (IllegalArgumentException e) {
            return new ApiResponse<>(false, "Giá trị enum không hợp lệ: " + e.getMessage(), null);
        } catch (Exception e) {
            return new ApiResponse<>(false, e.getMessage(), null);
        }
    }

    @Transactional
    public ApiResponse<String> deletePromotion(Integer promotionId) {
        try {
            Promotion promotion = promotionRepository.findById(promotionId)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy khuyến mãi"));
            promotionRepository.delete(promotion);
            return new ApiResponse<>(true, "Xóa khuyến mãi thành công", null);
        } catch (Exception e) {
            return new ApiResponse<>(false, e.getMessage(), null);
        }
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
        
        // Điều kiện áp dụng
        promotionMap.put("minOrderValue", promotion.getMinOrderValue());
        promotionMap.put("minWeight", promotion.getMinWeight());
        promotionMap.put("maxWeight", promotion.getMaxWeight());
        promotionMap.put("minOrdersCount", promotion.getMinOrdersCount());
        promotionMap.put("firstTimeUser", promotion.getFirstTimeUser());
        promotionMap.put("validMonthsAfterJoin", promotion.getValidMonthsAfterJoin());
        promotionMap.put("validYearsAfterJoin", promotion.getValidYearsAfterJoin());
        
        // ServiceTypes
        if (promotion.getServiceTypes() != null) {
            promotionMap.put("serviceTypeIds", promotion.getServiceTypes().stream()
                    .map(ServiceType::getId)
                    .collect(Collectors.toList()));
        }
        
        // Giới hạn sử dụng
        promotionMap.put("usageLimit", promotion.getUsageLimit());
        promotionMap.put("maxUsagePerUser", promotion.getMaxUsagePerUser());
        promotionMap.put("dailyUsageLimitGlobal", promotion.getDailyUsageLimitGlobal());
        promotionMap.put("dailyUsageLimitPerUser", promotion.getDailyUsageLimitPerUser());
        promotionMap.put("usedCount", promotion.getUsedCount());
        promotionMap.put("status", promotion.getStatus().name());
        promotionMap.put("createdAt", promotion.getCreatedAt());
        promotionMap.put("updatedAt", promotion.getUpdatedAt());
        
        // UserPromotions nếu không phải global
        if (!promotion.getIsGlobal() && promotion.getUserPromotions() != null) {
            promotionMap.put("userIds", promotion.getUserPromotions().stream()
                    .filter(up -> up.getUser() != null && up.getUser().getId() != null)
                    .map(up -> up.getUser().getId())
                    .collect(Collectors.toList()));
        }
        
        return promotionMap;
    }
}


