import type {BulkResponse} from "../types/response";
import type {ManagerOrderShipment} from "../types/shipment";
import axiosClient from "./axiosClient";

const shipmentOrderApi = {
  // Manager

  async checkManagerOrderForShipment(id: number, trackingNumber: string) {
    return await axiosClient.get<BulkResponse<ManagerOrderShipment>>(`/manager/shipment-orders/${id}/check`, { params: { trackingNumber } });
  },

  async saveManagerShipmentOrders(id: number, removedOrderIds: number[], addedOrderIds: number[]) {
    const param = { removedOrderIds, addedOrderIds };
    return await axiosClient.post<BulkResponse<string>>(`/manager/shipment-orders/${id}/save-orders`, param);
  },
};

export default shipmentOrderApi;