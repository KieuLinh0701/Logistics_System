package com.logistics.request.common.user;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.web.multipart.MultipartFile;

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
