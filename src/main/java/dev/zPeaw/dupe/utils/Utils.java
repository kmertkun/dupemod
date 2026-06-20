package dev.zPeaw.dupe.utils;

import dev.zPeaw.dupe.Config;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import static java.lang.Math.floor;

public class Utils {
    private static final MinecraftClient mc = MinecraftClient.getInstance();

    public static void ensureSprint() {
        if (!Config.INSTANCE.sprint || mc.player == null || mc.player.networkHandler == null) {
            return;
        }

        if (!mc.player.isSprinting()) {
            mc.player.setSprinting(true);
            mc.player.networkHandler.sendPacket(
                    new ClientCommandC2SPacket(mc.player, ClientCommandC2SPacket.Mode.START_SPRINTING));
        }
    }

    public static void attackEntity(Entity entity) {
        if (usePacketRoute()) {
            mc.player.networkHandler.sendPacket(
                    PlayerInteractEntityC2SPacket.attack(entity, mc.player.isSneaking()));
            return;
        }

        mc.interactionManager.attackEntity(mc.player, entity);
    }

    public static void interactEntity(Entity entity, Hand hand) {
        if (usePacketRoute()) {
            mc.player.networkHandler.sendPacket(
                    PlayerInteractEntityC2SPacket.interact(entity, mc.player.isSneaking(), hand));
            return;
        }

        mc.interactionManager.interactEntity(mc.player, entity, hand);
    }

    public static void interactEntity(Entity entity, Hand hand, int silentHotbarSlot) {
        if (canSilentSwitch(hand, silentHotbarSlot)) {
            withSilentSelectedSlot(silentHotbarSlot, () -> mc.player.networkHandler.sendPacket(
                    PlayerInteractEntityC2SPacket.interact(entity, mc.player.isSneaking(), hand)));
            return;
        }

        interactEntity(entity, hand);
    }

    public static void interactBlock(Hand hand, BlockHitResult hitResult, int silentHotbarSlot) {
        if (canSilentSwitch(hand, silentHotbarSlot)) {
            withSilentSelectedSlot(silentHotbarSlot, () -> mc.player.networkHandler.sendPacket(
                    new PlayerInteractBlockC2SPacket(hand, hitResult, 0)));
            return;
        }

        mc.interactionManager.interactBlock(mc.player, hand, hitResult);
    }

    public static void moveUpperSlotToHotbar(int inventorySlot) {
        if (mc.player == null) {
            return;
        }
        moveUpperSlotToHotbar(inventorySlot, mc.player.getInventory().getSelectedSlot());
    }

    public static void moveUpperSlotToHotbar(int inventorySlot, int hotbarSlot) {
        if (mc.player == null || mc.interactionManager == null) {
            return;
        }

        if (Config.INSTANCE.silentRoute) {
            mc.interactionManager.clickSlot(
                    mc.player.currentScreenHandler.syncId,
                    inventorySlot,
                    hotbarSlot,
                    SlotActionType.SWAP,
                    mc.player);
        } else {
            mc.player.getInventory().swapSlotWithHotbar(inventorySlot);
        }
    }

    private static boolean usePacketRoute() {
        return Config.INSTANCE.packetRoute || Config.INSTANCE.silentRoute;
    }

    private static boolean canSilentSwitch(Hand hand, int silentHotbarSlot) {
        return Config.INSTANCE.silentRoute
                && hand == Hand.MAIN_HAND
                && silentHotbarSlot >= 0
                && silentHotbarSlot <= 8
                && mc.player != null
                && mc.player.networkHandler != null;
    }

    private static void withSilentSelectedSlot(int hotbarSlot, Runnable action) {
        int currentSlot = mc.player.getInventory().getSelectedSlot();
        if (hotbarSlot == currentSlot) {
            action.run();
            return;
        }

        mc.player.networkHandler.sendPacket(new UpdateSelectedSlotC2SPacket(hotbarSlot));
        try {
            action.run();
        } finally {
            mc.player.networkHandler.sendPacket(new UpdateSelectedSlotC2SPacket(currentSlot));
        }
    }

    public static void TPX(Vec3d pos, Vec3d startPos) {

        if (mc.player.isSneaking()) {
            mc.player.setSneaking(false);
        }

        double distance = startPos.distanceTo(pos);

        int packetsRequired = (int) Math.ceil(Math.abs(distance / 10));
        for (int packetNumber = 0; packetNumber < (packetsRequired - 1); packetNumber++) {
            //? if <=1.20.1 {
            /*mc.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.OnGroundOnly(true, false));
            *///?} else {
            mc.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.OnGroundOnly(true, false));
            //?}
        }

        //? if <=1.20.1 {
        /*mc.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(pos.x, pos.y, pos.z, true, false));
        *///?} else {
        mc.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(pos.x, pos.y, pos.z, true, false));
        //?}
    }

    public static void TPX(Vec3d pos) {
        TPX(pos, new Vec3d(mc.player.getX(), mc.player.getY(), mc.player.getZ()));
    }

    public static BlockPos Vec3d2BlockPos(Vec3d pos) {
        return new BlockPos((int) floor(pos.x), (int) floor(pos.y), (int) floor(pos.z));
    }
}
