package com.logistics.service.manager;

import com.logistics.constants.EmployeeConstant;
import com.logistics.dto.BaseAuditLogDto;
import com.logistics.dto.manager.employee.ManagerEmployeeListDto;
import com.logistics.dto.manager.employee.ManagerEmployeeListWithShipperAssignmentDto;
import com.logistics.dto.manager.employee.ManagerEmployeePerformanceDto;
import com.logistics.entity.*;
import com.logistics.enums.*;
import com.logistics.exception.AppException;
import com.logistics.exception.enums.CommonErrorCode;
import com.logistics.exception.enums.EmployeeErrorCode;
import com.logistics.exception.enums.RoleErrorCode;
import com.logistics.exception.enums.UserErrorCode;
import com.logistics.mapper.AuditLogMapper;
import com.logistics.mapper.EmployeeMapper;
import com.logistics.repository.*;
import com.logistics.request.SearchRequest;
import com.logistics.request.manager.audit.AuditLogSearchRequest;
import com.logistics.request.manager.employee.ManagerEmployeeEditRequest;
import com.logistics.request.manager.employee.ManagerEmployeeSearchRequest;
import com.logistics.response.ListResponse;
import com.logistics.response.Pagination;
import com.logistics.service.common.NotificationService;
import com.logistics.specification.AuditLogSpecification;
import com.logistics.specification.EmployeeSpecification;
import com.logistics.specification.UserSpecification;
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
import java.text.DecimalFormat;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static com.logistics.utils.AuditLogUtils.translateAuditLogAction;
import static com.logistics.utils.AuditLogUtils.translateAuditLogStatus;
import static com.logistics.utils.EmployeeUtils.translateEmployeeShift;
import static com.logistics.utils.EmployeeUtils.translateEmployeeStatus;
import static com.logistics.utils.EntityTypeUtils.translateEntityType;
import static com.logistics.utils.RoleUtils.translateSystemRoleName;

@Service
@RequiredArgsConstructor
public class EmployeeManagerService {

    private final EmployeeRepository employeeRepository;

    private final AccountRepository accountRepository;

    private final UserRepository userRepository;

    private final RoleRepository roleRepository;

    private final AccountRoleRepository accountRoleRepository;

    private final PasswordEncoder passwordEncoder;

    private final EmailService emailService;

    private final NotificationService notificationService;

    private final AuditLogRepository auditLogRepository;

    public Office getManagedOfficeByUserId(Integer userId) {
        List<Employee> employees = employeeRepository.findByUserId(userId);

        Employee managed = employees.stream()
                .filter(emp -> emp.getStatus() != EmployeeStatus.LEAVE)
                .filter(emp -> emp.getOffice() != null && emp.getOffice()
                        .getManager()
                        .getId()
                        .equals(emp.getId()))
                .findFirst()
                .orElseThrow(() -> new AppException(EmployeeErrorCode.EMPLOYEE_MANAGER_NOT_FOUND));

        return managed.getOffice();
    }

    public ListResponse<ManagerEmployeeListDto> list(
            int userId,
            ManagerEmployeeSearchRequest request) {
            Office office = getManagedOfficeByUserId(userId);

            int page = request.getPage();
            int limit = request.getLimit();
            String search = request.getSearch();
            String sort = request.getSort();
            String status = request.getStatus();
            String role = request.getRole();
            String shift = request.getShift();

            LocalDateTime startDate = request.getStartDate() != null && !request.getStartDate()
                    .isBlank()
                    ? Instant.parse(request.getStartDate())
                    .atZone(ZoneId.systemDefault())
                    .toLocalDateTime()
                    : null;

            LocalDateTime endDate = request.getEndDate() != null && !request.getEndDate()
                    .isBlank()
                    ? Instant.parse(request.getEndDate())
                    .atZone(ZoneId.systemDefault())
                    .toLocalDateTime()
                    : null;

            Specification<Employee> spec = EmployeeSpecification.unrestrictedEmployee()
                    .and(EmployeeSpecification.officeId(office.getId()))
                    .and(EmployeeSpecification.search(search))
                    .and(EmployeeSpecification.status(status))
                    .and(EmployeeSpecification.role(role, true))
                    .and(EmployeeSpecification.shift(shift))
                    .and(EmployeeSpecification.hireDateBetween(startDate, endDate));

            Sort sortOpt = switch (sort.toLowerCase()) {
                case "newest" -> Sort.by("hireDate")
                        .descending();
                case "oldest" -> Sort.by("hireDate")
                        .ascending();
                default -> Sort.unsorted();
            };

            Pageable pageable = PageRequest.of(page - 1, limit, sortOpt);
            Page<Employee> pageData = employeeRepository.findAll(spec, pageable);

            List<ManagerEmployeeListDto> list = pageData.getContent()
                    .stream()
                    .map(EmployeeMapper::toManagerEmployeeListDto)
                    .toList();

            int total = (int) pageData.getTotalElements();

            Pagination pagination = new Pagination(total, page, limit, pageData.getTotalPages());

            ListResponse<ManagerEmployeeListDto> data = new ListResponse<>();
            data.setList(list);
            data.setPagination(pagination);

            return data;
    }

    public byte[] export(int userId, ManagerEmployeeSearchRequest request) {
        Office office = getManagedOfficeByUserId(userId);

        String search = request.getSearch();
        String sort = request.getSort();
        String status = request.getStatus();
        String role = request.getRole();
        String shift = request.getShift();

        LocalDateTime startDate = request.getStartDate() != null && !request.getStartDate().isBlank()
                ? Instant.parse(request.getStartDate()).atZone(ZoneId.systemDefault()).toLocalDateTime()
                : null;
        LocalDateTime endDate = request.getEndDate() != null && !request.getEndDate().isBlank()
                ? Instant.parse(request.getEndDate()).atZone(ZoneId.systemDefault()).toLocalDateTime()
                : null;

        Specification<Employee> spec = EmployeeSpecification.unrestrictedEmployee()
                .and(EmployeeSpecification.officeId(office.getId()))
                .and(EmployeeSpecification.search(search))
                .and(EmployeeSpecification.status(status))
                .and(EmployeeSpecification.role(role, true))
                .and(EmployeeSpecification.shift(shift))
                .and(EmployeeSpecification.hireDateBetween(startDate, endDate));

        Sort sortOpt = sort != null ? switch (sort.toLowerCase()) {
            case "newest" -> Sort.by("hireDate").descending();
            case "oldest" -> Sort.by("hireDate").ascending();
            default -> Sort.by("hireDate").descending();
        } : Sort.by("hireDate").descending();

        List<Employee> employees = employeeRepository.findAll(spec, sortOpt);

        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Employees");

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
                    "Mã NV",
                    "Họ tên",
                    "Số điện thoại", "Email",
                    "Chức vụ",
                    "Ca làm việc",
                    "Trạng thái",
                    "Ngày vào làm"
            };

            Row header = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                Cell cell = header.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd");

            int rowIdx = 1;
            for (Employee e : employees) {
                Row row = sheet.createRow(rowIdx++);

                row.createCell(0).setCellValue(e.getCode() != null ? e.getCode() : "");

                String lastName = (e.getUser() != null && e.getUser().getLastName() != null) ? e.getUser().getLastName() : "";
                String firstName = (e.getUser() != null && e.getUser().getFirstName() != null) ? e.getUser().getFirstName() : "";
                row.createCell(1).setCellValue((lastName + " " + firstName).trim());

                row.createCell(2).setCellValue(e.getUser() != null && e.getUser().getPhoneNumber() != null ? e.getUser().getPhoneNumber() : "");
                row.createCell(3).setCellValue(
                        e.getUser() != null
                                && e.getUser().getAccount() != null
                                && e.getUser().getAccount().getEmail() != null
                                ? e.getUser().getAccount().getEmail()
                                : "");

                boolean isSystem = e.getAccountRole() != null
                        && e.getAccountRole().getRole() != null
                        && e.getAccountRole().getRole().getUserOwner() == null;
                String roleName = (e.getAccountRole() != null && e.getAccountRole().getRole() != null)
                        ? e.getAccountRole().getRole().getName()
                        : null;
                row.createCell(4).setCellValue(translateSystemRoleName(roleName, isSystem));

                row.createCell(5).setCellValue(translateEmployeeShift(e.getShift()));
                row.createCell(6).setCellValue(translateEmployeeStatus(e.getStatus()));
                row.createCell(7).setCellValue(e.getHireDate() != null ? e.getHireDate().format(dtf) : "");
            }

            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            workbook.write(out);
            return out.toByteArray();

        } catch (Exception e) {
            throw new AppException(CommonErrorCode.EXPORT_EXCEL_ERROR);
        }
    }

    public ListResponse<ManagerEmployeePerformanceDto> getEmployeePerformance(
            int userId,
            SearchRequest request) {
            Office office = getManagedOfficeByUserId(userId);

            int page = request.getPage();
            int limit = request.getLimit();
            String search = request.getSearch();
            String status = request.getStatus();
            String shift = request.getShift();

            EmployeeShift employeeShift = null;
            EmployeeStatus employeeStatus = null;

            if (shift != null && !shift.isBlank()) {
                try {
                    employeeShift = EmployeeShift.valueOf(shift.trim()
                            .toUpperCase());
                } catch (IllegalArgumentException ex) {
                    throw new AppException(EmployeeErrorCode.EMPLOYEE_SHIFT_INVALID);
                }
            }

            if (status != null && !status.isBlank()) {
                try {
                    employeeStatus = EmployeeStatus.valueOf(status.trim()
                            .toUpperCase());
                } catch (IllegalArgumentException ex) {
                    throw new AppException(EmployeeErrorCode.EMPLOYEE_STATUS_INVALID);
                }
            }

            Pageable pageable = PageRequest.of(page - 1, limit);

            Page<ManagerEmployeePerformanceDto> pageData = employeeRepository.getShipperPerformance(
                    office.getId(),
                    search == null || search.isBlank() ? null : search,
                    employeeShift != null ? employeeShift.name() : null,
                    employeeStatus != null ? employeeStatus.name() : null,
                    pageable);

            Pagination pagination = new Pagination(
                    (int) pageData.getTotalElements(),
                    page,
                    limit,
                    pageData.getTotalPages());

            ListResponse<ManagerEmployeePerformanceDto> data = new ListResponse<>();
            data.setList(pageData.getContent());
            data.setPagination(pagination);

            return data;
    }

    @Transactional
    public String createEmployee(int creatorUserId, ManagerEmployeeEditRequest req) {
            validateCreate(req);

            // 1. Lấy office của người dùng hiện tại
            Office office = getManagedOfficeByUserId(creatorUserId);

            String email = req.getUserEmail()
                    .trim()
                    .toLowerCase();
            String phone = req.getUserPhoneNumber()
                    .trim();

            // 2. tìm account theo email
            Optional<Account> optAccount = accountRepository.findByEmail(email);

            // CASE A: Account CHƯA TỒN TẠI -> tạo toàn bộ
            if (optAccount.isEmpty()) {
                // phone phải unique toàn hệ thống
                if (userRepository.existsByPhoneNumber(phone)) {
                    throw new AppException(UserErrorCode.USER_PHONE_NUMBER_EXISTED);
                }

                String DEFAULT_TEMP_PASSWORD = PasswordUtils.generateTempPassword();

                // tạo account
                Account account = new Account();
                account.setEmail(email);
                account.setPassword(passwordEncoder.encode(DEFAULT_TEMP_PASSWORD));
                account.setIsActive(true);
                account.setIsVerified(true);
                account = accountRepository.save(account);

                emailService.sendNewEmployeeAccountEmail(
                        email,
                        DEFAULT_TEMP_PASSWORD,
                        req.getUserFirstName(),
                        req.getUserLastName());

                // tạo user
                User user = new User();
                user.setAccount(account);
                user.setFirstName(req.getUserFirstName());
                user.setLastName(req.getUserLastName());
                user.setPhoneNumber(phone);
                user = userRepository.save(user);

                account.setUser(user);
                accountRepository.save(account);

                Role role = getSystemRoleByName(req.getUserRole());

                AccountRole accountRole = new AccountRole();
                accountRole.setAccount(account);
                accountRole.setRole(role);
                accountRole.setIsActive(true);
                accountRole = accountRoleRepository.save(accountRole);

                Employee emp = buildEmployee(user, accountRole, office, req);
                employeeRepository.save(emp);

                notificationService.create(
                        "Chào mừng bạn đến bưu cục " + office.getName(),
                        "Bạn đã được thêm vào bưu cục với chức vụ " + req.getUserRole() +
                                ". Hãy sử dụng email của bạn để đăng nhập.",
                        "employee",
                        user.getId(),
                        null,
                        "employees",
                        emp.getId()
                                .toString());

                return EmployeeConstant.SUCCESS_ADD_EMPLOYEE_MSG;
            }

            // CASE B: Account ĐÃ TỒN TẠI -> xử lý theo rules
            Account account = optAccount.get();

            // nếu account inactive -> kích hoạt
            if (Boolean.FALSE.equals(account.getIsActive())) {
                account.setIsActive(true);
                accountRepository.save(account);
            }

            // load tất cả accountRole (active) của account
            List<AccountRole> activeAccountRoles = accountRoleRepository
                    .findByAccountIdAndIsActiveTrue(account.getId());

            boolean hasActiveUserRole = activeAccountRoles.stream()
                    .anyMatch(ar ->
                            "User".equalsIgnoreCase(ar.getRole().getName()) && ar.getRole().getUserOwner() == null);
            long activeNonUserRolesCount = activeAccountRoles.stream()
                    .filter(ar -> !"User".equalsIgnoreCase(ar.getRole().getName()) && ar.getRole().getUserOwner() == null)
                    .count();

            // role muốn thêm
            Role wantedRole = getSystemRoleByName(req.getUserRole());

            // tìm accountRole hiện có với role này (cả active/inactive)
            Optional<AccountRole> optExistingAr = accountRoleRepository.findByAccountIdAndRoleId(account.getId(),
                    wantedRole.getId());

            if (optExistingAr.isPresent()) {
                AccountRole ar = optExistingAr.get();

                // ===== nếu accountRole hiện tại đang active =====
                if (Boolean.TRUE.equals(ar.getIsActive())) {
                    // Lấy tất cả employee liên quan tới accountRole này
                    List<Employee> employees = employeeRepository.findAllByAccountRoleId(ar.getId());

                    // 1. Nếu có employee (ACTIVE/INACTIVE) ở cùng office -> lỗi (C đúng)
                    boolean existsActiveOrInactiveHere = employees.stream()
                            .anyMatch(e -> e.getOffice()
                                    .getId()
                                    .equals(office.getId())
                                    && e.getStatus() != EmployeeStatus.LEAVE);
                    if (existsActiveOrInactiveHere) {
                        throw  new AppException(EmployeeErrorCode.EMPLOYEE_ACCOUNT_IN_OFFICE);
                    }

                    // 2. Nếu có employee ACTIVE/INACTIVE ở office khác -> lỗi (một người chỉ làm 1
                    // bưu cục)
                    boolean existsActiveOrInactiveElsewhere = employees.stream()
                            .anyMatch(e -> !e.getOffice()
                                    .getId()
                                    .equals(office.getId())
                                    && e.getStatus() != EmployeeStatus.LEAVE);
                    if (existsActiveOrInactiveElsewhere) {
                        throw new AppException(EmployeeErrorCode.EMPLOYEE_ACCOUNT_IN_OTHER_OFFICE);
                    }

                    // 3. Nếu tới đây => tất cả employee liên quan đều LEAVE (hoặc không có) -> tạo
                    // employee mới
                    Employee newEmp = buildEmployee(account.getUser(), ar, office, req);
                    employeeRepository.save(newEmp);
                    return EmployeeConstant.SUCCESS_ADD_EMPLOYEE_MSG;
                }

                long totalActiveAfterRestore = activeAccountRoles.size() + 1; // kế hoạch restore ar -> +1
                // rule: nếu có User active -> max 2 (User + 1 non-user). Nếu không có User
                // active -> max 1
                if (hasActiveUserRole) {
                    if (totalActiveAfterRestore > 2) {
                        throw new AppException(EmployeeErrorCode.EMPLOYEE_LIMIT_ACTIVE_ROLES_REACHED);
                    }
                } else {
                    if (totalActiveAfterRestore > 1) {
                        throw new AppException(EmployeeErrorCode.EMPLOYEE_LIMIT_ACTIVE_ROLES_REACHED);
                    }
                }

                // ok restore accountRole
                ar.setIsActive(true);
                accountRoleRepository.save(ar);

                // xử lý employee liên quan:
                List<Employee> employees = employeeRepository.findAllByAccountRoleId(ar.getId());

                // nếu tồn tại employee cùng office -> khôi phục trạng thái
                Optional<Employee> empHereOpt = employees.stream()
                        .filter(e -> e.getOffice()
                                .getId()
                                .equals(office.getId()))
                        .findFirst();

                if (empHereOpt.isPresent()) {
                    Employee e = empHereOpt.get();
                    if (e.getStatus() == EmployeeStatus.LEAVE) {
                        e.setStatus(EmployeeStatus.ACTIVE);
                    }
                    e.setHireDate(req.getHireDate() != null
                            ? Instant.parse(req.getHireDate())
                            .atZone(ZoneId.systemDefault())
                            .toLocalDateTime()
                            : e.getHireDate());
                    employeeRepository.save(e);
                    return EmployeeConstant.MSG_RESTORED_EMPLOYEE_SUCCESS;
                }

                // nếu không có employee cùng office:
                // nhưng cần check rule "1 account chỉ được làm 1 office" => nếu có employee
                // ACTIVE/INACTIVE ở nơi khác => lỗi
                boolean existsActiveOrInactiveElsewhere = employees.stream()
                        .anyMatch(e -> e.getStatus() != EmployeeStatus.LEAVE);
                if (existsActiveOrInactiveElsewhere) {
                    throw  new AppException(EmployeeErrorCode.EMPLOYEE_ACCOUNT_IN_OTHER_OFFICE);
                }

                // tạo employee mới
                Employee newEmp = buildEmployee(account.getUser(), ar, office, req);
                employeeRepository.save(newEmp);

                notificationService.create(
                        "Nhân viên đã được kích hoạt lại",
                        "Bạn đã được kích hoạt trở lại với chức vụ " + ar.getRole()
                                .getName() +
                                ". Hãy đăng nhập để tiếp tục công việc.",
                        "employee",
                        account.getUser()
                                .getId(),
                        null,
                        "employees",
                        newEmp.getId()
                                .toString());

                return EmployeeConstant.MSG_RESTORED_EMPLOYEE_SUCCESS;
            }

            /*
             * =========================
             * CASE C: Chưa có AccountRole cho role này -> tạo mới AccountRole
             * =========================
             */
            // check policy active roles trước khi tạo new AccountRole active:
            // nếu đã có User active -> cho phép 1 non-user active (tổng 2)
            // nếu không có User active -> chỉ cho phép 1 active total
            if (hasActiveUserRole) {
                if (activeNonUserRolesCount >= 1) {
                    throw new AppException(EmployeeErrorCode.EMPLOYEE_LIMIT_ACTIVE_ROLES_REACHED);
                }
            } else {
                if (activeNonUserRolesCount >= 1) {
                    // account đã có 1 non-user active và không có user -> không thể thêm nữa
                    throw new AppException(EmployeeErrorCode.EMPLOYEE_LIMIT_ACTIVE_ROLES_REACHED);
                }
            }

            // tạo accountRole mới active
            AccountRole newAr = new AccountRole();
            newAr.setAccount(account);
            newAr.setRole(wantedRole);
            newAr.setIsActive(true);
            accountRoleRepository.save(newAr);

            // Kiểm tra employee active/inactive ở office khác
            List<Employee> allEmployeesForAccount = employeeRepository.findAllByAccountId(account.getId());
            boolean existsActiveOrInactiveElsewhere = allEmployeesForAccount.stream()
                    .anyMatch(e -> !e.getOffice()
                            .getId()
                            .equals(office.getId())
                            && e.getStatus() != EmployeeStatus.LEAVE);

            if (existsActiveOrInactiveElsewhere) {
                // Chặn nếu ACTIVE/INACTIVE ở office khác
                throw new AppException(EmployeeErrorCode.EMPLOYEE_ACCOUNT_IN_OTHER_OFFICE);
            }

            // Nếu không có employee active/inactive ở office khác -> tạo employee mới bình
            // thường
            Employee emp = buildEmployee(account.getUser(), newAr, office, req);
            employeeRepository.save(emp);

            return EmployeeConstant.SUCCESS_ADD_EMPLOYEE_MSG;
    }

    @Transactional
    public String updateEmployee(int editorUserId, int employeeId, ManagerEmployeeEditRequest req) {
            validateEdit(req);

            Office editorOffice = getManagedOfficeByUserId(editorUserId);

            Employee emp = employeeRepository.findById(employeeId)
                    .orElseThrow(() -> new AppException(EmployeeErrorCode.EMPLOYEE_NOT_FOUND));

            if (!emp.getOffice()
                    .getId()
                    .equals(editorOffice.getId())) {
                throw new AppException(EmployeeErrorCode.EMPLOYEE_MANAGER_ACCESS_DENIED);
            }

            Account account = emp.getAccountRole()
                    .getAccount();
            EmployeeStatus currentStatus = emp.getStatus();

            // Mới thêm
            if (!emp.getAccountRole()
                    .getIsActive()) {
                emp.getAccountRole()
                        .setIsActive(true);
                accountRoleRepository.save(emp.getAccountRole());
            }

            // ===== Kiểm tra employee ACTIVE/INACTIVE khác ở office khác =====
            boolean hasOtherOfficeActive = employeeRepository.findAllByAccountId(account.getId())
                    .stream()
                    .filter(e -> e.getStatus() == EmployeeStatus.ACTIVE
                            || e.getStatus() == EmployeeStatus.INACTIVE)
                    .anyMatch(e -> !e.getOffice()
                            .getId()
                            .equals(emp.getOffice()
                                    .getId()));

            if (hasOtherOfficeActive) {
                throw new AppException(EmployeeErrorCode.EMPLOYEE_ACCOUNT_IN_OTHER_OFFICE);
            }

            // ===== Xử lý role nếu thay đổi =====
            Role newRole = getSystemRoleByName(req.getUserRole());
            AccountRole currentAR = emp.getAccountRole();

            EmployeeStatus newStatus = req.getStatus() != null && !req.getStatus()
                    .isBlank()
                    ? EmployeeStatus.valueOf(req.getStatus())
                    : currentStatus;

            if (!currentAR.getRole()
                    .getId()
                    .equals(newRole.getId()) && newStatus == EmployeeStatus.LEAVE) {
                throw new AppException(EmployeeErrorCode.EMPLOYEE_CANNOT_CHANGE_ROLE_AND_LEAVE_SIMULTANEOUSLY);
            }

            if (!currentAR.getRole()
                    .getId()
                    .equals(newRole.getId())) {
                Optional<AccountRole> optAR = accountRoleRepository.findByAccountIdAndRoleId(account.getId(),
                        newRole.getId());
                AccountRole newAR;

                if (optAR.isPresent()) {
                    newAR = optAR.get();
                    if (!newAR.getIsActive()) {
                        newAR.setIsActive(true);
                        accountRoleRepository.save(newAR);
                    }
                } else {
                    // tạo accountRole mới và check policy active roles
                    List<AccountRole> activeRoles = accountRoleRepository
                            .findByAccountIdAndIsActiveTrue(account.getId());
                    boolean hasUserActive = activeRoles.stream()
                            .anyMatch(r -> "User".equalsIgnoreCase(r.getRole().getName()) && r.getRole().getUserOwner() == null);
                    long activeNonUserCount = activeRoles.stream()
                            .filter(r -> !"User".equalsIgnoreCase(r.getRole().getName()) && r.getRole().getUserOwner() == null)
                            .count();
                    long totalActiveAfterAdd = activeRoles.size() + 1;

                    if (hasUserActive && totalActiveAfterAdd > 2 || !hasUserActive && totalActiveAfterAdd > 1) {
                        throw new AppException(EmployeeErrorCode.EMPLOYEE_LIMIT_ACTIVE_ROLES_REACHED);
                    }

                    newAR = new AccountRole();
                    newAR.setAccount(account);
                    newAR.setRole(newRole);
                    newAR.setIsActive(true);
                    accountRoleRepository.save(newAR);
                }

                // check xem có employee LEAVE cùng office + newAR không
                List<Employee> leaveEmployees = employeeRepository.findAllByAccountRoleId(newAR.getId())
                        .stream()
                        .filter(e -> e.getOffice()
                                .getId()
                                .equals(emp.getOffice()
                                        .getId())
                                && e.getStatus() == EmployeeStatus.LEAVE)
                        .toList();

                if (!leaveEmployees.isEmpty()) {
                    Employee toRestore = leaveEmployees.get(0);
                    toRestore.setStatus(EmployeeStatus.ACTIVE);
                    toRestore.setHireDate(req.getHireDate() != null
                            ? Instant.parse(req.getHireDate())
                            .atZone(ZoneId.systemDefault())
                            .toLocalDateTime()
                            : toRestore.getHireDate());
                    toRestore.setShift(req.getShift() != null
                            ? EmployeeShift.valueOf(req.getShift())
                            : toRestore.getShift());
                    employeeRepository.save(toRestore);
                } else {
                    Employee newEmp = buildEmployee(account.getUser(), newAR, emp.getOffice(), req);
                    employeeRepository.save(newEmp);
                }

                // deactivate employee + accountRole cũ
                currentAR.setIsActive(false);
                accountRoleRepository.save(currentAR);

                emp.setStatus(EmployeeStatus.LEAVE);
                employeeRepository.save(emp);

                return EmployeeConstant.SUCCESS_ADD_EMPLOYEE_MSG;
            }

            // ===== Cập nhật hireDate, shift (chỉ khi nhân viên chưa nghỉ) =====
            if (newStatus != EmployeeStatus.LEAVE) {
                if (req.getHireDate() != null) {
                    emp.setHireDate(Instant.parse(req.getHireDate())
                            .atZone(ZoneId.systemDefault())
                            .toLocalDateTime());
                }

                if (req.getShift() != null && !req.getShift()
                        .isBlank()) {
                    emp.setShift(EmployeeShift.valueOf(req.getShift()));
                }
            }

            // ===== Cập nhật status =====
            if (req.getStatus() != null && !req.getStatus()
                    .isBlank()) {

                // ACTIVE ↔ INACTIVE
                if ((currentStatus == EmployeeStatus.ACTIVE || currentStatus == EmployeeStatus.INACTIVE)
                        && (newStatus == EmployeeStatus.ACTIVE || newStatus == EmployeeStatus.INACTIVE)) {
                    emp.setStatus(newStatus);
                }
                // ACTIVE/INACTIVE → LEAVE
                else if ((currentStatus == EmployeeStatus.ACTIVE || currentStatus == EmployeeStatus.INACTIVE)
                        && newStatus == EmployeeStatus.LEAVE) {

                    emp.setStatus(EmployeeStatus.LEAVE);

                    // deactivate tất cả role
                    List<AccountRole> roles =
                            accountRoleRepository.findByAccountIdAndIsActiveTrue(account.getId());
                    roles.forEach(r -> r.setIsActive(false));
                    accountRoleRepository.saveAll(roles);

                    boolean hasActiveNonUserRole =
                            accountRoleRepository.findByAccountIdAndIsActiveTrue(account.getId())
                                    .stream()
                                    .anyMatch(r -> !"User".equalsIgnoreCase(r.getRole()
                                            .getName()));
                    boolean hasUserRole = accountRoleRepository.findByAccountIdAndIsActiveTrue(account.getId())
                            .stream()
                            .anyMatch(r -> "User".equalsIgnoreCase(r.getRole()
                                    .getName()));

                    // nếu không còn role active nào → tắt account
                    if (!hasActiveNonUserRole && !hasUserRole) {
                        account.setIsActive(false);
                        accountRepository.save(account);
                    }

                    emp.setStatus(EmployeeStatus.LEAVE);

                    notificationService.create(
                            "Bạn đã nghỉ việc",
                            "Trạng thái nhân viên của bạn tại bưu cục " + emp.getOffice()
                                    .getName() +
                                    " vừa được cập nhật là Đã nghỉ việc. Bạn sẽ không thể truy cập giao diện của chức vụ hiện tại.",
                            "employee",
                            account.getUser()
                                    .getId(),
                            null,
                            "employees",
                            emp.getId()
                                    .toString());
                }
                // LEAVE → ACTIVE/INACTIVE
                else if (currentStatus == EmployeeStatus.LEAVE
                        && (newStatus == EmployeeStatus.ACTIVE || newStatus == EmployeeStatus.INACTIVE)) {

                    AccountRole ar = emp.getAccountRole();

                    // tìm employee LEAVE cùng office + accountRole
                    List<Employee> leaveEmployees = employeeRepository.findAllByAccountRoleId(ar.getId())
                            .stream()
                            .filter(e -> e.getOffice()
                                    .getId()
                                    .equals(emp.getOffice()
                                            .getId())
                                    && e.getStatus() == EmployeeStatus.LEAVE)
                            .toList();

                    if (!leaveEmployees.isEmpty()) {
                        // restore employee cũ
                        Employee toRestore = leaveEmployees.get(0);
                        toRestore.setStatus(newStatus);
                        toRestore.setHireDate(req.getHireDate() != null
                                ? Instant.parse(req.getHireDate())
                                .atZone(ZoneId.systemDefault())
                                .toLocalDateTime()
                                : toRestore.getHireDate());
                        toRestore.setShift(req.getShift() != null
                                ? EmployeeShift.valueOf(req.getShift())
                                : toRestore.getShift());
                        employeeRepository.save(toRestore);
                    } else {
                        // nếu không có employee LEAVE nào → tạo mới
                        if (!ar.getIsActive()) {
                            List<AccountRole> activeRoles = accountRoleRepository
                                    .findByAccountIdAndIsActiveTrue(account.getId());
                            boolean hasUserActive = activeRoles.stream()
                                    .anyMatch(r -> "User".equalsIgnoreCase(r.getRole().getName()) && r.getRole().getUserOwner() == null);
                            long activeNonUserCount = activeRoles.stream()
                                    .filter(r -> !"User".equalsIgnoreCase(r.getRole().getName()) && r.getRole().getUserOwner() == null)
                                    .count();

                            if (hasUserActive && activeNonUserCount >= 1
                                    || !hasUserActive && activeNonUserCount >= 1) {
                                throw new AppException(EmployeeErrorCode.EMPLOYEE_LIMIT_ACTIVE_ROLES_REACHED);
                            }
                            ar.setIsActive(true);
                            accountRoleRepository.save(ar);
                        }

                        Employee newEmp = buildEmployee(account.getUser(), ar, emp.getOffice(), req);
                        employeeRepository.save(newEmp);
                    }

                    account.setIsActive(true);
                    accountRepository.save(account);
                }
            }

            notificationService.create(
                    "Thông tin nhân viên đã được cập nhật",
                    "Thông tin nhận viên của bạn vừa được quản lý bưu cục thay đổi. Nếu có bất kỳ thắc mắc nào vui lòng liên hệ với chúng tôi",
                    "employee",
                    emp.getUser()
                            .getId(),
                    null,
                    "employees",
                    emp.getId()
                            .toString());

            return EmployeeConstant.SUCCESS_UPDATE_EMPLOYEE_MSG;
    }

    private Employee buildEmployee(User user, AccountRole accountRole, Office office,
                                   ManagerEmployeeEditRequest req) {
        Employee emp = new Employee();
        emp.setUser(user);
        emp.setAccountRole(accountRole);
        emp.setOffice(office);
        emp.setHireDate(
                req.getHireDate() != null
                        ? Instant.parse(req.getHireDate())
                        .atZone(ZoneId.systemDefault())
                        .toLocalDateTime()
                        : LocalDateTime.now());
        emp.setShift(req.getShift() != null ? EmployeeShift.valueOf(req.getShift()) : EmployeeShift.FULL_DAY);
        emp.setStatus(req.getStatus() != null ? EmployeeStatus.valueOf(req.getStatus()) : EmployeeStatus.ACTIVE);
        return emp;
    }

    private void validateCreate(ManagerEmployeeEditRequest request) {
        List<String> missingFields = new ArrayList<>();
        if (request.getUserFirstName() == null || String.valueOf(request.getUserFirstName())
                .isBlank())
            missingFields.add("Tên");
        if (request.getUserLastName() == null || String.valueOf(request.getUserLastName())
                .isBlank())
            missingFields.add("Họ");
        if (request.getUserPhoneNumber() == null || request.getUserPhoneNumber()
                .isBlank())
            missingFields.add("Số điện thoại");
        if (request.getUserEmail() == null || request.getUserEmail()
                .isBlank())
            missingFields.add("Email");
        if (request.getUserRole() == null || request.getUserRole()
                .isBlank())
            missingFields.add("Chức vụ");

        if (!missingFields.isEmpty())
            throw new AppException(CommonErrorCode.MISSING_REQUIRED_FIELDS, String.join(", ", missingFields));

        if (request.getShift() != null && !request.getShift()
                .isBlank()) {
            try {
                EmployeeShift.valueOf(request.getShift());
            } catch (IllegalArgumentException e) {
                throw new AppException(EmployeeErrorCode.EMPLOYEE_SHIFT_INVALID);
            }
        }
        if (request.getStatus() != null && !request.getStatus()
                .isBlank()) {
            try {
                EmployeeStatus.valueOf(request.getStatus());
            } catch (IllegalArgumentException e) {
                throw new AppException(EmployeeErrorCode.EMPLOYEE_STATUS_INVALID);
            }
        }
    }

    private void validateEdit(ManagerEmployeeEditRequest request) {
        List<String> missingFields = new ArrayList<>();
        if (request.getUserRole() == null || request.getUserRole()
                .isBlank())
            missingFields.add("Chức vụ");

        if (!missingFields.isEmpty())
            throw new AppException(CommonErrorCode.MISSING_REQUIRED_FIELDS, String.join(", ", missingFields));

        if (request.getShift() != null && !request.getShift()
                .isBlank()) {
            try {
                EmployeeShift.valueOf(request.getShift());
            } catch (IllegalArgumentException e) {
                throw new AppException(EmployeeErrorCode.EMPLOYEE_SHIFT_INVALID);
            }
        }
        if (request.getStatus() != null && !request.getStatus()
                .isBlank()) {
            try {
                EmployeeStatus.valueOf(request.getStatus());
            } catch (IllegalArgumentException e) {
                throw new AppException(EmployeeErrorCode.EMPLOYEE_STATUS_INVALID);
            }
        }
    }

    public ListResponse<ManagerEmployeeListWithShipperAssignmentDto> getActiveShippersWithActiveAssignments(
            int userId,
            ManagerEmployeeSearchRequest request) {
            Office office = getManagedOfficeByUserId(userId);

            int page = request.getPage();
            int limit = request.getLimit();
            String search = request.getSearch();
            LocalDateTime now = LocalDateTime.now();

            // Specification để lấy user đang active + role SHIPPER + thuộc office + search
            Specification<User> spec = Specification
                    .where(UserSpecification.activeShippersInOffice(office.getId(), search));

            Pageable pageable = PageRequest.of(page - 1, limit, Sort.by("lastName")
                    .ascending());
            Page<User> pageData = userRepository.findAll(spec, pageable);

            // Map sang DTO
            List<ManagerEmployeeListWithShipperAssignmentDto> list = pageData.getContent()
                    .stream()
                    .map(user -> {
                        // Lấy employee code
                        Employee employee = user.getEmployees()
                                .stream()
                                .filter(emp -> emp.getAccountRole() != null &&
                                        emp.getAccountRole().getRole() != null &&
                                        "Shipper".equalsIgnoreCase(emp.getAccountRole().getRole().getName()) &&
                                        emp.getAccountRole().getRole().getUserOwner() == null
                                )
                                .findFirst()
                                .orElse(null);

                        // Lấy assignment còn hiệu lực
                        List<ShipperAssignment> activeAssignments = user.getShipperAssignments()
                                .stream()
                                .filter(sa -> sa.getEndAt() == null || !sa.getEndAt()
                                        .isBefore(now))
                                .toList();

                        String employeeCode = employee != null ? employee.getCode() : null;
                        Integer employeeId = employee != null ? employee.getId() : null;

                        return EmployeeMapper.toManagerEmployeeListDto(user, employeeCode, employeeId,
                                activeAssignments);
                    })
                    .toList();

            int total = (int) pageData.getTotalElements();
            Pagination pagination = new Pagination(total, page, limit, pageData.getTotalPages());

            ListResponse<ManagerEmployeeListWithShipperAssignmentDto> data = new ListResponse<>();
            data.setList(list);
            data.setPagination(pagination);

            return data;
    }

    public ListResponse<ManagerEmployeeListDto> getActiveShippers(
            int userId,
            ManagerEmployeeSearchRequest request) {
            Office office = getManagedOfficeByUserId(userId);

            int page = request.getPage();
            int limit = request.getLimit();
            String search = request.getSearch();

            Specification<Employee> spec = EmployeeSpecification.unrestrictedEmployee()
                    .and(EmployeeSpecification.officeId(office.getId()))
                    .and(EmployeeSpecification.search(search))
                    .and(EmployeeSpecification.excludeStatus(EmployeeStatus.LEAVE.name()))
                    .and(EmployeeSpecification.role("Shipper", true));

            Pageable pageable = PageRequest.of(page - 1, limit);
            Page<Employee> pageData = employeeRepository.findAll(spec, pageable);

            List<ManagerEmployeeListDto> list = pageData.getContent()
                    .stream()
                    .map(EmployeeMapper::toManagerEmployeeListDto)
                    .toList();

            int total = (int) pageData.getTotalElements();

            Pagination pagination = new Pagination(total, page, limit, pageData.getTotalPages());

            ListResponse<ManagerEmployeeListDto> data = new ListResponse<>();
            data.setList(list);
            data.setPagination(pagination);

            return data;
    }

    public ListResponse<ManagerEmployeeListDto> getActiveEmployeesByShipmentType(
            int userId,
            SearchRequest request) {
            String type = request.getType();
            String search = request.getSearch();
            int page = request.getPage();
            int limit = request.getLimit();

            Office office = getManagedOfficeByUserId(userId);

            if (type == null || type.isBlank()) {
                throw new AppException(CommonErrorCode.MISSING_REQUIRED_FIELDS, "Loại chuyến");
            }

            String role = type.equals(ShipmentType.DELIVERY.name()) ? "Shipper" : "Driver";

            Specification<Employee> spec = EmployeeSpecification.unrestrictedEmployee()
                    .and(EmployeeSpecification.officeId(office.getId()))
                    .and(EmployeeSpecification.search(search))
                    .and(EmployeeSpecification.excludeStatus(EmployeeStatus.LEAVE.name()))
                    .and(EmployeeSpecification.role(role, true));

            Pageable pageable = PageRequest.of(page - 1, limit, Sort.by("hireDate")
                    .ascending());
            Page<Employee> pageData = employeeRepository.findAll(spec, pageable);

            List<ManagerEmployeeListDto> list = pageData.getContent()
                    .stream()
                    .map(EmployeeMapper::toManagerEmployeeListDto)
                    .toList();

            Pagination pagination = new Pagination(
                    (int) pageData.getTotalElements(),
                    page,
                    limit,
                    pageData.getTotalPages());

            ListResponse<ManagerEmployeeListDto> data = new ListResponse<>();
            data.setList(list);
            data.setPagination(pagination);

            return data;
    }

    public byte[] exportPerformance(Integer userId, SearchRequest request) {
        List<ManagerEmployeePerformanceDto> datas = getEmployeePerformanceForExport(userId, request);

        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("EmployeePerformance");

            // Header style
            XSSFCellStyle headerStyle = (XSSFCellStyle) workbook.createCellStyle();
            XSSFFont font = (XSSFFont) workbook.createFont();
            font.setBold(true);
            font.setColor(new XSSFColor(new byte[]{(byte) 0xFF, (byte) 0xFF, (byte) 0xFF}, null));
            headerStyle.setFont(font);
            headerStyle.setAlignment(HorizontalAlignment.CENTER);
            headerStyle.setFillForegroundColor(
                    new XSSFColor(new byte[]{(byte) 0x1C, (byte) 0x3D, (byte) 0x90}, null));
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            // Header row
            Row header = sheet.createRow(0);
            String[] headers = {"Tên nhân viên", "Mã nhân viên",
                    "Số điện thoại", "Chức vụ",
                    "Ca làm việc", "Trạng thái làm việc", "Số chuyến",
                    "Tổng đơn", "Đơn thành công", "Tỉ lệ giao thành công",
                    "Thời gian giao TB"};
            for (int i = 0; i < headers.length; i++) {
                Cell cell = header.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            DecimalFormat df = new DecimalFormat("#,###");
            df.setGroupingUsed(true);
            df.setGroupingSize(3);

            int rowIdx = 1;
            for (ManagerEmployeePerformanceDto data : datas) {
                Row row = sheet.createRow(rowIdx++);

                row.createCell(0)
                        .setCellValue(data.getEmployeeName() != null ? data.getEmployeeName() : "");
                row.createCell(1)
                        .setCellValue(data.getEmployeeCode() != null ? data.getEmployeeCode() : "");
                row.createCell(2)
                        .setCellValue(data.getEmployeePhone() != null ? data.getEmployeePhone() : "");
                row.createCell(3)
                        .setCellValue(data.getEmployeeRole() != null ? data.getEmployeeRole() : "");

                // Ca làm việc (string)
                row.createCell(4)
                        .setCellValue(translateEmployeeShift(data.getEmployeeShift()));
                // Trạng thái làm việc
                row.createCell(5)
                        .setCellValue(translateEmployeeStatus(data.getEmployeeStatus()));

                row.createCell(6)
                        .setCellValue(data.getTotalShipments() != null ? df.format(data.getTotalShipments()) : "");
                row.createCell(7)
                        .setCellValue(data.getTotalOrders() != null ? df.format(data.getTotalOrders()) : "");
                row.createCell(8)
                        .setCellValue(
                                data.getCompletedOrders() != null ? df.format(data.getCompletedOrders()) : "");
                row.createCell(9)
                        .setCellValue(data.getCompletionRate() != null ? data.getCompletionRate() : 0);
                row.createCell(10)
                        .setCellValue(
                                data.getAvgTimePerOrder() != null ? df.format(data.getAvgTimePerOrder()) : "");
            }

            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            workbook.write(out);
            return out.toByteArray();
        } catch (Exception e) {
            throw new AppException(CommonErrorCode.EXPORT_EXCEL_ERROR);
        }
    }

    public List<ManagerEmployeePerformanceDto> getEmployeePerformanceForExport(
            int userId,
            SearchRequest request) {

        Office office = getManagedOfficeByUserId(userId);

        EmployeeShift employeeShift = null;
        EmployeeStatus employeeStatus = null;

        if (request.getShift() != null && !request.getShift()
                .isBlank()) {
            employeeShift = EmployeeShift.valueOf(request.getShift()
                    .trim()
                    .toUpperCase());
        }
        if (request.getStatus() != null && !request.getStatus()
                .isBlank()) {
            employeeStatus = EmployeeStatus.valueOf(request.getStatus()
                    .trim()
                    .toUpperCase());
        }

        return employeeRepository.getShipperPerformanceList(
                office.getId(),
                request.getSearch(),
                employeeShift != null ? employeeShift.name() : null,
                employeeStatus != null ? employeeStatus.name() : null);
    }

    public byte[] exportActiveShippersWithActiveAssignments(int userId, ManagerEmployeeSearchRequest request) {
        Office office = getManagedOfficeByUserId(userId);

        String search = request.getSearch();
        LocalDateTime now = LocalDateTime.now();

        Specification<User> spec = Specification
                .where(UserSpecification.activeShippersInOffice(office.getId(), search));

        List<User> users = userRepository.findAll(spec, Sort.by("lastName").ascending());

        try (Workbook workbook = new XSSFWorkbook()) {

            XSSFCellStyle headerStyle = (XSSFCellStyle) workbook.createCellStyle();
            XSSFFont font = (XSSFFont) workbook.createFont();
            font.setBold(true);
            font.setColor(new XSSFColor(new byte[]{(byte) 0xFF, (byte) 0xFF, (byte) 0xFF}, null));
            headerStyle.setFont(font);
            headerStyle.setAlignment(HorizontalAlignment.CENTER);
            headerStyle.setFillForegroundColor(
                    new XSSFColor(new byte[]{(byte) 0x1C, (byte) 0x3D, (byte) 0x90}, null));
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

            // ── Sheet 1: Danh sách nhân viên ──
            Sheet empSheet = workbook.createSheet("Danh sách Shipper");

            String[] empHeaders = {
                    "Mã NV",
                    "Họ tên",
                    "Email",
                    "SĐT",
                    "Ca làm",
                    "Trạng thái",
                    "Số vùng đang đảm nhận"
            };

            Row empHeader = empSheet.createRow(0);
            for (int i = 0; i < empHeaders.length; i++) {
                Cell cell = empHeader.createCell(i);
                cell.setCellValue(empHeaders[i]);
                cell.setCellStyle(headerStyle);
            }

            int empRowIdx = 1;
            for (User user : users) {
                Employee employee = user.getEmployees().stream()
                        .filter(emp -> emp.getAccountRole() != null &&
                                emp.getAccountRole().getRole() != null &&
                                "Shipper".equalsIgnoreCase(emp.getAccountRole().getRole().getName()))
                        .findFirst().orElse(null);

                List<ShipperAssignment> activeAssignments = user.getShipperAssignments().stream()
                        .filter(sa -> sa.getEndAt() == null || !sa.getEndAt().isBefore(now))
                        .toList();

                Row row = empSheet.createRow(empRowIdx++);
                row.createCell(0).setCellValue(employee != null && employee.getCode() != null ? employee.getCode() : "");
                row.createCell(1).setCellValue((user.getLastName() != null ? user.getLastName() : "") + " "
                        + (user.getFirstName() != null ? user.getFirstName() : ""));
                row.createCell(2).setCellValue(user.getAccount() != null && user.getAccount().getEmail() != null ? user.getAccount().getEmail() : "");
                row.createCell(3).setCellValue(user.getPhoneNumber() != null ? user.getPhoneNumber() : "");
                row.createCell(4).setCellValue(employee != null ? translateEmployeeShift(employee.getShift()) : "");
                row.createCell(5).setCellValue(employee != null ? translateEmployeeStatus(employee.getStatus()) : "");
                row.createCell(6).setCellValue(activeAssignments.size());
            }

            for (int i = 0; i < empHeaders.length; i++) {
                empSheet.autoSizeColumn(i);
            }

            // ── Sheet 2: Danh sách vùng phụ trách ──
            Sheet assignSheet = workbook.createSheet("Vùng phụ trách");

            String[] assignHeaders = {
                    "Mã NV",
                    "Họ tên",
                    "Mã phường/xã",
                    "Mã tỉnh/thành phố",
                    "Ngày bắt đầu",
                    "Ngày kết thúc",
                    "Ghi chú"
            };

            Row assignHeader = assignSheet.createRow(0);
            for (int i = 0; i < assignHeaders.length; i++) {
                Cell cell = assignHeader.createCell(i);
                cell.setCellValue(assignHeaders[i]);
                cell.setCellStyle(headerStyle);
            }

            int assignRowIdx = 1;
            for (User user : users) {
                Employee employee = user.getEmployees().stream()
                        .filter(emp -> emp.getAccountRole() != null &&
                                emp.getAccountRole().getRole() != null &&
                                "Shipper".equalsIgnoreCase(emp.getAccountRole().getRole().getName()))
                        .findFirst().orElse(null);

                String code = employee != null && employee.getCode() != null ? employee.getCode() : "";
                String fullName = (user.getLastName() != null ? user.getLastName() : "") + " "
                        + (user.getFirstName() != null ? user.getFirstName() : "");

                List<ShipperAssignment> activeAssignments = user.getShipperAssignments().stream()
                        .filter(sa -> sa.getEndAt() == null || !sa.getEndAt().isBefore(now))
                        .toList();

                for (ShipperAssignment sa : activeAssignments) {
                    Row row = assignSheet.createRow(assignRowIdx++);
                    row.createCell(0).setCellValue(code);
                    row.createCell(1).setCellValue(fullName);
                    row.createCell(2).setCellValue(sa.getWardCode() != null ? String.valueOf(sa.getWardCode()) : "");
                    row.createCell(3).setCellValue(sa.getCityCode() != null ? String.valueOf(sa.getCityCode()) : "");
                    row.createCell(4).setCellValue(sa.getStartAt() != null ? sa.getStartAt().format(dtf) : "N/A");
                    row.createCell(5).setCellValue(sa.getEndAt() != null ? sa.getEndAt().format(dtf) : "N/A");
                    row.createCell(6).setCellValue(sa.getNotes() != null ? sa.getNotes() : "");
                }
            }

            for (int i = 0; i < assignHeaders.length; i++) {
                assignSheet.autoSizeColumn(i);
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            workbook.write(out);
            return out.toByteArray();

        } catch (Exception e) {
            throw new AppException(CommonErrorCode.EXPORT_EXCEL_ERROR);
        }
    }

    public ListResponse<BaseAuditLogDto> listAuditLogsByUserId(
            Integer userId,
            Integer employeeId,
            AuditLogSearchRequest request) {

        Office editorOffice = getManagedOfficeByUserId(userId);

        Employee emp = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new AppException(EmployeeErrorCode.EMPLOYEE_NOT_FOUND));

        if (!emp.getOffice()
                .getId()
                .equals(editorOffice.getId())) {
            throw new AppException(EmployeeErrorCode.EMPLOYEE_MANAGER_ACCESS_DENIED);
        }

        User user = emp.getUser();
        if (user == null || user.getId() == null) {
            throw new AppException(UserErrorCode.USER_NOT_FOUND);
        }

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
                .and(AuditLogSpecification.user(user.getId()))
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

        Office editorOffice = getManagedOfficeByUserId(userId);

        Employee emp = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new AppException(EmployeeErrorCode.EMPLOYEE_NOT_FOUND));

        if (!emp.getOffice()
                .getId()
                .equals(editorOffice.getId())) {
            throw new AppException(EmployeeErrorCode.EMPLOYEE_MANAGER_ACCESS_DENIED);
        }

        User user = emp.getUser();
        if (user == null || user.getId() == null) {
            throw new AppException(UserErrorCode.USER_NOT_FOUND);
        }

        LocalDateTime startDate = request.getStartDate() != null && !request.getStartDate().isBlank()
                ? LocalDateTime.parse(request.getStartDate()) : null;
        LocalDateTime endDate = request.getEndDate() != null && !request.getEndDate().isBlank()
                ? LocalDateTime.parse(request.getEndDate()) : null;

        Specification<AuditLog> spec = AuditLogSpecification.unrestricted()
                .and(AuditLogSpecification.user(user.getId()))
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

    private Role getSystemRoleByName(String name) {
        return roleRepository.findByNameAndUserOwnerIsNull(name)
                .orElseThrow(() -> new AppException(RoleErrorCode.ROLE_NOT_FOUND));
    }
}