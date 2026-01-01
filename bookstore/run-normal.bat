@echo off
echo ========================================
echo   BOOKSTORE - NORMAL RUN MODE
echo ========================================
echo.
echo This will:
echo   1. Use existing database
echo   2. Validate schema (no changes)
echo   3. NO seed/reset data
echo   4. Start application normally
echo.
echo Starting application...
call mvnw.cmd spring-boot:run -Dspring-boot.run.profiles=normal
pause

