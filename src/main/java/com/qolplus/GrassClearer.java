package com.qolplus;

import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/** Sneak + hit grass or a flower with a hoe to clear all grass and flowers nearby. */
public final class GrassClearer {
	private GrassClearer() {
	}

	public static InteractionResult onAttack(Player player, Level level, InteractionHand hand, BlockPos pos, Direction direction) {
		if (!QolConfig.get().grassClearing || level.isClientSide() || !player.mayBuild() || !player.isShiftKeyDown()) return InteractionResult.PASS;
		if (!player.getItemInHand(hand).is(ItemTags.HOES) || !isPlant(level.getBlockState(pos))) return InteractionResult.PASS;

		int radius = QolConfig.get().grassRadius;
		for (BlockPos p : BlockPos.betweenClosed(pos.offset(-radius, -1, -radius), pos.offset(radius, 1, radius))) {
			BlockState state = level.getBlockState(p);
			if (!isPlant(state)) continue;
			BlockPos target = p.immutable();
			// Let claim/protection mods veto each block like a normal break.
			if (!PlayerBlockBreakEvents.BEFORE.invoker().beforeBlockBreak(level, player, target, state, null)) continue;
			level.destroyBlock(target, !player.isCreative(), player, 512);
		}
		return InteractionResult.PASS;
	}

	private static boolean isPlant(BlockState state) {
		return state.is(BlockTags.SMALL_FLOWERS)
				|| state.is(Blocks.SHORT_GRASS) || state.is(Blocks.TALL_GRASS)
				|| state.is(Blocks.FERN) || state.is(Blocks.LARGE_FERN)
				|| state.is(Blocks.SHORT_DRY_GRASS) || state.is(Blocks.TALL_DRY_GRASS)
				|| state.is(Blocks.DEAD_BUSH) || state.is(Blocks.BUSH)
				|| state.is(Blocks.SUNFLOWER) || state.is(Blocks.LILAC)
				|| state.is(Blocks.ROSE_BUSH) || state.is(Blocks.PEONY)
				|| state.is(Blocks.PINK_PETALS) || state.is(Blocks.WILDFLOWERS) || state.is(Blocks.LEAF_LITTER);
	}
}
