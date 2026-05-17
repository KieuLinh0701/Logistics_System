package com.logistics.mapper;

import com.logistics.dto.user.settlement.UserSettlementBatchListDto;
import com.logistics.entity.SettlementBatch;

public class SettlementBatchMapper {

    public static UserSettlementBatchListDto toListDtos(SettlementBatch entity) {
        if (entity == null)
            return null;

        UserSettlementBatchListDto dto = new UserSettlementBatchListDto();

        dto.setId(entity.getId());
        dto.setCode(entity.getCode());
        dto.setBalanceAmount(entity.getBalanceAmount());
        dto.setStatus(entity.getStatus().name());
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setUpdatedAt(entity.getUpdatedAt());

        return dto;
    }
}