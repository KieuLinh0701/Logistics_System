from datetime import datetime, timedelta

from ortools.constraint_solver import pywrapcp, routing_enums_pb2

from app.models.route_optimization import OfficeLocation, OrderInput, ShipperInput
from app.services.geo_utils import build_haversine_duration_matrix, haversine_route_distance_km


def _format_time(start_time: str, minutes: float) -> str:
    base = datetime.strptime(start_time, "%H:%M")
    return (base + timedelta(minutes=minutes)).strftime("%H:%M")


def _locations_for_vrp(office: OfficeLocation, area_orders: list[OrderInput]) -> list[dict]:
    return [
        {"lat": office.latitude, "lng": office.longitude},
        *[{"lat": o.latitude, "lng": o.longitude} for o in area_orders],
    ]


def solve_route_for_shipper(
    office: OfficeLocation,
    shipper: ShipperInput,
    area_orders: list[OrderInput],
    time_limit_seconds: int,
    duration_matrix: list[list[int]] | None = None,
) -> tuple[list[OrderInput], int, float]:
    """
    Tối ưu thứ tự dừng cho một shipper bằng OR-Tools.
    Ma trận chi phí là thời gian di chuyển (giây).
    Trả về danh sách đã sắp xếp và thông tin thời gian/độ dài dự phòng.
    """
    if not area_orders:
        return [], 0, 0.0

    locations = _locations_for_vrp(office, area_orders)

    cost_matrix = duration_matrix
    if cost_matrix is None:
        cost_matrix = build_haversine_duration_matrix(locations)

    expected_size = len(locations)
    if len(cost_matrix) != expected_size or any(len(row) != expected_size for row in cost_matrix):
        raise ValueError(
            f"duration_matrix size mismatch: expected {expected_size}x{expected_size}, "
            f"got {len(cost_matrix)}"
        )

    manager = pywrapcp.RoutingIndexManager(len(cost_matrix), 1, 0)
    routing = pywrapcp.RoutingModel(manager)

    def duration_callback(from_index: int, to_index: int) -> int:
        from_node = manager.IndexToNode(from_index)
        to_node = manager.IndexToNode(to_index)
        return cost_matrix[from_node][to_node]

    transit_callback_index = routing.RegisterTransitCallback(duration_callback)
    routing.SetArcCostEvaluatorOfAllVehicles(transit_callback_index)

    demands = [0] + [1] * len(area_orders)

    def demand_callback(from_index: int) -> int:
        from_node = manager.IndexToNode(from_index)
        return demands[from_node]

    demand_callback_index = routing.RegisterUnaryTransitCallback(demand_callback)
    routing.AddDimensionWithVehicleCapacity(
        demand_callback_index,
        0,
        [shipper.capacity],
        True,
        "Capacity",
    )

    search_parameters = pywrapcp.DefaultRoutingSearchParameters()
    search_parameters.first_solution_strategy = (
        routing_enums_pb2.FirstSolutionStrategy.PATH_CHEAPEST_ARC
    )
    search_parameters.local_search_metaheuristic = (
        routing_enums_pb2.LocalSearchMetaheuristic.GUIDED_LOCAL_SEARCH
    )
    search_parameters.time_limit.seconds = time_limit_seconds

    solution = routing.SolveWithParameters(search_parameters)
    if solution is None:
        return [], 0, 0.0

    index = routing.Start(0)
    route_orders: list[OrderInput] = []
    total_duration_seconds = 0

    while not routing.IsEnd(index):
        previous_index = index
        index = solution.Value(routing.NextVar(index))
        total_duration_seconds += routing.GetArcCostForVehicle(previous_index, index, 0)
        node = manager.IndexToNode(index)
        if node != 0:
            route_orders.append(area_orders[node - 1])

    haversine_km = haversine_route_distance_km(
        {"lat": office.latitude, "lng": office.longitude},
        [{"lat": o.latitude, "lng": o.longitude} for o in route_orders],
    )

    return route_orders, total_duration_seconds, haversine_km


def build_stop_etas(
    shipper: ShipperInput,
    ordered_orders: list[OrderInput],
    total_duration_minutes: float,
) -> list[tuple[str, int]]:
    """Ước tính ETA cho từng điểm dừng theo tổng thời gian tuyến."""
    if not ordered_orders:
        return []
    n = len(ordered_orders)
    etas: list[tuple[str, int]] = []
    for idx, _ in enumerate(ordered_orders, start=1):
        minutes = int(total_duration_minutes * idx / n) if n else 0
        etas.append((_format_time(shipper.start_time, minutes), minutes))
    return etas
