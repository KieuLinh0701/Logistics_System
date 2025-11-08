import type { AuthResponse, LoginData, RegisterData, VerifyOTPData } from "../types/auth";
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

  async verifyOTP(data: VerifyOTPData): Promise<AuthResponse<string>> {
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
};

export default authApi;