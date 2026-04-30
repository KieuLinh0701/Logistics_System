import React, { useCallback, useEffect, useMemo, useState } from "react";
import { Form, message } from "antd";
import orderApi from "../../../api/orderApi";
import officeApi from "../../../api/officeApi";
import type { AdminOrder, Order } from "../../../types/order";
import "../../hr/recruitment/components/RecruitmentShared.css";
import "../../../styles/ListPage.css";
import "../AdminModal.css";
import "./OrdersPage.css";
import OrdersToolbar from "./components/OrdersToolbar";
import OrdersTable from "./components/OrdersTable";
import OrderDetailsDrawer from "./components/OrderDetailsDrawer";
import UpdateOrderStatusModal from "./components/UpdateOrderStatusModal";

type QueryState = { page: number; limit: number; search: string; status?: string };

type OfficeOption = { id: number; name: string; cityCode?: number; wardCode?: number };

const statusOptions = [
  { label: "Bản nháp", value: "DRAFT" },
  { label: "Chờ xử lý", value: "PENDING" },
  { label: "Đã xác nhận", value: "CONFIRMED" },
  { label: "Đã lấy hàng", value: "PICKED_UP" },
  { label: "Sẵn sàng lấy hàng", value: "READY_FOR_PICKUP" },
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

  const [statusModalOpen, setStatusModalOpen] = useState(false);
  const [offices, setOffices] = useState<OfficeOption[]>([]);
  const [statusForm] = Form.useForm();

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

  useEffect(() => {
    (async () => {
      try {
        const res = await officeApi.listAdminOffices({ page: 1, limit: 200 });
        if (res?.data?.data && Array.isArray(res.data.data)) {
          setOffices(res.data.data);
        }
      } catch {
        setOffices([]);
      }
    })();
  }, []);

  const statusText = (status?: string) => {
    if (!status) return "-";
    return statusOptions.find((item) => item.value === status)?.label || status;
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

  const onUpdateStatus = useCallback(
    async (record: AdminOrder) => {
      setSelectedOrder(null);
      setDetailLoading(true);

      try {
        const res = await orderApi.getAdminOrderById(record.id);
        const full = res?.success && res.data ? res.data : record;
        setSelectedOrder(full);

        try {
          const senderCity = (full as any).senderCityCode ?? (full as any).senderAddress?.cityCode;
          const senderWard = (full as any).senderWardCode ?? (full as any).senderAddress?.wardCode;
          if (senderCity) {
            const offRes = await officeApi.listLocalOffices({ city: senderCity, ward: senderWard });
            if (offRes?.success && Array.isArray(offRes.data) && offRes.data.length > 0) {
              setOffices(offRes.data);
            }
          }
        } catch {
          // keep the preloaded office list if local filtering fails
        }

        statusForm.setFieldsValue({
          status: (full as any).status,
          fromOfficeId: (full as any).fromOffice?.id,
        });

        setStatusModalOpen(true);
      } catch {
        setSelectedOrder(record);
        statusForm.setFieldsValue({ status: record.status, fromOfficeId: record.fromOffice?.id });
        setStatusModalOpen(true);
      } finally {
        setDetailLoading(false);
      }
    },
    [statusForm]
  );

  const onDelete = async (id: number) => {
    try {
      await orderApi.deleteAdminOrder(id);
      message.success("Đã xóa");
      fetchData();
    } catch (error: any) {
      message.error(error?.response?.data?.message || "Xóa thất bại");
    }
  };

  const submitStatusUpdate = async () => {
    try {
      const values = await statusForm.validateFields();
      if (selectedOrder) {
        await orderApi.updateAdminOrderStatus(selectedOrder.id, values);
        message.success("Cập nhật trạng thái thành công");
        setStatusModalOpen(false);
        fetchData();
      }
    } catch (error: any) {
      if (!error?.errorFields) {
        message.error(error?.response?.data?.message || "Cập nhật thất bại");
      }
    }
  };

  const handleRefreshAll = () => {
    setSearchValue("");
    setFilterStatus(undefined);
    setQuery((prev) => ({ ...prev, page: 1, search: "", status: undefined }));
  };

  const officeOptions = useMemo(() => {
    if (!selectedOrder || !Array.isArray(offices)) return offices;

    const senderCity = (selectedOrder as any).senderCityCode ?? (selectedOrder as any).senderAddress?.cityCode;
    const senderWard = (selectedOrder as any).senderWardCode ?? (selectedOrder as any).senderAddress?.wardCode;

    const scored = offices.map((office) => {
      let score = 0;
      if (senderCity && office.cityCode === senderCity) score += 2;
      if (senderWard && office.wardCode === senderWard) score += 1;
      return { office, score };
    });

    scored.sort((a, b) => b.score - a.score);
    return scored.map((item) => item.office);
  }, [offices, selectedOrder]);

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
            onUpdateStatus={onUpdateStatus}
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

        <UpdateOrderStatusModal
          open={statusModalOpen}
          form={statusForm}
          statusOptions={statusOptions}
          offices={officeOptions}
          onCancel={() => setStatusModalOpen(false)}
          onSubmit={submitStatusUpdate}
        />
      </div>
    </div>
  );
};

export default OrdersPage;
