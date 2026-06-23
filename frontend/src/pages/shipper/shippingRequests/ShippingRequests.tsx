import {useEffect, useRef, useState} from "react";
import {Button, Input, message, Modal, Space, Table, Tag, Typography} from "antd";
import {EyeOutlined, ReloadOutlined, SearchOutlined} from "@ant-design/icons";
import {connectWebSocket, disconnectWebSocket} from "../../../socket/socket";
import {getCurrentUser, getUserId} from "../../../utils/authUtils";
import orderApi from "../../../api/orderApi";
import {dispatchShipperRouteRefresh} from "../deliveryRouteEvents";
import SimpleMap from "../../../components/map/SimpleMap";
import PickupAttemptModal from "../PickupAttemptModal";
import "../../../styles/ListPage.css";
import "../ShipperPagesShared.css";

export default function ShippingRequests() {
  const [list, setList] = useState<any[]>([]);
  const [loading, setLoading] = useState(false);
  const [mapVisible, setMapVisible] = useState(false);
  const [selectedOrder, setSelectedOrder] = useState<any | null>(null);
  const [pickupFailedModalOpen, setPickupFailedModalOpen] = useState(false);
  const [filters, setFilters] = useState<{ search?: string }>({});
  const [pagination, setPagination] = useState({ current: 1, pageSize: 10, total: 0 });
  const paginationRef = useRef(pagination);
  paginationRef.current = pagination;
  const filtersRef = useRef(filters);
  filtersRef.current = filters;

  useEffect(() => {
    let cancelled = false;
    const run = async () => {
      setLoading(true);
      try {
        const res = await orderApi.getShipperPickupByCourierRequests({
          page: pagination.current,
          limit: pagination.pageSize,
        });
        if (cancelled) return;
        const shipperOrders = res.orders || [];
        const q = filters.search?.trim().toLowerCase();
        setList(
          shipperOrders.filter((o) => {
            if (!q) return true;
            return (
              (o.senderName || "").toLowerCase().includes(q) ||
              (o.senderPhone || "").toLowerCase().includes(q) ||
              (o.trackingNumber || "").toLowerCase().includes(q)
            );
          })
        );
        setPagination((p) => ({ ...p, total: res.pagination?.total || 0 }));
      } catch (e) {
        console.error("Error loading shipper pickup orders:", e);
        if (!cancelled) message.error("Không thể tải danh sách yêu cầu");
      } finally {
        if (!cancelled) setLoading(false);
      }
    };
    run();
    return () => {
      cancelled = true;
    };
  }, [pagination.current, pagination.pageSize, filters.search]);

  const refreshList = async (page = pagination.current, pageSize = pagination.pageSize) => {
    const res = await orderApi.getShipperPickupByCourierRequests({ page, limit: pageSize });
    const shipperOrders = res.orders || [];
    const q = filters.search?.trim().toLowerCase();
    setList(
      shipperOrders.filter((o) => {
        if (!q) return true;
        return (
          (o.senderName || "").toLowerCase().includes(q) ||
          (o.senderPhone || "").toLowerCase().includes(q) ||
          (o.trackingNumber || "").toLowerCase().includes(q)
        );
      })
    );
    setPagination((p) => ({ ...p, total: res.pagination?.total || 0 }));
  };

  useEffect(() => {
    const uid = getUserId();
    if (!uid) return;
    connectWebSocket(uid, (msg) => {
      message.info(msg.title || "Thông báo mới");
      if (
        msg.type === "assignment" ||
        msg.type === "shipping_request_accepted" ||
        msg.type === "order_ready_for_pickup"
      ) {
        const { pageSize } = paginationRef.current;
        orderApi
          .getShipperPickupByCourierRequests({ page: 1, limit: pageSize })
          .then((res) => {
            const shipperOrders = res.orders || [];
            const q = filtersRef.current.search?.trim().toLowerCase();
            setList(
              shipperOrders.filter((o) => {
                if (!q) return true;
                return (
                  (o.senderName || "").toLowerCase().includes(q) ||
                  (o.senderPhone || "").toLowerCase().includes(q) ||
                  (o.trackingNumber || "").toLowerCase().includes(q)
                );
              })
            );
            setPagination((p) => ({ ...p, total: res.pagination?.total || 0, current: 1 }));
          })
          .catch(() => {});
      }
    });
    return () => {
      disconnectWebSocket();
    };
  }, []);

  async function accept(id: number) {
    try {
      const rec: any = list.find((r) => r.id === id);
      if (!rec) {
        message.error("Yêu cầu không tồn tại");
        return;
      }

      await orderApi.claimShipperOrderRequest(rec.id);
      message.success("Đã nhận đơn");
      dispatchShipperRouteRefresh();
      await refreshList(1, pagination.pageSize);
      setPagination((p) => ({ ...p, current: 1 }));
    } catch (e) {
      console.error(e);
      message.error("Lỗi khi nhận yêu cầu");
    }
  }

  async function openMapForOrder(order: any) {
    try {
      setLoading(true);
      const detail = await orderApi.getShipperOrderDetail(order.id);
      setSelectedOrder(detail || order);
      setMapVisible(true);
    } catch (e) {
      console.error("Không tải được chi tiết đơn:", e);
      message.error("Không thể tải chi tiết đơn");
      setSelectedOrder(order);
      setMapVisible(true);
    } finally {
      setLoading(false);
    }
  }

  async function markPickedUpFromMap(order: any) {
    try {
      let payload: any = {};
      try {
        await new Promise<void>((resolve) => {
          if (!navigator.geolocation) return resolve();
          navigator.geolocation.getCurrentPosition(
            (pos) => {
              payload.latitude = pos.coords.latitude;
              payload.longitude = pos.coords.longitude;
              resolve();
            },
            () => resolve(),
            { timeout: 5000 }
          );
        });
      } catch (e) {}
      await orderApi.recordPickupAttempt(order.id, { status: "SUCCESS" });
      await orderApi.markShipperPickedUp(order.id, payload);
      message.success("Đã xác nhận đã lấy hàng");
      setMapVisible(false);
      await refreshList();
    } catch (e) {
      console.error(e);
      message.error("Lỗi khi xác nhận đã lấy");
    }
  }

  async function deliverToOriginFromMap(order: any) {
    try {
      await orderApi.deliverShipperToOrigin(order.id, {});
      message.success("Đã nộp hàng tại bưu cục");
      setMapVisible(false);
      await refreshList();
    } catch (e) {
      console.error(e);
      message.error("Lỗi khi nộp tại bưu cục");
    }
  }

  async function submitPickupFailed(values: { failReason: string; note?: string }) {
    if (!selectedOrder) return;
    try {
      setLoading(true);
      await orderApi.recordPickupAttempt(selectedOrder.id, {
        status: "FAILED",
        failReason: values.failReason,
        note: values.note,
      });
      message.success("Đã ghi nhận lấy hàng thất bại");
      setPickupFailedModalOpen(false);
      await refreshList();
      const detail = await orderApi.getShipperOrderDetail(selectedOrder.id);
      setSelectedOrder(detail || selectedOrder);
    } catch (e: any) {
      message.error(e?.response?.data?.message || "Lỗi khi báo lấy hàng thất bại");
    } finally {
      setLoading(false);
    }
  }

  const STATUS_MAP: Record<string, { label: string; color: string }> = {
    READY_FOR_PICKUP: { label: "Sẵn sàng lấy hàng", color: "blue" },
    PICKUP_RETRY: { label: "Lấy hàng thất bại - Thử lại", color: "orange" },
    PICKUP_FAILED_FINAL: { label: "Lấy hàng thất bại - Dừng", color: "red" },
    PICKING_UP: { label: "Đang lấy", color: "orange" },
    PICKED_UP: { label: "Đã lấy", color: "orange" },
    AT_ORIGIN_OFFICE: { label: "Đã nộp tại bưu cục", color: "green" },
    DELIVERED: { label: "Đã giao", color: "green" },
    CANCELLED: { label: "Đã huỷ", color: "red" },
    RETURNED: { label: "Đã hoàn", color: "red" },
  };

  const columns = [
    {
      title: "Mã đơn hàng",
      dataIndex: "trackingNumber",
      key: "trackingNumber",
      width: 140,
      render: (text: string) => (
        <Typography.Text strong className="shipper-table-strong">
          {text}
        </Typography.Text>
      ),
    },
    {
      title: "Thông tin người gửi",
      key: "sender",
      render: (record: any) => {
        const address =
          typeof record.senderAddress === "string"
            ? record.senderAddress
            : (record.senderAddress as any)?.fullAddress ?? "";
        return (
          <Space direction="vertical" size={2}>
            <Typography.Text strong className="shipper-table-strong">
              {record.senderName}
            </Typography.Text>
            <Typography.Text className="shipper-table-muted">{record.senderPhone}</Typography.Text>
            <Typography.Text className="shipper-table-muted">{address}</Typography.Text>
          </Space>
        );
      },
    },
    {
      title: "Dịch vụ & COD",
      key: "serviceCod",
      render: (record: any) => {
        const serviceName =
          typeof record.serviceType === "string"
            ? record.serviceType
            : (record.serviceType as any)?.name ?? "";
        return (
          <Space direction="vertical" size={2}>
            <Typography.Text className="shipper-table-strong">{serviceName || "—"}</Typography.Text>
            <Typography.Text className="shipper-cod-value">
              {record.cod ? `${record.cod.toLocaleString()}đ` : "COD: 0đ"}
            </Typography.Text>
          </Space>
        );
      },
    },
    {
      title: "Trạng thái",
      dataIndex: "status",
      key: "status",
      render: (s: string) => {
        const meta = STATUS_MAP[s] || { label: s, color: "default" };
        return (
          <Tag color={meta.color} style={{ fontWeight: 600, textTransform: "uppercase" }}>
            {meta.label}
          </Tag>
        );
      },
    },
    {
      title: "Thao tác",
      key: "action",
      render: (record: any) => (
        <Space>
          {record.status === "READY_FOR_PICKUP" && (
            <Button type="primary" className="primary-button" onClick={() => accept(record.id)}>
              Nhận
            </Button>
          )}
          <Button icon={<EyeOutlined />} onClick={() => openMapForOrder(record)}>
            Chi tiết
          </Button>
        </Space>
      ),
    },
  ];

  const handleToolbarRefresh = () => {
    setFilters({});
    setPagination((p) => ({ ...p, current: 1 }));
  };

  return (
    <div className="list-page-layout shipper-page-root">
      <div className="list-page-content">
        <div className="shipper-filter-panel">
          <div className="shipper-filter-grow">
            <Input
              allowClear
              className="search-input"
              placeholder="Tìm theo mã đơn, người gửi, SĐT"
              prefix={<SearchOutlined />}
              value={filters.search}
              onChange={(e) => setFilters((f) => ({ ...f, search: e.target.value || undefined }))}
              style={{ width: "100%" }}
            />
          </div>
          <div className="shipper-filter-actions">
            <Button icon={<ReloadOutlined />} onClick={handleToolbarRefresh}>
              Làm mới
            </Button>
          </div>
        </div>

        <div className="list-page-header shipper-page-header">
          <div>
            <h3 className="list-page-title-main">Yêu cầu lấy hàng tại nhà</h3>
            <div className="shipper-header-meta">
              <div className="list-page-tag">Kết quả: {list.length} yêu cầu</div>
            </div>
          </div>
        </div>

        <div className="list-page-table shipper-page-table">
          <Table
            rowKey="id"
            loading={loading}
            columns={columns}
            dataSource={list}
            pagination={{
              current: pagination.current,
              pageSize: pagination.pageSize,
              total: pagination.total,
              onChange: (page, pageSize) => {
                setPagination((p) => ({ ...p, current: page, pageSize: pageSize || 10 }));
              },
            }}
            scroll={{ x: 960 }}
          />

          <Modal
            title={selectedOrder ? `Bản đồ - ${selectedOrder.trackingNumber || selectedOrder.id}` : "Bản đồ"}
            open={mapVisible}
            onCancel={() => setMapVisible(false)}
            footer={
              selectedOrder ? (
                selectedOrder.status === "PICKED_UP" ? (
                  <Space>
                    <Button onClick={() => setMapVisible(false)}>Đóng</Button>
                    <Button
                      type="primary"
                      style={{ backgroundColor: "#16a34a", borderColor: "#16a34a" }}
                      onClick={() => selectedOrder && deliverToOriginFromMap(selectedOrder)}
                    >
                      Nộp tại bưu cục
                    </Button>
                  </Space>
                ) : (
                  <Space>
                    <Button onClick={() => setMapVisible(false)}>Đóng</Button>
                    <Button danger onClick={() => setPickupFailedModalOpen(true)} disabled={selectedOrder.status === "PICKUP_FAILED_FINAL"}>
                      Báo lấy hàng thất bại
                    </Button>
                    <Button type="primary" className="primary-button" onClick={() => selectedOrder && markPickedUpFromMap(selectedOrder)} disabled={selectedOrder.status === "PICKUP_FAILED_FINAL"}>
                      Xác nhận đã lấy
                    </Button>
                  </Space>
                )
              ) : null
            }
            width={800}
          >
            {selectedOrder &&
              (() => {
                const currentUser = getCurrentUser();
                const shipperOffice = (currentUser as any)?.office || selectedOrder.fromOffice || null;
                const deliverOffice = shipperOffice
                  ? {
                      id: shipperOffice.id,
                      name: shipperOffice.name,
                      address: shipperOffice.detail || `${shipperOffice.detail || ""}`,
                      latitude: shipperOffice.latitude,
                      longitude: shipperOffice.longitude,
                    }
                  : null;

                const attempts = selectedOrder.pickupAttempts || [];
                const maxAttempts = selectedOrder.maxPickupAttempts || 0;
                const failedAttempts = attempts.filter((a: any) => a.status === "FAILED").length;

                if (selectedOrder.status === "PICKED_UP") {
                  return <SimpleMap deliveryStops={[]} deliverOffice={deliverOffice} />;
                }

                return (
                  <div>
                    <Typography.Text type="secondary" style={{ display: "block", marginBottom: 12 }}>
                      Lần thử {failedAttempts} / {maxAttempts || "-"}
                    </Typography.Text>
                    <SimpleMap
                      title="Lộ trình nhận hàng"
                      deliveryStops={[
                        {
                          id: selectedOrder.id,
                          trackingNumber: selectedOrder.trackingNumber,
                          recipientName: selectedOrder.senderName || selectedOrder.recipientName,
                          recipientPhone: selectedOrder.senderPhone || selectedOrder.recipientPhone,
                          recipientAddress: selectedOrder.senderAddress || selectedOrder.recipientAddress || "",
                          codAmount: selectedOrder.cod || 0,
                          priority: "normal",
                          serviceType: selectedOrder.serviceType || "Tiêu chuẩn",
                          estimatedTime: "",
                          status: selectedOrder.status === "PICKED_UP" ? "completed" : "in_progress",
                          coordinates: { lat: selectedOrder.latitude || 0, lng: selectedOrder.longitude || 0 },
                          distance: 0,
                          travelTime: 0,
                        },
                      ]}
                      deliverOffice={null}
                    />
                  </div>
                );
              })()}
          </Modal>

          <PickupAttemptModal
            open={pickupFailedModalOpen}
            loading={loading}
            onCancel={() => setPickupFailedModalOpen(false)}
            onSubmit={submitPickupFailed}
          />
        </div>
      </div>
    </div>
  );
}
