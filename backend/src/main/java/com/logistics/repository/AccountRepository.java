package com.logistics.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.logistics.entity.Account;
import com.logistics.entity.User;

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
}
