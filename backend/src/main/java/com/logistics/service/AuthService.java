package com.logistics.service;

import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.logistics.dto.auth.ForgotPasswordEmailRequest;
import com.logistics.dto.auth.LoginRequest;
import com.logistics.dto.auth.RegisterRequest;
import com.logistics.dto.auth.ForgotPasswordResetRequest;
import com.logistics.dto.auth.VerifyRegisterOtpRequest;
import com.logistics.dto.auth.VerifyResetOtpRequest;
import com.logistics.entity.Account;
import com.logistics.entity.OTP;
import com.logistics.entity.Role;
import com.logistics.entity.User;
import com.logistics.enums.OTP.OTPType;
import com.logistics.repository.AccountRepository;
import com.logistics.repository.OTPRepository;
import com.logistics.repository.RoleRepository;
import com.logistics.repository.UserRepository;
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
        private final UserRepository userRepository;
        private final OTPRepository otpRepository;
        private final EmailService emailService;
        private final NotificationService notificationService;

        public ApiResponse<String> register(RegisterRequest request) {
                if (accountRepository.existsByEmail(request.getEmail()) ||
                                userRepository.existsByPhoneNumber(request.getPhoneNumber())) {
                        return new ApiResponse<>(false, "Email hoặc số điện thoại đã được sử dụng", null);
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
        }

        public ApiResponse<AuthResponse> verifyAndRegisterUser(VerifyRegisterOtpRequest request) {
                OTP otpEntity = otpRepository
                                .findByEmailAndOtpAndTypeAndIsUsedFalseAndExpiresAtAfter(
                                                request.getEmail(),
                                                request.getOtp(),
                                                OTPType.REGISTER,
                                                LocalDateTime.now())
                                .orElseThrow(() -> new RuntimeException("Mã OTP không hợp lệ hoặc đã hết hạn"));

                String hashed = PasswordUtils.hashPassword(request.getPassword());

                Role userRole = roleRepository.findByName("User")
                                .orElseThrow(() -> new RuntimeException("Không tìm thấy role User."));

                Account newAccount = new Account();
                newAccount.setEmail(request.getEmail());
                newAccount.setPassword(hashed);
                newAccount.setRole(userRole);
                newAccount.setIsVerified(true);
                newAccount.setIsActive(true);
                newAccount.setCreatedAt(LocalDateTime.now());

                accountRepository.save(newAccount);

                User newUser = new User();
                newUser.setFirstName(request.getFirstName());
                newUser.setLastName(request.getLastName());
                newUser.setPhoneNumber(request.getPhoneNumber());
                newUser.setAccount(newAccount);
                newUser.setCreatedAt(LocalDateTime.now());

                userRepository.save(newUser);

                newUser.setCode("USER_" + newUser.getId());
                userRepository.save(newUser);

                otpEntity.setIsUsed(true);
                otpRepository.save(otpEntity);

                String token = jwtUtils.generateToken(newAccount, newUser);

                AuthResponse.UserResponse userResponse = new AuthResponse.UserResponse(
                                newUser.getId(),
                                newUser.getFirstName(),
                                newUser.getLastName(),
                                newUser.getPhoneNumber(),
                                newUser.getImages()
                );

                AuthResponse authResponse = new AuthResponse(token, userResponse);

                return new ApiResponse<>(true, "Đăng ký thành công", authResponse);
        }

        public ApiResponse<AuthResponse> login(LoginRequest request) {
                String email = request.getEmail();
                String password = request.getPassword();

                Optional<Account> optionalAccount = accountRepository.findByEmail(email);

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

                if (!passwordEncoder.matches(password, account.getPassword())) {
                        return new ApiResponse<>(false, "Email hoặc mật khẩu không đúng", null);
                }

                account.setLastLoginAt(LocalDateTime.now());
                accountRepository.save(account);

                Optional<User> optionalUser = userRepository.findByAccountId(account.getId());
                if (optionalUser.isEmpty()) {
                        throw new RuntimeException("User chưa được tạo cho tài khoản này");
                }
                User user = optionalUser.get();

                String token = jwtUtils.generateToken(account, user);

                AuthResponse.UserResponse userResponse = new AuthResponse.UserResponse(
                                user.getId(),
                                user.getFirstName(),
                                user.getLastName(),
                                user.getPhoneNumber(),
                                user.getImages()
                );

                AuthResponse authResponse = new AuthResponse(token, userResponse);

                return new ApiResponse<>(true, "Đăng nhập thành công", authResponse);
        }

        public ApiResponse<String> forgotPasswordEmail(ForgotPasswordEmailRequest request) {
                String email = request.getEmail();

                Optional<Account> optionalAccount = accountRepository.findByEmail(email);
                if (optionalAccount.isEmpty()) {
                        return new ApiResponse<>(false, "Không tìm thấy tài khoản với email này",
                                        null);
                }

                Account account = optionalAccount.get();

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
        }

        public ApiResponse<String> verifyResetOtp(VerifyResetOtpRequest request) {
                String email = request.getEmail();
                String otp = request.getOtp();

                Optional<Account> optionalAccount = accountRepository.findByEmail(email);
                if (optionalAccount.isEmpty()) {
                        return new ApiResponse<>(false, "Không tìm thấy tài khoản với email này",
                                        null);
                }

                Account account = optionalAccount.get();

                Optional<OTP> optionalOtp = otpRepository.findByEmailAndOtpAndTypeAndIsUsedFalseAndExpiresAtAfter(
                                account.getEmail(), otp, OTPType.RESET, LocalDateTime.now());

                if (optionalOtp.isEmpty()) {
                        return new ApiResponse<>(false, "Mã OTP không hợp lệ hoặc đã hết hạn", null);
                }

                OTP otpEntity = optionalOtp.get();
                otpEntity.setIsUsed(true);
                otpRepository.save(otpEntity);

                return new ApiResponse<>(true, "Xác thực OTP thành công, bạn có thể đặt lại mật khẩu.", null);
        }

        public ApiResponse<String> forgotPasswordReset(ForgotPasswordResetRequest request) {
                String email = request.getEmail();
                String newPassword = request.getNewPassword();

                Optional<Account> optionalAccount = accountRepository.findByEmail(email);
                if (optionalAccount.isEmpty()) {
                        return new ApiResponse<>(false, "Không tìm thấy tài khoản để đặt lại mật khẩu", null);
                }

                Account account = optionalAccount.get();

                String hashedPassword = PasswordUtils.hashPassword(newPassword);

                account.setPassword(hashedPassword);
                accountRepository.save(account);

                Optional<User> optionalUser = userRepository.findByAccountId(account.getId());
                optionalUser.ifPresent(user -> {
                        notificationService.create(
                                        "Đổi mật khẩu thành công",
                                        "Mật khẩu của bạn đã được thay đổi. Nếu bạn không thực hiện hành động này, vui lòng liên hệ bộ phận hỗ trợ ngay.",
                                        "system",
                                        user.getId(),
                                        "user",
                                        null);
                });

                emailService.sendAlertEmail(
                                account.getEmail(),
                                "Cảnh báo thay đổi mật khẩu",
                                "Mật khẩu của bạn đã được thay đổi thành công.");

                return new ApiResponse<>(true, "Đặt lại mật khẩu thành công", null);
        }
}