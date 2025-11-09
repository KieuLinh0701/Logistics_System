package com.logistics.repository;

import com.logistics.entity.OTP;
import com.logistics.enums.OTP.OTPType;

import org.springframework.transaction.annotation.Transactional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface OTPRepository extends JpaRepository<OTP, Integer> {

    Optional<OTP> findByEmailAndOtpAndTypeAndIsUsedFalseAndExpiresAtAfter(
        String email, String otp, OTPType type, LocalDateTime now
    );

    @Modifying
    @Transactional
    @Query("UPDATE OTP o SET o.isUsed = :isUsed " +
           "WHERE o.email = :email AND o.type = :type AND o.isUsed = false")
    void updateIsUsedByEmailAndType(@Param("email") String email,
                                    @Param("type") OTPType type,
                                    @Param("isUsed") boolean isUsed);
}