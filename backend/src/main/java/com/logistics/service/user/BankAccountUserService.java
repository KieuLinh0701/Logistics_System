package com.logistics.service.user;

import com.logistics.dto.BankAccountDto;
import com.logistics.entity.BankAccount;
import com.logistics.entity.User;
import com.logistics.mapper.BankAccountMapper;
import com.logistics.repository.BankAccountRepository;
import com.logistics.repository.UserRepository;
import com.logistics.request.user.bankAccount.BankAccountRequest;
import com.logistics.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class BankAccountUserService {

    private final BankAccountRepository repository;
    private final UserRepository userRepository;

    public ApiResponse<List<BankAccountDto>> list(int userId) {
        try {
            List<BankAccount> accounts = repository.findByUserIdOrderByCreatedAtDesc(userId);
            List<BankAccountDto> data = accounts.stream()
                    .map(BankAccountMapper::toDto)
                    .toList();
            return new ApiResponse<>(true, "Lấy danh sách thành công", data);
        } catch (Exception e) {
            return new ApiResponse<>(false, "Lỗi: " + e.getMessage(), null);
        }
    }

    @Transactional
    public ApiResponse<BankAccountDto> create(int userId, BankAccountRequest request) {
        try {
            validateForm(request);
            long count = repository.countByUserId(userId);
            if (count >= 5) {
                return new ApiResponse<>(false, "Chỉ được tạo tối đa 5 tài khoản ngân hàng", null);
            }
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng"));

            BankAccount account = new BankAccount();
            account.setUser(user);
            account.setBankName(request.getBankName());
            account.setAccountNumber(request.getAccountNumber());
            account.setAccountName(request.getAccountName());
            account.setNotes(request.getNotes());

            if (request.getIsDefault() != null && request.getIsDefault()) {
                repository.clearDefaultExcept(userId, -1);
                account.setIsDefault(true);
            } else if (count == 0) {
                account.setIsDefault(true);
            }

            repository.save(account);
            return new ApiResponse<>(true, "Thêm tài khoản thành công", BankAccountMapper.toDto(account));
        } catch (Exception e) {
            return new ApiResponse<>(false, e.getMessage(), null);
        }
    }

    @Transactional
    public ApiResponse<BankAccountDto> update(int userId, int id, BankAccountRequest request) {
        try {
            validateForm(request);
            BankAccount account = repository.findByIdAndUserId(id, userId)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy tài khoản ngân hàng"));

            account.setBankName(request.getBankName());
            account.setAccountNumber(request.getAccountNumber());
            account.setAccountName(request.getAccountName());
            account.setNotes(request.getNotes());

            if (request.getIsDefault() != null && request.getIsDefault() && !account.getIsDefault()) {
                repository.clearDefaultExcept(userId, id);
                account.setIsDefault(true);
            }

            repository.save(account);
            return new ApiResponse<>(true, "Cập nhật tài khoản thành công", BankAccountMapper.toDto(account));
        } catch (Exception e) {
            return new ApiResponse<>(false, e.getMessage(), null);
        }
    }

    public ApiResponse<Boolean> delete(int userId, int id) {
        try {
            BankAccount account = repository.findByIdAndUserId(id, userId)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy tài khoản ngân hàng"));
            if (account.getIsDefault()) {
                return new ApiResponse<>(false, "Vui lòng chọn tài khoản mặc định khác trước khi xóa", false);
            }
            repository.delete(account);
            return new ApiResponse<>(true, "Xóa tài khoản thành công", true);
        } catch (Exception e) {
            return new ApiResponse<>(false, e.getMessage(), false);
        }
    }

    @Transactional
    public ApiResponse<Boolean> setDefault(int userId, int id) {
        try {
            BankAccount account = repository.findByIdAndUserId(id, userId)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy tài khoản ngân hàng"));
            repository.clearDefaultExcept(userId, id);
            account.setIsDefault(true);
            repository.save(account);
            return new ApiResponse<>(true, "Đặt tài khoản mặc định thành công", true);
        } catch (Exception e) {
            return new ApiResponse<>(false, e.getMessage(), false);
        }
    }

    private void validateForm(BankAccountRequest request) {
        List<String> missingFields = new ArrayList<>();
        if (request.getBankName() == null || request.getBankName().isBlank()) missingFields.add("Tên ngân hàng");
        if (request.getAccountNumber() == null || request.getAccountNumber().isBlank()) missingFields.add("Số tài khoản");
        if (request.getAccountName() == null || request.getAccountName().isBlank()) missingFields.add("Tên chủ tài khoản");
        if (!missingFields.isEmpty()) throw new RuntimeException("Thiếu thông tin: " + String.join(", ", missingFields));
    }
}