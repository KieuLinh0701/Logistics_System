from fastapi import APIRouter, Depends, HTTPException, Request

import traceback

from app.config.settings import Settings, get_settings
from app.models.route_optimization import RouteOptimizationRequest, RouteOptimizationResponse
from app.services.route_optimizer_service import RouteOptimizerService

router = APIRouter(prefix="/api/v1/optimization", tags=["optimization"])


def get_optimizer(settings: Settings = Depends(get_settings)) -> RouteOptimizerService:
    return RouteOptimizerService(settings)


@router.post("/route", response_model=RouteOptimizationResponse)
async def optimize_route(
    request: Request,
    optimizer: RouteOptimizerService = Depends(get_optimizer),
) -> RouteOptimizationResponse:
    """Tối ưu tuyến giao hàng dựa trên dữ liệu đầu vào."""
    try:
        body = await request.json()
    except Exception:
        body = None

    try:
        validated: RouteOptimizationRequest = RouteOptimizationRequest.model_validate(body) if body else None
    except Exception as exc:
        import logging
        logging.error("422 Pydantic validation thất bại – request_body=%s error=%s", body, exc)
        raise HTTPException(status_code=422, detail={
            "message": "Validation thất bại",
            "error": str(exc),
            "request_body": body,
        }) from exc

    if not validated.office:
        raise HTTPException(status_code=400, detail="office is required")
    if not validated.shippers:
        raise HTTPException(status_code=400, detail="Cần ít nhất một shipper")
    try:
        import logging
        logging.info("optimize_route: scope=%s body=%s", validated.optimization_scope, body)
        return optimizer.optimize(validated)
    except Exception as exc:
        import logging
        logging.exception("Tối ưu tuyến thất bại: %s", exc)
        traceback.print_exc()
        raise HTTPException(status_code=500, detail=f"Tối ưu thất bại: {exc}") from exc


@router.get("/health")
def health() -> dict:
    """Kiểm tra nhanh trạng thái dịch vụ."""
    return {"status": "ok", "service": "logistics-ai"}
