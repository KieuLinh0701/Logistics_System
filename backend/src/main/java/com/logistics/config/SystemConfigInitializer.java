package com.logistics.config;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import com.logistics.entity.SystemConfig;
import com.logistics.repository.SystemConfigRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class SystemConfigInitializer implements ApplicationRunner {

    private final SystemConfigRepository systemConfigRepository;

    @Override
    public void run(ApplicationArguments args) {
        if (!systemConfigRepository.existsById("MAX_PICKUP_ATTEMPTS")) {
            systemConfigRepository.save(new SystemConfig("MAX_PICKUP_ATTEMPTS", "3"));
        }
        if (!systemConfigRepository.existsById("MAX_DELIVERY_ATTEMPTS")) {
            systemConfigRepository.save(new SystemConfig("MAX_DELIVERY_ATTEMPTS", "3"));
        }
    }
}
