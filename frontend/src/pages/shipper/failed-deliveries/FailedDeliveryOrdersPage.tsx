import React, {useEffect, useMemo, useState} from "react";
import {Button, Empty, message, Modal, Spin, Tabs} from "antd";
import {InboxOutlined} from "@ant-design/icons";
import orderApi, {type ShipperOrder} from "../../../api/orderApi";
import "./FailedDeliveryOrdersPage.css";
import FailedDeliveriesTable from "./components/FailedDeliveriesTable";
import PickedUpOrdersTable from "./components/PickedUpOrdersTable";
import FailedDeliveriesToolbar from "./components/FailedDeliveriesToolbar";

type ActiveTab = "failed" | "pickedUp";

const FailedDeliveryOrdersPage: React.FC = () => {
  const [loadingFailed, setLoadingFailed] = useState(true);
  const [loadingPickedUp, setLoadingPickedUp] = useState(true);
  const [failedOrders, setFailedOrders] = useState<ShipperOrder[]>([]);
  const [pickedUpOrders, setPickedUpOrders] = useState<ShipperOrder[]>([]);
  const [search, setSearch] = useState<string | undefined>();
  const [actionLoading, setActionLoading] = useState(false);
  const [activeTab, setActiveTab] = useState<ActiveTab>("failed");

  const [failedSelectedKeys, setFailedSelectedKeys] = useState<React.Key[]>([]);
  const [pickedUpSelectedKeys, setPickedUpSelectedKeys] = useState<React.Key[]>([]);

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
      const list = res.orders || [];
      setPickedUpOrders(list);
      // Sau khi reload danh sách, loại bỏ các key không còn hợp lệ (status khác PICKED_UP hoặc không còn trong list)
      setPickedUpSelectedKeys((prev) =>
        prev.filter((k) => list.some((o) => o.id === k && o.status === "PICKED_UP"))
      );
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

  // Đổi tab → clear selection của tab đó để tránh nộp nhầm
  useEffect(() => {
    if (activeTab === "failed") {
      setPickedUpSelectedKeys([]);
    } else {
      setFailedSelectedKeys([]);
    }
  }, [activeTab]);

  const submitFailedSelected = async () => {
    if (failedSelectedKeys.length === 0) {
      message.warning("Vui lòng chọn ít nhất một đơn để nộp");
      return;
    }
    const trackingList = failedOrders
      .filter((o) => failedSelectedKeys.includes(o.id))
      .map((o) => o.trackingNumber)
      .join(", ");
    Modal.confirm({
      title: "Xác nhận nộp bưu cục",
      content: `Bạn có chắc chắn muốn nộp ${failedSelectedKeys.length} đơn giao thất bại (${trackingList}) về bưu cục không?`,
      okText: "Xác nhận",
      cancelText: "Hủy",
      okButtonProps: { style: { backgroundColor: "#1C3D90" } },
      onOk: async () => {
        const orderIds = failedSelectedKeys.map((k) => Number(k));
        let successCount = 0;
        let failedCount = 0;
        const failedList: string[] = [];
        try {
          setActionLoading(true);
          for (const orderId of orderIds) {
            try {
              await orderApi.returnFailedToOffice(orderId);
              successCount++;
            } catch {
              failedCount++;
              const o = failedOrders.find((x) => x.id === orderId);
              if (o) failedList.push(o.trackingNumber);
            }
          }
          if (failedCount === 0) {
            message.success(`Nộp bưu cục thành công ${successCount} đơn`);
          } else if (successCount === 0) {
            message.error(`Nộp bưu cục thất bại ${failedCount} đơn`);
          } else {
            message.warning(
              `Nộp thành công ${successCount} đơn, thất bại ${failedCount} đơn (${failedList.join(", ")})`
            );
          }
          setFailedSelectedKeys([]);
          await fetchFailedOrders();
        } finally {
          setActionLoading(false);
        }
      },
    });
  };

  const submitPickedUpSelected = async () => {
    if (pickedUpSelectedKeys.length === 0) {
      message.warning("Vui lòng chọn ít nhất một đơn để nộp");
      return;
    }
    const trackingList = pickedUpOrders
      .filter((o) => pickedUpSelectedKeys.includes(o.id) && o.status === "PICKED_UP")
      .map((o) => o.trackingNumber)
      .join(", ");
    Modal.confirm({
      title: "Xác nhận nộp bưu cục",
      content: `Bạn có chắc chắn muốn nộp ${pickedUpSelectedKeys.length} đơn đã lấy từ khách (${trackingList}) về bưu cục gốc không?`,
      okText: "Xác nhận",
      cancelText: "Hủy",
      okButtonProps: { style: { backgroundColor: "#1C3D90" } },
      onOk: async () => {
        const orderIds = pickedUpSelectedKeys.map((k) => Number(k));
        let successCount = 0;
        let failedCount = 0;
        const failedList: string[] = [];
        try {
          setActionLoading(true);
          for (const orderId of orderIds) {
            const order = pickedUpOrders.find((o) => o.id === orderId);
            // Chỉ nộp các đơn PICKED_UP (status bảo vệ bổ sung)
            if (!order || order.status !== "PICKED_UP") {
              failedCount++;
              if (order) failedList.push(order.trackingNumber);
              continue;
            }
            try {
              await orderApi.deliverShipperToOrigin(orderId);
              successCount++;
            } catch {
              failedCount++;
              failedList.push(order.trackingNumber);
            }
          }
          if (failedCount === 0) {
            message.success(`Nộp bưu cục thành công ${successCount} đơn`);
          } else if (successCount === 0) {
            message.error(`Nộp bưu cục thất bại ${failedCount} đơn`);
          } else {
            message.warning(
              `Nộp thành công ${successCount} đơn, thất bại ${failedCount} đơn (${failedList.join(", ")})`
            );
          }
          setPickedUpSelectedKeys([]);
          await fetchPickedUpOrders();
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
    // Clear selection của cả 2 tab khi reload thủ công
    setFailedSelectedKeys([]);
    setPickedUpSelectedKeys([]);
    fetchAll();
  };

  const isLoading = activeTab === "failed" ? loadingFailed : loadingPickedUp;
  const currentOrders = activeTab === "failed" ? failedOrders : pickedUpOrders;
  const resultText = `Kết quả: ${currentOrders.length} đơn`;

  const failedSelectedCount = failedSelectedKeys.length;
  const pickedUpSelectedCount = pickedUpSelectedKeys.length;

  const selectedCount = activeTab === "failed" ? failedSelectedCount : pickedUpSelectedCount;

  const submitButtonText = useMemo(() => {
    return selectedCount > 0 ? `Nộp bưu cục (${selectedCount})` : "Nộp bưu cục";
  }, [selectedCount]);

  const handleSubmitSelected = () => {
    if (actionLoading) return;
    if (activeTab === "failed") {
      submitFailedSelected();
    } else {
      submitPickedUpSelected();
    }
  };

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
          selectedRowKeys={failedSelectedKeys}
          onSelectionChange={setFailedSelectedKeys}
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
        selectedRowKeys={pickedUpSelectedKeys}
        onSelectionChange={setPickedUpSelectedKeys}
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
          onChange={(key) => setActiveTab(key as ActiveTab)}
          className="failed-deliveries-tabs"
          items={tabItems}
        />

        {/* Results badge + Selection action bar */}
        <div className="failed-deliveries-action-bar">
          <span className="failed-deliveries-results">{resultText}</span>
          <div className="failed-deliveries-action-bar-right">
            <Button
              type="primary"
              icon={<InboxOutlined />}
              disabled={selectedCount === 0 || actionLoading}
              loading={actionLoading}
              onClick={handleSubmitSelected}
              className="failed-deliveries-submit-btn"
            >
              {submitButtonText}
            </Button>
          </div>
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