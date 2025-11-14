package com.logistics.request.user;

import org.springframework.web.multipart.MultipartFile;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UpdateProfileRequest {
    private Integer id;
    private String firstName;
    private String lastName;
    private String phoneNumber;

    private MultipartFile avatarFile;
}
