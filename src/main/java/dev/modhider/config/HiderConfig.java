package dev.modhider.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import dev.modhider.ModHider;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Set;
import java.util.TreeSet;

/**
 * Immutable-by-convention config snapshot. A new instance is swapped in on every change,
 * so readers on the render thread never see a half-written config.
 */
public final class HiderConfig {
	public static final int DEFAULT_PORT = 25580;

	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("mod-hider.json");

	private static volatile HiderConfig current = new HiderConfig();

	public int port = DEFAULT_PORT;
	public boolean hideInModMenu = true;
	public boolean hideInKeybinds = true;
	public boolean hideInControlling = true;
	public boolean hideCreativeTabs = true;
	public Set<String> hiddenInModMenu = new TreeSet<>();
	/** Also used for Controlling, which shows the same key mappings. */
	public Set<String> hiddenInKeybinds = new TreeSet<>();
	public Set<String> hiddenInCreativeTabs = new TreeSet<>();
	/** The old single list shared by every area; migrated into all three lists on load. */
	public Set<String> hiddenMods;

	public static HiderConfig get() {
		return current;
	}

	public static void set(HiderConfig config) {
		current = config.sanitized();
		save();
	}

	public static void load() {
		if (!Files.exists(FILE)) {
			save();
			return;
		}

		try (Reader reader = Files.newBufferedReader(FILE)) {
			HiderConfig loaded = GSON.fromJson(reader, HiderConfig.class);
			current = loaded == null ? new HiderConfig() : loaded.sanitized();
		} catch (IOException | JsonParseException e) {
			ModHider.LOGGER.error("Konnte {} nicht lesen, benutze Standardwerte", FILE, e);
		}
	}

	private static synchronized void save() {
		try {
			Path tmp = FILE.resolveSibling(FILE.getFileName() + ".tmp");
			try (Writer writer = Files.newBufferedWriter(tmp)) {
				GSON.toJson(current, writer);
			}
			Files.move(tmp, FILE, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
		} catch (IOException e) {
			ModHider.LOGGER.error("Konnte {} nicht speichern", FILE, e);
		}
	}

	public static String toJson(HiderConfig config) {
		return GSON.toJson(config);
	}

	public static HiderConfig fromJson(String json) {
		HiderConfig config = GSON.fromJson(json, HiderConfig.class);
		if (config == null) {
			throw new JsonParseException("leerer Inhalt");
		}
		return config.sanitized();
	}

	private HiderConfig sanitized() {
		HiderConfig copy = new HiderConfig();
		copy.port = port > 0 && port < 65536 ? port : DEFAULT_PORT;
		copy.hideInModMenu = hideInModMenu;
		copy.hideInKeybinds = hideInKeybinds;
		copy.hideInControlling = hideInControlling;
		copy.hideCreativeTabs = hideCreativeTabs;
		copyIds(hiddenInModMenu, copy.hiddenInModMenu);
		copyIds(hiddenInKeybinds, copy.hiddenInKeybinds);
		copyIds(hiddenInCreativeTabs, copy.hiddenInCreativeTabs);
		copyIds(hiddenMods, copy.hiddenInModMenu);
		copyIds(hiddenMods, copy.hiddenInKeybinds);
		copyIds(hiddenMods, copy.hiddenInCreativeTabs);
		return copy;
	}

	private static void copyIds(Set<String> from, Set<String> to) {
		if (from == null) {
			return;
		}
		for (String id : from) {
			if (id != null && !id.isBlank()) {
				to.add(id.trim());
			}
		}
	}
}
