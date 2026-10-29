package com.qolplus.client;

import com.qolplus.QolConfig;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.util.Util;
import net.minecraft.world.phys.Vec3;

/**
 * Freecam (toggle with its keybind, F6 by default): fly the camera around while your player stays put.
 * Single-player only unless "Allow on Servers" is turned on, since many servers count it as cheating.
 */
public final class Freecam {
	private static boolean active;
	private static Vec3 position = Vec3.ZERO;
	private static float yaw;
	private static float pitch;
	private static long lastFrame;

	private Freecam() {
	}

	public static boolean isActive() {
		return active;
	}

	public static float yaw() {
		return yaw;
	}

	public static float pitch() {
		return pitch;
	}

	public static void turn(double xo, double yo) {
		yaw += (float) xo * 0.15f;
		pitch = Mth.clamp(pitch + (float) yo * 0.15f, -90f, 90f);
	}

	public static void tick(Minecraft mc) {
		QolConfig c = QolConfig.get();
		if (c.freecamOn && !active) start(mc);
		else if (!c.freecamOn && active) active = false;
		// Dying or leaving the world ends freecam.
		if (active && (mc.player == null || mc.player.isDeadOrDying())) c.freecamOn = active = false;
	}

	private static void start(Minecraft mc) {
		QolConfig c = QolConfig.get();
		if (mc.player == null) {
			c.freecamOn = false;
			return;
		}
		if (mc.getSingleplayerServer() == null && !c.freecamOnServers) {
			c.freecamOn = false;
			mc.player.sendOverlayMessage(Component.literal("Freecam is single-player only (turn on Allow on Servers in its settings)")
					.withStyle(ChatFormatting.RED));
			return;
		}
		active = true;
		position = mc.player.getEyePosition();
		yaw = mc.player.getYRot();
		pitch = mc.player.getXRot();
		lastFrame = 0;
	}

	public static void onDisconnect() {
		QolConfig.get().freecamOn = false;
		active = false;
	}

	/** Called by the camera mixin every frame: moves with WASD, Space (up) and Shift (down), Sprint for faster. */
	public static Vec3 frame() {
		Minecraft mc = Minecraft.getInstance();
		long now = Util.getMillis();
		double seconds = lastFrame == 0 ? 0 : Math.min(0.1, (now - lastFrame) / 1000.0);
		lastFrame = now;
		if (mc.gui.screen() != null) return position;

		double forward = (mc.options.keyUp.isDown() ? 1 : 0) - (mc.options.keyDown.isDown() ? 1 : 0);
		double strafe = (mc.options.keyLeft.isDown() ? 1 : 0) - (mc.options.keyRight.isDown() ? 1 : 0);
		double up = (mc.options.keyJump.isDown() ? 1 : 0) - (mc.options.keyShift.isDown() ? 1 : 0);
		double yawRad = Math.toRadians(yaw);
		double pitchRad = Math.toRadians(pitch);

		Vec3 look = new Vec3(-Math.sin(yawRad) * Math.cos(pitchRad), -Math.sin(pitchRad), Math.cos(yawRad) * Math.cos(pitchRad));
		Vec3 left = new Vec3(Math.cos(yawRad), 0, Math.sin(yawRad));
		Vec3 move = look.scale(forward).add(left.scale(strafe)).add(0, up, 0);
		if (move.lengthSqr() > 0) {
			double speed = QolConfig.get().freecamSpeed * (mc.options.keySprint.isDown() ? 2.5 : 1);
			position = position.add(move.normalize().scale(speed * seconds));
		}
		return position;
	}
}
