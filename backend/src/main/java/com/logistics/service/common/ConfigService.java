package com.logistics.service.common;

public interface ConfigService {
    int getInt(String key);
    int getInt(String key, int defaultValue);
    boolean getBoolean(String key, boolean defaultValue);
}