import React, {forwardRef, useCallback, useEffect, useImperativeHandle, useRef, useState} from "react";
import {Alert, Button, Space, Table, Tag, Tooltip, Typography} from "antd";
import {EyeOutlined, StarFilled, WarningOutlined} from "@ant-design/icons";
import {useNavigate} from "react-router-dom";
import {connectWebSocket, disconnectWebSocket} from "../../../../socket/socket";
import {getUserId} from "../../../../utils/authUtils";
import orderApi from "../../../../api/orderApi";
import {dispatchShipperRouteRefresh} from "../../delivery-route/deliveryRouteEvents";
import {getCurrentPositionOnce, type CurrentPosition} from "../../../../utils/geolocation";
import UnassignedRecommendationModal from "../../unassigned/components/UnassignedRecommendationModal";
import type {TabRefreshHandle} from "../MyOrdersPage";

const { Text } = Typography;

type RecommendationLevel =
  | "HIGH"
  | "MEDIUM"
  | "LOW"
  | "NOT_RECOMMENDED"
  | "OVER_CAPACITY";

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
  RETURN_READY_FOR_PICKUP: { label: "Sẵn sàng lấy hàng hoàn", color: "gold" },
  RETURN_PICKED_UP: { label: "Đã lấy hàng hoàn lên xe", color: "cyan" },
};

const levelLabel = (level?: RecommendationLevel | string): string => {
  switch (level) {
    case "HIGH": return "Rất phù hợp";
    case "MEDIUM": return "Phù hợp";
    case "LOW": return "Có thể nhận";
    case "NOT_RECOMMENDED": return "Ít phù hợp";
    case "OVER_CAPACITY": return "Vượt tải";
    default: return "—";
  }
};

const levelColor = (level?: RecommendationLevel | string): string => {
  switch (level) {
    case "HIGH": return "success";
    case "MEDIUM": return "processing";
    case "LOW": return "warning";
    case "OVER_CAPACITY": return "error";
    case "NOT_RECOMMENDED":
    default: return "default";
  }
};

const recommendationBadgeLabel = (
  score: number,
  level?: RecommendationLevel | string,
): string => {
  if (level === "OVER_CAPACITY") return "Vượt tải";
  return `${levelLabel(level)} ${score}%`;
};

const PickupRequestsTab = forwardRef<TabRefreshHandle, PickupRequestsTabProps>(
  ({ search }, ref) => {
    const navigate = useNavigate();
    const [list, setList] = useState<any[]>([]);
    const [loading, setLoading] = useState(false);
    const [pagination, setPagination] = useState({ current: 1, pageSize: 10, total: 0 });
    const [acceptingIds, setAcceptingIds] = useState<Set<number>>(new Set());
    const [gps, setGps] = useState<CurrentPosition | null>(null);
    const [locationSource, setLocationSource] = useState<string | null>(null);
    const [recommendationError, setRecommendationError] = useState<string | null>(null);
    const [modalOrder, setModalOrder] = useState<any | null>(null);
    const paginationRef = useRef(pagination);
    paginationRef.current = pagination;
    const searchRef = useRef(search);
    searchRef.current = search;
    const mountedRef = useRef(true);

    const fetchList = useCallback(async () => {
      setLoading(true);
      try {
        // Lấy GPS mỗi lần fetch (ưu tiên vị trí thực); backend tự fallback nếu null.
        const position = await getCurrentPositionOnce(4000);
        if (!mountedRef.current) return;
        setGps(position);

        const res = await orderApi.getShipperPickupByCourierRequests({
          page: paginationRef.current.current,
          limit: paginationRef.current.pageSize,
          latitude: position?.latitude ?? null,
          longitude: position?.longitude ?? null,
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
        setLocationSource(res.recommendationLocationSource ?? null);
        setRecommendationError(null);
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
          // Bỏ cache cũ vì đơn có thể đã được shipper khác nhận.
          orderApi
            .getShipperPickupByCourierRequests({
              page: 1,
              limit: paginationRef.current.pageSize,
              latitude: null,
              longitude: null,
            })
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
      const position = gps;
      const res = await orderApi.getShipperPickupByCourierRequests({
        page: pagination.current,
        limit: pagination.pageSize,
        latitude: position?.latitude ?? null,
        longitude: position?.longitude ?? null,
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
      setLocationSource(res.recommendationLocationSource ?? locationSource);
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

    const locationChip = (() => {
      if (gps) {
        return (
          <div className="list-page-tag" style={{marginLeft: 8}}>
            Vị trí: GPS ({gps.latitude.toFixed(4)}, {gps.longitude.toFixed(4)})
          </div>
        );
      }
      if (locationSource && locationSource !== "NONE") {
        const label = locationSource === "OFFICE"
          ? "Vị trí: Bưu cục"
          : locationSource === "ROUTE_LAST_POINT"
            ? "Vị trí: Điểm dừng gần nhất"
            : `Vị trí: ${locationSource}`;
        return (
          <div className="list-page-tag" style={{marginLeft: 8}}>{label}</div>
        );
      }
      return null;
    })();

    const columns = [
      {
        title: "Mã đơn hàng",
        dataIndex: "trackingNumber",
        key: "trackingNumber",
        width: 150,
        render: (text: string) => (
          <span className="tracking-number-cell table-strong">{text}</span>
        ),
      },
      {
        title: "Thông tin người gửi",
        key: "sender",
        render: (record: any) => {
          // Ưu tiên displayContact* nếu backend trả về (theo destinationType).
          const name = record.displayContactName || record.senderName;
          const phone = record.displayContactPhone || record.senderPhone;
          const address =
            record.displayContactAddress ||
            (typeof record.senderAddress === "string"
              ? record.senderAddress
              : (record.senderAddress as any)?.fullAddress) ||
            "";
          const contactType = record.displayContactType || "Người gửi";
          return (
            <Space direction="vertical" size={2}>
              <Text strong className="table-strong">{name}</Text>
              <Text className="table-muted" style={{fontSize: 11}}>{contactType}</Text>
              <Text className="table-muted">{phone}</Text>
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
        width: 130,
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
        title: "Mức phù hợp",
        key: "recommendation",
        width: 200,
        render: (record: any) => {
          const score = record.recommendationScore;
          const level = record.recommendationLevel as RecommendationLevel | undefined;
          const reasons: string[] | undefined = record.recommendationReasons;

          if (
            score == null ||
            level == null ||
            !Array.isArray(reasons) ||
            reasons.length === 0
          ) {
            return (
              <Typography.Text type="secondary" className="table-muted">
                —
              </Typography.Text>
            );
          }

          const isOverCapacity = level === "OVER_CAPACITY";
          const tooltipTitle = isOverCapacity
            ? "Đơn vượt tải trọng còn lại của phương tiện — bấm để xem chi tiết"
            : `Mức phù hợp ${score}% (${levelLabel(level)}) — bấm để xem chi tiết`;

          return (
            <Tooltip title={tooltipTitle}>
              <Tag
                color={levelColor(level)}
                icon={isOverCapacity ? <WarningOutlined /> : <StarFilled />}
                onClick={(e) => {
                  e.stopPropagation();
                  setModalOrder(record);
                }}
                onKeyDown={(e) => {
                  if (e.key === "Enter" || e.key === " ") {
                    e.preventDefault();
                    setModalOrder(record);
                  }
                }}
                role="button"
                tabIndex={0}
                aria-label={`Xem chi tiết mức phù hợp ${score}%`}
                style={{cursor: "pointer", fontWeight: 600, margin: 0, userSelect: "none"}}
              >
                {recommendationBadgeLabel(score, level)}
              </Tag>
            </Tooltip>
          );
        },
      },
      {
        title: "Thao tác",
        key: "action",
        width: 200,
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
        <div style={{display: "flex", alignItems: "center", marginBottom: 12}}>
          <div className="my-orders-results">Kết quả: {list.length} yêu cầu</div>
          {locationChip}
        </div>
        {recommendationError ? (
          <Alert
            type="warning"
            showIcon
            style={{marginBottom: 12}}
            message="Hệ thống đánh giá tạm thời không khả dụng"
            description="Danh sách đơn vẫn hiển thị bình thường. Vui lòng bấm 'Làm mới' để thử lại sau."
          />
        ) : null}
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
          scroll={{ x: 1080 }}
          className="my-orders-table"
        />
        <UnassignedRecommendationModal
          open={modalOrder !== null}
          order={modalOrder}
          onClose={() => setModalOrder(null)}
        />
      </div>
    );
  }
);

PickupRequestsTab.displayName = "PickupRequestsTab";

export default PickupRequestsTab;