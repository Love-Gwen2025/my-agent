# Couple-Agent Dev Script (Windows PowerShell)
# Usage: .\dev.ps1 <command>

param(
    [Parameter(Position=0)]
    [string]$Command = "help"
)

function Show-Help {
    Write-Host ""
    Write-Host "==================== Couple-Agent Commands ====================" -ForegroundColor Cyan
    Write-Host ""
    Write-Host "  Install:" -ForegroundColor Yellow
    Write-Host "    .\dev.ps1 install          - Install all dependencies"
    Write-Host "    .\dev.ps1 backend-install  - Install backend only"
    Write-Host "    .\dev.ps1 frontend-install - Install frontend only"
    Write-Host ""
    Write-Host "  Run:" -ForegroundColor Yellow
    Write-Host "    .\dev.ps1 run              - Start backend (dev mode)"
    Write-Host "    .\dev.ps1 frontend-run     - Start frontend"
    Write-Host ""
    Write-Host "  Code Quality:" -ForegroundColor Yellow
    Write-Host "    .\dev.ps1 lint             - Run linter (ruff)"
    Write-Host "    .\dev.ps1 format           - Format code"
    Write-Host "    .\dev.ps1 test             - Run tests"
    Write-Host ""
    Write-Host "  Database:" -ForegroundColor Yellow
    Write-Host "    .\dev.ps1 db-migrate       - Run migrations"
    Write-Host ""
    Write-Host "  Clean:" -ForegroundColor Yellow
    Write-Host "    .\dev.ps1 clean            - Clean cache files"
    Write-Host ""
    Write-Host "================================================================" -ForegroundColor Cyan
}

function Install-All {
    Install-Backend
    Install-Frontend
    Write-Host "[OK] All dependencies installed" -ForegroundColor Green
}

function Install-Backend {
    Write-Host "[*] Installing backend dependencies..." -ForegroundColor Blue
    Push-Location backend
    uv sync
    Pop-Location
}

function Install-Frontend {
    Write-Host "[*] Installing frontend dependencies..." -ForegroundColor Blue
    Push-Location frontend
    npm install
    Pop-Location
}

function Start-Backend {
    Write-Host "[*] Starting backend (dev mode)..." -ForegroundColor Green
    Push-Location backend
    uv run uvicorn app.main:app --host 0.0.0.0 --port 8080 --reload
    Pop-Location
}

function Start-Frontend {
    Write-Host "[*] Starting frontend..." -ForegroundColor Green
    Push-Location frontend
    npm run dev
    Pop-Location
}

function Invoke-Lint {
    Write-Host "[*] Running linter..." -ForegroundColor Yellow
    Push-Location backend
    uv run ruff check .
    Pop-Location
}

function Invoke-Format {
    Write-Host "[*] Formatting code..." -ForegroundColor Yellow
    Push-Location backend
    uv run ruff check --fix .
    uv run black .
    Pop-Location
}

function Invoke-Test {
    Write-Host "[*] Running tests..." -ForegroundColor Yellow
    Push-Location backend
    uv run pytest -v
    Pop-Location
}

function Invoke-DbMigrate {
    Write-Host "[*] Running database migrations..." -ForegroundColor Blue
    Push-Location backend
    uv run alembic upgrade head
    Pop-Location
}

function Invoke-Clean {
    Write-Host "[*] Cleaning cache..." -ForegroundColor Yellow
    Get-ChildItem -Path . -Directory -Recurse -Filter "__pycache__" -ErrorAction SilentlyContinue | Remove-Item -Recurse -Force -ErrorAction SilentlyContinue
    Get-ChildItem -Path . -Directory -Recurse -Filter ".pytest_cache" -ErrorAction SilentlyContinue | Remove-Item -Recurse -Force -ErrorAction SilentlyContinue
    Get-ChildItem -Path . -Directory -Recurse -Filter ".ruff_cache" -ErrorAction SilentlyContinue | Remove-Item -Recurse -Force -ErrorAction SilentlyContinue
    Write-Host "[OK] Clean complete" -ForegroundColor Green
}

switch ($Command.ToLower()) {
    "help"              { Show-Help }
    "install"           { Install-All }
    "backend-install"   { Install-Backend }
    "frontend-install"  { Install-Frontend }
    "run"               { Start-Backend }
    "backend-run"       { Start-Backend }
    "frontend-run"      { Start-Frontend }
    "lint"              { Invoke-Lint }
    "format"            { Invoke-Format }
    "test"              { Invoke-Test }
    "db-migrate"        { Invoke-DbMigrate }
    "clean"             { Invoke-Clean }
    default             { 
        Write-Host "[ERROR] Unknown command: $Command" -ForegroundColor Red
        Show-Help 
    }
}
