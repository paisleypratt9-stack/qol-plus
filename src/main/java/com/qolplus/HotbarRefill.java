package com.qolplus;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

/** When the stack in your hand runs out (or your tool breaks), the next matching stack from your inventory takes its place. */
public final class HotbarRefill {
	private record Held(int slot, ItemStack stack) {
	}

	private static final Map<UUID, Held> lastHeld = new HashMap<>();

	private HotbarRefill() {
	}

	public static void tick(MinecraftServer server) {
		if (!QolConfig.get().hotbarRefill) return;
		for (ServerPlayer player : server.getPlayerList().getPlayers()) {
			Inventory inv = player.getInventory();
			int slot = inv.getSelectedSlot();
			ItemStack now = inv.getSelectedItem();
			Held last = lastHeld.get(player.getUUID());
			// Skip while a chest or other menu is open, so moving items around yourself doesn't trigger it.
			if (last != null && last.slot() == slot && now.isEmpty() && !last.stack().isEmpty()
					&& player.containerMenu == player.inventoryMenu && !player.isCreative()) {
				refill(inv, slot, last.stack());
				now = inv.getSelectedItem();
			}
			lastHeld.put(player.getUUID(), new Held(slot, now.isEmpty() ? ItemStack.EMPTY : now.copyWithCount(1)));
		}
	}

	private static void refill(Inventory inv, int slot, ItemStack used) {
		for (int i = 9; i < Inventory.INVENTORY_SIZE; i++) {
			ItemStack candidate = inv.getItem(i);
			// A broken tool gets replaced by any tool of the same kind, everything else needs an exact match.
			boolean match = used.isDamageableItem() ? candidate.is(used.getItem()) : ItemStack.isSameItemSameComponents(candidate, used);
			if (!match) continue;
			inv.setItem(slot, candidate);
			inv.setItem(i, ItemStack.EMPTY);
			return;
		}
	}

	public static void forget(UUID player) {
		lastHeld.remove(player);
	}
}
