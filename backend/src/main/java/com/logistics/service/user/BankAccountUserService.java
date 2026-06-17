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
    private final UserUserService userService;

    public ApiResponse<List<BankAccountDto>> list(int userId) {
        try {
            Integer shopId = userService.getShopId(userId);

            List<BankAccount> accounts = repository.findByUserIdOrderByCreatedAtDesc(shopId);

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

            Integer shopId = userService.getShopId(userId);

            long count = repository.countByUserId(shopId);
            if (count >= 5) {
                return new ApiResponse<>(false, "Chỉ được tạo tối đa 5 tài khoản ngân hàng", null);
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
            return new ApiResponse<>(true, "Thêm tài khoản thành công", BankAccountMapper.toDto(account));
        } catch (Exception e) {
            return new ApiResponse<>(false, e.getMessage(), null);
        }
    }

    @Transactional
    public ApiResponse<BankAccountDto> update(int userId, int id, BankAccountRequest request) {
        try {
            validateForm(request);

            Integer shopId = userService.getShopId(userId);

            BankAccount account = repository.findByIdAndUserId(id, shopId)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy tài khoản ngân hàng"));

            account.setBankName(request.getBankName());
            account.setAccountNumber(request.getAccountNumber());
            account.setAccountName(request.getAccountName());
            account.setNotes(request.getNotes());

            if (request.getIsDefault() != null && request.getIsDefault() && !account.getIsDefault()) {
                repository.clearDefaultExcept(shopId, id);
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
            Integer shopId = userService.getShopId(userId);

            BankAccount account = repository.findByIdAndUserId(id, shopId)
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
            Integer shopId = userService.getShopId(userId);

            BankAccount account = repository.findByIdAndUserId(id, shopId)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy tài khoản ngân hàng"));

            repository.clearDefaultExcept(shopId, id);
            account.setIsDefault(true);
            repository.save(account);
            return new ApiResponse<>(true, "Đặt tài khoản mặc định thành công", true);
        } catch (Exception e) {
            return new ApiResponse<>(false, e.getMessage(), false);
        }
    }

    public ApiResponse<Boolean> hasBankAccount(int userId) {
        try {
            Integer shopId = userService.getShopId(userId);

            boolean exist = repository.existsByUserId(shopId);

            return new ApiResponse<>(true, "Kiểm tra tài khoản thành công", exist);
        } catch (Exception e) {
            return new ApiResponse<>(false, "Lỗi: " + e.getMessage(), null);
        }
    }

    private void validateForm(BankAccountRequest request) {
        List<String> missingFields = new ArrayList<>();
        if (request.getBankName() == null || request.getBankName().isBlank())
            missingFields.add("Tên ngân hàng");
        if (request.getAccountNumber() == null || request.getAccountNumber().isBlank())
            missingFields.add("Số tài khoản");
        if (request.getAccountName() == null || request.getAccountName().isBlank())
            missingFields.add("Tên chủ tài khoản");
        if (!missingFields.isEmpty())
            throw new RuntimeException("Thiếu thông tin: " + String.join(", ", missingFields));
    }
}