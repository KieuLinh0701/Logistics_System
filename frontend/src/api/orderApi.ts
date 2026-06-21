import type {ApiResponse, ListResponse} from "../types/response";
import type {
    AdminOrder,
    CreateOrderSuccess,
    ManagerOrderRequest,
    ManagerOrderSearchRequest,
    Order,
    OrderFulfillmentSummary,
    OrderPrint,
    StatusCount,
    UserOrderRequest,
    UserOrderSearchRequest
} from "../types/order";
import axiosClient from "./axiosClient";
import type {OrderHistory} from "../types/orderHistory";
import {axiosExport} from "./exportClient.ts";

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
        const res = await axiosClient.get<ApiResponse<ListResponse<AdminOrder>>>("/admin/orders", {params});
        return res;
    },

    async updateAdminOrderStatus(id: number, statusOrPayload: string | { status: string; fromOfficeId?: number }) {
        const payload = typeof statusOrPayload === "string" ? {status: statusOrPayload} : statusOrPayload;
        const res = await axiosClient.put<ApiResponse<AdminOrder>>(`/admin/orders/${id}/status`, payload);
        return res;
    },

    async deleteAdminOrder(id: number) {
        const res = await axiosClient.delete<ApiResponse<null>>(`/admin/orders/${id}`);
        return res;
    },

    async getAdminOrderById(id: number) {
        const res = await axiosClient.get<ApiResponse<Order>>(`/admin/orders/${id}`);
        return res;
    },

    // User
    async listUserOrders(params: UserOrderSearchRequest) {
        const res = await axiosClient.get<ApiResponse<ListResponse<Order>>>("/user/orders", {params});
        return res;
    },

    async getUserOrderStatusCounts() {
        return axiosClient.get<ApiResponse<StatusCount[]>>("/user/orders/status-counts");
    },

    async getAllUserOrderIds(params: UserOrderSearchRequest) {
        const res = await axiosClient.get<ApiResponse<number[]>>("/user/orders/all-ids", {params});
        return res;
    },

    async createUserOrder(params: UserOrderRequest) {
        const res = await axiosClient.post<ApiResponse<CreateOrderSuccess>>("/user/orders", params);
        return res;
    },

    async updateUserOrder(id: number, params: UserOrderRequest) {
        const res = await axiosClient.put<ApiResponse<void>>(`/user/orders/${id}`, params);
        return res;
    },

    async getUserOrderByTrackingNumber(trackingNumber: string) {
        const res = await axiosClient.get<ApiResponse<Order>>(`/user/orders/tracking/${trackingNumber}`);
        return res;
    },

    async getUserOrderById(id: number) {
        const res = await axiosClient.get<ApiResponse<Order>>(`/user/orders/id/${id}`);
        return res;
    },

    async publicUserOrder(id: number) {
        const res = await axiosClient.patch<ApiResponse<string>>(`/user/orders/${id}/public`);
        return res;
    },

    async cancelUserOrder(id: number) {
        const res = await axiosClient.patch<ApiResponse<void>>(`/user/orders/${id}/cancel`);
        return res;
    },

    async deleteUserOrder(id: number) {
        const res = await axiosClient.delete<ApiResponse<void>>(`/user/orders/${id}`);
        return res;
    },

    async printUserOrders(orderIds: number[]) {
        const query = orderIds.join(",");
        const res = await axiosClient.get<ApiResponse<OrderPrint[]>>(`/user/orders/print?orderIds=${query}`);
        return res;
    },

    async setUserOrderReadyForPickup(id: number) {
        const res = await axiosClient.patch<ApiResponse<void>>(`/user/orders/${id}/ready`);
        return res;
    },

    async exportUserOrders(params: UserOrderSearchRequest) {
        try {
            const res = await axiosExport.get("/user/orders/export", {
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
        const res = await axiosClient.get<ApiResponse<any>>("/shipper/orders", {params});
        const data = res.data || {};
        return {
            orders: (data.orders || []) as ShipperOrder[],
            pagination: data.pagination || {page: 1, limit: 10, total: 0},
        };
    },

    async getShipperUnassignedOrders(params: { page?: number; limit?: number }) {
        const res = await axiosClient.get<ApiResponse<any>>("/shipper/orders-unassigned", {params});
        const data = res.data || {};
        return {
            orders: (data.orders || []) as ShipperOrder[],
            pagination: data.pagination || {page: 1, limit: 10, total: 0},
        };
    },

    async getShipperPickupByCourierRequests(params: { page?: number; limit?: number }) {
        const res = await axiosClient.get<ApiResponse<any>>("/shipper/pickup-requests", {params});
        const data = res.data || {};
        return {
            orders: (data.orders || []) as ShipperOrder[],
            pagination: data.pagination || {page: 1, limit: 10, total: 0},
        };
    },

    async claimShipperOrder(orderId: number) {
        await axiosClient.post<ApiResponse<any>>(`/shipper/orders/${orderId}/claim`);
    },

    async claimShipperOrderRequest(orderId: number) {
        await axiosClient.post<ApiResponse<any>>(`/shipper/orders/${orderId}/claim-request`);
    },

    async markShipperPickedUp(orderId: number, payload?: {
        latitude?: number;
        longitude?: number;
        photoUrl?: string;
        notes?: string
    }) {
        const res = await axiosClient.post<ApiResponse<any>>(`/shipper/orders/${orderId}/picked-up`, payload || {});
        return res.data;
    },

    async deliverShipperToOrigin(orderId: number, payload?: {
        officeId?: number;
        latitude?: number;
        longitude?: number;
        photoUrl?: string;
        notes?: string
    }) {
        const res = await axiosClient.post<ApiResponse<any>>(`/shipper/orders/${orderId}/deliver-origin`, payload || {});
        return res.data;
    },

    async updateShipperDeliveryStatus(id: number, payload: { status: string; notes?: string; failReason?: string }) {
        await axiosClient.put<ApiResponse<any>>(`/shipper/orders/${id}/status`, payload);
    },

    async createDeliveryAttempt(id: number, payload: { status: string; failReason?: string; note?: string; notes?: string }) {
        const res = await axiosClient.post<ApiResponse<any>>(`/shipper/orders/${id}/delivery-attempt`, payload);
        return res.data;
    },

    async returnFailedToOffice(orderId: number) {
        const res = await axiosClient.post<ApiResponse<any>>(`/shipper/orders/${orderId}/return-failed-to-office`);
        return res.data;
    },

    async recordPickupAttempt(orderId: number, payload: { status: string; failReason?: string; note?: string }) {
        const res = await axiosClient.post<ApiResponse<any>>(`/shipper/orders/${orderId}/pickup-attempt`, payload);
        return res.data;
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

    async unclaimShipperOrder(orderId: number) {
        await axiosClient.post<ApiResponse<any>>(`/shipper/orders/${orderId}/unclaim`);
    },

    async getShipperOrderDetail(id: number) {
        const res = await axiosClient.get<ApiResponse<any>>(`/shipper/orders/${id}`);
        return res.data as ShipperOrder;
    },

    async getShipperOrderByTrackingNumber(trackingNumber: string) {
        const res = await axiosClient.get<ApiResponse<any>>(`/shipper/orders/tracking/${trackingNumber}`);
        return res.data as { id?: number; trackingNumber?: string };
    },

    // Partial delivery APIs for shipper
    async startPartialDelivery(orderId: number) {
        const res = await axiosClient.get<ApiResponse<any>>(`/shipper/orders/${orderId}/partial-start`);
        return res.data;
    },

    async markProductDelivered(orderProductId: number, deliveredQuantity: number) {
        const res = await axiosClient.post<ApiResponse<any>>(`/shipper/order-products/${orderProductId}/delivered`, {deliveredQuantity});
        return res.data;
    },

    async markProductReturned(orderProductId: number, returnedQuantity: number, reason?: string) {
        const res = await axiosClient.post<ApiResponse<any>>(`/shipper/order-products/${orderProductId}/returned`, {
            returnedQuantity,
            reason
        });
        return res.data;
    },

    async finishPartialDelivery(orderId: number) {
        const res = await axiosClient.post<ApiResponse<any>>(`/shipper/orders/${orderId}/partial-finish`);
        return res.data;
    },

    // COD
    async getShipperCODTransactions(params: {
        page?: number;
        limit?: number;
        status?: string;
        dateFrom?: string;
        dateTo?: string
    }) {
        const res = await axiosClient.get<ApiResponse<any>>("/shipper/cod", {params});
        const data = res.data || {};
        return {
            transactions: (data.transactions || []) as any[], // PaymentSubmission list
            pagination: data.pagination || {page: 1, limit: 10, total: 0},
            summary: data.summary || {totalCollected: 0, totalSubmitted: 0, totalPending: 0, transactionCount: 0},
        };
    },

    async collectShipperCOD(payload: { orderId: number; actualAmount?: number; notes?: string }) {
        const res = await axiosClient.post<ApiResponse<any>>("/shipper/cod/collect", payload);
        return res.data;
    },

    async submitShipperCOD(payload: {
        transactionIds: number[];
        totalAmount: number;
        notes?: string;
        imageUrls?: string[]
    }) {
        await axiosClient.post<ApiResponse<any>>("/shipper/cod/submit", payload);
    },

    async getShipperCODSubmissionHistory(params: {
        page?: number;
        limit?: number;
        status?: string;
        dateFrom?: string;
        dateTo?: string
    }) {
        const res = await axiosClient.get<ApiResponse<any>>("/shipper/cod/history", {params});
        const data = res.data || {};
        return {
            submissions: (data.submissions || []) as any[],
            pagination: data.pagination || {page: 1, limit: 10, total: 0},
            summary: data.summary || {totalSubmitted: 0, totalDiscrepancy: 0, totalSubmissions: 0},
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
        await axiosClient.post<ApiResponse<any>>("/shipper/route/start", {routeId});
    },

    // Report
    async createShipperIncident(payload: FormData) {
        await axiosClient.post<ApiResponse<any>>("/shipper/incident", payload, {
            headers: {"Content-Type": "multipart/form-data"},
        });
    },

    async getShipperIncidents() {
        const res = await axiosClient.get<ApiResponse<any>>("/shipper/incidents");
        return res.data ?? [];
    },


    async getShipperIncidentDetail(id: number) {
        const res = await axiosClient.get<ApiResponse<any>>(`/shipper/incidents/${id}`);
        return res.data;
    },

    // Manager
    async listManagerOrders(params: ManagerOrderSearchRequest) {
        const res = await axiosClient.get<ApiResponse<ListResponse<Order>>>("/manager/orders", {params});
        return res;
    },

    async getManagerOrderStatusCounts() {
        return axiosClient.get<ApiResponse<StatusCount[]>>("/manager/orders/status-counts");
    },

    async getAllManagerOrderIds(params: ManagerOrderSearchRequest) {
        const res = await axiosClient.get<ApiResponse<number[]>>("/manager/orders/all-ids", {params});
        return res;
    },

    async createManagerOrder(params: ManagerOrderRequest) {
        const res = await axiosClient.post<ApiResponse<string>>("/manager/orders", params);
        return res;
    },

    async updateManagerOrder(id: number, params: ManagerOrderRequest) {
        const res = await axiosClient.put<ApiResponse<boolean>>(`/manager/orders/${id}`, params);
        return res;
    },

    async getManagerOrderByTrackingNumber(trackingNumber: string) {
        const res = await axiosClient.get<ApiResponse<Order>>(`/manager/orders/tracking/${trackingNumber}`);
        return res;
    },

    async printManagerOrders(orderIds: number[]) {
        const query = orderIds.join(",");
        const res = await axiosClient.get<ApiResponse<OrderPrint[]>>(`/manager/orders/print?orderIds=${query}`);
        return res;
    },

    async cancelManagerOrder(id: number) {
        return await axiosClient.patch<ApiResponse<boolean>>(`/manager/orders/${id}/cancel`);
    },

    async setManagerOrderAtOriginOffice(id: number) {
         return await axiosClient.patch<ApiResponse<void>>(`/manager/orders/${id}/at-origin-office`);
    },

    async confirmManagerOrder(id: number) {
        return await axiosClient.patch<ApiResponse<void>>(`/manager/orders/${id}/confirm`);
    },

    // Public
    async getPublicOrderByTrackingNumber(trackingNumber: string) {
        const res = await axiosClient.get<ApiResponse<OrderHistory[]>>(`/public/orders/${trackingNumber}`);
        return res;
    },

    //
    async getFulfillmentSummary(orderId: number) {
        const res = await axiosClient.get<ApiResponse<OrderFulfillmentSummary>>(`/orders/${orderId}/fulfillment-summary`);
        return res;
    },

    async exportManagerOrders(params: ManagerOrderSearchRequest) {
        try {
            const res = await axiosExport.get("/manager/orders/export", {
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

export default orderApi;
