package com.logistics.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.logistics.entity.AccountRole;

@Repository
public interface AccountRoleRepository extends JpaRepository<AccountRole, Integer> {
    List<AccountRole> findByAccountId(Integer accountId);

    @Query("SELECT ar FROM AccountRole ar " +
            "WHERE ar.account.id = :accountId " +
            "AND ar.role.name = :roleName")
    Optional<AccountRole> findByAccountIdAndRoleName(@Param("accountId") Integer accountId,
            @Param("roleName") String roleName);
}