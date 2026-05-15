import axiosClient from "./axiosClient";
import type {ApiResponse, ListResponse} from "../types/response";
import type {
    RecipientAddressRequest,
    RecipientAddressSuggestionRequest, RecipientAddressWithStats,
    RecipientSuggestionAddressResponse,
} from "../types/recipientAddress.ts";
import type {SearchRequest} from "../types/request.ts";

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
};

export default recipientAddressApi;