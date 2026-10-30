package dev.modhider.mixin.compat.modmenu;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.terraformersmc.modmenu.util.mod.Mod;
import com.terraformersmc.modmenu.util.mod.fabric.FabricMod;
import dev.modhider.hide.Area;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Makes Mod Menu's own "hidden" logic (mod counts, update badges, search) treat our mods as hidden. */
@Mixin(FabricMod.class)
abstract class FabricModMixin {
	@ModifyReturnValue(method = "isHidden", at = @At("RETURN"))
	private boolean modhider$hide(boolean original) {
		return original || Area.MOD_MENU.hides(((Mod) this).getId());
	}
}
