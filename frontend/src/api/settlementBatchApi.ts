import type { ApiResponse, ListResponse } from "../types/response";
import axiosClient from "./axiosClient";
import type { SearchRequest } from "../types/request";
import type { SettlementBatch } from "../types/settlementBatch";
import type { Order } from "../types/order";
import type { SettlementTransaction } from "../types/settlementTransaction";
import { axiosExport } from "./exportClient";

const settlementBatchApi = {
  // User
  async listUserSettlementBatchs(params: SearchRequest) {
    const res = await axiosClient.get<ApiResponse<ListResponse<SettlementBatch>>>("/user/settlement-batchs", { params });
    return res;
  },

  async listUserOrdersBySettlementBatchId(id: number, params: SearchRequest) {
    const res = await axiosClient.get<ApiResponse<ListResponse<Order>>>(`/user/settlement-batchs/${id}/orders`, { params });
    return res;
  },

  async listUserSettlementTransactionsBySettlementBatchId(id: number) {
    const res = await axiosClient.get<ApiResponse<SettlementTransaction[]>>(`/user/settlement-batchs/${id}/transactions`);
    return res;
  },

  async exportUserSettlementBatchs(params: SearchRequest) {
    try {
      const res = await axiosExport.get("/user/settlement-batchs/export", {
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

  async exportUserSettlementBatchDetail(id: number, params: SearchRequest) {
    try {
      const res = await axiosExport.get(`/user/settlement-batchs/export/${id}`, {
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

export default settlementBatchApi;