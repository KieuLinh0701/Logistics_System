import React, { useEffect, useState } from "react";
import { Empty, Modal, Spin, Tabs, message } from "antd";
import orderApi, { type ShipperOrder } from "../../../api/orderApi";
import "./FailedDeliveryOrdersPage.css";
import FailedDeliveriesTable from "./components/FailedDeliveriesTable";
import PickedUpOrdersTable from "./components/PickedUpOrdersTable";
import FailedDeliveriesToolbar from "./components/FailedDeliveriesToolbar";

const FailedDeliveryOrdersPage: React.FC = () => {
  const [loadingFailed, setLoadingFailed] = useState(true);
  const [loadingPickedUp, setLoadingPickedUp] = useState(true);
  const [failedOrders, setFailedOrders] = useState<ShipperOrder[]>([]);
  const [pickedUpOrders, setPickedUpOrders] = useState<ShipperOrder[]>([]);
  const [search, setSearch] = useState<string | undefined>();
  const [actionLoading, setActionLoading] = useState(false);
  const [activeTab, setActiveTab] = useState<string>("failed");

  const fetchFailedOrders = async () => {
    try {
      setLoadingFailed(true);
      const resRetry = await orderApi.getShipperOrders({ page: 1, limit: 200, status: "DELIVERY_RETRY", search });
      const resFailed = await orderApi.getShipperOrders({ page: 1, limit: 200, status: "DELIVERY_FAILED_FINAL", search });
      const resReturnFailed = await orderApi.getShipperOrders({ page: 1, limit: 200, status: "RETURN_FAILED_FINAL", search });
      const retryOrders = (resRetry.orders || []).filter((o: ShipperOrder) => o.status === "DELIVERY_RETRY");
      const failedOrderList = (resFailed.orders || []).filter((o: ShipperOrder) => o.status === "DELIVERY_FAILED_FINAL");
      const returnFailedList = (resReturnFailed.orders || []).filter((o: ShipperOrder) => o.status === "RETURN_FAILED_FINAL");
      setFailedOrders([...retryOrders, ...failedOrderList, ...returnFailedList]);
    } catch {
      // Silently handle error
    } finally {
      setLoadingFailed(false);
    }
  };

  const fetchPickedUpOrders = async () => {
    try {
      setLoadingPickedUp(true);
      const res = await orderApi.getShipperPickedUpByCustomerOrders({ page: 1, limit: 200, search });
      setPickedUpOrders(res.orders || []);
    } catch {
      // Silently handle error
    } finally {
      setLoadingPickedUp(false);
    }
  };

  const fetchAll = async () => {
    await Promise.all([fetchFailedOrders(), fetchPickedUpOrders()]);
  };

  useEffect(() => {
    fetchAll();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  useEffect(() => {
    if (activeTab === "failed") {
      fetchFailedOrders();
    } else if (activeTab === "pickedUp") {
      fetchPickedUpOrders();
    }
  }, [search, activeTab]);

  const handleReturnToOffice = async (orderId: number, trackingNumber: string) => {
    Modal.confirm({
      title: "Xác nhận nộp bưu cục",
      content: `Bạn có chắc chắn muốn nộp đơn "${trackingNumber}" về bưu cục không?`,
      okText: "Xác nhận",
      cancelText: "Hủy",
      okButtonProps: { style: { backgroundColor: "#1C3D90" } },
      onOk: async () => {
        try {
          setActionLoading(true);
          await orderApi.returnFailedToOffice(orderId);
          message.success("Nộp bưu cục thành công");
          await fetchFailedOrders();
        } catch {
          message.error("Nộp bưu cục thất bại");
        } finally {
          setActionLoading(false);
        }
      },
    });
  };

  const handleDeliverToOrigin = async (orderId: number, trackingNumber: string) => {
    Modal.confirm({
      title: "Xác nhận nộp bưu cục",
      content: `Bạn có chắc chắn muốn nộp đơn "${trackingNumber}" về bưu cục gốc không?`,
      okText: "Xác nhận",
      cancelText: "Hủy",
      okButtonProps: { style: { backgroundColor: "#1C3D90" } },
      onOk: async () => {
        try {
          setActionLoading(true);
          await orderApi.deliverShipperToOrigin(orderId);
          message.success("Nộp bưu cục thành công");
          await fetchPickedUpOrders();
        } catch {
          message.error("Nộp bưu cục thất bại");
        } finally {
          setActionLoading(false);
        }
      },
    });
  };

  const handleSearchChange = (value: string | undefined) => {
    setSearch(value);
  };

  const handleRefresh = () => {
    fetchAll();
  };

  const isLoading = activeTab === "failed" ? loadingFailed : loadingPickedUp;
  const currentOrders = activeTab === "failed" ? failedOrders : pickedUpOrders;
  const resultText = `Kết quả: ${currentOrders.length} đơn`;

  const tabItems = [
    {
      key: "failed",
      label: <span className="tab-label">Giao thất bại</span>,
      children: null,
    },
    {
      key: "pickedUp",
      label: <span className="tab-label">Đã lấy từ khách</span>,
      children: null,
    },
  ];

  const renderContent = () => {
    if (activeTab === "failed") {
      if (loadingFailed && failedOrders.length === 0) {
        return (
          <div style={{ textAlign: "center", padding: 32 }}>
            <Spin />
          </div>
        );
      }
      if (failedOrders.length === 0) {
        return (
          <div style={{ padding: 48 }}>
            <Empty description="Không có đơn giao thất bại cần nộp" />
          </div>
        );
      }
      return (
        <FailedDeliveriesTable
          orders={failedOrders}
          loading={loadingFailed}
          onReturnToOffice={handleReturnToOffice}
          isReturning={actionLoading}
        />
      );
    }

    if (loadingPickedUp && pickedUpOrders.length === 0) {
      return (
        <div style={{ textAlign: "center", padding: 32 }}>
          <Spin />
        </div>
      );
    }
    if (pickedUpOrders.length === 0) {
      return (
        <div style={{ padding: 48 }}>
          <Empty description="Không có hàng đã lấy cần nộp" />
        </div>
      );
    }
    return (
      <PickedUpOrdersTable
        orders={pickedUpOrders}
        loading={loadingPickedUp}
        onDeliverToOrigin={handleDeliverToOrigin}
        isDelivering={actionLoading}
      />
    );
  };

  return (
    <div className="list-page-layout failed-deliveries-page-root">
      <div className="list-page-content">
        {/* Toolbar */}
        <FailedDeliveriesToolbar
          search={search}
          onSearchChange={handleSearchChange}
          onRefresh={handleRefresh}
        />

        {/* Tabs */}
        <Tabs
          activeKey={activeTab}
          onChange={setActiveTab}
          className="failed-deliveries-tabs"
          items={tabItems}
        />

        {/* Results badge */}
        <div className="failed-deliveries-results-wrapper">
          <span className="failed-deliveries-results">{resultText}</span>
        </div>

        {/* Table */}
        <div className="failed-deliveries-table">
          {renderContent()}
        </div>
      </div>
    </div>
  );
};

export default FailedDeliveryOrdersPage;
