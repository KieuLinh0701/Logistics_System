import type { ApiResponse, ListResponse } from "../types/response";
import axiosClient from "./axiosClient";
import type { SearchRequest } from "../types/request";
import { axiosExport } from "./exportClient";
import type { ManagerPaymentSubmissionBatch, ManagerPaymentSubmissionBatchCreateRequest, ManagerPaymentSubmissionBatchEditRequest } from "../types/paymentSubmissionBatch";

const paymentSubmissionBatchApi = {
  // Manager
  async listManagerPaymentSubmissionBatchs(params: SearchRequest) {
    const res = await axiosClient.get<ApiResponse<ListResponse<ManagerPaymentSubmissionBatch>>>("/manager/payment-submission-batchs", { params });
    return res;
  },

  async createManagerPaymentSubmissionBatch(data: ManagerPaymentSubmissionBatchCreateRequest) {
    const res = await axiosClient.post<ApiResponse<Boolean>>(`/manager/payment-submission-batchs`, data);
    return res;
  },

  async updateManagerPaymentSubmissionBatch(id: number, data: ManagerPaymentSubmissionBatchEditRequest) {
    const res = await axiosClient.put<ApiResponse<Boolean>>(`/manager/payment-submission-batchs/${id}`, data);
    return res;
  },

  async exportManagerPaymentSubmissionBatchs(params: SearchRequest) {
    try {
      const res = await axiosExport.get("/manager/payment-submission-batchs/export", {
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

export default paymentSubmissionBatchApi;