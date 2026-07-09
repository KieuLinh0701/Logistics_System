import React, {forwardRef, useCallback, useEffect, useImperativeHandle, useRef, useState} from "react";
import {Button, Space, Table, Tag, Typography} from "antd";
import {EyeOutlined} from "@ant-design/icons";
import {useNavigate} from "react-router-dom";
import {connectWebSocket, disconnectWebSocket} from "../../../../socket/socket";
import {getUserId} from "../../../../utils/authUtils";
import orderApi from "../../../../api/orderApi";
import {dispatchShipperRouteRefresh} from "../../delivery-route/deliveryRouteEvents";
import type {TabRefreshHandle} from "../MyOrdersPage";

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
    const navigate = useNavigate();
    const [list, setList] = useState<any[]>([]);
    const [loading, setLoading] = useState(false);
    const [pagination, setPagination] = useState({ current: 1, pageSize: 10, total: 0 });
    const [acceptingIds, setAcceptingIds] = useState<Set<number>>(new Set());
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
          dispatchShipperRouteRefresh();
        }

        await refreshList();
        setPagination((p) => ({ ...p, current: 1 }));
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

    const columns = [
      {
        title: "Mã đơn hàng",
        dataIndex: "trackingNumber",
        key: "trackingNumber",
        width: 160,
        minWidth: 160,
        render: (text: string) => (
          <span className="tracking-number-cell table-strong">{text}</span>
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
                onClick={() => navigate(`/shipper/orders/${record.id}`, { state: { from: "/shipper/my-orders?tab=pickup" } })}
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
      </div>
    );
  }
);

PickupRequestsTab.displayName = "PickupRequestsTab";

export default PickupRequestsTab;
