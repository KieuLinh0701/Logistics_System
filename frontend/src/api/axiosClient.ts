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
    get: <T = any>(url: string, config?: any): Promise<T> =>
        axiosClient.get(url, config) as unknown as Promise<T>,

    post: <T>(url: string, data?: any, config?: any): Promise<T> =>
        axiosClient.post(url, data, config) as unknown as Promise<T>,

    put: <T>(url: string, data?: any, config?: any): Promise<T> =>
        axiosClient.put(url, data, config) as unknown as Promise<T>,

    patch: <T>(url: string, data?: any, config?: any): Promise<T> =>
        axiosClient.patch(url, data, config) as unknown as Promise<T>,

    delete: <T>(url: string, config?: any): Promise<T> =>
        axiosClient.delete(url, config) as unknown as Promise<T>,
};

export default typedAxios;