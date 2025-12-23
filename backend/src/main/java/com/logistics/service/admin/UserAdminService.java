package com.logistics.service.admin;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.logistics.request.admin.CreateUserRequest;
import com.logistics.request.admin.UpdateUserRequest;
import com.logistics.entity.Account;
import com.logistics.entity.Role;
import com.logistics.entity.User;
import com.logistics.repository.AccountRepository;
import com.logistics.repository.RoleRepository;
import com.logistics.repository.UserRepository;
import com.logistics.response.ApiResponse;
import com.logistics.response.Pagination;
import com.logistics.utils.PasswordUtils;

@Service
public class UserAdminService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private RoleRepository roleRepository;

    public ApiResponse<Map<String, Object>> listUsers(int page, int limit, String search) {
        try {
            Pageable pageable = PageRequest.of(page - 1, limit, Sort.by("createdAt").descending());
            Page<Account> accountPage;

            if (search != null && !search.trim().isEmpty()) {
                accountPage = accountRepository
                        .findByEmailContainingOrUserFirstNameContainingOrUserLastNameContainingOrUserPhoneNumberContaining(
                                search, search, search, search, pageable);
            } else {
                accountPage = accountRepository.findAll(pageable);
            }

            List<Map<String, Object>> users = accountPage.getContent().stream().map(this::mapAccount)
                    .collect(Collectors.toList());

            Pagination pagination = new Pagination(
                    (int) accountPage.getTotalElements(),
                    page,
                    limit,
                    accountPage.getTotalPages());

            Map<String, Object> result = new HashMap<>();
            result.put("data", users);
            result.put("pagination", pagination);

            return new ApiResponse<>(true, "Lấy danh sách người dùng thành công", result);
        } catch (Exception e) {
            return new ApiResponse<>(false, "Lỗi: " + e.getMessage(), null);
        }
    }

    public ApiResponse<Map<String, Object>> getUserById(Integer userId) {
        try {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng"));

            Account account = accountRepository.findByUser(user)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy tài khoản"));

            return new ApiResponse<>(true, "Lấy thông tin người dùng thành công", mapAccount(account));
        } catch (Exception e) {
            return new ApiResponse<>(false, e.getMessage(), null);
        }
    }

    @Transactional
    public ApiResponse<Map<String, Object>> createUser(CreateUserRequest request) {
        try {
            if (accountRepository.findByEmail(request.getEmail()).isPresent()) {
                return new ApiResponse<>(false, "Email đã tồn tại", null);
            }

            if (userRepository.existsByPhoneNumber(request.getPhoneNumber())) {
                return new ApiResponse<>(false, "Số điện thoại đã tồn tại", null);
            }

            Role role = roleRepository.findById(request.getRoleId())
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy role"));

            Account account = new Account();
            account.setEmail(request.getEmail());
            account.setPassword(PasswordUtils.hashPassword(request.getPassword()));

            // Chỉnh này
            // account.setRole(role);
            account.setIsActive(request.getIsActive() != null ? request.getIsActive() : true);
            account.setIsVerified(false);
            account = accountRepository.save(account);

            User user = new User();
            user.setAccount(account);
            user.setFirstName(request.getFirstName());
            user.setLastName(request.getLastName());
            user.setPhoneNumber(request.getPhoneNumber());
            user = userRepository.save(user);

            return new ApiResponse<>(true, "Tạo người dùng thành công", mapAccount(account));
        } catch (Exception e) {
            return new ApiResponse<>(false, e.getMessage(), null);
        }
    }

    @Transactional
    public ApiResponse<Map<String, Object>> updateUser(Integer userId, UpdateUserRequest request) {
        try {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng"));

            Account account = accountRepository.findByUser(user)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy tài khoản"));

            if (request.getFirstName() != null)
                user.setFirstName(request.getFirstName());
            if (request.getLastName() != null)
                user.setLastName(request.getLastName());
            if (request.getPhoneNumber() != null) {
                if (!user.getPhoneNumber().equals(request.getPhoneNumber())
                        && userRepository.existsByPhoneNumber(request.getPhoneNumber())) {
                    return new ApiResponse<>(false, "Số điện thoại đã tồn tại", null);
                }
                user.setPhoneNumber(request.getPhoneNumber());
            }
            userRepository.save(user);

            if (request.getPassword() != null && !request.getPassword().trim().isEmpty()) {
                account.setPassword(PasswordUtils.hashPassword(request.getPassword()));
            }
            if (request.getRoleId() != null) {
                Role role = roleRepository.findById(request.getRoleId())
                        .orElseThrow(() -> new RuntimeException("Không tìm thấy role"));
                // Chỉnh này
                // account.setRole(role);
            }
            if (request.getIsActive() != null) {
                account.setIsActive(request.getIsActive());
            }
            accountRepository.save(account);

            return new ApiResponse<>(true, "Cập nhật người dùng thành công", mapAccount(account));
        } catch (Exception e) {
            return new ApiResponse<>(false, e.getMessage(), null);
        }
    }

    @Transactional
    public ApiResponse<String> deleteUser(Integer userId) {
        try {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng"));

            Account account = accountRepository.findByUser(user)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy tài khoản"));

            userRepository.delete(user);
            accountRepository.delete(account);

            return new ApiResponse<>(true, "Xóa người dùng thành công", null);
        } catch (Exception e) {
            return new ApiResponse<>(false, e.getMessage(), null);
        }
    }

    private Map<String, Object> mapAccount(Account account) {
        Map<String, Object> userMap = new HashMap<>();
        userMap.put("id", account.getUser() != null ? account.getUser().getId() : null);
        userMap.put("email", account.getEmail());
        if (account.getUser() != null) {
            userMap.put("firstName", account.getUser().getFirstName());
            userMap.put("lastName", account.getUser().getLastName());
            userMap.put("phoneNumber", account.getUser().getPhoneNumber());
            userMap.put("images", account.getUser().getImages());
            userMap.put("createdAt", account.getUser().getCreatedAt());
            userMap.put("updatedAt", account.getUser().getUpdatedAt());
        }
        // Chỉnh này
        // userMap.put("role", account.getRole() != null ? account.getRole().getName() : null);
        // userMap.put("roleId", account.getRole() != null ? account.getRole().getId() : null);
        userMap.put("isActive", account.getIsActive());
        userMap.put("isVerified", account.getIsVerified());
        return userMap;
    }
}
