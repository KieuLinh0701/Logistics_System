import type {ApiResponse, BulkResponse, ListResponse} from "../types/response";
import axiosClient from "./axiosClient";
import type {Product, UserProductActiveAndInstockRequest, UserProductSearchRequest} from "../types/product";
import {axiosExport} from "./exportClient.ts";

const productApi = {
    // User
    async getUserProducts(params: UserProductSearchRequest) {
        const res = await axiosClient.get<ApiResponse<ListResponse<Product>>>("/user/products", {params});
        return res;
    },

    async createUserProduct(data: FormData) {
        const res = await axiosClient.post<ApiResponse<Product>>("/user/products", data);
        return res;
    },

    async updateUserProduct(data: FormData) {
        const res = await axiosClient.put<ApiResponse<Product>>("/user/products", data);
        return res;
    },

    async deleteUserProduct(productId: number) {
        const res = await axiosClient.delete<ApiResponse<string>>(`/user/products/${productId}`);
        return res;
    },

    async createBulkUserProduct(data: FormData) {
        const res = await axiosClient.post<BulkResponse<Product>>("/user/products/bulk", data);
        return res;
    },

    async getActiveAndInstockUserProducts(params: UserProductActiveAndInstockRequest) {
        const res = await axiosClient.get<ApiResponse<ListResponse<Product>>>("/user/products/active", {params});
        return res;
    },

    async exportUserProducts(params: UserProductSearchRequest) {
        try {
            const res = await axiosExport.get("/user/products/export", {
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
};

export default productApi;