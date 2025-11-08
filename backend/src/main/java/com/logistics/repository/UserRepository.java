package com.logistics.repository;

import com.logistics.entity.User;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends JpaRepository<User, Integer> {
   Optional<User> findByAccountId(Integer accountId);
}
