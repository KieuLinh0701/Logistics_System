import {decode} from "@googlemaps/polyline-codec";
import type {AiRouteStop, AiShipperRoute, RouteStopType} from "../../../../types/aiRoute";

export const ROUTE_COLORS = [
  "#1C3D90",
  "#cf1322",
  "#722ed1",
  "#389e0d",
  "#fa8c16",
  "#13c2c2",
  "#eb2f96",
  "#2f54eb",
];

export function getRouteKey(route: AiShipperRoute, index: number): string {
  return String(route.routeId ?? route.shipperEmployeeId ?? index);
}

export function getRouteColor(index: number): string {
  return ROUTE_COLORS[index % ROUTE_COLORS.length];
}

export function decodeEncodedPolyline(encoded?: string | null): google.maps.LatLngLiteral[] {
  if (!encoded) return [];
  try {
    return decode(encoded).map(([lat, lng]) => ({ lat, lng }));
  } catch {
    return [];
  }
}

/**
 * Returns stop label for map markers.
 * Format: "{routeIndex+1}.{stopSequence}" e.g. "1.1", "1.2", "2.1", "2.3"
 */
export function getStopLabel(routeIndex: number, stopSequence: number): string {
  return `${routeIndex + 1}.${stopSequence}`;
}

export function formatMinutes(minutes?: number): string {
  if (minutes == null || Number.isNaN(minutes)) return "—";
  const h = Math.floor(minutes / 60);
  const m = Math.round(minutes % 60);
  if (h <= 0) return `${m} phút`;
  return `${h}h ${m}p`;
}

export function formatCurrency(amount?: number): string {
  return `${(amount ?? 0).toLocaleString("vi-VN")}đ`;
}

export function collectMapPoints(
  routes: AiShipperRoute[],
  office?: { latitude: number; longitude: number } | null,
  visibleKeys?: Set<string>
): google.maps.LatLngLiteral[] {
  const points: google.maps.LatLngLiteral[] = [];
  if (office?.latitude != null && office?.longitude != null) {
    points.push({ lat: office.latitude, lng: office.longitude });
  }
  routes.forEach((route, index) => {
    const key = getRouteKey(route, index);
    if (visibleKeys && !visibleKeys.has(key)) return;
    decodeEncodedPolyline(route.encodedPolyline).forEach((p) => points.push(p));
    (route.stops || []).forEach((stop) => {
      if (stop.latitude != null && stop.longitude != null) {
        points.push({ lat: stop.latitude, lng: stop.longitude });
      }
    });
  });
  return points;
}

export function buildLatLngBounds(
  points: google.maps.LatLngLiteral[]
): google.maps.LatLngBounds | null {
  if (!points.length || typeof google === "undefined") return null;
  const bounds = new google.maps.LatLngBounds();
  points.forEach((p) => bounds.extend(p));
  return bounds;
}

export function summarizePlan(routes: AiShipperRoute[]) {
  const AVG_SPEED_KMH = 25;
  return routes.reduce(
    (acc, route) => {
      const distanceKm = route.estimatedDistanceKm ?? 0;
      const etaMinutes = route.estimatedDurationMinutes ?? 0;
      acc.totalOrders += route.stops?.filter(s => !isReturnToOfficeStop(s)).length ?? route.stopCount ?? 0;
      acc.totalKm += distanceKm;
      acc.totalEta += etaMinutes > 0 ? etaMinutes : Math.ceil(distanceKm / AVG_SPEED_KMH * 60);
      acc.totalFuel += route.fuelCost ?? 0;
      acc.totalCod += route.totalCod ?? 0;
      return acc;
    },
    { totalOrders: 0, totalKm: 0, totalEta: 0, totalFuel: 0, totalCod: 0 }
  );
}

export function isValidStop(stop: AiRouteStop): boolean {
  return stop.latitude != null && stop.longitude != null;
}

export function isDeliveryStop(_stop: AiRouteStop): boolean {
  return _stop.stopType === undefined || _stop.stopType === "DELIVERY" || _stop.stopType === null;
}

export function isPickupStop(stop: AiRouteStop): boolean {
  return stop.stopType === "PICKUP";
}

export function isReturnToOfficeStop(stop: AiRouteStop): boolean {
  return stop.stopType === "RETURN_TO_OFFICE";
}

export function getStopTypeLabel(stopType?: RouteStopType): string {
  switch (stopType) {
    case "DELIVERY": return "Giao hàng";
    case "PICKUP": return "Lấy hàng";
    case "RETURN_TO_OFFICE": return "Quay về bưu cục";
    default: return "Giao hàng";
  }
}

export function getStopTypeBadge(stopType?: RouteStopType): { color: string; bg: string; label: string } {
  switch (stopType) {
    case "DELIVERY":
      return { color: "#1C3D90", bg: "#e3f2fd", label: "Giao" };
    case "PICKUP":
      return { color: "#722ed1", bg: "#f3e5f5", label: "Lấy" };
    case "RETURN_TO_OFFICE":
      return { color: "#E53935", bg: "#ffebee", label: "Về BC" };
    default:
      return { color: "#1C3D90", bg: "#e3f2fd", label: "Giao" };
  }
}

export function createStopMarkerSvg(
  color: string,
  label: string,
  highlighted: boolean,
  stopType?: RouteStopType
): string {
  const size = highlighted ? 40 : 34;
  const fontSize = label.length > 3 ? 9 : 11;
  let fillColor = color;
  if (stopType === "RETURN_TO_OFFICE") fillColor = "#E53935";
  if (stopType === "PICKUP") fillColor = "#722ed1";
  const svg = `
<svg xmlns="http://www.w3.org/2000/svg" width="${size}" height="${size}" viewBox="0 0 40 40">
  <circle cx="20" cy="20" r="17" fill="#ffffff" stroke="${fillColor}" stroke-width="${highlighted ? 4 : 3}"/>
  <text x="20" y="24" text-anchor="middle" font-family="Arial,sans-serif" font-size="${fontSize}" font-weight="700" fill="${fillColor}">${label}</text>
</svg>`;
  return `data:image/svg+xml;charset=UTF-8,${encodeURIComponent(svg.trim())}`;
}

export function createReturnToOfficeMarkerSvg(label?: string): string {
  const lbl = label || "VỀ";
  const svg = `
<svg xmlns="http://www.w3.org/2000/svg" width="56" height="72" viewBox="0 0 56 72">
  <path d="M28 0C12.54 0 0 12.54 0 28c0 21 28 44 28 44s28-23 28-44C56 12.54 43.46 0 28 0z"
        fill="#E53935"/>
  <circle cx="28" cy="28" r="17" fill="#ffffff"/>
  <path d="M17 30.5L28 20l11 10.5v13H31.5v-8h-7v8H17v-13z"
        fill="#E53935"/>
  <text x="28" y="55" text-anchor="middle" font-family="Arial,sans-serif" font-size="9" font-weight="700" fill="#ffffff">${lbl}</text>
</svg>`;
  return `data:image/svg+xml;charset=UTF-8,${encodeURIComponent(svg.trim())}`;
}

export function createDepotMarkerSvg(): string {
  const svg = `
<svg xmlns="http://www.w3.org/2000/svg" width="56" height="72" viewBox="0 0 56 72">
  <path d="M28 0C12.54 0 0 12.54 0 28c0 21 28 44 28 44s28-23 28-44C56 12.54 43.46 0 28 0z"
        fill="#E53935"/>
  <circle cx="28" cy="28" r="17" fill="#ffffff"/>
  <path d="M17 30.5L28 20l11 10.5v13H31.5v-8h-7v8H17v-13z"
        fill="#E53935"/>
  <path d="M14.5 29L28 16.5L41.5 29"
        stroke="#E53935" stroke-width="4" stroke-linecap="round" stroke-linejoin="round"/>
</svg>`;
  return `data:image/svg+xml;charset=UTF-8,${encodeURIComponent(svg.trim())}`;
}

export function createPickupMarkerSvg(label: string): string {
  const size = 34;
  const svg = `
<svg xmlns="http://www.w3.org/2000/svg" width="${size}" height="${size}" viewBox="0 0 40 40">
  <circle cx="20" cy="20" r="17" fill="#722ed1" stroke="#ffffff" stroke-width="3"/>
  <text x="20" y="25" text-anchor="middle" font-family="Arial,sans-serif" font-size="10" font-weight="700" fill="#ffffff">${label}</text>
</svg>`;
  return `data:image/svg+xml;charset=UTF-8,${encodeURIComponent(svg.trim())}`;
}
