package com.logistics.service.common;

import com.logistics.exception.AppException;
import com.logistics.exception.enums.CommonErrorCode;
import org.springframework.stereotype.Service;

import com.logistics.repository.SystemConfigRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ConfigService {

    private final SystemConfigRepository repository;

    public int getInt(String key) {
        String value = repository.findById(key)
            .map(cfg -> cfg.getValue())
            .orElseThrow(() -> new AppException(CommonErrorCode.CONFIG_NOT_FOUND, key));
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            throw new AppException(CommonErrorCode.CONFIG_VALUE_INVALID, key);
        }
    }

    public boolean getBoolean(String key, boolean defaultValue) {
        return repository.findById(key)
            .map(cfg -> Boolean.parseBoolean(cfg.getValue()))
            .orElse(defaultValue);
    }
}
