package com.logistics.service.common;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.logistics.entity.Account;
import com.logistics.entity.AccountRole;
import com.logistics.entity.OTP;
import com.logistics.entity.Role;
import com.logistics.entity.User;
import com.logistics.enums.OTPType;
import com.logistics.repository.AccountRepository;
import com.logistics.repository.AccountRoleRepository;
import com.logistics.repository.OTPRepository;
import com.logistics.repository.RoleRepository;
import com.logistics.repository.UserRepository;
import com.logistics.request.common.auth.ChooseRoleRequest;
import com.logistics.request.common.auth.ForgotPasswordEmailRequest;
import com.logistics.request.common.auth.ForgotPasswordResetRequest;
import com.logistics.request.common.auth.LoginRequest;
import com.logistics.request.common.auth.RegisterRequest;
import com.logistics.request.common.auth.VerifyRegisterOtpRequest;
import com.logistics.request.common.auth.VerifyResetOtpRequest;
import com.logistics.response.ApiResponse;
import com.logistics.response.AuthResponse;
import com.logistics.utils.EmailService;
import com.logistics.utils.JwtUtils;
import com.logistics.utils.OTPUtils;
import com.logistics.utils.PasswordUtils;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuthService {

        @Autowired
        private JwtUtils jwtUtils;

        @Autowired
        private PasswordEncoder passwordEncoder;

        private final RoleRepository roleRepository;
        private final AccountRepository accountRepository;
        private final AccountRoleRepository accountRoleRepository;
        private final UserRepository userRepository;
        private final OTPRepository otpRepository;
        private final EmailService emailService;
        private final NotificationService notificationService;

        public ApiResponse<String> register(RegisterRequest request) {
                try {
                        Optional<Account> existingAccountOpt = accountRepository.findByEmail(request.getEmail());

                        if (existingAccountOpt.isPresent()) {
                                Account existingAccount = existingAccountOpt.get();

                                boolean hasUserRole = existingAccount.getAccountRoles()
                                                .stream()
                                                .anyMatch(ar -> "User".equalsIgnoreCase(ar.getRole().getName()));

                                if (hasUserRole) {
                                        return new ApiResponse<>(false, "Email đã được sử dụng", null);
                                }
                        } else {
                                if (userRepository.existsByPhoneNumber(request.getPhoneNumber())) {
                                        return new ApiResponse<>(false, "Số điện thoại đã được sử dụng", null);
                                }
                        }

                        String otp = OTPUtils.generateOTP();
                        OTP otpEntity = new OTP(
                                        null,
                                        request.getEmail(),
                                        otp,
                                        OTPType.REGISTER,
                                        LocalDateTime.now().plusMinutes(5),
                                        false);
                        otpRepository.save(otpEntity);

                        emailService.sendOTPEmail(request.getEmail(), otp, "Xác thực đăng ký");

                        return new ApiResponse<>(true, "Mã OTP đã được gửi đến email của bạn", null);

                } catch (Exception e) {
                        return new ApiResponse<>(false, "Lỗi khi đăng ký: " + e.getMessage(), null);
                }
        }

        public ApiResponse<AuthResponse> verifyAndRegisterUser(VerifyRegisterOtpRequest request) {
                try {
                        OTP otpEntity = otpRepository
                                        .findByEmailAndOtpAndTypeAndIsUsedFalseAndExpiresAtAfter(
                                                        request.getEmail(),
                                                        request.getOtp(),
                                                        OTPType.REGISTER,
                                                        LocalDateTime.now())
                                        .orElseThrow(() -> new RuntimeException("Mã OTP không hợp lệ hoặc đã hết hạn"));

                        String hashed = PasswordUtils.hashPassword(request.getPassword());

                        Role userRole = roleRepository
                                        .findByNameAndIsSystemRole("User", true)
                                        .orElseThrow(() -> new RuntimeException(
                                                        "Không tìm thấy role User thuộc hệ thống."));

                        Optional<Account> existingAccountOpt = accountRepository.findByEmail(request.getEmail());
                        Account account;
                        User user;

                        if (existingAccountOpt.isPresent()) {
                                account = existingAccountOpt.get();

                                boolean hasUserRole = account.getAccountRoles().stream()
                                                .anyMatch(ar -> "User".equalsIgnoreCase(ar.getRole().getName()));

                                if (!hasUserRole) {
                                        AccountRole accountRole = new AccountRole();
                                        accountRole.setAccount(account);
                                        accountRole.setRole(userRole);
                                        account.setIsActive(true);
                                        accountRoleRepository.save(accountRole);
                                }

                                if (account.getUser() != null) {
                                        user = account.getUser();
                                } else {
                                        user = new User();
                                        user.setFirstName(request.getFirstName());
                                        user.setLastName(request.getLastName());
                                        user.setPhoneNumber(request.getPhoneNumber());
                                        user.setAccount(account);
                                        user.setCreatedAt(LocalDateTime.now());
                                        userRepository.save(user);

                                        user.setCode("USER" + user.getId());
                                        userRepository.save(user);
                                }

                        } else {
                                account = new Account();
                                account.setEmail(request.getEmail());
                                account.setPassword(hashed);
                                account.setIsVerified(true);
                                account.setIsActive(true);
                                account.setCreatedAt(LocalDateTime.now());
                                accountRepository.save(account);

                                user = new User();
                                user.setFirstName(request.getFirstName());
                                user.setLastName(request.getLastName());
                                user.setPhoneNumber(request.getPhoneNumber());
                                user.setAccount(account);
                                user.setCreatedAt(LocalDateTime.now());
                                userRepository.save(user);

                                user.setCode("USER" + user.getId());
                                userRepository.save(user);

                                AccountRole accountRole = new AccountRole();
                                accountRole.setAccount(account);
                                accountRole.setRole(userRole);
                                account.setIsActive(true);
                                accountRoleRepository.save(accountRole);
                        }

                        otpEntity.setIsUsed(true);
                        otpRepository.save(otpEntity);

                        String token = jwtUtils.generateToken(account, user, userRole.getName());

                        AuthResponse.UserResponse userResponse = new AuthResponse.UserResponse(
                                        user.getId(),
                                        user.getFirstName(),
                                        user.getLastName(),
                                        user.getPhoneNumber(),
                                        user.getImages());

                        AuthResponse authResponse = new AuthResponse(token, userResponse);

                        return new ApiResponse<>(true, "Đăng ký thành công", authResponse);

                } catch (Exception e) {
                        return new ApiResponse<>(false, "Lỗi khi xác thực và đăng ký: " + e.getMessage(), null);
                }
        }

        public ApiResponse<?> login(LoginRequest request) {
                try {
                        Optional<Account> optionalAccount = accountRepository.findByEmail(request.getEmail());
                        if (optionalAccount.isEmpty()) {
                                return new ApiResponse<>(false, "Email hoặc mật khẩu không đúng", null);
                        }

                        Account account = optionalAccount.get();

                        if (!account.getIsActive()) {
                                return new ApiResponse<>(false, "Tài khoản đã bị khóa", null);
                        }

                        if (!account.getIsVerified()) {
                                return new ApiResponse<>(false, "Tài khoản chưa được xác thực", null);
                        }

                        if (!passwordEncoder.matches(request.getPassword(), account.getPassword())) {
                                return new ApiResponse<>(false, "Email hoặc mật khẩu không đúng", null);
                        }

                        account.setLastLoginAt(LocalDateTime.now());
                        accountRepository.save(account);

                        User user = userRepository.findByAccountId(account.getId())
                                        .orElseThrow(() -> new RuntimeException(
                                                        "User chưa được tạo cho tài khoản này"));

                        List<AccountRole> roles = accountRoleRepository.findByAccountId(account.getId())
                                        .stream()
                                        .filter(AccountRole::getIsActive)
                                        .toList();

                        if (roles.isEmpty()) {
                                return new ApiResponse<>(false, "Tài khoản không có role nào hợp lệ", null);
                        }

                        if (roles.size() == 1) {
                                Role role = roles.get(0).getRole();
                                String token = jwtUtils.generateToken(account, user, role.getName());

                                AuthResponse.UserResponse userResponse = new AuthResponse.UserResponse(
                                                user.getId(), user.getFirstName(), user.getLastName(),
                                                user.getPhoneNumber(), user.getImages());

                                AuthResponse authResponse = new AuthResponse(token, userResponse);
                                return new ApiResponse<>(true, "Đăng nhập thành công", authResponse);
                        }

                        String tempToken = jwtUtils.generateTempToken(account);
                        List<String> roleNames = roles.stream().map(ar -> ar.getRole().getName()).toList();

                        Map<String, Object> data = new HashMap<>();
                        data.put("roles", roleNames);
                        data.put("tempToken", tempToken);

                        return new ApiResponse<>(true, "Chọn role để đăng nhập", data);

                } catch (Exception e) {
                        return new ApiResponse<>(false, "Lỗi khi đăng nhập: " + e.getMessage(), null);
                }
        }

        public AuthResponse chooseRole(ChooseRoleRequest request) {
                String tempToken = request.getTempToken();
                Integer accountId = jwtUtils.getAccountIdFromTempToken(tempToken);

                Account account = accountRepository.findById(accountId)
                                .orElseThrow(() -> new RuntimeException("Account không tồn tại"));

                User user = userRepository.findByAccountId(account.getId())
                                .orElseThrow(() -> new RuntimeException("User không tồn tại"));

                AccountRole accountRole = accountRoleRepository
                                .findByAccountIdAndRoleName(account.getId(), request.getRoleName())
                                .orElseThrow(() -> new RuntimeException("Role không hợp lệ"));

                Role selectedRole = accountRole.getRole();
                String token = jwtUtils.generateToken(account, user, selectedRole.getName());

                AuthResponse.UserResponse userResponse = new AuthResponse.UserResponse(
                                user.getId(), user.getFirstName(), user.getLastName(),
                                user.getPhoneNumber(), user.getImages());

                return new AuthResponse(token, userResponse);
        }

        public ApiResponse<String> forgotPasswordEmail(ForgotPasswordEmailRequest request) {
                try {
                        String email = request.getEmail();

                        Account account = accountRepository.findByEmail(email)
                                        .orElseThrow(() -> new RuntimeException(
                                                        "Không tìm thấy tài khoản với email này"));

                        otpRepository.updateIsUsedByEmailAndType(account.getEmail(), OTPType.RESET, true);

                        String otp = OTPUtils.generateOTP();

                        OTP otpEntity = new OTP(
                                        null,
                                        account.getEmail(),
                                        otp,
                                        OTPType.RESET,
                                        LocalDateTime.now().plusMinutes(5),
                                        false);
                        otpRepository.save(otpEntity);

                        emailService.sendOTPEmail(account.getEmail(), otp, "Xác thực quên mật khẩu");

                        return new ApiResponse<>(true, "Mã OTP đã được gửi đến email của bạn", null);
                } catch (Exception e) {
                        return new ApiResponse<>(false, "Lỗi khi gửi OTP quên mật khẩu: " + e.getMessage(), null);
                }
        }

        public ApiResponse<String> verifyResetOtp(VerifyResetOtpRequest request) {
                try {
                        Account account = accountRepository.findByEmail(request.getEmail())
                                        .orElseThrow(() -> new RuntimeException(
                                                        "Không tìm thấy tài khoản với email này"));

                        OTP otpEntity = otpRepository.findByEmailAndOtpAndTypeAndIsUsedFalseAndExpiresAtAfter(
                                        account.getEmail(), request.getOtp(), OTPType.RESET, LocalDateTime.now())
                                        .orElseThrow(() -> new RuntimeException("Mã OTP không hợp lệ hoặc đã hết hạn"));

                        otpEntity.setIsUsed(true);
                        otpRepository.save(otpEntity);

                        return new ApiResponse<>(true, "Xác thực OTP thành công, bạn có thể đặt lại mật khẩu.", null);
                } catch (Exception e) {
                        return new ApiResponse<>(false, "Lỗi khi xác thực OTP: " + e.getMessage(), null);
                }
        }

        public ApiResponse<String> forgotPasswordReset(ForgotPasswordResetRequest request) {
                try {
                        Account account = accountRepository.findByEmail(request.getEmail())
                                        .orElseThrow(() -> new RuntimeException(
                                                        "Không tìm thấy tài khoản để đặt lại mật khẩu"));

                        String hashedPassword = PasswordUtils.hashPassword(request.getNewPassword());
                        account.setPassword(hashedPassword);
                        accountRepository.save(account);

                        userRepository.findByAccountId(account.getId()).ifPresent(user -> {
                                notificationService.create(
                                                "Đổi mật khẩu thành công",
                                                "Mật khẩu của bạn đã được thay đổi. Nếu bạn không thực hiện hành động này, vui lòng liên hệ bộ phận hỗ trợ ngay.",
                                                "system",
                                                user.getId(),
                                                null,
                                                "user",
                                                null);
                        });

                        emailService.sendAlertEmail(
                                        account.getEmail(),
                                        "Cảnh báo thay đổi mật khẩu",
                                        "Mật khẩu của bạn đã được thay đổi thành công.");

                        return new ApiResponse<>(true, "Đặt lại mật khẩu thành công", null);
                } catch (Exception e) {
                        return new ApiResponse<>(false, "Lỗi khi đặt lại mật khẩu: " + e.getMessage(), null);
                }
        }
}