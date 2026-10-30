package dev.modhider.mixin;

import net.minecraft.world.item.CreativeModeTab;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(CreativeModeTab.class)
public interface CreativeModeTabAccessor {
	@Mutable
	@Accessor("row")
	void modhider$setRow(CreativeModeTab.Row row);

	@Mutable
	@Accessor("column")
	void modhider$setColumn(int column);
}
