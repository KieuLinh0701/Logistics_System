package com.logistics.service.common;

import com.logistics.entity.Region;
import com.logistics.entity.ShippingRate;
import com.logistics.enums.RegionName;
import com.logistics.enums.ShippingRateRegionType;
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
public class ShippingFeeService {

    private final RegionRepository regionRepository;
    private final ShippingRateRepository shippingRateRepository;

    public BigDecimal calculateShippingFee(BigDecimal weight, Integer serviceTypeId, Integer senderCodeCity, Integer recipientCodeCity) {
        Optional<Region> senderOpt = regionRepository.findByCodeCity(senderCodeCity);
        Optional<Region> recipientOpt = regionRepository.findByCodeCity(recipientCodeCity);

        if (senderOpt.isEmpty() || recipientOpt.isEmpty()) {
            throw new IllegalArgumentException("Không tìm thấy thông tin vùng của người gửi hoặc người nhận");
        }

        Region senderRegion = senderOpt.get();
        Region recipientRegion = recipientOpt.get();

        ShippingRateRegionType regionType = determineRegionType(senderRegion, recipientRegion, senderCodeCity, recipientCodeCity);
        List<ShippingRate> rates = shippingRateRepository.findByServiceType_IdAndRegionTypeOrderByWeightFromAsc(serviceTypeId, regionType);

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

        return shippingFee;
    }

    private ShippingRateRegionType determineRegionType(Region sender, Region recipient, Integer senderCode, Integer recipientCode) {
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