package com.qolplus.client;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayDeque;
import java.util.Comparator;
import java.util.List;

import com.qolplus.QolConfig;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.util.Util;

/** Watermark, rainbow module list, and small info boxes (FPS, CPS, ping, clock, direction, coords) plus keystrokes. */
public final class HudWidgets {
	private static final int BOX_BG = 0x90101018;
	private static final DateTimeFormatter CLOCK_12 = DateTimeFormatter.ofPattern("h:mm a");
	private static final DateTimeFormatter CLOCK_24 = DateTimeFormatter.ofPattern("HH:mm");
	private static final ArrayDeque<Long> leftClicks = new ArrayDeque<>();
	private static final ArrayDeque<Long> rightClicks = new ArrayDeque<>();

	private HudWidgets() {
	}

	/** Called from the mouse mixin on every button press while playing. */
	public static void onClick(int button) {
		if (button == 0) leftClicks.add(Util.getMillis());
		else if (button == 1) rightClicks.add(Util.getMillis());
	}

	private static int cps(ArrayDeque<Long> clicks) {
		long cutoff = Util.getMillis() - 1000;
		while (!clicks.isEmpty() && clicks.peekFirst() < cutoff) clicks.pollFirst();
		return clicks.size();
	}

	/** Smooth rainbow that drifts over time; offset shifts the hue so stacked lines form a gradient. */
	static int rainbow(int offset) {
		double degrees = Util.getMillis() / 1000.0 * QolConfig.get().rainbowSpeed * 36 + offset * 6;
		float hue = (float) (degrees % 360 / 360);
		return 0xFF000000 | Mth.hsvToRgb(hue, 0.55f, 1.0f);
	}

	public static void extract(GuiGraphicsExtractor graphics, Minecraft mc, LocalPlayer player) {
		QolConfig c = QolConfig.get();
		Font font = mc.font;
		int y = 4;

		if (c.hudWatermark) {
			String name = "QoL+";
			String version = " v1.0";
			int w = font.width(name) + font.width(version) + 10;
			graphics.fill(4, y, 4 + w, y + 14, BOX_BG);
			graphics.fill(4, y, 4 + w, y + 1, ClickGui.accent());
			int x = 9;
			for (int i = 0; i < name.length(); i++) {
				String ch = String.valueOf(name.charAt(i));
				graphics.text(font, ch, x, y + 4, rainbow(i * 4), true);
				x += font.width(ch);
			}
			graphics.text(font, version, x, y + 4, 0xFF8A8A99, false);
			y += 18;
		}

		if (c.hudFps) y = box(graphics, font, y, "FPS", String.valueOf(mc.getFps()));
		if (c.hudCps) y = box(graphics, font, y, "CPS", cps(leftClicks) + " | " + cps(rightClicks));
		if (c.hudPing) {
			PlayerInfo info = mc.getConnection() == null ? null : mc.getConnection().getPlayerInfo(player.getUUID());
			y = box(graphics, font, y, "Ping", (info == null ? 0 : info.getLatency()) + "ms");
		}
		if (c.hudClock) y = box(graphics, font, y, "Time", LocalTime.now().format(c.clock24h ? CLOCK_24 : CLOCK_12));
		if (c.hudDirection) {
			String dir = player.getDirection().getName();
			y = box(graphics, font, y, "Facing", Character.toUpperCase(dir.charAt(0)) + dir.substring(1));
		}
		if (c.hudCoords) y = box(graphics, font, y, "XYZ", player.getBlockX() + " " + player.getBlockY() + " " + player.getBlockZ());
		if (c.hudDay && mc.level != null) y = box(graphics, font, y, "Day", String.valueOf(mc.level.getOverworldClockTime() / 24000 + 1));
		if (c.hudSessionStats) {
			for (String[] line : SessionStats.lines()) y = box(graphics, font, y, line[0], line[1]);
		}
		if (c.hudKeystrokes) keystrokes(graphics, mc, font, y + 2);
		if (c.hudModuleList) moduleList(graphics, font);
	}

	private static int box(GuiGraphicsExtractor graphics, Font font, int y, String label, String value) {
		int w = font.width(label) + font.width(value) + 14;
		graphics.fill(4, y, 4 + w, y + 13, BOX_BG);
		graphics.fill(4, y, 5, y + 13, ClickGui.accent());
		graphics.text(font, label, 9, y + 3, ClickGui.accent(), false);
		graphics.text(font, value, 9 + font.width(label) + 4, y + 3, 0xFFFFFFFF, false);
		return y + 15;
	}

	private static void keystrokes(GuiGraphicsExtractor graphics, Minecraft mc, Font font, int y) {
		int size = 20;
		int gap = 2;
		int x = 4;
		key(graphics, font, mc.options.keyUp, x + size + gap, y, size, size);
		y += size + gap;
		key(graphics, font, mc.options.keyLeft, x, y, size, size);
		key(graphics, font, mc.options.keyDown, x + size + gap, y, size, size);
		key(graphics, font, mc.options.keyRight, x + 2 * (size + gap), y, size, size);
		y += size + gap;
		if (QolConfig.get().keystrokesMouse) {
			int half = (3 * size + 2 * gap - gap) / 2;
			mouseKey(graphics, font, shortName(mc.options.keyAttack), cps(leftClicks), mc.options.keyAttack.isDown(), x, y, half);
			mouseKey(graphics, font, shortName(mc.options.keyUse), cps(rightClicks), mc.options.keyUse.isDown(), x + half + gap, y, half);
			y += size + gap;
		}
		if (QolConfig.get().keystrokesSpace) key(graphics, font, mc.options.keyJump, x, y, 3 * size + 2 * gap, 12);
	}

	private static void key(GuiGraphicsExtractor graphics, Font font, KeyMapping key, int x, int y, int w, int h) {
		boolean down = key.isDown();
		graphics.fill(x, y, x + w, y + h, down ? 0xD0FFFFFF : BOX_BG);
		String label = shortName(key);
		graphics.centeredText(font, label, x + w / 2, y + (h - 8) / 2, down ? 0xFF111118 : 0xFFFFFFFF);
	}

	/** The key each action is actually bound to, kept short so it fits its box (so a swapped jump/attack shows right). */
	private static String shortName(KeyMapping key) {
		return switch (key.saveString()) {
			case "key.mouse.left" -> "LMB";
			case "key.mouse.right" -> "RMB";
			case "key.mouse.middle" -> "MMB";
			case "key.keyboard.space" -> "SPACE";
			default -> {
				String name = key.getTranslatedKeyMessage().getString().toUpperCase();
				yield name.length() > 5 ? name.substring(0, 5) : name;
			}
		};
	}

	private static void mouseKey(GuiGraphicsExtractor graphics, Font font, String label, int cps, boolean down, int x, int y, int w) {
		graphics.fill(x, y, x + w, y + 20, down ? 0xD0FFFFFF : BOX_BG);
		int color = down ? 0xFF111118 : 0xFFFFFFFF;
		graphics.centeredText(font, label, x + w / 2, y + 3, color);
		graphics.centeredText(font, cps + " CPS", x + w / 2, y + 11, down ? 0xFF333344 : 0xFF8A8A99);
	}

	/** Enabled modules down the right edge, longest first, with a rainbow fade and an accent bar. */
	private static void moduleList(GuiGraphicsExtractor graphics, Font font) {
		List<Modules.Module> enabled = Modules.ALL.stream()
				.filter(m -> m.category() != Modules.Category.HUD && m.enabled())
				.sorted(Comparator.comparingInt((Modules.Module m) -> -font.width(m.name())))
				.toList();
		int right = graphics.guiWidth() - 2;
		int y = 2;
		for (int i = 0; i < enabled.size(); i++) {
			String name = enabled.get(i).name();
			int w = font.width(name);
			int color = QolConfig.get().moduleListRainbow ? rainbow(i * 3) : ClickGui.accent();
			if (QolConfig.get().moduleListBackground) graphics.fill(right - w - 6, y, right, y + 11, BOX_BG);
			graphics.fill(right, y, right + 2, y + 11, color);
			graphics.text(font, name, right - w - 3, y + 2, color, true);
			y += 11;
		}
	}
}
