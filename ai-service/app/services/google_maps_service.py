from __future__ import annotations

import logging
from dataclasses import dataclass

import googlemaps
import polyline
from googlemaps.exceptions import ApiError, HTTPError, Timeout

from app.config.settings import Settings
from app.models.route_optimization import OrderInput, RouteStopInput
from app.services.geo_utils import (
    build_haversine_duration_matrix,
    haversine_distance_km,
    haversine_duration_seconds,
)

logger = logging.getLogger(__name__)

MATRIX_SOURCE_GOOGLE = "GOOGLE_DURATION_MATRIX"
MATRIX_SOURCE_MIXED = "MIXED_WITH_FALLBACK"
MATRIX_SOURCE_HAVERSINE = "HAVERSINE_FALLBACK"

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

    def _haversine_fallback_polyline(
        self,
        start_location: dict,
        ordered_stops: list,
        end_location: dict | None = None,
    ) -> list[tuple[float, float]]:
        """Tạo polyline đường thẳng từ Haversine points làm fallback."""
        points = [(start_location["lat"], start_location["lng"])]
        for stop in ordered_stops:
            points.append((stop.latitude, stop.longitude))
        if end_location:
            points.append((end_location["lat"], end_location["lng"]))
        else:
            points.append((start_location["lat"], start_location["lng"]))
        return points

    def build_duration_matrix(
        self,
        start_location: dict,
        stops: list[OrderInput | RouteStopInput],
        end_location: dict | None = None,
    ) -> DurationMatrixResult:
        locations = [{"lat": stop.latitude, "lng": stop.longitude} for stop in stops]

        # Nếu closed loop: thêm start và end (cùng office) vào matrix
        # Matrix nodes = start + stops + end
        full_locations = []
        if start_location:
            full_locations.append(start_location)
        full_locations.extend(locations)
        if end_location:
            full_locations.append(end_location)

        size = len(full_locations)
        if size <= 1:
            return DurationMatrixResult(
                duration_matrix=[[0]],
                fallback_used=False,
                matrix_source=MATRIX_SOURCE_HAVERSINE,
            )

        cache_key = self._cache_key(full_locations)
        cached = self._get_cached(cache_key)
        if cached is not None:
            logger.debug("Duration matrix cache hit (%s nodes)", size)
            return cached

        if not self._client:
            logger.warning("GOOGLE_MAPS_API_KEY missing; Haversine duration matrix fallback")
            result = self._haversine_matrix_result(full_locations)
            self._set_cached(cache_key, result)
            return result

        if size > _MAX_MATRIX_NODES:
            logger.warning(
                "Too many nodes (%s) for Google Distance Matrix; Haversine fallback",
                size,
            )
            result = self._haversine_matrix_result(full_locations)
            self._set_cached(cache_key, result)
            return result

        lat_lngs = [(loc["lat"], loc["lng"]) for loc in full_locations]
        mode = self._settings.duration_matrix_travel_mode

        try:
            response = self._client.distance_matrix(
                origins=lat_lngs,
                destinations=lat_lngs,
                mode=mode,
            )
        except (ApiError, HTTPError, Timeout, Exception) as exc:
            logger.warning("Google Distance Matrix API failed: %s; Haversine fallback", exc)
            result = self._haversine_matrix_result(full_locations)
            self._set_cached(cache_key, result)
            return result

        if response.get("status") != "OK":
            logger.warning(
                "Google Distance Matrix status=%s; Haversine fallback",
                response.get("status"),
            )
            result = self._haversine_matrix_result(full_locations)
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
                        self._pair_fallback_duration(full_locations[i], full_locations[j])
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
            result = self._haversine_matrix_result(full_locations)
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
        start_location: dict,
        ordered_stops: list[OrderInput | RouteStopInput],
        end_location: dict | None = None,
        route_mode: str = "CLOSED_LOOP",
    ) -> tuple[list[tuple[float, float]], float, float, str | None, float, float]:
        """Gọi Google Directions API để lấy tuyến thực tế.

        Args:
            start_location: dict với lat/lng điểm xuất phát
            ordered_stops: danh sách stops đã được sắp xếp
            end_location: dict với lat/lng điểm kết thúc (None = dùng start_location)
            route_mode: CLOSED_LOOP hoặc OPEN_ROUTE

        Returns:
            (decoded_polyline, total_distance_km, total_duration_minutes,
             encoded_polyline, return_distance_km, return_duration_minutes)
        """
        if not ordered_stops:
            return [], 0.0, 0.0, None, 0.0, 0.0

        if not self._client:
            logger.warning("GOOGLE_MAPS_API_KEY missing; using straight-line fallback")
            points = [(start_location["lat"], start_location["lng"])]
            for stop in ordered_stops:
                points.append((stop.latitude, stop.longitude))
            if route_mode == "CLOSED_LOOP" and end_location:
                points.append((end_location["lat"], end_location["lng"]))
            elif route_mode == "CLOSED_LOOP":
                points.append((start_location["lat"], start_location["lng"]))
            return points, 0.0, 0.0, None, 0.0, 0.0

        # Tính return leg từ last stop về office trước
        last_stop = {"lat": ordered_stops[-1].latitude, "lng": ordered_stops[-1].longitude}
        return_dest = end_location if end_location else start_location
        return_duration_seconds = haversine_duration_seconds(last_stop, return_dest)
        return_duration_minutes = return_duration_seconds / 60.0
        return_distance_km = haversine_distance_km(last_stop, return_dest)

        if route_mode == "OPEN_ROUTE":
            # OPEN: office → [1.1 → 1.2 → ... → 1.5], không quay về
            origin = (start_location["lat"], start_location["lng"])
            destination = (ordered_stops[-1].latitude, ordered_stops[-1].longitude)
            waypoints = [(s.latitude, s.longitude) for s in ordered_stops[:-1]]
            waypoints_str = "|".join(f"{lat},{lng}" for lat, lng in waypoints) if waypoints else None

            try:
                directions_result = self._client.directions(
                    origin=origin,
                    destination=destination,
                    waypoints=[waypoints_str] if waypoints_str else [],
                    mode="driving",
                    language="vi",
                    timeout=5,
                )
            except Exception as exc:
                logger.warning("[DIRECTIONS_TIMEOUT] OPEN_ROUTE Leg1 failed: %s. Using haversine fallback.", exc)
                decoded = self._haversine_fallback_polyline(start_location, ordered_stops, end_location)
                return decoded, 0.0, 0.0, None, 0.0, 0.0

            if not directions_result:
                logger.warning("[DIRECTIONS_EMPTY] OPEN_ROUTE Leg1 returned empty. Using haversine fallback.")
                decoded = self._haversine_fallback_polyline(start_location, ordered_stops, end_location)
                return decoded, 0.0, 0.0, None, 0.0, 0.0

            route = directions_result[0]
            total_distance_km = sum(leg["distance"]["value"] for leg in route["legs"]) / 1000
            total_duration_minutes = sum(leg["duration"]["value"] for leg in route["legs"]) / 60
            encoded = route["overview_polyline"]["points"]
            decoded = polyline.decode(encoded)
            return decoded, total_distance_km, total_duration_minutes, encoded, 0.0, 0.0

        # CLOSED_LOOP: tách thành 2 legs để đảm bảo stop cuối nằm trên polyline
        leg1_origin = (start_location["lat"], start_location["lng"])
        leg1_destination = (ordered_stops[-1].latitude, ordered_stops[-1].longitude)
        leg1_waypoints = [(s.latitude, s.longitude) for s in ordered_stops[:-1]]
        leg1_waypoints_str = "|".join(f"{lat},{lng}" for lat, lng in leg1_waypoints) if leg1_waypoints else None

        try:
            directions_leg1 = self._client.directions(
                origin=leg1_origin,
                destination=leg1_destination,
                waypoints=[leg1_waypoints_str] if leg1_waypoints_str else [],
                mode="driving",
                language="vi",
                timeout=5,
            )
        except Exception as exc:
            logger.warning("[DIRECTIONS_TIMEOUT] CLOSED_LOOP Leg1 failed: %s. Using haversine fallback.", exc)
            decoded = self._haversine_fallback_polyline(start_location, ordered_stops, end_location)
            return decoded, 0.0, 0.0, None, 0.0, 0.0

        if not directions_leg1:
            logger.warning("[DIRECTIONS_EMPTY] CLOSED_LOOP Leg1 returned empty. Using haversine fallback.")
            decoded = self._haversine_fallback_polyline(start_location, ordered_stops, end_location)
            return decoded, 0.0, 0.0, None, 0.0, 0.0

        leg1 = directions_leg1[0]
        leg1_distance_km = sum(leg["distance"]["value"] for leg in leg1["legs"]) / 1000
        leg1_duration_minutes = sum(leg["duration"]["value"] for leg in leg1["legs"]) / 60
        leg1_encoded = leg1["overview_polyline"]["points"]
        decoded_leg1 = polyline.decode(leg1_encoded)

        # Leg 2: last stop → office
        try:
            directions_leg2 = self._client.directions(
                origin=leg1_destination,
                destination=(return_dest["lat"], return_dest["lng"]),
                waypoints=[],
                mode="driving",
                language="vi",
                timeout=5,
            )
        except Exception as exc:
            logger.warning("[DIRECTIONS_TIMEOUT] CLOSED_LOOP Leg2 failed: %s. Using haversine fallback.", exc)
            decoded = self._haversine_fallback_polyline(start_location, ordered_stops, end_location)
            return decoded, 0.0, 0.0, None, 0.0, 0.0

        if not directions_leg2:
            logger.warning("[DIRECTIONS_EMPTY] CLOSED_LOOP Leg2 returned empty. Using haversine fallback.")
            decoded = self._haversine_fallback_polyline(start_location, ordered_stops, end_location)
            return decoded, 0.0, 0.0, None, 0.0, 0.0

        leg2 = directions_leg2[0]
        leg2_distance_km = sum(leg["distance"]["value"] for leg in leg2["legs"]) / 1000
        leg2_duration_minutes = sum(leg["duration"]["value"] for leg in leg2["legs"]) / 60
        decoded_leg2 = polyline.decode(leg2["overview_polyline"]["points"])

        # Ghép 2 legs: decoded_leg1 + decoded_leg2
        decoded = list(decoded_leg1) + list(decoded_leg2)

        encoded = polyline.encode(decoded)

        total_distance_km = leg1_distance_km + leg2_distance_km
        total_duration_minutes = leg1_duration_minutes + leg2_duration_minutes

        return decoded, total_distance_km, total_duration_minutes, encoded, return_distance_km, return_duration_minutes
