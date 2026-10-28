package com.qolplus.client;

import java.util.List;
import java.util.Map;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.DoubleConsumer;
import java.util.function.DoubleSupplier;
import java.util.function.Supplier;

import com.qolplus.QolConfig;

/**
 * Every toggleable feature with a name, category, one-line description and its settings (shown when you right-click
 * it in the click GUI). Everything reads and writes QolConfig directly.
 */
public final class Modules {
	public enum Category {
		WORLD("World"), MINING("Mining"), INVENTORY("Inventory"), RENDER("Render"), PERFORMANCE("Performance"), HUD("HUD");

		public final String title;

		Category(String title) {
			this.title = title;
		}
	}

	public sealed interface Setting permits NumberSetting, BoolSetting {
		String name();
	}

	/** Slider. Values snap to step and stay within min..max. */
	public record NumberSetting(String name, double min, double max, double step, String suffix, DoubleSupplier get, DoubleConsumer set)
			implements Setting {
		public double value() {
			return get.getAsDouble();
		}

		public void setValue(double value) {
			double snapped = Math.round((value - min) / step) * step + min;
			set.accept(Math.clamp(snapped, min, max));
		}

		public String display() {
			double v = value();
			return (v == Math.rint(v) ? String.valueOf((int) v) : String.format("%.1f", v)) + suffix;
		}
	}

	public record BoolSetting(String name, BooleanSupplier get, Consumer<Boolean> set) implements Setting {
		public void toggle() {
			set.accept(!get.getAsBoolean());
		}
	}

	public record Module(String name, Category category, String description, Supplier<Boolean> get, Consumer<Boolean> set,
			List<Setting> settings) {
		public Module(String name, Category category, String description, Supplier<Boolean> get, Consumer<Boolean> set) {
			this(name, category, description, get, set, List.of());
		}

		public boolean enabled() {
			return get.get();
		}

		public void toggle() {
			set.accept(!get.get());
			QolConfig.save();
		}
	}

	public static final String ZOOM = "Zoom";
	public static final String FREELOOK = "Freelook";
	public static final int NO_BIND = -1;
	/** Default keybinds (Minecraft's own key numbers): B, F7, Z, Left Alt. */
	private static final Map<String, Integer> DEFAULT_BINDS = Map.of("Excavator", 5, "Light Overlay", 64, ZOOM, 29, FREELOOK, 226);

	public static final List<Module> ALL = List.of(
			new Module("Tree Felling", Category.WORLD, "Chop one log with an axe to fell the whole tree", () -> c().treeFelling, v -> c().treeFelling = v,
					List.of(number("Max Logs", 16, 512, 16, "", () -> c().treeMaxLogs, v -> c().treeMaxLogs = (int) v))),
			new Module("Log Pickup", Category.WORLD, "Felled logs go straight into your inventory", () -> c().treeFellingAutoPickup, v -> c().treeFellingAutoPickup = v),
			new Module("Leaf Decay", Category.WORLD, "Leaves vanish fast after a tree is cut", () -> c().fastLeafDecay, v -> c().fastLeafDecay = v),
			new Module("Grass Clearing", Category.WORLD, "Sneak + hit grass with a hoe to clear the area", () -> c().grassClearing, v -> c().grassClearing = v,
					List.of(number("Radius", 1, 8, 1, "", () -> c().grassRadius, v -> c().grassRadius = (int) v))),
			new Module("Crop Harvest", Category.WORLD, "Right-click grown crops to harvest and replant", () -> c().rightClickHarvest, v -> c().rightClickHarvest = v),
			new Module("Double Doors", Category.WORLD, "Open both halves of a double door at once", () -> c().doubleDoors, v -> c().doubleDoors = v),
			new Module("Infinite Trading", Category.WORLD, "Villager trades never lock", () -> c().infiniteTrading, v -> c().infiniteTrading = v),
			new Module("Fixed Prices", Category.WORLD, "Villager prices never go up (discounts still work)",
					() -> c().fixedVillagerPrices, v -> c().fixedVillagerPrices = v),
			new Module("String Duper", Category.WORLD, "Brings back the old tripwire string duplication",
					() -> c().stringDupe, v -> c().stringDupe = v),

			new Module("Vein Miner", Category.MINING, "Sneak + mine an ore to mine the whole vein", () -> c().veinMining, v -> c().veinMining = v,
					List.of(number("Max Ores", 4, 128, 4, "", () -> c().veinMaxOres, v -> c().veinMaxOres = (int) v))),
			new Module("Excavator", Category.MINING, "Mine 3x3 (or bigger) in stone, dirt and sand", () -> c().excavatorActive, v -> c().excavatorActive = v,
					List.of(number("Size", 3, 7, 2, "x", () -> c().excavatorSize, v -> c().excavatorSize = (int) v))),

			new Module("Chest Search", Category.INVENTORY, "Search bar on top of chests", () -> c().chestSearch, v -> c().chestSearch = v),
			new Module("Chest Sort", Category.INVENTORY, "Sort button on chests and shulker boxes", () -> c().chestSort, v -> c().chestSort = v),
			new Module("Hotbar Refill", Category.INVENTORY, "Refill your hand when a stack runs out", () -> c().hotbarRefill, v -> c().hotbarRefill = v),
			new Module("Shulker Preview", Category.INVENTORY, "See what's inside shulker boxes in their tooltip",
					() -> c().shulkerPreview, v -> c().shulkerPreview = v),
			new Module("Tooltips", Category.INVENTORY, "Food values and durability in item tooltips", () -> c().itemTooltips, v -> c().itemTooltips = v),
			new Module("Dura Warning", Category.INVENTORY, "Ping when a tool or armor piece is almost broken", () -> c().durabilityWarnings, v -> c().durabilityWarnings = v,
					List.of(number("Warn At", 1, 50, 1, "%", () -> c().warningPercent, v -> c().warningPercent = (int) v))),

			new Module("Light Overlay", Category.RENDER, "Red = mobs spawn, yellow = mobs spawn at night", () -> c().lightOverlayOn, v -> c().lightOverlayOn = v,
					List.of(number("Range", 4, 24, 1, "", () -> c().lightRange, v -> c().lightRange = (int) v))),
			new Module(ZOOM, Category.RENDER, "Hold the keybind to zoom (Z by default), scroll to zoom more", () -> c().zoom, v -> c().zoom = v,
					List.of(number("Amount", 2, 10, 0.5, "x", () -> c().zoomFactor, v -> c().zoomFactor = v),
							bool("Smooth", () -> c().zoomSmooth, v -> c().zoomSmooth = v),
							bool("Scroll", () -> c().zoomScroll, v -> c().zoomScroll = v),
							bool("Cinematic", () -> c().zoomSmoothCamera, v -> c().zoomSmoothCamera = v))),
			new Module(FREELOOK, Category.RENDER, "Hold the keybind (Left Alt) to look around without turning",
					() -> c().freelook, v -> c().freelook = v),
			new Module("Crosshair", Category.RENDER, "Custom crosshair (style 1 cross, 2 dot, 3 both, 4 square)",
					() -> c().customCrosshair, v -> c().customCrosshair = v,
					List.of(number("Style", 1, 4, 1, "", () -> c().crosshairStyle, v -> c().crosshairStyle = (int) v),
							number("Size", 1, 10, 1, "", () -> c().crosshairSize, v -> c().crosshairSize = (int) v),
							number("Gap", 0, 8, 1, "", () -> c().crosshairGap, v -> c().crosshairGap = (int) v),
							number("Thickness", 1, 4, 1, "", () -> c().crosshairThickness, v -> c().crosshairThickness = (int) v),
							bool("Colored", () -> c().crosshairColored, v -> c().crosshairColored = v),
							number("Hue", 0, 359, 1, "", () -> c().crosshairHue, v -> c().crosshairHue = (int) v),
							bool("Outline", () -> c().crosshairOutline, v -> c().crosshairOutline = v))),
			new Module("Hit Color", Category.RENDER, "Change the red flash when mobs get hit",
					() -> c().hitColor, v -> c().hitColor = v,
					List.of(number("Hue", 0, 359, 1, "", () -> c().hitHue, v -> c().hitHue = (int) v),
							number("Strength", 10, 100, 5, "%", () -> c().hitOpacity, v -> c().hitOpacity = (int) v))),
			new Module("Fullbright", Category.RENDER, "See clearly in the dark, like night vision", () -> c().fullbright, v -> c().fullbright = v),
			new Module("Death Marker", Category.RENDER, "Arrow pointing back to where you died", () -> c().deathWaypoint, v -> c().deathWaypoint = v),

			new Module("Auto Render Dist", Category.PERFORMANCE, "Lowers render distance when FPS drops, raises it back after",
					() -> c().autoRenderDistance, v -> c().autoRenderDistance = v,
					List.of(number("Target FPS", 30, 240, 10, "", () -> c().targetFps, v -> c().targetFps = (int) v),
							number("Min Distance", 2, 16, 1, "", () -> c().minRenderDistance, v -> c().minRenderDistance = (int) v),
							number("Max Distance", 4, 32, 1, "", () -> c().maxRenderDistance, v -> c().maxRenderDistance = (int) v))),
			new Module("No Particles", Category.PERFORMANCE, "Removes every particle (smoke, breaking, rain splashes...)",
					() -> c().noParticles, v -> c().noParticles = v),
			new Module("No Shadows", Category.PERFORMANCE, "Turns off the round shadows under mobs and items",
					() -> c().noEntityShadows, v -> c().noEntityShadows = v),
			new Module("Hide Far Signs", Category.PERFORMANCE, "Signs and banners far away aren't drawn",
					() -> c().hideFarSigns, v -> c().hideFarSigns = v,
					List.of(number("Distance", 4, 64, 4, "", () -> c().signDistance, v -> c().signDistance = (int) v))),

			new Module("FPS Caps", Category.PERFORMANCE, "Lower FPS when tabbed out or in menus (cooler, quieter PC)",
					() -> c().fpsCaps, v -> c().fpsCaps = v,
					List.of(number("Background", 5, 60, 5, "", () -> c().backgroundFps, v -> c().backgroundFps = (int) v),
							number("Menus", 30, 240, 10, "", () -> c().menuFps, v -> c().menuFps = (int) v))),

			new Module("Theme", Category.HUD, "Menu and HUD colors (turn on for rainbow)", () -> c().themeRainbow, v -> c().themeRainbow = v,
					List.of(number("Hue", 0, 359, 1, "", () -> c().themeHue, v -> c().themeHue = (int) v),
							number("Speed", 1, 10, 1, "", () -> c().rainbowSpeed, v -> c().rainbowSpeed = (int) v),
							number("Opacity", 30, 100, 5, "%", () -> c().panelOpacity, v -> c().panelOpacity = (int) v),
							bool("Classic Menu", () -> c().classicMenu, v -> c().classicMenu = v))),
			new Module("Watermark", Category.HUD, "Client name in the top left", () -> c().hudWatermark, v -> c().hudWatermark = v),
			new Module("Module List", Category.HUD, "Enabled modules in the top right", () -> c().hudModuleList, v -> c().hudModuleList = v,
					List.of(bool("Rainbow", () -> c().moduleListRainbow, v -> c().moduleListRainbow = v),
							bool("Background", () -> c().moduleListBackground, v -> c().moduleListBackground = v))),
			new Module("FPS", Category.HUD, "Frames per second", () -> c().hudFps, v -> c().hudFps = v),
			new Module("CPS", Category.HUD, "Left and right clicks per second", () -> c().hudCps, v -> c().hudCps = v),
			new Module("Ping", Category.HUD, "Your connection delay to the server", () -> c().hudPing, v -> c().hudPing = v),
			new Module("Keystrokes", Category.HUD, "Shows WASD, jump and mouse buttons as you press them", () -> c().hudKeystrokes, v -> c().hudKeystrokes = v,
					List.of(bool("Mouse", () -> c().keystrokesMouse, v -> c().keystrokesMouse = v),
							bool("Space", () -> c().keystrokesSpace, v -> c().keystrokesSpace = v))),
			new Module("Day Counter", Category.HUD, "Which in-game day it is", () -> c().hudDay, v -> c().hudDay = v),
			new Module("Session Stats", Category.HUD, "Blocks mined, kills, distance and time this session",
					() -> c().hudSessionStats, v -> c().hudSessionStats = v),
			new Module("Clock", Category.HUD, "Real-life time", () -> c().hudClock, v -> c().hudClock = v,
					List.of(bool("24 Hour", () -> c().clock24h, v -> c().clock24h = v))),
			new Module("Direction", Category.HUD, "Which way you're facing", () -> c().hudDirection, v -> c().hudDirection = v),
			new Module("Coords", Category.HUD, "Your coordinates", () -> c().hudCoords, v -> c().hudCoords = v),
			new Module("Armor HUD", Category.HUD, "Armor durability next to the hotbar", () -> c().armorHud, v -> c().armorHud = v),
			new Module("Firework HUD", Category.HUD, "Rocket count while wearing an elytra", () -> c().fireworkHud, v -> c().fireworkHud = v));

	private Modules() {
	}

	/** Key code bound to a module, or NO_BIND. */
	public static int bind(String module) {
		Integer key = c().binds.get(module);
		return key != null ? key : DEFAULT_BINDS.getOrDefault(module, NO_BIND);
	}

	public static void setBind(String module, int key) {
		c().binds.put(module, key);
		QolConfig.save();
	}

	private static QolConfig c() {
		return QolConfig.get();
	}

	private static NumberSetting number(String name, double min, double max, double step, String suffix, DoubleSupplier get, DoubleConsumer set) {
		return new NumberSetting(name, min, max, step, suffix, get, set);
	}

	private static BoolSetting bool(String name, BooleanSupplier get, Consumer<Boolean> set) {
		return new BoolSetting(name, get, set);
	}
}
