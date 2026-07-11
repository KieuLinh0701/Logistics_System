from typing import Any

from pydantic import BaseModel, ConfigDict, Field


class _BaseAliasModel(BaseModel):
    model_config = ConfigDict(populate_by_name=True)


class AiLocationDto(_BaseAliasModel):
    type: str = "OFFICE"  # OFFICE, CURRENT_POSITION, CUSTOM
    id: int | None = None
    name: str | None = None
    latitude: float
    longitude: float


class OfficeLocation(_BaseAliasModel):
    id: int
    name: str
    address: str | None = None
    latitude: float
    longitude: float


class ShipperAssignmentArea(_BaseAliasModel):
    ward_code: int | None = Field(default=None, alias="wardCode")
    city_code: int = Field(alias="cityCode")


class ShipperInput(_BaseAliasModel):
    id: int
    employee_id: int = Field(alias="employeeId")
    name: str
    capacity: int = 20
    speed_kmh: float = Field(default=25.0, alias="speedKmh")
    fuel_cost_per_km: float = Field(default=3000.0, alias="fuelCostPerKm")
    start_time: str = Field(default="08:00", alias="startTime")
    vehicle_type: str | None = Field(default=None, alias="vehicleType")
    max_weight_kg: float | None = Field(default=None, alias="maxWeightKg")
    remaining_weight_kg: float | None = Field(default=None, alias="remainingWeightKg")
    battery_level: int | None = Field(default=None, alias="batteryLevel")
    assignments: list[ShipperAssignmentArea] = Field(default_factory=list)


class OrderInput(_BaseAliasModel):
    id: int
    tracking_number: str = Field(alias="trackingNumber")
    recipient_name: str = Field(alias="recipientName")
    recipient_phone: str | None = Field(default=None, alias="recipientPhone")
    recipient_address: str = Field(alias="recipientAddress")
    recipient_ward_code: int = Field(alias="recipientWardCode")
    recipient_city_code: int = Field(alias="recipientCityCode")
    latitude: float
    longitude: float
    cod_amount: int = Field(default=0, alias="codAmount")
    priority: str = "NORMAL"
    weight_kg: float = Field(default=1.0, alias="weightKg")


class RouteStopInput(_BaseAliasModel):
    stop_id: int | None = Field(default=None, alias="stopId")
    order_id: int | None = Field(default=None, alias="orderId")
    tracking_number: str | None = Field(default=None, alias="trackingNumber")
    stop_type: str = "DELIVERY"  # DELIVERY, PICKUP
    recipient_name: str | None = Field(default=None, alias="recipientName")
    recipient_phone: str | None = Field(default=None, alias="recipientPhone")
    address: str | None = None
    ward_code: int | None = Field(default=None, alias="wardCode")
    city_code: int | None = Field(default=None, alias="cityCode")
    latitude: float
    longitude: float
    cod_amount: int = Field(default=0, alias="codAmount")
    priority: str = "NORMAL"
    service_time_minutes: int = Field(default=5, alias="serviceTimeMinutes")
    weight_kg: float = Field(default=1.0, alias="weightKg")


class RouteOptimizationRequest(_BaseAliasModel):
    office: OfficeLocation
    start_location: AiLocationDto | None = None
    end_location: AiLocationDto | None = None
    return_to_office: bool = True
    route_mode: str = "CLOSED_LOOP"  # CLOSED_LOOP, OPEN_ROUTE
    optimization_scope: str = "MANAGER_GLOBAL"  # MANAGER_GLOBAL, SHIPPER_LOCAL, PICKUP_INSERTION
    shippers: list[ShipperInput] = Field(default_factory=list)
    orders: list[OrderInput] = Field(default_factory=list)
    stops: list[RouteStopInput] = Field(default_factory=list)
    options: dict[str, Any] = Field(default_factory=dict)


class RouteStopOutput(_BaseAliasModel):
    order_id: int | None = Field(default=None, alias="orderId")
    tracking_number: str | None = None
    recipient_name: str | None = None
    recipient_phone: str | None = None
    recipient_address: str | None = None
    latitude: float
    longitude: float
    cod_amount: int = Field(default=0, alias="codAmount")
    priority: str = "NORMAL"
    stop_sequence: int
    stop_type: str = "DELIVERY"  # DELIVERY, PICKUP, RETURN_TO_OFFICE
    eta_time: str | None = None
    eta_minutes_from_start: int | None = None
    leg_distance_km: float | None = None
    leg_duration_minutes: int | None = None
    service_time_minutes: int | None = None


class ShipperRouteOutput(_BaseAliasModel):
    shipper_id: int
    employee_id: int
    shipper_name: str
    route_sequence: int
    stops: list[RouteStopOutput] = Field(default_factory=list)
    return_to_office_stop: RouteStopOutput | None = None
    estimated_distance_km: float = 0.0
    estimated_duration_minutes: float = 0.0
    fuel_cost: float = 0.0
    total_cod: int = 0
    encoded_polyline: str | None = None
    start_time: str = "08:00"
    route_mode: str = "CLOSED_LOOP"
    return_to_office: bool = True
    matrix_source: str | None = None
    fallback_used: bool | None = None
    optimizer_duration_seconds: int | None = None
    cost_mode: str = "DURATION"


class UnassignedOrderOutput(_BaseAliasModel):
    order_id: int
    tracking_number: str
    reason: str


class OptimizationSummary(_BaseAliasModel):
    total_distance_km: float = 0.0
    total_duration_minutes: float = 0.0
    total_fuel_cost: float = 0.0
    total_cod: int = 0
    assigned_order_count: int = 0
    unassigned_order_count: int = 0
    shipper_count: int = 0


class RouteOptimizationResponse(_BaseAliasModel):
    success: bool = True
    message: str = "Optimization completed"
    routes: list[ShipperRouteOutput] = Field(default_factory=list)
    unassigned_orders: list[UnassignedOrderOutput] = Field(default_factory=list)
    summary: OptimizationSummary = Field(default_factory=OptimizationSummary)
