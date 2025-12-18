package com.logistics.service.manager;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.logistics.dto.manager.employee.ManagerEmployeeListDto;
import com.logistics.dto.manager.employee.ManagerEmployeeListWithShipperAssignmentDto;
import com.logistics.entity.Account;
import com.logistics.entity.AccountRole;
import com.logistics.entity.Employee;
import com.logistics.entity.Office;
import com.logistics.entity.Role;
import com.logistics.entity.ShipperAssignment;
import com.logistics.entity.User;
import com.logistics.enums.EmployeeShift;
import com.logistics.enums.EmployeeStatus;
import com.logistics.mapper.EmployeeMapper;
import com.logistics.repository.AccountRepository;
import com.logistics.repository.AccountRoleRepository;
import com.logistics.repository.EmployeeRepository;
import com.logistics.repository.RoleRepository;
import com.logistics.repository.UserRepository;
import com.logistics.request.manager.employee.ManagerEmployeeEditRequest;
import com.logistics.request.manager.employee.ManagerEmployeeSearchRequest;
import com.logistics.response.ApiResponse;
import com.logistics.response.ListResponse;
import com.logistics.response.Pagination;
import com.logistics.specification.EmployeeSpecification;
import com.logistics.specification.UserSpecification;
import com.logistics.utils.EmailService;
import com.logistics.utils.PasswordUtils;

import lombok.RequiredArgsConstructor;

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

    public Office getManagedOfficeByUserId(Integer userId) {
        List<Employee> employees = employeeRepository.findByUserId(userId);

        Employee managed = employees.stream()
                .filter(emp -> emp.getStatus() != EmployeeStatus.LEAVE)
                .filter(emp -> emp.getOffice() != null && emp.getOffice().getManager().getId().equals(emp.getId()))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Bạn không phải quản lý bưu cục hoặc đã nghỉ"));

        return managed.getOffice();
    }

    public ApiResponse<ListResponse<ManagerEmployeeListDto>> list(int userId, ManagerEmployeeSearchRequest request) {
        try {
            Office office = getManagedOfficeByUserId(userId);

            int page = request.getPage();
            int limit = request.getLimit();
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

            Sort sortOpt = switch (sort.toLowerCase()) {
                case "newest" -> Sort.by("hireDate").descending();
                case "oldest" -> Sort.by("hireDate").ascending();
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

            return new ApiResponse<>(true, "Lấy danh sách nhân viên thành công", data);
        } catch (Exception e) {
            return new ApiResponse<>(false, "Lỗi: " + e.getMessage(), null);
        }
    }

    @Transactional
    public ApiResponse<Boolean> createEmployee(int creatorUserId, ManagerEmployeeEditRequest req) {
        try {
            validateCreate(req);

            // 1. Lấy office của người dùng hiện tại
            Office office = getManagedOfficeByUserId(creatorUserId);

            String email = req.getUserEmail().trim().toLowerCase();
            String phone = req.getUserPhoneNumber().trim();
 
            // 2. tìm account theo email
            Optional<Account> optAccount = accountRepository.findByEmail(email);

            // CASE A: Account CHƯA TỒN TẠI -> tạo toàn bộ
            if (optAccount.isEmpty()) {
                // phone phải unique toàn hệ thống
                if (userRepository.existsByPhoneNumber(phone)) {
                    return new ApiResponse<>(false, "Số điện thoại này đã được sử dụng. Vui lòng nhập số khác", null);
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

                Role role = roleRepository.findByName(req.getUserRole())
                        .orElseThrow(
                                () -> new RuntimeException("Chức vụ bạn chọn không hợp lệ. Vui lòng kiểm tra lại."));

                AccountRole accountRole = new AccountRole();
                accountRole.setAccount(account);
                accountRole.setRole(role);
                accountRole.setIsActive(true);
                accountRole = accountRoleRepository.save(accountRole);

                Employee emp = buildEmployee(user, accountRole, office, req);
                employeeRepository.save(emp);

                return new ApiResponse<>(true, "Thêm nhân viên mới thành công", true);
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
                    .anyMatch(ar -> "User".equalsIgnoreCase(ar.getRole().getName()));
            long activeNonUserRolesCount = activeAccountRoles.stream()
                    .filter(ar -> !"User".equalsIgnoreCase(ar.getRole().getName()))
                    .count();

            // role muốn thêm
            Role wantedRole = roleRepository.findByName(req.getUserRole())
                    .orElseThrow(() -> new RuntimeException("Role không tồn tại: " + req.getUserRole()));

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
                            .anyMatch(e -> e.getOffice().getId().equals(office.getId())
                                    && e.getStatus() != EmployeeStatus.LEAVE);
                    if (existsActiveOrInactiveHere) {
                        return new ApiResponse<>(false, "Tài khoản này đã là nhân viên ở bưu cục hiện tại.", null);
                    }

                    // 2. Nếu có employee ACTIVE/INACTIVE ở office khác -> lỗi (một người chỉ làm 1
                    // bưu cục)
                    boolean existsActiveOrInactiveElsewhere = employees.stream()
                            .anyMatch(e -> !e.getOffice().getId().equals(office.getId())
                                    && e.getStatus() != EmployeeStatus.LEAVE);
                    if (existsActiveOrInactiveElsewhere) {
                        return new ApiResponse<>(false,
                                "ài khoản này đang làm việc tại bưu cục khác. Một nhân viên chỉ có thể thuộc 1 bưu cục.",
                                null);
                    }

                    // 3. Nếu tới đây => tất cả employee liên quan đều LEAVE (hoặc không có) -> tạo
                    // employee mới
                    Employee newEmp = buildEmployee(account.getUser(), ar, office, req);
                    employeeRepository.save(newEmp);
                    return new ApiResponse<>(true, "Nhân viên mới đã được tạo thành công!", true);
                }

                long totalActiveAfterRestore = activeAccountRoles.size() + 1; // kế hoạch restore ar -> +1
                // rule: nếu có User active -> max 2 (User + 1 non-user). Nếu không có User
                // active -> max 1
                if (hasActiveUserRole) {
                    if (totalActiveAfterRestore > 2) {
                        return new ApiResponse<>(false,
                                "Không thể thực hiện chức vụ này vì mỗi người chỉ được làm một công việc trong một thời gian, thuộc một bưu cục.",
                                null);
                    }
                } else {
                    if (totalActiveAfterRestore > 1) {
                        return new ApiResponse<>(false,
                                "Không thể kích hoạt chức vụ này vì tài khoản đã có chức vụ đang hoạt động.", null);
                    }
                }

                // ok restore accountRole
                ar.setIsActive(true);
                accountRoleRepository.save(ar);

                // xử lý employee liên quan:
                List<Employee> employees = employeeRepository.findAllByAccountRoleId(ar.getId());

                // nếu tồn tại employee cùng office -> khôi phục trạng thái
                Optional<Employee> empHereOpt = employees.stream()
                        .filter(e -> e.getOffice().getId().equals(office.getId()))
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
                    return new ApiResponse<>(true, "Nhân viên đã được khôi phục và sẵn sàng làm việc.", true);
                }

                // nếu không có employee cùng office:
                // nhưng cần check rule "1 account chỉ được làm 1 office" => nếu có employee
                // ACTIVE/INACTIVE ở nơi khác => lỗi
                boolean existsActiveOrInactiveElsewhere = employees.stream()
                        .anyMatch(e -> e.getStatus() != EmployeeStatus.LEAVE);
                if (existsActiveOrInactiveElsewhere) {
                    return new ApiResponse<>(false,
                            "ài khoản này đang làm việc tại bưu cục khác. Một nhân viên chỉ có thể thuộc 1 bưu cục.",
                            null);
                }

                // tạo employee mới
                Employee newEmp = buildEmployee(account.getUser(), ar, office, req);
                employeeRepository.save(newEmp);
                return new ApiResponse<>(true, "Nhân viên mới đã được tạo thành công sau khi khôi phục chức vụ.", true);
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
                    return new ApiResponse<>(false,
                            "Tài khoản đã có 1 chức vụ nhân viên đang active, không thể thêm chức vụ thứ 2", null);
                }
            } else {
                if (activeNonUserRolesCount >= 1) {
                    // account đã có 1 non-user active và không có user -> không thể thêm nữa
                    return new ApiResponse<>(false,
                            "Tài khoản này đã có một chức vụ đang hoạt động. Không thể thêm chức vụ thứ hai.", null);
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
                    .anyMatch(e -> !e.getOffice().getId().equals(office.getId())
                            && e.getStatus() != EmployeeStatus.LEAVE);

            if (existsActiveOrInactiveElsewhere) {
                // Chặn nếu ACTIVE/INACTIVE ở office khác
                return new ApiResponse<>(false, "Tài khoản này hiện đang là nhân viên tại bưu cục khác", null);
            }

            // Nếu không có employee active/inactive ở office khác -> tạo employee mới bình
            // thường
            Employee emp = buildEmployee(account.getUser(), newAr, office, req);
            employeeRepository.save(emp);

            return new ApiResponse<>(true, "Chức vụ và nhân viên mới đã được tạo thành công!", true);

        } catch (Exception ex) {
            return new ApiResponse<>(false, ex.getMessage(), null);
        }
    }

    @Transactional
    public ApiResponse<Boolean> updateEmployee(int editorUserId, int employeeId, ManagerEmployeeEditRequest req) {
        try {
            validateEdit(req);

            Office editorOffice = getManagedOfficeByUserId(editorUserId);

            Employee emp = employeeRepository.findById(employeeId)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy nhân viên này"));

            if (!emp.getOffice().getId().equals(editorOffice.getId())) {
                return new ApiResponse<>(false, "Bạn chỉ có thể chỉnh sửa nhân viên trong bưu cục của mình.", null);
            }

            Account account = emp.getAccountRole().getAccount();
            EmployeeStatus currentStatus = emp.getStatus();

            // ===== Kiểm tra employee ACTIVE/INACTIVE khác ở office khác =====
            boolean hasOtherOfficeActive = employeeRepository.findAllByAccountId(account.getId())
                    .stream()
                    .filter(e -> e.getStatus() == EmployeeStatus.ACTIVE || e.getStatus() == EmployeeStatus.INACTIVE)
                    .anyMatch(e -> !e.getOffice().getId().equals(emp.getOffice().getId()));

            if (hasOtherOfficeActive) {
                return new ApiResponse<>(false,
                        "Nhân viên này đang làm việc tại bưu cục khác. Không thể thay đổi chức vụ hoặc trạng thái.",
                        null);
            }

            // ===== Xử lý role nếu thay đổi =====
            Role newRole = roleRepository.findByName(req.getUserRole())
                    .orElseThrow(() -> new RuntimeException("Chức vụ bạn chọn không hợp lệ. Vui lòng kiểm tra lại."));
            AccountRole currentAR = emp.getAccountRole();

            EmployeeStatus newStatus = req.getStatus() != null && !req.getStatus().isBlank()
                    ? EmployeeStatus.valueOf(req.getStatus())
                    : currentStatus;

            if (!currentAR.getRole().getId().equals(newRole.getId()) && newStatus == EmployeeStatus.LEAVE) {
                return new ApiResponse<>(false,
                        "Không thể thay đổi chức vụ và cho nghỉ cùng lúc. Vui lòng thực hiện riêng từng thao tác.",
                        null);
            }

            if (!currentAR.getRole().getId().equals(newRole.getId())) {
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
                            .anyMatch(r -> "User".equalsIgnoreCase(r.getRole().getName()));
                    long activeNonUserCount = activeRoles.stream()
                            .filter(r -> !"User".equalsIgnoreCase(r.getRole().getName())).count();
                    long totalActiveAfterAdd = activeRoles.size() + 1;

                    if (hasUserActive && totalActiveAfterAdd > 2 || !hasUserActive && totalActiveAfterAdd > 1) {
                        return new ApiResponse<>(false,
                                "Không thể thêm chức vụ mới vì đã đạt giới hạn chức vụ đang hoạt động.", null);
                    }

                    newAR = new AccountRole();
                    newAR.setAccount(account);
                    newAR.setRole(newRole);
                    newAR.setIsActive(true);
                    accountRoleRepository.save(newAR);
                }

                // deactivate employee + accountRole cũ
                currentAR.setIsActive(false);
                accountRoleRepository.save(currentAR);

                emp.setStatus(EmployeeStatus.LEAVE);
                employeeRepository.save(emp);

                // tạo employee mới với role mới
                Employee newEmp = buildEmployee(account.getUser(), newAR, emp.getOffice(), req);
                employeeRepository.save(newEmp);

                return new ApiResponse<>(true,
                        "Chức vụ đã được thay đổi. Nhân viên cũ đã kết thúc công việc, nhân viên mới đã được tạo.",
                        true);
            }

            // ===== Cập nhật hireDate, shift (chỉ khi nhân viên chưa nghỉ) =====
            if (newStatus != EmployeeStatus.LEAVE) {
                if (req.getHireDate() != null) {
                    emp.setHireDate(Instant.parse(req.getHireDate()).atZone(ZoneId.systemDefault()).toLocalDateTime());
                }

                if (req.getShift() != null && !req.getShift().isBlank()) {
                    emp.setShift(EmployeeShift.valueOf(req.getShift()));
                }
            }

            // ===== Cập nhật status =====
            if (req.getStatus() != null && !req.getStatus().isBlank()) {

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
                    List<AccountRole> roles = accountRoleRepository.findByAccountIdAndIsActiveTrue(account.getId());
                    roles.forEach(r -> r.setIsActive(false));
                    accountRoleRepository.saveAll(roles);

                    boolean hasActiveNonUserRole = accountRoleRepository.findByAccountIdAndIsActiveTrue(account.getId())
                            .stream()
                            .anyMatch(r -> !"User".equalsIgnoreCase(r.getRole().getName()));
                    boolean hasUserRole = accountRoleRepository.findByAccountIdAndIsActiveTrue(account.getId())
                            .stream()
                            .anyMatch(r -> "User".equalsIgnoreCase(r.getRole().getName()));

                    // nếu không còn role active nào → tắt account
                    if (!hasActiveNonUserRole && !hasUserRole) {
                        account.setIsActive(false);
                        accountRepository.save(account);
                    }
                }
                // LEAVE → ACTIVE/INACTIVE
                else if (currentStatus == EmployeeStatus.LEAVE
                        && (newStatus == EmployeeStatus.ACTIVE || newStatus == EmployeeStatus.INACTIVE)) {

                    AccountRole ar = emp.getAccountRole();

                    // tìm employee LEAVE cùng office + accountRole
                    List<Employee> leaveEmployees = employeeRepository.findAllByAccountRoleId(ar.getId())
                            .stream()
                            .filter(e -> e.getOffice().getId().equals(emp.getOffice().getId())
                                    && e.getStatus() == EmployeeStatus.LEAVE)
                            .toList();

                    if (!leaveEmployees.isEmpty()) {
                        // restore employee cũ
                        Employee toRestore = leaveEmployees.get(0);
                        toRestore.setStatus(newStatus);
                        toRestore.setHireDate(req.getHireDate() != null
                                ? Instant.parse(req.getHireDate()).atZone(ZoneId.systemDefault()).toLocalDateTime()
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
                                    .anyMatch(r -> "User".equalsIgnoreCase(r.getRole().getName()));
                            long activeNonUserCount = activeRoles.stream()
                                    .filter(r -> !"User".equalsIgnoreCase(r.getRole().getName())).count();

                            if (hasUserActive && activeNonUserCount >= 1 || !hasUserActive && activeNonUserCount >= 1) {
                                return new ApiResponse<>(false,
                                        "Không thể kích hoạt nhân viên mới vì số lượng chức vụ đang hoạt động đã đạt giới hạn",
                                        null);
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

            return new ApiResponse<>(true, "Thông tin nhân viên đã được cập nhật thành công!", true);

        } catch (Exception ex) {
            return new ApiResponse<>(false,
                    "Đã xảy ra lỗi: " + ex.getMessage() + ". Vui lòng thử lại hoặc liên hệ quản trị viên.", null);
        }
    }

    private Employee buildEmployee(User user, AccountRole accountRole, Office office, ManagerEmployeeEditRequest req) {
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
        if (request.getUserFirstName() == null || String.valueOf(request.getUserFirstName()).isBlank())
            missingFields.add("Tên");
        if (request.getUserLastName() == null || String.valueOf(request.getUserLastName()).isBlank())
            missingFields.add("Họ");
        if (request.getUserPhoneNumber() == null || request.getUserPhoneNumber().isBlank())
            missingFields.add("Số điện thoại");
        if (request.getUserEmail() == null || request.getUserEmail().isBlank())
            missingFields.add("Email");
        if (request.getUserRole() == null || request.getUserRole().isBlank())
            missingFields.add("Chức vụ");

        if (!missingFields.isEmpty())
            throw new RuntimeException("Thiếu thông tin: " + String.join(", ", missingFields));

        if (request.getShift() != null && !request.getShift().isBlank()) {
            try {
                EmployeeShift.valueOf(request.getShift());
            } catch (IllegalArgumentException e) {
                throw new RuntimeException("Ca làm việc không hợp lệ: " + request.getShift());
            }
        }
        if (request.getStatus() != null && !request.getStatus().isBlank()) {
            try {
                EmployeeStatus.valueOf(request.getStatus());
            } catch (IllegalArgumentException e) {
                throw new RuntimeException("Trạng thái không hợp lệ: " + request.getStatus());
            }
        }
    }

    private void validateEdit(ManagerEmployeeEditRequest request) {
        List<String> missingFields = new ArrayList<>();
        if (request.getUserRole() == null || request.getUserRole().isBlank())
            missingFields.add("Chức vụ");

        if (!missingFields.isEmpty())
            throw new RuntimeException("Thiếu thông tin: " + String.join(", ", missingFields));

        if (request.getShift() != null && !request.getShift().isBlank()) {
            try {
                EmployeeShift.valueOf(request.getShift());
            } catch (IllegalArgumentException e) {
                throw new RuntimeException("Ca làm việc không hợp lệ: " + request.getShift());
            }
        }
        if (request.getStatus() != null && !request.getStatus().isBlank()) {
            try {
                EmployeeStatus.valueOf(request.getStatus());
            } catch (IllegalArgumentException e) {
                throw new RuntimeException("Trạng thái không hợp lệ: " + request.getStatus());
            }
        }
    }

    public ApiResponse<ListResponse<ManagerEmployeeListWithShipperAssignmentDto>> getActiveShippersWithActiveAssignments(
            int userId,
            ManagerEmployeeSearchRequest request) {
        try {
            Office office = getManagedOfficeByUserId(userId);

            int page = request.getPage();
            int limit = request.getLimit();
            String search = request.getSearch();
            LocalDateTime now = LocalDateTime.now();

            // Specification để lấy user đang active + role SHIPPER + thuộc office + search
            Specification<User> spec = Specification
                    .where(UserSpecification.activeShippersInOffice(office.getId(), search));

            Pageable pageable = PageRequest.of(page - 1, limit, Sort.by("lastName").ascending());
            Page<User> pageData = userRepository.findAll(spec, pageable);

            // Map sang DTO
            List<ManagerEmployeeListWithShipperAssignmentDto> list = pageData.getContent().stream()
                    .map(user -> {
                        // Lấy employee code
                        Employee employee = user.getEmployees().stream()
                                .filter(emp -> emp.getAccountRole() != null &&
                                        emp.getAccountRole().getRole() != null &&
                                        "Shipper".equalsIgnoreCase(emp.getAccountRole().getRole().getName()))
                                .findFirst()
                                .orElse(null);

                        // Lấy assignment còn hiệu lực
                        List<ShipperAssignment> activeAssignments = user.getShipperAssignments().stream()
                                .filter(sa -> sa.getEndAt() == null || !sa.getEndAt().isBefore(now))
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

            return new ApiResponse<>(true, "Lấy danh sách Shipper đang làm việc thành công", data);

        } catch (Exception e) {
            return new ApiResponse<>(false, "Lỗi: " + e.getMessage(), null);
        }
    }

    public ApiResponse<ListResponse<ManagerEmployeeListDto>> getActiveShippers(int userId,
            ManagerEmployeeSearchRequest request) {
        try {
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

            return new ApiResponse<>(true, "Lấy danh sách nhân viên thành công", data);
        } catch (Exception e) {
            return new ApiResponse<>(false, "Lỗi: " + e.getMessage(), null);
        }
    }

}