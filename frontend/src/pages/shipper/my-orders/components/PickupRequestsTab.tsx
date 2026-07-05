import React, { forwardRef, useCallback, useEffect, useImperativeHandle, useRef, useState } from "react";
import { Button, Modal, Space, Table, Tag, Typography } from "antd";
import { EyeOutlined, PictureOutlined } from "@ant-design/icons";
import { connectWebSocket, disconnectWebSocket } from "../../../../socket/socket";
import { getUserId } from "../../../../utils/authUtils";
import orderApi from "../../../../api/orderApi";
import { dispatchShipperRouteRefresh } from "../../delivery-route/deliveryRouteEvents";
import PickupAttemptModal from "../../shared/components/PickupAttemptModal";
import type { TabRefreshHandle } from "../MyOrdersPage";

const { Text } = Typography;

interface PickupRequestsTabProps {
  search?: string;
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

const PickupRequestsTab = forwardRef<TabRefreshHandle, PickupRequestsTabProps>(
  ({ search }, ref) => {
    const [list, setList] = useState<any[]>([]);
    const [loading, setLoading] = useState(false);
    const [mapVisible, setMapVisible] = useState(false);
    const [selectedOrder, setSelectedOrder] = useState<any | null>(null);
    const [pickupFailedModalOpen, setPickupFailedModalOpen] = useState(false);
    const [pagination, setPagination] = useState({ current: 1, pageSize: 10, total: 0 });
    const [acceptingIds, setAcceptingIds] = useState<Set<number>>(new Set());
    const [confirmPickupModalVisible, setConfirmPickupModalVisible] = useState(false);
    const [confirmPickupImageFile, setConfirmPickupImageFile] = useState<File | null>(null);
    const [confirmPickupImagePreview, setConfirmPickupImagePreview] = useState<string | null>(null);
    const [confirmPickupLoading, setConfirmPickupLoading] = useState(false);
    const paginationRef = useRef(pagination);
    paginationRef.current = pagination;
    const searchRef = useRef(search);
    searchRef.current = search;
    const mountedRef = useRef(true);

    const fetchList = useCallback(async () => {
      setLoading(true);
      try {
        const res = await orderApi.getShipperPickupByCourierRequests({
          page: paginationRef.current.current,
          limit: paginationRef.current.pageSize,
        });
        if (!mountedRef.current) return;
        const shipperOrders = res.orders || [];
        const q = searchRef.current?.trim().toLowerCase();
        setList(
          shipperOrders.filter((o: any) => {
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
      } finally {
        if (mountedRef.current) setLoading(false);
      }
    }, []);

    useEffect(() => {
      mountedRef.current = true;
      fetchList();
      return () => {
        mountedRef.current = false;
      };
    }, [fetchList]);

    useEffect(() => {
      const uid = getUserId();
      if (!uid) return;
      connectWebSocket(uid, (msg) => {
        if (
          msg.type === "assignment" ||
          msg.type === "shipping_request_accepted" ||
          msg.type === "order_ready_for_pickup"
        ) {
          orderApi
            .getShipperPickupByCourierRequests({ page: 1, limit: paginationRef.current.pageSize })
            .then((res) => {
              const shipperOrders = res.orders || [];
              const q = searchRef.current?.trim().toLowerCase();
              setList(
                shipperOrders.filter((o: any) => {
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

    useImperativeHandle(ref, () => ({
      reload: () => {
        fetchList();
      },
    }));

    const refreshList = async () => {
      const res = await orderApi.getShipperPickupByCourierRequests({
        page: pagination.current,
        limit: pagination.pageSize,
      });
      const shipperOrders = res.orders || [];
      const q = search?.trim().toLowerCase();
      setList(
        shipperOrders.filter((o: any) => {
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

    async function accept(id: number) {
      if (acceptingIds.has(id)) return;
      try {
        const rec: any = list.find((r) => r.id === id);
        if (!rec) return;

        setAcceptingIds((prev) => {
          const next = new Set(prev);
          next.add(id);
          return next;
        });

        const res: any = await orderApi.claimShipperOrderRequest(rec.id);
        const data = res?.data ?? res;
        const isSuccess = res?.success !== false && data?.success !== false;
        const requiresReoptimize = data?.requiresReoptimize === true;

        const msg = data?.message || res?.message || (requiresReoptimize
            ? "Đơn đã được thêm vào chuyến lấy hàng hiện tại."
            : isSuccess
              ? "Nhận yêu cầu lấy hàng thành công."
              : "Không thể nhận yêu cầu lấy hàng");

        if (isSuccess) {
          import("antd").then(({ message }) => message.success(msg));
        } else {
          import("antd").then(({ message }) => message.error(msg));
        }

        if (requiresReoptimize) {
          import("antd").then(({ message }) => message.warning("Đơn đã được thêm vào chuyến đang chạy. Vui lòng tối ưu lại tuyến."));
        }

        if (isSuccess) {
          setList((prev) =>
            prev.map((r) =>
              r.id === rec.id
                ? { ...r, status: data?.status || "PICKING_UP" }
                : r
            )
          );
        }

        try {
          await refreshList();
          setPagination((p) => ({ ...p, current: 1 }));
        } catch (refreshErr) {
          console.warn("[ACCEPT_PICKUP_REFRESH_FAILED]", refreshErr);
        }

        if (isSuccess) {
          dispatchShipperRouteRefresh();
        }
      } catch (e: any) {
        import("antd").then(({ message }) => {
          message.error(e?.response?.data?.message || e?.message || "Lỗi khi nhận yêu cầu");
        });
      } finally {
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
        import("antd").then(({ message }) => message.success("Đã xác nhận đã lấy hàng"));
        setConfirmPickupModalVisible(false);
        setMapVisible(false);
        await refreshList();
      } catch (e: any) {
        import("antd").then(({ message }) => {
          message.error(e?.response?.data?.message || "Lỗi khi xác nhận đã lấy");
        });
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
        import("antd").then(({ message }) => message.success("Đã nộp hàng tại bưu cục"));
        setMapVisible(false);
        await refreshList();
      } catch (e) {
        import("antd").then(({ message }) => message.error("Lỗi khi nộp tại bưu cục"));
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
        import("antd").then(({ message }) => message.success("Đã ghi nhận lấy hàng thất bại"));
        setPickupFailedModalOpen(false);
        await refreshList();
        const detail = await orderApi.getShipperOrderDetail(selectedOrder.id);
        setSelectedOrder(detail || selectedOrder);
      } catch (e: any) {
        import("antd").then(({ message }) => {
          message.error(e?.response?.data?.message || "Lỗi khi báo lấy hàng thất bại");
        });
      } finally {
        setLoading(false);
      }
    }

    async function handleRetryPickup(order: any) {
      try {
        setLoading(true);
        await orderApi.retryPickup(order.id);
        import("antd").then(({ message }) => message.success("Đã tiến hành đến lấy lại. Người gửi sẽ được thông báo."));
        setSelectedOrder((prev: any) => prev ? { ...prev, status: "PICKING_UP" } : prev);
        await refreshList();
      } catch (e: any) {
        import("antd").then(({ message }) => {
          message.error(e?.response?.data?.message || "Lỗi khi tiến hành lấy lại");
        });
      } finally {
        setLoading(false);
      }
    }

    const columns = [
      {
        title: "Mã đơn hàng",
        dataIndex: "trackingNumber",
        key: "trackingNumber",
        width: 140,
        render: (text: string) => (
          <Text strong className="table-strong">{text}</Text>
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
              <Text strong className="table-strong">{record.senderName}</Text>
              <Text className="table-muted">{record.senderPhone}</Text>
              <Text className="table-muted">{address}</Text>
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
              <Text className="table-strong">{serviceName || "—"}</Text>
              <Text className="table-cod">
                {record.cod ? `${record.cod.toLocaleString()}đ` : "COD: 0đ"}
              </Text>
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

    return (
      <div className="my-orders-tab-wrapper">
        <div className="my-orders-results">Kết quả: {list.length} yêu cầu</div>
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
          className="my-orders-table"
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
              <Text type="secondary" style={{ display: "block" }}>
                Lần thử {failedAttempts} / {maxAttempts || "-"}
              </Text>
            );
          })()}
        </Modal>

        <PickupAttemptModal
          open={pickupFailedModalOpen}
          loading={loading}
          onCancel={() => setPickupFailedModalOpen(false)}
          onSubmit={submitPickupFailed}
        />

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
            <Text type="secondary">
              Bạn đang xác nhận đã lấy hàng cho đơn: <strong>{selectedOrder?.trackingNumber || selectedOrder?.id}</strong>
            </Text>

            <div style={{ marginBottom: 12 }}>
              <Text strong>Ảnh minh chứng lấy hàng (tuỳ chọn)</Text>
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
    );
  }
);

PickupRequestsTab.displayName = "PickupRequestsTab";

export default PickupRequestsTab;
