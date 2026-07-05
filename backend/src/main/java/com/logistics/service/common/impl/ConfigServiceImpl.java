package com.logistics.service.common.impl;

import com.logistics.entity.SystemConfig;
import com.logistics.exception.AppException;
import com.logistics.exception.enums.CommonErrorCode;
import com.logistics.repository.SystemConfigRepository;
import com.logistics.service.common.ConfigService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ConfigServiceImpl implements ConfigService {

    private final SystemConfigRepository repository;

    @Override
    public int getInt(String key) {
        String value = repository.findById(key)
            .map(SystemConfig::getValue)
            .orElseThrow(() -> new AppException(CommonErrorCode.CONFIG_NOT_FOUND, key));
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            throw new AppException(CommonErrorCode.CONFIG_VALUE_INVALID, key);
        }
    }

    /**
     * Safe getter với fallback mặc định khi key không tồn tại trong DB.
     * Dùng cho các config optional (e.g. MAX_PICKUP_ATTEMPTS khi DB chưa seed).
     */
    @Override
    public int getInt(String key, int defaultValue) {
        return repository.findById(key)
            .map(cfg -> {
                try {
                    return Integer.parseInt(cfg.getValue());
                } catch (NumberFormatException e) {
                    return defaultValue;
                }
            })
            .orElse(defaultValue);
    }

    @Override
    public boolean getBoolean(String key, boolean defaultValue) {
        return repository.findById(key)
            .map(cfg -> Boolean.parseBoolean(cfg.getValue()))
            .orElse(defaultValue);
    }
}
