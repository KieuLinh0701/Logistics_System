import React from "react";
import {Empty, Spin, Typography} from "antd";
import {LoadScript} from "@react-google-maps/api";
import type {AiRoutePlanDetail, AiShipperRoute} from "../../../../types/aiRoute";
import GoogleMapRouteRenderer, {type SelectedStopInfo} from "./GoogleMapRouteRenderer";
import RouteLegend from "./RouteLegend";

const MAP_LIBRARIES: ("places")[] = ["places"];

interface RouteMapProps {
  plan: AiRoutePlanDetail | null;
  routes: AiShipperRoute[];
  office: { latitude: number; longitude: number; name: string } | null;
  loading?: boolean;
  visibleRouteKeys: Set<string>;
  highlightedRouteKey: string | null;
  selectedStop: SelectedStopInfo | null;
  fitBoundsVersion: number;
  onStopSelect: (info: SelectedStopInfo | null) => void;
  onHighlightRoute: (routeKey: string) => void;
}

const RouteMap: React.FC<RouteMapProps> = ({
  plan,
  routes,
  office,
  loading,
  visibleRouteKeys,
  highlightedRouteKey,
  selectedStop,
  fitBoundsVersion,
  onStopSelect,
  onHighlightRoute,
}) => {
  const apiKey = import.meta.env.VITE_GOOGLE_MAPS_KEY as string | undefined;

  if (!apiKey) {
    return (
      <div className="ai-route-map-panel ai-route-map-empty">
        <Empty description="Thiếu VITE_GOOGLE_MAPS_KEY trong file .env" />
      </div>
    );
  }

  if (!plan || !routes.length) {
    return (
      <div className="ai-route-map-panel ai-route-map-empty">
        <div className="ai-route-map-empty-card">
          <Empty image={Empty.PRESENTED_IMAGE_SIMPLE} description={false} />
          <Typography.Title level={4} className="ai-route-map-empty-title">
            Chưa có kế hoạch tuyến
          </Typography.Title>
          <Typography.Paragraph type="secondary" className="ai-route-map-empty-desc">
            Chọn đơn hàng sẵn sàng và tạo kế hoạch tuyến để xem bản đồ điều phối giao hàng.
          </Typography.Paragraph>
        </div>
      </div>
    );
  }

  return (
    <div className="ai-route-map-panel">
      <LoadScript googleMapsApiKey={apiKey} libraries={MAP_LIBRARIES} loadingElement={<Spin size="large" />}>
        <div className="ai-route-map-inner">
          {loading && (
            <div className="ai-route-map-loading">
              <Spin size="large" tip="Đang tải bản đồ..." />
            </div>
          )}
          <GoogleMapRouteRenderer
            office={office}
            routes={routes}
            visibleRouteKeys={visibleRouteKeys}
            highlightedRouteKey={highlightedRouteKey}
            selectedStop={selectedStop}
            fitBoundsVersion={fitBoundsVersion}
            onStopSelect={onStopSelect}
            onHighlightRoute={onHighlightRoute}
          />
          <RouteLegend
            routes={routes}
            visibleRouteKeys={visibleRouteKeys}
            highlightedRouteKey={highlightedRouteKey}
            onRouteClick={onHighlightRoute}
          />
        </div>
      </LoadScript>
    </div>
  );
};

export default RouteMap;
