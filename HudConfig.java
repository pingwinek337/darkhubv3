package pl.darkhud;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;
import org.lwjgl.glfw.GLFW;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

/** Ustawienia moda zapisywane w config/darkhud.json. */
public class HudConfig {
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("darkhud.json");

	public static HudConfig INSTANCE = new HudConfig();

	// Elementy HUD
	public boolean showCoords = true;
	public boolean showNether = true;
	public boolean showFps = true;
	public boolean showPing = true;
	public boolean showCps = true;
	public boolean showFacing = true;
	public boolean showServer = true;
	public boolean showNick = true;

	// Wyglad
	public boolean showBackground = true;
	/** 0 = lewy gorny, 1 = prawy gorny, 2 = lewy dolny, 3 = prawy dolny */
	public int corner = 0;

	/** Wlasna wiadomosc (osobny baner). Pusta = ukryta. */
	public String customMessage = "DarkHUD";
	public String messageColor = "FFD166";
	/** 0 = lewy gorny, 1 = prawy gorny, 2 = lewy dolny, 3 = prawy dolny, 4 = gora srodek */
	public int messageCorner = 1;
	/** Rozmiar tekstu 1-3 */
	public int messageScale = 1;
	public boolean messageBackground = true;

	// Kolory w formacie HEX (RRGGBB, bez #)
	public String accentColor = "8B7CFF";
	public String labelColor = "9A9AB0";
	public String valueColor = "F2F2F7";
	public String backgroundColor = "101015";

	// Klawisze (kody GLFW)
	public int guiKey = GLFW.GLFW_KEY_8;
	public int hideCoordsKey = GLFW.GLFW_KEY_6;

	/** Chwilowe ukrycie koordynatow klawiszem (nie zapisywane). */
	public transient boolean coordsHidden = false;

	/** Zwraca kolor RRGGBB jako liczbe albo -1 gdy tekst nie jest poprawnym HEX-em. */
	public static int parseHex(String s) {
		if (s == null) {
			return -1;
		}
		String t = s.trim();
		if (t.startsWith("#")) {
			t = t.substring(1);
		}
		if (t.length() != 6) {
			return -1;
		}
		try {
			return Integer.parseInt(t, 16);
		} catch (NumberFormatException e) {
			return -1;
		}
	}

	private static int argb(String hex, int fallbackRgb, int alpha) {
		int c = parseHex(hex);
		if (c < 0) {
			c = fallbackRgb;
		}
		return (alpha << 24) | c;
	}

	public int accent() {
		return argb(accentColor, 0x8B7CFF, 0xFF);
	}

	public int label() {
		return argb(labelColor, 0x9A9AB0, 0xFF);
	}

	public int value() {
		return argb(valueColor, 0xF2F2F7, 0xFF);
	}

	public int messageArgb() {
		return argb(messageColor, 0xFFD166, 0xFF);
	}

	public int background() {
		return argb(backgroundColor, 0x101015, 0xC0);
	}

	public static void load() {
		if (Files.exists(FILE)) {
			try (Reader r = Files.newBufferedReader(FILE)) {
				HudConfig loaded = GSON.fromJson(r, HudConfig.class);
				if (loaded != null) {
					INSTANCE = loaded;
				}
			} catch (Exception e) {
				DarkHudClient.LOGGER.warn("Nie udalo sie wczytac configu, uzywam domyslnych", e);
			}
		}
		save();
	}

	public static void save() {
		try (Writer w = Files.newBufferedWriter(FILE)) {
			GSON.toJson(INSTANCE, w);
		} catch (IOException e) {
			DarkHudClient.LOGGER.warn("Nie udalo sie zapisac configu", e);
		}
	}

	public static void reset() {
		INSTANCE = new HudConfig();
		save();
	}
}
