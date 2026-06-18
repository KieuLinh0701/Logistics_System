package com.logistics.config.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "settlement")
public class SettlementProperties {
    private long warningOverHours;
    private long lockOverHours;
}