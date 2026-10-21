@echo off
chcp 65001 >nul
title DarkHUD - budowanie moda
cd /d "%~dp0"

where java >nul 2>nul
if errorlevel 1 (
  echo.
  echo [BLAD] Nie znaleziono Javy. Zainstaluj JDK 25 ^(np. Temurin: https://adoptium.net^) i uruchom ponownie.
  echo.
  pause
  exit /b 1
)

echo Buduje DarkHUD... ^(przy pierwszym razie pobiera sie kilkaset MB, to normalne^)
call gradlew.bat build
if errorlevel 1 (
  echo.
  echo [BLAD] Budowanie nie powiodlo sie. Sprawdz komunikaty powyzej.
  echo.
  pause
  exit /b 1
)

echo.
echo GOTOWE! Plik moda: build\libs\darkhud-1.0.0.jar
echo Wrzuc go do folderu .minecraft\mods ^(NIE ten z "-sources"^).
start "" "%~dp0build\libs"
pause
