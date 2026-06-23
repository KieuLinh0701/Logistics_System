import React from "react";
import type {AiShipperRoute} from "../../../../types/aiRoute";
import {getRouteColor, getRouteKey, isReturnToOfficeStop} from "../utils/routeMapUtils";

interface RouteLegendProps {
  routes: AiShipperRoute[];
  visibleRouteKeys: Set<string>;
  highlightedRouteKey: string | null;
  onRouteClick?: (routeKey: string) => void;
}

const RouteLegend: React.FC<RouteLegendProps> = ({
  routes,
  visibleRouteKeys,
  highlightedRouteKey,
  onRouteClick,
}) => {
  if (!routes.length) return null;

  return (
    <div className="ai-route-map-legend">
      <div className="ai-route-map-legend-title">Chú thích tuyến</div>
      <ul className="ai-route-map-legend-list">
        {routes.map((route, index) => {
          const routeKey = getRouteKey(route, index);
          const visible = visibleRouteKeys.has(routeKey);
          const color = getRouteColor(index);
          const active = highlightedRouteKey === routeKey;
          return (
            <li key={routeKey}>
              <button
                type="button"
                className={`ai-route-legend-item${active ? " is-active" : ""}${!visible ? " is-hidden" : ""}`}
                onClick={() => onRouteClick?.(routeKey)}
              >
                <span className="ai-route-legend-swatch" style={{ backgroundColor: color }} />
                <span className="ai-route-legend-name">{route.shipperName}</span>
                <span className="ai-route-legend-meta">{route.stops?.filter(s => !isReturnToOfficeStop(s)).length ?? route.stopCount ?? 0} điểm</span>
              </button>
            </li>
          );
        })}
      </ul>
    </div>
  );
};

export default RouteLegend;
