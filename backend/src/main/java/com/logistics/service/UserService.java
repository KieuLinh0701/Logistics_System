package com.logistics.service;

import java.time.LocalDateTime;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.logistics.dto.user.UpdateEmailRequest;
import com.logistics.dto.user.UpdatePasswordRequest;
import com.logistics.dto.user.UpdateProfileRequest;
import com.logistics.dto.user.VerifyEmailUpdateOTPRequest;
import com.logistics.entity.Account;
import com.logistics.entity.OTP;
import com.logistics.entity.User;
import com.logistics.enums.OTP.OTPType;
import com.logistics.repository.AccountRepository;
import com.logistics.repository.OTPRepository;
import com.logistics.repository.UserRepository;
import com.logistics.response.ApiResponse;
import com.logistics.response.AuthResponse;
import com.logistics.utils.EmailService;
import com.logistics.utils.JwtUtils;
import com.logistics.utils.OTPUtils;
import com.logistics.utils.PasswordUtils;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserService {

        private final Cloudinary cloudinary;

        @Autowired
        private JwtUtils jwtUtils;

        @Autowired
        private PasswordEncoder passwordEncoder;

        private final AccountRepository accountRepository;
        private final UserRepository userRepository;
        private final OTPRepository otpRepository;
        private final EmailService emailService;
        private final NotificationService notificationService;

        public ApiResponse<String> updatePassword(@NonNull Integer accountId, UpdatePasswordRequest request) {
                Account account = accountRepository.findById(accountId)
                                .orElseThrow(() -> new RuntimeException("Không tìm thấy tài khoản"));

                if (!passwordEncoder.matches(request.getOldPassword(), account.getPassword())) {
                        return new ApiResponse<>(false, "Mật khẩu cũ không chính xác", null);
                }

                account.setPassword(PasswordUtils.hashPassword(request.getNewPassword()));
                accountRepository.save(account);

                userRepository.findByAccountId(account.getId()).ifPresent(user -> notificationService.create(
                                "Đổi mật khẩu thành công",
                                "Mật khẩu của bạn đã được thay đổi. Nếu bạn không thực hiện hành động này, vui lòng liên hệ bộ phận hỗ trợ ngay.",
                                "system",
                                user.getId(),
                                "user",
                                null));

                emailService.sendAlertEmail(
                                account.getEmail(),
                                "Cảnh báo thay đổi mật khẩu",
                                "Mật khẩu của bạn đã được thay đổi thành công.");

                return new ApiResponse<>(true, "Đổi mật khẩu thành công", null);
        }

        public ApiResponse<String> sendEmailUpdateOTP(@NonNull Integer accountId, UpdateEmailRequest request) {
                Account account = accountRepository.findById(accountId)
                                .orElseThrow(() -> new RuntimeException("Không tìm thấy tài khoản"));

                if (!passwordEncoder.matches(request.getPassword(), account.getPassword())) {
                        return new ApiResponse<>(false, "Mật khẩu không chính xác", null);
                }

                if (request.getNewEmail().equalsIgnoreCase(account.getEmail())) {
                        return new ApiResponse<>(false, "Email mới không được trùng với email hiện tại", null);
                }

                if (accountRepository.findByEmail(request.getNewEmail()).isPresent()) {
                        return new ApiResponse<>(false, "Email này đã được sử dụng bởi tài khoản khác", null);
                }

                otpRepository.updateIsUsedByEmailAndType(account.getEmail(), OTPType.UPDATE_EMAIL, true);

                String otp = OTPUtils.generateOTP();
                OTP otpEntity = new OTP(
                                null,
                                request.getNewEmail(),
                                otp,
                                OTPType.UPDATE_EMAIL,
                                LocalDateTime.now().plusMinutes(5),
                                false);
                otpRepository.save(otpEntity);

                emailService.sendOTPEmail(request.getNewEmail(), otp, "Xác thực đổi email");

                userRepository.findByAccountId(account.getId()).ifPresent(user -> notificationService.create(
                                "Cảnh báo thay đổi email",
                                "Tài khoản của bạn vừa được yêu cầu thay đổi email. Nếu bạn không thực hiện hành động này, vui lòng liên hệ bộ phận hỗ trợ ngay.",
                                "system",
                                user.getId(),
                                "user",
                                null));

                emailService.sendAlertEmail(
                                account.getEmail(),
                                "Cảnh báo thay đổi email",
                                "Tài khoản của bạn vừa được yêu cầu thay đổi email.");

                return new ApiResponse<>(true, "Mã OTP đã được gửi đến email mới của bạn", null);
        }

        public ApiResponse<AuthResponse> verifyEmailUpdateOTP(@NonNull Integer accountId,
                        VerifyEmailUpdateOTPRequest request) {
                Account account = accountRepository.findById(accountId)
                                .orElseThrow(() -> new RuntimeException("Không tìm thấy tài khoản"));

                OTP otpEntity = otpRepository
                                .findByEmailAndOtpAndTypeAndIsUsedFalseAndExpiresAtAfter(
                                                request.getNewEmail(),
                                                request.getOtp(),
                                                OTPType.UPDATE_EMAIL,
                                                LocalDateTime.now())
                                .orElseThrow(() -> new RuntimeException("Mã OTP không hợp lệ hoặc đã hết hạn"));

                account.setEmail(request.getNewEmail());
                accountRepository.save(account);

                otpEntity.setIsUsed(true);
                otpRepository.save(otpEntity);

                userRepository.findByAccountId(account.getId()).ifPresent(user -> notificationService.create(
                                "Đổi email thành công",
                                "Địa chỉ email của bạn đã được thay đổi. Nếu bạn không thực hiện hành động này, vui lòng liên hệ bộ phận hỗ trợ ngay.",
                                "system",
                                user.getId(),
                                "user",
                                null));

                emailService.sendAlertEmail(
                                request.getNewEmail(),
                                "Cảnh báo thay đổi email",
                                "Địa chỉ email này vừa được sử dụng làm email mới cho tài khoản của UTE Logistics.");

                User user = userRepository.findByAccountId(accountId).get();

                String token = jwtUtils.generateToken(account, user);

                AuthResponse.UserResponse userResponse = new AuthResponse.UserResponse(
                                user.getId(),
                                user.getFirstName(),
                                user.getLastName(),
                                user.getPhoneNumber(),
                                user.getImages());

                AuthResponse authResponse = new AuthResponse(token, userResponse);

                return new ApiResponse<>(true, "Thay đổi email thành công", authResponse);
        }

        @SuppressWarnings("unchecked")
        public ApiResponse<String> updateProfile(@NonNull Integer userId, @NonNull UpdateProfileRequest updatedUser) {
                User user = userRepository.findById(userId)
                                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng"));

                user.setFirstName(updatedUser.getFirstName());
                user.setLastName(updatedUser.getLastName());
                user.setPhoneNumber(updatedUser.getPhoneNumber());

                MultipartFile avatarFile = updatedUser.getAvatarFile();
                if (avatarFile != null && !avatarFile.isEmpty()) {
                        try {
                                Map<String, Object> uploadResult = cloudinary.uploader().upload(
                                                avatarFile.getBytes(),
                                                ObjectUtils.asMap("folder", "avatars", "resource_type", "image"));
                                String imageUrl = uploadResult.get("secure_url").toString();
                                user.setImages(imageUrl);
                        } catch (Exception e) {
                                e.printStackTrace();
                                return new ApiResponse<>(false, "Upload ảnh thất bại", null);
                        }
                }

                userRepository.save(user);

                return new ApiResponse<>(true, "Cập nhật thông tin thành công", user.getImages());
        }
}