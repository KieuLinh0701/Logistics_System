from __future__ import annotations

from typing import List, Optional

from pydantic import BaseModel, ConfigDict, Field


class _BaseAliasModel(BaseModel):
    model_config = ConfigDict(populate_by_name=True)


class RecommendationLocation(_BaseAliasModel):
    """Vị trí tham chiếu dùng để chấm điểm.

    `source` giúp backend truy vết cách fallback vị trí (OFFICE, LAST_STOP, CUSTOM, NONE).
    """

    source: str = "OFFICE"
    latitude: float
    longitude: float


class RecommendationLoad(_BaseAliasModel):
    weight: float = 0.0
    volume: float = 0.0


class RecommendationVehicleCapacity(_BaseAliasModel):
    weight: float = 0.0
    volume: float = 0.0
    max_orders: int = 0
    current_orders: int = 0


class RecommendationCurrentOrder(_BaseAliasModel):
    order_id: int = Field(alias="orderId")
    recipient_ward_code: Optional[int] = Field(default=None, alias="recipientWardCode")
    recipient_city_code: Optional[int] = Field(default=None, alias="recipientCityCode")
    status: Optional[str] = None


class RecommendationCandidateOrder(_BaseAliasModel):
    order_id: int = Field(alias="orderId")
    weight_kg: float = Field(default=0.0, alias="weightKg")
    volume_m3: float = Field(default=0.0, alias="volumeM3")
    latitude: Optional[float] = None
    longitude: Optional[float] = None
    recipient_ward_code: Optional[int] = Field(default=None, alias="recipientWardCode")
    recipient_city_code: Optional[int] = Field(default=None, alias="recipientCityCode")
    status: Optional[str] = None
    is_urgent: bool = Field(default=False, alias="isUrgent")
    # Destination semantics for this candidate.
    # - RECIPIENT: giao thường, điểm đến là người nhận (mặc định, tương thích ngược)
    # - SENDER_RETURN: đơn hoàn, điểm đến là shop/người gửi
    # - PICKUP_SENDER: yêu cầu lấy hàng tại nhà, điểm đến là người gửi
    destination_type: str = Field(default="RECIPIENT", alias="destinationType")


class RecommendationRequest(_BaseAliasModel):
    shipper_id: int = Field(alias="shipperId")
    current_location: Optional[RecommendationLocation] = Field(
        default=None, alias="currentLocation"
    )
    current_load: RecommendationLoad = Field(
        default_factory=RecommendationLoad, alias="currentLoad"
    )
    vehicle_capacity: RecommendationVehicleCapacity = Field(
        default_factory=RecommendationVehicleCapacity, alias="vehicleCapacity"
    )
    current_orders: List[RecommendationCurrentOrder] = Field(
        default_factory=list, alias="currentOrders"
    )
    candidate_orders: List[RecommendationCandidateOrder] = Field(
        default_factory=list, alias="candidateOrders"
    )


class RecommendationItem(_BaseAliasModel):
    order_id: int = Field(alias="orderId")
    score: int
    level: str  # HIGH | MEDIUM | LOW | NOT_RECOMMENDED | OVER_CAPACITY
    reasons: List[str] = Field(default_factory=list)
    estimated_distance_km: Optional[float] = Field(default=None, alias="estimatedDistanceKm")
    estimated_duration_minutes: Optional[int] = Field(default=None, alias="estimatedDurationMinutes")
    recommended: bool = False


class RecommendationResponse(_BaseAliasModel):
    success: bool = True
    message: str = "OK"
    recommendations: List[RecommendationItem] = Field(default_factory=list)
    fallback_location_source: str = Field(
        default="OFFICE", alias="fallbackLocationSource"
    )
    location_source: str = Field(default="OFFICE", alias="locationSource")
