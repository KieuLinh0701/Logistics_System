package com.logistics.repository;

import com.logistics.entity.OTP;
import com.logistics.enums.OTP.OTPType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface OTPRepository extends JpaRepository<OTP, Integer> {

    Optional<OTP> findByEmailAndOtpAndTypeAndIsUsedFalseAndExpiresAtAfter(
        String email, String otp, OTPType type, LocalDateTime now
    );
}