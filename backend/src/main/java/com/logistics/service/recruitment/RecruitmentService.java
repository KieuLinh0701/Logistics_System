package com.logistics.service.recruitment;

import com.logistics.dto.recruitment.JobApplicationDto;
import com.logistics.dto.recruitment.JobPostingDto;
import com.logistics.enums.JobApplicationStatus;
import com.logistics.enums.JobPostingStatus;
import com.logistics.request.recruitment.CreateJobApplicationRequest;
import com.logistics.request.recruitment.CreateJobPostingRequest;
import com.logistics.request.recruitment.UpdateJobApplicationStatusRequest;
import com.logistics.request.recruitment.UpdateJobPostingRequest;
import com.logistics.response.ListResponse;

public interface RecruitmentService {
    JobPostingDto createJob(CreateJobPostingRequest request);
    ListResponse<JobPostingDto> listJobs(int page, int limit, JobPostingStatus status, Integer officeId);
    JobPostingDto getJobById(Long id);
    JobPostingDto updateJob(Long id, UpdateJobPostingRequest request);
    void deleteJob(Long id);
    JobApplicationDto createApplication(CreateJobApplicationRequest request);
    ListResponse<JobApplicationDto> listApplications(int page, int limit, Long jobPostingId, JobApplicationStatus status);
    JobApplicationDto getApplicationById(Long id);
    JobApplicationDto updateApplicationStatus(Long id, UpdateJobApplicationStatusRequest request);
    void approveApplication(Long applicationId);
}
