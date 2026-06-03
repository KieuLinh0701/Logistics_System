from __future__ import annotations

import logging

from app.config.settings import Settings
from app.models.route_optimization import (
    OptimizationSummary,
    RouteOptimizationRequest,
    RouteOptimizationResponse,
    RouteStopOutput,
    ShipperRouteOutput,
    UnassignedOrderOutput,
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
        """Điều phối phân công đơn và tối ưu thứ tự tuyến cho từng shipper."""
        shipper_load, unassigned_pairs = assign_orders_to_shippers(
            request.office,
            request.shippers,
            request.orders,
            settings=self._settings,
            enable_debug_log=self._settings.enable_assignment_debug_log,
        )

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
            matrix_result = self._maps.build_duration_matrix(request.office, assigned_orders)
            # Dùng ma trận thời gian để OR-Tools tối ưu thứ tự dừng.
            ordered_orders, optimizer_duration_seconds, haversine_km = solve_route_for_shipper(
                request.office,
                shipper,
                assigned_orders,
                time_limit,
                duration_matrix=matrix_result.duration_matrix,
            )
            if not ordered_orders:
                # Không có nghiệm, trả về danh sách chưa gán để backend xử lý.
                for order in assigned_orders:
                    unassigned_pairs.append((order, "ORTOOLS_NO_SOLUTION"))
                continue

            logger.info(
                "OR-Tools route shipper=%s matrix_source=%s fallback_used=%s "
                "route_order=%s total_duration_seconds=%s",
                shipper.name,
                matrix_result.matrix_source,
                matrix_result.fallback_used,
                [o.id for o in ordered_orders],
                optimizer_duration_seconds,
            )

            # Lấy tuyến thực tế từ Directions để tính quãng đường và ETA.
            _, real_distance_km, real_duration_minutes, encoded_polyline = self._maps.get_driving_route(
                request.office, ordered_orders
            )
            if real_distance_km <= 0:
                real_distance_km = haversine_km

            fuel_cost = real_distance_km * shipper.fuel_cost_per_km
            total_cod = sum(o.cod_amount for o in ordered_orders)
            etas = build_stop_etas(shipper, ordered_orders, real_duration_minutes)

            stops: list[RouteStopOutput] = []
            for idx, order in enumerate(ordered_orders, start=1):
                eta_time, eta_minutes = etas[idx - 1] if idx - 1 < len(etas) else (None, None)
                stops.append(
                    RouteStopOutput(
                        order_id=order.id,
                        tracking_number=order.tracking_number,
                        recipient_name=order.recipient_name,
                        recipient_phone=order.recipient_phone,
                        recipient_address=order.recipient_address,
                        latitude=order.latitude,
                        longitude=order.longitude,
                        cod_amount=order.cod_amount,
                        priority=order.priority,
                        stop_sequence=idx,
                        eta_time=eta_time,
                        eta_minutes_from_start=eta_minutes,
                    )
                )

            route_sequence += 1
            routes.append(
                ShipperRouteOutput(
                    shipper_id=shipper.id,
                    employee_id=shipper.employee_id,
                    shipper_name=shipper.name,
                    route_sequence=route_sequence,
                    stops=stops,
                    estimated_distance_km=round(real_distance_km, 3),
                    estimated_duration_minutes=round(real_duration_minutes, 2),
                    fuel_cost=round(fuel_cost, 2),
                    total_cod=total_cod,
                    encoded_polyline=encoded_polyline,
                    start_time=shipper.start_time,
                    matrix_source=matrix_result.matrix_source,
                    fallback_used=matrix_result.fallback_used,
                    optimizer_duration_seconds=optimizer_duration_seconds,
                    cost_mode="DURATION",
                )
            )

        unassigned_outputs = [
            UnassignedOrderOutput(
                order_id=order.id,
                tracking_number=order.tracking_number,
                reason=reason,
            )
            for order, reason in unassigned_pairs
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
            message="Tối ưu tuyến giao hàng thành công",
            routes=routes,
            unassigned_orders=unassigned_outputs,
            summary=summary,
        )
