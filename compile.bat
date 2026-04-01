@echo off
echo ========================================
echo   ChargeFlow V2 - Compile and Run
echo ========================================
echo.

echo [1/3] Compiling all Java source files...
javac -d out src\com\chargeflow\model\*.java src\com\chargeflow\factory\*.java src\com\chargeflow\strategy\*.java src\com\chargeflow\service\*.java src\com\chargeflow\utils\*.java src\com\chargeflow\main\*.java 2>&1
if %ERRORLEVEL% neq 0 (
    echo.
    echo COMPILATION FAILED! Check errors above.
    pause
    exit /b 1
)
echo      Compiled successfully.

echo [2/3] Running ChargeFlow V2...
echo.
echo ========================================
echo.
java -cp out com.chargeflow.main.ChargeFlowApp
echo.
pause
