package com.logistics.service;

import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.logistics.dto.RegisterRequest;
import com.logistics.dto.VerifyOtpRequest;
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

        public ApiResponse<String> register(RegisterRequest request) {
                if (accountRepository.existsByEmail(request.getEmail()) ||
                                accountRepository.existsByPhoneNumber(request.getPhoneNumber())) {
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

        public ApiResponse<String> verifyAndRegisterUser(VerifyOtpRequest request) {
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
                newAccount.setPhoneNumber(request.getPhoneNumber());
                newAccount.setPassword(hashed);
                newAccount.setRole(userRole);
                newAccount.setIsVerified(true);
                newAccount.setIsActive(true);
                newAccount.setCreatedAt(LocalDateTime.now());

                accountRepository.save(newAccount);

                User newUser = new User();
                newUser.setFirstName(request.getFirstName());
                newUser.setLastName(request.getLastName());
                newUser.setAccount(newAccount);
                newUser.setCreatedAt(LocalDateTime.now());

                userRepository.save(newUser);

                newUser.setCode("USER_" + newUser.getId());
                userRepository.save(newUser);

                otpEntity.setIsUsed(true);
                otpRepository.save(otpEntity);

                String token = jwtUtils.generateToken(newAccount, newUser);

                return new ApiResponse<>(true, "Đăng ký thành công", token);
        }

        public ApiResponse<String> login(String indentifier, String password) {
                Optional<Account> optionalAccount = accountRepository.findByEmailOrPhoneNumber(indentifier,
                                indentifier);

                if (optionalAccount.isEmpty()) {
                        return new ApiResponse<>(false, "Email hoặc số điện thoại hoặc mật khẩu không đúng", null);
                }

                Account account = optionalAccount.get();

                if (!account.getIsActive()) {
                        return new ApiResponse<>(false, "Tài khoản đã bị khóa", null);
                }

                if (!account.getIsVerified()) {
                        return new ApiResponse<>(false, "Tài khoản chưa được xác thực", null);
                }

                if (!passwordEncoder.matches(password, account.getPassword())) {
                        return new ApiResponse<>(false, "Email hoặc số điện thoại hoặc mật khẩu không đúng", null);
                }

                account.setLastLoginAt(LocalDateTime.now());
                accountRepository.save(account);

                Optional<User> optionalUser = userRepository.findByAccountId(account.getId());
                if (optionalUser.isEmpty()) {
                        throw new RuntimeException("User chưa được tạo cho tài khoản này");
                }
                User user = optionalUser.get();

                String token = jwtUtils.generateToken(account, user);

                return new ApiResponse<>(true, "Đăng nhập thành công", token);
        }
}