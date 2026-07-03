package com.logistics.service.user;

import com.logistics.dto.BankAccountDto;
import com.logistics.request.user.bankAccount.BankAccountRequest;

import java.util.List;

public interface BankAccountUserService {
    List<BankAccountDto> list(int userId);
    BankAccountDto create(int userId, BankAccountRequest request);
    BankAccountDto update(int userId, int id, BankAccountRequest request);
    void delete(int userId, int id);
    void setDefault(int userId, int id);
    Boolean hasBankAccount(int userId);
}