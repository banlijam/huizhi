$ErrorActionPreference = 'Stop'
Push-Location $PSScriptRoot
try {
    if (-not (Test-Path -LiteralPath '.env')) {
        Copy-Item -LiteralPath '.env.example' -Destination '.env'
        Write-Host 'Created .env from .env.example. Configure the HuizhiPay test API key before creating an order.'
    }
    node --experimental-sqlite src/server.js
} finally {
    Pop-Location
}
