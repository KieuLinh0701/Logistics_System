import type {ApiResponse, ListResponse} from "../types/response";
import type {
    ManagerShippingRequestSearchRequest,
    PublicShippingRequestCreate,
    ShippingRequest,
    UserShippingRequestSearchRequest
} from "../types/shippingRequest";
import axiosClient from "./axiosClient";
import {axiosExport} from "./exportClient.ts";

const shippingRequestApi = {
    // User
    async listUserShippingRequests(params: UserShippingRequestSearchRequest) {
        const res = await axiosClient.get<ApiResponse<ListResponse<ShippingRequest>>>("/user/shipping-requests", {params});
        return res;
    },

    async createUserShippingRequest(data: FormData) {
        const res = await axiosClient.post<ApiResponse<void>>("/user/shipping-requests", data);
        return res;
    },

    async updateUserShippingRequest(id: number, data: FormData) {
        const res = await axiosClient.put<ApiResponse<void>>(`/user/shipping-requests/${id}`, data);
        return res;
    },

    async getUserShippingRequestById(id: number) {
        const res = await axiosClient.get<ApiResponse<ShippingRequest>>(`/user/shipping-requests/${id}`);
        return res;
    },

    async getUserShippingRequestByIdForEdit(id: number) {
        const res = await axiosClient.get<ApiResponse<ShippingRequest>>(`/user/shipping-requests/${id}/edit`);
        return res;
    },

    async cancelUserShippingRequest(id: number) {
        const res = await axiosClient.patch<ApiResponse<void>>(`/user/shipping-requests/${id}/cancel`);
        return res;
    },

    async exportUserShippingRequests(params: UserShippingRequestSearchRequest) {
        try {
            const res = await axiosExport.get("/user/shipping-requests/export", {
                params,
                responseType: "blob",
            });

            const blob = res.data;
            const contentDisposition = res.headers['content-disposition'];

            let fileName = "BaoCao.xlsx";

            if (contentDisposition) {
                let fileNameMatch = contentDisposition.match(/filename\*=UTF-8''([^;\n]+)/i);
                if (fileNameMatch && fileNameMatch[1]) {
                    fileName = decodeURIComponent(fileNameMatch[1].trim());
                } else {
                    fileNameMatch = contentDisposition.match(/filename="([^"]+)"/i);
                    if (fileNameMatch && fileNameMatch[1]) {
                        fileName = fileNameMatch[1].trim();
                    }
                }
            }

            const url = window.URL.createObjectURL(blob);
            const link = document.createElement("a");
            link.href = url;
            link.download = fileName;
            document.body.appendChild(link);
            link.click();
            link.remove();
            window.URL.revokeObjectURL(url);

            return {success: true, fileName};
        } catch (error) {
            return {success: false, error};
        }
    },

    // Manager
    async listManagerShippingRequests(
        params: ManagerShippingRequestSearchRequest
    ) {
        const res = await axiosClient.get<ApiResponse<ListResponse<ShippingRequest>>>(
            "/manager/shipping-requests",
            {params}
        );
        return res;
    },

    async exportManagerShippingRequests(params: ManagerShippingRequestSearchRequest) {
        try {
            const res = await axiosExport.get("/manager/shipping-requests/export", {
                params,
                responseType: "blob",
            });

            const blob = res.data;
            const contentDisposition = res.headers['content-disposition'];

            let fileName = "BaoCao.xlsx";

            if (contentDisposition) {
                let fileNameMatch = contentDisposition.match(/filename\*=UTF-8''([^;\n]+)/i);
                if (fileNameMatch && fileNameMatch[1]) {
                    fileName = decodeURIComponent(fileNameMatch[1].trim());
                } else {
                    fileNameMatch = contentDisposition.match(/filename="([^"]+)"/i);
                    if (fileNameMatch && fileNameMatch[1]) {
                        fileName = fileNameMatch[1].trim();
                    }
                }
            }

            const url = window.URL.createObjectURL(blob);
            const link = document.createElement("a");
            link.href = url;
            link.download = fileName;
            document.body.appendChild(link);
            link.click();
            link.remove();
            window.URL.revokeObjectURL(url);

            return {success: true, fileName};
        } catch (error) {
            return {success: false, error};
        }
    },

    async getManagerShippingRequestById(id: number) {
        return axiosClient.get<ApiResponse<ShippingRequest>>(
            `/manager/shipping-requests/${id}`
        );
    },

    async processingManagerShippingRequest(id: number, data: FormData) {
        return axiosClient.put<ApiResponse<void>>(
            `/manager/shipping-requests/${id}`,
            data
        );
    },

    // Shipper
    async listShipperShippingRequests() {
        const res = await axiosClient.get<ApiResponse<ListResponse<ShippingRequest>>>(
            "/shipper/shipping-requests"
        );
        return res;
    },

    async acceptShipperShippingRequest(id: number) {
        const res = await axiosClient.post<ApiResponse<boolean>>(`/shipper/shipping-requests/${id}/accept`);
        return res;
    },

    // Public
    async createPublicShippingRequest(data: PublicShippingRequestCreate) {
        const res = await axiosClient.post<ApiResponse<boolean>>("/public/shipping-requests", data);
        return res;
    },
};


export default shippingRequestApi;