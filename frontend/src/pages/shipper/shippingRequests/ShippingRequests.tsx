import {useEffect, useRef, useState} from "react";
import {Button, Input, message, Modal, Space, Spin, Table, Tag, Typography} from "antd";
import {EyeOutlined, PictureOutlined, ReloadOutlined, SearchOutlined} from "@ant-design/icons";
import {connectWebSocket, disconnectWebSocket} from "../../../socket/socket";
import {getUserId} from "../../../utils/authUtils";
import orderApi from "../../../api/orderApi";
import {dispatchShipperRouteRefresh} from "../deliveryRouteEvents";
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
  // Track các order đang được accept để disable button chống double-click.
  // Set thay vì single id vì sau refresh list có thể có nhiều đơn.
  const [acceptingIds, setAcceptingIds] = useState<Set<number>>(new Set());
  // Modal xác nhận đã lấy hàng với upload ảnh
  const [confirmPickupModalVisible, setConfirmPickupModalVisible] = useState(false);
  const [confirmPickupImageFile, setConfirmPickupImageFile] = useState<File | null>(null);
  const [confirmPickupImagePreview, setConfirmPickupImagePreview] = useState<string | null>(null);
  const [confirmPickupLoading, setConfirmPickupLoading] = useState(false);
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
    // [DOUBLE-CLICK GUARD] Nếu đang accept id này rồi thì bỏ qua.
    if (acceptingIds.has(id)) {
      console.log("[ACCEPT_PICKUP_GUARD] orderId={} đang xử lý, bỏ qua click trùng", id);
      return;
    }
    try {
      const rec: any = list.find((r) => r.id === id);
      if (!rec) {
        message.error("Yêu cầu không tồn tại");
        return;
      }

      // [DOUBLE-CLICK GUARD] Đánh dấu đang accept -> disable button.
      setAcceptingIds((prev) => {
        const next = new Set(prev);
        next.add(id);
        return next;
      });

      // [Phase] orderApi.claimShipperOrderRequest trả về response body đã được unwrap
      // bởi axios interceptor, shape: { success, data: { message, requiresReoptimize, ... }, message }
      // Một số trường hợp interceptor chưa unwrap -> fallback về res.data hoặc res.
      const res: any = await orderApi.claimShipperOrderRequest(rec.id);
      console.log("[ACCEPT_PICKUP_RESPONSE]", res);
      const data = res?.data ?? res;
      const isSuccess = res?.success !== false && data?.success !== false;
      const requiresReoptimize = data?.requiresReoptimize === true;

      // 1. Show message từ backend (ưu tiên), fallback nếu backend không trả.
      //    Trường hợp success=false: dùng message.error để shipper thấy rõ là lỗi.
      const msg = data?.message || res?.message || (requiresReoptimize
          ? "Đã thêm đơn pickup vào chuyến đang chạy"
          : isSuccess
            ? "Đã nhận yêu cầu lấy hàng"
            : "Không thể nhận yêu cầu lấy hàng");
      if (isSuccess) {
        message.success(msg);
      } else {
        message.error(msg);
      }

      // 3. Nếu đơn được auto-add vào shipment IN_TRANSIT -> cảnh báo tối ưu lại tuyến.
      if (requiresReoptimize) {
        message.warning(
          "Đơn đã được thêm vào chuyến đang chạy. Vui lòng tối ưu lại tuyến."
        );
      }

      // 5. Update local state NGAY để UI phản hồi tức thì (kể cả khi success=false vì
      //    có thể backend vẫn đã set PICKING_UP, hoặc status đã chuyển).
      //    Trường hợp success=false thì KHÔNG update local state - để user thấy button "Nhận"
      //    và status cũ để retry.
      if (isSuccess) {
        setList((prev) =>
          prev.map((r) =>
            r.id === rec.id
              ? { ...r, status: data?.status || "PICKING_UP" }
              : r
          )
        );
      }

      // 4. Đồng bộ lại với backend để chắc chắn list đúng.
      //    Sau refresh, đơn có thể đã được filter ra khỏi danh sách pickup-by-courier
      //    (do backend loại đơn PICKING_UP khỏi danh sách "chờ nhận") - đó là behavior đúng.
      try {
        await refreshList(1, pagination.pageSize);
        setPagination((p) => ({ ...p, current: 1 }));
      } catch (refreshErr) {
        console.warn("[ACCEPT_PICKUP_REFRESH_FAILED]", refreshErr);
      }

      // Phát event để các màn hình khác (DeliveryRoute) refresh tuyến nếu cần.
      if (isSuccess) {
        dispatchShipperRouteRefresh();
      }
    } catch (e: any) {
      console.error("[ACCEPT_PICKUP_ERROR]", e);
      const errMsg =
        e?.response?.data?.message ||
        e?.message ||
        "Lỗi khi nhận yêu cầu";
      message.error(errMsg);
    } finally {
      // [DOUBLE-CLICK GUARD] Luôn clear trạng thái đang accept (kể cả khi throw).
      setAcceptingIds((prev) => {
        const next = new Set(prev);
        next.delete(id);
        return next;
      });
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
    setSelectedOrder(order);
    setConfirmPickupImageFile(null);
    setConfirmPickupImagePreview(null);
    setConfirmPickupModalVisible(true);
  }

  const handleConfirmPickupWithImage = async () => {
    if (!selectedOrder) return;
    setConfirmPickupLoading(true);
    try {
      let photoUrl: string | undefined;
      if (confirmPickupImageFile) {
        const uploadRes: any = await orderApi.uploadShipperProofImage(confirmPickupImageFile);
        photoUrl = uploadRes?.data?.imageUrl || (uploadRes as any)?.imageUrl || undefined;
      }
      await orderApi.markShipperPickedUp(selectedOrder.id, { photoUrl });
      message.success("Đã xác nhận đã lấy hàng");
      setConfirmPickupModalVisible(false);
      setMapVisible(false);
      await refreshList();
    } catch (e: any) {
      message.error(e?.response?.data?.message || "Lỗi khi xác nhận đã lấy");
    } finally {
      setConfirmPickupLoading(false);
    }
  };

  const readFilePreview = (file: File): Promise<string> =>
    new Promise((resolve, reject) => {
      const reader = new FileReader();
      reader.onload = () => resolve(reader.result as string);
      reader.onerror = () => reject(reader.error);
      reader.readAsDataURL(file);
    });

  const handleSelectPickupImage = async (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (file) {
      setConfirmPickupImageFile(file);
      setConfirmPickupImagePreview(await readFilePreview(file));
    }
    e.target.value = "";
  };

  const handleRemovePickupImage = () => {
    setConfirmPickupImageFile(null);
    setConfirmPickupImagePreview(null);
  };

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

  async function handleRetryPickup(order: any) {
    try {
      setLoading(true);
      await orderApi.retryPickup(order.id);
      message.success("Đã tiến hành đến lấy lại. Người gửi sẽ được thông báo.");
      setSelectedOrder((prev: any) => prev ? { ...prev, status: "PICKING_UP" } : prev);
      await refreshList();
    } catch (e: any) {
      message.error(e?.response?.data?.message || "Lỗi khi tiến hành lấy lại");
    } finally {
      setLoading(false);
    }
  }

  const STATUS_MAP: Record<string, { label: string; color: string }> = {
    READY_FOR_PICKUP: { label: "Sẵn sàng lấy hàng", color: "blue" },
    URGENT_PICKUP: { label: "Ưu tiên lấy hàng", color: "red" },
    PICKUP_RETRY: { label: "Lấy hàng thất bại - Thử lại", color: "orange" },
    PICKUP_FAILED_FINAL: { label: "Lấy hàng thất bại - Dừng", color: "red" },
    PICKING_UP: { label: "Đang lấy hàng", color: "orange" },
    PICKED_UP: { label: "Đã lấy hàng", color: "orange" },
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
      render: (record: any) => {
        const isAccepting = acceptingIds.has(record.id);
        const canAccept = record.status === "READY_FOR_PICKUP" || record.status === "URGENT_PICKUP";
        return (
          <Space>
            {canAccept && (
              <Button
                type="primary"
                className="primary-button"
                onClick={() => accept(record.id)}
                loading={isAccepting}
                disabled={isAccepting}
              >
                Nhận
              </Button>
            )}
            <Button
              icon={<EyeOutlined />}
              onClick={() => openMapForOrder(record)}
              disabled={isAccepting}
            >
              Chi tiết
            </Button>
          </Space>
        );
      },
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
            title={selectedOrder ? `Chi tiết đơn hàng - ${selectedOrder.trackingNumber || selectedOrder.id}` : "Chi tiết đơn hàng"}
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
                ) : selectedOrder.status === "PICKUP_RETRY" ? (
                  <Space>
                    <Button onClick={() => setMapVisible(false)}>Đóng</Button>
                    <Button
                      type="primary"
                      className="primary-button"
                      onClick={() => selectedOrder && handleRetryPickup(selectedOrder)}
                      loading={loading}
                    >
                      Tiến hành đến lấy lại
                    </Button>
                  </Space>
                ) : selectedOrder.status === "PICKING_UP" ? (
                  <Space>
                    <Button onClick={() => setMapVisible(false)}>Đóng</Button>
                    <Button danger onClick={() => setPickupFailedModalOpen(true)}>
                      Báo lấy hàng thất bại
                    </Button>
                    <Button type="primary" className="primary-button" onClick={() => selectedOrder && markPickedUpFromMap(selectedOrder)}>
                      Xác nhận đã lấy
                    </Button>
                  </Space>
                ) : selectedOrder.status === "URGENT_PICKUP" || selectedOrder.status === "READY_FOR_PICKUP" ? (
                  <Space>
                    <Button onClick={() => setMapVisible(false)}>Đóng</Button>
                    <Button
                      type="primary"
                      className="primary-button"
                      onClick={() => accept(selectedOrder.id)}
                      loading={acceptingIds.has(selectedOrder.id)}
                      disabled={acceptingIds.has(selectedOrder.id)}
                    >
                      Nhận
                    </Button>
                  </Space>
                ) : (
                  <Space>
                    <Button onClick={() => setMapVisible(false)}>Đóng</Button>
                  </Space>
                )
              ) : null
            }
            width={600}
          >
            {selectedOrder && (() => {
              const attempts = selectedOrder.pickupAttempts || [];
              const maxAttempts = selectedOrder.maxPickupAttempts || 0;
              const failedAttempts = attempts.filter((a: any) => a.status === "FAILED").length;
              return (
                <Typography.Text type="secondary" style={{ display: "block" }}>
                  Lần thử {failedAttempts} / {maxAttempts || "-"}
                </Typography.Text>
              );
            })()}
          </Modal>

          <PickupAttemptModal
            open={pickupFailedModalOpen}
            loading={loading}
            onCancel={() => setPickupFailedModalOpen(false)}
            onSubmit={submitPickupFailed}
          />

          {/* Modal: Xác nhận đã lấy hàng với upload ảnh */}
          <Modal
            title="Xác nhận đã lấy hàng"
            open={confirmPickupModalVisible}
            onOk={handleConfirmPickupWithImage}
            onCancel={() => setConfirmPickupModalVisible(false)}
            confirmLoading={confirmPickupLoading}
            width={640}
            okText="Xác nhận"
          >
            <Space direction="vertical" size="large" style={{ width: "100%" }}>
              <Typography.Text type="secondary">
                Bạn đang xác nhận đã lấy hàng cho đơn: <strong>{selectedOrder?.trackingNumber || selectedOrder?.id}</strong>
              </Typography.Text>

              {/* Image picker */}
              <div style={{ marginBottom: 12 }}>
                <Typography.Text strong>Ảnh minh chứng lấy hàng (tuỳ chọn)</Typography.Text>
                <div style={{ marginTop: 8 }}>
                  <input
                    id="confirm-pickup-image-input"
                    type="file"
                    accept="image/*"
                    style={{ display: "none" }}
                    onChange={handleSelectPickupImage}
                  />
                  <Space>
                    <Button
                      icon={<PictureOutlined />}
                      onClick={() => document.getElementById("confirm-pickup-image-input")?.click()}
                    >
                      Chọn ảnh
                    </Button>
                    {confirmPickupImagePreview && (
                      <Button danger onClick={handleRemovePickupImage}>
                        Xóa ảnh
                      </Button>
                    )}
                  </Space>
                </div>
                {confirmPickupImagePreview && (
                  <div style={{ marginTop: 12 }}>
                    <img
                      src={confirmPickupImagePreview}
                      alt="Ảnh minh chứng lấy hàng"
                      style={{ maxWidth: "100%", maxHeight: 220, borderRadius: 8, border: "1px solid #e5e7eb" }}
                    />
                  </div>
                )}
              </div>
            </Space>
          </Modal>
        </div>
      </div>
    </div>
  );
}
