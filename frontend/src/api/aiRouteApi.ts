import axiosClient from "./axiosClient";
import type {
  AiRoutePlanDetail,
  AiRoutePlanSummary,
  AiOptimizeRequest,
} from "../types/aiRoute";

const unwrap = <T>(res: any): T => {
  if (res?.data !== undefined) return res.data as T;
  return res as T;
};

export const aiRouteApi = {
  preview: async () => {
    const res = await axiosClient.get<any>("/manager/ai-routes/preview");
    return unwrap<{ orderCount: number; aiServiceHealthy: boolean; orders: any[] }>(res);
  },

  optimize: async (body?: AiOptimizeRequest) => {
    const res = await axiosClient.post<any>("/manager/ai-routes/optimize", body || {});
    return unwrap<AiRoutePlanDetail>(res);
  },

  listPlans: async () => {
    const res = await axiosClient.get<any>("/manager/ai-routes/plans");
    return unwrap<AiRoutePlanSummary[]>(res);
  },

  getPlan: async (planId: number) => {
    const res = await axiosClient.get<any>(`/manager/ai-routes/plans/${planId}`);
    return unwrap<AiRoutePlanDetail>(res);
  },

  confirmPlan: async (planId: number) => {
    const res = await axiosClient.post<any>(`/manager/ai-routes/plans/${planId}/confirm`);
    return unwrap<AiRoutePlanDetail>(res);
  },

  cancelPlan: async (planId: number) => {
    const res = await axiosClient.post<any>(`/manager/ai-routes/plans/${planId}/cancel`);
    return unwrap<boolean>(res);
  },
};

export default aiRouteApi;
