package com.logistics.service.admin;

import com.logistics.dto.BaseAuditLogDto;
import com.logistics.entity.*;
import com.logistics.enums.AuditLogAction;
import com.logistics.enums.AuditLogStatus;
import com.logistics.enums.EntityType;
import com.logistics.exception.AppException;
import com.logistics.exception.enums.AccountErrorCode;
import com.logistics.exception.enums.CommonErrorCode;
import com.logistics.exception.enums.UserErrorCode;
import com.logistics.mapper.AuditLogMapper;
import com.logistics.repository.AccountRepository;
import com.logistics.repository.AuditLogRepository;
import com.logistics.repository.RoleRepository;
import com.logistics.repository.UserRepository;
import com.logistics.request.admin.CreateUserRequest;
import com.logistics.request.admin.UpdateUserRequest;
import com.logistics.request.manager.audit.AuditLogSearchRequest;
import com.logistics.response.ListResponse;
import com.logistics.response.Pagination;
import com.logistics.specification.AuditLogSpecification;
import com.logistics.utils.PasswordUtils;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.logistics.utils.AuditLogUtils.translateAuditLogAction;
import static com.logistics.utils.AuditLogUtils.translateAuditLogStatus;
import static com.logistics.utils.EntityTypeUtils.translateEntityType;

@Service
public class UserAdminService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private AuditLogRepository auditLogRepository;

    public Map<String, Object> listUsers(int page, int limit, String search, String status, String roleName) {
        Pageable pageable = PageRequest.of(page - 1, limit, Sort.by("createdAt").descending());
        Page<Account> accountPage;

        Boolean statusBool = null;
        if (status != null) {
            if ("ACTIVE".equalsIgnoreCase(status)) statusBool = true;
            else if ("INACTIVE".equalsIgnoreCase(status)) statusBool = false;
        }

        if ((search != null && !search.trim().isEmpty()) || roleName != null || statusBool != null) {
            String searchParam = (search != null && !search.trim().isEmpty()) ? search : null;
            accountPage = accountRepository.findBySearchAndRoleAndStatus(searchParam, roleName, statusBool, pageable);
        } else {
            accountPage = accountRepository.findAll(pageable);
        }

            List<Map<String, Object>> users = accountPage.getContent().stream()
                .map(acc -> mapAccount(acc, roleName))
                .collect(Collectors.toList());

        Pagination pagination = new Pagination(
                (int) accountPage.getTotalElements(),
                page,
                limit,
                accountPage.getTotalPages());

        Map<String, Object> result = new HashMap<>();
        result.put("data", users);
        result.put("pagination", pagination);

        return result;
    }

    public Map<String, Object> getUserById(Integer userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(UserErrorCode.USER_NOT_FOUND));

        Account account = accountRepository.findByUser(user)
                .orElseThrow(() -> new AppException(UserErrorCode.USER_ACCOUNT_NOT_FOUND));

        return mapAccount(account);
    }

    @Transactional
    public void createUser(CreateUserRequest request) {
        if (accountRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new AppException(AccountErrorCode.ACCOUNT_EMAIL_ALREADY_IN_USE);
        }

        if (userRepository.existsByPhoneNumber(request.getPhoneNumber())) {
            throw new AppException(UserErrorCode.USER_PHONE_EXISTED);
        }

            Account account = new Account();
            account.setEmail(request.getEmail());
            account.setPassword(PasswordUtils.hashPassword(request.getPassword()));

            List<AccountRole> ars = new ArrayList<>();
            if (request.getRoleIds() != null && !request.getRoleIds().isEmpty()) {
                for (Integer rid : request.getRoleIds()) {
                    Role role = roleRepository.findById(rid)
                            .orElseThrow(() -> new AppException(UserErrorCode.USER_ROLE_NOT_FOUND, rid));
                    AccountRole ar = new AccountRole();
                    ar.setAccount(account);
                    ar.setRole(role);
                    ar.setIsActive(true);
                    ars.add(ar);
                }
            }
            account.setAccountRoles(ars);

            account.setIsActive(request.getIsActive() != null ? request.getIsActive() : true);
            account.setIsVerified(false);
            account = accountRepository.save(account);

        User user = new User();
        user.setAccount(account);
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setPhoneNumber(request.getPhoneNumber());
        user = userRepository.save(user);
    }

    @Transactional
    public void updateUser(Integer userId, UpdateUserRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(UserErrorCode.USER_NOT_FOUND));

        Account account = accountRepository.findByUser(user)
                .orElseThrow(() -> new AppException(UserErrorCode.USER_ACCOUNT_NOT_FOUND));

        if (request.getFirstName() != null)
            user.setFirstName(request.getFirstName());
        if (request.getLastName() != null)
            user.setLastName(request.getLastName());
        if (request.getPhoneNumber() != null) {
            if (!user.getPhoneNumber().equals(request.getPhoneNumber())
                    && userRepository.existsByPhoneNumber(request.getPhoneNumber())) {
                throw new AppException(UserErrorCode.USER_PHONE_EXISTED);
            }
            user.setPhoneNumber(request.getPhoneNumber());
        }
        userRepository.save(user);

        if (request.getPassword() != null && !request.getPassword().trim().isEmpty()) {
            account.setPassword(PasswordUtils.hashPassword(request.getPassword()));
        }
        if (request.getRoleIds() != null) {
            List<Integer> target = request.getRoleIds();
            List<AccountRole> current = account.getAccountRoles();
            if (current == null) {
                current = new ArrayList<>();
                account.setAccountRoles(current);
            }

            for (AccountRole ar : current) {
                if (ar != null && ar.getRole() != null && ar.getRole().getId() != null) {
                    if (target.contains(ar.getRole().getId())) {
                        ar.setIsActive(true);
                    } else {
                        ar.setIsActive(false);
                    }
                }
            }

            for (Integer rid : target) {
                boolean exists = false;
                for (AccountRole ar : current) {
                    if (ar != null && ar.getRole() != null && ar.getRole().getId() != null
                            && ar.getRole().getId().equals(rid)) {
                        exists = true;
                        break;
                    }
                }
                if (!exists) {
                    Role role = roleRepository.findById(rid)
                            .orElseThrow(() -> new AppException(UserErrorCode.USER_ROLE_NOT_FOUND, rid));
                    AccountRole newAr = new AccountRole();
                    newAr.setAccount(account);
                    newAr.setRole(role);
                    newAr.setIsActive(true);
                    current.add(newAr);
                }
            }
        }
        if (request.getIsActive() != null) {
            account.setIsActive(request.getIsActive());
        }
        accountRepository.save(account);
    }

    @Transactional
    public void deleteUser(Integer userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(UserErrorCode.USER_NOT_FOUND));

        Account account = accountRepository.findByUser(user)
                .orElseThrow(() -> new AppException(UserErrorCode.USER_ACCOUNT_NOT_FOUND));

        userRepository.delete(user);
        accountRepository.delete(account);
    }

    private Map<String, Object> mapAccount(Account account) {
        return mapAccount(account, null);
    }

    private Map<String, Object> mapAccount(Account account, String preferredRoleName) {
        Map<String, Object> userMap = new HashMap<>();
        userMap.put("id", account.getUser() != null ? account.getUser().getId() : null);
        userMap.put("email", account.getEmail());
        if (account.getUser() != null) {
            userMap.put("firstName", account.getUser().getFirstName());
            userMap.put("lastName", account.getUser().getLastName());
            userMap.put("phoneNumber", account.getUser().getPhoneNumber());
            userMap.put("images", account.getUser().getImages());
            userMap.put("createdAt", account.getUser().getCreatedAt());
            userMap.put("updatedAt", account.getUser().getUpdatedAt());
        }

        Role chosenRole = null;
        if (account.getAccountRoles() != null) {
            if (preferredRoleName != null) {
                for (AccountRole ar : account.getAccountRoles()) {
                    if (ar != null && ar.getRole() != null && preferredRoleName.equals(ar.getRole().getName())) {
                        chosenRole = ar.getRole();
                        break;
                    }
                }
            }

            if (chosenRole == null) {
                for (AccountRole ar : account.getAccountRoles()) {
                    if (ar != null && Boolean.TRUE.equals(ar.getIsActive()) && ar.getRole() != null) {
                        chosenRole = ar.getRole();
                        break;
                    }
                }
            }
        }
        userMap.put("role", chosenRole != null ? chosenRole.getName() : null);
        userMap.put("roleId", chosenRole != null ? chosenRole.getId() : null);

        List<String> rolesList = new ArrayList<>();
        List<Integer> rolesIdList = new ArrayList<>();
        if (account.getAccountRoles() != null) {
            for (AccountRole ar : account.getAccountRoles()) {
                if (ar != null && ar.getRole() != null && ar.getRole().getName() != null) {
                    rolesList.add(ar.getRole().getName());
                    if (ar.getRole().getId() != null) rolesIdList.add(ar.getRole().getId());
                }
            }
        }
        userMap.put("roles", rolesList);
        userMap.put("rolesIds", rolesIdList);
        userMap.put("isActive", account.getIsActive());
        userMap.put("isVerified", account.getIsVerified());
        return userMap;
    }

    public ListResponse<BaseAuditLogDto> listAuditLogsByUserId(
            Integer userId,
            AuditLogSearchRequest request) {
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
                .and(AuditLogSpecification.user(userId))
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

    public byte[] export(Integer userId, AuditLogSearchRequest request) {

        LocalDateTime startDate = request.getStartDate() != null && !request.getStartDate().isBlank()
                ? LocalDateTime.parse(request.getStartDate()) : null;
        LocalDateTime endDate = request.getEndDate() != null && !request.getEndDate().isBlank()
                ? LocalDateTime.parse(request.getEndDate()) : null;

        Specification<AuditLog> spec = AuditLogSpecification.unrestricted()
                .and(AuditLogSpecification.user(userId))
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
}
