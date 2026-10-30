package dev.modhider.compat;

import com.mojang.blaze3d.Blaze3D;
import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import dev.modhider.ModHider;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ConfirmLinkScreen;

import java.net.URI;

/** The "Config" button in Mod Menu just offers to open the web page. */
public final class ModMenuIntegration implements ModMenuApi {
	@Override
	public ConfigScreenFactory<?> getModConfigScreenFactory() {
		return parent -> {
			URI url = URI.create(ModHider.webUrl());
			return new ConfirmLinkScreen(open -> {
				if (open) {
					Blaze3D.openUri(url);
				}
				Minecraft.getInstance().gui.setScreen(parent);
			}, url, true);
		};
	}
}
