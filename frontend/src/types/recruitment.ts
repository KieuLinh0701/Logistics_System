export type RecruitmentRoleType = "DRIVER" | "SHIPPER";
export type JobPostingStatus = "OPEN" | "CLOSED";
export type JobApplicationStatus = "PENDING" | "REVIEWING" | "APPROVED" | "REJECTED";

export interface JobPosting {
  id: number;
  title: string;
  description: string;
  roleType: RecruitmentRoleType;
  officeId: number;
  officeName: string;
  status: JobPostingStatus;
  quantityNeeded?: number;
  shift?: "MORNING" | "AFTERNOON" | "EVENING" | "FULL_DAY";
  createdByAccountId: number;
  createdAt: string;
  updatedAt: string;
}

export interface JobApplication {
  id: number;
  jobPostingId: number;
  jobTitle: string;
  officeId: number;
  officeName: string;
  fullName: string;
  phone: string;
  email: string;
  address: string;
  cvUrl: string;
  status: JobApplicationStatus;
  createdAt: string;
}

export type Job = JobPosting;
export type JobSummary = Pick<JobPosting, "id" | "title" | "officeId" | "officeName" | "status" | "roleType">;

export interface JobResponse {
  success: boolean;
  message?: string;
  data?: {
    list?: JobPosting[];
    item?: JobPosting;
  };
}

// Requests
export interface GetJobsRequest {
  page?: number;
  limit?: number;
  status?: JobPostingStatus;
  officeId?: number;
}

export interface GetApplicationsRequest {
  page?: number;
  limit?: number;
  status?: JobApplicationStatus;
  jobPostingId?: number;
}

export interface CreateJobRequest {
  title: string;
  description: string;
  roleType: RecruitmentRoleType;
  officeId: number;
  status?: JobPostingStatus;
  quantityNeeded: number;
  shift: "MORNING" | "AFTERNOON" | "EVENING" | "FULL_DAY";
}

export interface UpdateJobRequest {
  title?: string;
  description?: string;
  roleType?: RecruitmentRoleType;
  officeId?: number;
  status?: JobPostingStatus;
  quantityNeeded?: number;
  shift?: "MORNING" | "AFTERNOON" | "EVENING" | "FULL_DAY";
}

export interface CreateApplicationRequest {
  fullName: string;
  phone: string;
  email: string;
  address: string;
  cvUrl: string;
  jobPostingId: number;
}

export interface UpdateApplicationStatusRequest {
  status: JobApplicationStatus;
}
