import type { BulkResponse } from "../types/response";
import type { ManagerOrderShipment } from "../types/shipment";
import axiosClient from "./axiosClient";

const shipmentOrderApi = {
  // Manager
  async createManagerShipperAssignment(id: number, param: string[]) {
    const res = await axiosClient.post<BulkResponse<string>>(`/manager/shipment-orders/${id}`, param);
    return res;
  },

  async checkManagerOrderForShipment(id: number, trackingNumber: string) {
    const res = await axiosClient.get<BulkResponse<ManagerOrderShipment>>(`/manager/shipment-orders/${id}/check`, { params: { trackingNumber } });
    return res;
  },

  async saveManagerShipmentOrders(id: number, removedOrderIds: number[], addedOrderIds: number[]) {
    const param = { removedOrderIds, addedOrderIds };
    const res = await axiosClient.post<BulkResponse<string>>(`/manager/shipment-orders/${id}/save-orders`, param);
    return res;
  },
};

export default shipmentOrderApi;