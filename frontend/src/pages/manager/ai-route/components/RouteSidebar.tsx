import React from "react";
import {Badge, Button, Card, Empty, Space, Switch, Tag, Tooltip, Typography} from "antd";
import {AimOutlined, CarOutlined, ClockCircleOutlined, EyeInvisibleOutlined, EyeOutlined,} from "@ant-design/icons";
import type {AiRoutePlanDetail, AiShipperRoute} from "../../../../types/aiRoute";
import RouteSummaryCards from "./RouteSummaryCards";
import {formatCurrency, formatMinutes, getRouteColor, getRouteKey, isReturnToOfficeStop, summarizePlan,} from "../utils/routeMapUtils";

const AI_ROUTE_DEBUG = import.meta.env.DEV;

interface RouteSidebarProps {
  plan: AiRoutePlanDetail | null;
  routes: AiShipperRoute[];
  previewCount: number;
  visibleRouteKeys: Set<string>;
  highlightedRouteKey: string | null;
  onToggleVisibility: (routeKey: string, visible: boolean) => void;
  onHighlightRoute: (routeKey: string) => void;
  onSelectRoute: (routeKey: string) => void;
}

const RouteSidebar: React.FC<RouteSidebarProps> = ({
  plan,
  routes,
  previewCount,
  visibleRouteKeys,
  highlightedRouteKey,
  onToggleVisibility,
  onHighlightRoute,
  onSelectRoute,
}) => {
  const summary = plan
    ? {
        routeCount: routes.length,
        ...summarizePlan(routes),
        totalKm: plan.totalDistanceKm ?? summarizePlan(routes).totalKm,
        totalEta: plan.totalDurationMinutes ?? summarizePlan(routes).totalEta,
        totalFuel: plan.totalFuelCost ?? summarizePlan(routes).totalFuel,
        totalCod: plan.totalCod ?? summarizePlan(routes).totalCod,
      }
    : {
        routeCount: 0,
        totalOrders: previewCount,
        totalKm: 0,
        totalEta: 0,
        totalFuel: 0,
        totalCod: 0,
      };

  const statusColor =
    plan?.status === "CONFIRMED" ? "green" : plan?.status === "DRAFT" ? "blue" : "default";

  const statusLabelMap: Record<string, string> = {
    DRAFT: "Bản nháp",
    CONFIRMED: "Đã xác nhận",
    CANCELLED: "Đã hủy",
  };

  const statusLabel = plan?.status ? statusLabelMap[plan.status] ?? plan.status : "";

  return (
    <aside className="ai-route-sidebar">
      <Card size="small" className="ai-route-sidebar-summary" title="Tổng quan vận hành">
        {plan ? (
          <Space direction="vertical" size={4} style={{ width: "100%", marginBottom: 8 }}>
            <Typography.Text type="secondary" className="ai-route-plan-code">
              {plan.planCode}
            </Typography.Text>
            <Tag color={statusColor}>{statusLabel}</Tag>
            {plan.officeName && (
              <Typography.Text className="ai-route-office-name">{plan.officeName}</Typography.Text>
            )}
          </Space>
        ) : (
          <Typography.Paragraph type="secondary" className="ai-route-sidebar-hint">
            {previewCount} đơn sẵn sàng · Chưa có kế hoạch tuyến
          </Typography.Paragraph>
        )}
        <RouteSummaryCards
          routeCount={summary.routeCount}
          totalOrders={summary.totalOrders}
          totalKm={summary.totalKm}
          totalEtaMinutes={summary.totalEta}
          totalFuel={summary.totalFuel}
          totalCod={summary.totalCod}
          compact
        />
      </Card>

      <div className="ai-route-sidebar-routes-header">
        <Typography.Title level={5} className="ai-route-routes-title">
          Tuyến shipper
        </Typography.Title>
        {plan && <Badge count={routes.length} showZero color="#1C3D90" />}
      </div>

      <div className="ai-route-route-list">
        {!plan || !routes.length ? (
          <Card size="small" className="ai-route-empty-card">
            <Empty image={Empty.PRESENTED_IMAGE_SIMPLE} description="Chưa có tuyến điều phối" />
          </Card>
        ) : (
          routes.map((route, index) => {
            const routeKey = getRouteKey(route, index);
            if (AI_ROUTE_DEBUG) {
              console.group(`[AI_ROUTE_DEBUG] RouteSidebar route[${index}] ${route.shipperName}`);
              console.log("route full:", route);
              console.log("route.stops:", route.stops);
              console.log("route.stops length:", route.stops?.length);
              console.log("route.returnToOfficeStop:", route.returnToOfficeStop);
              console.log("route.stopCount:", route.stopCount);
              console.log(
                "stop types:",
                route.stops?.map((s) => ({
                  id: s.stopId,
                  orderId: s.orderId,
                  trackingNumber: s.trackingNumber,
                  stopType: s.stopType,
                  lat: s.latitude,
                  lng: s.longitude,
                }))
              );
              console.groupEnd();
            }
            const color = getRouteColor(index);
            const visible = visibleRouteKeys.has(routeKey);
            const active = highlightedRouteKey === routeKey;
            const stopCount = route.stops?.filter(s => !isReturnToOfficeStop(s)).length ?? route.stopCount ?? 0;

            return (
              <Card
                key={routeKey}
                size="small"
                className={`ai-route-route-card${active ? " is-active" : ""}`}
                onClick={() => onSelectRoute(routeKey)}
                hoverable
              >
                <div className="ai-route-route-card-head">
                  <span className="ai-route-color-dot" style={{ backgroundColor: color }} />
                  <div className="ai-route-route-card-title">
                    <Typography.Text strong>{route.shipperName}</Typography.Text>
                    <Tag color={statusColor} className="ai-route-route-status">
                      {statusLabel}
                    </Tag>
                  </div>
                </div>

                <div className="ai-route-route-metrics">
                  <span>
                    <CarOutlined /> {stopCount} điểm
                  </span>
                  <span>{Number(route.estimatedDistanceKm || 0).toFixed(2)} km</span>
                  <span>
                    <ClockCircleOutlined /> {formatMinutes(route.estimatedDurationMinutes)}
                  </span>
                </div>

                <div className="ai-route-route-metrics ai-route-route-metrics-secondary">
                  <span>COD: {formatCurrency(route.totalCod)}</span>
                  <span>Xăng: {formatCurrency(route.fuelCost)}</span>
                </div>

                {(route.vehicleType || route.totalWeightKg != null || route.maxWeightKg != null || route.batteryLevel != null) && (
                  <div className="ai-route-route-metrics ai-route-route-metrics-secondary">
                    {route.vehicleType && (
                      <span>
                        Loại xe: {route.vehicleType === "ELECTRIC_BIKE" ? "Xe điện" : route.vehicleType === "MOTORBIKE" ? "Xe máy" : route.vehicleType}
                      </span>
                    )}
                    <span>Số đơn: {stopCount}</span>
                    {(route.totalWeightKg != null || route.maxWeightKg != null) && (
                      <span>
                        Kg: {Number(route.totalWeightKg || 0).toFixed(1)}/{Number(route.maxWeightKg || 0).toFixed(1)}
                      </span>
                    )}
                    {route.batteryLevel != null && <span>Pin: {route.batteryLevel}%</span>}
                  </div>
                )}

                <div
                  className="ai-route-route-card-actions"
                  onClick={(e) => e.stopPropagation()}
                >
                  <Tooltip title={visible ? "Ẩn tuyến" : "Hiện tuyến"}>
                    <Switch
                      size="small"
                      checked={visible}
                      checkedChildren={<EyeOutlined />}
                      unCheckedChildren={<EyeInvisibleOutlined />}
                      onChange={(checked) => onToggleVisibility(routeKey, checked)}
                    />
                  </Tooltip>
                  <Button
                    size="small"
                    type={active ? "primary" : "default"}
                    className={active ? "primary-button" : undefined}
                    icon={<AimOutlined />}
                    onClick={() => onHighlightRoute(routeKey)}
                  >
                    Tập trung
                  </Button>
                </div>
              </Card>
            );
          })
        )}
      </div>

      {plan && plan.unassignedOrders?.length > 0 && (
        <Card size="small" className="ai-route-unassigned-card" title={`Chưa phân (${plan.unassignedOrders.length})`}>
          <ul className="ai-route-unassigned-list">
            {plan.unassignedOrders.slice(0, 5).map((o) => (
              <li key={o.orderId}>
                <Typography.Text>{o.trackingNumber}</Typography.Text>
                <Typography.Text type="secondary"> — {o.reason}</Typography.Text>
              </li>
            ))}
          </ul>
        </Card>
      )}
    </aside>
  );
};

export default RouteSidebar;
