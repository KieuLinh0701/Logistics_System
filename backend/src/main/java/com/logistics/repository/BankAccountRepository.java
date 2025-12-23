package com.logistics.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.logistics.entity.BankAccount;
import com.logistics.entity.User;

public interface BankAccountRepository extends JpaRepository<BankAccount, Integer> {

    List<BankAccount> findByUserIdOrderByCreatedAtDesc(Integer userId);

    long countByUserId(Integer userId);

    Optional<BankAccount> findByIdAndUserId(Integer id, Integer userId);

    @Modifying
    @Query("UPDATE BankAccount b SET b.isDefault = false WHERE b.user.id = :userId AND b.id <> :id")
    void clearDefaultExcept(@Param("userId") Integer userId, @Param("id") Integer id);

    boolean existsByUserId(Integer userId);

    @Query("SELECT b FROM BankAccount b WHERE b.user = :user AND b.isDefault = true")
    BankAccount findDefaultByUser(@Param("user") User user);
}