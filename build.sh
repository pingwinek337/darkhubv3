#!/usr/bin/env bash
set -e
cd "$(dirname "$0")"

if ! command -v java >/dev/null 2>&1; then
  echo "[BLAD] Nie znaleziono Javy. Zainstaluj JDK 25 (np. Temurin) i uruchom ponownie."
  exit 1
fi

chmod +x gradlew
./gradlew build

echo
echo "GOTOWE! Plik moda: build/libs/darkhud-1.0.0.jar"
echo "Wrzuc go do folderu .minecraft/mods (NIE ten z '-sources')."
