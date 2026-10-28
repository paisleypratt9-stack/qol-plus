package com.qolplus;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;

/** After a log is broken, leaves around it that lost their tree decay within a few seconds instead of minutes. */
public final class LeafDecay {
	private static final int RADIUS = 7;
	private static final int WATCH_TICKS = 200;
	private static final int INTERVAL = 4;

	private record Watch(ServerLevel level, BlockPos center, long until) {
	}

	private static final List<Watch> WATCHES = new ArrayList<>();

	private LeafDecay() {
	}

	static void clear() {
		WATCHES.clear();
	}

	static void watch(ServerLevel level, BlockPos center) {
		if (!QolConfig.get().fastLeafDecay) return;
		WATCHES.add(new Watch(level, center.immutable(), level.getGameTime() + WATCH_TICKS));
	}

	public static void tick(ServerLevel level) {
		long time = level.getGameTime();
		if (time % INTERVAL != 0) return;

		Iterator<Watch> it = WATCHES.iterator();
		while (it.hasNext()) {
			Watch watch = it.next();
			if (watch.level() != level) continue;
			if (time > watch.until()) {
				it.remove();
				continue;
			}
			decayAround(level, watch.center());
		}
	}

	private static void decayAround(ServerLevel level, BlockPos center) {
		for (BlockPos pos : BlockPos.betweenClosed(center.offset(-RADIUS, -RADIUS, -RADIUS), center.offset(RADIUS, RADIUS, RADIUS))) {
			BlockState state = level.getBlockState(pos);
			if (state.getBlock() instanceof LeavesBlock
					&& !state.getValue(LeavesBlock.PERSISTENT)
					&& state.getValue(LeavesBlock.DISTANCE) >= LeavesBlock.DECAY_DISTANCE) {
				// The vanilla random tick is what removes and drops decaying leaves; we just call it right away.
				state.randomTick(level, pos.immutable(), level.getRandom());
			}
		}
	}
}
