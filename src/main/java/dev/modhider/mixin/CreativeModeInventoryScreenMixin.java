package dev.modhider.mixin;

import dev.modhider.hide.CreativeTabLayout;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(CreativeModeInventoryScreen.class)
abstract class CreativeModeInventoryScreenMixin {
	@Shadow
	private static CreativeModeTab selectedTab;

	@Inject(method = "init", at = @At("HEAD"))
	private void modhider$layoutTabs(CallbackInfo ci) {
		CreativeTabLayout.apply();
		// The last opened tab is remembered statically; don't reopen one that is now hidden.
		if (!selectedTab.shouldDisplay()) {
			selectedTab = CreativeModeTabs.getDefaultTab();
		}
	}
}
