package com.logistics.config.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "payment")
public class PaymentProperties {
    private long minAmount;
    private long expireMinutes;
}