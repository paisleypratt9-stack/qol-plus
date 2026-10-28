package com.qolplus.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.qolplus.QolConfig;

import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;

/** Hold the Freelook keybind (Left Alt by default) to look around in third person without turning your player. */
public final class Freelook {
	private static boolean active;
	private static float yaw;
	private static float pitch;
	private static CameraType previousCamera;

	private Freelook() {
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
		int key = Modules.bind(Modules.FREELOOK);
		boolean held = QolConfig.get().freelook && mc.player != null && mc.gui.screen() == null
				&& key != Modules.NO_BIND && InputConstants.isKeyDown(key);
		if (held && !active) {
			active = true;
			yaw = mc.player.getYRot();
			pitch = mc.player.getXRot();
			previousCamera = mc.options.getCameraType();
			mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);
		} else if (!held && active) {
			active = false;
			if (previousCamera != null) mc.options.setCameraType(previousCamera);
		}
	}
}
