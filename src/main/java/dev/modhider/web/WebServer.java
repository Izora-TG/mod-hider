package dev.modhider.web;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import dev.modhider.ModHider;
import dev.modhider.config.HiderConfig;
import dev.modhider.hide.ModOwners;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.fabricmc.loader.api.metadata.CustomValue;
import net.fabricmc.loader.api.metadata.ModMetadata;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.CreativeModeTab;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * A tiny HTTP/1.1 server bound to 127.0.0.1 only. Hand-rolled on purpose: the JDK's
 * com.sun.net.httpserver module is not guaranteed to be present in the launcher's Java runtime.
 */
public final class WebServer {
	private static final int MAX_BODY = 1 << 20;
	private static final Set<String> BUILTIN_MODS = Set.of("java", "minecraft", "fabricloader");

	private static final int PORT_ATTEMPTS = 20;

	private int port;
	private final ExecutorService workers = Executors.newFixedThreadPool(2, runnable -> {
		Thread thread = new Thread(runnable, "Mod Hider Web");
		thread.setDaemon(true);
		return thread;
	});
	private ServerSocket socket;

	public WebServer(int port) {
		this.port = port;
	}

	public int port() {
		return port;
	}

	public void start() {
		int preferred = port;
		for (int attempt = 0; attempt < PORT_ATTEMPTS; attempt++) {
			int candidate = preferred + attempt;
			if (candidate > 65535) {
				break;
			}
			ServerSocket candidateSocket = null;
			try {
				candidateSocket = new ServerSocket();
				candidateSocket.bind(new InetSocketAddress(InetAddress.getLoopbackAddress(), candidate));
				socket = candidateSocket;
				port = candidate;
				break;
			} catch (IOException e) {
				if (candidateSocket != null) {
					try {
						candidateSocket.close();
					} catch (IOException ignored) {
					}
				}
			}
		}

		if (socket == null) {
			ModHider.LOGGER.error("Webseite konnte nicht starten: Ports {} bis {} sind alle belegt.", preferred, preferred + PORT_ATTEMPTS - 1);
			return;
		}
		if (port != preferred) {
			ModHider.LOGGER.warn("Port {} ist belegt (läuft eventuell eine zweite Spielinstanz?), benutze stattdessen Port {}", preferred, port);
		}

		Thread acceptor = new Thread(this::acceptLoop, "Mod Hider Web Acceptor");
		acceptor.setDaemon(true);
		acceptor.start();
		ModHider.LOGGER.info("Mod Hider Einstellungen: {}", ModHider.webUrl());
	}

	private void acceptLoop() {
		while (!socket.isClosed()) {
			try {
				Socket client = socket.accept();
				workers.execute(() -> handle(client));
			} catch (IOException e) {
				if (!socket.isClosed()) {
					ModHider.LOGGER.warn("Fehler beim Annehmen einer Verbindung", e);
				}
			}
		}
	}

	private void handle(Socket client) {
		try (client) {
			client.setSoTimeout(5000);
			InputStream in = new BufferedInputStream(client.getInputStream());
			OutputStream out = client.getOutputStream();

			String requestLine = readLine(in);
			if (requestLine == null || requestLine.isEmpty()) {
				return;
			}
			String[] parts = requestLine.split(" ");
			if (parts.length < 2) {
				send(out, 400, "text/plain", "Bad Request");
				return;
			}
			String method = parts[0];
			String path = parts[1];
			int query = path.indexOf('?');
			if (query >= 0) {
				path = path.substring(0, query);
			}

			Map<String, String> headers = new HashMap<>();
			String line;
			while ((line = readLine(in)) != null && !line.isEmpty()) {
				int colon = line.indexOf(':');
				if (colon > 0) {
					headers.put(line.substring(0, colon).trim().toLowerCase(Locale.ROOT), line.substring(colon + 1).trim());
				}
			}

			// Only answer requests that are really addressed to us (blocks DNS rebinding) and
			// refuse cross-site writes: other websites can't send a JSON POST without CORS preflight.
			if (!isLocalHost(headers.get("host")) || !isAllowedOrigin(headers.get("origin"))) {
				send(out, 403, "text/plain", "Forbidden");
				return;
			}

			byte[] body = readBody(in, headers);
			route(out, method, path, headers, body);
		} catch (SocketTimeoutException ignored) {
			// Browser kept an idle connection open; nothing to do.
		} catch (Exception e) {
			ModHider.LOGGER.warn("Fehler bei Webanfrage", e);
		}
	}

	private void route(OutputStream out, String method, String path, Map<String, String> headers, byte[] body) throws IOException {
		switch (method + " " + path) {
			case "GET /", "GET /index.html" -> sendResource(out, "index.html", "text/html; charset=utf-8");
			case "GET /api/state" -> send(out, 200, "application/json; charset=utf-8", stateJson());
			case "POST /api/config" -> {
				String type = headers.getOrDefault("content-type", "");
				if (!type.startsWith("application/json")) {
					send(out, 415, "text/plain", "Unsupported Media Type");
					return;
				}
				try {
					HiderConfig.set(HiderConfig.fromJson(new String(body, StandardCharsets.UTF_8)));
				} catch (JsonParseException | IllegalStateException e) {
					send(out, 400, "text/plain; charset=utf-8", "Ungültige Konfiguration: " + e.getMessage());
					return;
				}
				ModHider.applyChanges();
				send(out, 200, "application/json; charset=utf-8", stateJson());
			}
			default -> send(out, 404, "text/plain", "Not Found");
		}
	}

	private String stateJson() {
		Map<String, Integer> keyCounts = new HashMap<>();
		Map<String, Integer> tabCounts = new HashMap<>();
		collectCounts(keyCounts, tabCounts);

		JsonObject state = new JsonObject();
		HiderConfig config = HiderConfig.get();
		state.add("config", JsonParser.parseString(HiderConfig.toJson(config)));
		state.addProperty("runningPort", port);
		state.addProperty("modMenuInstalled", FabricLoader.getInstance().isModLoaded("modmenu"));
		state.addProperty("controllingInstalled", FabricLoader.getInstance().isModLoaded("controlling"));

		JsonArray mods = new JsonArray();
		FabricLoader.getInstance().getAllMods().stream()
				.sorted(Comparator.comparing(mod -> mod.getMetadata().getName().toLowerCase(Locale.ROOT)))
				.forEach(mod -> {
					ModMetadata meta = mod.getMetadata();
					String id = meta.getId();
					if (BUILTIN_MODS.contains(id)) {
						return;
					}
					JsonObject entry = new JsonObject();
					entry.addProperty("id", id);
					entry.addProperty("name", meta.getName());
					entry.addProperty("version", meta.getVersion().getFriendlyString());
					entry.addProperty("description", meta.getDescription());
					entry.addProperty("parent", mod.getContainingMod().map(parent -> parent.getMetadata().getId()).orElse(null));
					entry.addProperty("library", isLibrary(mod));
					entry.addProperty("keybinds", keyCounts.getOrDefault(id, 0));
					entry.addProperty("tabs", tabCounts.getOrDefault(id, 0));
					mods.add(entry);
				});
		state.add("mods", mods);
		return state.toString();
	}

	/** Counted on the render thread so we never read game state mid-update; skipped if the game is busy. */
	private static void collectCounts(Map<String, Integer> keyCounts, Map<String, Integer> tabCounts) {
		Minecraft minecraft = Minecraft.getInstance();
		try {
			minecraft.submit(() -> {
				if (minecraft.options != null) {
					for (KeyMapping mapping : minecraft.options.keyMappings) {
						for (String owner : ModOwners.ownersOf(mapping)) {
							keyCounts.merge(owner, 1, Integer::sum);
						}
					}
				}
				for (CreativeModeTab tab : BuiltInRegistries.CREATIVE_MODE_TAB) {
					String owner = ModOwners.ownerOf(tab);
					if (owner != null && !owner.equals("minecraft")) {
						tabCounts.merge(owner, 1, Integer::sum);
					}
				}
			}).get(2, TimeUnit.SECONDS);
		} catch (Exception e) {
			ModHider.LOGGER.debug("Konnte Tasten/Tabs nicht zählen", e);
		}
	}

	private static boolean isLibrary(ModContainer mod) {
		ModMetadata meta = mod.getMetadata();
		if (meta.getId().startsWith("fabric-") || meta.getId().equals("fabric-api")) {
			return true;
		}
		var modmenu = meta.getCustomValue("modmenu");
		if (modmenu != null && modmenu.getType() == CustomValue.CvType.OBJECT) {
			var badges = modmenu.getAsObject().get("badges");
			if (badges != null && badges.getType() == CustomValue.CvType.ARRAY) {
				for (var badge : badges.getAsArray()) {
					if (badge.getType() == CustomValue.CvType.STRING && badge.getAsString().equals("library")) {
						return true;
					}
				}
			}
		}
		return false;
	}

	private boolean isLocalHost(String host) {
		if (host == null) {
			return false;
		}
		return host.equals("127.0.0.1:" + port) || host.equals("localhost:" + port) || host.equals("[::1]:" + port);
	}

	private boolean isAllowedOrigin(String origin) {
		return origin == null || isLocalHost(origin.replaceFirst("^https?://", ""));
	}

	private static byte[] readBody(InputStream in, Map<String, String> headers) throws IOException {
		String length = headers.get("content-length");
		if (length == null) {
			return new byte[0];
		}
		int size;
		try {
			size = Integer.parseInt(length);
		} catch (NumberFormatException e) {
			throw new IOException("ungültige Content-Length");
		}
		if (size < 0 || size > MAX_BODY) {
			throw new IOException("Body zu groß");
		}
		return in.readNBytes(size);
	}

	private static String readLine(InputStream in) throws IOException {
		ByteArrayOutputStream buffer = new ByteArrayOutputStream();
		int b;
		while ((b = in.read()) != -1) {
			if (b == '\n') {
				break;
			}
			if (b != '\r') {
				buffer.write(b);
			}
			if (buffer.size() > 8192) {
				throw new IOException("Zeile zu lang");
			}
		}
		if (b == -1 && buffer.size() == 0) {
			return null;
		}
		return buffer.toString(StandardCharsets.UTF_8);
	}

	private static void sendResource(OutputStream out, String name, String type) throws IOException {
		try (InputStream resource = WebServer.class.getResourceAsStream("/assets/mod-hider/web/" + name)) {
			if (resource == null) {
				send(out, 404, "text/plain", "Not Found");
				return;
			}
			send(out, 200, type, resource.readAllBytes());
		}
	}

	private static void send(OutputStream out, int status, String type, String body) throws IOException {
		send(out, status, type, body.getBytes(StandardCharsets.UTF_8));
	}

	private static void send(OutputStream out, int status, String type, byte[] body) throws IOException {
		String head = "HTTP/1.1 " + status + " " + reason(status) + "\r\n"
				+ "Content-Type: " + type + "\r\n"
				+ "Content-Length: " + body.length + "\r\n"
				+ "Cache-Control: no-store\r\n"
				+ "X-Content-Type-Options: nosniff\r\n"
				+ "X-Frame-Options: DENY\r\n"
				+ "Connection: close\r\n\r\n";
		out.write(head.getBytes(StandardCharsets.US_ASCII));
		out.write(body);
		out.flush();
	}

	private static String reason(int status) {
		return switch (status) {
			case 200 -> "OK";
			case 400 -> "Bad Request";
			case 403 -> "Forbidden";
			case 404 -> "Not Found";
			case 415 -> "Unsupported Media Type";
			default -> "Error";
		};
	}
}
