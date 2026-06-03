import React, { useCallback, useEffect, useMemo, useState } from "react";
import {
  Alert,
  Button,
  Drawer,
  Table,
  Tag,
  Typography,
  message,
} from "antd";
import {
  ThunderboltOutlined,
  ReloadOutlined,
  CheckOutlined,
  CloseOutlined,
  NodeIndexOutlined,
  HistoryOutlined,
} from "@ant-design/icons";
import aiRouteApi from "../../../api/aiRouteApi";
import officeApi from "../../../api/officeApi";
import type { AiRoutePlanDetail, AiRoutePlanSummary } from "../../../types/aiRoute";
import type { ApiResponse } from "../../../types/response";
import type { Office } from "../../../types/office";
import type { SelectedStopInfo } from "./components/GoogleMapRouteRenderer";
import RouteMap from "./components/RouteMap";
import RouteSidebar from "./components/RouteSidebar";
import { getRouteKey } from "./utils/routeMapUtils";
import "../../../styles/ListPage.css";
import "../../shipper/ShipperPagesShared.css";
import "./ManagerAiRouteOptimization.css";

const ManagerAiRouteOptimization: React.FC = () => {
  const [loading, setLoading] = useState(false);
  const [previewCount, setPreviewCount] = useState(0);
  const [aiHealthy, setAiHealthy] = useState<boolean | null>(null);
  const [currentPlan, setCurrentPlan] = useState<AiRoutePlanDetail | null>(null);
  const [plans, setPlans] = useState<AiRoutePlanSummary[]>([]);
  const [office, setOffice] = useState<Office | null>(null);
  const [historyOpen, setHistoryOpen] = useState(false);

  const [visibleRouteKeys, setVisibleRouteKeys] = useState<Set<string>>(new Set());
  const [highlightedRouteKey, setHighlightedRouteKey] = useState<string | null>(null);
  const [selectedStop, setSelectedStop] = useState<SelectedStopInfo | null>(null);
  const [fitBoundsVersion, setFitBoundsVersion] = useState(0);

  const routes = useMemo(() => currentPlan?.routes ?? [], [currentPlan]);

  const syncVisibleRoutes = useCallback((planRoutes: typeof routes) => {
    const keys = new Set(planRoutes.map((r, i) => getRouteKey(r, i)));
    setVisibleRouteKeys(keys);

    setHighlightedRouteKey(null);

    setSelectedStop(null);
    setFitBoundsVersion((v) => v + 1);
  }, []);

  useEffect(() => {
    if (currentPlan?.routes) {
      syncVisibleRoutes(currentPlan.routes);
    }
  }, [currentPlan?.id, syncVisibleRoutes]);

  const loadOffice = async () => {
    try {
      const res = await officeApi.getManagerOffice();
      const officeData = (res as ApiResponse<Office>)?.data ?? null;
      setOffice(officeData);
    } catch {
      /* office optional for map center */
    }
  };

  const loadPreview = async () => {
    try {
      const data = await aiRouteApi.preview();
      setPreviewCount(data.orderCount || 0);
      setAiHealthy(data.aiServiceHealthy);
    } catch {
      message.error("Không tải được dữ liệu xem trước");
    }
  };

  const loadPlans = async () => {
    try {
      const list = await aiRouteApi.listPlans();
      setPlans(list || []);
    } catch {
      message.error("Không tải được danh sách kế hoạch");
    }
  };

  useEffect(() => {
    loadPreview();
    loadPlans();
    loadOffice();
  }, []);

  const handleOptimize = async () => {
    try {
      setLoading(true);
      const plan = await aiRouteApi.optimize();
      setCurrentPlan(plan);
      message.success("Đã tạo đề xuất tuyến giao hàng");
      loadPlans();
      loadPreview();
    } catch (e: unknown) {
      const err = e as { message?: string };
      message.error(err?.message || "Tạo kế hoạch thất bại");
    } finally {
      setLoading(false);
    }
  };

  const handleConfirm = async () => {
    if (!currentPlan?.id) return;
    try {
      setLoading(true);
      const plan = await aiRouteApi.confirmPlan(currentPlan.id);
      setCurrentPlan(plan);
      message.success("Đã xác nhận tuyến và gán đơn cho shipper");
      loadPlans();
      loadPreview();
    } catch (e: unknown) {
      const err = e as { message?: string };
      message.error(err?.message || "Xác nhận thất bại");
    } finally {
      setLoading(false);
    }
  };

  const handleCancel = async () => {
    if (!currentPlan?.id) return;
    try {
      setLoading(true);
      await aiRouteApi.cancelPlan(currentPlan.id);
      setCurrentPlan(null);
      setVisibleRouteKeys(new Set());
      setHighlightedRouteKey(null);
      setSelectedStop(null);
      message.success("Đã hủy kế hoạch");
      loadPlans();
    } catch {
      message.error("Hủy kế hoạch thất bại");
    } finally {
      setLoading(false);
    }
  };

  const loadPlanDetail = async (planId: number) => {
    try {
      setLoading(true);
      const plan = await aiRouteApi.getPlan(planId);
      setCurrentPlan(plan);
      syncVisibleRoutes(plan.routes || []);
      setHistoryOpen(false);
    } catch {
      message.error("Không tải được chi tiết kế hoạch");
    } finally {
      setLoading(false);
    }
  };

  const handleToggleVisibility = (routeKey: string, visible: boolean) => {
    setVisibleRouteKeys((prev) => {
      const next = new Set(prev);
      if (visible) next.add(routeKey);
      else next.delete(routeKey);
      return next;
    });
    setFitBoundsVersion((v) => v + 1);
  };

  const handleSelectRoute = (routeKey: string) => {
    setHighlightedRouteKey(routeKey);
    if (!visibleRouteKeys.has(routeKey)) {
      setVisibleRouteKeys((prev) => new Set(prev).add(routeKey));
    }
  };

  const handleHighlightRoute = (routeKey: string) => {
    setHighlightedRouteKey((prev) =>
      prev === routeKey ? null : routeKey
    );

    if (!visibleRouteKeys.has(routeKey)) {
      setVisibleRouteKeys((prev) => new Set(prev).add(routeKey));
    }
  };

  const officePoint = office
    ? { latitude: office.latitude, longitude: office.longitude, name: office.name }
    : null;

  const isDraft = currentPlan?.status === "DRAFT";

  const statusLabelMap: Record<string, string> = {
    DRAFT: "Bản nháp",
    CONFIRMED: "Đã xác nhận",
    CANCELLED: "Đã hủy",
  };

  const planColumns = [
    { title: "Mã kế hoạch", dataIndex: "planCode", key: "planCode" },
    {
      title: "Trạng thái",
      dataIndex: "status",
      key: "status",
      render: (s: string) => (
        <Tag color={s === "CONFIRMED" ? "green" : s === "DRAFT" ? "blue" : "default"}>
          {statusLabelMap[s] ?? s}
        </Tag>
      ),
    },
    { title: "Số tuyến", dataIndex: "routeCount", key: "routeCount" },
    {
      title: "Tổng km",
      dataIndex: "totalDistanceKm",
      key: "totalDistanceKm",
      render: (v: number) => (v != null ? `${Number(v).toFixed(2)} km` : "—"),
    },
    {
      title: "ETA",
      dataIndex: "totalDurationMinutes",
      key: "totalDurationMinutes",
      render: (v: number) => (v != null ? `${Math.round(v)} phút` : "—"),
    },
    {
      title: "Thao tác",
      key: "action",
      render: (_: unknown, row: AiRoutePlanSummary) => (
        <Button type="link" onClick={() => loadPlanDetail(row.id)}>
          Mở trên bản đồ
        </Button>
      ),
    },
  ];

  return (
    <div className="list-page-layout manager-ai-route-page">
      <div className="list-page-content">
        <div className="shipper-filter-panel manager-ai-filter-panel manager-ai-toolbar">
          <div className="manager-ai-header">
            <div className="manager-ai-header-left">
              <div className="manager-ai-title-row">
                <h3 className="list-page-title-main manager-ai-title">
                  <NodeIndexOutlined style={{ marginRight: 8 }} />
                  Tối ưu tuyến giao hàng
                </h3>
              </div>
              <Typography.Paragraph type="secondary" className="manager-ai-subtitle">
                Trung tâm điều phối và tối ưu lộ trình giao hàng
              </Typography.Paragraph>
            </div>
            <div className="manager-ai-header-actions">
              <div className="manager-ai-action-buttons">
                <Typography.Text className="manager-ai-ready-count" role="status">
                  Đơn sẵn sàng: {previewCount}
                  {aiHealthy === false && " · Dịch vụ tối ưu offline"}
                </Typography.Text>
                <Button icon={<ReloadOutlined />} onClick={() => { loadPreview(); loadPlans(); }}>
                  Làm mới
                </Button>
                <Button icon={<HistoryOutlined />} onClick={() => setHistoryOpen(true)}>
                  Lịch sử
                </Button>
                <Button
                  type="primary"
                  className="primary-button"
                  icon={<ThunderboltOutlined />}
                  loading={loading}
                  onClick={handleOptimize}
                >
                  Tạo kế hoạch tuyến
                </Button>
              </div>
              {isDraft && (
                <div className="manager-ai-draft-actions">
                  <Button
                    type="primary"
                    className="success-button"
                    icon={<CheckOutlined />}
                    loading={loading}
                    onClick={handleConfirm}
                  >
                    Xác nhận kế hoạch
                  </Button>
                  <Button danger icon={<CloseOutlined />} loading={loading} onClick={handleCancel}>
                    Hủy kế hoạch
                  </Button>
                </div>
              )}
            </div>
          </div>
        </div>

        {aiHealthy === false && (
          <Alert
            type="warning"
            showIcon
            className="manager-ai-alert"
            message="Dịch vụ tối ưu tuyến chưa chạy. Khởi động: uvicorn app.main:app --port 8001 trong thư mục ai-service"
          />
        )}

        <div className="manager-ai-dispatch-board">
          <RouteSidebar
            plan={currentPlan}
            routes={routes}
            previewCount={previewCount}
            visibleRouteKeys={visibleRouteKeys}
            highlightedRouteKey={highlightedRouteKey}
            onToggleVisibility={handleToggleVisibility}
            onHighlightRoute={handleHighlightRoute}
            onSelectRoute={handleSelectRoute}
          />
          <RouteMap
            plan={currentPlan}
            routes={routes}
            office={officePoint}
            loading={loading}
            visibleRouteKeys={visibleRouteKeys}
            highlightedRouteKey={highlightedRouteKey}
            selectedStop={selectedStop}
            fitBoundsVersion={fitBoundsVersion}
            onStopSelect={setSelectedStop}
            onHighlightRoute={handleHighlightRoute}
          />
        </div>
      </div>

      <Drawer
        title="Lịch sử kế hoạch tuyến"
        placement="right"
        width={580}
        open={historyOpen}
        onClose={() => setHistoryOpen(false)}
      >
        <Table
          rowKey="id"
          loading={loading}
          columns={planColumns}
          dataSource={plans}
          pagination={{ pageSize: 10 }}
          size="small"
        />
      </Drawer>
    </div>
  );
};

export default ManagerAiRouteOptimization;
