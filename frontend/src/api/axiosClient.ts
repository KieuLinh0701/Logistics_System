import type {AxiosError, AxiosInstance, AxiosResponse, InternalAxiosRequestConfig,} from "axios";
import axios from "axios";

const axiosClient: AxiosInstance = axios.create({
  baseURL: import.meta.env.VITE_API_BASE || "http://localhost:8080/api",
  timeout: 60000,
  withCredentials: false,
});


axiosClient.interceptors.request.use(
  (config: InternalAxiosRequestConfig) => {
    const token = sessionStorage.getItem("token");
    if (token) {
      config.headers = config.headers || {};
      (config.headers as any).Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error: AxiosError) => Promise.reject(error)
);

axiosClient.interceptors.response.use(
  (response: AxiosResponse) => response.data,
  (error: AxiosError) => {
    console.error("API Error:", {
      url: error.config?.url,
      method: error.config?.method,
      status: error.response?.status,
      data: error.response?.data,
      message: error.message,
    });

    if (error.response) {
      const status = error.response.status;
      if (status === 401 && !error.config?.url?.includes("/auth/login")) {
        console.warn("Phiên đăng nhập hết hạn, vui lòng đăng nhập lại!");
        sessionStorage.clear();
        window.location.href = "/login";
      } else if (status >= 500) {
        console.error("Lỗi máy chủ, vui lòng thử lại sau!");
      }
    } else if (error.request) {
      console.error("Không thể kết nối tới server!");
    } else {
      console.error("Lỗi không xác định:", error.message);
    }

    return Promise.reject(error);
  }
);

type AxiosResponseData<T> = Promise<T>;

const typedAxios = {
  get: <T = any>(url: string, config?: any): AxiosResponseData<T> =>
    axiosClient.get<T>(url, config).then(res => res as unknown as T),

  post: <T>(url: string, data?: any, config?: any): AxiosResponseData<T> =>
    axiosClient.post<T>(url, data, config) as AxiosResponseData<T>,

  put: <T>(url: string, data?: any, config?: any): AxiosResponseData<T> =>
    axiosClient.put<T>(url, data, config) as AxiosResponseData<T>,

  patch: <T>(url: string, data?: any, config?: any): AxiosResponseData<T> =>
    axiosClient.patch<T>(url, data, config) as AxiosResponseData<T>,

  delete: <T>(url: string, config?: any): AxiosResponseData<T> =>
    axiosClient.delete<T>(url, config) as AxiosResponseData<T>,
};

export default typedAxios;