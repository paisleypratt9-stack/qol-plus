package com.qolplus.client;

import java.util.ArrayList;
import java.util.List;

import com.qolplus.QolConfig;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gizmos.GizmoStyle;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.phys.AABB;

/**
 * Toggle with its keybind (F7 by default): marks blocks around you where mobs can spawn. Red = always (dark caves, no torch light),
 * yellow = only at night (no torch light, but open to the sky).
 */
public final class LightOverlay {
	private static final GizmoStyle ALWAYS = GizmoStyle.fill(0x60FF2020);
	private static final GizmoStyle NIGHT = GizmoStyle.fill(0x60FFD020);

	private static List<BlockPos> always = List.of();
	private static List<BlockPos> night = List.of();

	private LightOverlay() {
	}

	public static void tick(Minecraft mc) {
		if (!QolConfig.get().lightOverlayOn) {
			always = List.of();
			night = List.of();
			return;
		}
		if (mc.player == null || mc.level == null) return;
		if (mc.level.getGameTime() % 10 == 0) scan(mc.level, mc.player.blockPosition());
		// Gizmos added during a client tick are drawn until the next tick, so re-add them every tick.
		for (BlockPos pos : always) Gizmos.cuboid(new AABB(pos.getX(), pos.getY(), pos.getZ(), pos.getX() + 1, pos.getY() + 0.03, pos.getZ() + 1), ALWAYS);
		for (BlockPos pos : night) Gizmos.cuboid(new AABB(pos.getX(), pos.getY(), pos.getZ(), pos.getX() + 1, pos.getY() + 0.03, pos.getZ() + 1), NIGHT);
	}

	private static void scan(ClientLevel level, BlockPos center) {
		List<BlockPos> foundAlways = new ArrayList<>();
		List<BlockPos> foundNight = new ArrayList<>();
		int radius = QolConfig.get().lightRange;
		int height = Math.max(4, radius / 2);
		for (BlockPos p : BlockPos.betweenClosed(center.offset(-radius, -height, -radius), center.offset(radius, height, radius))) {
			if (level.getBrightness(LightLayer.BLOCK, p) > 0) continue;
			if (!level.getBlockState(p).getCollisionShape(level, p).isEmpty() || !level.getFluidState(p).isEmpty()) continue;
			BlockPos below = p.below();
			if (!level.getBlockState(below).isFaceSturdy(level, below, Direction.UP)) continue;
			(level.getBrightness(LightLayer.SKY, p) > 0 ? foundNight : foundAlways).add(p.immutable());
		}
		always = foundAlways;
		night = foundNight;
	}
}
