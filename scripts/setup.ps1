$ErrorActionPreference = "Stop"
Set-Location (Join-Path $PSScriptRoot "../server")
if (!(Test-Path .env)) { Copy-Item .env.example .env }
npm install
Write-Host "Backend dependencies installed. Run npm start in server/."
