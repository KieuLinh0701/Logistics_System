import logging

from app.config.settings import Settings
from app.models.route_optimization import OfficeLocation, OrderInput, ShipperAssignmentArea, ShipperInput
from app.services.geo_utils import distance_meter

logger = logging.getLogger(__name__)


def assignment_matches_order(assignment: ShipperAssignmentArea, order: OrderInput) -> bool:
    """
    Kiểm tra đơn hàng có thuộc khu vực được phân công hay không.
    Ưu tiên so khớp theo phường/xã, nếu không có thì so khớp theo thành phố.
    """
    ward = assignment.ward_code
    if ward is not None and ward != 0:
        return order.recipient_ward_code == ward
    return order.recipient_city_code == assignment.city_code


def shipper_matches_order(shipper: ShipperInput, order: OrderInput) -> bool:
    return any(assignment_matches_order(a, order) for a in shipper.assignments)


def _order_point(order: OrderInput) -> dict:
    return {"lat": order.latitude, "lng": order.longitude}


def _office_point(office: OfficeLocation) -> dict:
    return {"lat": office.latitude, "lng": office.longitude}


def _min_distance_to_order(existing_orders: list[OrderInput], order: OrderInput) -> int:
    if not existing_orders:
        return 0
    target = _order_point(order)
    return min(distance_meter(_order_point(o), target) for o in existing_orders)


def _compactness_penalty(existing_orders: list[OrderInput]) -> float:
    if len(existing_orders) < 2:
        return 0.0
    points = [_order_point(o) for o in existing_orders]
    total = 0
    count = 0
    for i in range(len(points) - 1):
        for j in range(i + 1, len(points)):
            total += distance_meter(points[i], points[j])
            count += 1
    return total / count if count else 0.0


def _normalize(value: float, max_value: float) -> float:
    if max_value <= 0:
        return 0.0
    return value / max_value


def _safe_weight_kg(order: OrderInput) -> float:
    return order.weight_kg if order.weight_kg > 0 else 1.0


def _shipper_weight_limit(shipper: ShipperInput, settings: Settings) -> float:
    if shipper.remaining_weight_kg is not None:
        return max(0.0, shipper.remaining_weight_kg)
    if shipper.max_weight_kg is not None:
        return max(0.0, shipper.max_weight_kg)
    return max(0.0, settings.default_shipper_weight_capacity_kg)


def assign_orders_to_shippers(
    office: OfficeLocation,
    shippers: list[ShipperInput],
    orders: list[OrderInput],
    settings: Settings,
    enable_debug_log: bool = False,
) -> tuple[dict[int, list[OrderInput]], list[tuple[OrderInput, str]]]:
    """
    Phân công đơn cho shipper theo khu vực, sức chứa và tải trọng.
    Trả về danh sách đã gán và danh sách chưa gán kèm lý do.
    """
    shipper_load: dict[int, list[OrderInput]] = {s.id: [] for s in shippers}
    unassigned: list[tuple[OrderInput, str]] = []

    if not shippers:
        return shipper_load, [(o, "NO_SHIPPER_AVAILABLE") for o in orders]

    for shipper in shippers:
        if (
            (shipper.vehicle_type or "").upper() == "ELECTRIC_BIKE"
            and shipper.battery_level is not None
            and shipper.battery_level <= settings.electric_bike_low_battery_threshold
        ):
            logger.warning(
                "Low battery warning shipper_id=%s battery_level=%s threshold=%s",
                shipper.id,
                shipper.battery_level,
                settings.electric_bike_low_battery_threshold,
            )

    depot = _office_point(office)

    for order in orders:
        candidates = [s for s in shippers if shipper_matches_order(s, order)]
        if enable_debug_log:
            logger.debug(
                "Assignment order=%s ward=%s city=%s matched_shippers=%s",
                order.id,
                order.recipient_ward_code,
                order.recipient_city_code,
                len(candidates),
            )
        if not candidates:
            unassigned.append((order, "NO_MATCHING_AREA"))
            if enable_debug_log:
                logger.debug("Assignment unassigned order=%s reason=NO_MATCHING_AREA", order.id)
            continue

        # Bỏ qua shipper đã vượt sức chứa đơn.
        candidates = [s for s in candidates if len(shipper_load[s.id]) < s.capacity]
        if not candidates:
            unassigned.append((order, "CAPACITY_EXCEEDED"))
            if enable_debug_log:
                logger.debug("Assignment unassigned order=%s reason=CAPACITY_EXCEEDED", order.id)
            continue

        order_weight = _safe_weight_kg(order)
        weight_filtered: list[ShipperInput] = []
        for shipper in candidates:
            current_weight = sum(_safe_weight_kg(o) for o in shipper_load[shipper.id])
            limit_weight = _shipper_weight_limit(shipper, settings)
            if current_weight + order_weight <= limit_weight:
                weight_filtered.append(shipper)

        if not weight_filtered:
            unassigned.append((order, "WEIGHT_CAPACITY_EXCEEDED"))
            if enable_debug_log:
                logger.debug("Assignment unassigned order=%s reason=WEIGHT_CAPACITY_EXCEEDED", order.id)
            continue

        candidates = weight_filtered

        distance_scores: dict[int, float] = {}
        compactness_scores: dict[int, float] = {}
        workload_scores: dict[int, float] = {}

        for shipper in candidates:
            current_orders = shipper_load[shipper.id]
            workload_ratio = len(current_orders) / max(shipper.capacity, 1)
            workload_scores[shipper.id] = workload_ratio

            if not current_orders:
                distance_scores[shipper.id] = float(distance_meter(depot, _order_point(order)))
            else:
                distance_scores[shipper.id] = float(_min_distance_to_order(current_orders, order))

            compactness_scores[shipper.id] = float(_compactness_penalty(current_orders))

        max_distance = max(distance_scores.values(), default=0.0)
        max_compactness = max(compactness_scores.values(), default=0.0)

        def assignment_score(shipper: ShipperInput) -> float:
            # Chuẩn hóa điểm để cộng các tiêu chí khác đơn vị.
            return (
                workload_scores[shipper.id] * 0.45
                + _normalize(distance_scores[shipper.id], max_distance) * 0.45
                + _normalize(compactness_scores[shipper.id], max_compactness) * 0.10
            )

        if enable_debug_log:
            for shipper in candidates:
                score = assignment_score(shipper)
                current_orders = len(shipper_load[shipper.id])
                distance_km = distance_scores[shipper.id] / 1000.0
                compactness_km = compactness_scores[shipper.id] / 1000.0
                logger.debug(
                    "Assignment candidate order=%s shipper_id=%s shipper_name=%s "
                    "current_orders=%s capacity=%s workload_ratio=%.3f "
                    "distance_km=%.3f compactness_km=%.3f score=%.4f",
                    order.id,
                    shipper.id,
                    shipper.name,
                    current_orders,
                    shipper.capacity,
                    workload_scores[shipper.id],
                    distance_km,
                    compactness_km,
                    score,
                )

        chosen = min(candidates, key=assignment_score)
        shipper_load[chosen.id].append(order)

        if enable_debug_log:
            selected_score = assignment_score(chosen)
            logger.debug(
                "Assignment selected order=%s shipper_id=%s shipper_name=%s score=%.4f",
                order.id,
                chosen.id,
                chosen.name,
                selected_score,
            )

    return shipper_load, unassigned
