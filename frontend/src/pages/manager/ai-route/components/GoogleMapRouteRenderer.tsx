import React, {useCallback, useEffect, useRef} from "react";
import {GoogleMap, InfoWindowF, Marker as InfoWindowMarker} from "@react-google-maps/api";
import type {AiRouteStop, AiShipperRoute} from "../../../../types/aiRoute";
import RouteStopMarker from "./RouteStopMarker";
import {
    buildLatLngBounds,
    collectMapPoints,
    createDepotMarkerSvg,
    decodeEncodedPolyline,
    getRouteColor,
    getRouteKey,
    getStopLabel,
    getStopTypeLabel,
    getStopTypeBadge,
    isValidStop,
} from "../utils/routeMapUtils";

export interface SelectedStopInfo {
  routeKey: string;
  routeIndex: number;
  shipperName: string;
  stop: AiRouteStop;
}

interface GoogleMapRouteRendererProps {
  office: { latitude: number; longitude: number; name: string } | null;
  routes: AiShipperRoute[];
  visibleRouteKeys: Set<string>;
  highlightedRouteKey: string | null;
  selectedStop: SelectedStopInfo | null;
  fitBoundsVersion: number;
  onMapLoad?: (map: google.maps.Map) => void;
  onStopSelect: (info: SelectedStopInfo | null) => void;
  onHighlightRoute: (routeKey: string) => void;
}

const DEFAULT_CENTER = { lat: 10.8231, lng: 106.6297 };

const mapOptions: google.maps.MapOptions = {
  disableDefaultUI: false,
  zoomControl: true,
  mapTypeControl: false,
  streetViewControl: false,
  fullscreenControl: true,
  gestureHandling: "greedy",
};

const GoogleMapRouteRenderer: React.FC<GoogleMapRouteRendererProps> = ({
  office,
  routes,
  visibleRouteKeys,
  highlightedRouteKey,
  selectedStop,
  fitBoundsVersion,
  onMapLoad,
  onStopSelect,
}) => {
  const mapRef = useRef<google.maps.Map | null>(null);
  const routePolylinesRef = useRef<google.maps.Polyline[]>([]);
  const depotMarkerRef = useRef<google.maps.Marker | null>(null);
  const [mapReadyTick, setMapReadyTick] = React.useState(0);

  const handleLoad = useCallback(
    (map: google.maps.Map) => {
      mapRef.current = map;
      onMapLoad?.(map);

      window.setTimeout(() => {
        setMapReadyTick((v) => v + 1);
      }, 300);
    },
    [onMapLoad]
  );

  useEffect(() => {
    const map = mapRef.current;
    if (!map) return;
    const points = collectMapPoints(routes, office, visibleRouteKeys);
    const bounds = buildLatLngBounds(points);
    if (!bounds) return;
    map.fitBounds(bounds, 56);
    const listener = google.maps.event.addListenerOnce(map, "bounds_changed", () => {
      const zoom = map.getZoom();
      if (zoom != null && zoom > 16) map.setZoom(16);
    });
    return () => {
      google.maps.event.removeListener(listener);
    };
  }, [routes, office, visibleRouteKeys, fitBoundsVersion, mapReadyTick]);

  useEffect(() => {
    const map = mapRef.current;
    if (!map || !highlightedRouteKey) return;
    const routeIndex = routes.findIndex((r, i) => getRouteKey(r, i) === highlightedRouteKey);
    if (routeIndex < 0) return;
    const route = routes[routeIndex];
    const points = [
      ...(office ? [{ lat: office.latitude, lng: office.longitude }] : []),
      ...decodeEncodedPolyline(route.encodedPolyline),
      ...(route.stops || [])
        .filter(isValidStop)
        .map((s) => ({ lat: s.latitude as number, lng: s.longitude as number })),
    ];
    const bounds = buildLatLngBounds(points);
    if (bounds) map.fitBounds(bounds, 72);
  }, [highlightedRouteKey, routes, office]);

  useEffect(() => {
    const map = mapRef.current;
    if (!map) return;

    routePolylinesRef.current.forEach((polyline) => {
      polyline.setMap(null);
    });
    routePolylinesRef.current = [];

    routes.forEach((route, routeIndex) => {
      const routeKey = getRouteKey(route, routeIndex);

      if (!visibleRouteKeys.has(routeKey)) {
        return;
      }

        const decodedPath = decodeEncodedPolyline(route.encodedPolyline);

        if (decodedPath.length <= 1) return;

        const color = getRouteColor(routeIndex);
        const highlighted = highlightedRouteKey === routeKey;
        const dimmed = highlightedRouteKey != null && !highlighted;

        const polyline = new google.maps.Polyline({
          path: decodedPath,
          map,
          strokeColor: color,
          strokeOpacity: dimmed ? 0.28 : 0.92,
          strokeWeight: highlighted ? 7 : 5,
          geodesic: true,
          zIndex: highlighted ? 300 : 100 + routeIndex,
        });

      routePolylinesRef.current.push(polyline);
    });

    return () => {
      routePolylinesRef.current.forEach((polyline) => {
        polyline.setMap(null);
      });
      routePolylinesRef.current = [];
    };
  }, [routes, visibleRouteKeys, highlightedRouteKey, mapReadyTick]);

  useEffect(() => {
    const map = mapRef.current;
    if (!map) return;

    if (depotMarkerRef.current) {
      depotMarkerRef.current.setMap(null);
      depotMarkerRef.current = null;
    }

    if (!office || office.latitude == null || office.longitude == null) return;

    const marker = new google.maps.Marker({
      position: {
        lat: Number(office.latitude),
        lng: Number(office.longitude),
      },
      map,
      title: `Bưu cục: ${office.name}`,
      zIndex: 1000,
      optimized: false,
      icon: {
        url: createDepotMarkerSvg(),
        scaledSize: new google.maps.Size(32, 42),
        anchor: new google.maps.Point(16, 42),
      },
    });

    depotMarkerRef.current = marker;

    return () => {
      marker.setMap(null);
      if (depotMarkerRef.current === marker) {
        depotMarkerRef.current = null;
      }
    };
  }, [office?.latitude, office?.longitude, office?.name, mapReadyTick]);

  useEffect(() => {
    if (!mapRef.current) return;
    if (!routes || routes.length === 0) return;

    const timer = window.setTimeout(() => {
      setMapReadyTick((v) => v + 1);
    }, 150);

    return () => {
      window.clearTimeout(timer);
    };
  }, [routes, visibleRouteKeys]);

  const center = office
    ? { lat: office.latitude, lng: office.longitude }
    : routes[0]?.stops?.[0]?.latitude != null
      ? { lat: routes[0].stops[0].latitude as number, lng: routes[0].stops[0].longitude as number }
      : DEFAULT_CENTER;

  return (
    <GoogleMap
      mapContainerClassName="ai-route-google-map"
      center={center}
      zoom={12}
      options={mapOptions}
      onLoad={handleLoad}
    >
      {routes.map((route, routeIndex) => {
        const routeKey = getRouteKey(route, routeIndex);
        if (!visibleRouteKeys.has(routeKey)) {
          return null;
        }
        const color = getRouteColor(routeIndex);
        const highlighted = highlightedRouteKey === routeKey;

        return (
          <React.Fragment key={`${routeKey}-${mapReadyTick}`}>
            {(route.stops || []).filter(isValidStop).map((stop) => {
              const label = getStopLabel(routeIndex, stop.stopSequence);
              const selected =
                selectedStop?.routeKey === routeKey && selectedStop.stop.orderId === stop.orderId;
              return (
                <RouteStopMarker
                  key={`${routeKey}-${stop.orderId}-${mapReadyTick}`}
                  stop={stop}
                  label={label}
                  color={color}
                  highlighted={highlighted}
                  selected={selected}
                  onClick={() =>
                    onStopSelect({
                      routeKey,
                      routeIndex,
                      shipperName: route.shipperName,
                      stop,
                    })
                  }
                />
              );
            })}
          </React.Fragment>
        );
      })}

      {selectedStop && isValidStop(selectedStop.stop) && (
        <InfoWindowF
          position={{
            lat: selectedStop.stop.latitude as number,
            lng: selectedStop.stop.longitude as number,
          }}
          onCloseClick={() => onStopSelect(null)}
        >
          <div className="ai-route-info-window">
            <div className="ai-route-info-title">{selectedStop.stop.trackingNumber || "Quay về bưu cục"}</div>
            {(() => {
              const badge = getStopTypeBadge(selectedStop.stop.stopType);
              return (
                <span style={{
                  display: "inline-block",
                  padding: "2px 8px",
                  borderRadius: 4,
                  backgroundColor: badge.bg,
                  color: badge.color,
                  fontSize: 11,
                  fontWeight: 600,
                  marginBottom: 8,
                }}>
                  {getStopTypeLabel(selectedStop.stop.stopType)}
                </span>
              );
            })()}
            <p>
              <strong>Người nhận:</strong> {selectedStop.stop.recipientName || "—"}
            </p>
            <p>
              <strong>Địa chỉ:</strong> {selectedStop.stop.recipientAddress || "—"}
            </p>
            <p>
              <strong>Thứ tự:</strong> {getStopLabel(selectedStop.routeIndex, selectedStop.stop.stopSequence)}
            </p>
            <p>
              <strong>Shipper:</strong> {selectedStop.shipperName}
            </p>
            <p>
              <strong>ETA:</strong> {selectedStop.stop.etaTime || "—"}
            </p>
            {selectedStop.stop.stopType !== "RETURN_TO_OFFICE" && (
              <p>
                <strong>COD:</strong> {(selectedStop.stop.codAmount ?? 0).toLocaleString("vi-VN")}đ
              </p>
            )}
            {selectedStop.stop.stopType === "RETURN_TO_OFFICE" && (
              <p style={{color: "#E53935", fontStyle: "italic"}}>
                Quay về bưu cục để nộp COD / bàn giao ca
              </p>
            )}
          </div>
        </InfoWindowF>
      )}

    </GoogleMap>
  );
};

export default GoogleMapRouteRenderer;
