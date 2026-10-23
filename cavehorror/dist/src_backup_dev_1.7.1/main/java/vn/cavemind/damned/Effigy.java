package vn.cavemind.damned;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Rotations;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.item.DyeableLeatherItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import vn.cavemind.CaveMind;
import vn.cavemind.compat.Compat;
import vn.cavemind.director.Dread;
import vn.cavemind.memory.Profile;
import vn.cavemind.sense.Sight;
import vn.cavemind.sense.Watch;
import vn.cavemind.world.Spots;

public final class Effigy {
    private static final Map<UUID, UUID> MORNING = new HashMap<>();

    public static ArmorStand make(ServerPlayer p, BlockPos feet, Vec3 face, String role) {
        ServerLevel lvl = p.serverLevel();
        ArmorStand as = EntityType.ARMOR_STAND.create(lvl);
        if (as == null) return null;
        float yaw = Haunt.yawTo(Vec3.atBottomCenterOf(feet), face);
        as.moveTo(feet.getX() + 0.5, feet.getY(), feet.getZ() + 0.5, yaw, 0);
        as.setYHeadRot(yaw);
        as.setYBodyRot(yaw);
        as.setShowArms(true);
        as.setNoBasePlate(true);
        as.setHeadPose(new Rotations(18f, 0f, 14f));
        as.setRightArmPose(new Rotations(-12f, 0f, 4f));
        as.setLeftArmPose(new Rotations(-8f, 0f, -4f));
        ItemStack head = new ItemStack(Items.PLAYER_HEAD);
        head.getOrCreateTag().put("SkullOwner", NbtUtils.writeGameProfile(new CompoundTag(), p.getGameProfile()));
        as.setItemSlot(EquipmentSlot.HEAD, head);
        as.setItemSlot(EquipmentSlot.CHEST, dyed(Items.LEATHER_CHESTPLATE, 0x34302c));
        as.setItemSlot(EquipmentSlot.LEGS, dyed(Items.LEATHER_LEGGINGS, 0x24242c));
        as.setItemSlot(EquipmentSlot.FEET, dyed(Items.LEATHER_BOOTS, 0x1c1a18));
        Compat.mark(as, role, p.getUUID(), false);
        as.getPersistentData().putLong("cavemind_day", Dread.day(lvl));
        if (!lvl.addFreshEntity(as)) return null;
        return as;
    }

    static ItemStack dyed(net.minecraft.world.item.Item item, int rgb) {
        ItemStack s = new ItemStack(item);
        if (item instanceof DyeableLeatherItem d) d.setColor(s, rgb);
        s.getOrCreateTag().putBoolean("cavemind", true);
        return s;
    }

    static void tick(ServerPlayer p, Profile pr, int tod) {
        ServerLevel lvl = p.serverLevel();
        UUID id = MORNING.get(p.getUUID());
        if (id != null && tod > 11500 && tod < 13000) {
            Entity e = lvl.getEntity(id);
            if (e != null) e.discard();
            MORNING.remove(p.getUUID());
        }
        if (pr.dStage < 2 || pr.home == null || id != null) return;
        long day = Dread.day(lvl);
        if (pr.dEffigyDay == day || !(tod >= 23200 || tod < 600)) return;
        if (!(p.isSleeping() || Watch.of(p).indoors)) return;
        if (pr.home.distSqr(p.blockPosition()) > 30 * 30) return;
        pr.dEffigyDay = day;
        if (p.getRandom().nextFloat() > 0.4f) return;
        Watch w = Watch.of(p);
        for (int i = 0; i < 30; i++) {
            double a = p.getRandom().nextDouble() * Math.PI * 2;
            double d = 7 + p.getRandom().nextDouble() * 6;
            int x = (int) Math.floor(pr.home.getX() + Math.cos(a) * d), z = (int) Math.floor(pr.home.getZ() + Math.sin(a) * d);
            BlockPos feet = Spots.surface(lvl, x, z, 2);
            if (feet == null || Math.abs(feet.getY() - pr.home.getY()) > 4 || !lvl.canSeeSky(feet.above())) continue;
            if (Sight.of(p, w, Vec3.atCenterOf(feet.above())).on) continue;
            ArmorStand as = make(p, feet, Vec3.atCenterOf(pr.home), "effigy_morning");
            if (as != null) {
                MORNING.put(p.getUUID(), as.getUUID());
                CaveMind.debug("an effigy of {} waits outside at {}", p.getGameProfile().getName(), feet);
                vn.cavemind.dev.Dev.say(p, "Hình nộm mang đầu bạn đứng ngoài nhà tại " + feet.getX() + " " + feet.getY() + " " + feet.getZ());
            }
            return;
        }
    }

    static void remove(ServerPlayer p, Profile pr) {
        UUID id = MORNING.remove(p.getUUID());
        if (id != null) {
            Entity e = p.serverLevel().getEntity(id);
            if (e != null) e.discard();
        }
    }

    public static void clear() {
        MORNING.clear();
    }

    private Effigy() {}
}
