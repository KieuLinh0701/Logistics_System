package com.logistics.mapper;

import com.logistics.dto.BankAccountDto;
import com.logistics.entity.BankAccount;

public class BankAccountMapper {

    public static BankAccountDto toDto(BankAccount entity) {
        if (entity == null) {
            return null;
        }

        return new BankAccountDto(
                entity.getId(),
                entity.getBankName(),
                entity.getAccountNumber(),
                entity.getAccountName(),
                entity.getIsDefault(),
                entity.getNotes());
    }
}