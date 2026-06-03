from fastapi import APIRouter, Depends, HTTPException

from app.config.settings import Settings, get_settings
from app.models.route_optimization import RouteOptimizationRequest, RouteOptimizationResponse
from app.services.route_optimizer_service import RouteOptimizerService

router = APIRouter(prefix="/api/v1/optimization", tags=["optimization"])


def get_optimizer(settings: Settings = Depends(get_settings)) -> RouteOptimizerService:
    return RouteOptimizerService(settings)


@router.post("/route", response_model=RouteOptimizationResponse)
def optimize_route(
    request: RouteOptimizationRequest,
    optimizer: RouteOptimizerService = Depends(get_optimizer),
) -> RouteOptimizationResponse:
    """Tối ưu tuyến giao hàng dựa trên dữ liệu đầu vào."""
    if not request.office:
        raise HTTPException(status_code=400, detail="office is required")
    if not request.shippers:
        raise HTTPException(status_code=400, detail="At least one shipper is required")
    try:
        return optimizer.optimize(request)
    except Exception as exc:
        raise HTTPException(status_code=500, detail=f"Optimization failed: {exc}") from exc


@router.get("/health")
def health() -> dict:
    """Kiểm tra nhanh trạng thái dịch vụ."""
    return {"status": "ok", "service": "logistics-ai"}
