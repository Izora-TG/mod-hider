package dev.modhider.mixin;

import dev.modhider.hide.ModOwners;
import net.minecraft.client.KeyMapping;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(KeyMapping.class)
abstract class KeyMappingMixin {
	@Inject(method = "<init>(Ljava/lang/String;Lcom/mojang/blaze3d/platform/InputConstants$Type;ILnet/minecraft/client/KeyMapping$Category;I)V", at = @At("TAIL"))
	private void modhider$recordRegistrant(CallbackInfo ci) {
		ModOwners.recordRegistrant((KeyMapping) (Object) this);
	}
}
