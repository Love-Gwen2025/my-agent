# Couple-Agent Makefile
# 常用命令集合，简化开发流程

.PHONY: help install backend-install frontend-install run backend-run frontend-run test lint clean db-migrate

# 默认显示帮助
help:
	@echo "==================== Couple-Agent 命令 ===================="
	@echo ""
	@echo "  安装依赖:"
	@echo "    make install          - 安装所有依赖 (后端+前端)"
	@echo "    make backend-install  - 仅安装后端依赖"
	@echo "    make frontend-install - 仅安装前端依赖"
	@echo ""
	@echo "  启动服务:"
	@echo "    make run              - 启动后端服务 (开发模式)"
	@echo "    make frontend-run     - 启动前端服务"
	@echo ""
	@echo "  代码质量:"
	@echo "    make lint             - 代码检查 (ruff)"
	@echo "    make format           - 代码格式化 (ruff + black)"
	@echo "    make test             - 运行测试"
	@echo ""
	@echo "  数据库:"
	@echo "    make db-migrate       - 执行数据库迁移"
	@echo "    make db-revision      - 创建新的迁移文件"
	@echo ""
	@echo "  清理:"
	@echo "    make clean            - 清理缓存文件"
	@echo ""
	@echo "==========================================================="

# ============ 安装依赖 ============

install: backend-install frontend-install
	@echo "✅ 所有依赖安装完成"

backend-install:
	@echo "📦 安装后端依赖..."
	cd backend && uv sync

frontend-install:
	@echo "📦 安装前端依赖..."
	cd frontend && npm install

# ============ 启动服务 ============

run: backend-run

backend-run:
	@echo "🚀 启动后端服务 (开发模式)..."
	cd backend && uv run uvicorn app.main:app --host 0.0.0.0 --port 8080 --reload

frontend-run:
	@echo "🚀 启动前端服务..."
	cd frontend && npm run dev

# ============ 代码质量 ============

lint:
	@echo "🔍 代码检查..."
	cd backend && uv run ruff check .

format:
	@echo "✨ 代码格式化..."
	cd backend && uv run ruff check --fix .
	cd backend && uv run black .

test:
	@echo "🧪 运行测试..."
	cd backend && uv run pytest -v

# ============ 数据库 ============

db-migrate:
	@echo "🗄️ 执行数据库迁移..."
	cd backend && uv run alembic upgrade head

db-revision:
	@echo "📝 创建迁移文件..."
	@read -p "迁移描述: " msg && cd backend && uv run alembic revision --autogenerate -m "$$msg"

# ============ 清理 ============

clean:
	@echo "🧹 清理缓存..."
	find . -type d -name "__pycache__" -exec rm -rf {} + 2>/dev/null || true
	find . -type d -name ".pytest_cache" -exec rm -rf {} + 2>/dev/null || true
	find . -type d -name ".ruff_cache" -exec rm -rf {} + 2>/dev/null || true
	find . -type f -name "*.pyc" -delete 2>/dev/null || true
	@echo "✅ 清理完成"
