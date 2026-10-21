package pl.darkhud;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

/** Ciemne GUI ustawien z zakladkami: HUD, Wiadomosc, Kolory, Klawisze. */
public class SettingsScreen extends Screen {
	// --- paleta GUI (dark) ---
	private static final int OVERLAY = 0xB0000000;
	private static final int PANEL = 0xF0121217;
	private static final int PANEL_BORDER = 0xFF2A2A36;
	private static final int BTN = 0xFF1C1C25;
	private static final int BTN_HOVER = 0xFF282835;
	private static final int ACCENT = 0xFF8B7CFF;
	private static final int TEXT = 0xFFF2F2F7;
	private static final int MUTED = 0xFF9A9AB0;
	private static final int SEPARATOR = 0xFF24242F;
	private static final int SWITCH_OFF = 0xFF3A3A48;

	private static final String[] TABS = {"HUD", "Wiadomosc", "Kolory", "Klawisze"};

	/** Gotowe kolory do szybkiego wyboru (RRGGBB). */
	private static final String[] PALETTE = {
			"8B7CFF", "4FC3F7", "66BB6A", "FFD166", "FF8A65", "EF5350", "F48FB1", "FFFFFF"
	};

	private static final String[] THEME_NAMES = {"Fiolet", "Cyan", "Zielony", "Czerwony", "Zloty"};
	/** {akcent, etykiety, wartosci, tlo} */
	private static final String[][] THEMES = {
			{"8B7CFF", "9A9AB0", "F2F2F7", "101015"},
			{"4FC3F7", "8FA3AD", "E8F4F8", "0B1216"},
			{"66BB6A", "8FA896", "EAF5EC", "0D140E"},
			{"EF5350", "AD9090", "F8ECEC", "160C0C"},
			{"FFD166", "B0A58A", "FAF3E0", "15120A"}
	};

	private static final int PANEL_W = 380;
	private static final int PANEL_H = 220;
	private static final int ROW_H = 18;
	private static final int GAP = 4;

	// zapamietane miedzy otwarciami
	private static int tab = 0;
	private static int themeIdx = 0;

	private static class Btn {
		int x, y, w, h;
		Supplier<String> label;
		Supplier<Boolean> active;
		Runnable action;
		/** nie-null = przelacznik (nazwa po lewej + suwak po prawej) */
		String toggleName;
		/** nie-null = kwadracik z kolorem */
		Supplier<Integer> swatch;
		boolean isTab;

		boolean contains(double mx, double my) {
			return mx >= x && mx < x + w && my >= y && my < y + h;
		}
	}

	private record Label(String text, int x, int y, int color) {}

	private record ColorField(EditBox box, int swatchX, int y) {}

	private final List<Btn> buttons = new ArrayList<>();
	private final List<Label> labels = new ArrayList<>();
	private final List<ColorField> colorFields = new ArrayList<>();

	private int panelX, panelY;
	private int previewX, previewY = -1;

	/** 0 = nic, 1 = czekamy na klawisz GUI, 2 = czekamy na klawisz ukrywania */
	private int listening = 0;
	private boolean guiKeyWasDown = true;

	public SettingsScreen() {
		super(Component.literal("DarkHUD"));
	}

	// ------------------------------------------------------------------ budowa

	@Override
	protected void init() {
		clearWidgets();
		buttons.clear();
		labels.clear();
		colorFields.clear();
		previewY = -1;

		HudConfig cfg = HudConfig.INSTANCE;

		panelX = (this.width - PANEL_W) / 2;
		panelY = Math.max(4, (this.height - PANEL_H) / 2);

		int innerX = panelX + 14;
		int innerW = PANEL_W - 28;
		int colW = (innerW - GAP) / 2;

		// zakladki
		int tabW = (innerW - GAP * 3) / 4;
		for (int i = 0; i < TABS.length; i++) {
			final int idx = i;
			Btn b = addButton(innerX + i * (tabW + GAP), panelY + 28, tabW, 16,
					() -> TABS[idx], () -> tab == idx, () -> {
						tab = idx;
						listening = 0;
						init();
					});
			b.isTab = true;
		}

		int y = panelY + 54;
		switch (tab) {
			case 0 -> buildHudTab(cfg, innerX, y, innerW, colW);
			case 1 -> buildMessageTab(cfg, innerX, y, innerW, colW);
			case 2 -> buildColorsTab(cfg, innerX, y, innerW, colW);
			default -> buildKeysTab(cfg, innerX, y, innerW);
		}

		// stopka
		int fy = panelY + PANEL_H - 28;
		addButton(innerX, fy, colW, ROW_H + 2, () -> "Zapisz i zamknij", () -> false, () -> {
			HudConfig.save();
			onClose();
		});
		addButton(innerX + colW + GAP, fy, colW, ROW_H + 2, () -> "Resetuj wszystko", () -> false, () -> {
			HudConfig.reset();
			listening = 0;
			init();
		});
	}

	private void buildHudTab(HudConfig cfg, int x, int y, int w, int colW) {
		int x2 = x + colW + GAP;
		int row = ROW_H + GAP;

		addToggle(x, y, colW, "Coords", () -> cfg.showCoords, () -> cfg.showCoords = !cfg.showCoords);
		addToggle(x2, y, colW, "Nether", () -> cfg.showNether, () -> cfg.showNether = !cfg.showNether);
		y += row;
		addToggle(x, y, colW, "FPS", () -> cfg.showFps, () -> cfg.showFps = !cfg.showFps);
		addToggle(x2, y, colW, "Ping", () -> cfg.showPing, () -> cfg.showPing = !cfg.showPing);
		y += row;
		addToggle(x, y, colW, "CPS", () -> cfg.showCps, () -> cfg.showCps = !cfg.showCps);
		addToggle(x2, y, colW, "Facing", () -> cfg.showFacing, () -> cfg.showFacing = !cfg.showFacing);
		y += row;
		addToggle(x, y, colW, "Server", () -> cfg.showServer, () -> cfg.showServer = !cfg.showServer);
		addToggle(x2, y, colW, "Nick", () -> cfg.showNick, () -> cfg.showNick = !cfg.showNick);
		y += row;
		addToggle(x, y, colW, "Tlo panelu", () -> cfg.showBackground, () -> cfg.showBackground = !cfg.showBackground);
		addButton(x2, y, colW, ROW_H, () -> "Pozycja: " + cornerName(cfg.corner), () -> false,
				() -> cfg.corner = (cfg.corner + 1) % 4);
	}

	private void buildMessageTab(HudConfig cfg, int x, int y, int w, int colW) {
		int x2 = x + colW + GAP;
		int row = ROW_H + GAP;

		// tekst wiadomosci
		EditBox msg = new EditBox(this.font, x, y, w, ROW_H, Component.literal("Wiadomosc"));
		msg.setMaxLength(48);
		msg.setHint(Component.literal("Wpisz swoja wiadomosc (pusta = ukryta)"));
		msg.setValue(cfg.customMessage == null ? "" : cfg.customMessage);
		msg.setResponder(s -> cfg.customMessage = s);
		addRenderableWidget(msg);
		y += row;

		// kolor + paleta
		labels.add(new Label("Kolor", x, y + 5, MUTED));
		addColorBox(x + 40, y, "Kolor", () -> cfg.messageColor, s -> cfg.messageColor = s);
		int palX = x + 40 + 62 + 4 + 14 + 8;
		addPalette(palX, y + 2, c -> {
			cfg.messageColor = c;
			init();
		});
		y += row;

		addButton(x, y, colW, ROW_H, () -> "Pozycja: " + messageCornerName(cfg.messageCorner), () -> false,
				() -> cfg.messageCorner = (cfg.messageCorner + 1) % 5);
		addButton(x2, y, colW, ROW_H, () -> "Rozmiar: " + cfg.messageScale + "x", () -> false,
				() -> cfg.messageScale = cfg.messageScale % 3 + 1);
		y += row;

		addToggle(x, y, colW, "Tlo wiadomosci", () -> cfg.messageBackground,
				() -> cfg.messageBackground = !cfg.messageBackground);
		y += row;

		// podglad
		labels.add(new Label("Podglad:", x, y + 5, MUTED));
		previewX = x + 52;
		previewY = y + 5;
	}

	private void buildColorsTab(HudConfig cfg, int x, int y, int w, int colW) {
		int x2 = x + colW + GAP;
		int row = ROW_H + GAP;

		labels.add(new Label("Akcent", x, y + 5, MUTED));
		addColorBox(x + 54, y, "Akcent", () -> cfg.accentColor, s -> cfg.accentColor = s);
		labels.add(new Label("Etykiety", x2, y + 5, MUTED));
		addColorBox(x2 + 54, y, "Etykiety", () -> cfg.labelColor, s -> cfg.labelColor = s);
		y += row;

		labels.add(new Label("Wartosci", x, y + 5, MUTED));
		addColorBox(x + 54, y, "Wartosci", () -> cfg.valueColor, s -> cfg.valueColor = s);
		labels.add(new Label("Tlo", x2, y + 5, MUTED));
		addColorBox(x2 + 54, y, "Tlo", () -> cfg.backgroundColor, s -> cfg.backgroundColor = s);
		y += row;

		labels.add(new Label("Szybki akcent", x, y + 5, MUTED));
		addPalette(x + 90, y + 2, c -> {
			cfg.accentColor = c;
			init();
		});
		y += row;

		addButton(x, y, w, ROW_H, () -> "Motyw: " + THEME_NAMES[themeIdx] + "  (kliknij, aby zmienic)", () -> false, () -> {
			themeIdx = (themeIdx + 1) % THEMES.length;
			String[] t = THEMES[themeIdx];
			cfg.accentColor = t[0];
			cfg.labelColor = t[1];
			cfg.valueColor = t[2];
			cfg.backgroundColor = t[3];
			init();
		});
		y += row;

		labels.add(new Label("Kolory wpisujesz jako HEX, np. #8B7CFF. Zly kod robi pole czerwone.", x, y + 4, MUTED));
	}

	private void buildKeysTab(HudConfig cfg, int x, int y, int w) {
		int row = ROW_H + GAP;

		addButton(x, y, w, ROW_H, () -> listening == 1
						? "Klawisz GUI:  > nacisnij klawisz <"
						: "Klawisz GUI:  " + keyName(cfg.guiKey),
				() -> listening == 1, () -> listening = (listening == 1) ? 0 : 1);
		y += row;
		addButton(x, y, w, ROW_H, () -> listening == 2
						? "Ukryj koordy:  > nacisnij klawisz <"
						: "Ukryj koordy:  " + keyName(cfg.hideCoordsKey),
				() -> listening == 2, () -> listening = (listening == 2) ? 0 : 2);
		y += row + 4;

		labels.add(new Label("Kliknij przycisk, potem nacisnij nowy klawisz. ESC anuluje.", x, y, MUTED));
		labels.add(new Label("Uwaga: 6 i 8 to tez sloty paska szybkiego dostepu.", x, y + 12, MUTED));
		labels.add(new Label("Jesli to przeszkadza, ustaw np. H i J.", x, y + 24, MUTED));
	}

	// ----------------------------------------------------------------- helpery

	private Btn addButton(int x, int y, int w, int h, Supplier<String> label, Supplier<Boolean> active, Runnable action) {
		Btn b = new Btn();
		b.x = x;
		b.y = y;
		b.w = w;
		b.h = h;
		b.label = label;
		b.active = active;
		b.action = action;
		buttons.add(b);
		return b;
	}

	private void addToggle(int x, int y, int w, String name, Supplier<Boolean> state, Runnable toggle) {
		Btn b = addButton(x, y, w, ROW_H, () -> name, state, toggle);
		b.toggleName = name;
	}

	/** Pole HEX z podgladem koloru. Zajmuje 62 + 4 + 14 px szerokosci. */
	private void addColorBox(int x, int y, String name, Supplier<String> get, Consumer<String> set) {
		EditBox box = new EditBox(this.font, x, y, 62, ROW_H, Component.literal(name));
		box.setMaxLength(7);
		box.setValue("#" + get.get());
		box.setResponder(s -> {
			String t = s.startsWith("#") ? s.substring(1) : s;
			if (HudConfig.parseHex(t) >= 0) {
				set.accept(t.toUpperCase());
				box.setTextColor(0xFFE0E0E0);
			} else {
				box.setTextColor(0xFFFF5555);
			}
		});
		addRenderableWidget(box);
		colorFields.add(new ColorField(box, x + 62 + 4, y));
	}

	/** Rzad kwadracikow z gotowymi kolorami. */
	private void addPalette(int x, int y, Consumer<String> onPick) {
		for (int i = 0; i < PALETTE.length; i++) {
			final String hex = PALETTE[i];
			Btn b = addButton(x + i * 17, y, 14, 14, () -> "", () -> false, () -> onPick.accept(hex));
			b.swatch = () -> 0xFF000000 | HudConfig.parseHex(hex);
		}
	}

	private static String cornerName(int c) {
		return switch (c) {
			case 1 -> "prawy gora";
			case 2 -> "lewy dol";
			case 3 -> "prawy dol";
			default -> "lewy gora";
		};
	}

	private static String messageCornerName(int c) {
		return switch (c) {
			case 0 -> "lewy gora";
			case 2 -> "lewy dol";
			case 3 -> "prawy dol";
			case 4 -> "gora srodek";
			default -> "prawy gora";
		};
	}

	public static String keyName(int code) {
		if (code < 0) {
			return "brak";
		}
		try {
			return InputConstants.getKey(code, -1).getDisplayName().getString();
		} catch (Exception e) {
			return "KEY " + code;
		}
	}

	private static void roundRect(GuiGraphics g, int x1, int y1, int x2, int y2, int color) {
		g.fill(x1 + 1, y1, x2 - 1, y2, color);
		g.fill(x1, y1 + 1, x2, y2 - 1, color);
	}

	// ----------------------------------------------------------------- render

	@Override
	public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float delta) {
		// przyciemnienie + panel (pod polami tekstowymi, ktore rysuje super.render)
		g.fill(0, 0, this.width, this.height, OVERLAY);

		roundRect(g, panelX - 1, panelY - 1, panelX + PANEL_W + 1, panelY + PANEL_H + 1, PANEL_BORDER);
		roundRect(g, panelX, panelY, panelX + PANEL_W, panelY + PANEL_H, PANEL);
		g.fill(panelX + 1, panelY, panelX + PANEL_W - 1, panelY + 2, ACCENT);

		// separatory
		g.fill(panelX + 14, panelY + 48, panelX + PANEL_W - 14, panelY + 49, SEPARATOR);
		g.fill(panelX + 14, panelY + PANEL_H - 34, panelX + PANEL_W - 14, panelY + PANEL_H - 33, SEPARATOR);
	}

	@Override
	public void render(GuiGraphics g, int mouseX, int mouseY, float delta) {
		super.render(g, mouseX, mouseY, delta);

		HudConfig cfg = HudConfig.INSTANCE;

		// naglowek
		g.drawString(this.font, Component.literal("DarkHUD").withStyle(ChatFormatting.BOLD), panelX + 14, panelY + 11, ACCENT);
		g.drawString(this.font, "ustawienia", panelX + 14 + this.font.width("DarkHUD") + 6, panelY + 11, MUTED);
		String hint = "ESC - zamknij";
		g.drawString(this.font, hint, panelX + PANEL_W - 14 - this.font.width(hint), panelY + 11, MUTED);

		// przyciski
		for (Btn b : buttons) {
			drawButton(g, b, mouseX, mouseY);
		}

		// etykiety
		for (Label l : labels) {
			g.drawString(this.font, l.text(), l.x(), l.y(), l.color());
		}

		// podglady kolorow przy polach HEX
		for (ColorField f : colorFields) {
			int c = HudConfig.parseHex(f.box().getValue());
			int argb = c >= 0 ? (0xFF000000 | c) : 0xFF444444;
			g.fill(f.swatchX() - 1, f.y() + 1, f.swatchX() + 15, f.y() + 17, PANEL_BORDER);
			g.fill(f.swatchX(), f.y() + 2, f.swatchX() + 14, f.y() + 16, argb);
		}

		// podglad wiadomosci
		if (previewY >= 0) {
			String m = (cfg.customMessage == null || cfg.customMessage.isBlank()) ? "(pusta - ukryta)" : cfg.customMessage;
			g.drawString(this.font, m, previewX, previewY, cfg.messageArgb());
		}
	}

	private void drawButton(GuiGraphics g, Btn b, int mouseX, int mouseY) {
		boolean hover = b.contains(mouseX, mouseY);
		boolean on = b.active.get();

		// kwadracik z kolorem
		if (b.swatch != null) {
			g.fill(b.x - 1, b.y - 1, b.x + b.w + 1, b.y + b.h + 1, hover ? TEXT : PANEL_BORDER);
			g.fill(b.x, b.y, b.x + b.w, b.y + b.h, b.swatch.get());
			return;
		}

		// zakladka
		if (b.isTab) {
			g.fill(b.x, b.y, b.x + b.w, b.y + b.h, on ? BTN_HOVER : (hover ? BTN_HOVER : BTN));
			if (on) {
				g.fill(b.x, b.y + b.h - 2, b.x + b.w, b.y + b.h, ACCENT);
			}
			String text = b.label.get();
			g.drawString(this.font, text, b.x + (b.w - this.font.width(text)) / 2,
					b.y + (b.h - this.font.lineHeight) / 2, on ? TEXT : MUTED);
			return;
		}

		roundRect(g, b.x, b.y, b.x + b.w, b.y + b.h, hover ? BTN_HOVER : BTN);

		// przelacznik (suwak)
		if (b.toggleName != null) {
			g.drawString(this.font, b.toggleName, b.x + 8, b.y + (b.h - this.font.lineHeight) / 2 + 1,
					on ? TEXT : MUTED);
			int sw = 24;
			int sh = 10;
			int sx = b.x + b.w - sw - 7;
			int sy = b.y + (b.h - sh) / 2;
			roundRect(g, sx, sy, sx + sw, sy + sh, on ? ACCENT : SWITCH_OFF);
			int kx = on ? sx + sw - 9 : sx + 1;
			g.fill(kx, sy + 1, kx + 8, sy + sh - 1, 0xFFF2F2F7);
			return;
		}

		// zwykly przycisk
		if (on) {
			g.fill(b.x, b.y + 2, b.x + 2, b.y + b.h - 2, ACCENT);
		}
		String text = b.label.get();
		g.drawString(this.font, text, b.x + (b.w - this.font.width(text)) / 2,
				b.y + (b.h - this.font.lineHeight) / 2 + 1, on ? TEXT : MUTED);
	}

	// ------------------------------------------------------------------ wejscie

	@Override
	public boolean mouseClicked(double mx, double my, int button) {
		if (button == 0) {
			for (Btn b : new ArrayList<>(buttons)) {
				if (b.contains(mx, my)) {
					this.setFocused(null);
					b.action.run();
					return true;
				}
			}
		}
		return super.mouseClicked(mx, my, button);
	}

	@Override
	public boolean keyPressed(int key, int scancode, int mods) {
		if (listening != 0) {
			if (key == GLFW.GLFW_KEY_ESCAPE) {
				listening = 0;
				return true;
			}
			HudConfig cfg = HudConfig.INSTANCE;
			if (listening == 1) {
				cfg.guiKey = key;
				// zeby nie zamknac okna od razu tym samym klawiszem
				guiKeyWasDown = true;
			} else {
				cfg.hideCoordsKey = key;
			}
			listening = 0;
			HudConfig.save();
			return true;
		}
		return super.keyPressed(key, scancode, mods);
	}

	@Override
	public void tick() {
		if (this.minecraft == null) {
			return;
		}
		boolean down = DarkHudClient.isDown(this.minecraft, HudConfig.INSTANCE.guiKey);
		// gdy piszesz w polu tekstowym, klawisz GUI (np. 8) nie moze zamykac okna
		boolean typing = this.getFocused() instanceof EditBox eb && eb.isFocused();
		if (listening == 0 && !typing && down && !guiKeyWasDown) {
			onClose();
			return;
		}
		guiKeyWasDown = down;
	}

	@Override
	public void onClose() {
		HudConfig.save();
		super.onClose();
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}
}
