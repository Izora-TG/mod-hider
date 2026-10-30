package dev.modhider.mixin.compat.modmenu;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.terraformersmc.modmenu.util.mod.Mod;
import com.terraformersmc.modmenu.util.mod.fabric.FabricDummyParentMod;
import dev.modhider.hide.Area;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(FabricDummyParentMod.class)
abstract class FabricDummyParentModMixin {
	@ModifyReturnValue(method = "isHidden", at = @At("RETURN"))
	private boolean modhider$hide(boolean original) {
		return original || Area.MOD_MENU.hides(((Mod) this).getId());
	}
}
