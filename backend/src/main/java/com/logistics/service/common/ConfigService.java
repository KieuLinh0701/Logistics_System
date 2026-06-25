package com.logistics.service.common;

import com.logistics.exception.AppException;
import com.logistics.exception.enums.CommonErrorCode;
import com.logistics.repository.SystemConfigRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

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

    /**
     * Safe getter với fallback mặc định khi key không tồn tại trong DB.
     * Dùng cho các config optional (e.g. MAX_PICKUP_ATTEMPTS khi DB chưa seed).
     */
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

    public boolean getBoolean(String key, boolean defaultValue) {
        return repository.findById(key)
            .map(cfg -> Boolean.parseBoolean(cfg.getValue()))
            .orElse(defaultValue);
    }
}
