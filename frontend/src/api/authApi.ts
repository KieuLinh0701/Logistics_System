import type { AuthResponse, LoginData, RegisterData } from "../type/auth";
import axiosClient from "./axiosClient";

const authApi = {
  // Đăng nhập
  async login(data: LoginData): Promise<AuthResponse> {
    const res = await axiosClient.post<AuthResponse>("/auth/login", data);

    // Lưu token & user tạm thời
    sessionStorage.setItem("accessToken", res.accessToken);
    sessionStorage.setItem("user", JSON.stringify(res.user));

    return res;
  },

  // Đăng ký
  register(data: RegisterData): Promise<AuthResponse> {
    return axiosClient.post("/auth/register", data);
  },

  // Lấy thông tin người dùng hiện tại
  getProfile(): Promise<AuthResponse["user"]> {
    return axiosClient.get("/auth/me");
  },

  // Đăng xuất
  logout() {
    sessionStorage.removeItem("accessToken");
    sessionStorage.removeItem("user");
  },

  // Lấy người dùng hiện tại
  getCurrentUser() {
    const user = sessionStorage.getItem("user");
    return user ? JSON.parse(user) : null;
  },
};

export default authApi;