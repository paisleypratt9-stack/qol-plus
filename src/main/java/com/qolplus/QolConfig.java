package com.qolplus;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import net.fabricmc.loader.api.FabricLoader;

/** Settings stored in config/qolplus.json. Editable in-game through Mod Menu, or by hand. */
public final class QolConfig {
	private static final Logger LOGGER = LoggerFactory.getLogger(QolPlus.MOD_ID);
	private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("qolplus.json");
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static QolConfig instance = new QolConfig();

	// World features (run on the server side)
	public boolean treeFelling = true;
	public boolean treeFellingAutoPickup = true;
	public boolean fastLeafDecay = true;
	public boolean grassClearing = true;
	public boolean veinMining = true;
	public boolean excavator = true;
	public boolean infiniteTrading = false;
	public boolean fixedVillagerPrices = false;
	public boolean stringDupe = false;
	public int treeMaxLogs = 256;
	public int grassRadius = 3;
	public int veinMaxOres = 64;
	public int excavatorSize = 3;
	public boolean rightClickHarvest = true;
	public boolean doubleDoors = true;
	public boolean hotbarRefill = true;
	public boolean chestSort = true;

	// Client features
	public boolean chestSearch = true;
	public boolean itemTooltips = true;
	public boolean durabilityWarnings = true;
	public int warningPercent = 10;
	public boolean armorHud = true;
	public boolean fireworkHud = true;
	public boolean deathWaypoint = true;
	public boolean lightOverlay = true;
	public boolean zoom = true;
	public boolean fullbright = false;
	public boolean zoomSmooth = true;
	public boolean zoomScroll = true;
	public boolean zoomSmoothCamera = false;
	public boolean excavatorActive = false;
	public boolean lightOverlayOn = false;
	/** Module name to key code (Minecraft's own key numbers), or -1 for cleared. Missing ones use the defaults in Modules. */
	public Map<String, Integer> binds = new HashMap<>();
	public int lightRange = 12;
	public double zoomFactor = 4;

	// HUD widgets
	public boolean hudWatermark = true;
	public boolean hudModuleList = true;
	public boolean hudFps = true;
	public boolean hudCps = true;
	public boolean hudPing = true;
	public boolean hudKeystrokes = true;
	public boolean hudClock = false;
	public boolean hudDirection = false;
	public boolean hudCoords = false;
	public boolean moduleListRainbow = true;
	public boolean moduleListBackground = true;
	public boolean keystrokesMouse = true;
	public boolean keystrokesSpace = true;
	public boolean clock24h = false;

	// Theme
	public boolean themeRainbow = false;
	public int themeHue = 262;
	public int rainbowSpeed = 4;
	public int panelOpacity = 90;
	public boolean classicMenu = false;

	// Performance
	public boolean autoRenderDistance = false;
	public int targetFps = 60;
	public int minRenderDistance = 6;
	public int maxRenderDistance = 16;
	public boolean noParticles = false;
	public boolean noEntityShadows = false;
	public boolean hideFarSigns = false;
	public int signDistance = 16;
	public boolean fpsCaps = false;
	public int backgroundFps = 15;
	public int menuFps = 60;

	// More client features
	public boolean shulkerPreview = true;
	public boolean freelook = true;
	public boolean hudDay = false;
	public boolean hudSessionStats = false;
	public boolean customCrosshair = false;
	public int crosshairStyle = 1;
	public int crosshairSize = 4;
	public int crosshairGap = 2;
	public int crosshairThickness = 1;
	public int crosshairHue = 262;
	public boolean crosshairColored = false;
	public boolean crosshairOutline = true;
	public boolean hitColor = false;
	public int hitHue = 262;
	public int hitOpacity = 70;

	public static QolConfig get() {
		return instance;
	}

	public static void load() {
		if (Files.exists(FILE)) {
			try (Reader reader = Files.newBufferedReader(FILE)) {
				QolConfig loaded = GSON.fromJson(reader, QolConfig.class);
				if (loaded != null) instance = loaded;
			} catch (IOException | RuntimeException e) {
				LOGGER.warn("Could not read {}, using defaults", FILE, e);
			}
		}
		instance.warningPercent = Math.clamp(instance.warningPercent, 1, 50);
		save(); // writes any options added in newer versions
	}

	public static void save() {
		try (Writer writer = Files.newBufferedWriter(FILE)) {
			GSON.toJson(instance, writer);
		} catch (IOException e) {
			LOGGER.warn("Could not save {}", FILE, e);
		}
	}
}
