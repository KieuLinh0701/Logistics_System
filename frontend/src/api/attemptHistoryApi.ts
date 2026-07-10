import type {ApiResponse, ListResponse} from "../types/response";
import type {
    AttemptHistoryItem,
    AttemptSearchParams,
} from "../types/attemptHistory";
import axiosClient from "./axiosClient";

const attemptHistoryApi = {
    async listManagerAttemptHistory(params: AttemptSearchParams) {
        return await axiosClient.get<ApiResponse<ListResponse<AttemptHistoryItem>>>(
            "/manager/attempt-history",
            {params},
        );
    },
};

export default attemptHistoryApi;
