// file: api/exportClient.ts
import axios from "axios";

export const axiosExport = axios.create({
  baseURL: import.meta.env.VITE_API_URL || "http://localhost:8080/api",
  timeout: 10000,
  withCredentials: false,
});

axiosExport.interceptors.request.use((config) => {
  const token = sessionStorage.getItem("token");
  if (token) {
    config.headers = config.headers || {};
    (config.headers as any).Authorization = `Bearer ${token}`;
  }
  return config;
});