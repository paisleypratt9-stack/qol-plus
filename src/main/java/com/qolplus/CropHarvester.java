package com.qolplus;

import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.NetherWartBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/** Right-click a fully grown crop to harvest it and plant it again (uses one of the seeds it drops). */
public final class CropHarvester {
	private CropHarvester() {
	}

	public static InteractionResult onUseBlock(Player player, Level level, InteractionHand hand, BlockHitResult hit) {
		if (!(level instanceof ServerLevel server) || !QolConfig.get().rightClickHarvest) return InteractionResult.PASS;
		if (hand != InteractionHand.MAIN_HAND || player.isShiftKeyDown() || !player.mayBuild()) return InteractionResult.PASS;
		BlockPos pos = hit.getBlockPos();
		BlockState state = server.getBlockState(pos);
		if (!isRipe(state)) return InteractionResult.PASS;
		// Let claim/protection mods veto it like a normal break.
		if (!PlayerBlockBreakEvents.BEFORE.invoker().beforeBlockBreak(server, player, pos, state, null)) return InteractionResult.PASS;

		boolean replanted = false;
		for (ItemStack drop : Block.getDrops(state, server, pos, null, player, player.getMainHandItem())) {
			if (!replanted && drop.getItem() instanceof BlockItem seed && seed.getBlock() == state.getBlock()) {
				drop.shrink(1);
				replanted = true;
			}
			if (!drop.isEmpty()) Block.popResource(server, pos, drop);
		}
		server.setBlock(pos, replanted ? state.getBlock().defaultBlockState() : Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
		server.playSound(null, pos, state.getSoundType().getBreakSound(), SoundSource.BLOCKS, 1.0f, 1.0f);
		return InteractionResult.SUCCESS;
	}

	private static boolean isRipe(BlockState state) {
		if (state.getBlock() instanceof CropBlock crop) return crop.isMaxAge(state);
		return state.is(Blocks.NETHER_WART) && state.getValue(NetherWartBlock.AGE) >= NetherWartBlock.MAX_AGE;
	}
}
