package dev.zPeaw.dupe;

import dev.zPeaw.dupe.utils.Utils;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.ItemFrameEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemFrameItem;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class DuperManager {
    private static final MinecraftClient mc = MinecraftClient.getInstance();
    private static final List<FrameSnapshot> targetFrames = new ArrayList<>();
    private static final List<ItemFrameEntity> dontHit = new ArrayList<>();

    private static final long DPS_UPDATE_INTERVAL_MS = 1000;
    private static final long CLEANUP_INTERVAL_MS = 5000;
    private static final long COOLDOWN_EXPIRY_MS = 10000;
    private static final int DISPLAY_DPS_MIN = 15;
    private static final int DISPLAY_DPS_MAX = 20;

    private static final Map<Integer, Long> interactionCooldowns = new ConcurrentHashMap<>();

    private static int currentDps = 0;
    private static long lastDpsUpdate = 0;
    private static int accumulatedDupes = 0;

    public static int getDps() {
        return currentDps;
    }

    private record FrameSnapshot(Vec3d pos, BlockPos supportPos, net.minecraft.util.math.Direction facing) {
    }

    private record PreparedHand(Hand hand, int silentHotbarSlot) {
        private static PreparedHand main() {
            return new PreparedHand(Hand.MAIN_HAND, -1);
        }

        private static PreparedHand offhand() {
            return new PreparedHand(Hand.OFF_HAND, -1);
        }

        private static PreparedHand silentMain(int hotbarSlot) {
            return new PreparedHand(Hand.MAIN_HAND, hotbarSlot);
        }
    }

    private static int placements = 0;
    private static int swaps = 0;
    private static int moves = 0;
    private static long lastCleanup = 0;

    private static int lastDetectedFrames = 0;
    private static int lastActiveFrames = 0;
    private static int lastEmptyFrames = 0;
    private static int lastReadyFrames = 0;
    private static int lastMissingFrames = 0;
    private static int lastPlacements = 0;
    private static int lastSwaps = 0;
    private static int lastMoves = 0;
    private static int lastDupeItemCount = 0;

    public static int getDetectedFrameCount() {
        return lastDetectedFrames;
    }

    public static int getActiveFrameCount() {
        return lastActiveFrames;
    }

    public static int getEmptyFrameCount() {
        return lastEmptyFrames;
    }

    public static int getReadyFrameCount() {
        return lastReadyFrames;
    }

    public static int getMissingFrameCount() {
        return lastMissingFrames;
    }

    public static int getLastPlacements() {
        return lastPlacements;
    }

    public static int getLastSwaps() {
        return lastSwaps;
    }

    public static int getLastMoves() {
        return lastMoves;
    }

    public static int getDupeItemCount() {
        return lastDupeItemCount;
    }

    public static void onDupeKeyPressed() {
        Config.INSTANCE.enabled = !Config.INSTANCE.enabled;
        Config.save();
        if (Config.INSTANCE.enabled) {
            reloadFrames();
        } else {
            targetFrames.clear();
            dontHit.clear();
            interactionCooldowns.clear();
        }
    }

    public static void reloadFrames() {
        targetFrames.clear();
        dontHit.clear();
        if (mc.player == null || mc.world == null)
            return;

        double rangeSq = Config.INSTANCE.range * Config.INSTANCE.range;
        List<ItemFrameEntity> detected = new ArrayList<>();

        for (Entity entity : mc.world.getEntities()) {
            if (entity instanceof ItemFrameEntity itemFrame) {
                if (itemFrame.isRemoved() || !itemFrame.isAlive())
                    continue;
                if (entity.squaredDistanceTo(mc.player) <= rangeSq) {
                    detected.add(itemFrame);
                }
            }
        }

        detected.sort((f1, f2) -> Float.compare(f1.distanceTo(mc.player), f2.distanceTo(mc.player)));

        int limit = Config.INSTANCE.maxFrames;
        if (detected.size() > limit) {
            detected = detected.subList(0, limit);
        }

        for (ItemFrameEntity frame : detected) {
            Vec3d rotationVec = frame.getRotationVector();
            if (rotationVec == null)
                continue;
            BlockPos supportPos = Utils.Vec3d2BlockPos(frame.getPos().subtract(rotationVec.normalize()));
            targetFrames.add(new FrameSnapshot(frame.getPos(), supportPos, frame.getHorizontalFacing()));
        }
        lastDetectedFrames = detected.size();
        lastActiveFrames = targetFrames.size();
        lastMissingFrames = 0;
    }

    public static void tick() {
        long now = System.currentTimeMillis();
        if (now - lastDpsUpdate >= DPS_UPDATE_INTERVAL_MS) {
            int measuredDps = accumulatedDupes / 2;
            currentDps = measuredDps > 0
                    ? Math.max(DISPLAY_DPS_MIN, Math.min(DISPLAY_DPS_MAX, measuredDps))
                    : 0;
            accumulatedDupes = 0;
            lastDpsUpdate = now;
        }

        if (mc.player != null) {
            lastDupeItemCount = countDupeItems(mc.player.getInventory(), Config.getDupeItems());
        } else {
            lastDupeItemCount = 0;
        }

        if (!Config.INSTANCE.enabled || mc.player == null || mc.world == null || mc.interactionManager == null)
            return;

        if (!Config.INSTANCE.multitask) {
            if (mc.player.isUsingItem() || mc.options.attackKey.isPressed() || mc.options.useKey.isPressed()
                    || mc.currentScreen != null) {
                return;
            }
        }

        Utils.ensureSprint();

        if (now - lastCleanup >= CLEANUP_INTERVAL_MS) {
            cleanupDeadEntities();
            lastCleanup = now;
        }

        placements = 0;
        swaps = 0;
        moves = 0;

        double rangeSq = Config.INSTANCE.range * Config.INSTANCE.range;
        List<Item> dupeItemList = Config.getDupeItems();

        List<ItemFrameEntity> allDetectedFrames = new ArrayList<>();
        for (Entity entity : mc.world.getEntities()) {
            if (entity instanceof ItemFrameEntity itemFrame) {
                if (entity.squaredDistanceTo(mc.player.getEyePos()) <= rangeSq) {
                    allDetectedFrames.add(itemFrame);
                }
            }
        }
        lastDetectedFrames = allDetectedFrames.size();

        if (Config.INSTANCE.mode == Config.Mode.Normal) {
            handleNormalMode(allDetectedFrames, dupeItemList, rangeSq);
        } else if (Config.INSTANCE.mode == Config.Mode.Speed) {
            handleSpeedMode(allDetectedFrames, dupeItemList, rangeSq);
        }

        lastPlacements = placements;
        lastSwaps = swaps;
        lastMoves = moves;
    }

    private static void handleNormalMode(List<ItemFrameEntity> allDetectedFrames, List<Item> dupeItemList,
            double rangeSq) {
        PlayerInventory inv = mc.player.getInventory();
        int maxPlacements = Config.INSTANCE.maxPlacements;
        int maxSwaps = Config.INSTANCE.maxSwaps;
        int maxInventoryMoves = Config.INSTANCE.maxInventoryMoves;

        List<ItemFrameEntity> activeFrames = new ArrayList<>(allDetectedFrames);
        activeFrames.sort((f1, f2) -> Float.compare(f1.distanceTo(mc.player), f2.distanceTo(mc.player)));

        int limit = Config.INSTANCE.maxFrames;
        if (activeFrames.size() > limit) {
            activeFrames = activeFrames.subList(0, limit);
        }

        List<ItemFrameEntity> emptyItemFrames = new ArrayList<>();
        List<ItemFrameEntity> filledItemFrames = new ArrayList<>();

        for (ItemFrameEntity frame : activeFrames) {
            if (frame.isRemoved() || !frame.isAlive())
                continue;
            if (frame.squaredDistanceTo(mc.player.getEyePos()) > rangeSq)
                continue;

            if (frame.getHeldItemStack().isEmpty()) {
                emptyItemFrames.add(frame);
            } else if (dupeItemList.contains(frame.getHeldItemStack().getItem())) {
                filledItemFrames.add(frame);
            }
        }

        lastActiveFrames = activeFrames.size();
        lastEmptyFrames = emptyItemFrames.size();
        lastReadyFrames = filledItemFrames.size();
        lastMissingFrames = 0;

        for (ItemFrameEntity emptyFrame : emptyItemFrames) {
            if (placements >= maxPlacements)
                break;

            PreparedHand dupeHand = prepareDupeItemHand(inv, dupeItemList, maxSwaps, maxInventoryMoves);
            if (dupeHand != null) {
                interactItemFrame(emptyFrame, dupeHand);
                dontHit.remove(emptyFrame);
                placements++;
            }
        }

        for (ItemFrameEntity filledFrame : filledItemFrames) {
            if (!dontHit.contains(filledFrame)) {
                if (Config.INSTANCE.checkStatus && filledFrame.getHeldItemStack().isEmpty())
                    continue;

                long currentTime = System.currentTimeMillis();
                if (Config.INSTANCE.antiDoubleClick && currentTime
                        - interactionCooldowns.getOrDefault(filledFrame.getId(), 0L) < Config.INSTANCE.doubleClickDelay)
                    continue;

                Utils.attackEntity(filledFrame);
                accumulatedDupes++;
                dontHit.add(filledFrame);
                interactionCooldowns.put(filledFrame.getId(), currentTime);
            }
        }
    }

    private static void handleSpeedMode(List<ItemFrameEntity> allDetectedFrames, List<Item> dupeItemList,
            double rangeSq) {
        List<ItemFrameEntity> presentTargets = new ArrayList<>();
        List<FrameSnapshot> missingTargets = new ArrayList<>();

        for (FrameSnapshot target : targetFrames) {
            if (target.pos.squaredDistanceTo(mc.player.getEyePos()) > rangeSq) {
                continue;
            }

            ItemFrameEntity found = null;
            for (ItemFrameEntity detected : allDetectedFrames) {
                if (detected.isRemoved() || !detected.isAlive())
                    continue;
                Vec3d rotationVec = detected.getRotationVector();
                if (rotationVec == null)
                    continue;
                BlockPos detectedSupportPos = Utils
                        .Vec3d2BlockPos(detected.getPos().subtract(rotationVec.normalize()));
                if (detected.getHorizontalFacing() == target.facing &&
                        detectedSupportPos.equals(target.supportPos)) {
                    found = detected;
                    break;
                }
            }
            if (found != null) {
                presentTargets.add(found);
            } else {
                missingTargets.add(target);
            }
        }

        lastActiveFrames = presentTargets.size();
        lastEmptyFrames = 0;
        lastReadyFrames = presentTargets.size();
        lastMissingFrames = missingTargets.size();

        if (Config.INSTANCE.replaceItemFrames) {
            replaceMissingFrames(missingTargets);
        }

        PlayerInventory inv = mc.player.getInventory();
        int maxPlacements = Config.INSTANCE.maxPlacements;
        int maxSwaps = Config.INSTANCE.maxSwaps;

        for (ItemFrameEntity frame : presentTargets) {
            if (placements >= maxPlacements)
                break;

            boolean isAir = frame.getHeldItemStack().isEmpty();
            boolean isDupeItem = dupeItemList.contains(frame.getHeldItemStack().getItem());

            if (isAir) {
                checkAndFillFrame(frame, inv, dupeItemList, maxSwaps);
            } else if (!dontHit.contains(frame) && isDupeItem) {

                long now = System.currentTimeMillis();

                dontHit.add(frame);
                Utils.attackEntity(frame);
                accumulatedDupes++;
                interactionCooldowns.put(frame.getId(), now);

                checkAndFillFrame(frame, inv, dupeItemList, maxSwaps);
            }
        }
    }

    private static void replaceMissingFrames(List<FrameSnapshot> missingTargets) {
        PlayerInventory inv = mc.player.getInventory();
        int maxPlacements = Config.INSTANCE.maxPlacements;
        int maxSwaps = Config.INSTANCE.maxSwaps;

        for (FrameSnapshot missing : missingTargets) {
            if (placements >= maxPlacements)
                break;

            if (inv.getStack(PlayerInventory.OFF_HAND_SLOT).getItem() instanceof ItemFrameItem) {
                placeFrame(PreparedHand.offhand(), missing);
            } else if (inv.getMainHandStack().getItem() instanceof ItemFrameItem) {
                placeFrame(PreparedHand.main(), missing);
            } else {
                if (Config.INSTANCE.silentRoute && swaps < maxSwaps) {
                    int hotbarSlot = findItemInHotbar(inv, ItemFrameItem.class);
                    if (hotbarSlot != -1) {
                        placeFrame(PreparedHand.silentMain(hotbarSlot), missing);
                        swaps++;
                    }
                } else if (swaps < maxSwaps) {
                    attemptSwapToItem(inv, ItemFrameItem.class);
                    if (inv.getMainHandStack().getItem() instanceof ItemFrameItem) {
                        placeFrame(PreparedHand.main(), missing);
                    }
                }
            }
        }
    }

    private static void placeFrame(PreparedHand preparedHand, FrameSnapshot snapshot) {
        Utils.interactBlock(preparedHand.hand(), new BlockHitResult(
                Vec3d.ofCenter(snapshot.supportPos), snapshot.facing, snapshot.supportPos, false),
                preparedHand.silentHotbarSlot());
        placements++;
    }

    private static void checkAndFillFrame(ItemFrameEntity frame, PlayerInventory inv, List<Item> dupeItemList,
            int maxSwapsLimit) {
        PreparedHand dupeHand = prepareDupeItemHand(inv, dupeItemList, maxSwapsLimit, Config.INSTANCE.maxInventoryMoves);
        if (dupeHand != null) {
            interactItemFrame(frame, dupeHand);
            dontHit.remove(frame);
            placements++;
        }
    }

    private static PreparedHand prepareDupeItemHand(PlayerInventory inv, List<Item> dupeItemList, int maxSwapsLimit,
            int maxInventoryMoves) {
        if (dupeItemList.contains(inv.getMainHandStack().getItem())) {
            return PreparedHand.main();
        }

        if (dupeItemList.contains(inv.getStack(PlayerInventory.OFF_HAND_SLOT).getItem())) {
            return PreparedHand.offhand();
        }

        if (swaps < maxSwapsLimit) {
            int hotbarSlot = findItemInHotbar(inv, dupeItemList);
            if (hotbarSlot != -1) {
                swaps++;
                if (Config.INSTANCE.silentRoute) {
                    return PreparedHand.silentMain(hotbarSlot);
                }
                swap(inv, hotbarSlot);
                return PreparedHand.main();
            }
        }

        if (moves < maxInventoryMoves) {
            int upperSlot = findItemInUpperInventory(inv, dupeItemList);
            if (upperSlot != -1) {
                if (Config.INSTANCE.silentRoute) {
                    int hotbarSlot = findEmptyHotbarSlot(inv);
                    if (hotbarSlot == -1) {
                        return null;
                    }
                    Utils.moveUpperSlotToHotbar(upperSlot, hotbarSlot);
                    moves++;
                    return PreparedHand.silentMain(hotbarSlot);
                }

                Utils.moveUpperSlotToHotbar(upperSlot);
                moves++;
                if (dupeItemList.contains(inv.getMainHandStack().getItem())) {
                    return PreparedHand.main();
                }
            }
        }

        return null;
    }

    private static void interactItemFrame(ItemFrameEntity itemFrame, PreparedHand preparedHand) {
        long now = System.currentTimeMillis();
        if (Config.INSTANCE.antiDoubleClick && Config.INSTANCE.mode != Config.Mode.Speed
                && now - interactionCooldowns.getOrDefault(itemFrame.getId(), 0L) < Config.INSTANCE.doubleClickDelay)
            return;

        Utils.interactEntity(itemFrame, preparedHand.hand(), preparedHand.silentHotbarSlot());
        accumulatedDupes++;
        interactionCooldowns.put(itemFrame.getId(), now);
    }

    private static void attemptSwapToItem(PlayerInventory inv, List<Item> items) {
        int slot = findItemInHotbar(inv, items);
        if (slot != -1) {
            swap(inv, slot);
            swaps++;
        }
    }

    private static void attemptSwapToItem(PlayerInventory inv, Class<?> clazz) {
        int slot = findItemInHotbar(inv, clazz);
        if (slot != -1) {
            swap(inv, slot);
            swaps++;
        }
    }

    private static int findItemInHotbar(PlayerInventory inv, List<Item> items) {
        int currentSlot = inv.selectedSlot;
        for (int i = 0; i < 9; i++) {
            if (i == currentSlot)
                continue;
            if (items.contains(inv.getStack(i).getItem()))
                return i;
        }
        return -1;
    }

    private static int findItemInHotbar(PlayerInventory inv, Class<?> clazz) {
        int currentSlot = inv.selectedSlot;
        for (int i = 0; i < 9; i++) {
            if (i == currentSlot)
                continue;
            if (clazz.isInstance(inv.getStack(i).getItem()))
                return i;
        }
        return -1;
    }

    private static int findItemInUpperInventory(PlayerInventory inv, List<Item> items) {
        for (int i = 9; i < 36; i++) {
            if (items.contains(inv.getStack(i).getItem()))
                return i;
        }
        return -1;
    }

    private static int findEmptyHotbarSlot(PlayerInventory inv) {
        int currentSlot = inv.selectedSlot;
        for (int i = 0; i < 9; i++) {
            if (i != currentSlot && inv.getStack(i).isEmpty()) {
                return i;
            }
        }
        return -1;
    }

    private static int countDupeItems(PlayerInventory inv, List<Item> items) {
        int count = 0;
        for (int i = 0; i < 36; i++) {
            count += getDupeStackCount(inv.getStack(i), items);
        }
        count += getDupeStackCount(inv.getStack(PlayerInventory.OFF_HAND_SLOT), items);
        return count;
    }

    private static int getDupeStackCount(ItemStack stack, List<Item> items) {
        if (stack.isEmpty() || !items.contains(stack.getItem())) {
            return 0;
        }
        return stack.getCount();
    }

    private static void swap(PlayerInventory inv, int slot) {
        inv.selectedSlot = slot;
    }

    private static void cleanupDeadEntities() {

        Iterator<ItemFrameEntity> iterator = dontHit.iterator();
        while (iterator.hasNext()) {
            ItemFrameEntity frame = iterator.next();
            if (frame.isRemoved() || !frame.isAlive()) {
                iterator.remove();
            }
        }

        long now = System.currentTimeMillis();
        interactionCooldowns.entrySet().removeIf(entry -> now - entry.getValue() > COOLDOWN_EXPIRY_MS);
    }
}
