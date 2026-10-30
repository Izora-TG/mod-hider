package dev.modhider.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import dev.modhider.hide.Area;
import dev.modhider.hide.ModOwners;
import net.minecraft.world.item.CreativeModeTab;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Vanilla and Fabric API both decide tab visibility (and page count) through shouldDisplay(). */
@Mixin(CreativeModeTab.class)
abstract class CreativeModeTabMixin {
	@ModifyReturnValue(method = "shouldDisplay", at = @At("RETURN"))
	private boolean modhider$hideTab(boolean original) {
		return original && !Area.CREATIVE_TABS.hides(ModOwners.ownerOf((CreativeModeTab) (Object) this));
	}
}
