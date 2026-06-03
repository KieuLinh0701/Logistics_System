from typing import Any

from pydantic import BaseModel, ConfigDict, Field


class _BaseAliasModel(BaseModel):
    model_config = ConfigDict(populate_by_name=True)


class OfficeLocation(_BaseAliasModel):
    id: int
    name: str
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


class RouteOptimizationRequest(_BaseAliasModel):
    office: OfficeLocation
    shippers: list[ShipperInput]
    orders: list[OrderInput]
    options: dict[str, Any] = Field(default_factory=dict)


class RouteStopOutput(_BaseAliasModel):
    order_id: int
    tracking_number: str
    recipient_name: str
    recipient_phone: str | None = None
    recipient_address: str
    latitude: float
    longitude: float
    cod_amount: int
    priority: str
    stop_sequence: int
    eta_time: str | None = None
    eta_minutes_from_start: int | None = None
    leg_distance_km: float | None = None


class ShipperRouteOutput(_BaseAliasModel):
    shipper_id: int
    employee_id: int
    shipper_name: str
    route_sequence: int
    stops: list[RouteStopOutput]
    estimated_distance_km: float = 0.0
    estimated_duration_minutes: float = 0.0
    fuel_cost: float = 0.0
    total_cod: int = 0
    encoded_polyline: str | None = None
    start_time: str = "08:00"
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
