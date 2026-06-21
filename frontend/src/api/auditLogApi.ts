import axiosClient from "./axiosClient";
import type {ApiResponse, ListResponse} from "../types/response";
import type {AuditLog, AuditLogSearchRequest} from "../types/auditLog.ts";
import {axiosExport} from "./exportClient.ts";

const auditLogApi = {
  // Manager
  async listManagerAuditLogs(params: AuditLogSearchRequest) {
    return await axiosClient.get<ApiResponse<ListResponse<AuditLog>>>("/manager/logs", {params});
  },

  async listManagerAuditLogsByEmployeeId(employeeId: number, params: AuditLogSearchRequest) {
    return await axiosClient.get<ApiResponse<ListResponse<AuditLog>>>(`/manager/employees/${employeeId}/logs`, {params});
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
  async listUserAuditLogs(params: AuditLogSearchRequest) {
    return await axiosClient.get<ApiResponse<ListResponse<AuditLog>>>("/user/logs", {params});
  },

  async listUserAuditLogsByUserId(userId: number, params: AuditLogSearchRequest) {
    return await axiosClient.get<ApiResponse<ListResponse<AuditLog>>>(`/user/employees/${userId}/logs`, {params});
  },

  async exportUserByUserId(userId: number, params: AuditLogSearchRequest) {
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

  async exportUser(params: AuditLogSearchRequest) {
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

  // Admin
  async listAdminAuditLogs(params: AuditLogSearchRequest) {
    return await axiosClient.get<ApiResponse<ListResponse<AuditLog>>>("/admin/logs", {params});
  },

  async listAdminAuditLogsByUserId(userId: number, params: AuditLogSearchRequest) {
    return await axiosClient.get<ApiResponse<ListResponse<AuditLog>>>(`/admin/users/${userId}/logs`, {params});
  },

  async exportAdminByUserId(userId: number, params: AuditLogSearchRequest) {
    try {
      const res = await axiosExport.get(`/admin/users/${userId}/logs/export`, {
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

  async exportAdmin(params: AuditLogSearchRequest) {
    try {
      const res = await axiosExport.get("/admin/logs/export", {
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
};

export default auditLogApi;