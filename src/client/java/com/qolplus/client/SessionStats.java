package com.qolplus.client;

import java.util.HashSet;
import java.util.Set;

import net.minecraft.client.Minecraft;
import net.minecraft.util.Util;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/** Session Stats: blocks mined, kills, distance travelled and time played since you joined the world. */
public final class SessionStats {
	private static long joinedAt = Util.getMillis();
	private static int blocksMined;
	private static int kills;
	private static double distance;
	private static Vec3 lastPos;
	private static int lastAttacked = -1;
	private static long lastAttackTime;
	private static final Set<Integer> counted = new HashSet<>();

	private SessionStats() {
	}

	public static void reset() {
		joinedAt = Util.getMillis();
		blocksMined = 0;
		kills = 0;
		distance = 0;
		lastPos = null;
		lastAttacked = -1;
		counted.clear();
	}

	public static void onBlockMined() {
		blocksMined++;
	}

	public static void onAttack(Entity target) {
		lastAttacked = target.getId();
		lastAttackTime = Util.getMillis();
	}

	public static void tick(Minecraft mc) {
		if (mc.player == null || mc.level == null) return;
		Vec3 pos = mc.player.position();
		if (lastPos != null) {
			double moved = pos.subtract(lastPos).horizontalDistance();
			if (moved < 10) distance += moved; // ignore teleports
		}
		lastPos = pos;

		// A mob you hit in the last 5 seconds that dies counts as a kill.
		if (lastAttacked != -1 && Util.getMillis() - lastAttackTime < 5000 && !counted.contains(lastAttacked)
				&& mc.level.getEntity(lastAttacked) instanceof LivingEntity target && target.isDeadOrDying()) {
			kills++;
			counted.add(lastAttacked);
		}
	}

	public static String[][] lines() {
		long minutes = (Util.getMillis() - joinedAt) / 60000;
		String time = minutes >= 60 ? minutes / 60 + "h " + minutes % 60 + "m" : minutes + "m";
		String walked = distance >= 1000 ? String.format("%.1fkm", distance / 1000) : (int) distance + "m";
		return new String[][] {{"Mined", String.valueOf(blocksMined)}, {"Kills", String.valueOf(kills)},
				{"Walked", walked}, {"Played", time}};
	}
}
