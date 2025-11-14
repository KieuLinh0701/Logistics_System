import type {ForgotPasswordResetData, ForgotPasswordEmailData, LoginData, RegisterData, VerifyRegisterOtpData, VerifyResetOTPData, TokenResponse } from "../types/auth";
import type { ApiResponse } from "../types/response";
import axiosClient from "./axiosClient";

const authApi = {
  async login(data: LoginData): Promise<ApiResponse<TokenResponse>> {
    const res = await axiosClient.post<ApiResponse<TokenResponse>>("/auth/login", data);

    if (res.success && res.data) {
      const token = res.data.token;
      const user = res.data.user;
      sessionStorage.setItem("token", token);
      sessionStorage.setItem("user", JSON.stringify(user));
    }

    return res;
  },

  register(data: RegisterData): Promise<ApiResponse<null>> {
    return axiosClient.post<ApiResponse<null>>("/auth/register", data);
  },

  async verifyAndRegisterUser(data: VerifyRegisterOtpData): Promise<ApiResponse<TokenResponse>> {
    const res = await axiosClient.post<ApiResponse<TokenResponse>>("/auth/register/verify-otp", data);

    if (res.success && res.data) {
      const token = res.data.token;
      const user = res.data.user;
      sessionStorage.setItem("token", token);
      sessionStorage.setItem("user", JSON.stringify(user));
    }

    return res;
  },

  logout() {
    sessionStorage.removeItem("token");
    sessionStorage.removeItem("user");
  },

  forgotPasswordEmail(data: ForgotPasswordEmailData): Promise<ApiResponse<null>> {
    return axiosClient.post<ApiResponse<null>>("/auth/password/forgot", data);
  },

  forgotPasswordReset(data: ForgotPasswordResetData): Promise<ApiResponse<null>> {
    return axiosClient.post<ApiResponse<null>>("/auth/password/reset", data);
  },

  async verifyResetOtp(data: VerifyResetOTPData): Promise<ApiResponse<string>> {
    const res = await axiosClient.post<ApiResponse<string>>("/auth/password/verify-otp", data);
    return res;
  },
};

export default authApi;