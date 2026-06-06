package com.logistics.service.common;

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
            .orElseThrow(() -> new RuntimeException("Không tìm thấy cấu hình: " + key));
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            throw new RuntimeException("Giá trị cấu hình không hợp lệ: " + key);
        }
    }

    public boolean getBoolean(String key, boolean defaultValue) {
        return repository.findById(key)
            .map(cfg -> Boolean.parseBoolean(cfg.getValue()))
            .orElse(defaultValue);
    }
}
