package com.qolplus;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoorHingeSide;
import net.minecraft.world.phys.BlockHitResult;

/** Opening one door of a double door opens the other one too. Sneak to open just one. */
public final class DoubleDoors {
	private DoubleDoors() {
	}

	public static InteractionResult onUseBlock(Player player, Level level, InteractionHand hand, BlockHitResult hit) {
		if (level.isClientSide() || !QolConfig.get().doubleDoors || player.isShiftKeyDown()) return InteractionResult.PASS;
		BlockPos pos = hit.getBlockPos();
		BlockState state = level.getBlockState(pos);
		if (!(state.getBlock() instanceof DoorBlock door) || !door.type().canOpenByHand()) return InteractionResult.PASS;

		Direction facing = state.getValue(DoorBlock.FACING);
		DoorHingeSide hinge = state.getValue(DoorBlock.HINGE);
		BlockPos partnerPos = pos.relative(hinge == DoorHingeSide.LEFT ? facing.getClockWise() : facing.getCounterClockWise());
		BlockState partner = level.getBlockState(partnerPos);
		if (partner.getBlock() instanceof DoorBlock partnerDoor && partnerDoor.type().canOpenByHand()
				&& partner.getValue(DoorBlock.FACING) == facing && partner.getValue(DoorBlock.HINGE) != hinge
				&& partner.getValue(DoorBlock.HALF) == state.getValue(DoorBlock.HALF)
				&& partner.getValue(DoorBlock.OPEN) == state.getValue(DoorBlock.OPEN)) {
			// Vanilla toggles the clicked door right after this, so match what it's about to become.
			partnerDoor.setOpen(player, level, partner, partnerPos, !door.isOpen(state));
		}
		return InteractionResult.PASS;
	}
}
