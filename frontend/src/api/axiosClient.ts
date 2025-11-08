import axios from "axios";
import type {
  AxiosInstance,
  AxiosError,
  InternalAxiosRequestConfig,
  AxiosResponse,
} from "axios";

const axiosClient: AxiosInstance = axios.create({
  baseURL: import.meta.env.VITE_API_URL || "http://localhost:8080/api",
  headers: {
    "Content-Type": "application/json",
  },
  timeout: 10000,
});

axiosClient.interceptors.request.use(
  (config: InternalAxiosRequestConfig) => {
    const token = sessionStorage.getItem("accessToken");
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
    if (error.response) {
      const status = error.response.status;
      if (status === 401) {
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
  get: <T>(url: string, data?: any, config?: any): AxiosResponseData<T> =>
    axiosClient.post<T>(url, data, config) as AxiosResponseData<T>, 
  post: <T>(url: string, data?: any, config?: any): AxiosResponseData<T> =>
    axiosClient.post<T>(url, data, config) as AxiosResponseData<T>, 
  put: <T>(url: string, data?: any, config?: any): AxiosResponseData<T> =>
    axiosClient.post<T>(url, data, config) as AxiosResponseData<T>,
  delete: <T>(url: string, data?: any, config?: any): AxiosResponseData<T> =>
    axiosClient.post<T>(url, data, config) as AxiosResponseData<T>,
};

export default typedAxios;