package com.qolplus.client;

import com.qolplus.QolConfig;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.util.Mth;

/** Custom Crosshair: replaces the vanilla one. Styles: 1 cross, 2 dot, 3 cross with dot, 4 square. */
public final class Crosshair {
	private Crosshair() {
	}

	public static void extract(GuiGraphicsExtractor graphics, DeltaTracker delta) {
		Minecraft mc = Minecraft.getInstance();
		if (!mc.options.getCameraType().isFirstPerson() || mc.player == null) return;
		QolConfig c = QolConfig.get();
		int color = c.crosshairColored ? 0xFF000000 | Mth.hsvToRgb(c.crosshairHue / 360f, 0.8f, 1.0f) : 0xFFFFFFFF;
		int cx = graphics.guiWidth() / 2;
		int cy = graphics.guiHeight() / 2;
		int size = c.crosshairSize;
		int gap = c.crosshairGap;
		int t = c.crosshairThickness;
		int half = t / 2;

		switch (c.crosshairStyle) {
			case 2 -> box(graphics, cx - half, cy - half, t, t, color, c.crosshairOutline);
			case 3 -> {
				cross(graphics, cx, cy, size, gap, t, color, c.crosshairOutline);
				box(graphics, cx - half, cy - half, t, t, color, c.crosshairOutline);
			}
			case 4 -> {
				int r = gap + size / 2;
				box(graphics, cx - r, cy - r, 2 * r + t, t, color, c.crosshairOutline);
				box(graphics, cx - r, cy + r, 2 * r + t, t, color, c.crosshairOutline);
				box(graphics, cx - r, cy - r, t, 2 * r + t, color, c.crosshairOutline);
				box(graphics, cx + r, cy - r, t, 2 * r + t, color, c.crosshairOutline);
			}
			default -> cross(graphics, cx, cy, size, gap, t, color, c.crosshairOutline);
		}
	}

	private static void cross(GuiGraphicsExtractor graphics, int cx, int cy, int size, int gap, int t, int color, boolean outline) {
		int half = t / 2;
		box(graphics, cx - gap - size, cy - half, size, t, color, outline);
		box(graphics, cx + gap + 1, cy - half, size, t, color, outline);
		box(graphics, cx - half, cy - gap - size, t, size, color, outline);
		box(graphics, cx - half, cy + gap + 1, t, size, color, outline);
	}

	private static void box(GuiGraphicsExtractor graphics, int x, int y, int w, int h, int color, boolean outline) {
		if (outline) graphics.fill(x - 1, y - 1, x + w + 1, y + h + 1, 0xC0000000);
		graphics.fill(x, y, x + w, y + h, color);
	}
}
