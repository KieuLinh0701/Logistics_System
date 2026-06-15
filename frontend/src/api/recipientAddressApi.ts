import axiosClient from "./axiosClient";
import type {ApiResponse, ListResponse} from "../types/response";
import type {
    RecipientAddressRequest,
    RecipientAddressSuggestionRequest,
    RecipientAddressWithStats,
    RecipientSuggestionAddressResponse,
} from "../types/recipientAddress.ts";
import type {SearchRequest} from "../types/request.ts";
import {axiosExport} from "./exportClient.ts";

const recipientAddressApi = {
    // User
    async getUserSuggestion(params: RecipientAddressSuggestionRequest) {
        const res =
            await axiosClient.get<ApiResponse<RecipientSuggestionAddressResponse>>("/user/recipient-addresses/suggestion", {params});

        return res;
    },

    async getUserAddresses(params: SearchRequest) {
        const res =
            await axiosClient.get<ApiResponse<ListResponse<RecipientAddressWithStats>>>("/user/recipient-addresses", {params});
        return res;
    },

    async createUserAddress(data: RecipientAddressRequest) {
        const res = await axiosClient.post<ApiResponse<RecipientAddressWithStats>>("/user/recipient-addresses", data);
        return res;
    },

    async updateUserAddress(id: number, data: RecipientAddressRequest) {
        const res = await axiosClient.put<ApiResponse<RecipientAddressWithStats>>(`/user/recipient-addresses/${id}`, data);
        return res;
    },

    async deleteUserAddress(id: number) {
        const res = await axiosClient.delete<ApiResponse<string>>(`/user/recipient-addresses/${id}`);
        return res;
    },

    async exportUserAddresses(params: SearchRequest) {
        try {
            const res = await axiosExport.get("/user/recipient-addresses/export", {
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

            return { success: true, fileName };
        } catch (error) {
            return { success: false, error };
        }
    },
};

export default recipientAddressApi;