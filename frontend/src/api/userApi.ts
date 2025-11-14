import type { TokenResponse } from "../types/auth";
import type { ApiResponse } from "../types/response";
import type { UpadteEmailData, UpadtePasswordData, VerifyEmailUpdateOTPData } from "../types/user";
import axiosClient from "./axiosClient";

const userApi = {
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
};

export default userApi;