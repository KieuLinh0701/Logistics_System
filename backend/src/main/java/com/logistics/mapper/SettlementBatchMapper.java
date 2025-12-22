package com.logistics.mapper;

import java.math.BigDecimal;

import com.logistics.dto.user.UserSettlementBatchListDto;
import com.logistics.entity.SettlementBatch;

public class SettlementBatchMapper {

    public static UserSettlementBatchListDto toListDtos(SettlementBatch entity,
    BigDecimal remainAmount) {
        if (entity == null)
            return null;

        UserSettlementBatchListDto dto = new UserSettlementBatchListDto();

        dto.setId(entity.getId());
        dto.setCode(entity.getCode());
        dto.setBalanceAmount(entity.getBalanceAmount());
        dto.setStatus(entity.getStatus().name());
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setUpdatedAt(entity.getUpdatedAt());
        dto.setRemainAmount(remainAmount);

        return dto;
    }
}