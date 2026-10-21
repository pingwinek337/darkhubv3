# DarkHUD (Fabric 1.21.4, kompatybilny z Sodium)

Ciemny HUD klient-side: **coords, nether, FPS, ping, CPS, kierunek, serwer, nick**
oraz GUI ustawien (przelaczniki, zmiana klawiszy, zapis do `config/darkhud.json`).

## Domyslne klawisze

| Akcja | Klawisz |
|---|---|
| Otwarcie / zamkniecie GUI | `8` |
| Ukrycie / pokazanie koordow (XYZ + Nether) | `6` |

Klawisze zmienisz w GUI (kliknij przycisk i nacisnij nowy klawisz, ESC anuluje).
Uwaga: `6` i `8` to tez sloty paska szybkiego dostepu - moga przelaczac przedmiot.
Jesli ci to przeszkadza, ustaw inne klawisze w GUI (np. `H`, `Right Shift`).

## GUI (zakladki)

- **HUD** - przelaczniki elementow, tlo, pozycja panelu.
- **Wiadomosc** - wlasny tekst jako osobny baner (domyslnie prawy gorny rog): kolor (HEX albo paleta), pozycja, rozmiar 1-3x, tlo, podglad.
- **Kolory** - akcent, etykiety, wartosci, tlo (HEX), szybka paleta akcentu i gotowe motywy.
- **Klawisze** - zmiana klawisza GUI i ukrywania koordow.

Wszystko zapisuje sie do `config/darkhud.json`.

## Instalacja gotowego pliku

1. Fabric Loader dla 1.21.4 + **Fabric API** (+ opcjonalnie Sodium).
2. Wrzuc `darkhud-1.0.0.jar` do `.minecraft/mods`.

## Jak zbudowac jar (jedno klikniecie)

Wymagane: **JDK 25** (np. https://adoptium.net; sam mod dziala na Javie 21 w grze) i internet (przy pierwszym buildzie).

- **Windows:** rozpakuj ZIP i kliknij dwukrotnie `build.bat`.
- **Linux / macOS:** `./build.sh`

Gotowy plik pojawi sie w `build/libs/darkhud-1.0.0.jar`
(plik z koncowka `-sources` nie jest do modow).

### Bez instalowania Javy (GitHub Actions)

1. Wrzuc zawartosc folderu na swoje repozytorium GitHub.
2. Zakladka **Actions** -> workflow **Build DarkHUD** -> po zakonczeniu pobierz artefakt `darkhud-mod`.

## Uruchamianie w srodowisku dev

`./gradlew runClient` (opcjonalnie odkomentuj Sodium w `build.gradle`).

## Struktura

- `DarkHudClient` - wejscie moda, obsluga klawiszy
- `HudRenderer` - rysowanie HUD i licznik CPS
- `HudConfig` - ustawienia (JSON)
- `SettingsScreen` - ciemne GUI
