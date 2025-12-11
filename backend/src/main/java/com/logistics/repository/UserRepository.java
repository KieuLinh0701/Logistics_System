package com.logistics.repository;

import com.logistics.entity.User;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends JpaRepository<User, Integer> {
   Optional<User> findByAccountId(Integer accountId);

   boolean existsByPhoneNumber(String phoneNumber);

   @Query("SELECT u FROM User u " +
         "JOIN FETCH u.account a " +
         "JOIN FETCH a.accountRoles ar " +
         "LEFT JOIN FETCH ar.role " +
         "WHERE u.id = :userId")
   Optional<User> findByIdWithRoles(@Param("userId") Integer userId); 
}
