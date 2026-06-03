from functools import lru_cache

from pydantic_settings import BaseSettings, SettingsConfigDict


class Settings(BaseSettings):
    model_config = SettingsConfigDict(env_file=".env", env_file_encoding="utf-8", extra="ignore")

    app_name: str = "Logistics AI Service"
    app_env: str = "development"
    app_host: str = "0.0.0.0"
    app_port: int = 8001
    log_level: str = "INFO"

    google_maps_api_key: str = ""

    default_shipper_capacity: int = 20
    default_shipper_weight_capacity_kg: float = 35.0
    default_shipper_speed_kmh: float = 25.0
    default_fuel_cost_per_km: float = 3000.0
    default_start_time: str = "08:00"
    ortools_time_limit_seconds: int = 8
    ortools_guided_search_time_limit_seconds: int = 5

    duration_matrix_travel_mode: str = "driving"
    duration_matrix_avg_speed_kmh: float = 30.0
    duration_matrix_coord_precision: int = 5

    enable_assignment_debug_log: bool = False


@lru_cache
def get_settings() -> Settings:
    return Settings()
