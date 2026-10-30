package dev.modhider.mixin.compat.modmenu;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.terraformersmc.modmenu.gui.widget.ModListWidget;
import com.terraformersmc.modmenu.util.mod.Mod;
import dev.modhider.hide.Area;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.List;

/**
 * Mod Menu still lists "hidden" mods in config mode and for some searches, so drop ours
 * from every search result the list is built from (parents and children alike).
 */
@Mixin(ModListWidget.class)
abstract class ModListWidgetMixin {
	@ModifyExpressionValue(method = "filter(Ljava/lang/String;ZZ)V", at = @At(value = "INVOKE", target = "Lcom/terraformersmc/modmenu/util/mod/ModSearch;search(Lcom/terraformersmc/modmenu/gui/ModsScreen;Ljava/lang/String;Ljava/util/List;)Ljava/util/List;"))
	private List<Mod> modhider$hideMods(List<Mod> mods) {
		return mods.stream().filter(mod -> !Area.MOD_MENU.hides(mod.getId())).toList();
	}
}
