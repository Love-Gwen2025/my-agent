from fastapi import APIRouter

from app.api.routes import chat, checkpoint, conversation, model, user

api_router = APIRouter(prefix="/api")
api_router.include_router(user.router)
api_router.include_router(conversation.router)
api_router.include_router(chat.router)
api_router.include_router(model.router)
api_router.include_router(checkpoint.router)  # Checkpoint 分支路由

