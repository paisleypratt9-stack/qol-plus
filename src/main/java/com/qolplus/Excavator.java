package com.qolplus;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Toggle with the Excavator keybind (B by default): breaking stone, deepslate, ores, dirt, sand or gravel with a pickaxe or shovel also breaks the 3x3
 * (or bigger, see the Size setting) around it, facing the way you look. Sneak to mine a single block. The client tells the server when you toggle it.
 */
public final class Excavator {
	public record TogglePayload(boolean on) implements CustomPacketPayload {
		public static final Type<TogglePayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath(QolPlus.MOD_ID, "excavator"));
		public static final StreamCodec<RegistryFriendlyByteBuf, TogglePayload> CODEC =
				StreamCodec.composite(ByteBufCodecs.BOOL, TogglePayload::on, TogglePayload::new);

		@Override
		public Type<TogglePayload> type() {
			return TYPE;
		}
	}

	private static final Set<UUID> enabled = new HashSet<>();
	private static boolean digging;

	private Excavator() {
	}

	public static void register() {
		PayloadTypeRegistry.serverboundPlay().register(TogglePayload.TYPE, TogglePayload.CODEC);
		ServerPlayNetworking.registerGlobalReceiver(TogglePayload.TYPE, (payload, context) -> {
			if (payload.on()) enabled.add(context.player().getUUID());
			else enabled.remove(context.player().getUUID());
		});
		ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> enabled.remove(handler.getPlayer().getUUID()));
		PlayerBlockBreakEvents.AFTER.register(Excavator::afterBreak);
	}

	private static void afterBreak(Level level, Player player, BlockPos pos, BlockState state, BlockEntity blockEntity) {
		if (!(level instanceof ServerLevel server) || digging || !QolConfig.get().excavator) return;
		if (!enabled.contains(player.getUUID()) || player.isShiftKeyDown() || !player.mayBuild()) return;
		ItemStack tool = player.getMainHandItem();
		if (!canDig(player, tool, state)) return;

		// Looking mostly up or down digs a flat layer, otherwise a wall facing you.
		Direction.Axis axis = Math.abs(player.getXRot()) > 45 ? Direction.Axis.Y : player.getDirection().getAxis();
		int r = Math.clamp(QolConfig.get().excavatorSize, 3, 7) / 2;
		int rx = axis == Direction.Axis.X ? 0 : r;
		int ry = axis == Direction.Axis.Y ? 0 : r;
		int rz = axis == Direction.Axis.Z ? 0 : r;

		digging = true;
		try {
			for (BlockPos p : BlockPos.betweenClosed(pos.offset(-rx, -ry, -rz), pos.offset(rx, ry, rz))) {
				if (tool.isEmpty()) break; // tool broke
				BlockPos target = p.immutable();
				BlockState targetState = server.getBlockState(target);
				if (target.equals(pos) || !canDig(player, tool, targetState)) continue;
				BlockEntity targetEntity = server.getBlockEntity(target);
				// Let claim/protection mods veto each extra block like a normal break.
				if (!PlayerBlockBreakEvents.BEFORE.invoker().beforeBlockBreak(server, player, target, targetState, targetEntity)) continue;
				if (!player.isCreative()) {
					for (ItemStack drop : Block.getDrops(targetState, server, target, targetEntity, player, tool)) Block.popResource(server, target, drop);
					targetState.spawnAfterBreak(server, target, tool, true);
				}
				server.destroyBlock(target, false, player, 512);
				tool.hurtAndBreak(1, player, EquipmentSlot.MAINHAND);
				player.causeFoodExhaustion(0.005f);
			}
		} finally {
			digging = false;
		}
	}

	/** Only plain terrain, and only with the tool that's meant for it. */
	private static boolean canDig(Player player, ItemStack tool, BlockState state) {
		boolean terrain = state.is(BlockTags.BASE_STONE_OVERWORLD) || state.is(BlockTags.ORES)
				|| state.is(Blocks.COBBLESTONE) || state.is(Blocks.COBBLED_DEEPSLATE)
				|| state.is(BlockTags.DIRT) || state.is(Blocks.SAND) || state.is(Blocks.RED_SAND) || state.is(Blocks.GRAVEL);
		if (!terrain) return false;
		if (tool.is(ItemTags.PICKAXES)) return state.is(BlockTags.MINEABLE_WITH_PICKAXE) && player.hasCorrectToolForDrops(state);
		if (tool.is(ItemTags.SHOVELS)) return state.is(BlockTags.MINEABLE_WITH_SHOVEL);
		return false;
	}
}
