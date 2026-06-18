package com.logistics.service.user;

import com.logistics.dto.user.role.PermissionModuleDto;
import com.logistics.entity.PermissionModule;
import com.logistics.mapper.PermissionModuleMapper;
import com.logistics.repository.PermissionModuleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PermissionModuleUserService {

    @Autowired
    private PermissionModuleRepository repository;

    @Transactional(readOnly = true)
    public List<PermissionModuleDto> activeList() {
            List<PermissionModule> list = repository.findAllActiveWithGroupsOrdered();

            return PermissionModuleMapper.toPermissionModuleListDto(list);
    }
}