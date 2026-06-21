package com.logistics.repository;

import com.logistics.entity.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RoleRepository
        extends JpaRepository<Role, Integer>,
        JpaSpecificationExecutor<Role> {

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