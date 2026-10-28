package com.qolplus;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** Breaking a log with an axe (not sneaking) fells the whole tree and puts the logs straight into your inventory. */
public final class TreeFeller {
	private static boolean felling;

	private TreeFeller() {
	}

	public static void afterBreak(Level level, Player player, BlockPos pos, BlockState state, BlockEntity blockEntity) {
		if (!(level instanceof ServerLevel server) || !state.is(BlockTags.LOGS)) return;
		LeafDecay.watch(server, pos);

		ItemStack tool = player.getMainHandItem();
		QolConfig config = QolConfig.get();
		if (!config.treeFelling || felling || player.isShiftKeyDown() || !player.mayBuild() || !tool.is(ItemTags.AXES)) return;

		List<BlockPos> logs = findTree(server, pos, state.getBlock());
		if (logs.isEmpty()) return;

		felling = true;
		try {
			BlockPos top = pos;
			for (BlockPos log : logs) {
				if (tool.isEmpty()) break; // axe broke
				BlockState logState = server.getBlockState(log);
				BlockEntity logEntity = server.getBlockEntity(log);
				// Let claim/protection mods veto each extra log like a normal break.
				if (!PlayerBlockBreakEvents.BEFORE.invoker().beforeBlockBreak(server, player, log, logState, logEntity)) continue;
				if (!player.isCreative()) {
					for (ItemStack drop : Block.getDrops(logState, server, log, logEntity, player, tool)) {
						if (config.treeFellingAutoPickup) giveOrDrop(player, server, drop);
						else Block.popResource(server, log, drop);
					}
				}
				server.destroyBlock(log, false, player, 512);
				tool.hurtAndBreak(1, player, EquipmentSlot.MAINHAND);
				player.causeFoodExhaustion(0.005f);
				if (log.getY() > top.getY()) top = log;
			}
			LeafDecay.watch(server, top);
		} finally {
			felling = false;
		}
	}

	/** Connected logs of the same type at or above the cut. Empty unless natural leaves touch them, so log houses are safe. */
	private static List<BlockPos> findTree(ServerLevel level, BlockPos origin, Block logBlock) {
		List<BlockPos> found = new ArrayList<>();
		Set<BlockPos> seen = new HashSet<>();
		ArrayDeque<BlockPos> queue = new ArrayDeque<>();
		queue.add(origin);
		seen.add(origin);
		boolean hasLeaves = false;

		while (!queue.isEmpty() && found.size() < QolConfig.get().treeMaxLogs) {
			BlockPos current = queue.poll();
			for (int dx = -1; dx <= 1; dx++) {
				for (int dy = -1; dy <= 1; dy++) {
					for (int dz = -1; dz <= 1; dz++) {
						BlockPos next = current.offset(dx, dy, dz);
						if (next.getY() < origin.getY() || !seen.add(next)) continue;
						BlockState state = level.getBlockState(next);
						if (state.is(logBlock)) {
							found.add(next);
							queue.add(next);
						} else if (state.getBlock() instanceof LeavesBlock && !state.getValue(LeavesBlock.PERSISTENT)) {
							hasLeaves = true;
						}
					}
				}
			}
		}
		return hasLeaves ? found : List.of();
	}

	private static void giveOrDrop(Player player, ServerLevel level, ItemStack drop) {
		player.getInventory().add(drop);
		if (!drop.isEmpty()) Block.popResource(level, player.blockPosition(), drop);
	}
}
