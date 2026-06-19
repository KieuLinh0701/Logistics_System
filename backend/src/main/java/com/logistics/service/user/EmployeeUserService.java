package com.logistics.service.user;

import com.logistics.dto.user.employee.EmployeeByRoleIdListUserDto;
import com.logistics.dto.user.employee.EmployeeListUserDto;
import com.logistics.dto.user.employee.ShopWorkHistoryListUserDto;
import com.logistics.entity.Account;
import com.logistics.entity.AccountRole;
import com.logistics.entity.Role;
import com.logistics.entity.ShopWorkHistory;
import com.logistics.entity.User;
import com.logistics.exception.enums.EmployeeErrorCode;
import com.logistics.exception.AppException;
import com.logistics.exception.enums.UserErrorCode;
import com.logistics.mapper.UserMapper;
import com.logistics.repository.AccountRepository;
import com.logistics.repository.AccountRoleRepository;
import com.logistics.repository.ShopWorkHistoryRepository;
import com.logistics.repository.UserRepository;
import com.logistics.request.user.employee.CreateEmployeeUserRequest;
import com.logistics.request.user.employee.EmployeeByRoleIdSearchUserRequest;
import com.logistics.request.user.employee.EmployeeSearchUserRequest;
import com.logistics.request.user.employee.ShopWorkHistorySearchUserRequest;
import com.logistics.request.user.employee.UpdateEmployeeUserRequest;
import com.logistics.request.user.employee.UpdateIsActiveUserRequest;
import com.logistics.response.ListResponse;
import com.logistics.response.Pagination;
import com.logistics.utils.EmailService;
import com.logistics.utils.PasswordUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class EmployeeUserService {

    private final UserRepository repository;
    private final RoleUserService roleUserService;
    private final UserRepository userRepository;
    private final AccountRoleRepository accountRoleRepository;
    private final AccountRepository accountRepository;
    private final ShopWorkHistoryRepository shopWorkHistoryRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final UserUserService userUserService;

    public ListResponse<EmployeeByRoleIdListUserDto> listByRoleId(
            int userId,
            int roleId,
            EmployeeByRoleIdSearchUserRequest request) {
        User user = getUser(userId);
        Role role = roleUserService.getRole(roleId);
        roleUserService.checkOwnerPermission(user, role);

        int page = request.getPage();
        int limit = request.getLimit();
        String search = request.getSearch();
        LocalDateTime startDate = request.getStartDate() != null && !request.getStartDate()
                .isBlank()
                ? LocalDateTime.parse(request.getStartDate())
                : null;

        LocalDateTime endDate = request.getEndDate() != null && !request.getEndDate()
                .isBlank()
                ? LocalDateTime.parse(request.getEndDate())
                : null;

        Integer shopId = userUserService.getShopId(user);

        Pageable pageable = PageRequest.of(page - 1, limit, Sort.by("createdAt")
                .descending());

        Page<User> pageData = repository.findAllByShopIdWithLatestWorkHistory(
                shopId,
                roleId,
                search,
                null,
                startDate,
                endDate,
                pageable);

        List<EmployeeByRoleIdListUserDto> list = UserMapper.toEmployeeListUserDto(pageData.getContent(), roleId);

        int total = (int) pageData.getTotalElements();
        Pagination pagination = new Pagination(total, page, limit, pageData.getTotalPages());

        ListResponse<EmployeeByRoleIdListUserDto> data = new ListResponse<>();
        data.setList(list);
        data.setPagination(pagination);

        return data;
    }

    public ListResponse<EmployeeListUserDto> list(
            int userId,
            EmployeeSearchUserRequest request) {
        User user = getUser(userId);
        Integer shopId = userUserService.getShopId(user);

        int page = request.getPage();
        int limit = request.getLimit();
        String search = request.getSearch();
        String sort = request.getSort();
        Integer roleId = request.getRoleId();
        Boolean active = request.getActive();

        LocalDateTime startDate = request.getStartDate() != null && !request.getStartDate().isBlank()
                ? LocalDateTime.parse(request.getStartDate())
                : null;

        LocalDateTime endDate = request.getEndDate() != null && !request.getEndDate().isBlank()
                ? LocalDateTime.parse(request.getEndDate())
                : null;

        Sort sortOrder = "OLDEST".equalsIgnoreCase(sort)
                ? Sort.by("createdAt").ascending()
                : Sort.by("createdAt").descending();

        Pageable pageable = PageRequest.of(page - 1, limit, sortOrder);

        Page<User> pageData = repository.findAllByShopIdWithLatestWorkHistory(
                shopId,
                roleId,
                search,
                active,
                startDate,
                endDate,
                pageable);

        List<EmployeeListUserDto> list = UserMapper.toEmployeeListDto(pageData.getContent());

        int total = (int) pageData.getTotalElements();
        Pagination pagination = new Pagination(total, page, limit, pageData.getTotalPages());

        ListResponse<EmployeeListUserDto> data = new ListResponse<>();
        data.setList(list);
        data.setPagination(pagination);

        return data;
    }

    @Transactional
    public void updateIsActive(
            int userId,
            int id,
            UpdateIsActiveUserRequest request) {
        User currentUser = getUser(userId);
        User targetUser = getUser(id);

        checkShopPermission(currentUser, targetUser);

        Role role = roleUserService.getRole(request.getRoleId());
        roleUserService.checkOwnerPermission(currentUser, role);

        AccountRole accountRole = accountRoleRepository
                .findByAccountIdAndRoleId(targetUser.getAccount()
                        .getId(), request.getRoleId())
                .orElseThrow(() -> new AppException(UserErrorCode.USER_EMPLOYEE_ROLE_NOT_FOUND));

        Integer shopId = userUserService.getShopId(currentUser);

        if (request.getIsActive()) {
            validateNoActiveShopRole(targetUser, shopId);

            accountRole.setIsActive(true);
            accountRoleRepository.save(accountRole);

            targetUser.getAccount()
                    .setIsActive(true);
            accountRepository.save(targetUser.getAccount());

            shopWorkHistoryRepository.findByUserIdAndIsCurrentTrue(targetUser.getId())
                    .forEach(wh -> {
                        wh.setIsCurrent(false);
                        wh.setLeftAt(LocalDateTime.now());
                        shopWorkHistoryRepository.save(wh);
                    });

            ShopWorkHistory newWorkHistory = new ShopWorkHistory();
            newWorkHistory.setUser(targetUser);
            newWorkHistory.setShop(currentUser.getCurrentShop() != null
                    ? currentUser.getCurrentShop()
                    : currentUser);
            newWorkHistory.setRole(role);
            newWorkHistory.setIsCurrent(true);
            newWorkHistory.setJoinedAt(LocalDateTime.now());
            shopWorkHistoryRepository.save(newWorkHistory);

        } else {
            accountRole.setIsActive(false);
            accountRoleRepository.save(accountRole);

            boolean hasOtherActiveRole = targetUser.getAccount()
                    .getAccountRoles()
                    .stream()
                    .anyMatch(ar -> !ar.getId()
                            .equals(accountRole.getId())
                            && ar.getIsActive());

            if (!hasOtherActiveRole) {
                targetUser.getAccount()
                        .setIsActive(false);
                accountRepository.save(targetUser.getAccount());
            }

            shopWorkHistoryRepository
                    .findByUserIdAndRoleIdAndIsCurrentTrue(targetUser.getId(), role.getId())
                    .ifPresent(wh -> {
                        wh.setIsCurrent(false);
                        wh.setLeftAt(LocalDateTime.now());
                        shopWorkHistoryRepository.save(wh);
                    });
        }
    }

    @Transactional
    public void createEmployee(int userId, CreateEmployeeUserRequest request) {
        User currentUser = getUser(userId);
        Role role = roleUserService.getRole(request.getRoleId());
        roleUserService.checkOwnerPermission(currentUser, role);

        Integer shopId = userUserService.getShopId(currentUser);
        User shopOwner = getUser(shopId);

        Optional<Account> existingAccountOpt = accountRepository.findByEmail(request.getEmail());
        Account account;
        User targetUser;

        if (existingAccountOpt.isPresent()) {
            account = existingAccountOpt.get();
            targetUser = account.getUser();

            // Đã thuộc shop khác rồi
            if (targetUser.getCurrentShop() != null) {
                throw new AppException(UserErrorCode.USER_EMPLOYEE_ALREADY_IN_ANOTHER_SHOP);
            }

            // Gán vào shop
            targetUser.setCurrentShop(shopOwner);
            userRepository.save(targetUser);

        } else {
            // Kiểm tra sđt trùng
            if (userRepository.existsByPhoneNumber(request.getPhoneNumber())) {
                throw new AppException(UserErrorCode.USER_PHONE_NUMBER_EXISTED);
            }

            // Generate password
            String rawPassword = PasswordUtils.generateTempPassword();
            String hashedPassword = passwordEncoder.encode(rawPassword);

            // Tạo Account
            account = new Account();
            account.setEmail(request.getEmail());
            account.setPassword(hashedPassword);
            account.setIsVerified(true);
            account.setIsActive(true);
            accountRepository.save(account);

            // Tạo User
            targetUser = new User();
            targetUser.setFirstName(request.getFirstName());
            targetUser.setLastName(request.getLastName());
            targetUser.setPhoneNumber(request.getPhoneNumber());
            targetUser.setAccount(account);
            targetUser.setCurrentShop(shopOwner);
            userRepository.save(targetUser);

            // Gửi mail thông báo tài khoản
            emailService.sendNewEmployeeAccountEmail(
                    request.getEmail(),
                    rawPassword,
                    request.getFirstName(),
                    request.getLastName());
        }

        // Tạo AccountRole
        AccountRole accountRole = new AccountRole();
        accountRole.setAccount(account);
        accountRole.setRole(role);
        accountRole.setIsActive(true);
        accountRoleRepository.save(accountRole);

        // Tạo ShopWorkHistory
        ShopWorkHistory workHistory = new ShopWorkHistory();
        workHistory.setUser(targetUser);
        workHistory.setShop(shopOwner);
        workHistory.setRole(role);
        workHistory.setIsCurrent(true);
        workHistory.setJoinedAt(LocalDateTime.now());
        shopWorkHistoryRepository.save(workHistory);
    }

    @Transactional
    public void updateEmployee(
            int userId,
            int id,
            UpdateEmployeeUserRequest request) {
        User currentUser = getUser(userId);
        User targetUser = getUser(id);

        checkShopPermission(currentUser, targetUser);

        // Kiểm tra sđt trùng nếu thay đổi
        if (!targetUser.getPhoneNumber().equals(request.getPhoneNumber())
                && userRepository.existsByPhoneNumber(request.getPhoneNumber())) {
            throw new AppException(UserErrorCode.USER_PHONE_NUMBER_EXISTED);
        }

        targetUser.setFirstName(request.getFirstName());
        targetUser.setLastName(request.getLastName());
        targetUser.setPhoneNumber(request.getPhoneNumber());
        userRepository.save(targetUser);
    }

    public ListResponse<ShopWorkHistoryListUserDto> listWorkHistory(
            int userId,
            int targetUserId,
            ShopWorkHistorySearchUserRequest request) {
        User currentUser = getUser(userId);
        User targetUser = getUser(targetUserId);

        checkShopPermission(currentUser, targetUser);

        int page = request.getPage();
        int limit = request.getLimit();
        String search = request.getSearch();
        Boolean isCurrent = request.getIsCurrent();
        String sort = request.getSort();
        LocalDateTime startDate = request.getStartDate() != null && !request.getStartDate()
                .isBlank()
                ? LocalDateTime.parse(request.getStartDate())
                : null;

        LocalDateTime endDate = request.getEndDate() != null && !request.getEndDate()
                .isBlank()
                ? LocalDateTime.parse(request.getEndDate())
                : null;

        Sort sortOrder = "OLDEST".equalsIgnoreCase(sort)
                ? Sort.by("joinedAt").ascending()
                : Sort.by("joinedAt").descending();

        Pageable pageable = PageRequest.of(page - 1, limit, sortOrder);

        Page<ShopWorkHistory> pageData = shopWorkHistoryRepository.findAllByUserIdWithFilter(
                targetUserId,
                isCurrent,
                search,
                startDate,
                endDate,
                pageable);

        List<ShopWorkHistoryListUserDto> list = UserMapper.toShopWorkHistoryListDto(pageData.getContent());

        int total = (int) pageData.getTotalElements();
        Pagination pagination = new Pagination(total, page, limit, pageData.getTotalPages());

        ListResponse<ShopWorkHistoryListUserDto> data = new ListResponse<>();
        data.setList(list);
        data.setPagination(pagination);

        return data;
    }

    private User getUser(int userId) {
        return userRepository.findByIdWithRoles(userId)
                .orElseThrow(() -> new AppException(UserErrorCode.USER_NOT_FOUND));
    }

    private void checkShopPermission(User currentUser, User targetUser) {
        Integer currentShopId = currentUser.getCurrentShop() != null
                ? currentUser.getCurrentShop()
                .getId()
                : currentUser.getId();

        Integer targetShopId = targetUser.getCurrentShop() != null
                ? targetUser.getCurrentShop()
                .getId()
                : null;

        if (targetShopId == null || !targetShopId.equals(currentShopId)) {
            throw new AppException(UserErrorCode.USER_EMPLOYEE_PERMISSION_DENIED);
        }
    }

    private void validateNoActiveShopRole(User targetUser, Integer shopId) {
        boolean hasActiveShopRole = targetUser.getAccount()
                .getAccountRoles()
                .stream()
                .anyMatch(ar -> ar.getIsActive()
                        && ar.getRole() != null
                        && ar.getRole()
                        .getUserOwner() != null);

        if (hasActiveShopRole) {
            throw new AppException(UserErrorCode.USER_EMPLOYEE_HAS_ACTIVE_ROLE);
        }
    }
}