import type { ApiResponse, ListResponse } from "../types/response";
import axiosClient from "./axiosClient";
import type { ManagerPaymentSubmission, ManagerPaymentSubmissionEditRequest } from "../types/paymentSubmission";
import type { SearchRequest } from "../types/request";
import { axiosExport } from "./exportClient";

const paymentSubmissionApi = {
  // Manager
  async listManagerPaymentSubmissions(id: number, params: SearchRequest) {
    const res = await axiosClient.get<ApiResponse<ListResponse<ManagerPaymentSubmission>>>(`/manager/payment-submissions/${id}`, { params });
    return res;
  },

  async updateManagerPaymentSubmission(id: number, data: ManagerPaymentSubmissionEditRequest) {
    const res = await axiosClient.put<ApiResponse<Boolean>>(`/manager/payment-submissions/${id}`, data);
    return res;
  },

  async exportManagerPaymentSubmissions(id: number, params: SearchRequest) {
    try {
      const res = await axiosExport.get(`/manager/payment-submissions/${id}/export`, {
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

export default paymentSubmissionApi;