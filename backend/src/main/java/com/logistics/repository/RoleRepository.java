package com.logistics.repository;

import java.util.Optional;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.logistics.entity.Role;

@Repository
public interface RoleRepository
        extends JpaRepository<Role, Integer>,
        JpaSpecificationExecutor<Role> {

    Optional<Role> findByName(String name);

    Optional<Role> findByNameAndUserOwnerIsNull(String name);

    boolean existsByNameAndUserOwnerId(String name, Integer userOwnerId);

    boolean existsByNameAndUserOwnerIdAndIdNot(String name, Integer userOwnerId, Integer id);

    @Query("""
            SELECT DISTINCT r FROM Role r
            LEFT JOIN FETCH r.permissionGroups pg
            LEFT JOIN FETCH pg.permissionGroupApis pga
            LEFT JOIN FETCH pga.permissionApi
            WHERE r.id = :id
            """)
    Optional<Role> findByIdWithPermissionGroups(@Param("id") Integer id);
}