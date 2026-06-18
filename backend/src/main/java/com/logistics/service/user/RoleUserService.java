package com.logistics.service.user;

import com.logistics.dto.user.role.RoleDetailUserDto;
import com.logistics.dto.user.role.RoleListUserDto;
import com.logistics.entity.PermissionGroup;
import com.logistics.entity.Role;
import com.logistics.entity.User;
import com.logistics.exception.AppException;
import com.logistics.exception.enums.RoleErrorCode;
import com.logistics.mapper.RoleMapper;
import com.logistics.repository.PermissionGroupRepository;
import com.logistics.repository.RoleRepository;
import com.logistics.request.user.role.RoleSearchUserRequest;
import com.logistics.request.user.role.RoleUserRequest;
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

    public ListResponse<RoleListUserDto> list(int userId, RoleSearchUserRequest request) {
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

            return data;
    }

    public List<RoleListUserDto> findAll(int userId) {
            Integer shopId = userService.getShopId(userId);

            Specification<Role> spec = RoleSpecification.unrestrictedRole()
                    .and(RoleSpecification.currentShop(shopId));

            Sort sort = Sort.by("createdAt").descending();
            List<Role> roles = repository.findAll(spec, sort);

            return roles.stream()
                    .map(RoleMapper::toRoleListUserDto)
                    .toList();
    }

    public RoleDetailUserDto detail(int userId, int roleId) {
            Integer shopId = userService.getShopId(userId);

            User user = userService.getUser(shopId);

            Role role = getRole(roleId);
            checkOwnerPermission(user, role);

            return RoleMapper.toRoleDetailUserDto(role);
    }

    public void create(int userId, RoleUserRequest request) {
            Integer shopId = userService.getShopId(userId);
            User owner = userService.getUser(shopId);

            if (repository.existsByNameAndUserOwnerId(request.name(), shopId)) {
                throw new AppException(RoleErrorCode.ROLE_NAME_EXISTS);
            }

            List<PermissionGroup> groups = validateAndGetGroups(request.permissionGroupIds());

            Role role = new Role();
            role.setName(request.name());
            role.setDescription(request.description());
            role.setUserOwner(owner);
            role.setPermissionGroups(new HashSet<>(groups));
            repository.save(role);
    }

    public void update(int userId, int roleId, RoleUserRequest request) {
            Integer shopId = userService.getShopId(userId);
            User user = userService.getUser(shopId);
            Role role = getRole(roleId);
            checkOwnerPermission(user, role);

            Integer ownerId = role.getUserOwner().getId();

            if (repository.existsByNameAndUserOwnerIdAndIdNot(request.name(), ownerId, roleId)) {
                throw new AppException(RoleErrorCode.ROLE_NAME_EXISTS);
            }

            List<PermissionGroup> groups = validateAndGetGroups(request.permissionGroupIds());
            role.setName(request.name());
            role.setDescription(request.description());
            role.setPermissionGroups(new HashSet<>(groups));
            repository.save(role);
    }

    public void delete(int userId, int roleId) {
            Integer shopId = userService.getShopId(userId);
            User user = userService.getUser(shopId);
            Role role = getRole(roleId);
            checkOwnerPermission(user, role);

            if (!role.getAccountRoles()
                    .isEmpty()) {
                throw new AppException(RoleErrorCode.ROLE_IN_USE);
            }
            repository.delete(role);
    }

    private List<PermissionGroup> validateAndGetGroups(List<Integer> ids) {
        List<PermissionGroup> groups = permissionGroupRepository.findAllByIdsWithParent(ids);

        if (groups.size() != ids.size()) {
            throw new AppException(RoleErrorCode.ROLE_INVALID_PERMISSION_GROUP);
        }

        Set<Integer> selectedIds = groups.stream()
                .map(PermissionGroup::getId)
                .collect(Collectors.toSet());

        for (PermissionGroup group : groups) {
            if (group.getParent() != null && !selectedIds.contains(group.getParent().getId())) {
                throw new AppException(
                        RoleErrorCode.ROLE_INVALID_HIERARCHY_GROUP,
                        group.getName(),
                        group.getParent().getName());
            }
        }

        return groups;
    }

    public void checkOwnerPermission(User user, Role role) {

        if (role.getUserOwner() == null || !role.getUserOwner()
                .getId()
                .equals(user.getId())) {
            throw new AppException(RoleErrorCode.ROLE_INVALID_HIERARCHY_GROUP);
        }
    }

    public Role getRole(int roleId) {
        return repository.findByIdWithPermissionGroups(roleId)
                .orElseThrow(() -> new AppException(RoleErrorCode.ROLE_NOT_FOUND));
    }
}