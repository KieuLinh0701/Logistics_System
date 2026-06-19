import type {ApiResponse} from "../types/response";
import axiosClient from "./axiosClient";
import type {UserSettlementSchedule} from "../types/UserSettlementBatch";

const userSettlementScheduleApi = {
  // User
  async getUserSchedule() {
    return await axiosClient.get<ApiResponse<UserSettlementSchedule>>("/user/user-settlement-batchs");
  },

  async updateUserSchedule(param: string[]) {
    return await axiosClient.put<ApiResponse<void>>("/user/user-settlement-batchs", param);
  },

};

export default userSettlementScheduleApi;