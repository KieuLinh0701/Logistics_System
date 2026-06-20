package com.logistics.repository;

import com.logistics.entity.JobPosting;
import com.logistics.enums.JobPostingStatus;
import com.logistics.enums.RecruitmentRoleType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface JobPostingRepository extends JpaRepository<JobPosting, Long> {
    Page<JobPosting> findAllByOrderByCreatedAtDesc(Pageable pageable);

    Page<JobPosting> findByStatusOrderByCreatedAtDesc(JobPostingStatus status, Pageable pageable);

    Page<JobPosting> findByOfficeIdOrderByCreatedAtDesc(Integer officeId, Pageable pageable);

    Page<JobPosting> findByOfficeIdAndStatusOrderByCreatedAtDesc(Integer officeId, JobPostingStatus status, Pageable pageable);

    List<JobPosting> findByStatus(JobPostingStatus status);

    List<JobPosting> findByOfficeId(Integer officeId);

    List<JobPosting> findByRoleType(RecruitmentRoleType roleType);

    List<JobPosting> findByOfficeIdAndStatus(Integer officeId, JobPostingStatus status);
}
