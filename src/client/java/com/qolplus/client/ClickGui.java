package com.qolplus.client;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import com.qolplus.QolConfig;

import com.mojang.blaze3d.platform.InputConstants;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

/**
 * Right Shift menu: one draggable panel per category. Left-click a module to toggle it, right-click to open its
 * settings underneath (sliders, switches and its keybind).
 */
public class ClickGui extends Screen {
	private static final int PANEL_WIDTH = 92;
	private static final int HEADER = 16;
	private static final int ROW = 13;
	private static final int SLIDER_ROW = 17;
	private static final int SWITCH_ROW = 12;
	private static final int ROW_HOVER = 0x14FFFFFF;
	private static final int TEXT_OFF = 0xFFB4B4C0;
	private static final int TEXT_DIM = 0xFF8A8A99;

	/** One line in a panel: a module, one of the settings of an expanded module, or its keybind (bind = true). */
	private record Row(int y, int height, Modules.Module module, Modules.Setting setting, boolean bind) {
	}

	/** Panel positions and which modules are expanded, kept while the game is open. */
	private static final Map<Modules.Category, int[]> positions = new EnumMap<>(Modules.Category.class);
	private static final Set<Modules.Module> expanded = new HashSet<>();

	private EditBox search;
	private Modules.Category dragging;
	private int dragOffsetX;
	private int dragOffsetY;
	private Modules.NumberSetting sliding;
	/** Module waiting for a key press to bind, or null. */
	private Modules.Module listening;
	private int slideX;

	public ClickGui() {
		super(Component.literal("QoL+"));
	}

	// Theme colors, shared with the HUD.

	static int accent() {
		QolConfig c = QolConfig.get();
		return c.themeRainbow ? HudWidgets.rainbow(0) : 0xFF000000 | Mth.hsvToRgb(c.themeHue / 360f, 0.65f, 0.96f);
	}

	static int accentDark() {
		int a = accent();
		return 0xFF000000 | ((a >> 16 & 0xFF) * 6 / 10) << 16 | ((a >> 8 & 0xFF) * 6 / 10) << 8 | (a & 0xFF) * 6 / 10;
	}

	static int withAlpha(int rgb, int alpha) {
		return alpha << 24 | (rgb & 0xFFFFFF);
	}

	static int panelBg() {
		return withAlpha(0x111118, QolConfig.get().panelOpacity * 255 / 100);
	}

	@Override
	protected void init() {
		Modules.Category[] categories = Modules.Category.values();
		int gap = 4;
		int startX = Math.max(4, (width - categories.length * (PANEL_WIDTH + gap) + gap) / 2);
		for (int i = 0; i < categories.length; i++) {
			positions.putIfAbsent(categories[i], new int[] {startX + i * (PANEL_WIDTH + gap), 34});
		}
		search = new EditBox(font, width / 2 - 60, 10, 120, 14, Component.literal("Search"));
		search.setHint(Component.literal("Search modules..."));
		addRenderableWidget(search);
	}

	private List<Row> rows(Modules.Category category) {
		String query = search.getValue().trim().toLowerCase(Locale.ROOT);
		List<Row> rows = new ArrayList<>();
		int y = positions.get(category)[1] + HEADER + 2;
		for (Modules.Module module : Modules.ALL) {
			if (module.category() != category || !(query.isEmpty() || module.name().toLowerCase(Locale.ROOT).contains(query))) continue;
			rows.add(new Row(y, ROW, module, null, false));
			y += ROW;
			if (!expanded.contains(module)) continue;
			for (Modules.Setting setting : module.settings()) {
				int h = setting instanceof Modules.NumberSetting ? SLIDER_ROW : SWITCH_ROW;
				rows.add(new Row(y, h, module, setting, false));
				y += h;
			}
			rows.add(new Row(y, SWITCH_ROW, module, null, true));
			y += SWITCH_ROW;
		}
		return rows;
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
		super.extractRenderState(graphics, mouseX, mouseY, partialTick);
		int accent = accent();
		String hint = null;

		for (Modules.Category category : Modules.Category.values()) {
			int[] pos = positions.get(category);
			int x = pos[0];
			int y = pos[1];
			List<Row> rows = rows(category);
			int bottom = (rows.isEmpty() ? y + HEADER + 2 : rows.getLast().y() + rows.getLast().height()) + 2;

			graphics.fill(x - 1, y - 1, x + PANEL_WIDTH + 1, bottom + 1, 0x60000000);
			graphics.fillGradient(x, y, x + PANEL_WIDTH, y + HEADER, accent, accentDark());
			graphics.text(font, category.title, x + 6, y + 4, 0xFFFFFFFF, true);
			long on = Modules.ALL.stream().filter(m -> m.category() == category && m.enabled()).count();
			String count = on + "/" + Modules.ALL.stream().filter(m -> m.category() == category).count();
			graphics.text(font, count, x + PANEL_WIDTH - 6 - font.width(count), y + 4, 0xC0FFFFFF, false);
			graphics.fill(x, y + HEADER, x + PANEL_WIDTH, bottom, panelBg());

			for (Row row : rows) {
				boolean over = mouseX >= x && mouseX < x + PANEL_WIDTH && mouseY >= row.y() && mouseY < row.y() + row.height();
				if (row.setting() == null && !row.bind()) {
					Modules.Module module = row.module();
					if (module.enabled()) {
						graphics.fill(x, row.y(), x + PANEL_WIDTH, row.y() + ROW, withAlpha(accent, 0x30));
						graphics.fill(x, row.y(), x + 2, row.y() + ROW, accent);
					}
					if (over) {
						graphics.fill(x, row.y(), x + PANEL_WIDTH, row.y() + ROW, ROW_HOVER);
						hint = module.description() + "  (right-click for settings)";
					}
					graphics.text(font, module.name(), x + 7, row.y() + 3, module.enabled() ? accent : TEXT_OFF, false);
					graphics.text(font, expanded.contains(module) ? "-" : "+", x + PANEL_WIDTH - 9, row.y() + 3, TEXT_DIM, false);
					int key = Modules.bind(module.name());
					if (key != Modules.NO_BIND) {
						String keyName = keyName(key);
						graphics.text(font, keyName, x + PANEL_WIDTH - 13 - font.width(keyName), row.y() + 3, TEXT_DIM, false);
					}
				} else {
					graphics.fill(x, row.y(), x + PANEL_WIDTH, row.y() + row.height(), 0x40000000);
					graphics.fill(x + 3, row.y(), x + 4, row.y() + row.height(), withAlpha(accent, 0x80));
					drawSetting(graphics, row, x, accent, over);
				}
			}
		}

		if (hint != null) {
			int w = font.width(hint);
			int tx = width / 2 - w / 2;
			int ty = height - 22;
			graphics.fill(tx - 6, ty - 4, tx + w + 6, ty + 12, panelBg());
			graphics.fill(tx - 6, ty + 12, tx + w + 6, ty + 13, accent);
			graphics.text(font, hint, tx, ty, 0xFFFFFFFF, false);
		}
	}

	private static String keyName(int key) {
		return InputConstants.Type.KEYBOARD.getOrCreate(key).getDisplayName().getString();
	}

	private void drawSetting(GuiGraphicsExtractor graphics, Row row, int x, int accent, boolean over) {
		int y = row.y();
		if (row.bind()) {
			int key = Modules.bind(row.module().name());
			String value = listening == row.module() ? "Press a key..." : key == Modules.NO_BIND ? "None" : keyName(key);
			graphics.text(font, "Bind", x + 9, y + 2, over ? 0xFFFFFFFF : TEXT_OFF, false);
			graphics.text(font, value, x + PANEL_WIDTH - 6 - font.width(value), y + 2, listening == row.module() ? accent : 0xFFFFFFFF, false);
		} else if (row.setting() instanceof Modules.NumberSetting number) {
			graphics.text(font, number.name(), x + 9, y + 2, over ? 0xFFFFFFFF : TEXT_OFF, false);
			String value = number.display();
			graphics.text(font, value, x + PANEL_WIDTH - 6 - font.width(value), y + 2, 0xFFFFFFFF, false);
			int barX0 = x + 9;
			int barX1 = x + PANEL_WIDTH - 6;
			double fraction = (number.value() - number.min()) / (number.max() - number.min());
			int fillX = barX0 + (int) Math.round((barX1 - barX0) * fraction);
			graphics.fill(barX0, y + 12, barX1, y + 14, 0xFF2A2A35);
			graphics.fill(barX0, y + 12, fillX, y + 14, accent);
			graphics.fill(fillX - 1, y + 11, fillX + 1, y + 15, 0xFFFFFFFF);
		} else if (row.setting() instanceof Modules.BoolSetting bool) {
			boolean on = bool.get().getAsBoolean();
			graphics.text(font, bool.name(), x + 9, y + 2, over ? 0xFFFFFFFF : TEXT_OFF, false);
			int sx = x + PANEL_WIDTH - 20;
			graphics.fill(sx, y + 3, sx + 14, y + 9, on ? accent : 0xFF2A2A35);
			int knob = on ? sx + 8 : sx;
			graphics.fill(knob, y + 2, knob + 6, y + 10, 0xFFFFFFFF);
		}
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		if (super.mouseClicked(event, doubleClick)) return true;
		double mx = event.x();
		double my = event.y();
		for (Modules.Category category : Modules.Category.values()) {
			int[] pos = positions.get(category);
			if (mx < pos[0] || mx >= pos[0] + PANEL_WIDTH) continue;
			if (my >= pos[1] && my < pos[1] + HEADER && event.button() == InputConstants.MOUSE_BUTTON_LEFT) {
				dragging = category;
				dragOffsetX = (int) mx - pos[0];
				dragOffsetY = (int) my - pos[1];
				return true;
			}
			for (Row row : rows(category)) {
				if (my < row.y() || my >= row.y() + row.height()) continue;
				clickRow(row, event.button(), pos[0], mx);
				return true;
			}
		}
		return false;
	}

	private void clickRow(Row row, int button, int panelX, double mx) {
		if (row.bind()) {
			listening = listening == row.module() ? null : row.module();
		} else if (row.setting() == null) {
			if (button == InputConstants.MOUSE_BUTTON_LEFT) row.module().toggle();
			else if (button == InputConstants.MOUSE_BUTTON_RIGHT && !expanded.remove(row.module())) expanded.add(row.module());
		} else if (row.setting() instanceof Modules.BoolSetting bool) {
			bool.toggle();
			QolConfig.save();
		} else if (row.setting() instanceof Modules.NumberSetting number) {
			sliding = number;
			slideX = panelX;
			slide(mx);
		}
	}

	private void slide(double mx) {
		int barX0 = slideX + 9;
		int barX1 = slideX + PANEL_WIDTH - 6;
		double fraction = Math.clamp((mx - barX0) / (barX1 - barX0), 0, 1);
		sliding.setValue(sliding.min() + fraction * (sliding.max() - sliding.min()));
	}

	/** While waiting for a bind: Escape cancels, Backspace/Delete clears it, any other key binds it. */
	@Override
	public boolean keyPressed(KeyEvent event) {
		if (listening == null) return super.keyPressed(event);
		int key = event.key();
		if (key == InputConstants.KEY_BACKSPACE || key == InputConstants.KEY_DELETE) Modules.setBind(listening.name(), Modules.NO_BIND);
		else if (key != InputConstants.KEY_ESCAPE) Modules.setBind(listening.name(), key);
		listening = null;
		return true;
	}

	@Override
	public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
		if (sliding != null) {
			slide(event.x());
			return true;
		}
		if (dragging == null) return super.mouseDragged(event, dx, dy);
		int[] pos = positions.get(dragging);
		pos[0] = Math.clamp((int) event.x() - dragOffsetX, 0, width - PANEL_WIDTH);
		pos[1] = Math.clamp((int) event.y() - dragOffsetY, 0, height - HEADER);
		return true;
	}

	@Override
	public boolean mouseReleased(MouseButtonEvent event) {
		if (sliding != null) QolConfig.save();
		sliding = null;
		dragging = null;
		return super.mouseReleased(event);
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}
}
