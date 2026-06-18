import logging
from datetime import datetime, timezone

from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware

from app.api.routes.optimization import router as optimization_router
from app.config.settings import get_settings

settings = get_settings()
logging.basicConfig(level=settings.log_level)
logger = logging.getLogger(__name__)

app = FastAPI(title=settings.app_name, version="1.0.0")

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

app.include_router(optimization_router)


@app.get("/")
def root():
    return {"service": settings.app_name, "status": "running"}


@app.get("/health")
def health_check():
    """Health check endpoint cho monitoring tools (UptimeRobot, BetterStack...)."""
    timestamp = datetime.now(timezone.utc).isoformat()
    logger.info(f"Health check called at {timestamp}")
    return {"status": "ok", "timestamp": timestamp}
