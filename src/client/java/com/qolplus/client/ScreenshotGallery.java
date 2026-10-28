package com.qolplus.client;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

import com.mojang.blaze3d.Blaze3D;
import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.platform.NativeImage;
import com.qolplus.QolPlus;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

/** Screenshot Gallery: flip through your screenshots in-game (arrow keys or the buttons), newest first. */
public class ScreenshotGallery extends Screen {
	private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(QolPlus.MOD_ID, "gallery");
	private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("MMM d, yyyy  h:mm a").withZone(ZoneId.systemDefault());

	private final Screen parent;
	private final List<Path> files = new ArrayList<>();
	private int index;
	private int imageWidth;
	private int imageHeight;
	private boolean loaded;

	public ScreenshotGallery(Screen parent) {
		super(Component.literal("Screenshots"));
		this.parent = parent;
	}

	private Path folder() {
		return minecraft.gameDirectory.toPath().resolve("screenshots");
	}

	@Override
	protected void init() {
		files.clear();
		if (Files.isDirectory(folder())) {
			try (Stream<Path> list = Files.list(folder())) {
				list.filter(p -> p.toString().toLowerCase().endsWith(".png"))
						.sorted(Comparator.comparing(ScreenshotGallery::modified).reversed())
						.forEach(files::add);
			} catch (IOException e) {
				QolPlusClient.LOGGER.warn("Could not list screenshots", e);
			}
		}
		index = Math.clamp(index, 0, Math.max(0, files.size() - 1));
		int y = height - 28;
		addRenderableWidget(Button.builder(Component.literal("<"), b -> show(index - 1)).bounds(width / 2 - 130, y, 30, 20).build());
		addRenderableWidget(Button.builder(Component.literal("Open Folder"), b -> Blaze3D.openPath(folder())).bounds(width / 2 - 95, y, 90, 20).build());
		addRenderableWidget(Button.builder(Component.literal("Done"), b -> onClose()).bounds(width / 2 + 5, y, 90, 20).build());
		addRenderableWidget(Button.builder(Component.literal(">"), b -> show(index + 1)).bounds(width / 2 + 100, y, 30, 20).build());
		show(index);
	}

	private static long modified(Path path) {
		try {
			return Files.getLastModifiedTime(path).toMillis();
		} catch (IOException e) {
			return 0;
		}
	}

	private void show(int newIndex) {
		if (files.isEmpty()) return;
		index = Math.floorMod(newIndex, files.size());
		try (InputStream in = Files.newInputStream(files.get(index))) {
			NativeImage image = NativeImage.read(in);
			imageWidth = image.getWidth();
			imageHeight = image.getHeight();
			minecraft.getTextureManager().register(TEXTURE, new DynamicTexture(() -> "qolplus gallery", image));
			loaded = true;
		} catch (IOException e) {
			loaded = false;
			QolPlusClient.LOGGER.warn("Could not load screenshot {}", files.get(index), e);
		}
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
		super.extractRenderState(graphics, mouseX, mouseY, partialTick);
		int accent = ClickGui.accent();
		if (files.isEmpty()) {
			graphics.centeredText(font, "No screenshots yet. Press F2 in-game to take one!", width / 2, height / 2, 0xFFB4B4C0);
			return;
		}
		graphics.centeredText(font, (index + 1) + " / " + files.size() + "   " + files.get(index).getFileName(), width / 2, 10, 0xFFFFFFFF);
		graphics.centeredText(font, DATE.format(Instant.ofEpochMilli(modified(files.get(index)))), width / 2, 22, 0xFF8C84A0);
		if (!loaded) return;

		// Fit the image between the title and the buttons, keeping its shape.
		int maxW = width - 40;
		int maxH = height - 36 - 40;
		float scale = Math.min((float) maxW / imageWidth, (float) maxH / imageHeight);
		int w = (int) (imageWidth * scale);
		int h = (int) (imageHeight * scale);
		int x = (width - w) / 2;
		int y = 36 + (maxH - h) / 2;
		graphics.fill(x - 1, y - 1, x + w + 1, y + h + 1, accent);
		graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, x, y, 0, 0, w, h, imageWidth, imageHeight, imageWidth, imageHeight);
	}

	@Override
	public boolean keyPressed(KeyEvent event) {
		if (event.key() == InputConstants.KEY_LEFT) {
			show(index - 1);
			return true;
		}
		if (event.key() == InputConstants.KEY_RIGHT) {
			show(index + 1);
			return true;
		}
		return super.keyPressed(event);
	}

	@Override
	public void removed() {
		minecraft.getTextureManager().release(TEXTURE);
	}

	@Override
	public void onClose() {
		minecraft.setScreenAndShow(parent);
	}
}
