from datetime import datetime, timedelta

from ortools.constraint_solver import pywrapcp, routing_enums_pb2
import logging

logger = logging.getLogger(__name__)

from app.models.route_optimization import OrderInput, RouteStopInput, ShipperInput
from app.services.geo_utils import (
    build_haversine_duration_matrix,
    haversine_duration_seconds,
    haversine_route_distance_km,
)


def _format_time(start_time: str, minutes: float) -> str:
    base = datetime.strptime(start_time, "%H:%M")
    return (base + timedelta(minutes=minutes)).strftime("%H:%M")


def _locations_for_vrp(
    start_location: dict,
    area_orders: list,
    end_location: dict | None = None,
) -> list[dict]:
    locs = [{"lat": start_location["lat"], "lng": start_location["lng"]}]
    locs.extend({"lat": o.latitude, "lng": o.longitude} for o in area_orders)
    if end_location:
        locs.append({"lat": end_location["lat"], "lng": end_location["lng"]})
    return locs


def _build_distance_matrix_from_locations(locations: list[dict], speed_kmh: float = 30.0) -> list[list[int]]:
    return build_haversine_duration_matrix(locations, speed_kmh=speed_kmh)


def solve_route_for_shipper(
    start_location: dict,
    shipper: ShipperInput,
    area_orders: list,
    time_limit_seconds: int,
    duration_matrix: list[list[int]] | None = None,
    return_to_office: bool = True,
    end_location: dict | None = None,
    start_time: str = "08:00",
    optimization_scope: str | None = None,
) -> tuple[list, int, float, int]:
    """
    Giai VRP cho mot shipper bang OR-Tools.
    Cau truc matrix: [START(VI_TRI_HIEN_TAI), ...stops..., END(BUU_CUC)].
    RoutingIndexManager su dung explicit end node de dam bao route ket thuc tai BUU_CUC,
    chu khong phai tai START/VI_TRI_HIEN_TAI.
    """
    if not area_orders:
        return [], 0, 0.0, 0

    num_stops = len(area_orders)
    has_end_node = end_location is not None
    end_node_index = num_stops + 1 if has_end_node else None

    locations = _locations_for_vrp(start_location, area_orders, end_location)
    cost_matrix = duration_matrix
    if cost_matrix is None:
        cost_matrix = _build_distance_matrix_from_locations(locations)

    expected_size = len(locations)
    if len(cost_matrix) != expected_size or any(len(row) != expected_size for row in cost_matrix):
        raise ValueError(
            f"duration_matrix size mismatch: expected {expected_size}x{expected_size}, "
            f"got {len(cost_matrix)}"
        )

    if has_end_node:
        manager = pywrapcp.RoutingIndexManager(len(cost_matrix), 1, [0], [end_node_index])
    else:
        manager = pywrapcp.RoutingIndexManager(len(cost_matrix), 1, 0)
    routing = pywrapcp.RoutingModel(manager)

    def duration_callback(from_index: int, to_index: int) -> int:
        from_node = manager.IndexToNode(from_index)
        to_node = manager.IndexToNode(to_index)
        return cost_matrix[from_node][to_node]

    transit_callback_index = routing.RegisterTransitCallback(duration_callback)
    routing.SetArcCostEvaluatorOfAllVehicles(transit_callback_index)

    demands = [0] + [1] * num_stops
    if has_end_node:
        demands.append(0)

    def demand_callback(from_index: int) -> int:
        return demands[manager.IndexToNode(from_index)]

    demand_callback_index = routing.RegisterUnaryTransitCallback(demand_callback)
    routing.AddDimensionWithVehicleCapacity(
        demand_callback_index, 0, [shipper.capacity], True, "Capacity",
    )

    search_parameters = pywrapcp.DefaultRoutingSearchParameters()
    is_shipper_local = optimization_scope == "SHIPPER_LOCAL"
    if is_shipper_local:
        search_parameters.first_solution_strategy = (
            routing_enums_pb2.FirstSolutionStrategy.PARALLEL_CHEAPEST_INSERTION
        )
    else:
        search_parameters.first_solution_strategy = (
            routing_enums_pb2.FirstSolutionStrategy.PATH_CHEAPEST_ARC
        )
    search_parameters.local_search_metaheuristic = (
        routing_enums_pb2.LocalSearchMetaheuristic.GUIDED_LOCAL_SEARCH
    )
    search_parameters.time_limit.seconds = time_limit_seconds

    solution = routing.SolveWithParameters(search_parameters)
    if solution is None:
        logger.warning("[SOLVER] No solution found for shipper_id=%s", shipper.id)
        return [], 0, 0.0, 0

    index = routing.Start(0)
    route_orders: list = []
    total_duration_seconds = 0

    while not routing.IsEnd(index):
        next_var_value = solution.Value(routing.NextVar(index))
        next_node = manager.IndexToNode(next_var_value)
        leg_cost = routing.GetArcCostForVehicle(index, next_var_value, 0)
        total_duration_seconds += leg_cost
        index = next_var_value

        if 1 <= next_node <= num_stops:
            route_orders.append(area_orders[next_node - 1])

    return_duration_seconds = 0
    if has_end_node and route_orders:
        office_point = end_location if end_location else start_location
        last_stop = {"lat": route_orders[-1].latitude, "lng": route_orders[-1].longitude}
        return_duration_seconds = haversine_duration_seconds(last_stop, office_point)

    haversine_km = haversine_route_distance_km(
        start_location,
        [{"lat": o.latitude, "lng": o.longitude} for o in route_orders],
        include_return_to_office=False,
    )

    return route_orders, total_duration_seconds, haversine_km, return_duration_seconds


def build_stop_etas(
    shipper: ShipperInput,
    ordered_orders: list,
    total_duration_minutes: float,
    return_to_office: bool = True,
    return_duration_seconds: int = 0,
) -> list[tuple[str, int]]:
    if not ordered_orders:
        return []

    n = len(ordered_orders)
    delivery_duration_minutes = total_duration_minutes
    if return_to_office:
        delivery_duration_minutes = total_duration_minutes - (return_duration_seconds / 60.0)

    etas: list[tuple[str, int]] = []
    for idx, _ in enumerate(ordered_orders, start=1):
        minutes = int(delivery_duration_minutes * idx / n) if n else 0
        etas.append((_format_time(shipper.start_time, minutes), minutes))

    if return_to_office:
        total_minutes = int(total_duration_minutes)
        etas.append((_format_time(shipper.start_time, total_minutes), total_minutes))

    return etas
