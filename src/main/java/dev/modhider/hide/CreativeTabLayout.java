package dev.modhider.hide;

import dev.modhider.mixin.CreativeModeTabAccessor;
import net.fabricmc.fabric.impl.creativetab.FabricCreativeModeTabImpl;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.CreativeModeTab;

import java.util.Comparator;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Re-runs Fabric API's creative tab pagination so hidden tabs leave no gaps.
 * Mirrors {@code CreativeModeTabsMixin#paginateTabs} from fabric-creative-tab-api-v1; hidden tabs
 * report {@code shouldDisplay() == false} and therefore sort to the end, exactly as Fabric does it.
 */
public final class CreativeTabLayout {
	private static final int TABS_PER_PAGE = FabricCreativeModeTabImpl.TABS_PER_PAGE;
	private static final Set<Identifier> VANILLA_TABS = Stream.of(
			"building_blocks", "colored_blocks", "natural_blocks", "functional_blocks", "redstone_blocks", "hotbar",
			"search", "tools_and_utilities", "combat", "food_and_drinks", "ingredients", "spawn_eggs", "op_blocks", "inventory"
	).map(Identifier::withDefaultNamespace).collect(Collectors.toUnmodifiableSet());

	private static final Comparator<Holder.Reference<CreativeModeTab>> ORDER =
			Comparator.<Holder.Reference<CreativeModeTab>, Boolean>comparing(ref -> !ref.value().shouldDisplay())
					.thenComparing(ref -> ref.key().identifier().getNamespace())
					.thenComparing(ref -> ref.key().identifier().getPath());

	private CreativeTabLayout() {
	}

	public static void apply() {
		int count = 0;
		for (Holder.Reference<CreativeModeTab> ref : BuiltInRegistries.CREATIVE_MODE_TAB.listElements().sorted(ORDER).toList()) {
			if (VANILLA_TABS.contains(ref.key().identifier())) {
				continue;
			}

			CreativeModeTab tab = ref.value();
			int pageIndex = count % TABS_PER_PAGE;
			CreativeModeTab.Row row = pageIndex < TABS_PER_PAGE / 2 ? CreativeModeTab.Row.TOP : CreativeModeTab.Row.BOTTOM;
			((FabricCreativeModeTabImpl) tab).fabric_setPage(count / TABS_PER_PAGE + 1);
			((CreativeModeTabAccessor) tab).modhider$setRow(row);
			((CreativeModeTabAccessor) tab).modhider$setColumn(row == CreativeModeTab.Row.TOP ? pageIndex : pageIndex - TABS_PER_PAGE / 2);
			count++;
		}
	}
}
