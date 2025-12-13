package com.logistics.service.common;

import com.logistics.entity.FeeConfiguration;
import com.logistics.entity.Region;
import com.logistics.entity.ShippingRate;
import com.logistics.enums.CodFeeType;
import com.logistics.enums.FeeType;
import com.logistics.enums.RegionName;
import com.logistics.enums.ShippingRateRegionType;
import com.logistics.repository.FeeConfigurationRepository;
import com.logistics.repository.RegionRepository;
import com.logistics.repository.ShippingRateRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class FeePublicService {

    private final RegionRepository regionRepository;
    private final ShippingRateRepository shippingRateRepository;
    private final FeeConfigurationRepository feeConfigRepository;

    public Integer calculateShippingFee(BigDecimal weight, Integer serviceTypeId, Integer senderCodeCity,
            Integer recipientCodeCity) {
        Optional<Region> senderOpt = regionRepository.findByCodeCity(senderCodeCity);
        Optional<Region> recipientOpt = regionRepository.findByCodeCity(recipientCodeCity);

        if (senderOpt.isEmpty() || recipientOpt.isEmpty()) {
            throw new IllegalArgumentException("Không tìm thấy thông tin vùng của người gửi hoặc người nhận");
        }

        Region senderRegion = senderOpt.get();
        Region recipientRegion = recipientOpt.get();

        ShippingRateRegionType regionType = determineRegionType(senderRegion, recipientRegion, senderCodeCity,
                recipientCodeCity);
        List<ShippingRate> rates = shippingRateRepository
                .findByServiceType_IdAndRegionTypeOrderByWeightFromAsc(serviceTypeId, regionType);

        if (rates.isEmpty()) {
            throw new IllegalArgumentException("Không tìm thấy bảng giá cho loại dịch vụ này");
        }

        ShippingRate selectedRate = null;
        for (ShippingRate rate : rates) {
            if (rate.getWeightTo() != null) {
                if (weight.compareTo(rate.getWeightFrom()) > 0 && weight.compareTo(rate.getWeightTo()) <= 0) {
                    selectedRate = rate;
                    break;
                }
            } else {
                if (weight.compareTo(rate.getWeightFrom()) > 0) {
                    selectedRate = rate;
                    break;
                }
            }
        }

        if (selectedRate == null) {
            throw new IllegalArgumentException("Không tìm thấy mức giá phù hợp cho cân nặng");
        }

        BigDecimal shippingFee = selectedRate.getPrice();

        if (selectedRate.getWeightTo() == null && selectedRate.getExtraPrice() != null) {
            BigDecimal extraWeight = weight.subtract(selectedRate.getWeightFrom());
            BigDecimal unit = selectedRate.getUnit() != null ? selectedRate.getUnit() : new BigDecimal("0.5");
            BigDecimal extraSteps = extraWeight.divide(unit, 0, RoundingMode.UP);
            shippingFee = shippingFee.add(extraSteps.multiply(selectedRate.getExtraPrice()));
        }

        return shippingFee.setScale(0, RoundingMode.HALF_UP).intValue();
    }

    public Integer calculateTotalFee(BigDecimal weight, Integer serviceTypeId,
            Integer senderCodeCity, Integer recipientCodeCity,
            Integer orderValueInt, Integer codAmountInt) {

        BigDecimal codAmount = codAmountInt != null ? BigDecimal.valueOf(codAmountInt) : BigDecimal.ZERO;
        BigDecimal orderValue = orderValueInt != null ? BigDecimal.valueOf(orderValueInt) : BigDecimal.ZERO;

        Integer shippingFeeInt = calculateShippingFee(weight, serviceTypeId, senderCodeCity, recipientCodeCity);
        BigDecimal shippingFee = BigDecimal.valueOf(shippingFeeInt);

        List<FeeConfiguration> fees = feeConfigRepository.findActiveByServiceTypeIdIncludingNull(serviceTypeId);

        BigDecimal codFee = BigDecimal.ZERO;
        BigDecimal insuranceFee = BigDecimal.ZERO;
        BigDecimal vatFee = BigDecimal.ZERO;

        for (FeeConfiguration f : fees) {
            switch (f.getFeeType()) {
                case COD:
                    if (codAmount.compareTo(BigDecimal.ZERO) > 0) {
                        codFee = calculateFee(f, codAmount);
                    }
                    break;
                case INSURANCE:
                    if (orderValue.compareTo(BigDecimal.ZERO) > 0) {
                        insuranceFee = calculateFee(f, orderValue);
                    }
                    break;
                case VAT:
                    break;
                case PACKAGING:
                    break;
            }
        }

        BigDecimal subtotal = shippingFee.add(codFee).add(insuranceFee);
        if (subtotal.compareTo(BigDecimal.ZERO) < 0)
            subtotal = BigDecimal.ZERO;

        Optional<FeeConfiguration> vatConfigOpt = fees.stream()
                .filter(f -> f.getFeeType() == FeeType.VAT)
                .findFirst();
        if (vatConfigOpt.isPresent()) {
            vatFee = calculateFee(vatConfigOpt.get(), subtotal);
        }

        BigDecimal totalFee = subtotal.add(vatFee);

        return totalFee.setScale(0, RoundingMode.HALF_UP).intValue();
    }

    public Integer calculateTotalFeeManager(BigDecimal weight, Integer serviceTypeId,
            Integer senderCodeCity, Integer recipientCodeCity,
            Integer orderValueInt, Integer codAmountInt) {

        BigDecimal codAmount = codAmountInt != null ? BigDecimal.valueOf(codAmountInt) : BigDecimal.ZERO;
        BigDecimal orderValue = orderValueInt != null ? BigDecimal.valueOf(orderValueInt) : BigDecimal.ZERO;

        Integer shippingFeeInt = calculateShippingFee(weight, serviceTypeId, senderCodeCity, recipientCodeCity);
        BigDecimal shippingFee = BigDecimal.valueOf(shippingFeeInt);

        List<FeeConfiguration> fees = feeConfigRepository.findActiveByServiceTypeIdIncludingNull(serviceTypeId);

        BigDecimal codFee = BigDecimal.ZERO;
        BigDecimal insuranceFee = BigDecimal.ZERO;
        BigDecimal packaging = BigDecimal.ZERO;
        BigDecimal vatFee = BigDecimal.ZERO;

        for (FeeConfiguration f : fees) {
            switch (f.getFeeType()) {
                case COD:
                    if (codAmount.compareTo(BigDecimal.ZERO) > 0) {
                        codFee = calculateFee(f, codAmount);
                    }
                    break;
                case INSURANCE:
                    if (orderValue.compareTo(BigDecimal.ZERO) > 0) {
                        insuranceFee = calculateFee(f, orderValue);
                    }
                    break;
                case VAT:
                    break;
                case PACKAGING:
                    packaging = f.getFeeValue() != null ? f.getFeeValue() : BigDecimal.ZERO;
                    break;
            }
        }

        BigDecimal subtotal = shippingFee.add(codFee).add(insuranceFee).add(packaging);
        if (subtotal.compareTo(BigDecimal.ZERO) < 0)
            subtotal = BigDecimal.ZERO;

        Optional<FeeConfiguration> vatConfigOpt = fees.stream()
                .filter(f -> f.getFeeType() == FeeType.VAT)
                .findFirst();
        if (vatConfigOpt.isPresent()) {
            vatFee = calculateFee(vatConfigOpt.get(), subtotal);
        }

        BigDecimal totalFee = subtotal.add(vatFee);

        return totalFee.setScale(0, RoundingMode.HALF_UP).intValue();
    }

    private BigDecimal calculateFee(FeeConfiguration f, BigDecimal base) {
        BigDecimal result;
        if (f.getCalculationType() == CodFeeType.FIXED) {
            result = f.getFeeValue();
        } else {
            result = base.multiply(f.getFeeValue()).divide(BigDecimal.valueOf(100), 10, RoundingMode.HALF_UP);
        }
        if (f.getMinOrderFee() != null)
            result = result.max(f.getMinOrderFee());
        if (f.getMaxOrderFee() != null)
            result = result.min(f.getMaxOrderFee());
        return result.setScale(0, RoundingMode.HALF_UP);
    }

    private ShippingRateRegionType determineRegionType(Region sender, Region recipient, Integer senderCode,
            Integer recipientCode) {
        if (senderCode.equals(recipientCode)) {
            return ShippingRateRegionType.INTRA_CITY;
        } else if (sender.getRegionName() == recipient.getRegionName()) {
            return ShippingRateRegionType.INTRA_REGION;
        } else if (isNearRegion(sender.getRegionName(), recipient.getRegionName())) {
            return ShippingRateRegionType.NEAR_REGION;
        } else {
            return ShippingRateRegionType.INTER_REGION;
        }
    }

    private boolean isNearRegion(RegionName sender, RegionName recipient) {
        return (sender == RegionName.NORTH && recipient == RegionName.CENTRAL)
                || (sender == RegionName.CENTRAL && recipient == RegionName.NORTH)
                || (sender == RegionName.CENTRAL && recipient == RegionName.SOUTH)
                || (sender == RegionName.SOUTH && recipient == RegionName.CENTRAL);
    }
}