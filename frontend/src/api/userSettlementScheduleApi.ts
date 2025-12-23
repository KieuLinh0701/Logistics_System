import type { ApiResponse } from "../types/response";
import axiosClient from "./axiosClient";
import type { UserSettlementSchedule } from "../types/UserSettlementBatch";

const userSettlementScheduleApi = {
  // User
  async getUserSchedule() {
    const res = await axiosClient.get<ApiResponse<UserSettlementSchedule>>("/user/user-settlement-batchs");
    return res;
  },

  async updateUserSchedule(param: string[]) {
    const res = await axiosClient.put<ApiResponse<Boolean>>("/user/user-settlement-batchs", param);
    return res;
  },

};

export default userSettlementScheduleApi;