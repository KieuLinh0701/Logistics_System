import type {AxiosError, AxiosInstance, AxiosResponse, InternalAxiosRequestConfig,} from "axios";
import axios from "axios";

const axiosClient: AxiosInstance = axios.create({
  baseURL: import.meta.env.VITE_API_BASE || "http://localhost:8080/api",
  timeout: 15000,
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
        const status = error.response?.status;
        const requestUrl = error.config?.url || "";

        if (status === 401 && !requestUrl.includes("/auth/login")) {
            sessionStorage.clear();
            window.location.href = "/login";
        }

        return Promise.resolve(error.response?.data);
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