package com.logistics.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "ai.service")
public class AiServiceProperties {
    private String baseUrl = "http://localhost:8001";
    private int connectTimeoutMs = 5000;
    private int readTimeoutMs = 120000;
    private int defaultCapacity = 20;
    private double defaultSpeedKmh = 25.0;
    private double defaultFuelCostPerKm = 3000.0;
    private String defaultStartTime = "08:00";
}
