import React, { useEffect, useState } from "react";
import { Button, Card, Empty, message, Spin } from "antd";
import orderApi, { type ShipperOrder } from "../../../api/orderApi";
import "../../../styles/ListPage.css";
import "../ShipperPagesShared.css";
import FailedDeliveriesTable from "./components/FailedDeliveriesTable";
import FailedDeliveriesToolbar from "./components/FailedDeliveriesToolbar";

const FailedDeliveryOrdersPage: React.FC = () => {
  const [loading, setLoading] = useState(true);
  const [orders, setOrders] = useState<ShipperOrder[]>([]);
  const [search, setSearch] = useState<string | undefined>();
  const [actionLoading, setActionLoading] = useState(false);

  const fetchOrders = async () => {
    try {
      setLoading(true);
      const resRetry = await orderApi.getShipperOrders({ page: 1, limit: 200, status: "DELIVERY_RETRY", search });
      const resFailed = await orderApi.getShipperOrders({ page: 1, limit: 200, status: "DELIVERY_FAILED_FINAL", search });
      const retryOrders = (resRetry.orders || []).filter((o: ShipperOrder) => o.status === "DELIVERY_RETRY");
      const failedOrders = (resFailed.orders || []).filter((o: ShipperOrder) => o.status === "DELIVERY_FAILED_FINAL");
      setOrders([...retryOrders, ...failedOrders]);
    } catch {
      message.error("Lỗi khi tải danh sách hàng giao thất bại");
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchOrders();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const handleReturnToOffice = async (orderId: number) => {
    try {
      setActionLoading(true);
      await orderApi.returnFailedToOffice(orderId);
      message.success("Đã nộp hàng về bưu cục");
      await fetchOrders();
    } catch (error: any) {
      message.error(error?.response?.data?.message || "Lỗi khi nộp hàng về bưu cục");
    } finally {
      setActionLoading(false);
    }
  };

  const handleSearchChange = (value: string | undefined) => {
    setSearch(value);
  };

  return (
    <div className="list-page-layout shipper-page-root">
      <div className="list-page-content">
        <FailedDeliveriesToolbar
          search={search}
          onSearchChange={handleSearchChange}
          onRefresh={fetchOrders}
        />

        <div className="list-page-header shipper-page-header">
          <div>
            <h3 className="list-page-title-main" style={{ margin: 0 }}>
              Hàng giao thất bại
            </h3>
            <div className="shipper-header-meta">
              <div className="list-page-tag">Kết quả: {orders.length} đơn</div>
            </div>
          </div>
        </div>

        <Card className="shipper-page-table list-page-table" bodyStyle={{ padding: 0 }}>
          {loading && !orders.length ? (
            <div style={{ textAlign: "center", padding: 32 }}>
              <Spin />
            </div>
          ) : orders.length === 0 ? (
            <div style={{ padding: 24 }}>
              <Empty description="Không có đơn nào cần nộp về bưu cục" />
            </div>
          ) : (
            <FailedDeliveriesTable
              orders={orders}
              loading={loading}
              onReturnToOffice={handleReturnToOffice}
              isReturning={actionLoading}
            />
          )}
        </Card>
      </div>
    </div>
  );
};

export default FailedDeliveryOrdersPage;
