package com.qolplus.client;

import java.util.Locale;

import com.mojang.blaze3d.platform.InputConstants;
import com.qolplus.ChestSorter;
import com.qolplus.QolConfig;
import com.qolplus.client.mixin.ContainerScreenAccessor;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenKeyboardEvents;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.DispenserMenu;
import net.minecraft.world.inventory.HopperMenu;
import net.minecraft.world.inventory.ShulkerBoxMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/** Search bar on top of chests (and other containers) that lights up the slots holding what you type, plus a Sort button. */
public final class ChestFeatures {
	private ChestFeatures() {
	}

	public static void onScreenOpened(Minecraft mc, Screen screen, int width, int height) {
		if (!(screen instanceof AbstractContainerScreen<?> containerScreen)) return;
		// Only storage: chests, barrels, ender chests, shulker boxes, hoppers, dispensers and droppers.
		if (!isStorage(containerScreen.getMenu())) return;

		ContainerScreenAccessor pos = (ContainerScreenAccessor) containerScreen;
		int barY = Math.max(2, pos.qolplus$getTopPos() - 16);
		int barWidth = pos.qolplus$getImageWidth() - 2;
		boolean sortable = containerScreen.getMenu() instanceof ChestMenu || containerScreen.getMenu() instanceof ShulkerBoxMenu;
		if (QolConfig.get().chestSort && sortable && ClientPlayNetworking.canSend(ChestSorter.SortPayload.TYPE)) {
			barWidth -= 34;
			Screens.getWidgets(screen).add(Button.builder(Component.translatable("button.qolplus.sort"),
					b -> ClientPlayNetworking.send(new ChestSorter.SortPayload()))
					.bounds(pos.qolplus$getLeftPos() + barWidth + 3, barY, 32, 14).build());
		}
		if (!QolConfig.get().chestSearch) return;

		EditBox search = new EditBox(Screens.getFont(screen), pos.qolplus$getLeftPos() + 1, barY,
				barWidth, 14, Component.translatable("screen.qolplus.search"));
		search.setMaxLength(64);
		search.setHint(Component.translatable("screen.qolplus.search"));
		Screens.getWidgets(screen).add(search);

		// While typing, keys like E (close) or 1-9 (hotbar swap) should go into the box instead of the chest.
		ScreenKeyboardEvents.allowKeyPress(screen).register((s, event) -> {
			if (!search.isFocused() || event.key() == InputConstants.KEY_ESCAPE) return true;
			search.keyPressed(event);
			return false;
		});

		ScreenEvents.afterExtract(screen).register((s, graphics, mouseX, mouseY, tickDelta) -> {
			String query = search.getValue().trim().toLowerCase(Locale.ROOT);
			if (query.isEmpty()) return;
			String idQuery = query.replace(' ', '_');
			int left = pos.qolplus$getLeftPos();
			int top = pos.qolplus$getTopPos();
			for (Slot slot : containerScreen.getMenu().slots) {
				if (slot.container instanceof Inventory) continue;
				int x = left + slot.x;
				int y = top + slot.y;
				if (matches(slot.getItem(), query, idQuery)) {
					graphics.fill(x - 1, y - 1, x + 17, y + 17, 0x6055FFFF);
				} else {
					graphics.fill(x, y, x + 16, y + 16, 0xB0000000);
				}
			}
		});
	}

	private static boolean isStorage(AbstractContainerMenu menu) {
		return menu instanceof ChestMenu || menu instanceof ShulkerBoxMenu || menu instanceof HopperMenu || menu instanceof DispenserMenu;
	}

	private static boolean matches(ItemStack stack, String query, String idQuery) {
		if (stack.isEmpty()) return false;
		String id = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
		String name = stack.getHoverName().getString().toLowerCase(Locale.ROOT);
		return id.contains(idQuery) || name.contains(query);
	}
}
