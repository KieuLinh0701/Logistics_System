import React, { useCallback, useEffect, useState } from "react";
import { message } from "antd";
import orderApi from "../../../api/orderApi";
import type { AdminOrder, Order } from "../../../types/order";
import "../../hr/recruitment/components/RecruitmentShared.css";
import "../../../styles/ListPage.css";
import "../AdminModal.css";
import "./OrdersPage.css";
import OrdersToolbar from "./components/OrdersToolbar";
import OrdersTable from "./components/OrdersTable";
import OrderDetailsDrawer from "./components/OrderDetailsDrawer";
import { translateOrderStatus } from "../../../utils/orderUtils";

type QueryState = { page: number; limit: number; search: string; status?: string };

const statusOptions = [
  { label: "Bản nháp", value: "DRAFT" },
  { label: "Chờ xử lý", value: "PENDING" },
  { label: "Đã xác nhận", value: "CONFIRMED" },
  { label: "Đã lấy hàng", value: "PICKED_UP" },
  { label: "Đang lấy hàng", value: "PICKING_UP" },
  { label: "Sẵn sàng lấy hàng", value: "READY_FOR_PICKUP" },
  { label: "Tại bưu cục gốc", value: "AT_ORIGIN_OFFICE" },
  { label: "Đang giao", value: "DELIVERING" },
  { label: "Tại văn phòng đích", value: "AT_DEST_OFFICE" },
  { label: "Đang vận chuyển", value: "IN_TRANSIT" },
  { label: "Giao thất bại", value: "FAILED_DELIVERY" },
  { label: "Đã giao", value: "DELIVERED" },
  { label: "Đã hủy", value: "CANCELLED" },
];

const OrdersPage: React.FC = () => {
  const [loading, setLoading] = useState(false);
  const [rows, setRows] = useState<AdminOrder[]>([]);
  const [total, setTotal] = useState(0);
  const [query, setQuery] = useState<QueryState>({ page: 1, limit: 10, search: "" });

  const [searchValue, setSearchValue] = useState("");
  const [filterStatus, setFilterStatus] = useState<string | undefined>(undefined);

  const [drawerOpen, setDrawerOpen] = useState(false);
  const [selectedOrder, setSelectedOrder] = useState<Order | AdminOrder | null>(null);
  const [detailLoading, setDetailLoading] = useState(false);

  useEffect(() => {
    const timer = setTimeout(() => {
      setQuery((prev) => ({ ...prev, page: 1, search: searchValue }));
    }, 300);
    return () => clearTimeout(timer);
  }, [searchValue]);

  useEffect(() => {
    setQuery((prev) => ({ ...prev, page: 1, status: filterStatus }));
  }, [filterStatus]);

  const fetchData = useCallback(async () => {
    try {
      setLoading(true);
      const res = await orderApi.listAdminOrders({
        page: query.page,
        limit: query.limit,
        search: query.search,
        status: query.status,
      });

      if (!res || !res.success) {
        message.error("Lấy danh sách đơn hàng không thành công");
        setRows([]);
        setTotal(0);
        return;
      }

      const payload = res.data as any;
      let list: AdminOrder[] = [];

      if (Array.isArray(payload?.list)) list = payload.list;
      else if (Array.isArray(payload?.data)) list = payload.data;
      else if (Array.isArray(payload?.orders)) list = payload.orders;

      setRows(list);
      setTotal(payload?.pagination?.total || 0);
    } catch (error: any) {
      message.error(error?.response?.data?.message || "Tải dữ liệu thất bại");
    } finally {
      setLoading(false);
    }
  }, [query.page, query.limit, query.search, query.status]);

  useEffect(() => {
    fetchData();
  }, [fetchData]);


  const statusText = (status?: string) => {
    if (!status) return "-";
    return statusOptions.find((item) => item.value === status)?.label || translateOrderStatus(status);
  };

  const paymentText = (paymentStatus?: string, payer?: string) => {
    const statusMap: Record<string, string> = {
      PAID: "Đã thanh toán",
      UNPAID: "Chưa thanh toán",
      REFUNDED: "Đã hoàn tiền",
    };

    const payerMap: Record<string, string> = {
      CUSTOMER: "Người gửi",
      SHOP: "Cửa hàng",
    };

    const statusKey = (paymentStatus || "").toUpperCase();
    const payerKey = (payer || "").toUpperCase();

    if (statusMap[statusKey]) return statusMap[statusKey];
    if (payerMap[payerKey]) return payerMap[payerKey];
    return paymentStatus || payer || "-";
  };

  const onViewDetails = (record: AdminOrder) => {
    setSelectedOrder(null);
    setDrawerOpen(true);
    setDetailLoading(true);

    (async () => {
      try {
        const res = await orderApi.getAdminOrderById(record.id);
        if (res?.success && res.data) setSelectedOrder(res.data);
        else setSelectedOrder(record);
      } catch {
        setSelectedOrder(record);
      } finally {
        setDetailLoading(false);
      }
    })();
  };

  const onDelete = async (id: number) => {
    try {
      await orderApi.deleteAdminOrder(id);
      message.success("Đã xóa");
      fetchData();
    } catch (error: any) {
      message.error(error?.response?.data?.message || "Xóa thất bại");
    }
  };

  const handleRefreshAll = () => {
    setSearchValue("");
    setFilterStatus(undefined);
    setQuery((prev) => ({ ...prev, page: 1, search: "", status: undefined }));
  };


  return (
    <div className="list-page-layout orders-page">
      <div className="list-page-content">
        <OrdersToolbar
          searchValue={searchValue}
          filterStatus={filterStatus}
          statusOptions={statusOptions}
          onSearchChange={setSearchValue}
          onStatusChange={setFilterStatus}
          onRefresh={handleRefreshAll}
        />

        <div className="list-page-header" style={{ marginTop: 12 }}>
          <div>
            <h3 className="list-page-title-main">Quản lý đơn hàng</h3>
            <div style={{ marginTop: 8 }}>
              <div className="list-page-tag">Kết quả trả về: {total} đơn hàng</div>
            </div>
          </div>
        </div>

        <div className="list-page-table">
          <OrdersTable
            loading={loading}
            rows={rows}
            page={query.page}
            pageSize={query.limit}
            total={total}
            statusOptions={statusOptions}
            onPageChange={(page, pageSize) =>
              setQuery((prev) => ({
                ...prev,
                page,
                limit: pageSize || prev.limit,
              }))
            }
            onView={onViewDetails}
            onDelete={onDelete}
          />
        </div>

        <OrderDetailsDrawer
          open={drawerOpen}
          loading={detailLoading}
          selectedOrder={selectedOrder}
          statusText={statusText}
          paymentText={paymentText}
          onClose={() => setDrawerOpen(false)}
        />
      </div>
    </div>
  );
};

export default OrdersPage;
