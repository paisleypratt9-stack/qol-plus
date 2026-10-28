package com.qolplus.client;

import java.util.List;

import com.qolplus.QolConfig;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.NonNullList;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.block.ShulkerBoxBlock;

/** Shulker Preview: a 9x3 grid of the box's items in its tooltip. */
public final class ShulkerPreview {
	private static final int SLOT = 18;

	/** The tooltip data: just the 27 slots. */
	public record Contents(List<ItemStack> items) implements TooltipComponent {
	}

	private ShulkerPreview() {
	}

	/** Preview data for a shulker box with something in it, or null. */
	public static Contents of(ItemStack stack) {
		if (!QolConfig.get().shulkerPreview) return null;
		if (!(stack.getItem() instanceof BlockItem item) || !(item.getBlock() instanceof ShulkerBoxBlock)) return null;
		ItemContainerContents container = stack.get(DataComponents.CONTAINER);
		if (container == null || container.nonEmptyItemCopyStream().findAny().isEmpty()) return null;
		NonNullList<ItemStack> items = NonNullList.withSize(27, ItemStack.EMPTY);
		container.copyInto(items);
		return new Contents(items);
	}

	/** Draws the grid. */
	public record Grid(Contents contents) implements ClientTooltipComponent {
		@Override
		public int getHeight(Font font) {
			return 3 * SLOT + 4;
		}

		@Override
		public int getWidth(Font font) {
			return 9 * SLOT;
		}

		@Override
		public void extractImage(Font font, int x, int y, int w, int h, GuiGraphicsExtractor graphics) {
			Minecraft mc = Minecraft.getInstance();
			for (int i = 0; i < contents.items().size(); i++) {
				int sx = x + (i % 9) * SLOT;
				int sy = y + 2 + (i / 9) * SLOT;
				graphics.fill(sx, sy, sx + SLOT - 1, sy + SLOT - 1, 0x40000000);
				ItemStack stack = contents.items().get(i);
				if (stack.isEmpty()) continue;
				graphics.item(stack, sx + 1, sy + 1);
				graphics.itemDecorations(mc.font, stack, sx + 1, sy + 1);
			}
		}
	}
}
