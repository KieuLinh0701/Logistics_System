package com.logistics.service.common;

import java.math.BigDecimal;

public interface FeePublicService {
    Integer calculateShippingFee(BigDecimal weight, Integer serviceTypeId, Integer senderCodeCity, Integer recipientCodeCity);

    Integer calculateTotalFee(BigDecimal weight, Integer serviceTypeId, Integer senderCodeCity,
                              Integer recipientCodeCity, Integer orderValueInt, Integer codAmountInt);

    Integer calculateTotalFeeManager(BigDecimal weight, Integer serviceTypeId, Integer senderCodeCity,
                                     Integer recipientCodeCity, Integer orderValueInt, Integer codAmountInt);

    BigDecimal calculateWeight(BigDecimal originalWeight, BigDecimal height, BigDecimal length, BigDecimal width);
}