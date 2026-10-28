package com.qolplus.client.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.qolplus.QolConfig;

import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.world.level.block.entity.BannerBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.phys.Vec3;

/** Hide Far Signs: signs and banners further away than the setting aren't drawn (their text is the expensive part). */
@Mixin(BlockEntityRenderDispatcher.class)
public class SignDistanceMixin {
	@Shadow
	private Vec3 cameraPos;

	@Inject(method = "tryExtractRenderState", at = @At("HEAD"), cancellable = true)
	private void qolplus$hideFarSigns(BlockEntity blockEntity, float partialTicks, ModelFeatureRenderer.CrumblingOverlay breakProgress,
			boolean isGloballyRendered, CallbackInfoReturnable<BlockEntityRenderState> cir) {
		QolConfig c = QolConfig.get();
		if (!c.hideFarSigns || cameraPos == null) return;
		if (!(blockEntity instanceof SignBlockEntity || blockEntity instanceof BannerBlockEntity)) return;
		if (!Vec3.atCenterOf(blockEntity.getBlockPos()).closerThan(cameraPos, c.signDistance)) cir.setReturnValue(null);
	}
}
