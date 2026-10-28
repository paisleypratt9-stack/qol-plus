package com.qolplus;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.AttackBlockCallback;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;

/** Server-side features. In single-player these run on the built-in server; on multiplayer the server needs the mod too. */
public class QolPlus implements ModInitializer {
	public static final String MOD_ID = "qolplus";

	@Override
	public void onInitialize() {
		QolConfig.load();
		PlayerBlockBreakEvents.AFTER.register(TreeFeller::afterBreak);
		PlayerBlockBreakEvents.AFTER.register(VeinMiner::afterBreak);
		ServerTickEvents.END_LEVEL_TICK.register(LeafDecay::tick);
		ServerLifecycleEvents.SERVER_STOPPED.register(server -> LeafDecay.clear());
		AttackBlockCallback.EVENT.register(GrassClearer::onAttack);
		Excavator.register();
		ChestSorter.register();
		UseBlockCallback.EVENT.register(CropHarvester::onUseBlock);
		UseBlockCallback.EVENT.register(DoubleDoors::onUseBlock);
		ServerTickEvents.END_SERVER_TICK.register(HotbarRefill::tick);
		ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> HotbarRefill.forget(handler.getPlayer().getUUID()));
	}
}
