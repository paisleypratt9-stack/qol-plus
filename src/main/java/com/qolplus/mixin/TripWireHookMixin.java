package com.qolplus.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import com.qolplus.QolConfig;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.TripWireHookBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * String Duper: undoes Mojang's string duplication fix. When a hook re-attaches its wire it used to put string back
 * on every spot it remembered, even if that string had just been pushed away, which is what string dupers rely on.
 * Mojang added a "is there still tripwire here?" check; with this on, that check always passes.
 */
@Mixin(TripWireHookBlock.class)
public class TripWireHookMixin {
	@Redirect(method = "calculateState", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/world/level/block/state/BlockState;is(Ljava/lang/Object;)Z", ordinal = 3))
	private static boolean qolplus$allowStringDupe(BlockState state, Object block) {
		return QolConfig.get().stringDupe || state.is((Block) block);
	}
}
