package com.logistics.service.user;

import com.logistics.dto.BaseAuditLogDto;
import com.logistics.dto.user.employee.EmployeeByRoleIdListUserDto;
import com.logistics.dto.user.employee.EmployeeListUserDto;
import com.logistics.dto.user.employee.ShopWorkHistoryListUserDto;
import com.logistics.entity.*;
import com.logistics.enums.AuditLogAction;
import com.logistics.enums.AuditLogStatus;
import com.logistics.enums.EntityType;
import com.logistics.exception.AppException;
import com.logistics.exception.enums.CommonErrorCode;
import com.logistics.exception.enums.UserErrorCode;
import com.logistics.mapper.AuditLogMapper;
import com.logistics.mapper.UserMapper;
import com.logistics.repository.*;
import com.logistics.request.manager.audit.AuditLogSearchRequest;
import com.logistics.request.user.employee.*;
import com.logistics.response.ListResponse;
import com.logistics.response.Pagination;
import com.logistics.specification.AuditLogSpecification;
import com.logistics.utils.EmailService;
import com.logistics.utils.PasswordUtils;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

import static com.logistics.utils.AuditLogUtils.translateAuditLogAction;
import static com.logistics.utils.AuditLogUtils.translateAuditLogStatus;
import static com.logistics.utils.EntityTypeUtils.translateEntityType;

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
    private final AuditLogRepository auditLogRepository;

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

    public ListResponse<BaseAuditLogDto> listAuditLogsByUserId(
            Integer userId,
            Integer employeeId,
            AuditLogSearchRequest request) {

        User currentUser = getUser(userId);
        User employee = getUser(employeeId);

        checkShopPermission(currentUser, employee);

        int page = request.getPage();
        int limit = request.getLimit();
        String search = request.getSearch();
        AuditLogStatus status = request.getStatus();
        EntityType entity = request.getEntity();
        String sort = request.getSort();
        AuditLogAction action = request.getAction();
        LocalDateTime startDate = request.getStartDate() != null && !request.getStartDate().isBlank()
                ? LocalDateTime.parse(request.getStartDate())
                : null;

        LocalDateTime endDate = request.getEndDate() != null && !request.getEndDate().isBlank()
                ? LocalDateTime.parse(request.getEndDate())
                : null;

        Specification<AuditLog> spec = AuditLogSpecification.unrestricted()
                .and(AuditLogSpecification.user(employeeId))
                .and(AuditLogSpecification.search(search))
                .and(AuditLogSpecification.status(status))
                .and(AuditLogSpecification.entityType(entity))
                .and(AuditLogSpecification.action(action))
                .and(AuditLogSpecification.createdAtBetween(startDate, endDate));

        Sort sortOpt = switch (sort.toLowerCase()) {
            case "newest" -> Sort.by("createdAt").descending();
            case "oldest" -> Sort.by("createdAt").ascending();
            default -> Sort.unsorted();
        };

        Pageable pageable = PageRequest.of(page - 1, limit, sortOpt);
        Page<AuditLog> pageData = auditLogRepository.findAll(spec, pageable);

        List<BaseAuditLogDto> list = AuditLogMapper.toBaseAuditLogDtoList(pageData.getContent());

        int total = (int) pageData.getTotalElements();

        Pagination pagination = new Pagination(total, page, limit, pageData.getTotalPages());

        ListResponse<BaseAuditLogDto> data = new ListResponse<>();
        data.setList(list);
        data.setPagination(pagination);

        return data;
    }

    public byte[] exportAuditLogsByUserId(
            Integer userId,
            Integer employeeId,
            AuditLogSearchRequest request) {

        User currentUser = getUser(userId);
        User employee = getUser(employeeId);
        checkShopPermission(currentUser, employee);

        LocalDateTime startDate = request.getStartDate() != null && !request.getStartDate().isBlank()
                ? LocalDateTime.parse(request.getStartDate()) : null;
        LocalDateTime endDate = request.getEndDate() != null && !request.getEndDate().isBlank()
                ? LocalDateTime.parse(request.getEndDate()) : null;

        Specification<AuditLog> spec = AuditLogSpecification.unrestricted()
                .and(AuditLogSpecification.user(employeeId))
                .and(AuditLogSpecification.search(request.getSearch()))
                .and(AuditLogSpecification.status(request.getStatus()))
                .and(AuditLogSpecification.entityType(request.getEntity()))
                .and(AuditLogSpecification.action(request.getAction()))
                .and(AuditLogSpecification.createdAtBetween(startDate, endDate));

        List<AuditLog> logs = auditLogRepository.findAll(spec, Sort.by("createdAt").descending());

        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Audit Logs");

            XSSFCellStyle headerStyle = (XSSFCellStyle) workbook.createCellStyle();
            XSSFFont font = (XSSFFont) workbook.createFont();
            font.setBold(true);
            font.setColor(new XSSFColor(new byte[]{(byte) 0xFF, (byte) 0xFF, (byte) 0xFF}, null));
            headerStyle.setFont(font);
            headerStyle.setAlignment(HorizontalAlignment.CENTER);
            headerStyle.setFillForegroundColor(
                    new XSSFColor(new byte[]{(byte) 0x1C, (byte) 0x3D, (byte) 0x90}, null));
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            String[] headers = {
                    "Thời gian",
                    "Đối tượng",
                    "Mã ĐT",
                    "Hành động",
                    "Mô tả",
                    "Trạng thái"};

            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            DateTimeFormatter dtf = DateTimeFormatter.ofPattern("HH:mm:ss dd/MM/yyyy");

            int rowIdx = 1;
            for (AuditLog log : logs) {
                Row row = sheet.createRow(rowIdx++);

                row.createCell(0).setCellValue(log.getCreatedAt() != null ? log.getCreatedAt().format(dtf) : "");
                row.createCell(1).setCellValue(translateEntityType(log.getEntity()));
                row.createCell(2).setCellValue(log.getId() != null ? log.getId().toString() : "");
                row.createCell(3).setCellValue(translateAuditLogAction(log.getAction()));
                row.createCell(4).setCellValue(log.getDescription() != null ? log.getDescription() : "");
                row.createCell(5).setCellValue(translateAuditLogStatus(log.getStatus()));
            }

            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            workbook.write(out);
            return out.toByteArray();

        } catch (Exception e) {
            throw new AppException(CommonErrorCode.EXPORT_EXCEL_ERROR, e);
        }
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