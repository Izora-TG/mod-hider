package dev.modhider.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import dev.modhider.hide.Area;
import dev.modhider.hide.ModOwners;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.screens.options.controls.KeyBindsList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(KeyBindsList.class)
abstract class KeyBindsListMixin {
	@ModifyExpressionValue(method = "<init>", at = @At(value = "INVOKE", target = "Lorg/apache/commons/lang3/ArrayUtils;clone([Ljava/lang/Object;)[Ljava/lang/Object;"))
	private Object[] modhider$hideKeys(Object[] mappings) {
		return ModOwners.filter((KeyMapping[]) mappings, Area.KEYBINDS);
	}
}
