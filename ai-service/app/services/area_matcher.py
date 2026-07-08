import logging
from enum import Enum

from app.config.settings import Settings
from app.models.route_optimization import (
    OfficeLocation,
    OrderInput,
    RouteStopInput,
    ShipperAssignmentArea,
    ShipperInput,
)
from app.services.geo_utils import distance_meter

logger = logging.getLogger(__name__)


class MatchType(str, Enum):
    WARD_MATCH = "WARD_MATCH"
    CITY_MATCH = "CITY_MATCH"
    NO_MATCH = "NO_MATCH"


def _to_dict(obj) -> dict:
    """Chuyển Pydantic model hoặc dict thành dict thuần để truy cập trường."""
    if isinstance(obj, dict):
        return obj
    if hasattr(obj, "model_dump"):
        return obj.model_dump()
    return {}


def _order_ward_code(order: OrderInput | RouteStopInput) -> int | None:
    d = _to_dict(order)
    return d.get("recipient_ward_code", d.get("ward_code"))


def _order_city_code(order: OrderInput | RouteStopInput) -> int | None:
    d = _to_dict(order)
    return d.get("recipient_city_code", d.get("city_code"))


def _order_tracking_number(order: OrderInput | RouteStopInput) -> str | None:
    d = _to_dict(order)
    return d.get("tracking_number")


def classify_assignment_match(
    assignment: ShipperAssignmentArea, order: OrderInput | RouteStopInput
) -> MatchType:
    """
    Phân loại mức độ match giữa assignment và order:
    - WARD_MATCH: assignment có ward trùng với ward của order.
    - CITY_MATCH: assignment.city_code trùng với city_code của order
                  (ward khác hoặc assignment không có ward).
    - NO_MATCH:   khác cả city.
    """
    order_city = _order_city_code(order)
    order_ward = _order_ward_code(order)

    # 1) Ward match (chỉ khi assignment có ward hợp lệ và order cũng có ward)
    assignment_ward = assignment.ward_code
    if (
        assignment_ward is not None
        and assignment_ward != 0
        and order_ward is not None
        and order_ward != 0
        and int(assignment_ward) == int(order_ward)
    ):
        return MatchType.WARD_MATCH

    # 2) City match
    if order_city is not None and int(assignment.city_code) == int(order_city):
        return MatchType.CITY_MATCH

    return MatchType.NO_MATCH


def best_match_for_shipper(
    shipper: ShipperInput, order: OrderInput | RouteStopInput
) -> MatchType:
    """
    Lấy mức match tốt nhất của shipper với order dựa trên các assignments.
    Ưu tiên: WARD_MATCH > CITY_MATCH > NO_MATCH.
    """
    best = MatchType.NO_MATCH
    for a in shipper.assignments:
        m = classify_assignment_match(a, order)
        if m == MatchType.WARD_MATCH:
            return MatchType.WARD_MATCH
        if m == MatchType.CITY_MATCH:
            best = MatchType.CITY_MATCH
    return best


def shipper_matches_order(shipper: ShipperInput, order: OrderInput | RouteStopInput) -> bool:
    """Shipper phù hợp order nếu có ít nhất 1 assignment match ward hoặc city."""
    return best_match_for_shipper(shipper, order) != MatchType.NO_MATCH


def _order_point(order: OrderInput | RouteStopInput) -> dict:
    d = _to_dict(order)
    return {"lat": d.get("latitude"), "lng": d.get("longitude")}


def _order_id(order: OrderInput | RouteStopInput):
    """Lấy order id — RouteStopInput dùng order_id, OrderInput dùng id."""
    d = _to_dict(order)
    return d.get("order_id", d.get("id"))


def _safe_weight_kg(order: OrderInput | RouteStopInput) -> float:
    d = _to_dict(order)
    val = d.get("weight_kg")
    return val if val and val > 0 else 1.0


def _office_point(office: OfficeLocation) -> dict:
    return {"lat": office.latitude, "lng": office.longitude}


def _min_distance_to_order(
    existing_orders: list[OrderInput | RouteStopInput], order: OrderInput | RouteStopInput
) -> int:
    if not existing_orders:
        return 0
    target = _order_point(order)
    return min(distance_meter(_order_point(o), target) for o in existing_orders)


def _compactness_penalty(existing_orders: list[OrderInput | RouteStopInput]) -> float:
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


def _shipper_weight_limit(shipper: ShipperInput, settings: Settings) -> float:
    if shipper.remaining_weight_kg is not None:
        return max(0.0, shipper.remaining_weight_kg)
    if shipper.max_weight_kg is not None:
        return max(0.0, shipper.max_weight_kg)
    return max(0.0, settings.default_shipper_weight_capacity_kg)


def assign_orders_to_shippers(
    office: OfficeLocation,
    shippers: list[ShipperInput],
    orders: list[OrderInput | RouteStopInput],
    settings: Settings,
    enable_debug_log: bool = False,
) -> tuple[
    dict[int, list[OrderInput | RouteStopInput]],
    list[tuple[OrderInput | RouteStopInput, str]],
]:
    """
    Phân công đơn cho shipper theo khu vực (WARD_MATCH ưu tiên CITY_MATCH), sức chứa và tải trọng.
    Trả về danh sách đã gán và danh sách chưa gán kèm lý do.
    """
    shipper_load: dict[int, list[OrderInput | RouteStopInput]] = {s.id: [] for s in shippers}
    unassigned: list[tuple[OrderInput | RouteStopInput, str]] = []

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
        order_id = _order_id(order)
        order_ward = _order_ward_code(order)
        order_city = _order_city_code(order)
        order_tracking = _order_tracking_number(order)

        # Phân loại từng shipper theo matchType
        candidate_with_match: list[tuple[ShipperInput, MatchType]] = []
        for sp in shippers:
            mt = best_match_for_shipper(sp, order)
            if mt == MatchType.NO_MATCH:
                continue
            candidate_with_match.append((sp, mt))

        if not candidate_with_match:
            unassigned.append((order, "NO_MATCHING_AREA"))
            logger.warning(
                "[AREA_MATCH_NO_MATCH] order_id=%s tracking=%s order_city=%s order_ward=%s",
                order_id,
                order_tracking,
                order_city,
                order_ward,
            )
            continue

        # Log từng shipper match gì
        for sp, mt in candidate_with_match:
            assignment_summary = ",".join(
                f"(city={a.city_code},ward={a.ward_code})" for a in sp.assignments
            )
            logger.info(
                "[AREA_MATCH] order_id=%s tracking=%s order_city=%s order_ward=%s "
                "shipper_id=%s shipper_name=%s employee_id=%s assignments=%s match_type=%s",
                order_id,
                order_tracking,
                order_city,
                order_ward,
                sp.id,
                sp.name,
                sp.employee_id,
                assignment_summary,
                mt.value,
            )

        # Ưu tiên WARD_MATCH trước, nếu không có WARD_MATCH thì dùng CITY_MATCH
        ward_candidates = [sp for sp, mt in candidate_with_match if mt == MatchType.WARD_MATCH]
        if ward_candidates:
            candidates = ward_candidates
            match_type_used = MatchType.WARD_MATCH
        else:
            candidates = [sp for sp, mt in candidate_with_match if mt == MatchType.CITY_MATCH]
            match_type_used = MatchType.CITY_MATCH

        if enable_debug_log:
            logger.debug(
                "Assignment order=%s match_type_used=%s candidates=%s",
                order_id,
                match_type_used.value,
                len(candidates),
            )

        # Bỏ qua shipper đã vượt sức chứa đơn.
        candidates = [s for s in candidates if len(shipper_load[s.id]) < s.capacity]
        if not candidates:
            unassigned.append((order, "CAPACITY_EXCEEDED"))
            if enable_debug_log:
                logger.debug("Assignment unassigned order=%s reason=CAPACITY_EXCEEDED", order_id)
            continue

        order_weight = _safe_weight_kg(order)
        weight_filtered: list[ShipperInput] = []
        for sp in candidates:
            current_weight = sum(_safe_weight_kg(o) for o in shipper_load[sp.id])
            limit_weight = _shipper_weight_limit(sp, settings)
            if current_weight + order_weight <= limit_weight:
                weight_filtered.append(sp)

        if not weight_filtered:
            unassigned.append((order, "WEIGHT_CAPACITY_EXCEEDED"))
            if enable_debug_log:
                logger.debug("Assignment unassigned order=%s reason=WEIGHT_CAPACITY_EXCEEDED", order_id)
            continue

        candidates = weight_filtered

        distance_scores: dict[int, float] = {}
        compactness_scores: dict[int, float] = {}
        workload_scores: dict[int, float] = {}

        for sp in candidates:
            current_orders = shipper_load[sp.id]
            workload_ratio = len(current_orders) / max(sp.capacity, 1)
            workload_scores[sp.id] = workload_ratio

            if not current_orders:
                distance_scores[sp.id] = float(distance_meter(depot, _order_point(order)))
            else:
                distance_scores[sp.id] = float(_min_distance_to_order(current_orders, order))

            compactness_scores[sp.id] = float(_compactness_penalty(current_orders))

        max_distance = max(distance_scores.values(), default=0.0)
        max_compactness = max(compactness_scores.values(), default=0.0)

        def assignment_score(sp: ShipperInput) -> float:
            return (
                workload_scores[sp.id] * 0.45
                + _normalize(distance_scores[sp.id], max_distance) * 0.45
                + _normalize(compactness_scores[sp.id], max_compactness) * 0.10
            )

        if enable_debug_log:
            for sp in candidates:
                score = assignment_score(sp)
                current_orders = len(shipper_load[sp.id])
                distance_km = distance_scores[sp.id] / 1000.0
                compactness_km = compactness_scores[sp.id] / 1000.0
                logger.debug(
                    "Assignment candidate order=%s shipper_id=%s shipper_name=%s "
                    "current_orders=%s capacity=%s workload_ratio=%.3f "
                    "distance_km=%.3f compactness_km=%.3f score=%.4f",
                    order_id,
                    sp.id,
                    sp.name,
                    current_orders,
                    sp.capacity,
                    workload_scores[sp.id],
                    distance_km,
                    compactness_km,
                    score,
                )

        chosen = min(candidates, key=assignment_score)
        shipper_load[chosen.id].append(order)

        logger.info(
            "[AREA_MATCH_SELECTED] order_id=%s tracking=%s shipper_id=%s shipper_name=%s "
            "employee_id=%s match_type=%s",
            order_id,
            order_tracking,
            chosen.id,
            chosen.name,
            chosen.employee_id,
            match_type_used.value,
        )

    return shipper_load, unassigned