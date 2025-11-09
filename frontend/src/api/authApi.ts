import type { AuthResponse, ForgotPasswordData, LoginData, RegisterData, ResetPasswordData, VerifyRegisterOtpData, VerifyResetOTPData } from "../types/auth";
import axiosClient from "./axiosClient";

const authApi = {
  async login(data: LoginData): Promise<AuthResponse<string>> {
    const res = await axiosClient.post<AuthResponse<string>>("/auth/login", data);

    if (res.success && res.data) {
      const token = res.data;
      sessionStorage.setItem("token", token);
    }

    return res;
  },

  register(data: RegisterData): Promise<AuthResponse<null>> {
    return axiosClient.post<AuthResponse<null>>("/auth/register", data);
  },

  async verifyAndRegisterUser(data: VerifyRegisterOtpData): Promise<AuthResponse<string>> {
    const res = await axiosClient.post<AuthResponse<string>>("/auth/register/verify-otp", data);

    if (res.success && res.data) {
      const token = res.data;
      sessionStorage.setItem("token", token);
    }

    return res;
  },

  // getProfile(): Promise<AuthResponse["user"]> {
  //   return axiosClient.get("/auth/me");
  // },

  logout() {
    sessionStorage.removeItem("token");
  },

  forgotPassword(data: ForgotPasswordData): Promise<AuthResponse<null>> {
    return axiosClient.post<AuthResponse<null>>("/auth/password/forgot", data);
  },

  resetPassword(data: ResetPasswordData): Promise<AuthResponse<null>> {
    return axiosClient.post<AuthResponse<null>>("/auth/password/reset", data);
  },

  async verifyResetOtp(data: VerifyResetOTPData): Promise<AuthResponse<string>> {
    const res = await axiosClient.post<AuthResponse<string>>("/auth/password/verify-otp", data);
    return res;
  },
};

export default authApi;