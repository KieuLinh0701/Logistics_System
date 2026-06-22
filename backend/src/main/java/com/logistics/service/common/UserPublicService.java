package com.logistics.service.common;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.logistics.entity.Account;
import com.logistics.entity.OTP;
import com.logistics.entity.Role;
import com.logistics.entity.User;
import com.logistics.enums.OTPType;
import com.logistics.exception.AppException;
import com.logistics.exception.enums.AccountErrorCode;
import com.logistics.exception.enums.CommonErrorCode;
import com.logistics.exception.enums.OtpErrorCode;
import com.logistics.exception.enums.UserErrorCode;
import com.logistics.repository.AccountRepository;
import com.logistics.repository.OTPRepository;
import com.logistics.repository.UserRepository;
import com.logistics.request.common.user.UpdateEmailRequest;
import com.logistics.request.common.user.UpdatePasswordRequest;
import com.logistics.request.common.user.UpdateProfileRequest;
import com.logistics.request.common.user.VerifyEmailUpdateOTPRequest;
import com.logistics.response.AuthResponse;
import com.logistics.service.email.EmailService;
import com.logistics.utils.JwtUtils;
import com.logistics.utils.OTPUtils;
import com.logistics.utils.PasswordUtils;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class UserPublicService {

    private final Cloudinary cloudinary;
    private final JwtUtils jwtUtils;
    private final PasswordEncoder passwordEncoder;

    private final AccountRepository accountRepository;
    private final UserRepository userRepository;
    private final OTPRepository otpRepository;

    private final EmailService emailService;
    private final NotificationService notificationService;
    private final RoleService roleService;

    public void updatePassword(@NonNull Integer accountId, UpdatePasswordRequest request) {
            Account account = accountRepository.findById(accountId)
                    .orElseThrow(() -> new AppException(AccountErrorCode.ACCOUNT_NOT_FOUND));

            if (!passwordEncoder.matches(request.getOldPassword(), account.getPassword())) {
                throw  new AppException(AccountErrorCode.ACCOUNT_OLD_PASSWORD_INCORRECT);
            }

            account.setPassword(PasswordUtils.hashPassword(request.getNewPassword()));
            accountRepository.save(account);

            userRepository.findByAccountId(account.getId())
                    .ifPresent(user -> notificationService.create(
                            "Đổi mật khẩu thành công",
                            "Mật khẩu của bạn đã được thay đổi. Nếu bạn không thực hiện hành động này, vui lòng liên hệ bộ phận hỗ trợ ngay.",
                            "system",
                            user.getId(),
                            null,
                            "user",
                            null));

            emailService.sendAlertEmail(
                    account.getEmail(),
                    "Cảnh báo thay đổi mật khẩu",
                    "Mật khẩu của bạn đã được thay đổi thành công.");
    }

    public void sendEmailUpdateOTP(@NonNull Integer accountId, UpdateEmailRequest request) {
            Account account = accountRepository.findById(accountId)
                    .orElseThrow(() -> new AppException(AccountErrorCode.ACCOUNT_NOT_FOUND));

            if (!passwordEncoder.matches(request.getPassword(), account.getPassword())) {
                throw new AppException(AccountErrorCode.ACCOUNT_PASSWORD_INCORRECT);
            }

            if (request.getNewEmail()
                    .equalsIgnoreCase(account.getEmail())) {
                throw new AppException(AccountErrorCode.ACCOUNT_NEW_EMAIL_DUPLICATE_CURRENT);
            }

            if (accountRepository.findByEmail(request.getNewEmail())
                    .isPresent()) {
                throw new AppException(AccountErrorCode.ACCOUNT_EMAIL_ALREADY_IN_USE);
            }

            otpRepository.updateIsUsedByEmailAndType(account.getEmail(), OTPType.UPDATE_EMAIL, true);

            String otp = OTPUtils.generateOTP();
            OTP otpEntity = new OTP(
                    null,
                    request.getNewEmail(),
                    otp,
                    OTPType.UPDATE_EMAIL,
                    LocalDateTime.now()
                            .plusMinutes(5),
                    false);
            otpRepository.save(otpEntity);

            emailService.sendOTPEmail(request.getNewEmail(), otp, "Xác thực đổi email");

            userRepository.findByAccountId(account.getId())
                    .ifPresent(user -> notificationService.create(
                            "Cảnh báo thay đổi email",
                            "Tài khoản của bạn vừa được yêu cầu thay đổi email. Nếu bạn không thực hiện hành động này, vui lòng liên hệ bộ phận hỗ trợ ngay.",
                            "system",
                            user.getId(),
                            null,
                            "user",
                            null));

            emailService.sendAlertEmail(
                    account.getEmail(),
                    "Cảnh báo thay đổi email",
                    "Tài khoản của bạn vừa được yêu cầu thay đổi email.");
    }

    public AuthResponse verifyEmailUpdateOTP(
            @NonNull Integer accountId,
            VerifyEmailUpdateOTPRequest request,
            Integer roleId) {
            Account account = accountRepository.findById(accountId)
                    .orElseThrow(() -> new AppException(AccountErrorCode.ACCOUNT_NOT_FOUND));

            OTP otpEntity = otpRepository
                    .findByEmailAndOtpAndTypeAndIsUsedFalseAndExpiresAtAfter(
                            request.getNewEmail(),
                            request.getOtp(),
                            OTPType.UPDATE_EMAIL,
                            LocalDateTime.now())
                    .orElseThrow(() -> new AppException(OtpErrorCode.OTP_INVALID_OR_EXPIRED));

            account.setEmail(request.getNewEmail());
            accountRepository.save(account);

            otpEntity.setIsUsed(true);
            otpRepository.save(otpEntity);

            userRepository.findByAccountId(account.getId())
                    .ifPresent(user -> notificationService.create(
                            "Đổi email thành công",
                            "Địa chỉ email của bạn đã được thay đổi. Nếu bạn không thực hiện hành động này, vui lòng liên hệ bộ phận hỗ trợ ngay.",
                            "system",
                            user.getId(),
                            null,
                            "user",
                            null));

            emailService.sendAlertEmail(
                    request.getNewEmail(),
                    "Cảnh báo thay đổi email",
                    "Địa chỉ email này vừa được sử dụng làm email mới cho tài khoản của UTE Logistics.");

            User user = userRepository.findByAccountId(accountId).get();

            Role role = roleService.findByIdWithPermissionGroups(roleId);

            String token = jwtUtils.generateToken(
                    account,
                    user,
                    role,
                    roleService.getPermissionGroupCodes(role)
            );

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

    @SuppressWarnings("unchecked")
    public String updateProfile(@NonNull Integer userId, @NonNull UpdateProfileRequest updatedUser) {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new AppException(UserErrorCode.USER_NOT_FOUND));

            user.setFirstName(updatedUser.getFirstName());
            user.setLastName(updatedUser.getLastName());
            user.setPhoneNumber(updatedUser.getPhoneNumber());

            MultipartFile avatarFile = updatedUser.getAvatarFile();
            if (avatarFile != null && !avatarFile.isEmpty()) {
                try {
                    Map<String, Object> uploadResult = cloudinary.uploader()
                            .upload(
                                    avatarFile.getBytes(),
                                    ObjectUtils.asMap(
                                            "folder",
                                            "avatars",
                                            "resource_type",
                                            "image"));
                    String imageUrl = uploadResult.get("secure_url")
                            .toString();
                    user.setImages(imageUrl);
                } catch (Exception e) {
                    e.printStackTrace();
                    throw new AppException(CommonErrorCode.CLOUDINARY_UPLOAD_FAILED);
                }
            }

            userRepository.save(user);

            return user.getImages();
    }
}