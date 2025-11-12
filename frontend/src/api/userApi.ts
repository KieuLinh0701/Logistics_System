import type { TokenResponse } from "../types/auth";
import type { UpadteEmailData, UpadtePasswordData, VerifyEmailUpdateOTPData } from "../types/user";
import type { UserResponse } from "../types/user";
import axiosClient from "./axiosClient";

const userApi = {
  updatePassword(data: UpadtePasswordData): Promise<UserResponse<null>> {
    return axiosClient.post<UserResponse<null>>("/user/password/update", data);
  },

  sendEmailUpdateOTP(data: UpadteEmailData): Promise<UserResponse<null>> {
    return axiosClient.post<UserResponse<null>>("/user/email/update", data);
  },

  async verifyEmailUpdateOTP(data: VerifyEmailUpdateOTPData): Promise<UserResponse<TokenResponse>> {
    const res = await axiosClient.post<UserResponse<TokenResponse>>("/user/email/verify-otp", data);

    if (res.success && res.data) {
      const token = res.data.token;
      const user = res.data.user;
      sessionStorage.setItem("token", token);
      sessionStorage.setItem("user", JSON.stringify(user));
    }

    return res;
  },

  async updateProfile(data: FormData): Promise<UserResponse<string>> {
    return axiosClient.put<UserResponse<string>>("/user/profile/update", data);
  },
};

export default userApi;