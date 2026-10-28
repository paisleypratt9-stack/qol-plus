package com.qolplus.client;

import java.util.HashSet;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mojang.blaze3d.platform.InputConstants;
import com.qolplus.Excavator;
import com.qolplus.QolConfig;
import com.qolplus.QolPlus;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.ClientTooltipComponentCallback;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.event.client.player.ClientPlayerBlockBreakEvents;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.minecraft.ChatFormatting;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.InteractionResult;

public class QolPlusClient implements ClientModInitializer {
	public static final Logger LOGGER = LoggerFactory.getLogger(QolPlus.MOD_ID);

	private static final KeyMapping.Category CATEGORY = KeyMapping.Category.register(Identifier.fromNamespaceAndPath(QolPlus.MOD_ID, "main"));
	private static final KeyMapping MENU_KEY = KeyMappingHelper.registerKeyMapping(new KeyMapping(
			"key.qolplus.menu", InputConstants.Type.KEYBOARD, InputConstants.KEY_RSHIFT, CATEGORY));

	/** Module keybinds held down last tick, so holding a key toggles only once. */
	private static final Set<String> heldBinds = new HashSet<>();
	/** What the server was last told about the excavator, or null if it hasn't been told yet. */
	private static Boolean excavatorSent;

	@Override
	public void onInitializeClient() {
		ScreenEvents.AFTER_INIT.register(ChestFeatures::onScreenOpened);

		ItemTooltipCallback.EVENT.register(ItemTooltips::append);
		ClientPlayConnectionEvents.JOIN.register((handler, sender, mc) -> {
			excavatorSent = null;
			SessionStats.reset();
		});
		ClientPlayerBlockBreakEvents.AFTER.register((level, player, pos, state) -> SessionStats.onBlockMined());
		AttackEntityCallback.EVENT.register((player, level, hand, entity, hit) -> {
			if (level.isClientSide()) SessionStats.onAttack(entity);
			return InteractionResult.PASS;
		});
		ClientTooltipComponentCallback.EVENT.register(data -> data instanceof ShulkerPreview.Contents contents ? new ShulkerPreview.Grid(contents) : null);
		ClientPlayConnectionEvents.DISCONNECT.register((handler, mc) -> DeathMarker.onDisconnect());

		ClientTickEvents.END_CLIENT_TICK.register(mc -> {
			while (MENU_KEY.consumeClick()) {
				if (mc.player != null) mc.setScreenAndShow(QolConfig.get().classicMenu ? new ClickGui() : new CleanMenu());
			}
			tickBinds(mc);
			syncExcavator(mc);
			QolHud.tickWarnings(mc);
			DeathMarker.tick(mc);
			LightOverlay.tick(mc);
			Performance.tick(mc);
			Freelook.tick(mc);
			HitColor.tick(mc);
			SessionStats.tick(mc);
		});

		HudElementRegistry.attachElementAfter(VanillaHudElements.HOTBAR,
				Identifier.fromNamespaceAndPath(QolPlus.MOD_ID, "hud"), QolHud::extract);
		HudElementRegistry.replaceElement(VanillaHudElements.CROSSHAIR, vanilla -> (graphics, delta) -> {
			if (QolConfig.get().customCrosshair) Crosshair.extract(graphics, delta);
			else vanilla.extractRenderState(graphics, delta);
		});
	}

	public static boolean isZooming() {
		Minecraft mc = Minecraft.getInstance();
		int key = Modules.bind(Modules.ZOOM);
		return QolConfig.get().zoom && key != Modules.NO_BIND && mc.gui.screen() == null && InputConstants.isKeyDown(key);
	}

	/** Pressing a module's keybind (while playing, not in a menu) toggles it. Zoom's key is held instead. */
	private static void tickBinds(Minecraft mc) {
		boolean playing = mc.player != null && mc.gui.screen() == null;
		for (Modules.Module module : Modules.ALL) {
			int key = Modules.bind(module.name());
			boolean down = playing && key != Modules.NO_BIND && InputConstants.isKeyDown(key);
			if (!down) {
				heldBinds.remove(module.name());
				continue;
			}
			// Zoom and Freelook keys are held, not toggles.
			if (!heldBinds.add(module.name()) || module.name().equals(Modules.ZOOM) || module.name().equals(Modules.FREELOOK)) continue;
			module.toggle();
			mc.player.sendOverlayMessage(Component.literal(module.name() + (module.enabled() ? " ON" : " OFF"))
					.withStyle(module.enabled() ? ChatFormatting.GREEN : ChatFormatting.GRAY));
		}
	}

	/** Tells the server whenever the Excavator module is turned on or off (and once after joining). */
	private static void syncExcavator(Minecraft mc) {
		boolean on = QolConfig.get().excavatorActive;
		if (mc.player == null || Boolean.valueOf(on).equals(excavatorSent)) return;
		if (!ClientPlayNetworking.canSend(Excavator.TogglePayload.TYPE)) {
			if (on) mc.player.sendOverlayMessage(Component.translatable("message.qolplus.excavator_unavailable").withStyle(ChatFormatting.RED));
			excavatorSent = on;
			return;
		}
		ClientPlayNetworking.send(new Excavator.TogglePayload(on));
		excavatorSent = on;
	}
}
