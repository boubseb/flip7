# Script de diagnostic pour le problème de chargement du jeu

Write-Host "🔍 Diagnostic Flip7 Game Loading Issue" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# 1. Vérifier si le backend est en cours d'exécution
Write-Host "1. Vérification du backend..." -ForegroundColor Yellow
try {
    $response = Invoke-WebRequest -Uri "http://localhost:8080/actuator/health" -Method GET -ErrorAction Stop
    Write-Host "   ✅ Backend is running (Status: $($response.StatusCode))" -ForegroundColor Green
} catch {
    Write-Host "   ❌ Backend NOT running or not accessible" -ForegroundColor Red
    Write-Host "   Error: $($_.Exception.Message)" -ForegroundColor Red
    Write-Host ""
    Write-Host "   👉 Please start the backend with:" -ForegroundColor Yellow
    Write-Host "      cd backend" -ForegroundColor White
    Write-Host "      .\mvnw.cmd spring-boot:run" -ForegroundColor White
    exit 1
}

Write-Host ""

# 2. Vérifier si le frontend est en cours d'exécution
Write-Host "2. Vérification du frontend..." -ForegroundColor Yellow
try {
    $response = Invoke-WebRequest -Uri "http://localhost:4200" -Method GET -ErrorAction Stop
    Write-Host "   ✅ Frontend is running (Status: $($response.StatusCode))" -ForegroundColor Green
} catch {
    Write-Host "   ⚠️  Frontend NOT accessible" -ForegroundColor Yellow
    Write-Host "   Please start it with:" -ForegroundColor White
    Write-Host "      cd frontend" -ForegroundColor White
    Write-Host "      npm start" -ForegroundColor White
}

Write-Host ""

# 3. Instructions pour tester
Write-Host "3. Pour tester le chargement du jeu:" -ForegroundColor Yellow
Write-Host "   a) Ouvrez la console du navigateur (F12)" -ForegroundColor White
Write-Host "   b) Créez une nouvelle room" -ForegroundColor White
Write-Host "   c) Lancez la partie" -ForegroundColor White
Write-Host "   d) Observez les logs dans:" -ForegroundColor White
Write-Host "      - Console navigateur (Frontend)" -ForegroundColor Cyan
Write-Host "      - Terminal backend (Backend)" -ForegroundColor Cyan
Write-Host ""

Write-Host "4. Logs à chercher dans le backend:" -ForegroundColor Yellow
Write-Host "   🔍 GameService.getGame(...)" -ForegroundColor White
Write-Host "   🔍 Active games count: X" -ForegroundColor White
Write-Host "   🔍 GET /game/{roomId}/state" -ForegroundColor White
Write-Host ""

Write-Host "5. Logs à chercher dans le navigateur:" -ForegroundColor Yellow
Write-Host "   🔍 '📡 Fetching current game state...'" -ForegroundColor White
Write-Host "   🔍 '✅ Game state received...'" -ForegroundColor White
Write-Host "   🔍 Erreurs HTTP 400/404/500" -ForegroundColor White
Write-Host ""

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "Diagnostic terminé!" -ForegroundColor Green
