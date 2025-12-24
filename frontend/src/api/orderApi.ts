import type { ApiResponse, ListResponse } from "../types/response";
import type { AdminOrder, CreateOrderSuccess, ManagerOrderRequest, ManagerOrderSearchRequest, Order, OrderPrint, UserOrderRequest, UserOrderSearchRequest } from "../types/order";
import axiosClient from "./axiosClient";
import type { OrderHistory } from "../types/orderHistory";

export interface ShipperStats {
  totalAssigned: number;
  inProgress: number;
  delivered: number;
  failed: number;
  codCollected: number;
}

export type ShipperOrder = Order;

const orderApi = {
  // Admin
  async listAdminOrders(params: { page?: number; limit?: number; search?: string; status?: string }) {
    const res = await axiosClient.get<ApiResponse<ListResponse<AdminOrder>>>("/admin/orders", { params });
    return res;
  },

  async updateAdminOrderStatus(id: number, statusOrPayload: string | { status: string; fromOfficeId?: number }) {
    const payload = typeof statusOrPayload === "string" ? { status: statusOrPayload } : statusOrPayload;
    const res = await axiosClient.put<ApiResponse<AdminOrder>>(`/admin/orders/${id}/status`, payload);
    return res;
  },

  async deleteAdminOrder(id: number) {
    const res = await axiosClient.delete<ApiResponse<null>>(`/admin/orders/${id}`);
    return res;
  },

  // User
  async listUserOrders(params: UserOrderSearchRequest) {
    const res = await axiosClient.get<ApiResponse<ListResponse<Order>>>("/user/orders", { params });
    return res;
  },

  async getAllUserOrderIds(params: UserOrderSearchRequest) {
    const res = await axiosClient.get<ApiResponse<number[]>>("/user/orders/all-ids", { params });
    return res;
  },

  async createUserOrder(params: UserOrderRequest) {
    const res = await axiosClient.post<ApiResponse<CreateOrderSuccess>>("/user/orders", params);
    return res;
  },

  async updateUserOrder(id: number, params: UserOrderRequest) {
    const res = await axiosClient.put<ApiResponse<Boolean>>(`/user/orders/${id}`, params);
    return res;
  },

  async getUserOrderByTrackingNumber(trackingNumber: string) {
    const res = await axiosClient.get<ApiResponse<Order>>(`/user/orders/${trackingNumber}`);
    return res;
  },

  async getUserOrderById(id: number) {
    const res = await axiosClient.get<ApiResponse<Order>>(`/user/orders/id/${id}`);
    return res;
  },

  async getAdminOrderById(id: number) {
    const res = await axiosClient.get<ApiResponse<Order>>(`/admin/orders/${id}`);
    return res;
  },

  async publicUserOrder(id: number) {
    const res = await axiosClient.patch<ApiResponse<string>>(`/user/orders/${id}/public`);
    return res;
  },

  async cancelUserOrder(id: number) {
    const res = await axiosClient.patch<ApiResponse<Boolean>>(`/user/orders/${id}/cancel`);
    return res;
  },

  async deleteUserOrder(id: number) {
    const res = await axiosClient.delete<ApiResponse<Boolean>>(`/user/orders/${id}`);
    return res;
  },

  async printUserOrders(orderIds: number[]) {
    const query = orderIds.join(",");
    const res = await axiosClient.get<ApiResponse<OrderPrint[]>>(`/user/orders/print?orderIds=${query}`);
    return res;
  },

  async setUserOrderReadyForPickup(id: number) {
    const res = await axiosClient.patch<ApiResponse<Boolean>>(`/user/orders/${id}/ready`);
    return res;
  },

  // Shipper 
  async getShipperDashboard() {
    const res = await axiosClient.get<ApiResponse<any>>("/shipper/dashboard");
    return res.data as {
      stats: ShipperStats;
      todayOrders: any[];
      notifications: any[];
    };
  },

  async getShipperOrders(params: { page?: number; limit?: number; status?: string; search?: string }) {
    const res = await axiosClient.get<ApiResponse<any>>("/shipper/orders", { params });
    const data = res.data || {};
    return {
      orders: (data.orders || []) as ShipperOrder[],
      pagination: data.pagination || { page: 1, limit: 10, total: 0 },
    };
  },

  async getShipperUnassignedOrders(params: { page?: number; limit?: number }) {
    const res = await axiosClient.get<ApiResponse<any>>("/shipper/orders-unassigned", { params });
    const data = res.data || {};
    return {
      orders: (data.orders || []) as ShipperOrder[],
      pagination: data.pagination || { page: 1, limit: 10, total: 0 },
    };
  },

  async getShipperPickupByCourierRequests(params: { page?: number; limit?: number }) {
    const res = await axiosClient.get<ApiResponse<any>>("/shipper/pickup-requests", { params });
    const data = res.data || {};
    return {
      orders: (data.orders || []) as ShipperOrder[],
      pagination: data.pagination || { page: 1, limit: 10, total: 0 },
    };
  },

  async claimShipperOrder(orderId: number) {
    await axiosClient.post<ApiResponse<any>>(`/shipper/orders/${orderId}/claim`);
  },

  async markShipperPickedUp(orderId: number, payload?: { latitude?: number; longitude?: number; photoUrl?: string; notes?: string }) {
    const res = await axiosClient.post<ApiResponse<any>>(`/shipper/orders/${orderId}/picked-up`, payload || {});
    return res.data;
  },

  async deliverShipperToOrigin(orderId: number, payload?: { officeId?: number; latitude?: number; longitude?: number; photoUrl?: string; notes?: string }) {
    const res = await axiosClient.post<ApiResponse<any>>(`/shipper/orders/${orderId}/deliver-origin`, payload || {});
    return res.data;
  },

  async unclaimShipperOrder(orderId: number) {
    await axiosClient.post<ApiResponse<any>>(`/shipper/orders/${orderId}/unclaim`);
  },

  async getShipperOrderDetail(id: number) {
    const res = await axiosClient.get<ApiResponse<any>>(`/shipper/orders/${id}`);
    return res.data as ShipperOrder;
  },

  async updateShipperDeliveryStatus(id: number, payload: { status: string; notes?: string }) {
    await axiosClient.put<ApiResponse<any>>(`/shipper/orders/${id}/status`, payload);
  },

  async getShipperDeliveryHistory(params: { page?: number; limit?: number; status?: string }) {
    const res = await axiosClient.get<ApiResponse<any>>("/shipper/history", { params });
    const data = res.data || {};
    return {
      orders: (data.orders || []) as ShipperOrder[],
      pagination: data.pagination || { page: 1, limit: 10, total: 0 },
      stats: (data.stats || {}) as ShipperStats,
    };
  },

  // COD
  async getShipperCODTransactions(params: { page?: number; limit?: number; status?: string; dateFrom?: string; dateTo?: string }) {
    const res = await axiosClient.get<ApiResponse<any>>("/shipper/cod", { params });
    const data = res.data || {};
    return {
      transactions: (data.transactions || []) as any[], // PaymentSubmission list
      pagination: data.pagination || { page: 1, limit: 10, total: 0 },
      summary: data.summary || { totalCollected: 0, totalSubmitted: 0, totalPending: 0, transactionCount: 0 },
    };
  },

  async collectShipperCOD(payload: { orderId: number; actualAmount?: number; notes?: string }) {
    const res = await axiosClient.post<ApiResponse<any>>("/shipper/cod/collect", payload);
    return res.data;
  },

  async submitShipperCOD(payload: { transactionIds: number[]; totalAmount: number; notes?: string; imageUrls?: string[] }) {
    await axiosClient.post<ApiResponse<any>>("/shipper/cod/submit", payload);
  },

  async getShipperCODSubmissionHistory(params: { page?: number; limit?: number; status?: string; dateFrom?: string; dateTo?: string }) {
    const res = await axiosClient.get<ApiResponse<any>>("/shipper/cod/history", { params });
    const data = res.data || {};
    return {
      submissions: (data.submissions || []) as any[],
      pagination: data.pagination || { page: 1, limit: 10, total: 0 },
      summary: data.summary || { totalSubmitted: 0, totalDiscrepancy: 0, totalSubmissions: 0 },
    };
  },

  // Route
  async getShipperRoute() {
    const res = await axiosClient.get<ApiResponse<any>>("/shipper/route");
    return res.data as {
      routeInfo: {
        id: number;
        name: string;
        startLocation: string;
        totalStops: number;
        completedStops: number;
        totalDistance: number;
        estimatedDuration: number;
        totalCOD: number;
        status: string;
      };
      deliveryStops: any[];
    };
  },

  async startShipperRoute(routeId: number) {
    await axiosClient.post<ApiResponse<any>>("/shipper/route/start", { routeId });
  },

  // Report
  async createShipperIncident(payload: { orderId: number; incidentType?: string; title: string; description?: string; priority?: string; images?: string[] }) {
    await axiosClient.post<ApiResponse<any>>("/shipper/incident", payload);
  },

  async getShipperIncidents() {
    const res = await axiosClient.get<ApiResponse<any>>("/shipper/incidents");
    return res.data ?? [];
  },


  async getShipperIncidentDetail(id: number) {
    const res = await axiosClient.get<ApiResponse<any>>(`/shipper/incidents/${id}`);
    return res.data;
  },

  // DRIVER
  async getDriverContext() {
    const res = await axiosClient.get<ApiResponse<any>>("/driver/orders/context");
    const data = res.data || {};
    return {
      office: data.office || null,
      vehicles: data.vehicles || [],
    };
  },

  async getDriverPendingOrders(params: { page?: number; limit?: number }) {
    const res = await axiosClient.get<ApiResponse<any>>("/driver/orders/pending", { params });
    const data = res.data || {};
    return {
      orders: (data.orders || []) as any[],
      pagination: data.pagination || { page: 1, limit: 10, total: 0 },
    };
  },

  async driverPickUp(payload: { vehicleId?: number; orderIds: number[] }) {
    const res = await axiosClient.post<ApiResponse<any>>(
      "/driver/orders/pickup",
      payload
    );
    return res.data;
  },

  // Manager
  async listManagerOrders(params: ManagerOrderSearchRequest) {
    const res = await axiosClient.get<ApiResponse<ListResponse<Order>>>("/manager/orders", { params });
    return res;
  },

  async getAllManagerOrderIds(params: ManagerOrderSearchRequest) {
    const res = await axiosClient.get<ApiResponse<number[]>>("/manager/orders/all-ids", { params });
    return res;
  },

  async createManagerOrder(params: ManagerOrderRequest) {
    const res = await axiosClient.post<ApiResponse<string>>("/manager/orders", params);
    return res;
  },

  async updateManagerOrder(id: number, params: ManagerOrderRequest) {
    const res = await axiosClient.put<ApiResponse<Boolean>>(`/manager/orders/${id}`, params);
    return res;
  },

  async getManagerOrderByTrackingNumber(trackingNumber: string) {
    const res = await axiosClient.get<ApiResponse<Order>>(`/manager/orders/${trackingNumber}`);
    return res;
  },

  async printManagerOrders(orderIds: number[]) {
    const query = orderIds.join(",");
    const res = await axiosClient.get<ApiResponse<OrderPrint[]>>(`/manager/orders/print?orderIds=${query}`);
    return res;
  },

  async cancelManagerOrder(id: number) {
    const res = await axiosClient.patch<ApiResponse<Boolean>>(`/manager/orders/${id}/cancel`);
    return res;
  },

  async setManagerOrderAtOriginOffice(id: number) {
    const res = await axiosClient.patch<ApiResponse<Boolean>>(`/manager/orders/${id}/at-origin-office`);
    return res;
  },

  async confirmManagerOrder(id: number) {
    const res = await axiosClient.patch<ApiResponse<Boolean>>(`/manager/orders/${id}/confirm`);
    return res;
  },

  // Pubic
  async getPublicOrderByTrackingNumber(trackingNumber: string) {
    const res = await axiosClient.get<ApiResponse<OrderHistory[]>>(`/public/orders/${trackingNumber}`);
    return res;
  },
};

export default orderApi;
