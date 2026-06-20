package com.logistics.dto.recruitment;

import com.logistics.enums.JobApplicationStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class JobApplicationDto {
    private Long id;
    private Long jobPostingId;
    private String jobTitle;
    private Integer officeId;
    private String officeName;
    private String fullName;
    private String phone;
    private String email;
    private String address;
    private String cvUrl;
    private JobApplicationStatus status;
    private LocalDateTime createdAt;
}
