import axios from "axios";
import type { BankAccount, BankAccountRequest, BankApiResponse } from "../types/bankAccount";
import axiosClient from "./axiosClient";
import type { ApiResponse } from "../types/response";

const bankAccountApi = {
  // Net
  getBanks: async (): Promise<string[]> => {
    const res = await axios.get<BankApiResponse>('https://api.banklookup.net/bank/list');
    return res.data.data.map((bank: any) => `${bank.name} (${bank.short_name})`);
  },

  // User
  async getUserBankAccounts() {
    const res = await axiosClient.get<ApiResponse<BankAccount[]>>("/user/bank-accounts");
    return res;
  },

  async createUserBankAccount(data: BankAccountRequest) {
    const res = await axiosClient.post<ApiResponse<BankAccount>>("/user/bank-accounts", data);
    return res;
  },

  async updateUserBankAccount(id: number, data: BankAccountRequest) {
    const res = await axiosClient.put<ApiResponse<BankAccount>>(`/user/bank-accounts/${id}`, data);
    return res;
  },

  async deleteUserBankAccount(id: number) {
    const res = await axiosClient.delete<ApiResponse<string>>(`/user/bank-accounts/${id}`);
    return res;
  },

  async setDefaultUserBankAccount(id: number) {
    const res = await axiosClient.patch<ApiResponse<string>>(`/user/bank-accounts/${id}/default`);
    return res;
  },

  async existUserBankAccounts() {
    const res = await axiosClient.get<ApiResponse<Boolean>>("/user/bank-accounts/exists");
    return res;
  },
};

export default bankAccountApi;