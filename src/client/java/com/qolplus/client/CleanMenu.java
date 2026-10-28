package com.qolplus.client;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import com.mojang.blaze3d.platform.InputConstants;
import com.qolplus.QolConfig;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

/**
 * The default Right Shift menu: one centered window with categories down the left and module cards on the right.
 * Each card has an on/off switch; click the card to open its settings and keybind.
 */
public class CleanMenu extends Screen {
	private static final int WINDOW_BG = 0xF2120E18;
	private static final int SIDEBAR_BG = 0xFF0D0A12;
	private static final int BORDER = 0xFF2A2140;
	private static final int CARD = 0xFF1A1424;
	private static final int CARD_HOVER = 0xFF221A30;
	private static final int TRACK = 0xFF2E2640;
	private static final int TEXT = 0xFFFFFFFF;
	private static final int TEXT_MUTED = 0xFF8C84A0;
	private static final int TEXT_FAINT = 0xFF5E5772;

	private static final int SIDEBAR = 104;
	private static final int CARD_HEAD = 30;
	private static final int SETTING_ROW = 18;
	private static final int CARD_GAP = 4;

	/** Remembered while the game is open. */
	private static Modules.Category selected = Modules.Category.WORLD;
	private static Modules.Module open;
	private static double scroll;

	/** A clickable line in the module list: a card header, a setting inside an open card, or its keybind. */
	private record Item(int y, int height, Modules.Module module, Modules.Setting setting, boolean bind) {
	}

	private int left;
	private int top;
	private int right;
	private int bottom;
	private EditBox search;
	private Modules.NumberSetting sliding;
	private int sliderX0;
	private int sliderX1;
	private Modules.Module listening;

	public CleanMenu() {
		super(Component.literal("QoL+"));
	}

	@Override
	protected void init() {
		int w = Math.min(440, width - 24);
		int h = Math.min(270, height - 24);
		left = (width - w) / 2;
		top = (height - h) / 2;
		right = left + w;
		bottom = top + h;
		search = new EditBox(font, right - 112, top + 10, 102, 14, Component.literal("Search"));
		search.setHint(Component.literal("Search modules..."));
		addRenderableWidget(search);
	}

	private int listTop() {
		return top + 40;
	}

	private int contentLeft() {
		return left + SIDEBAR + 10;
	}

	private int contentRight() {
		return right - 10;
	}

	private List<Modules.Module> shown() {
		String query = search.getValue().trim().toLowerCase(Locale.ROOT);
		return Modules.ALL.stream()
				.filter(m -> query.isEmpty() ? m.category() == selected : m.name().toLowerCase(Locale.ROOT).contains(query))
				.toList();
	}

	private List<Item> layout() {
		List<Item> items = new ArrayList<>();
		int y = listTop() - (int) scroll;
		for (Modules.Module module : shown()) {
			items.add(new Item(y, CARD_HEAD, module, null, false));
			y += CARD_HEAD;
			if (module == open) {
				for (Modules.Setting setting : module.settings()) {
					items.add(new Item(y, SETTING_ROW, module, setting, false));
					y += SETTING_ROW;
				}
				items.add(new Item(y, SETTING_ROW, module, null, true));
				y += SETTING_ROW + 4;
			}
			y += CARD_GAP;
		}
		return items;
	}

	private int contentHeight() {
		List<Item> items = layout();
		return items.isEmpty() ? 0 : items.getLast().y() + items.getLast().height() + (int) scroll - listTop() + 8;
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
		super.extractRenderState(graphics, mouseX, mouseY, partialTick);
		int accent = ClickGui.accent();

		// Window and sidebar.
		graphics.fill(left - 1, top - 1, right + 1, bottom + 1, BORDER);
		graphics.fill(left, top, right, bottom, WINDOW_BG);
		graphics.fill(left, top, left + SIDEBAR, bottom, SIDEBAR_BG);
		graphics.fill(left + SIDEBAR, top, left + SIDEBAR + 1, bottom, BORDER);

		graphics.text(font, "QoL+", left + 12, top + 12, accent, false);
		graphics.text(font, "client", left + 14 + font.width("QoL+"), top + 12, TEXT_FAINT, false);
		graphics.text(font, "MODULES", left + 12, top + 34, TEXT_FAINT, false);

		int y = top + 46;
		for (Modules.Category category : Modules.Category.values()) {
			boolean active = category == selected && search.getValue().isBlank();
			boolean over = mouseX >= left && mouseX < left + SIDEBAR && mouseY >= y && mouseY < y + 18;
			if (active) {
				graphics.fill(left + 6, y, left + SIDEBAR - 6, y + 18, ClickGui.withAlpha(accent, 0x30));
				graphics.fill(left + 6, y, left + 8, y + 18, accent);
			} else if (over) {
				graphics.fill(left + 6, y, left + SIDEBAR - 6, y + 18, 0x14FFFFFF);
			}
			graphics.text(font, category.title, left + 14, y + 5, active ? TEXT : TEXT_MUTED, false);
			String count = String.valueOf(Modules.ALL.stream().filter(m -> m.category() == category && m.enabled()).count());
			graphics.text(font, count, left + SIDEBAR - 12 - font.width(count), y + 5, active ? accent : TEXT_FAINT, false);
			y += 20;
		}
		y += 6;
		graphics.text(font, "CLIENT", left + 12, y, TEXT_FAINT, false);
		y += 12;
		boolean overGallery = mouseX >= left && mouseX < left + SIDEBAR && mouseY >= y && mouseY < y + 18;
		if (overGallery) graphics.fill(left + 6, y, left + SIDEBAR - 6, y + 18, 0x14FFFFFF);
		graphics.text(font, "Screenshots", left + 14, y + 5, overGallery ? TEXT : TEXT_MUTED, false);
		String hint = "Right Shift to close";
		graphics.text(font, hint, left + 12, bottom - 14, TEXT_FAINT, false);

		// Header.
		List<Modules.Module> shown = shown();
		String heading = search.getValue().isBlank() ? selected.title + " Modules" : "Search";
		graphics.text(font, heading, contentLeft(), top + 11, TEXT, false);
		long enabled = shown.stream().filter(Modules.Module::enabled).count();
		graphics.text(font, shown.size() + " modules  ·  " + enabled + " enabled", contentLeft(), top + 23, TEXT_MUTED, false);

		// Module cards, clipped to the list area.
		graphics.enableScissor(contentLeft() - 2, listTop() - 2, contentRight() + 2, bottom - 6);
		Modules.Module hoveredCard = null;
		int cardTop = 0;
		for (Item item : layout()) {
			boolean over = mouseX >= contentLeft() && mouseX < contentRight() && mouseY >= item.y() && mouseY < item.y() + item.height()
					&& mouseY >= listTop() && mouseY < bottom - 6;
			if (item.setting() == null && !item.bind()) {
				cardTop = item.y();
				int cardBottom = item.y() + CARD_HEAD + (item.module() == open ? (item.module().settings().size() + 1) * SETTING_ROW + 4 : 0);
				if (over) hoveredCard = item.module();
				graphics.fill(contentLeft(), cardTop, contentRight(), cardBottom, over || item.module() == open ? CARD_HOVER : CARD);
				if (item.module().enabled()) graphics.fill(contentLeft(), cardTop, contentLeft() + 2, cardBottom, accent);
				drawCardHeader(graphics, item.module(), item.y(), accent);
			} else {
				drawSetting(graphics, item, accent, over);
			}
		}
		graphics.disableScissor();

		if (hoveredCard != null && listening == null) {
			// Small hint so it's obvious cards open.
			String text = hoveredCard == open ? "click to close" : "click for settings";
			graphics.text(font, text, contentRight() - 34 - font.width(text), top + 23, TEXT_FAINT, false);
		}
	}

	private void drawCardHeader(GuiGraphicsExtractor graphics, Modules.Module module, int y, int accent) {
		int x = contentLeft() + 9;
		graphics.text(font, module.name(), x, y + 6, TEXT, false);
		int key = Modules.bind(module.name());
		if (key != Modules.NO_BIND) {
			String keyName = keyName(key);
			int kx = x + font.width(module.name()) + 6;
			graphics.fill(kx - 2, y + 4, kx + font.width(keyName) + 2, y + 15, TRACK);
			graphics.text(font, keyName, kx, y + 6, TEXT_MUTED, false);
		}
		String desc = font.plainSubstrByWidth(module.description(), contentRight() - x - 40);
		graphics.text(font, desc, x, y + 18, TEXT_MUTED, false);
		drawSwitch(graphics, contentRight() - 28, y + 10, module.enabled(), accent);
	}

	private void drawSwitch(GuiGraphicsExtractor graphics, int x, int y, boolean on, int accent) {
		graphics.fill(x, y, x + 20, y + 10, on ? accent : TRACK);
		int knob = on ? x + 11 : x + 1;
		graphics.fill(knob, y + 1, knob + 8, y + 9, on ? 0xFFFFFFFF : 0xFF9A93AE);
	}

	private void drawSetting(GuiGraphicsExtractor graphics, Item item, int accent, boolean over) {
		int x = contentLeft() + 14;
		int y = item.y();
		int labelColor = over ? TEXT : TEXT_MUTED;
		if (item.bind()) {
			int key = Modules.bind(item.module().name());
			String value = listening == item.module() ? "Press a key..." : key == Modules.NO_BIND ? "None" : keyName(key);
			graphics.text(font, "Keybind", x, y + 5, labelColor, false);
			int w = font.width(value) + 10;
			int bx = contentRight() - 10 - w;
			graphics.fill(bx, y + 2, bx + w, y + 15, listening == item.module() ? ClickGui.withAlpha(accent, 0x60) : TRACK);
			graphics.text(font, value, bx + 5, y + 5, TEXT, false);
		} else if (item.setting() instanceof Modules.NumberSetting number) {
			graphics.text(font, number.name(), x, y + 5, labelColor, false);
			String value = number.display();
			graphics.text(font, value, contentRight() - 10 - font.width(value), y + 5, TEXT, false);
			int x0 = sliderStart();
			int x1 = contentRight() - 44;
			double fraction = (number.value() - number.min()) / (number.max() - number.min());
			int fill = x0 + (int) Math.round((x1 - x0) * fraction);
			graphics.fill(x0, y + 8, x1, y + 10, TRACK);
			graphics.fill(x0, y + 8, fill, y + 10, accent);
			graphics.fill(fill - 3, y + 6, fill + 3, y + 12, 0xFFFFFFFF);
		} else if (item.setting() instanceof Modules.BoolSetting bool) {
			graphics.text(font, bool.name(), x, y + 5, labelColor, false);
			drawSwitch(graphics, contentRight() - 28, y + 4, bool.get().getAsBoolean(), accent);
		}
	}

	private int sliderStart() {
		return contentLeft() + 100;
	}

	private static String keyName(int key) {
		return InputConstants.Type.KEYBOARD.getOrCreate(key).getDisplayName().getString();
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		if (super.mouseClicked(event, doubleClick)) return true;
		double mx = event.x();
		double my = event.y();

		if (mx >= left && mx < left + SIDEBAR) {
			int y = top + 46;
			for (Modules.Category category : Modules.Category.values()) {
				if (my >= y && my < y + 18) {
					selected = category;
					scroll = 0;
					search.setValue("");
					return true;
				}
				y += 20;
			}
			if (my >= y + 18 && my < y + 36) {
				minecraft.setScreenAndShow(new ScreenshotGallery(this));
				return true;
			}
			return false;
		}

		if (mx < contentLeft() || mx >= contentRight() || my < listTop() || my >= bottom - 6) return false;
		for (Item item : layout()) {
			if (my < item.y() || my >= item.y() + item.height()) continue;
			click(item, event.button(), mx);
			return true;
		}
		return false;
	}

	private void click(Item item, int button, double mx) {
		Modules.Module module = item.module();
		if (item.bind()) {
			listening = listening == module ? null : module;
		} else if (item.setting() == null) {
			// The switch toggles; anywhere else on the card (or right-click) opens the settings.
			if (button == InputConstants.MOUSE_BUTTON_LEFT && mx >= contentRight() - 34) {
				module.toggle();
			} else {
				open = open == module ? null : module;
				listening = null;
			}
		} else if (item.setting() instanceof Modules.BoolSetting bool) {
			bool.toggle();
			QolConfig.save();
		} else if (item.setting() instanceof Modules.NumberSetting number) {
			sliding = number;
			sliderX0 = sliderStart();
			sliderX1 = contentRight() - 44;
			slide(mx);
		}
	}

	private void slide(double mx) {
		double fraction = Math.clamp((mx - sliderX0) / (sliderX1 - sliderX0), 0, 1);
		sliding.setValue(sliding.min() + fraction * (sliding.max() - sliding.min()));
	}

	@Override
	public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
		if (sliding == null) return super.mouseDragged(event, dx, dy);
		slide(event.x());
		return true;
	}

	@Override
	public boolean mouseReleased(MouseButtonEvent event) {
		if (sliding != null) QolConfig.save();
		sliding = null;
		return super.mouseReleased(event);
	}

	@Override
	public boolean mouseScrolled(double x, double y, double scrollX, double scrollY) {
		int max = Math.max(0, contentHeight() - (bottom - 6 - listTop()));
		scroll = Math.clamp(scroll - scrollY * 16, 0, max);
		return true;
	}

	/** While waiting for a bind: Escape cancels, Backspace/Delete clears it, any other key binds it. */
	@Override
	public boolean keyPressed(KeyEvent event) {
		if (listening != null) {
			int key = event.key();
			if (key == InputConstants.KEY_BACKSPACE || key == InputConstants.KEY_DELETE) Modules.setBind(listening.name(), Modules.NO_BIND);
			else if (key != InputConstants.KEY_ESCAPE) Modules.setBind(listening.name(), key);
			listening = null;
			return true;
		}
		if (event.key() == InputConstants.KEY_RSHIFT && !search.isFocused()) {
			onClose();
			return true;
		}
		boolean handled = super.keyPressed(event);
		if (search.isFocused()) scroll = 0;
		return handled;
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}
}
