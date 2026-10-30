package dev.modhider.mixin.compat.controlling;

import com.blamejared.controlling.client.NewKeyBindsList;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import dev.modhider.hide.Area;
import dev.modhider.hide.ModOwners;
import net.minecraft.client.KeyMapping;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Controlling builds its own list (and search index) from Options.keyMappings; filter it at the source. */
@Mixin(NewKeyBindsList.class)
abstract class NewKeyBindsListMixin {
	@ModifyExpressionValue(method = "<init>", at = @At(value = "INVOKE", target = "Lorg/apache/commons/lang3/ArrayUtils;clone([Ljava/lang/Object;)[Ljava/lang/Object;"))
	private Object[] modhider$hideKeys(Object[] mappings) {
		return ModOwners.filter((KeyMapping[]) mappings, Area.CONTROLLING);
	}
}
