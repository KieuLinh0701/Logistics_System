import axiosClient from "./axiosClient";
import type {ApiResponse, ListResponse} from "../types/response";
import type {
    RecipientAddress, RecipientAddressRequest,
    RecipientAddressSuggestionRequest,
    RecipientSuggestionResponse
} from "../types/recipientAddress.ts";
import type {SearchRequest} from "../types/request.ts";

const recipientAddressApi = {
    // User
    async getUserSuggestion(params: RecipientAddressSuggestionRequest) {
        const res =
            await axiosClient.get<ApiResponse<RecipientSuggestionResponse>>("/user/recipient-addresses/suggestion", {params});

        return res;
    },

    async getUserAddresses(params: SearchRequest) {
        const res =
            await axiosClient.get<ApiResponse<ListResponse<RecipientAddress>>>("/user/recipient-addresses", {params});
        return res;
    },

    async createUserAddress(data: RecipientAddressRequest) {
        const res = await axiosClient.post<ApiResponse<RecipientAddress>>("/user/recipient-addresses", data);
        return res;
    },

    async updateUserAddress(id: number, data: RecipientAddressRequest) {
        const res = await axiosClient.put<ApiResponse<RecipientAddress>>(`/user/recipient-addresses/${id}`, data);
        return res;
    },

    async deleteUserAddress(id: number) {
        const res = await axiosClient.delete<ApiResponse<string>>(`/user/recipient-addresses/${id}`);
        return res;
    },
};

export default recipientAddressApi;