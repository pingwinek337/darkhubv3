package pl.darkhud;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class HudRenderer {
	private static final ArrayDeque<Long> LEFT_CLICKS = new ArrayDeque<>();
	private static final ArrayDeque<Long> RIGHT_CLICKS = new ArrayDeque<>();
	private static boolean leftWasDown = false;
	private static boolean rightWasDown = false;

	private record Line(String label, String value) {}

	public static void render(GuiGraphics g, DeltaTracker delta) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.player == null || mc.level == null) {
			return;
		}

		updateCps(mc);

		if (mc.options.hideGui) {
			return;
		}

		HudConfig cfg = HudConfig.INSTANCE;
		List<Line> lines = buildLines(mc, cfg);

		drawMessage(g, mc, cfg);
		if (!lines.isEmpty()) {
			drawPanel(g, mc, "", lines, cfg);
		}
	}

	/** Osobny baner z wlasna wiadomoscia (kolor, rozmiar i pozycja z configu). */
	private static void drawMessage(GuiGraphics g, Minecraft mc, HudConfig cfg) {
		String msg = cfg.customMessage == null ? "" : cfg.customMessage.trim();
		if (msg.isEmpty()) {
			return;
		}
		Font font = mc.font;
		int scale = Mth.clamp(cfg.messageScale, 1, 3);
		int color = cfg.messageArgb();

		int padX = 6;
		int padY = 4;
		int textW = font.width(msg) * scale;
		int textH = font.lineHeight * scale;
		int boxW = textW + padX * 2;
		int boxH = textH + padY * 2;
		int margin = 6;

		int sw = g.guiWidth();
		int sh = g.guiHeight();
		int x;
		int y;
		switch (cfg.messageCorner) {
			case 0 -> { x = margin; y = margin; }
			case 2 -> { x = margin; y = sh - boxH - margin; }
			case 3 -> { x = sw - boxW - margin; y = sh - boxH - margin; }
			case 4 -> { x = (sw - boxW) / 2; y = margin; }
			default -> { x = sw - boxW - margin; y = margin; }
		}

		if (cfg.messageBackground) {
			roundRect(g, x - 1, y - 1, x + boxW + 1, y + boxH + 1, withAlpha(color, 0x80));
			roundRect(g, x, y, x + boxW, y + boxH, cfg.background());
		}

		g.pose().pushPose();
		g.pose().translate(x + padX, y + padY, 0);
		g.pose().scale(scale, scale, 1.0f);
		g.drawString(font, msg, 0, 0, color);
		g.pose().popPose();
	}

	private static List<Line> buildLines(Minecraft mc, HudConfig cfg) {
		List<Line> lines = new ArrayList<>();

		if (cfg.showNick) {
			lines.add(new Line("Nick", mc.getUser().getName()));
		}
		if (cfg.showServer) {
			lines.add(new Line("Serwer", serverName(mc)));
		}
		if (cfg.showFps) {
			lines.add(new Line("FPS", String.valueOf(mc.getFps())));
		}
		if (cfg.showPing) {
			lines.add(new Line("Ping", pingText(mc)));
		}
		if (cfg.showCps) {
			lines.add(new Line("CPS", cps(LEFT_CLICKS) + " | " + cps(RIGHT_CLICKS)));
		}
		if (cfg.showCoords && !cfg.coordsHidden) {
			lines.add(new Line("XYZ", String.format(Locale.ROOT, "%.1f / %.1f / %.1f",
					mc.player.getX(), mc.player.getY(), mc.player.getZ())));
		}
		if (cfg.showNether && !cfg.coordsHidden) {
			var dim = mc.level.dimension();
			if (dim == Level.OVERWORLD) {
				lines.add(new Line("Nether", String.format(Locale.ROOT, "%.1f / %.1f",
						mc.player.getX() / 8.0, mc.player.getZ() / 8.0)));
			} else if (dim == Level.NETHER) {
				lines.add(new Line("Overworld", String.format(Locale.ROOT, "%.1f / %.1f",
						mc.player.getX() * 8.0, mc.player.getZ() * 8.0)));
			}
		}
		if (cfg.showFacing) {
			lines.add(new Line("Kierunek", facingText(mc)));
		}
		return lines;
	}

	/** Prostokat z uciętymi rogami (efekt zaokraglenia). */
	private static void roundRect(GuiGraphics g, int x1, int y1, int x2, int y2, int color) {
		g.fill(x1 + 1, y1, x2 - 1, y2, color);
		g.fill(x1, y1 + 1, x2, y2 - 1, color);
	}

	private static int withAlpha(int argb, int alpha) {
		return (alpha << 24) | (argb & 0xFFFFFF);
	}

	private static void drawPanel(GuiGraphics g, Minecraft mc, String message, List<Line> lines, HudConfig cfg) {
		Font font = mc.font;
		int accent = cfg.accent();
		int labelColor = cfg.label();
		int valueColor = cfg.value();

		int padX = 6;
		int padY = 5;
		int lineH = font.lineHeight + 3;
		int gapAfterMessage = 5;

		// Kolumny: etykiety wyrownane, wartosci w drugiej kolumnie
		int labelW = 0;
		int valueW = 0;
		for (Line l : lines) {
			labelW = Math.max(labelW, font.width(l.label()));
			valueW = Math.max(valueW, font.width(l.value()));
		}
		int colGap = 8;
		int contentW = lines.isEmpty() ? 0 : labelW + colGap + valueW;
		int msgW = message.isEmpty() ? 0 : font.width(message);
		contentW = Math.max(contentW, msgW);

		int accentBar = 3;
		int boxW = contentW + padX * 2 + accentBar;
		int contentH = lines.size() * lineH - (lines.isEmpty() ? 0 : 3);
		if (!message.isEmpty()) {
			contentH += font.lineHeight + gapAfterMessage + (lines.isEmpty() ? 0 : 1 + gapAfterMessage);
		}
		int boxH = contentH + padY * 2;
		int margin = 6;

		int sw = g.guiWidth();
		int sh = g.guiHeight();
		int x = (cfg.corner == 1 || cfg.corner == 3) ? sw - boxW - margin : margin;
		int y = (cfg.corner == 2 || cfg.corner == 3) ? sh - boxH - margin : margin;

		if (cfg.showBackground) {
			// obramowanie w kolorze akcentu + tlo
			roundRect(g, x - 1, y - 1, x + boxW + 1, y + boxH + 1, withAlpha(accent, 0x70));
			roundRect(g, x, y, x + boxW, y + boxH, cfg.background());
			// pionowy pasek akcentu po lewej
			g.fill(x + 1, y + 3, x + 1 + 2, y + boxH - 3, accent);
		}

		int tx = x + padX + accentBar;
		int ty = y + padY;

		if (!message.isEmpty()) {
			g.drawString(font, message, tx, ty, accent);
			ty += font.lineHeight + gapAfterMessage;
			if (!lines.isEmpty()) {
				g.fill(tx, ty, tx + contentW, ty + 1, withAlpha(accent, 0x55));
				ty += 1 + gapAfterMessage;
			}
		}

		int valueX = tx + labelW + colGap;
		for (Line l : lines) {
			g.drawString(font, l.label(), tx, ty, labelColor);
			g.drawString(font, l.value(), valueX, ty, valueColor);
			ty += lineH;
		}
	}

	private static String serverName(Minecraft mc) {
		ServerData server = mc.getCurrentServer();
		if (server != null) {
			return server.ip;
		}
		return mc.hasSingleplayerServer() ? "Singleplayer" : "-";
	}

	private static String pingText(Minecraft mc) {
		if (mc.getConnection() == null || mc.hasSingleplayerServer()) {
			return "-";
		}
		PlayerInfo info = mc.getConnection().getPlayerInfo(mc.player.getUUID());
		if (info == null) {
			return "-";
		}
		return info.getLatency() + " ms";
	}

	private static String facingText(Minecraft mc) {
		Direction dir = mc.player.getDirection();
		String name = switch (dir) {
			case NORTH -> "Polnoc (-Z)";
			case SOUTH -> "Poludnie (+Z)";
			case EAST -> "Wschod (+X)";
			case WEST -> "Zachod (-X)";
			default -> dir.getName();
		};
		float yaw = Mth.wrapDegrees(mc.player.getYRot());
		return name + String.format(Locale.ROOT, "  [%.0f]", yaw);
	}

	// --- CPS ---

	private static void updateCps(Minecraft mc) {
		long now = System.currentTimeMillis();
		long window = mc.getWindow().getWindow();

		boolean left = GLFW.glfwGetMouseButton(window, GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS;
		boolean right = GLFW.glfwGetMouseButton(window, GLFW.GLFW_MOUSE_BUTTON_RIGHT) == GLFW.GLFW_PRESS;

		if (mc.screen == null) {
			if (left && !leftWasDown) {
				LEFT_CLICKS.addLast(now);
			}
			if (right && !rightWasDown) {
				RIGHT_CLICKS.addLast(now);
			}
		}
		leftWasDown = left;
		rightWasDown = right;

		prune(LEFT_CLICKS, now);
		prune(RIGHT_CLICKS, now);
	}

	private static void prune(ArrayDeque<Long> q, long now) {
		while (!q.isEmpty() && now - q.peekFirst() > 1000) {
			q.pollFirst();
		}
	}

	private static int cps(ArrayDeque<Long> q) {
		return q.size();
	}
}
