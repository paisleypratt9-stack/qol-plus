package com.qolplus.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.qolplus.QolConfig;

import net.minecraft.util.Mth;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.MerchantOffer;

/**
 * Infinite Trading: trades never lock. Fixed Prices: prices never go up (no demand increase, no penalty for hurting
 * villagers), but discounts like curing a zombie villager or Hero of the Village still apply.
 */
@Mixin(MerchantOffer.class)
public class MerchantOfferMixin {
	@Shadow
	private int specialPriceDiff;

	@Inject(method = "isOutOfStock", at = @At("HEAD"), cancellable = true)
	private void qolplus$neverLock(CallbackInfoReturnable<Boolean> cir) {
		if (QolConfig.get().infiniteTrading) cir.setReturnValue(false);
	}

	@Inject(method = "getModifiedCostCount", at = @At("HEAD"), cancellable = true)
	private void qolplus$fixedPrice(ItemCost cost, CallbackInfoReturnable<Integer> cir) {
		if (!QolConfig.get().fixedVillagerPrices) return;
		cir.setReturnValue(Mth.clamp(cost.count() + Math.min(0, specialPriceDiff), 1, cost.itemStack().getMaxStackSize()));
	}
}
