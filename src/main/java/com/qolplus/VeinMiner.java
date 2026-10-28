package com.qolplus;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** Sneak + break an ore with a pickaxe to mine the whole vein. Drops (and XP) land where you broke the first ore. */
public final class VeinMiner {
	private static boolean mining;

	private VeinMiner() {
	}

	public static void afterBreak(Level level, Player player, BlockPos pos, BlockState state, BlockEntity blockEntity) {
		if (!(level instanceof ServerLevel server) || !isOre(state)) return;
		ItemStack tool = player.getMainHandItem();
		if (!QolConfig.get().veinMining || mining || !player.isShiftKeyDown() || !player.mayBuild() || !tool.is(ItemTags.PICKAXES)) return;

		mining = true;
		try {
			for (BlockPos ore : findVein(server, pos, oreFamily(state))) {
				if (tool.isEmpty()) break; // pickaxe broke
				BlockState oreState = server.getBlockState(ore);
				BlockEntity oreEntity = server.getBlockEntity(ore);
				if (!player.hasCorrectToolForDrops(oreState)) continue;
				// Let claim/protection mods veto each extra ore like a normal break.
				if (!PlayerBlockBreakEvents.BEFORE.invoker().beforeBlockBreak(server, player, ore, oreState, oreEntity)) continue;
				if (!player.isCreative()) {
					for (ItemStack drop : Block.getDrops(oreState, server, ore, oreEntity, player, tool)) Block.popResource(server, pos, drop);
					oreState.spawnAfterBreak(server, pos, tool, true);
				}
				server.destroyBlock(ore, false, player, 512);
				tool.hurtAndBreak(1, player, EquipmentSlot.MAINHAND);
				player.causeFoodExhaustion(0.005f);
			}
		} finally {
			mining = false;
		}
	}

	private static boolean isOre(BlockState state) {
		return state.is(BlockTags.ORES) || state.is(Blocks.ANCIENT_DEBRIS);
	}

	/** Deepslate and normal versions of an ore count as the same vein. */
	private static String oreFamily(BlockState state) {
		return BuiltInRegistries.BLOCK.getKey(state.getBlock()).getPath().replace("deepslate_", "");
	}

	/** Touching ores of the same kind (diagonals count), not including the one already broken. */
	private static List<BlockPos> findVein(ServerLevel level, BlockPos origin, String family) {
		List<BlockPos> found = new ArrayList<>();
		Set<BlockPos> seen = new HashSet<>();
		ArrayDeque<BlockPos> queue = new ArrayDeque<>();
		queue.add(origin);
		seen.add(origin);

		while (!queue.isEmpty() && found.size() < QolConfig.get().veinMaxOres) {
			BlockPos current = queue.poll();
			for (BlockPos next : BlockPos.betweenClosed(current.offset(-1, -1, -1), current.offset(1, 1, 1))) {
				if (!seen.add(next.immutable())) continue;
				BlockState state = level.getBlockState(next);
				if (isOre(state) && oreFamily(state).equals(family)) {
					found.add(next.immutable());
					queue.add(next.immutable());
				}
			}
		}
		return found;
	}
}
