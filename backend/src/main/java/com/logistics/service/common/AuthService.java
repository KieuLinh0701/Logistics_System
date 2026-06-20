package com.logistics.service.common;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import com.logistics.entity.PermissionGroup;
import com.logistics.exception.AppException;
import com.logistics.exception.enums.AccountErrorCode;
import com.logistics.exception.enums.OtpErrorCode;
import com.logistics.exception.enums.RoleErrorCode;
import com.logistics.exception.enums.UserErrorCode;
import com.logistics.repository.PermissionGroupRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.logistics.entity.Account;
import com.logistics.entity.AccountRole;
import com.logistics.entity.OTP;
import com.logistics.entity.Role;
import com.logistics.entity.User;
import com.logistics.entity.UserSettlementSchedule;
import com.logistics.enums.OTPType;
import com.logistics.enums.WeekDay;
import com.logistics.repository.AccountRepository;
import com.logistics.repository.AccountRoleRepository;
import com.logistics.repository.OTPRepository;
import com.logistics.repository.RoleRepository;
import com.logistics.repository.UserRepository;
import com.logistics.repository.UserSettlementScheduleRepository;
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
    private final JwtUtils jwtUtils;
    private final PasswordEncoder passwordEncoder;

    private final RoleRepository roleRepository;
    private final AccountRepository accountRepository;
    private final AccountRoleRepository accountRoleRepository;
    private final UserRepository userRepository;
    private final OTPRepository otpRepository;
    private final EmailService emailService;
    private final NotificationService notificationService;
    private final UserSettlementScheduleRepository scheduleRepository;
    private final RoleService roleService;

    public void register(RegisterRequest request) {
            Optional<Account> existingAccountOpt = accountRepository.findByEmail(request.getEmail());

            if (existingAccountOpt.isPresent()) {
                Account existingAccount = existingAccountOpt.get();

                boolean hasUserRole = existingAccount.getAccountRoles() != null &&
                        existingAccount.getAccountRoles().stream()
                                .anyMatch(ar -> ar.getRole() != null
                                        && "User".equalsIgnoreCase(ar.getRole().getName())
                                        && ar.getRole().getUserOwner() != null);

                if (hasUserRole) {
                    throw new AppException(AccountErrorCode.ACCOUNT_EMAIL_ALREADY_IN_USE);
                }
            } else {
                if (userRepository.existsByPhoneNumber(request.getPhoneNumber())) {
                    throw new AppException(UserErrorCode.USER_PHONE_EXISTED);
                }
            }

            String otp = OTPUtils.generateOTP();
            OTP otpEntity = new OTP(
                    null,
                    request.getEmail(),
                    otp,
                    OTPType.REGISTER,
                    LocalDateTime.now()
                            .plusMinutes(5),
                    false);
            otpRepository.save(otpEntity);

            emailService.sendOTPEmail(request.getEmail(), otp, "Xác thực đăng ký");
    }

    public AuthResponse verifyAndRegisterUser(VerifyRegisterOtpRequest request) {
            OTP otpEntity = otpRepository
                    .findByEmailAndOtpAndTypeAndIsUsedFalseAndExpiresAtAfter(
                            request.getEmail(),
                            request.getOtp(),
                            OTPType.REGISTER,
                            LocalDateTime.now())
                    .orElseThrow(() -> new AppException(OtpErrorCode.OTP_INVALID_OR_EXPIRED));

            String hashed = PasswordUtils.hashPassword(request.getPassword());

            Role userRole = roleRepository
                    .findByNameAndUserOwnerIsNull("User")
                    .orElseThrow(() -> new AppException(UserErrorCode.USER_NOT_FOUND));

            Optional<Account> existingAccountOpt = accountRepository.findByEmail(request.getEmail());
            Account account;
            User user;

            if (existingAccountOpt.isPresent()) {
                account = existingAccountOpt.get();

                boolean hasUserRole = account.getAccountRoles()
                        .stream()
                        .anyMatch(ar -> "User".equalsIgnoreCase(ar.getRole()
                                .getName()));

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

            UserSettlementSchedule schedule = new UserSettlementSchedule();
            schedule.setUser(user);
            schedule.setWeekdays(Set.of(WeekDay.MONDAY, WeekDay.TUESDAY, WeekDay.WEDNESDAY,
                    WeekDay.THURSDAY, WeekDay.FRIDAY));
            scheduleRepository.save(schedule);

            String token = jwtUtils.generateToken(
                    account,
                    user,
                    userRole,
                    roleService.getPermissionGroupCodes(userRole));

            AuthResponse.UserResponse userResponse = new AuthResponse.UserResponse(
                    user.getId(),
                    user.getFirstName(),
                    user.getLastName(),
                    user.getPhoneNumber(),
                    user.getImages());

        return AuthResponse.builder()
                .token(token)
                .user(userResponse)
                .build();
    }

    public AuthResponse login(LoginRequest request) {
            Optional<Account> optionalAccount = accountRepository.findByEmail(request.getEmail());
            if (optionalAccount.isEmpty()) {
                throw new AppException(AccountErrorCode.ACCOUNT_LOGIN_FAILED);
            }

            Account account = optionalAccount.get();

            if (!account.getIsActive()) {
                throw new AppException(AccountErrorCode.ACCOUNT_LOCKED);
            }

            if (!account.getIsVerified()) {
                throw new AppException(AccountErrorCode.ACCOUNT_NOT_VERIFIED);
            }

            if (!passwordEncoder.matches(request.getPassword(), account.getPassword())) {
                throw new AppException(AccountErrorCode.ACCOUNT_LOGIN_FAILED);
            }

            account.setLastLoginAt(LocalDateTime.now());
            accountRepository.save(account);

            User user = userRepository.findByAccountId(account.getId())
                    .orElseThrow(() -> new AppException(UserErrorCode.USER_NOT_FOUND));

            List<AccountRole> roles = accountRoleRepository.findByAccountId(account.getId())
                    .stream()
                    .filter(AccountRole::getIsActive)
                    .toList();

            if (roles.isEmpty()) {
                throw new AppException(AccountErrorCode.ACCOUNT_NO_VALID_ROLE);
            }

            if (roles.size() == 1) {
                Role role = roles.getFirst().getRole();

                String token = jwtUtils.generateToken(
                        account,
                        user,
                        role,
                        roleService.getPermissionGroupCodes(role));

                AuthResponse.UserResponse userResponse = AuthResponse.UserResponse.builder()
                        .id(user.getId())
                        .firstName(user.getFirstName())
                        .lastName(user.getLastName())
                        .phoneNumber(user.getPhoneNumber())
                        .images(user.getImages())
                        .build();

                return AuthResponse.builder()
                        .token(token)
                        .user(userResponse)
                        .build();
            }

            String tempToken = jwtUtils.generateTempToken(account);
            List<String> roleNames = roles.stream()
                    .map(ar -> ar.getRole()
                            .getName())
                    .toList();

        return AuthResponse.builder()
                .roles(roleNames)
                .tempToken(tempToken)
                .build();
    }

    public AuthResponse chooseRole(ChooseRoleRequest request) {
        String tempToken = request.getTempToken();
        Integer accountId = jwtUtils.getAccountIdFromTempToken(tempToken);

        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new AppException(AccountErrorCode.ACCOUNT_NOT_FOUND));

        User user = userRepository.findByAccountId(account.getId())
                .orElseThrow(() -> new AppException(UserErrorCode.USER_NOT_FOUND));

        AccountRole accountRole = accountRoleRepository
                .findByAccountIdAndRoleName(account.getId(), request.getRoleName())
                .orElseThrow(() -> new AppException(RoleErrorCode.ROLE_NOT_FOUND));

        Role selectedRole = accountRole.getRole();
        String token = jwtUtils.generateToken(
                account,
                user,
                selectedRole,
                roleService.getPermissionGroupCodes(selectedRole)
        );

        AuthResponse.UserResponse userResponse = new AuthResponse.UserResponse(
                user.getId(), user.getFirstName(), user.getLastName(),
                user.getPhoneNumber(), user.getImages());

        return AuthResponse.builder()
                .token(token)
                .user(userResponse)
                .build();
    }

    public void forgotPasswordEmail(ForgotPasswordEmailRequest request) {
            String email = request.getEmail();

            Account account = accountRepository.findByEmail(email)
                    .orElseThrow(() -> new AppException(AccountErrorCode.ACCOUNT_NOT_FOUND));

            otpRepository.updateIsUsedByEmailAndType(account.getEmail(), OTPType.RESET, true);

            String otp = OTPUtils.generateOTP();

            OTP otpEntity = new OTP(
                    null,
                    account.getEmail(),
                    otp,
                    OTPType.RESET,
                    LocalDateTime.now()
                            .plusMinutes(5),
                    false);
            otpRepository.save(otpEntity);

            emailService.sendOTPEmail(account.getEmail(), otp, "Xác thực quên mật khẩu");
    }

    public void verifyResetOtp(VerifyResetOtpRequest request) {
            Account account = accountRepository.findByEmail(request.getEmail())
                    .orElseThrow(() -> new AppException(AccountErrorCode.ACCOUNT_NOT_FOUND));

            OTP otpEntity = otpRepository.findByEmailAndOtpAndTypeAndIsUsedFalseAndExpiresAtAfter(
                            account.getEmail(), request.getOtp(), OTPType.RESET, LocalDateTime.now())
                    .orElseThrow(() -> new AppException(OtpErrorCode.OTP_INVALID_OR_EXPIRED));

            otpEntity.setIsUsed(true);
            otpRepository.save(otpEntity);
    }

    public void forgotPasswordReset(ForgotPasswordResetRequest request) {
            Account account = accountRepository.findByEmail(request.getEmail())
                    .orElseThrow(() -> new AppException(AccountErrorCode.ACCOUNT_NOT_FOUND));

            String hashedPassword = PasswordUtils.hashPassword(request.getNewPassword());
            account.setPassword(hashedPassword);
            accountRepository.save(account);

            userRepository.findByAccountId(account.getId())
                    .ifPresent(user -> {
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

    }
}