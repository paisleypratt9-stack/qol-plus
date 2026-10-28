package com.qolplus.client;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

import com.qolplus.QolConfig;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

/** Settings screen, opened from Mod Menu. */
public class QolConfigScreen extends Screen {
	private record Toggle(String key, Supplier<Boolean> get, Consumer<Boolean> set) {
	}

	private static final int[] PERCENT_STEPS = {5, 10, 15, 20, 25};

	private final Screen parent;

	public QolConfigScreen(Screen parent) {
		super(Component.translatable("screen.qolplus.config"));
		this.parent = parent;
	}

	@Override
	protected void init() {
		QolConfig c = QolConfig.get();
		List<Toggle> toggles = List.of(
				new Toggle("treeFelling", () -> c.treeFelling, v -> c.treeFelling = v),
				new Toggle("treeFellingAutoPickup", () -> c.treeFellingAutoPickup, v -> c.treeFellingAutoPickup = v),
				new Toggle("fastLeafDecay", () -> c.fastLeafDecay, v -> c.fastLeafDecay = v),
				new Toggle("grassClearing", () -> c.grassClearing, v -> c.grassClearing = v),
				new Toggle("veinMining", () -> c.veinMining, v -> c.veinMining = v),
				new Toggle("excavator", () -> c.excavator, v -> c.excavator = v),
				new Toggle("rightClickHarvest", () -> c.rightClickHarvest, v -> c.rightClickHarvest = v),
				new Toggle("doubleDoors", () -> c.doubleDoors, v -> c.doubleDoors = v),
				new Toggle("hotbarRefill", () -> c.hotbarRefill, v -> c.hotbarRefill = v),
				new Toggle("chestSort", () -> c.chestSort, v -> c.chestSort = v),
				new Toggle("chestSearch", () -> c.chestSearch, v -> c.chestSearch = v),
				new Toggle("itemTooltips", () -> c.itemTooltips, v -> c.itemTooltips = v),
				new Toggle("durabilityWarnings", () -> c.durabilityWarnings, v -> c.durabilityWarnings = v),
				new Toggle("armorHud", () -> c.armorHud, v -> c.armorHud = v),
				new Toggle("fireworkHud", () -> c.fireworkHud, v -> c.fireworkHud = v),
				new Toggle("deathWaypoint", () -> c.deathWaypoint, v -> c.deathWaypoint = v),
				new Toggle("lightOverlay", () -> c.lightOverlay, v -> c.lightOverlay = v),
				new Toggle("zoom", () -> c.zoom, v -> c.zoom = v));

		int left = width / 2 - 155;
		int top = 30;
		int i = 0;
		// Three columns so everything fits on one screen.
		for (Toggle toggle : toggles) {
			addRenderableWidget(CycleButton.onOffBuilder(toggle.get().get()).create(
					left + (i % 3) * 105, top + (i / 3) * 22, 100, 20,
					Component.translatable("option.qolplus." + toggle.key()),
					(button, value) -> toggle.set().accept(value)));
			i++;
		}

		addRenderableWidget(Button.builder(percentLabel(c.warningPercent), b -> {
			c.warningPercent = next(PERCENT_STEPS, c.warningPercent);
			b.setMessage(percentLabel(c.warningPercent));
		}).bounds(left + (i % 3) * 105, top + (i / 3) * 22, 100, 20).build());

		addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, b -> onClose())
				.bounds(width / 2 - 100, height - 28, 200, 20).build());
	}

	private static int next(int[] steps, int current) {
		for (int step : steps) if (step > current) return step;
		return steps[0];
	}

	private static Component percentLabel(int percent) {
		return Component.translatable("option.qolplus.warningPercent", percent);
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
		super.extractRenderState(graphics, mouseX, mouseY, partialTick);
		graphics.centeredText(font, title, width / 2, 14, 0xFFFFFFFF);
		graphics.centeredText(font, Component.translatable("screen.qolplus.config.server_note"), width / 2, height - 44, 0xFFAAAAAA);
	}

	@Override
	public void onClose() {
		QolConfig.save();
		minecraft.setScreenAndShow(parent);
	}
}
