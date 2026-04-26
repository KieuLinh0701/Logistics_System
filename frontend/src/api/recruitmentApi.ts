import axiosClient from "./axiosClient";
import type { ApiResponse, ListResponse } from "../types/response";
import type {
  JobPosting,
  JobApplication,
  CreateApplicationRequest,
  CreateJobRequest,
  GetApplicationsRequest,
  GetJobsRequest,
  UpdateApplicationStatusRequest,
  UpdateJobRequest,
} from "../types/recruitment";

const recruitmentApi = {
  async getJobs(params: GetJobsRequest) {
    return axiosClient.get<ApiResponse<ListResponse<JobPosting>>>("/jobs", { params });
  },

  async getJobById(id: number) {
    return axiosClient.get<ApiResponse<JobPosting>>(`/jobs/${id}`);
  },

  async createJob(payload: CreateJobRequest) {
    return axiosClient.post<ApiResponse<JobPosting>>("/jobs", payload);
  },

  async updateJob(id: number, payload: UpdateJobRequest) {
    return axiosClient.put<ApiResponse<JobPosting>>(`/jobs/${id}`, payload);
  },

  async deleteJob(id: number) {
    return axiosClient.delete<ApiResponse<string>>(`/jobs/${id}`);
  },

  async createApplication(payload: CreateApplicationRequest) {
    return axiosClient.post<ApiResponse<JobApplication>>("/job-applications", payload);
  },

  async getApplications(params: GetApplicationsRequest) {
    return axiosClient.get<ApiResponse<ListResponse<JobApplication>>>("/job-applications", { params });
  },

  async getApplicationById(id: number) {
    return axiosClient.get<ApiResponse<JobApplication>>(`/job-applications/${id}`);
  },

  async updateApplicationStatus(id: number, payload: UpdateApplicationStatusRequest) {
    return axiosClient.put<ApiResponse<JobApplication>>(`/job-applications/${id}/status`, payload);
  },
};

export default recruitmentApi;
