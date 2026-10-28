package com.qolplus.client;

import java.util.List;

import com.qolplus.QolConfig;

import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

/** Hunger and saturation on food, and durability numbers on tools and armor, in the item tooltip. */
public final class ItemTooltips {
	private ItemTooltips() {
	}

	public static void append(ItemStack stack, Item.TooltipContext context, TooltipFlag flag, List<Component> lines) {
		if (!QolConfig.get().itemTooltips) return;
		FoodProperties food = stack.get(DataComponents.FOOD);
		if (food != null) {
			lines.add(Component.translatable("tooltip.qolplus.food", food.nutrition(), String.format("%.1f", food.saturation()))
					.withStyle(ChatFormatting.GOLD));
		}
		// Vanilla only shows durability with advanced tooltips (F3+H) on.
		if (stack.isDamageableItem() && !flag.isAdvanced()) {
			lines.add(Component.translatable("tooltip.qolplus.durability", stack.getMaxDamage() - stack.getDamageValue(), stack.getMaxDamage())
					.withStyle(QolHud.isLow(stack) ? ChatFormatting.RED : ChatFormatting.GRAY));
		}
	}
}
