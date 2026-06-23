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

        console.error("[AXIOS_ROUTE_ERROR]");
        console.error("URL", error.config?.url);
        console.error("METHOD", error.config?.method);
        console.error("STATUS", error.response?.status);
        console.error("DATA", error.response?.data);
        console.error("FULL", error);

        if (status === 401 && !requestUrl.includes("/auth/login")) {
            sessionStorage.clear();
            window.location.href = "/login";
        }

        if (error.response?.data) {
            return Promise.resolve(error.response.data);
        }

        return Promise.resolve({
            success: false,
            message: error.code === "ECONNABORTED"
                ? "Yêu cầu quá thời gian, vui lòng thử lại"
                : "Không thể kết nối đến máy chủ",
            data: null,
        });
    }
);

const TIMEOUTS = {
    get: 15000,
    post: 60000,
    put: 30000,
    patch: 30000,
    delete: 15000,
};
const typedAxios = {
    get: <T = any>(url: string, config?: any): Promise<T> =>
        axiosClient.get(url, { timeout: TIMEOUTS.get, ...config }) as unknown as Promise<T>,

    post: <T>(url: string, data?: any, config?: any): Promise<T> =>
        axiosClient.post(url, data, { timeout: TIMEOUTS.post, ...config }) as unknown as Promise<T>,

    put: <T>(url: string, data?: any, config?: any): Promise<T> =>
        axiosClient.put(url, data, { timeout: TIMEOUTS.put, ...config }) as unknown as Promise<T>,

    patch: <T>(url: string, data?: any, config?: any): Promise<T> =>
        axiosClient.patch(url, data, { timeout: TIMEOUTS.patch, ...config }) as unknown as Promise<T>,

    delete: <T>(url: string, config?: any): Promise<T> =>
        axiosClient.delete(url, { timeout: TIMEOUTS.delete, ...config }) as unknown as Promise<T>,
};

export default typedAxios;