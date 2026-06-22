package com.logistics.config.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "spring.mail")
public class MailProperties {
    private String host;
    private int port;
    private String username;
    private String password;
}