package com.logistics.mapper;

import java.util.List;
import java.util.stream.Collectors;

import com.logistics.dto.user.UserSettlementTransactionDto;
import com.logistics.entity.SettlementTransaction;

public class SettlementTransactionMapper {

    public static UserSettlementTransactionDto toUserSettlementTransactionDto(SettlementTransaction entity) {
        if (entity == null) return null;

        UserSettlementTransactionDto dto = new UserSettlementTransactionDto();

        dto.setId(entity.getId());
        dto.setCode(entity.getCode());
        dto.setAmount(entity.getAmount());
        dto.setType(entity.getType().name());
        dto.setStatus(entity.getStatus().name());
        dto.setBankName(entity.getBankName());
        dto.setAccountName(entity.getAccountName());
        dto.setAccountNumber(entity.getAccountNumber());
        dto.setPaidAt(entity.getPaidAt());

        return dto;
    }

    public static List<UserSettlementTransactionDto>
        toUserSettlementBatchDetailDtoList(List<SettlementTransaction> entities) {

        if (entities == null || entities.isEmpty()) return List.of();

        return entities.stream()
                .map(SettlementTransactionMapper::toUserSettlementTransactionDto)
                .collect(Collectors.toList());
    }

}