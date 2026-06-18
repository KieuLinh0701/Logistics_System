package com.logistics.service.user;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

import com.logistics.exception.AppException;
import com.logistics.exception.enums.PromotionErrorCode;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import com.logistics.dto.user.UserPromotionDto;
import com.logistics.entity.Promotion;
import com.logistics.entity.User;
import com.logistics.entity.UserPromotion;
import com.logistics.enums.OrderStatus;
import com.logistics.enums.PromotionDiscountType;
import com.logistics.enums.PromotionStatus;
import com.logistics.mapper.PromotionMapper;
import com.logistics.repository.OrderRepository;
import com.logistics.repository.PromotionRepository;
import com.logistics.repository.UserPromotionRepository;
import com.logistics.repository.UserRepository;
import com.logistics.request.user.promotion.PromotionUserRequest;
import com.logistics.response.ListResponse;
import com.logistics.response.Pagination;
import com.logistics.specification.PromotionSpecification;

import jakarta.transaction.Transactional;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PromotionUserService {

    private final PromotionRepository promotionRepo;
    private final UserPromotionRepository userPromotionRepo;
    private final OrderRepository orderRepo;
    private final UserRepository userRepo;
    private final UserUserService userService;

    public ListResponse<UserPromotionDto> getActiveUserPromotions(@NonNull Integer userId,
            PromotionUserRequest req) {
            Integer shopId = userService.getShopId(userId);

            int page = req.getPage() != null ? req.getPage() - 1 : 0;
            int limit = req.getLimit() != null ? req.getLimit() : 10;

            Sort sort = Sort.by("startDate").descending();
            Pageable pageable = PageRequest.of(page, limit, sort);

            Page<Promotion> promotionsPage = promotionRepo.findAll(
                    PromotionSpecification.activeAndUsable(), pageable);

            User user = userService.getUser(shopId);

            // Đếm tất cả các đơn đã tạo của user, trừ đơn bị hủy
            long nonCanceledOrders = orderRepo.countByUserIdAndStatusNot(shopId, OrderStatus.CANCELLED);

            List<UserPromotionDto> result = promotionsPage.getContent().stream()
                    .filter(p -> isPromotionValidForUser(p, user, nonCanceledOrders, req))
                    .filter(p -> filterBySearch(p, req.getSearch()))
                    .map(PromotionMapper::toUserPromotionDto)
                    .toList();

            Pagination pagination = new Pagination(
                    (int) promotionsPage.getTotalElements(),
                    page + 1,
                    limit,
                    promotionsPage.getTotalPages());

            return new ListResponse<>(result, pagination);
    }

    private boolean filterBySearch(Promotion p, String search) {
        if (search == null || search.isBlank())
            return true;
        String keyword = search.trim().toLowerCase();
        return p.getCode() != null && p.getCode().toLowerCase().contains(keyword);
    }

    private boolean isPromotionValidForUser(Promotion p, User user,
            long nonCanceledOrders,
            PromotionUserRequest req) {
        LocalDateTime now = LocalDateTime.now();

        if (p.getStatus() != PromotionStatus.ACTIVE)
            return false;

        // 1. Thời gian hiệu lực
        if (now.isBefore(p.getStartDate()) || now.isAfter(p.getEndDate()))
            return false;

        // 2. Kiểm tra global / user-specific
        if (!p.getIsGlobal()) {
            UserPromotion up = userPromotionRepo.findByUserIdAndPromotionId(user.getId(), p.getId());
            if (up == null)
                return false;
            if (p.getMaxUsagePerUser() != null && up.getUsedCount() >= p.getMaxUsagePerUser())
                return false;
        }

        // 3. Giới hạn tổng số lượt sử dụng
        if (p.getUsageLimit() != null && p.getUsedCount() >= p.getUsageLimit())
            return false;

        // 4. Giới hạn lượt sử dụng theo ngày
        if (p.getDailyUsageLimitGlobal() != null) {
            int todayUsed = promotionRepo.countTodayUsageGlobal(p.getId());
            if (todayUsed >= p.getDailyUsageLimitGlobal())
                return false;
        }
        if (p.getDailyUsageLimitPerUser() != null) {
            int todayUsedByUser = promotionRepo.countTodayUsageByUser(user.getId(), p.getId());
            if (todayUsedByUser >= p.getDailyUsageLimitPerUser())
                return false;
        }

        // 5. Điều kiện số đơn tối thiểu
        if (p.getMinOrdersCount() != null && nonCanceledOrders < p.getMinOrdersCount())
            return false;

        // 6. Dịch vụ áp dụng
        if (req.getServiceTypeId() != null && !p.getServiceTypes().isEmpty()) {
            boolean ok = p.getServiceTypes().stream()
                    .anyMatch(s -> s.getId().equals(req.getServiceTypeId()));
            if (!ok)
                return false;
        }

        // 7. Giá trị dịch vụ / trọng lượng
        if (req.getServiceFee() != null && p.getMinOrderValue() != null &&
                BigDecimal.valueOf(req.getServiceFee()).compareTo(p.getMinOrderValue()) < 0)
            return false;

        if (req.getWeight() != null) {
            if (p.getMinWeight() != null && req.getWeight().compareTo(p.getMinWeight()) < 0)
                return false;
            if (p.getMaxWeight() != null && req.getWeight().compareTo(p.getMaxWeight()) > 0)
                return false;
        }

        // 8. Khách hàng mới (first time user)
        if (Boolean.TRUE.equals(p.getFirstTimeUser()) && nonCanceledOrders > 0)
            return false;

        // 9. Giới hạn thời gian kể từ ngày đăng ký
        if (p.getValidMonthsAfterJoin() != null) {
            long months = ChronoUnit.MONTHS.between(user.getCreatedAt().toLocalDate(), LocalDate.now());
            if (months > p.getValidMonthsAfterJoin())
                return false;
        }
        if (p.getValidYearsAfterJoin() != null) {
            long years = ChronoUnit.YEARS.between(user.getCreatedAt().toLocalDate(), LocalDate.now());
            if (years > p.getValidYearsAfterJoin())
                return false;
        }

        return true;
    }

    public boolean canUsePromotion(Integer userId, Integer promotionId,
            Integer serviceTypeId, Integer orderValue, BigDecimal weight) {
        User user = userRepo.findById(userId).orElse(null);
        if (user == null)
            return false;

        Promotion promotion = promotionRepo.findById(promotionId).orElse(null);
        if (promotion == null)
            return false;

        long nonCanceledOrders = orderRepo.countByUserIdAndStatusNot(userId, OrderStatus.CANCELLED);

        PromotionUserRequest req = new PromotionUserRequest();
        req.setServiceTypeId(serviceTypeId);
        req.setServiceFee(orderValue);
        req.setWeight(weight);

        return isPromotionValidForUser(promotion, user, nonCanceledOrders, req);
    }

    public Optional<Promotion> findById(Integer id) {
        return promotionRepo.findById(id);
    }

    public int calculateDiscount(Promotion promotion, int serviceFee) {
        if (promotion == null || serviceFee <= 0)
            return 0;

        BigDecimal discount = BigDecimal.ZERO;
        BigDecimal fee = BigDecimal.valueOf(serviceFee);

        if (promotion.getDiscountType() == PromotionDiscountType.FIXED) {
            discount = promotion.getDiscountValue();
        } else if (promotion.getDiscountType() == PromotionDiscountType.PERCENTAGE) {
            discount = fee.multiply(promotion.getDiscountValue()).divide(BigDecimal.valueOf(100));
        }

        if (promotion.getMaxDiscountAmount() != null) {
            discount = discount.min(BigDecimal.valueOf(promotion.getMaxDiscountAmount()));
        }

        if (promotion.getMinOrderValue() != null &&
                fee.compareTo(promotion.getMinOrderValue()) < 0)
            return 0;

        return discount.intValue();
    }

    @Transactional
    public void increaseUsage(Integer promotionId, Integer userId) {
        if (promotionId == null)
            return;

        Promotion promotion = promotionRepo.findById(promotionId)
                .orElseThrow(() -> new AppException(PromotionErrorCode.PROMOTION_NOT_FOUND));

        promotion.setUsedCount(promotion.getUsedCount() + 1);
        promotionRepo.save(promotion);

        if (Boolean.FALSE.equals(promotion.getIsGlobal()) && userId != null) {
            UserPromotion up = userPromotionRepo.findByUserIdAndPromotionId(userId, promotionId);
            if (up == null)
                throw new AppException(PromotionErrorCode.PROMOTION_NOT_FOUND);

            up.setUsedCount(up.getUsedCount() + 1);
            userPromotionRepo.save(up);
        }
    }

    @Transactional
    public void decreaseUsage(Integer promotionId, Integer userId) {
        if (promotionId == null)
            return;

        Promotion promotion = promotionRepo.findById(promotionId)
                .orElseThrow(() -> new AppException(PromotionErrorCode.PROMOTION_NOT_FOUND));

        int newUsedCount = Math.max(0, promotion.getUsedCount() - 1);
        promotion.setUsedCount(newUsedCount);
        promotionRepo.save(promotion);

        if (Boolean.FALSE.equals(promotion.getIsGlobal()) && userId != null) {
            UserPromotion up = userPromotionRepo.findByUserIdAndPromotionId(userId, promotionId);
            if (up == null)
                throw new AppException(PromotionErrorCode.PROMOTION_NOT_FOUND);

            int newUserUsed = Math.max(0, up.getUsedCount() - 1);
            up.setUsedCount(newUserUsed);
            userPromotionRepo.save(up);
        }
    }
}