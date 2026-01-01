@echo off
echo ========================================
echo   BOOKSTORE - SEED DATABASE MODE
echo ========================================
echo.
echo This will:
echo   1. Create database if not exists
echo   2. Update schema if needed
echo   3. Add seed data (only if not exists)
echo.
pause
echo.
echo Starting application with seed profile...
call mvnw.cmd spring-boot:run -Dspring-boot.run.profiles=seed
pause

