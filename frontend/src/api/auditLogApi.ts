import axiosClient from "./axiosClient";
import type {ApiResponse} from "../types/response";
import type {AuditLog, AuditLogSearchRequest} from "../types/auditLog.ts";
import type {SearchRequest} from "../types/request.ts";
import {axiosExport} from "./exportClient.ts";

const auditLogApi = {
  // Manager
  async listManagerAuditLogs(param: AuditLogSearchRequest) {
    return await axiosClient.get<ApiResponse<AuditLog[]>>("/manager/logs", {param});
  },

  async listManagerAuditLogsByEmployeeId(employeeId: number, param: AuditLogSearchRequest) {
    return await axiosClient.get<ApiResponse<AuditLog[]>>(`/manager/employees/${employeeId}/logs`, {param});
  },

  async exportManagerByEmployeeId(employeeId: number, params: AuditLogSearchRequest) {
    try {
      const res = await axiosExport.get(`/manager/employees/${employeeId}/logs/export`, {
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

  async exportManager(params: AuditLogSearchRequest) {
    try {
      const res = await axiosExport.get("/manager/logs/export", {
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

  // User
  async listUserAuditLogs(param: AuditLogSearchRequest) {
    return await axiosClient.get<ApiResponse<AuditLog[]>>("/user/logs", {param});
  },

  async listUserAuditLogsByUserId(userId: number, param: AuditLogSearchRequest) {
    return await axiosClient.get<ApiResponse<AuditLog[]>>(`/user/employees/${userId}/logs`, {param});
  },

  async exportUserByEmployeeId(userId: number, params: AuditLogSearchRequest) {
    try {
      const res = await axiosExport.get(`/user/employees/${userId}/logs/export`, {
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

  async exportManager(params: AuditLogSearchRequest) {
    try {
      const res = await axiosExport.get("/user/logs/export", {
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

  // Manager
  async listAdminAuditLogs(param: AuditLogSearchRequest) {
    return await axiosClient.get<ApiResponse<AuditLog[]>>("/admin/logs", {param});
  },

  async listAdminAuditLogsByEmployeeId(employeeId: number, param: AuditLogSearchRequest) {
    return await axiosClient.get<ApiResponse<AuditLog[]>>(`/admin/employees/${employeeId}/logs`, {param});
  },
};

export default auditLogApi;