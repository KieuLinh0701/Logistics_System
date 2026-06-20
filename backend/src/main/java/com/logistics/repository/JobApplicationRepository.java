package com.logistics.repository;

import com.logistics.entity.JobApplication;
import com.logistics.enums.JobApplicationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface JobApplicationRepository extends JpaRepository<JobApplication, Long> {
    Page<JobApplication> findAllByOrderByCreatedAtDesc(Pageable pageable);

    Page<JobApplication> findByStatusOrderByCreatedAtDesc(JobApplicationStatus status, Pageable pageable);

    Page<JobApplication> findByJobPostingIdOrderByCreatedAtDesc(Long jobPostingId, Pageable pageable);

    Page<JobApplication> findByJobPostingOfficeIdOrderByCreatedAtDesc(Integer officeId, Pageable pageable);

    Page<JobApplication> findByJobPostingOfficeIdAndStatusOrderByCreatedAtDesc(Integer officeId, JobApplicationStatus status, Pageable pageable);

    List<JobApplication> findByStatus(JobApplicationStatus status);

    List<JobApplication> findByJobPostingId(Long jobPostingId);

    List<JobApplication> findByJobPostingOfficeId(Integer officeId);

    boolean existsByEmailAndJobPostingId(String email, Long jobPostingId);
}
