package pl.darkhud;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DarkHudClient implements ClientModInitializer {
	public static final String MOD_ID = "darkhud";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	private boolean guiKeyWasDown = false;
	private boolean hideKeyWasDown = false;

	@Override
	public void onInitializeClient() {
		HudConfig.load();

		HudRenderCallback.EVENT.register(HudRenderer::render);

		ClientTickEvents.END_CLIENT_TICK.register(this::onTick);

		LOGGER.info("DarkHUD zaladowany");
	}

	private void onTick(Minecraft mc) {
		HudConfig cfg = HudConfig.INSTANCE;

		// Klawisze dzialaja tylko gdy nie ma otwartego ekranu (czat, menu itp.)
		if (mc.screen != null || !mc.isWindowActive() || mc.player == null) {
			guiKeyWasDown = isDown(mc, cfg.guiKey);
			hideKeyWasDown = isDown(mc, cfg.hideCoordsKey);
			return;
		}

		boolean guiDown = isDown(mc, cfg.guiKey);
		if (guiDown && !guiKeyWasDown) {
			mc.setScreen(new SettingsScreen());
		}
		guiKeyWasDown = guiDown;

		boolean hideDown = isDown(mc, cfg.hideCoordsKey);
		if (hideDown && !hideKeyWasDown) {
			cfg.coordsHidden = !cfg.coordsHidden;
		}
		hideKeyWasDown = hideDown;
	}

	public static boolean isDown(Minecraft mc, int key) {
		if (key < 0) {
			return false;
		}
		return GLFW.glfwGetKey(mc.getWindow().getWindow(), key) == GLFW.GLFW_PRESS;
	}
}
