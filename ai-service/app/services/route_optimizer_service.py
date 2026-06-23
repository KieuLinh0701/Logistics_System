from __future__ import annotations

import logging
import time as time_module

from app.config.settings import Settings
from app.models.route_optimization import (
    OptimizationSummary,
    RouteOptimizationRequest,
    RouteOptimizationResponse,
    RouteStopOutput,
    ShipperRouteOutput,
    UnassignedOrderOutput,
    RouteStopInput,
    OrderInput,
    AiLocationDto,
)
from app.services.area_matcher import assign_orders_to_shippers
from app.services.google_maps_service import GoogleMapsService
from app.services.ortools_optimizer import build_stop_etas, solve_route_for_shipper

logger = logging.getLogger(__name__)


class RouteOptimizerService:
    def __init__(self, settings: Settings):
        self._settings = settings
        self._maps = GoogleMapsService(settings)

    def optimize(self, request: RouteOptimizationRequest) -> RouteOptimizationResponse:
        """Dieu phoi phan cong don va toi uu thu tu tuyen cho tung shipper.
        Ho tro closed loop (return-to-office).
        """
        start_location = self._resolve_start_location(request)
        end_location = self._resolve_end_location(request)
        return_to_office = getattr(request, "return_to_office", True)
        route_mode = getattr(request, "route_mode", "CLOSED_LOOP")
        optimization_scope = getattr(request, "optimization_scope", "MANAGER_GLOBAL")

        route_stops = self._normalize_stops(request)

        if optimization_scope in ("MANAGER_GLOBAL", "PICKUP_INSERTION"):
            shipper_load, unassigned_pairs = assign_orders_to_shippers(
                request.office,
                request.shippers,
                route_stops,
                settings=self._settings,
                enable_debug_log=self._settings.enable_assignment_debug_log,
            )
        elif optimization_scope == "SHIPPER_LOCAL":
            if not request.shippers:
                raise ValueError("SHIPPER_LOCAL requires exactly one shipper")
            if len(request.shippers) > 1:
                raise ValueError("SHIPPER_LOCAL supports only one shipper")
            shipper = request.shippers[0]
            if not route_stops:
                shipper_load = {}
                unassigned_pairs = []
            else:
                shipper_load = {shipper.id: list(route_stops)}
                unassigned_pairs = []
        else:
            raise ValueError(f"Unknown optimization_scope: {optimization_scope}")

        routes: list[ShipperRouteOutput] = []
        route_sequence = 0
        time_limit = int(
            request.options.get("ortools_time_limit_seconds", self._settings.ortools_time_limit_seconds)
        )

        shipper_by_id = {s.id: s for s in request.shippers}

        for shipper_id, assigned_orders in shipper_load.items():
            if not assigned_orders:
                continue
            shipper = shipper_by_id[shipper_id]

            matrix_result = self._maps.build_duration_matrix(
                start_location, assigned_orders, end_location
            )

            ai_start_ms = time_module.monotonic() * 1000
            ordered_orders, optimizer_duration_seconds, haversine_km, return_duration_seconds = (
                solve_route_for_shipper(
                    start_location,
                    shipper,
                    assigned_orders,
                    time_limit,
                    duration_matrix=matrix_result.duration_matrix,
                    return_to_office=return_to_office,
                    end_location=end_location,
                    start_time=shipper.start_time,
                    optimization_scope=optimization_scope,
                )
            )
            ai_end_ms = time_module.monotonic() * 1000

            if not ordered_orders:
                for order in assigned_orders:
                    unassigned_pairs.append((order, "ORTOOLS_NO_SOLUTION"))
                continue

            logger.info(
                "Route optimized shipper=%s scope=%s stops=%d duration_seconds=%s",
                shipper.name,
                optimization_scope,
                len(ordered_orders),
                optimizer_duration_seconds,
            )

            # Directions API — fallback không block nếu OVER_QUERY_LIMIT / timeout.
            decoded_polyline_points: list[tuple[float, float]] = []
            real_distance_km: float = haversine_km
            real_duration_minutes: float = optimizer_duration_seconds / 60.0
            encoded_polyline: str | None = None
            return_distance_km: float = 0.0
            return_duration_min: float = 0.0
            try:
                (
                    decoded_polyline_points,
                    real_distance_km,
                    real_duration_minutes,
                    encoded_polyline,
                    return_distance_km,
                    return_duration_min,
                ) = self._maps.get_driving_route(
                    start_location,
                    ordered_orders,
                    end_location,
                    route_mode,
                )
            except Exception as exc:
                logger.warning(
                    "Directions API failed for shipper=%s: %s. Using haversine fallback.",
                    shipper.name,
                    exc,
                )
            directions_end_ms = time_module.monotonic() * 1000
            logger.debug(
                "[TIMING] shipper=%s aiCallMs=%.0f directionsMs=%.0f",
                shipper.name,
                ai_end_ms - ai_start_ms,
                directions_end_ms - ai_end_ms,
            )

            if real_distance_km <= 0:
                real_distance_km = haversine_km
            if return_to_office and return_distance_km > 0:
                real_distance_km += return_distance_km
            if return_to_office:
                real_duration_minutes += return_duration_min

            fuel_cost = real_distance_km * shipper.fuel_cost_per_km
            total_cod = sum(getattr(o, "cod_amount", 0) for o in ordered_orders)
            etas = build_stop_etas(
                shipper,
                ordered_orders,
                real_duration_minutes,
                return_to_office=return_to_office,
                return_duration_seconds=int(return_duration_seconds),
            )

            stops: list[RouteStopOutput] = []
            for idx, order in enumerate(ordered_orders, start=1):
                eta_time, eta_minutes = etas[idx - 1] if idx - 1 < len(etas) else (None, None)
                stop_type = getattr(order, "stop_type", "DELIVERY")
                order_id_out = getattr(order, "order_id", getattr(order, "id", None))
                stops.append(
                    RouteStopOutput(
                        order_id=order_id_out,
                        tracking_number=getattr(order, "tracking_number", None),
                        recipient_name=getattr(order, "recipient_name", None),
                        recipient_phone=getattr(order, "recipient_phone", None),
                        recipient_address=getattr(order, "address", getattr(order, "recipient_address", None)),
                        latitude=order.latitude,
                        longitude=order.longitude,
                        cod_amount=getattr(order, "cod_amount", 0),
                        priority=getattr(order, "priority", "NORMAL"),
                        stop_sequence=idx,
                        stop_type=stop_type,
                        eta_time=eta_time,
                        eta_minutes_from_start=eta_minutes,
                        leg_distance_km=getattr(order, "leg_distance_km", None),
                        leg_duration_minutes=getattr(order, "leg_duration_minutes", None),
                        service_time_minutes=getattr(order, "service_time_minutes", None),
                    )
                )

            return_to_office_stop = None
            if return_to_office:
                return_eta_time, return_eta_minutes = None, None
                if etas and len(etas) > len(stops):
                    return_eta_time, return_eta_minutes = etas[len(stops)]
                office_info = self._get_office_info(request, end_location)
                return_to_office_stop = RouteStopOutput(
                    order_id=None,
                    tracking_number=None,
                    recipient_name=office_info.get("name", "Bưu cục"),
                    recipient_phone=None,
                    recipient_address=office_info.get("address", "Bưu cục"),
                    latitude=office_info.get("latitude", end_location["lat"] if end_location else start_location["lat"]),
                    longitude=office_info.get("longitude", end_location["lng"] if end_location else start_location["lng"]),
                    cod_amount=0,
                    priority="NORMAL",
                    stop_sequence=len(stops) + 1,
                    stop_type="RETURN_TO_OFFICE",
                    eta_time=return_eta_time,
                    eta_minutes_from_start=return_eta_minutes,
                    leg_distance_km=return_distance_km if return_distance_km else None,
                    leg_duration_minutes=int(return_duration_seconds) if return_duration_seconds else None,
                    service_time_minutes=None,
                )

            route_sequence += 1
            routes.append(
                ShipperRouteOutput(
                    shipper_id=shipper.id,
                    employee_id=shipper.employee_id,
                    shipper_name=shipper.name,
                    route_sequence=route_sequence,
                    stops=stops,
                    return_to_office_stop=return_to_office_stop,
                    estimated_distance_km=round(real_distance_km, 3),
                    estimated_duration_minutes=round(real_duration_minutes, 2),
                    fuel_cost=round(fuel_cost, 2),
                    total_cod=total_cod,
                    encoded_polyline=encoded_polyline,
                    start_time=shipper.start_time,
                    route_mode=route_mode,
                    return_to_office=return_to_office,
                    matrix_source=matrix_result.matrix_source,
                    fallback_used=matrix_result.fallback_used,
                    optimizer_duration_seconds=optimizer_duration_seconds,
                    cost_mode="DURATION",
                )
            )

        unassigned_outputs = [
            UnassignedOrderOutput(
                order_id=o.order_id if hasattr(o, "order_id") else o.id,
                tracking_number=getattr(o, "tracking_number", None),
                reason=reason,
            )
            for o, reason in unassigned_pairs
        ]

        summary = OptimizationSummary(
            total_distance_km=round(sum(r.estimated_distance_km for r in routes), 3),
            total_duration_minutes=round(sum(r.estimated_duration_minutes for r in routes), 2),
            total_fuel_cost=round(sum(r.fuel_cost for r in routes), 2),
            total_cod=sum(r.total_cod for r in routes),
            assigned_order_count=sum(len(r.stops) for r in routes),
            unassigned_order_count=len(unassigned_outputs),
            shipper_count=len(routes),
        )

        return RouteOptimizationResponse(
            success=True,
            message="Toi uu tuyen giao hang thanh cong",
            routes=routes,
            unassigned_orders=unassigned_outputs,
            summary=summary,
        )

    def _resolve_start_location(self, request: RouteOptimizationRequest) -> dict:
        if hasattr(request, "start_location") and request.start_location:
            loc = request.start_location
            return {"lat": loc.latitude, "lng": loc.longitude, "type": loc.type, "name": loc.name}
        return {"lat": request.office.latitude, "lng": request.office.longitude, "type": "OFFICE", "name": request.office.name}

    def _resolve_end_location(self, request: RouteOptimizationRequest) -> dict | None:
        return_to_office = getattr(request, "return_to_office", True)
        if not return_to_office:
            return None
        if hasattr(request, "end_location") and request.end_location:
            loc = request.end_location
            return {"lat": loc.latitude, "lng": loc.longitude, "type": loc.type, "name": loc.name}
        return {"lat": request.office.latitude, "lng": request.office.longitude, "type": "OFFICE", "name": request.office.name}

    def _normalize_stops(self, request: RouteOptimizationRequest) -> list:
        if hasattr(request, "stops") and request.stops:
            return request.stops
        route_stops = []
        for order in getattr(request, "orders", []):
            route_stops.append(
                RouteStopInput(
                    stop_id=order.id,
                    order_id=order.id,
                    tracking_number=order.tracking_number,
                    stop_type="DELIVERY",
                    recipient_name=order.recipient_name,
                    recipient_phone=order.recipient_phone,
                    address=order.recipient_address,
                    ward_code=order.recipient_ward_code,
                    city_code=order.recipient_city_code,
                    latitude=order.latitude,
                    longitude=order.longitude,
                    cod_amount=order.cod_amount,
                    priority=order.priority,
                    service_time_minutes=5,
                    weight_kg=getattr(order, "weight_kg", 1.0),
                )
            )
        return route_stops

    def _get_office_info(self, request: RouteOptimizationRequest, end_location: dict | None) -> dict:
        if end_location:
            return {
                "name": end_location.get("name", "Bưu cục"),
                "address": end_location.get("address", "Bưu cục"),
                "latitude": end_location["lat"],
                "longitude": end_location["lng"],
            }
        return {
            "name": request.office.name,
            "address": getattr(request.office, "address", None) or request.office.name,
            "latitude": request.office.latitude,
            "longitude": request.office.longitude,
        }
