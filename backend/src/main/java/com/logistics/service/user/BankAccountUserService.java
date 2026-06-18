package com.logistics.service.user;

import com.logistics.dto.BankAccountDto;
import com.logistics.entity.BankAccount;
import com.logistics.entity.User;
import com.logistics.exception.AppException;
import com.logistics.exception.ErrorCode;
import com.logistics.mapper.BankAccountMapper;
import com.logistics.repository.BankAccountRepository;
import com.logistics.request.user.bankAccount.BankAccountRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class BankAccountUserService {

    private final BankAccountRepository repository;
    private final UserUserService userService;

    public List<BankAccountDto> list(int userId) {
        Integer shopId = userService.getShopId(userId);

        List<BankAccount> accounts = repository.findByUserIdOrderByCreatedAtDesc(shopId);

        return accounts.stream()
                .map(BankAccountMapper::toDto)
                .toList();
    }

    @Transactional
    public BankAccountDto create(int userId, BankAccountRequest request) {
        Integer shopId = userService.getShopId(userId);

        long count = repository.countByUserId(shopId);
        if (count >= 5) {
            throw new AppException(ErrorCode.BANK_ACCOUNT_LIMIT_REACHED);
        }
        User user = userService.getUser(shopId);

        BankAccount account = new BankAccount();
        account.setUser(user);
        account.setBankName(request.getBankName());
        account.setAccountNumber(request.getAccountNumber());
        account.setAccountName(request.getAccountName());
        account.setNotes(request.getNotes());

        if (request.getIsDefault() != null && request.getIsDefault()) {
            repository.clearDefaultExcept(shopId, -1);
            account.setIsDefault(true);
        } else if (count == 0) {
            account.setIsDefault(true);
        }

        repository.save(account);
        return BankAccountMapper.toDto(account);
    }

    @Transactional
    public BankAccountDto update(int userId, int id, BankAccountRequest request) {
        Integer shopId = userService.getShopId(userId);

        BankAccount account = getBankAccount(id, shopId);

        account.setBankName(request.getBankName());
        account.setAccountNumber(request.getAccountNumber());
        account.setAccountName(request.getAccountName());
        account.setNotes(request.getNotes());

        if (request.getIsDefault() != null && request.getIsDefault() && !account.getIsDefault()) {
            repository.clearDefaultExcept(shopId, id);
            account.setIsDefault(true);
        }

        repository.save(account);
        return BankAccountMapper.toDto(account);
    }

    public void delete(int userId, int id) {
        Integer shopId = userService.getShopId(userId);

        BankAccount account = getBankAccount(id, shopId);

        if (account.getIsDefault()) {
            throw new AppException(ErrorCode.BANK_ACCOUNT_IS_DEFAULT);
        }

        repository.delete(account);
    }

    @Transactional
    public void setDefault(int userId, int id) {
        Integer shopId = userService.getShopId(userId);

        BankAccount account = getBankAccount(id, shopId);

        repository.clearDefaultExcept(shopId, id);
        account.setIsDefault(true);
        repository.save(account);
    }

    public Boolean hasBankAccount(int userId) {
        Integer shopId = userService.getShopId(userId);
        return repository.existsByUserId(shopId);
    }

    private BankAccount getBankAccount(Integer id, Integer shopId) {
        return repository.findByIdAndUserId(id, shopId)
                .orElseThrow(() -> new AppException(ErrorCode.BANK_ACCOUNT_NOT_FOUND));
    }
}