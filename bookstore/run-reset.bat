@echo off
echo ========================================
echo   BOOKSTORE - RESET DATABASE MODE
echo ========================================
echo.
echo WARNING: This will DELETE ALL DATA in the database!
echo.
echo This will:
echo   1. Keep existing database
echo   2. Update schema if needed
echo   3. DELETE all existing data
echo   4. Add fresh seed data
echo.
set /p confirm="Are you sure? (yes/no): "
if /i not "%confirm%"=="yes" (
    echo Operation cancelled.
    pause
    exit /b
)
echo.
echo Starting application with reset profile...
call mvnw.cmd spring-boot:run -Dspring-boot.run.profiles=reset
pause

