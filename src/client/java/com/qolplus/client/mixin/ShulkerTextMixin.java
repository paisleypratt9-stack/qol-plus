package com.qolplus.client.mixin;

import java.util.function.Consumer;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.qolplus.QolConfig;

import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.ItemContainerContents;

/** With Shulker Preview on, skip vanilla's "Diamond x64, and 3 more..." text, since the grid already shows it. */
@Mixin(ItemContainerContents.class)
public class ShulkerTextMixin {
	@Inject(method = "addToTooltip", at = @At("HEAD"), cancellable = true)
	private void qolplus$hideText(Item.TooltipContext context, Consumer<Component> consumer, TooltipFlag flag, DataComponentGetter components,
			CallbackInfo ci) {
		if (QolConfig.get().shulkerPreview) ci.cancel();
	}
}
