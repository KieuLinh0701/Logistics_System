package com.logistics.mapper;

import com.logistics.dto.recruitment.JobApplicationDto;
import com.logistics.dto.recruitment.JobPostingDto;
import com.logistics.entity.JobApplication;
import com.logistics.entity.JobPosting;

public class RecruitmentMapper {

    private RecruitmentMapper() {
    }

    public static JobPostingDto toJobPostingDto(JobPosting entity) {
        if (entity == null) {
            return null;
        }

        return new JobPostingDto(
                entity.getId(),
                entity.getTitle(),
                entity.getDescription(),
                entity.getRoleType(),
                entity.getOffice() != null ? entity.getOffice().getId() : null,
                entity.getOffice() != null ? entity.getOffice().getName() : null,
                entity.getStatus(),
                entity.getQuantityNeeded(),
                entity.getShift(),
                entity.getCreatedBy() != null ? entity.getCreatedBy().getId() : null,
                entity.getCreatedAt(),
                entity.getUpdatedAt());
    }

    public static JobApplicationDto toJobApplicationDto(JobApplication entity) {
        if (entity == null) {
            return null;
        }

        JobPosting posting = entity.getJobPosting();

        return new JobApplicationDto(
                entity.getId(),
                posting != null ? posting.getId() : null,
                posting != null ? posting.getTitle() : null,
                posting != null && posting.getOffice() != null ? posting.getOffice().getId() : null,
                posting != null && posting.getOffice() != null ? posting.getOffice().getName() : null,
                entity.getFullName(),
                entity.getPhone(),
                entity.getEmail(),
                entity.getAddress(),
                entity.getCvUrl(),
                entity.getStatus(),
                entity.getCreatedAt());
    }
}
