package com.logistics.repository;

import com.logistics.entity.PermissionModule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PermissionModuleRepository
        extends JpaRepository<PermissionModule, Integer> {

    @Query("SELECT DISTINCT pm FROM PermissionModule pm " +
            "LEFT JOIN FETCH pm.permissionGroups pg " +
            "LEFT JOIN FETCH pg.subPermissions sub " +
            "WHERE pm.isActive = true " +
            "AND pg.isActive = true " +
            "AND pm.isSystemOnly = false " +
            "AND pg.isSystemOnly = false " +
            "AND pg.parent IS NULL " +
            "AND (sub IS NULL OR sub.isSystemOnly = false) " +
            "ORDER BY pm.sortOrder ASC, pg.sortOrder ASC")
    List<PermissionModule> findAllActiveWithGroupsOrdered();
}