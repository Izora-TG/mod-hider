package dev.modhider.hide;

import dev.modhider.ModHider;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.minecraft.client.KeyMapping;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.CreativeModeTab;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/** Works out which mod a key mapping or creative tab belongs to. */
public final class ModOwners {
	/** Mods that never count as the "owner" of something another mod created. */
	private static final Set<String> FRAMEWORK_MODS = Set.of("minecraft", "java", "fabricloader", "mixinextras", ModHider.MOD_ID);
	private static final Optional<String> NONE = Optional.empty();

	private static final Map<KeyMapping, String> REGISTRANTS = Collections.synchronizedMap(new IdentityHashMap<>());
	private static final Map<String, Optional<String>> CLASS_OWNERS = new ConcurrentHashMap<>();
	private static final StackWalker WALKER = StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE);

	private ModOwners() {
	}

	/** Called from every KeyMapping constructor: remembers which mod's code created it. */
	public static void recordRegistrant(KeyMapping mapping) {
		String owner = WALKER.walk(frames -> frames
				.map(StackWalker.StackFrame::getDeclaringClass)
				.filter(c -> !isFrameworkClass(c.getName()))
				.map(ModOwners::modOfClass)
				.flatMap(Optional::stream)
				.findFirst()
				.orElse(null));
		if (owner != null) {
			REGISTRANTS.put(mapping, owner);
		}
	}

	/** Every mod that can plausibly claim this key: whoever created it, its category's namespace and its translation key. */
	public static Set<String> ownersOf(KeyMapping mapping) {
		Set<String> owners = new LinkedHashSet<>(2);
		String registrant = REGISTRANTS.get(mapping);
		if (registrant != null) {
			owners.add(registrant);
		}

		addIfMod(owners, mapping.getCategory().id().getNamespace());

		// "key.<modid>.something" is the convention almost every mod follows.
		String[] parts = mapping.getName().split("\\.");
		if (parts.length >= 3 && parts[0].equals("key")) {
			addIfMod(owners, parts[1]);
		}
		return owners;
	}

	public static boolean isHidden(KeyMapping mapping, Area area) {
		for (String owner : ownersOf(mapping)) {
			if (area.hides(owner)) {
				return true;
			}
		}
		return false;
	}

	public static KeyMapping[] filter(KeyMapping[] mappings, Area area) {
		if (!area.hidesAnything()) {
			return mappings;
		}
		List<KeyMapping> visible = new ArrayList<>(mappings.length);
		for (KeyMapping mapping : mappings) {
			if (!isHidden(mapping, area)) {
				visible.add(mapping);
			}
		}
		return visible.toArray(KeyMapping[]::new);
	}

	public static String ownerOf(CreativeModeTab tab) {
		Identifier id = BuiltInRegistries.CREATIVE_MODE_TAB.getKey(tab);
		return id == null ? null : id.getNamespace();
	}

	private static void addIfMod(Set<String> owners, String id) {
		if (!FRAMEWORK_MODS.contains(id) && FabricLoader.getInstance().isModLoaded(id)) {
			owners.add(id);
		}
	}

	private static boolean isFrameworkClass(String name) {
		return name.startsWith("java.") || name.startsWith("jdk.") || name.startsWith("sun.")
				|| name.startsWith("net.minecraft.") || name.startsWith("com.mojang.")
				|| name.startsWith("net.fabricmc.") || name.startsWith("org.spongepowered.")
				|| name.startsWith("com.llamalad7.") || name.startsWith("dev.modhider.");
	}

	/** Finds the mod whose jar (or build folder in dev) contains the given class. Works for nested jars too. */
	private static Optional<String> modOfClass(Class<?> type) {
		return CLASS_OWNERS.computeIfAbsent(type.getName(), name -> {
			String file = name.replace('.', '/') + ".class";
			for (ModContainer mod : FabricLoader.getInstance().getAllMods()) {
				String id = mod.getMetadata().getId();
				if (FRAMEWORK_MODS.contains(id)) {
					continue;
				}
				for (Path root : mod.getRootPaths()) {
					if (Files.exists(root.resolve(file))) {
						return Optional.of(id);
					}
				}
			}
			return NONE;
		});
	}
}
