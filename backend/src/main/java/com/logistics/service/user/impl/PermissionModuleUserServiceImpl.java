package com.logistics.service.user.impl;

import com.logistics.dto.user.role.PermissionModuleDto;
import com.logistics.entity.PermissionGroup;
import com.logistics.entity.PermissionModule;
import com.logistics.mapper.PermissionModuleMapper;
import com.logistics.repository.PermissionModuleRepository;
import com.logistics.service.user.PermissionModuleUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PermissionModuleUserServiceImpl implements PermissionModuleUserService {

    private final PermissionModuleRepository repository;

    @Override
    @Transactional(readOnly = true)
    public List<PermissionModuleDto> activeList() {
        List<PermissionModule> list = repository.findAllActiveWithGroupsOrdered();

        list.forEach(pm -> {
            Set<PermissionGroup> filteredGroups = pm.getPermissionGroups().stream()
                    .filter(pg -> pg.getIsActive()
                            && !pg.getIsSystemOnly()
                            && pg.getParent() == null)
                    .collect(Collectors.toCollection(LinkedHashSet::new));

            filteredGroups.forEach(pg -> {
                Set<PermissionGroup> filteredSubs = pg.getSubPermissions().stream()
                        .filter(sub -> !sub.getIsSystemOnly())
                        .collect(Collectors.toCollection(LinkedHashSet::new));
                pg.setSubPermissions(filteredSubs);
            });

            pm.setPermissionGroups(filteredGroups);
        });

        return PermissionModuleMapper.toPermissionModuleListDto(list);
    }
}