import logging
import traceback

from fastapi import APIRouter, Depends, HTTPException, Request

from app.config.settings import Settings, get_settings
from app.models.recommendation import RecommendationRequest, RecommendationResponse
from app.services.recommendation_service import score_candidates

logger = logging.getLogger(__name__)

router = APIRouter(prefix="/api/v1/recommendations", tags=["recommendations"])


@router.post("/orders", response_model=RecommendationResponse)
async def recommend_orders(
    request: Request,
    settings: Settings = Depends(get_settings),
) -> RecommendationResponse:
    """Gợi ý đơn phù hợp cho shipper theo thuật toán chấm điểm đa tiêu chí.
    """
    try:
        body = await request.json()
    except Exception:
        body = None

    try:
        validated = (
            RecommendationRequest.model_validate(body) if body else RecommendationRequest()
        )
    except Exception as exc:
        logger.error("422 Pydantic validation thất bại – request_body=%s error=%s", body, exc)
        raise HTTPException(
            status_code=422,
            detail={
                "message": "Validation thất bại",
                "error": str(exc),
                "request_body": body,
            },
        ) from exc

    logger.info(
        "[AI][Recommendation] request candidates=%s current_orders=%s current_location=%s",
        len(validated.candidate_orders or []),
        len(validated.current_orders or []),
        validated.current_location
    )

    for candidate in validated.candidate_orders or []:
        logger.info(
            "[AI][Recommendation] candidate order_id=%s lat=%s lng=%s city_id=%s ward_id=%s weight=%s volume=%s urgent=%s",
            candidate.order_id,
            candidate.latitude,
            candidate.longitude,
            candidate.recipient_city_code,
            candidate.recipient_ward_code,
            candidate.weight_kg,
            candidate.volume_m3,
            candidate.is_urgent
        )

    logger.debug(
        "[AI][Recommendation] request_payload=%s",
        validated.model_dump_json()
    )

    if not validated.candidate_orders:
        return RecommendationResponse(
            success=True,
            message="No candidate orders",
            recommendations=[],
            location_source="OFFICE",
            fallback_location_source="OFFICE",
        )

    try:
        return score_candidates(validated, settings)
    except Exception as exc:  # noqa: BLE001
        logger.exception("Chấm điểm gợi ý thất bại: %s", exc)
        traceback.print_exc()
        raise HTTPException(status_code=500, detail=f"Chấm điểm thất bại: {exc}") from exc
