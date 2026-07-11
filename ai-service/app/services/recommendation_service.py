from __future__ import annotations

import logging
from typing import List, Optional, Tuple

from app.config.settings import Settings
from app.models.recommendation import (
    RecommendationCandidateOrder,
    RecommendationCurrentOrder,
    RecommendationItem,
    RecommendationLocation,
    RecommendationRequest,
    RecommendationResponse,
    RecommendationVehicleCapacity,
)
from app.services.geo_utils import (
    haversine_distance_km,
    haversine_duration_seconds,
)

logger = logging.getLogger(__name__)

W_DISTANCE = 40.0
W_AREA = 30.0
W_LOAD = 20.0
W_PRIORITY = 10.0

# Trọng số khi thiếu tọa độ hợp lệ → phân bổ lại.
W_AREA_NO_DIST = 50.0
W_LOAD_NO_DIST = 33.33
W_PRIORITY_NO_DIST = 16.67

# Ngưỡng khoảng cách để chấm điểm tuyến tính.
NEAR_DISTANCE_KM = 1.5
FAR_DISTANCE_KM = 5.0

# Tốc độ trung bình mặc định (km/h) dùng để ước lượng thời gian di chuyển.
DEFAULT_SPEED_KMH = 25.0


def is_valid_coordinate(lat: Optional[float], lng: Optional[float]) -> bool:
    """Tọa độ được xem là hợp lệ khi:

    - lat trong [-90, 90]
    - lng trong [-180, 180]
    - Cặp (0, 0) bị xem là dữ liệu thiếu trong nghiệp vụ này (gần Null Island).
    """
    if lat is None or lng is None:
        return False
    if lat == 0.0 and lng == 0.0:
        return False
    if not -90.0 <= lat <= 90.0:
        return False
    if not -180.0 <= lng <= 180.0:
        return False
    return True


def _resolve_location(
    request: RecommendationRequest,
) -> Tuple[Optional[RecommendationLocation], str]:
    """Xác định vị trí tham chiếu dùng để chấm điểm.

    Trả về (location, source). Nếu location None thì source = NONE.
    """
    if request.current_location is not None and is_valid_coordinate(
        request.current_location.latitude, request.current_location.longitude
    ):
        return request.current_location, "CUSTOM"
    return None, "NONE"


def _area_match_ratio(
    candidate: RecommendationCandidateOrder,
    current_orders: List[RecommendationCurrentOrder],
) -> Tuple[float, int]:
    """Tỉ lệ đơn đang thực hiện cùng khu vực (wardCode hoặc cityCode) với candidate.

    Trả về (ratio, count_in_area). Nếu không có đơn đang thực hiện → ratio = 0.5
    (cho điểm trung bình cộng, không phạt cũng không thưởng).
    """
    if not current_orders:
        return 0.5, 0

    same_area = 0
    for co in current_orders:
        if (
            candidate.recipient_ward_code is not None
            and co.recipient_ward_code is not None
            and candidate.recipient_ward_code == co.recipient_ward_code
        ):
            same_area += 1
            continue
        if (
            candidate.recipient_city_code is not None
            and co.recipient_city_code is not None
            and candidate.recipient_city_code == co.recipient_city_code
        ):
            same_area += 1
    return same_area / len(current_orders), same_area


def _distance_score(distance_km: Optional[float]) -> float:
    """Chấm điểm khoảng cách trong khoảng [0, W_DISTANCE].

    - ≤ 1.5 km: full điểm
    - 1.5–5 km: giảm tuyến tính
    - > 5 km: 0
    """
    if distance_km is None:
        return 0.0
    if distance_km <= NEAR_DISTANCE_KM:
        return W_DISTANCE
    if distance_km >= FAR_DISTANCE_KM:
        return 0.0
    span = FAR_DISTANCE_KM - NEAR_DISTANCE_KM
    ratio = (FAR_DISTANCE_KM - distance_km) / span
    return round(W_DISTANCE * ratio, 4)


def _load_score(
    candidate: RecommendationCandidateOrder,
    capacity: RecommendationVehicleCapacity,
) -> Tuple[float, bool]:
    """Chấm điểm khả năng tải trong khoảng [0, W_LOAD].

    Trả về (score, is_over_capacity). Nếu đơn vượt tải, trả 0 và cờ True.
    """
    remaining_weight = max(0.0, (capacity.weight or 0.0))

    if candidate.weight_kg <= 0:
        # Không có dữ liệu khối lượng → cho điểm trung bình.
        return W_LOAD * 0.5, False

    if remaining_weight <= 0:
        return 0.0, True

    if candidate.weight_kg > remaining_weight:
        return 0.0, True

    # Tỉ lệ headroom còn lại sau khi nhận đơn này.
    headroom = (remaining_weight - candidate.weight_kg) / remaining_weight
    return round(W_LOAD * max(0.0, min(1.0, headroom)), 4), False


def _priority_score(candidate: RecommendationCandidateOrder) -> float:
    """Chấm điểm mức độ ưu tiên trong khoảng [0, W_PRIORITY].

    Dữ liệu thật chỉ có OrderStatus.URGENT_PICKUP. Nếu thiếu dữ liệu deadline
    thì cho 0 (không cộng điểm).
    """
    if candidate.is_urgent:
        return W_PRIORITY
    return 0.0


def _format_distance_km(distance_km: float) -> str:
    return f"{distance_km:.2f}".rstrip("0").rstrip(".")


def _build_reasons(
    candidate: RecommendationCandidateOrder,
    distance_km: Optional[float],
    eta_minutes: int,
    area_count: int,
    in_flight_count: int,
    is_over_capacity: bool,
    location_source: str,
    distance_evaluated: bool,
) -> List[str]:
    reasons: List[str] = []
    if is_over_capacity:
        reasons.append("Vượt tải trọng còn lại")

    if not distance_evaluated:
        # Không tính khoảng cách → KHÔNG in lý do dạng "cách X km".
        reasons.append("Không có tọa độ người nhận để ước lượng khoảng cách")
    else:
        assert distance_km is not None
        if distance_km <= NEAR_DISTANCE_KM:
            reasons.append("Gần vị trí hiện tại")
        elif distance_km <= FAR_DISTANCE_KM:
            reasons.append(f"Cách vị trí hiện tại {_format_distance_km(distance_km)} km")
        else:
            reasons.append(f"Cách vị trí hiện tại {_format_distance_km(distance_km)} km (hơi xa)")

    if distance_evaluated and eta_minutes > 0:
        if eta_minutes <= 8:
            reasons.append(f"Thời gian dự kiến ngắn ({eta_minutes} phút)")
        else:
            reasons.append(f"Thời gian dự kiến {eta_minutes} phút")

    if in_flight_count > 0 and area_count > 0:
        reasons.append(f"Cùng khu vực với {area_count} đơn đang thực hiện")

    if not is_over_capacity and candidate.weight_kg > 0:
        reasons.append("Không vượt tải trọng")

    if candidate.is_urgent:
        reasons.append("Đơn ưu tiên lấy hàng")

    if location_source == "OFFICE":
        # Ghi chú nhẹ khi vị trí fallback là bưu cục.
        reasons.append("Ước lượng theo vị trí bưu cục")

    if not reasons:
        reasons.append("Có thể nhận")
    return reasons


def _level_for_score(score: int, is_over_capacity: bool) -> str:
    if is_over_capacity:
        return "OVER_CAPACITY"
    if score >= 80:
        return "HIGH"
    if score >= 60:
        return "MEDIUM"
    if score >= 40:
        return "LOW"
    return "NOT_RECOMMENDED"


def _clip_score(raw: float) -> int:
    return max(0, min(100, int(round(raw))))


def score_candidates(
    request: RecommendationRequest,
    settings: Settings,
) -> RecommendationResponse:
    """Tính điểm cho toàn bộ candidate_orders, trả về danh sách đã sắp xếp giảm dần."""
    speed_kmh = float(getattr(settings, "default_shipper_speed_kmh", DEFAULT_SPEED_KMH))
    if speed_kmh <= 0:
        speed_kmh = DEFAULT_SPEED_KMH

    location, location_source = _resolve_location(request)
    location_valid = location is not None

    items: List[RecommendationItem] = []
    for cand in request.candidate_orders:
        distance_km: Optional[float] = None
        eta_minutes: int = 0
        distance_evaluated: bool = False

        candidate_coords_valid = is_valid_coordinate(cand.latitude, cand.longitude)
        if location_valid and candidate_coords_valid:
            try:
                a = {"lat": location.latitude, "lng": location.longitude}
                b = {"lat": cand.latitude, "lng": cand.longitude}
                distance_km = round(haversine_distance_km(a, b), 3)
                eta_seconds = haversine_duration_seconds(a, b, speed_kmh)
                eta_minutes = max(1, int(round(eta_seconds / 60.0))) if eta_seconds > 0 else 0
                distance_evaluated = True
            except Exception as exc:  # noqa: BLE001
                logger.warning(
                    "Cannot compute distance for candidate orderId=%s: %s",
                    cand.order_id,
                    exc,
                )
                distance_km = None
                eta_minutes = 0
                distance_evaluated = False

        # Trọng số: nếu thiếu tọa độ ở location hoặc candidate → bỏ distance, phân bổ lại.
        if distance_evaluated:
            s_dist = _distance_score(distance_km)
            s_area = round(W_AREA * _area_match_ratio(cand, request.current_orders)[0], 4)
            s_load, is_over = _load_score(cand, request.vehicle_capacity)
            s_pri = _priority_score(cand)
        else:
            s_dist = 0.0
            s_area = round(W_AREA_NO_DIST * _area_match_ratio(cand, request.current_orders)[0], 4)
            s_load, is_over = _load_score_no_dist(cand, request.vehicle_capacity)
            s_pri = _priority_score_no_dist(cand)

        area_ratio, area_count = _area_match_ratio(cand, request.current_orders)

        raw = s_dist + s_area + s_load + s_pri
        score = _clip_score(raw)
        level = _level_for_score(score, is_over)
        recommended = level in ("HIGH", "MEDIUM", "LOW")

        in_flight_count = len(request.current_orders)
        reasons = _build_reasons(
            candidate=cand,
            distance_km=distance_km,
            eta_minutes=eta_minutes,
            area_count=area_count,
            in_flight_count=in_flight_count,
            is_over_capacity=is_over,
            location_source=location_source,
            distance_evaluated=distance_evaluated,
        )

        items.append(
            RecommendationItem(
                order_id=cand.order_id,
                score=score,
                level=level,
                reasons=reasons,
                estimated_distance_km=distance_km if distance_evaluated else None,
                estimated_duration_minutes=eta_minutes if distance_evaluated else None,
                recommended=recommended,
            )
        )

        logger.info(
            "[AI][Recommendation] scored order_id=%s score=%s level=%s recommended=%s "
            "distance_km=%s duration_min=%s distance_evaluated=%s reasons=%s",
            cand.order_id,
            score,
            level,
            recommended,
            distance_km,
            eta_minutes,
            distance_evaluated,
            reasons,
        )

    items.sort(key=lambda x: (-x.score, x.order_id))

    logger.info(
        "[AI][Recommendation] response recommendation_count=%s",
        len(items)
    )

    response = RecommendationResponse(
        success=True,
        message="OK",
        recommendations=items,
        fallback_location_source=location_source,
        location_source=location_source,
    )

    logger.info(
        "[AI][Recommendation] raw_response=%s",
        response.model_dump_json()
    )
    return response


def _load_score_no_dist(
    candidate: RecommendationCandidateOrder,
    capacity: RecommendationVehicleCapacity,
) -> Tuple[float, bool]:
    """Phiên bản _load_score dùng khi không có khoảng cách (trọng số lớn hơn)."""
    remaining_weight = max(0.0, (capacity.weight or 0.0))

    if candidate.weight_kg <= 0:
        return W_LOAD_NO_DIST * 0.5, False
    if remaining_weight <= 0:
        return 0.0, True
    if candidate.weight_kg > remaining_weight:
        return 0.0, True
    headroom = (remaining_weight - candidate.weight_kg) / remaining_weight
    return round(W_LOAD_NO_DIST * max(0.0, min(1.0, headroom)), 4), False


def _priority_score_no_dist(candidate: RecommendationCandidateOrder) -> float:
    if candidate.is_urgent:
        return W_PRIORITY_NO_DIST
    return 0.0