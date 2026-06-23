from math import atan2, cos, radians, sin, sqrt

DEFAULT_FALLBACK_SPEED_KMH = 30.0


def distance_meter(a: dict, b: dict) -> int:
    """Khoảng cách Haversine tính theo mét."""
    r = 6371000
    dlat = radians(b["lat"] - a["lat"])
    dlng = radians(b["lng"] - a["lng"])
    x = sin(dlat / 2) ** 2 + cos(radians(a["lat"])) * cos(radians(b["lat"])) * sin(dlng / 2) ** 2
    return int(r * 2 * atan2(sqrt(x), sqrt(1 - x)))


def haversine_duration_seconds(
    a: dict,
    b: dict,
    speed_kmh: float = DEFAULT_FALLBACK_SPEED_KMH,
) -> int:
    """Ước tính thời gian di chuyển từ khoảng cách thẳng với tốc độ trung bình."""
    if a["lat"] == b["lat"] and a["lng"] == b["lng"]:
        return 0
    meters = distance_meter(a, b)
    speed_mps = speed_kmh * 1000.0 / 3600.0
    return max(1, int(meters / speed_mps))


def build_haversine_duration_matrix(
    locations: list[dict],
    speed_kmh: float = DEFAULT_FALLBACK_SPEED_KMH,
) -> list[list[int]]:
    size = len(locations)
    matrix: list[list[int]] = []
    for i, from_loc in enumerate(locations):
        row: list[int] = []
        for j, to_loc in enumerate(locations):
            if i == j:
                row.append(0)
            else:
                row.append(haversine_duration_seconds(from_loc, to_loc, speed_kmh))
        matrix.append(row)
    return matrix


def haversine_route_distance_km(
    office: dict,
    orders: list[dict],
    include_return_to_office: bool = False,
) -> float:
    """Tổng quãng đường Haversine theo tuyến.

    Args:
        office: dict với lat/lng của bưu cục
        orders: danh sách dict với lat/lng của các điểm dừng
        include_return_to_office: nếu True, cộng thêm quãng từ last stop về office
    """
    if not orders:
        return 0.0
    legs = [office, *orders]
    total_m = 0
    for i in range(len(legs) - 1):
        total_m += distance_meter(legs[i], legs[i + 1])
    if include_return_to_office:
        total_m += distance_meter(legs[-1], office)
    return total_m / 1000.0


def haversine_return_duration_seconds(
    last_stop: dict,
    office: dict,
    speed_kmh: float = DEFAULT_FALLBACK_SPEED_KMH,
) -> int:
    """Thời gian từ last stop quay về office."""
    return haversine_duration_seconds(last_stop, office, speed_kmh)


def haversine_distance_km(a: dict, b: dict) -> float:
    """Khoảng cách km giữa 2 điểm Haversine."""
    return distance_meter(a, b) / 1000.0
