package com.logistics.service.user;

import com.logistics.dto.user.role.PermissionModuleDto;
import com.logistics.dto.user.role.RoleListUserDto;
import com.logistics.entity.PermissionModule;
import com.logistics.entity.Role;
import com.logistics.entity.User;
import com.logistics.mapper.PermissionModuleMapper;
import com.logistics.mapper.RoleMapper;
import com.logistics.repository.PermissionModuleRepository;
import com.logistics.repository.RoleRepository;
import com.logistics.repository.UserRepository;
import com.logistics.request.user.role.RoleSearchUserRequest;
import com.logistics.response.ApiResponse;
import com.logistics.response.ListResponse;
import com.logistics.response.Pagination;
import com.logistics.specification.RoleSpecification;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PermissionModuleUserService {

    @Autowired
    private PermissionModuleRepository repository;

    @Transactional(readOnly = true)
    public ApiResponse<List<PermissionModuleDto>> activeList() {
        try {
            List<PermissionModule> list = repository.findAllActiveWithGroupsOrdered();

            List<PermissionModuleDto> data = PermissionModuleMapper.toPermissionModuleListDto(list);

            return new ApiResponse<>(true, "Lấy danh quyền của cửa hàng thaành công", data);
        } catch (Exception e) {
            return new ApiResponse<>(false, "Lỗi: " + e.getMessage(), null);
        }
    }
}