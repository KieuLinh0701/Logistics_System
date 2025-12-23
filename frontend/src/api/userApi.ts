import type { TokenResponse } from "../types/auth";
import type { ApiResponse } from "../types/response";
import type { AdminUser, UpadteEmailData, UpadtePasswordData, VerifyEmailUpdateOTPData } from "../types/user";
import axiosClient from "./axiosClient";

const userApi = {
  // All User
  updatePassword(data: UpadtePasswordData): Promise<ApiResponse<null>> {
    return axiosClient.post<ApiResponse<null>>("/user/password/update", data);
  },

  sendEmailUpdateOTP(data: UpadteEmailData): Promise<ApiResponse<null>> {
    return axiosClient.post<ApiResponse<null>>("/user/email/update", data);
  },

  async verifyEmailUpdateOTP(data: VerifyEmailUpdateOTPData): Promise<ApiResponse<TokenResponse>> {
    const res = await axiosClient.post<ApiResponse<TokenResponse>>("/user/email/verify-otp", data);

    if (res.success && res.data) {
      const token = res.data.token;
      const user = res.data.user;
      sessionStorage.setItem("token", token);
      sessionStorage.setItem("user", JSON.stringify(user));
    }

    return res;
  },

  async updateProfile(data: FormData): Promise<ApiResponse<string>> {
    return axiosClient.put<ApiResponse<string>>("/user/profile/update", data);
  },

  // Admin
  async listAdminUsers(params: { page?: number; limit?: number; search?: string }) {
    const res = await axiosClient.get<ApiResponse<{ data: AdminUser[]; pagination: any }>>("/admin/users", { params });
    return res;
  },

  async getAdminUserById(id: number) {
    const res = await axiosClient.get<ApiResponse<AdminUser>>(`/admin/users/${id}`);
    return res;
  },

  async createAdminUser(data: {
    email: string;
    password: string;
    firstName: string;
    lastName: string;
    phoneNumber: string;
    roleId: number;
    isActive?: boolean;
  }) {
    const res = await axiosClient.post<ApiResponse<AdminUser>>("/admin/users", data);
    return res;
  },

  async updateAdminUser(id: number, data: {
    firstName?: string;
    lastName?: string;
    phoneNumber?: string;
    password?: string;
    roleId?: number;
    isActive?: boolean;
  }) {
    const res = await axiosClient.put<ApiResponse<AdminUser>>(`/admin/users/${id}`, data);
    return res;
  },

  async deleteAdminUser(id: number) {
    const res = await axiosClient.delete<ApiResponse<null>>(`/admin/users/${id}`);
    return res;
  },
};

export default userApi;