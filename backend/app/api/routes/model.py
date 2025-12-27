"""
AI 模型配置接口
"""

from fastapi import APIRouter

from app.core.settings import get_settings
from app.schema.base import ApiResult
from app.schema.model import ModelVo

router = APIRouter(prefix="/model", tags=["模型"])


@router.get("", response_model=ApiResult[list[ModelVo]])
async def list_models() -> ApiResult[list[ModelVo]]:
    """
    1. 返回可用模型列表，根据配置动态生成。
    """
    settings = get_settings()
    models: list[ModelVo] = []

    # DeepSeek 模型
    if settings.ai_deepseek_api_key:
        models.append(
            ModelVo(
                id=1,
                modelCode=settings.ai_deepseek_model_name,
                modelName="DeepSeek Chat",
                provider="deepseek",
                isDefault=(settings.ai_provider == "deepseek"),
                status=1,
            )
        )

    # OpenAI 模型
    if settings.ai_openai_api_key and settings.ai_openai_deployment_name:
        models.append(
            ModelVo(
                id=2,
                modelCode=settings.ai_openai_deployment_name,
                modelName="OpenAI GPT",
                provider="openai",
                isDefault=(settings.ai_provider == "openai"),
                status=1,
            )
        )

    # 中转站模型 (Custom)
    if settings.ai_custom_api_key:
        models.append(
            ModelVo(
                id=3,
                modelCode=settings.ai_custom_model_name,
                modelName=f"Custom ({settings.ai_custom_model_name})",
                provider="custom",
                isDefault=(settings.ai_provider == "custom"),
                status=1,
            )
        )

    # Google Gemini 模型
    if settings.ai_gemini_api_key:
        models.append(
            ModelVo(
                id=4,
                modelCode=settings.ai_gemini_model_name,
                modelName=f"Gemini ({settings.ai_gemini_model_name})",
                provider="gemini",
                isDefault=(settings.ai_provider == "gemini"),
                status=1,
            )
        )

    # 如果没有配置任何模型，返回占位
    if not models:
        models.append(
            ModelVo(
                id=0,
                modelCode="echo",
                modelName="回显模式 (未配置模型)",
                provider="local",
                isDefault=True,
                status=0,
            )
        )

    return ApiResult.ok(models)


@router.get("/default", response_model=ApiResult[ModelVo])
async def get_default_model() -> ApiResult[ModelVo]:
    """
    1. 返回默认模型。
    """
    result = await list_models()
    models = result.data or []
    default_model = next((m for m in models if m.isDefault), models[0] if models else None)
    if default_model is None:
        return ApiResult.error("MODEL-404", "无可用模型")
    return ApiResult.ok(default_model)
