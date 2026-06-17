from __future__ import annotations

import logging
from dataclasses import dataclass

import googlemaps
import polyline
from googlemaps.exceptions import ApiError, HTTPError, Timeout

from app.config.settings import Settings
from app.models.route_optimization import OfficeLocation, OrderInput
from app.services.geo_utils import (
    build_haversine_duration_matrix,
    haversine_duration_seconds,
)

logger = logging.getLogger(__name__)

MATRIX_SOURCE_GOOGLE = "GOOGLE_DURATION_MATRIX"
MATRIX_SOURCE_MIXED = "MIXED_WITH_FALLBACK"
MATRIX_SOURCE_HAVERSINE = "HAVERSINE_FALLBACK"

# Google Distance Matrix: toi da 25 diem xuat phat × 25 diem den moi request
_MAX_MATRIX_NODES = 25
_MAX_CACHE_ENTRIES = 64


@dataclass(frozen=True)
class DurationMatrixResult:
    duration_matrix: list[list[int]]
    fallback_used: bool
    matrix_source: str


class GoogleMapsService:
    def __init__(self, settings: Settings):
        self._settings = settings
        self._client = googlemaps.Client(key=settings.google_maps_api_key) if settings.google_maps_api_key else None
        self._duration_matrix_cache: dict[str, DurationMatrixResult] = {}

    def _coord_key(self, lat: float, lng: float) -> str:
        precision = self._settings.duration_matrix_coord_precision
        return f"{round(lat, precision)},{round(lng, precision)}"

    def _locations_from_depot_stops(
        self,
        depot: OfficeLocation,
        stops: list[OrderInput],
    ) -> list[dict]:
        return [
            {"lat": depot.latitude, "lng": depot.longitude},
            *[ {"lat": s.latitude, "lng": s.longitude} for s in stops],
        ]

    def _cache_key(self, locations: list[dict]) -> str:
        mode = self._settings.duration_matrix_travel_mode
        parts = "|".join(self._coord_key(loc["lat"], loc["lng"]) for loc in locations)
        return f"{mode}:{parts}"

    def _get_cached(self, key: str) -> DurationMatrixResult | None:
        return self._duration_matrix_cache.get(key)

    def _set_cached(self, key: str, result: DurationMatrixResult) -> None:
        if key in self._duration_matrix_cache:
            return
        if len(self._duration_matrix_cache) >= _MAX_CACHE_ENTRIES:
            oldest = next(iter(self._duration_matrix_cache))
            del self._duration_matrix_cache[oldest]
        self._duration_matrix_cache[key] = result

    def _haversine_matrix_result(self, locations: list[dict]) -> DurationMatrixResult:
        matrix = build_haversine_duration_matrix(
            locations,
            speed_kmh=self._settings.duration_matrix_avg_speed_kmh,
        )
        return DurationMatrixResult(
            duration_matrix=matrix,
            fallback_used=True,
            matrix_source=MATRIX_SOURCE_HAVERSINE,
        )

    def _pair_fallback_duration(self, from_loc: dict, to_loc: dict) -> int:
        return haversine_duration_seconds(
            from_loc,
            to_loc,
            speed_kmh=self._settings.duration_matrix_avg_speed_kmh,
        )

    def build_duration_matrix(
        self,
        depot: OfficeLocation,
        stops: list[OrderInput],
    ) -> DurationMatrixResult:
        """
        Tạo ma trận chi phí thời gian (giây) cho OR-Tools.
        Nút 0 là depot, các nút còn lại là điểm dừng.
        """
        locations = self._locations_from_depot_stops(depot, stops)
        size = len(locations)

        if size <= 1:
            return DurationMatrixResult(
                duration_matrix=[[0]],
                fallback_used=False,
                matrix_source=MATRIX_SOURCE_HAVERSINE,
            )

        cache_key = self._cache_key(locations)
        cached = self._get_cached(cache_key)
        if cached is not None:
            logger.debug("Duration matrix cache hit (%s nodes)", size)
            return cached

        if not self._client:
            logger.warning("GOOGLE_MAPS_API_KEY missing; Haversine duration matrix fallback")
            result = self._haversine_matrix_result(locations)
            self._set_cached(cache_key, result)
            return result

        if size > _MAX_MATRIX_NODES:
            logger.warning(
                "Too many nodes (%s) for Google Distance Matrix; Haversine fallback",
                size,
            )
            result = self._haversine_matrix_result(locations)
            self._set_cached(cache_key, result)
            return result

        lat_lngs = [(loc["lat"], loc["lng"]) for loc in locations]
        mode = self._settings.duration_matrix_travel_mode

        try:
            response = self._client.distance_matrix(
                origins=lat_lngs,
                destinations=lat_lngs,
                mode=mode,
            )
        except (ApiError, HTTPError, Timeout, Exception) as exc:
            logger.warning("Google Distance Matrix API failed: %s; Haversine fallback", exc)
            result = self._haversine_matrix_result(locations)
            self._set_cached(cache_key, result)
            return result

        if response.get("status") != "OK":
            logger.warning(
                "Google Distance Matrix status=%s; Haversine fallback",
                response.get("status"),
            )
            result = self._haversine_matrix_result(locations)
            self._set_cached(cache_key, result)
            return result

        matrix: list[list[int]] = []
        pair_fallback = False

        for i, row in enumerate(response.get("rows", [])):
            matrix_row: list[int] = []
            for j, element in enumerate(row.get("elements", [])):
                if i == j:
                    matrix_row.append(0)
                    continue

                status = element.get("status", "UNKNOWN")
                duration = element.get("duration")

                if status == "OK" and duration and "value" in duration:
                    matrix_row.append(int(duration["value"]))
                else:
                    pair_fallback = True
                    matrix_row.append(
                        self._pair_fallback_duration(locations[i], locations[j])
                    )
                    logger.debug(
                        "Duration matrix pair fallback i=%s j=%s status=%s",
                        i,
                        j,
                        status,
                    )

            matrix.append(matrix_row)

        if len(matrix) != size:
            logger.warning("Google matrix size mismatch; Haversine fallback")
            result = self._haversine_matrix_result(locations)
            self._set_cached(cache_key, result)
            return result

        if pair_fallback:
            matrix_source = MATRIX_SOURCE_MIXED
            fallback_used = True
        else:
            matrix_source = MATRIX_SOURCE_GOOGLE
            fallback_used = False

        result = DurationMatrixResult(
            duration_matrix=matrix,
            fallback_used=fallback_used,
            matrix_source=matrix_source,
        )
        self._set_cached(cache_key, result)
        logger.info(
            "Duration matrix built nodes=%s source=%s fallback_used=%s",
            size,
            matrix_source,
            fallback_used,
        )
        return result

    def get_driving_route(
        self,
        office: OfficeLocation,
        route_orders: list[OrderInput],
    ) -> tuple[list[tuple[float, float]], float, float, str | None]:
        """
        Gọi Google Directions API để lấy tuyến thực tế.
        Trả về điểm polyline, quãng đường, thời gian và encoded polyline.
        """
        if not route_orders:
            return [], 0.0, 0.0, None

        if not self._client:
            logger.warning("GOOGLE_MAPS_API_KEY missing; using straight-line fallback")
            points = [(office.latitude, office.longitude)] + [
                (o.latitude, o.longitude) for o in route_orders
            ]
            return points, 0.0, 0.0, None

        origin = (office.latitude, office.longitude)
        destination = (route_orders[-1].latitude, route_orders[-1].longitude)
        waypoints = [(o.latitude, o.longitude) for o in route_orders[:-1]]

        try:
            directions_result = self._client.directions(
                origin=origin,
                destination=destination,
                waypoints=waypoints,
                mode="driving",
                language="vi",
            )
        except Exception as exc:
            logger.exception("Google Directions API failed: %s", exc)
            return [], 0.0, 0.0, None

        if not directions_result:
            return [], 0.0, 0.0, None

        route = directions_result[0]
        real_distance_km = sum(leg["distance"]["value"] for leg in route["legs"]) / 1000
        real_duration_minutes = sum(leg["duration"]["value"] for leg in route["legs"]) / 60
        encoded = route["overview_polyline"]["points"]
        decoded = polyline.decode(encoded)
        return decoded, real_distance_km, real_duration_minutes, encoded
