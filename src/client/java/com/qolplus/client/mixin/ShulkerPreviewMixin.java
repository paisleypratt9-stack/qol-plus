package com.qolplus.client.mixin;

import java.util.Optional;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.qolplus.client.ShulkerPreview;

import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/** Shulker Preview: shulker boxes get a grid of their contents in the tooltip. */
@Mixin(Item.class)
public class ShulkerPreviewMixin {
	@Inject(method = "getTooltipImage", at = @At("HEAD"), cancellable = true)
	private void qolplus$shulkerPreview(ItemStack stack, CallbackInfoReturnable<Optional<TooltipComponent>> cir) {
		ShulkerPreview.Contents contents = ShulkerPreview.of(stack);
		if (contents != null) cir.setReturnValue(Optional.of(contents));
	}
}
