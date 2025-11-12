package com.logistics.config;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CloudinaryConfig {

    @Bean
    public Cloudinary cloudinary() {
        return new Cloudinary(ObjectUtils.asMap(
                "cloud_name", "dmlasboc3",
                "api_key", "519149597972987",
                "api_secret", "yMOYceawWPwkJxgESK25DgWDnV4"));
    }
}