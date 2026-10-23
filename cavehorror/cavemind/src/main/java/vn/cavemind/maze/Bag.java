package vn.cavemind.maze;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import vn.cavemind.CaveMind;
import vn.cavemind.memory.MindData;
import vn.cavemind.memory.Profile;

// Everything the player carried waits in the profile while they are below; only the camera kit goes down the well.
final class Bag {
    static final String KIT = "cavemind_kit";
    static final int RECORDER_SLOT = 7, MARBLE_SLOT = 8;

    static boolean kit(ItemStack st) {
        return !st.isEmpty() && st.hasTag() && st.getTag().getBoolean(KIT);
    }

    static void stash(ServerPlayer p) {
        Profile pr = MindData.profile(p);
        if (pr.mazeBag != null) restore(p);
        Inventory inv = p.getInventory();
        ListTag bag = new ListTag();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack st = inv.getItem(i);
            if (st.isEmpty()) continue;
            CompoundTag t = new CompoundTag();
            t.putInt("Slot", i);
            st.save(t);
            bag.add(t);
        }
        ItemStack carried = p.containerMenu.getCarried();
        if (!carried.isEmpty()) {
            CompoundTag t = new CompoundTag();
            t.putInt("Slot", -1);
            carried.save(t);
            bag.add(t);
            p.containerMenu.setCarried(ItemStack.EMPTY);
        }
        pr.mazeBag = bag;
        inv.clearContent();
        MindData.dirty(p.server);
        give(p);
        p.inventoryMenu.broadcastChanges();
        p.displayClientMessage(Component.literal("Đồ đạc của bạn ở lại trên kia. Dưới này chỉ có máy quay, máy thu âm và hai viên bi.").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC), false);
        CaveMind.debug("stashed {} stacks of {} before the maze", bag.size(), p.getGameProfile().getName());
    }

    static void give(ServerPlayer p) {
        Inventory inv = p.getInventory();
        ItemStack rec = new ItemStack(vn.cavemind.gear.Gear.LISTEN.get());
        rec.getOrCreateTag().putBoolean(KIT, true);
        ItemStack bi = new ItemStack(vn.cavemind.gear.Gear.DECOY.get(), 2);
        bi.getOrCreateTag().putBoolean(KIT, true);
        inv.setItem(RECORDER_SLOT, rec);
        inv.setItem(MARBLE_SLOT, bi);
    }

    static boolean holding(ServerPlayer p) {
        return MindData.profile(p).mazeBag != null;
    }

    static void restore(ServerPlayer p) {
        Profile pr = MindData.profile(p);
        ListTag bag = pr.mazeBag;
        if (bag == null) return;
        Inventory inv = p.getInventory();
        List<ItemStack> extra = new ArrayList<>();
        boolean bookBack = false;
        for (int i = 0; i < bag.size(); i++) if (vn.cavemind.chapter.Book.is(ItemStack.of(bag.getCompound(i)))) bookBack = true;
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack st = inv.getItem(i);
            if (st.isEmpty() || kit(st) || st.hasTag() && st.getTag().contains(Maze.TOY) || bookBack && vn.cavemind.chapter.Book.is(st)) continue;
            extra.add(st.copy());
        }
        ItemStack carried = p.containerMenu.getCarried();
        if (kit(carried)) p.containerMenu.setCarried(ItemStack.EMPTY);
        inv.clearContent();
        for (int i = 0; i < bag.size(); i++) {
            CompoundTag t = bag.getCompound(i);
            ItemStack st = ItemStack.of(t);
            if (st.isEmpty()) continue;
            int slot = t.getInt("Slot");
            if (slot >= 0 && slot < inv.getContainerSize() && inv.getItem(slot).isEmpty()) inv.setItem(slot, st);
            else extra.add(st);
        }
        for (ItemStack st : extra) if (!inv.add(st)) p.drop(st, false);
        pr.mazeBag = null;
        MindData.dirty(p.server);
        p.inventoryMenu.broadcastChanges();
        p.displayClientMessage(Component.literal("Đồ đạc của bạn đã về đúng chỗ cũ trong túi.").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC), false);
    }

    private Bag() {}
}
