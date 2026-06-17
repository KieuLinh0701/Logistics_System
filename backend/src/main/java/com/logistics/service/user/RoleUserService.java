package com.logistics.service.user;

import com.logistics.dto.user.role.RoleDetailUserDto;
import com.logistics.dto.user.role.RoleListUserDto;
import com.logistics.entity.PermissionGroup;
import com.logistics.entity.Role;
import com.logistics.entity.User;
import com.logistics.mapper.RoleMapper;
import com.logistics.repository.PermissionGroupRepository;
import com.logistics.repository.RoleRepository;
import com.logistics.repository.UserRepository;
import com.logistics.request.user.role.RoleSearchUserRequest;
import com.logistics.request.user.role.RoleUserRequest;
import com.logistics.response.ApiResponse;
import com.logistics.response.ListResponse;
import com.logistics.response.Pagination;
import com.logistics.specification.RoleSpecification;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RoleUserService {

    private final RoleRepository repository;
    private final PermissionGroupRepository permissionGroupRepository;
    private final UserUserService userService;

    public ApiResponse<ListResponse<RoleListUserDto>> list(int userId, RoleSearchUserRequest request) {
        try {
            Integer shopId = userService.getShopId(userId);

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

            Specification<Role> spec = RoleSpecification.unrestrictedRole()
                    .and(RoleSpecification.currentShop(shopId))
                    .and(RoleSpecification.search(search))
                    .and(RoleSpecification.createdAtBetween(startDate, endDate));

            String sortBy = "createdAt";
            Sort sort = Sort.by(sortBy)
                    .descending();

            Pageable pageable = PageRequest.of(page - 1, limit, sort);
            Page<Role> pageData = repository.findAll(spec, pageable);

            List<RoleListUserDto> list = pageData.getContent()
                    .stream()
                    .map(RoleMapper::toRoleListUserDto)
                    .toList();

            int total = (int) pageData.getTotalElements();

            Pagination pagination = new Pagination(total, page, limit, pageData.getTotalPages());

            ListResponse<RoleListUserDto> data = new ListResponse<>();
            data.setList(list);
            data.setPagination(pagination);

            return new ApiResponse<>(true, "Lấy danh quyền của cửa hàng thaành công", data);
        } catch (Exception e) {
            return new ApiResponse<>(false, "Lỗi: " + e.getMessage(), null);
        }
    }

    public ApiResponse<List<RoleListUserDto>> findAll(int userId) {
        try {
            Integer shopId = userService.getShopId(userId);

            Specification<Role> spec = RoleSpecification.unrestrictedRole()
                    .and(RoleSpecification.currentShop(shopId));

            Sort sort = Sort.by("createdAt").descending();
            List<Role> roles = repository.findAll(spec, sort);

            List<RoleListUserDto> list = roles.stream()
                    .map(RoleMapper::toRoleListUserDto)
                    .toList();

            return new ApiResponse<>(true, "Lấy danh sách nhóm quyền thành công", list);
        } catch (Exception e) {
            return new ApiResponse<>(false, "Lỗi: " + e.getMessage(), null);
        }
    }

    public ApiResponse<RoleDetailUserDto> detail(int userId, int roleId) {
        try {
            Integer shopId = userService.getShopId(userId);

            User user = userService.getUser(shopId);

            Role role = getRole(roleId);
            checkOwnerPermission(user, role);

            RoleDetailUserDto data = RoleMapper.toRoleDetailUserDto(role);
            return new ApiResponse<>(
                    true,
                    "Lấy thông tin nhóm quyền thành công",
                    data);
        } catch (Exception e) {
            return new ApiResponse<>(false, e.getMessage(), null);
        }
    }

    public ApiResponse<Void> create(int userId, RoleUserRequest request) {
        return executeRoleAction(() -> {
            validateRoleRequest(request);

            Integer shopId = userService.getShopId(userId);
            User owner = userService.getUser(shopId);

            if (repository.existsByNameAndUserOwnerId(request.getName(), shopId)) {
                throw new RuntimeException("Tên nhóm quyền đã tồn tại");
            }

            List<PermissionGroup> groups = validateAndGetGroups(request.getPermissionGroupIds());

            Role role = new Role();
            role.setName(request.getName());
            role.setDescription(request.getDescription());
            role.setUserOwner(owner);
            role.setPermissionGroups(new HashSet<>(groups));
            repository.save(role);
        }, "Thêm nhóm quyền thành công");
    }

    public ApiResponse<Void> update(int userId, int roleId, RoleUserRequest request) {
        return executeRoleAction(() -> {
            validateRoleRequest(request);

            Integer shopId = userService.getShopId(userId);
            User user = userService.getUser(shopId);
            Role role = getRole(roleId);
            checkOwnerPermission(user, role);

            Integer ownerId = role.getUserOwner().getId();

            if (repository.existsByNameAndUserOwnerIdAndIdNot(request.getName(), ownerId, roleId)) {
                throw new RuntimeException("Tên nhóm quyền đã tồn tại");
            }

            List<PermissionGroup> groups = validateAndGetGroups(request.getPermissionGroupIds());
            role.setName(request.getName());
            role.setDescription(request.getDescription());
            role.setPermissionGroups(new HashSet<>(groups));
            repository.save(role);
        }, "Cập nhật nhóm quyền thành công");
    }

    public ApiResponse<Void> delete(int userId, int roleId) {
        return executeRoleAction(() -> {
            Integer shopId = userService.getShopId(userId);
            User user = userService.getUser(shopId);
            Role role = getRole(roleId);
            checkOwnerPermission(user, role);

            if (!role.getAccountRoles()
                    .isEmpty()) {
                throw new RuntimeException("Không thể xóa nhóm quyền đang được sử dụng");
            }
            repository.delete(role);
        }, "Xóa nhóm quyền thành công");
    }

    private List<PermissionGroup> validateAndGetGroups(List<Integer> ids) {
        List<PermissionGroup> groups = permissionGroupRepository.findAllByIdsWithParent(ids);

        if (groups.size() != ids.size()) {
            throw new RuntimeException("Một số nhóm quyền không tồn tại hoặc không hợp lệ");
        }

        Set<Integer> selectedIds = groups.stream()
                .map(PermissionGroup::getId)
                .collect(Collectors.toSet());

        for (PermissionGroup group : groups) {
            if (group.getParent() != null && !selectedIds.contains(group.getParent().getId())) {
                throw new RuntimeException(
                        "Nhóm quyền \"" + group.getName() + "\" yêu cầu phải chọn cha \""
                                + group.getParent().getName() + "\" trước");
            }
        }

        return groups;
    }

    public void checkOwnerPermission(User user, Role role) {

        if (role.getUserOwner() == null || !role.getUserOwner()
                .getId()
                .equals(user.getId())) {
            throw new RuntimeException("Bạn không có quyền thao tác trên nhóm quyền này");
        }
    }

    private ApiResponse<Void> executeRoleAction(Runnable action, String successMsg) {
        try {
            action.run();
            return new ApiResponse<>(true, successMsg, null);
        } catch (Exception e) {
            return new ApiResponse<>(false, e.getMessage(), null);
        }
    }

    public Role getRole(int roleId) {
        return repository.findByIdWithPermissionGroups(roleId)
                .orElseThrow(() -> new RuntimeException("Nhóm quyền không tồn tại"));
    }

    private void validateRoleRequest(RoleUserRequest request) {
        if (request.getName() == null || request.getName()
                .isBlank()) {
            throw new RuntimeException("Tên nhóm quyền không được để trống");
        }

        if (request.getDescription() == null || request.getDescription()
                .isBlank()) {
            throw new RuntimeException("Mô tả nhóm quyền không được để trống");
        }

        if (request.getPermissionGroupIds() == null || request.getPermissionGroupIds()
                .isEmpty()) {
            throw new RuntimeException("Phải chọn ít nhất một nhóm quyền");
        }
    }
}