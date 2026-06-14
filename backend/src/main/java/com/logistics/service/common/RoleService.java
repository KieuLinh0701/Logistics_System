package com.logistics.service.common;

import com.logistics.entity.Account;
import com.logistics.entity.AccountRole;
import com.logistics.entity.OTP;
import com.logistics.entity.PermissionGroup;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class RoleService {

    @Autowired
    private RoleRepository roleRepository;

    @Transactional(readOnly = true)
    public Role findByIdWithPermissionGroups(Integer roleId) {
        return roleRepository.findByIdWithPermissionGroups(roleId)
                .orElseThrow(() -> new RuntimeException("Nhóm quyền không tồn tại"));
    }

    public List<String> getPermissionGroupCodes(Role role) {
        return role.getPermissionGroups().stream()
                .filter(PermissionGroup::getIsActive)
                .map(PermissionGroup::getCode)
                .distinct()
                .toList();
    }
}