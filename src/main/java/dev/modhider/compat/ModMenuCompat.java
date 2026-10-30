package dev.modhider.compat;

import com.terraformersmc.modmenu.ModMenu;

/** Only loaded when Mod Menu is installed. */
public final class ModMenuCompat {
	private ModMenuCompat() {
	}

	public static void refresh() {
		ModMenu.clearModCountCache();
	}
}
