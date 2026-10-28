package com.qolplus.client;

import java.util.EnumMap;
import java.util.Map;

import com.qolplus.QolConfig;

import net.minecraft.ChatFormatting;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** Armor durability HUD, firework counter and low-durability warnings. */
public final class QolHud {
	private static final EquipmentSlot[] ARMOR = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
	private static final EquipmentSlot[] WATCHED = {EquipmentSlot.MAINHAND, EquipmentSlot.OFFHAND,
			EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
	private static final Map<EquipmentSlot, Item> WARNED = new EnumMap<>(EquipmentSlot.class);
	private static int elytraNagCooldown;

	private QolHud() {
	}

	static boolean isLow(ItemStack stack) {
		return stack.isDamageableItem() && remaining(stack) <= Math.max(1, stack.getMaxDamage() * QolConfig.get().warningPercent / 100);
	}

	private static int remaining(ItemStack stack) {
		return stack.getMaxDamage() - stack.getDamageValue();
	}

	public static void tickWarnings(Minecraft mc) {
		LocalPlayer player = mc.player;
		if (player == null || !QolConfig.get().durabilityWarnings) return;
		for (EquipmentSlot slot : WATCHED) {
			ItemStack stack = player.getItemBySlot(slot);
			if (!isLow(stack)) {
				WARNED.remove(slot);
				continue;
			}
			if (WARNED.get(slot) == stack.getItem()) continue;
			WARNED.put(slot, stack.getItem());
			warn(player, stack);
		}

		// Keep nagging while flying on a nearly broken elytra.
		ItemStack chest = player.getItemBySlot(EquipmentSlot.CHEST);
		if (elytraNagCooldown > 0) elytraNagCooldown--;
		if (player.isFallFlying() && chest.is(Items.ELYTRA) && isLow(chest) && elytraNagCooldown == 0) {
			elytraNagCooldown = 60;
			warn(player, chest);
		}
	}

	private static void warn(LocalPlayer player, ItemStack stack) {
		player.playSound(SoundEvents.NOTE_BLOCK_PLING.value(), 1.0f, 0.6f);
		player.sendOverlayMessage(Component.translatable("message.qolplus.low_durability", stack.getHoverName(), remaining(stack))
				.withStyle(ChatFormatting.RED, ChatFormatting.BOLD));
	}

	public static void extract(GuiGraphicsExtractor graphics, DeltaTracker delta) {
		Minecraft mc = Minecraft.getInstance();
		LocalPlayer player = mc.player;
		if (player == null || mc.level == null) return;
		QolConfig config = QolConfig.get();
		drawEquipment(graphics, mc, player, config);
		DeathMarker.extract(graphics, mc, player);
		HudWidgets.extract(graphics, mc, player);
	}

	/** Armor pieces with durability numbers, then a firework count when you have an elytra, to the right of the hotbar. */
	private static void drawEquipment(GuiGraphicsExtractor graphics, Minecraft mc, LocalPlayer player, QolConfig config) {
		Font font = mc.font;
		int x = graphics.guiWidth() / 2 + 98;
		int y = graphics.guiHeight() - 19;
		boolean flashOn = (mc.level.getGameTime() / 8) % 2 == 0;

		for (EquipmentSlot slot : config.armorHud ? ARMOR : new EquipmentSlot[0]) {
			ItemStack stack = player.getItemBySlot(slot);
			if (stack.isEmpty()) continue;
			if (isLow(stack) && flashOn) graphics.fill(x - 1, y - 1, x + 17, y + 17, 0x80FF0000);
			graphics.item(stack, x, y);
			graphics.itemDecorations(font, stack, x, y);
			if (stack.isDamageableItem()) {
				graphics.centeredText(font, String.valueOf(remaining(stack)), x + 8, y - 9, durabilityColor(stack));
			}
			x += 20;
		}

		ItemStack chest = player.getItemBySlot(EquipmentSlot.CHEST);
		if (config.fireworkHud && (chest.is(Items.ELYTRA) || player.isFallFlying())) {
			int rockets = 0;
			for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
				ItemStack stack = player.getInventory().getItem(i);
				if (stack.is(Items.FIREWORK_ROCKET)) rockets += stack.getCount();
			}
			x += 4;
			if (rockets == 0 && flashOn) graphics.fill(x - 1, y - 1, x + 17, y + 17, 0x80FF0000);
			graphics.item(new ItemStack(Items.FIREWORK_ROCKET), x, y);
			graphics.centeredText(font, String.valueOf(rockets), x + 8, y - 9, rockets == 0 ? 0xFFFF5555 : 0xFFFFFFFF);
		}
	}

	private static int durabilityColor(ItemStack stack) {
		float fraction = (float) remaining(stack) / stack.getMaxDamage();
		if (fraction <= 0.1f) return 0xFFFF5555;
		if (fraction <= 0.3f) return 0xFFFFAA00;
		return 0xFF55FF55;
	}
}
