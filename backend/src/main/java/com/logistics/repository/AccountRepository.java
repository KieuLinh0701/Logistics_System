package com.logistics.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.logistics.entity.Account;
import com.logistics.entity.User;

import java.util.List;
import java.util.Optional;

@Repository
public interface AccountRepository extends JpaRepository<Account, Integer> {
    Optional<Account> findByEmail(String email);
    boolean existsByEmail(String email);
    Optional<Account> findByUser(User user);
    
    @Query("SELECT a FROM Account a WHERE a.email LIKE %:search% OR a.user.firstName LIKE %:search% OR a.user.lastName LIKE %:search% OR a.user.phoneNumber LIKE %:search%")
    Page<Account> findByEmailContainingOrUserFirstNameContainingOrUserLastNameContainingOrUserPhoneNumberContaining(
        @Param("search") String search1,
        @Param("search") String search2,
        @Param("search") String search3,
        @Param("search") String search4,
        Pageable pageable
    );

        @Query("""
                        SELECT DISTINCT a FROM Account a
                        JOIN FETCH a.user u
                        JOIN a.accountRoles ar
                        JOIN ar.role r
                        WHERE a.isActive = true
                            AND ar.isActive = true
                            AND LOWER(r.name) IN ('manager', 'admin')
                        """)
        List<Account> findActiveSupportStaffAccounts();

        @Query("""
                        SELECT DISTINCT a FROM Account a
                        JOIN FETCH a.user u
                        JOIN a.accountRoles ar
                        JOIN ar.role r
                        WHERE a.isActive = true
                            AND ar.isActive = true
                            AND LOWER(r.name) = 'manager'
                        """)
        List<Account> findActiveSupportManagers();

    @Query("SELECT r.name FROM Account a JOIN a.accountRoles ar JOIN ar.role r WHERE a.id = :accountId AND ar.isActive = true")
    List<String> findActiveRoleNamesByAccountId(@Param("accountId") Integer accountId);

    @Query("SELECT DISTINCT a FROM Account a JOIN a.accountRoles ar JOIN ar.role r " +
        "WHERE (:search IS NULL OR a.email LIKE %:search% OR a.user.firstName LIKE %:search% OR a.user.lastName LIKE %:search% OR a.user.phoneNumber LIKE %:search%) " +
        "AND (:roleName IS NULL OR LOWER(r.name) = LOWER(:roleName)) " +
        "AND (:statusBool IS NULL OR a.isActive = :statusBool) " +
        "AND ar.isActive = true")
    Page<Account> findBySearchAndRoleAndStatus(@Param("search") String search,
                            @Param("roleName") String roleName,
                            @Param("statusBool") Boolean statusBool,
                            Pageable pageable);
}
