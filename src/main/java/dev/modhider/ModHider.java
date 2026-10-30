package dev.modhider;

import dev.modhider.compat.ModMenuCompat;
import dev.modhider.config.HiderConfig;
import dev.modhider.hide.CreativeTabLayout;
import dev.modhider.web.WebServer;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.gui.screens.options.controls.KeyBindsScreen;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class ModHider implements ClientModInitializer {
	public static final String MOD_ID = "mod-hider";
	public static final Logger LOGGER = LoggerFactory.getLogger("Mod Hider");

	private static WebServer webServer;

	@Override
	public void onInitializeClient() {
		HiderConfig.load();
		webServer = new WebServer(HiderConfig.get().port);
		webServer.start();
	}

	public static String webUrl() {
		return "http://127.0.0.1:" + (webServer != null ? webServer.port() : HiderConfig.get().port) + "/";
	}

	/** Re-applies the config to whatever is currently on screen. Safe to call from any thread. */
	public static void applyChanges() {
		Minecraft minecraft = Minecraft.getInstance();
		minecraft.execute(() -> {
			if (FabricLoader.getInstance().isModLoaded("modmenu")) {
				ModMenuCompat.refresh();
			}
			CreativeTabLayout.apply();

			Screen screen = minecraft.gui.screen();
			if (screen instanceof KeyBindsScreen || screen instanceof CreativeModeInventoryScreen
					|| (screen != null && screen.getClass().getName().startsWith("com.terraformersmc.modmenu."))) {
				screen.resize(screen.width, screen.height);
			}
		});
	}
}
