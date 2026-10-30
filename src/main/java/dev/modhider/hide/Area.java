package dev.modhider.hide;

import dev.modhider.config.HiderConfig;

import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;

/** The places a mod can be hidden from, each with its own on/off switch and list in the config. */
public enum Area {
	MOD_MENU(c -> c.hideInModMenu, c -> c.hiddenInModMenu),
	KEYBINDS(c -> c.hideInKeybinds, c -> c.hiddenInKeybinds),
	CONTROLLING(c -> c.hideInControlling, c -> c.hiddenInKeybinds),
	CREATIVE_TABS(c -> c.hideCreativeTabs, c -> c.hiddenInCreativeTabs);

	private final Predicate<HiderConfig> enabled;
	private final Function<HiderConfig, Set<String>> hiddenMods;

	Area(Predicate<HiderConfig> enabled, Function<HiderConfig, Set<String>> hiddenMods) {
		this.enabled = enabled;
		this.hiddenMods = hiddenMods;
	}

	public boolean hides(String modId) {
		HiderConfig config = HiderConfig.get();
		return modId != null && enabled.test(config) && hiddenMods.apply(config).contains(modId);
	}

	public boolean hidesAnything() {
		HiderConfig config = HiderConfig.get();
		return enabled.test(config) && !hiddenMods.apply(config).isEmpty();
	}
}
