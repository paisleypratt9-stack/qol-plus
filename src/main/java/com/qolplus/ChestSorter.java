package com.qolplus;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.ShulkerBoxMenu;
import net.minecraft.world.item.ItemStack;

/** The Sort button on chests, barrels and shulker boxes: stacks everything up and orders it by item. */
public final class ChestSorter {
	public record SortPayload() implements CustomPacketPayload {
		public static final Type<SortPayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath(QolPlus.MOD_ID, "sort"));
		public static final StreamCodec<RegistryFriendlyByteBuf, SortPayload> CODEC = StreamCodec.unit(new SortPayload());

		@Override
		public Type<SortPayload> type() {
			return TYPE;
		}
	}

	private ChestSorter() {
	}

	public static void register() {
		PayloadTypeRegistry.serverboundPlay().register(SortPayload.TYPE, SortPayload.CODEC);
		ServerPlayNetworking.registerGlobalReceiver(SortPayload.TYPE, (payload, context) -> sort(context.player()));
	}

	private static void sort(ServerPlayer player) {
		AbstractContainerMenu menu = player.containerMenu;
		if (!QolConfig.get().chestSort || !(menu instanceof ChestMenu || menu instanceof ShulkerBoxMenu)) return;
		Container container = menu.getSlot(0).container;

		List<ItemStack> merged = new ArrayList<>();
		for (int i = 0; i < container.getContainerSize(); i++) {
			ItemStack stack = container.getItem(i).copy();
			for (ItemStack existing : merged) {
				if (stack.isEmpty()) break;
				if (!ItemStack.isSameItemSameComponents(existing, stack)) continue;
				int moved = Math.min(stack.getCount(), existing.getMaxStackSize() - existing.getCount());
				existing.grow(moved);
				stack.shrink(moved);
			}
			if (!stack.isEmpty()) merged.add(stack);
		}
		merged.sort(Comparator.comparing((ItemStack s) -> BuiltInRegistries.ITEM.getKey(s.getItem()).toString())
				.thenComparing(s -> -s.getCount()));

		for (int i = 0; i < container.getContainerSize(); i++) container.setItem(i, i < merged.size() ? merged.get(i) : ItemStack.EMPTY);
		container.setChanged();
		menu.broadcastChanges();
	}
}
