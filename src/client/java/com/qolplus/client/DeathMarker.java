package com.qolplus.client;

import com.qolplus.QolConfig;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;

/** After you die, shows how far away your death spot is and an arrow pointing to it, until you get there. */
public final class DeathMarker {
	private static final String[] ARROWS = {"↑", "↗", "→", "↘", "↓", "↙", "←", "↖"};

	private static BlockPos deathPos;
	private static ResourceKey<Level> deathLevel;
	private static boolean wasDead;

	private DeathMarker() {
	}

	public static void tick(Minecraft mc) {
		LocalPlayer player = mc.player;
		if (player == null || mc.level == null) return;
		boolean dead = player.isDeadOrDying();
		if (dead && !wasDead) {
			deathPos = player.blockPosition();
			deathLevel = mc.level.dimension();
		}
		wasDead = dead;
		if (!dead && deathPos != null && mc.level.dimension() == deathLevel && player.blockPosition().closerThan(deathPos, 4)) deathPos = null;
	}

	public static void extract(GuiGraphicsExtractor graphics, Minecraft mc, LocalPlayer player) {
		if (deathPos == null || player.isDeadOrDying() || !QolConfig.get().deathWaypoint) return;
		Component text;
		if (mc.level.dimension() != deathLevel) {
			text = Component.translatable("hud.qolplus.death_other_dimension");
		} else {
			double dx = deathPos.getX() + 0.5 - player.getX();
			double dz = deathPos.getZ() + 0.5 - player.getZ();
			float targetYaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
			int arrow = Math.floorMod(Math.round(Mth.wrapDegrees(targetYaw - player.getYRot()) / 45f), 8);
			text = Component.translatable("hud.qolplus.death", (int) Math.sqrt(dx * dx + dz * dz), ARROWS[arrow]);
		}
		graphics.centeredText(mc.font, text, graphics.guiWidth() / 2, 4, 0xFFFF5555);
	}

	public static void onDisconnect() {
		deathPos = null;
		wasDead = false;
	}
}
