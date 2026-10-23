package vn.cavemind.damned;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Rotations;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import vn.cavemind.compat.Compat;
import vn.cavemind.sense.Watch;
import vn.cavemind.world.Spots;

public final class Kids {
    static final int[] SHADE = {0x4a4c54, 0x55565c, 0x3e4048};

    static List<UUID> hide(ServerPlayer p, Watch w, int n) {
        List<UUID> out = new ArrayList<>();
        List<Vec3> used = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            Spots.Cand c = Spots.find(p, w, new Spots.Spec().ring(13, 22).dir(0, 180).body(0.5, 1.0).cave(w.underground).tries(60)
                .when(k -> !k.sight.on && k.vis >= 0.25 && Math.abs(k.feet.getY() - p.getY()) < 6 && used.stream().noneMatch(u -> u.distanceTo(k.center) < 9))
                .rank(k -> (k.trunk ? 0.8 : 0) + (k.cover ? 0.5 : 0) + k.vis * 0.3));
            if (c == null) c = Spots.find(p, w, new Spots.Spec().ring(8, 26).dir(0, 180).body(0.5, 1.0).cave(w.underground).tries(80)
                .when(k -> !k.sight.on && Math.abs(k.feet.getY() - p.getY()) < 8 && used.stream().noneMatch(u -> u.distanceTo(k.center) < 7)).rank(k -> k.vis));
            if (c == null) break;
            ArmorStand a = make(p, c.feet, p.position(), i);
            if (a == null) break;
            used.add(c.center);
            out.add(a.getUUID());
        }
        return out;
    }

    static ArmorStand make(ServerPlayer p, BlockPos feet, Vec3 away, int i) {
        ServerLevel lvl = p.serverLevel();
        net.minecraft.nbt.CompoundTag t = new net.minecraft.nbt.CompoundTag();
        t.putString("id", "minecraft:armor_stand");
        t.putBoolean("Small", true);
        if (!(EntityType.create(t, lvl).orElse(null) instanceof ArmorStand as)) return null;
        float yaw = Haunt.yawTo(Vec3.atBottomCenterOf(feet), away) + 180f + (p.getRandom().nextFloat() - 0.5f) * 50f;
        as.moveTo(feet.getX() + 0.5, feet.getY(), feet.getZ() + 0.5, yaw, 0);
        as.setYHeadRot(yaw);
        as.setYBodyRot(yaw);
        as.setInvisible(true);
        as.setShowArms(true);
        as.setNoBasePlate(true);
        as.setInvulnerable(true);
        as.setHeadPose(new Rotations(32f, 0f, (i % 2 == 0 ? 10f : -12f)));
        as.setRightArmPose(new Rotations(-70f, -22f, 0f));
        as.setLeftArmPose(new Rotations(-70f, 22f, 0f));
        as.setRightLegPose(new Rotations(-60f, 0f, 0f));
        as.setLeftLegPose(new Rotations(-60f, 0f, 0f));
        int col = SHADE[i % SHADE.length];
        as.setItemSlot(EquipmentSlot.HEAD, Effigy.dyed(Items.LEATHER_HELMET, col));
        as.setItemSlot(EquipmentSlot.CHEST, Effigy.dyed(Items.LEATHER_CHESTPLATE, col));
        as.setItemSlot(EquipmentSlot.LEGS, Effigy.dyed(Items.LEATHER_LEGGINGS, col));
        as.setItemSlot(EquipmentSlot.FEET, Effigy.dyed(Items.LEATHER_BOOTS, col));
        Compat.mark(as, "damned_kid", p.getUUID(), true);
        if (!lvl.addFreshEntity(as)) return null;
        return as;
    }

    public static ArmorStand test(ServerPlayer p, BlockPos feet) {
        return make(p, feet, p.position(), 0);
    }

    static Entity get(ServerLevel lvl, UUID id) {
        Entity e = lvl.getEntity(id);
        return e != null && e.isAlive() ? e : null;
    }

    static void gone(ServerLevel lvl, Entity e, boolean ink) {
        AABB box = e.getBoundingBox();
        Vec3 c = box.getCenter();
        if (ink) lvl.sendParticles(ParticleTypes.SQUID_INK, c.x, c.y, c.z, 26, 0.2, 0.3, 0.2, 0.02);
        e.discard();
    }

    static void clear(ServerLevel lvl, List<UUID> kids) {
        for (UUID id : kids) {
            Entity e = get(lvl, id);
            if (e != null) e.discard();
        }
        kids.clear();
    }

    private Kids() {}
}
