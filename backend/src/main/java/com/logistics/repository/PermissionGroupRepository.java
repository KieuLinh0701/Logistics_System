package com.logistics.repository;

import com.logistics.entity.PermissionGroup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PermissionGroupRepository
        extends JpaRepository<PermissionGroup, Integer> {

    @Query("SELECT pg FROM PermissionGroup pg " +
            "LEFT JOIN FETCH pg.parent " +
            "WHERE pg.id IN :ids " +
            "AND pg.isSystemOnly = false " +
            "AND pg.isActive = true")
    List<PermissionGroup> findAllByIdsWithParent(@Param("ids") List<Integer> ids);
}