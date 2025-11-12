import type { AuthResponse, ForgotPasswordResetData, ForgotPasswordEmailData, LoginData, RegisterData, VerifyRegisterOtpData, VerifyResetOTPData, TokenResponse } from "../types/auth";
import axiosClient from "./axiosClient";

const authApi = {
  async login(data: LoginData): Promise<AuthResponse<TokenResponse>> {
    const res = await axiosClient.post<AuthResponse<TokenResponse>>("/auth/login", data);

    if (res.success && res.data) {
      const token = res.data.token;
      const user = res.data.user;
      sessionStorage.setItem("token", token);
      sessionStorage.setItem("user", JSON.stringify(user));
    }

    return res;
  },

  register(data: RegisterData): Promise<AuthResponse<null>> {
    return axiosClient.post<AuthResponse<null>>("/auth/register", data);
  },

  async verifyAndRegisterUser(data: VerifyRegisterOtpData): Promise<AuthResponse<TokenResponse>> {
    const res = await axiosClient.post<AuthResponse<TokenResponse>>("/auth/register/verify-otp", data);

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

  forgotPasswordEmail(data: ForgotPasswordEmailData): Promise<AuthResponse<null>> {
    return axiosClient.post<AuthResponse<null>>("/auth/password/forgot", data);
  },

  forgotPasswordReset(data: ForgotPasswordResetData): Promise<AuthResponse<null>> {
    return axiosClient.post<AuthResponse<null>>("/auth/password/reset", data);
  },

  async verifyResetOtp(data: VerifyResetOTPData): Promise<AuthResponse<string>> {
    const res = await axiosClient.post<AuthResponse<string>>("/auth/password/verify-otp", data);
    return res;
  },
};

export default authApi;