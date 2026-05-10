package com.logistics.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.logistics.entity.SystemConfig;

public interface SystemConfigRepository extends JpaRepository<SystemConfig, String> {
}
