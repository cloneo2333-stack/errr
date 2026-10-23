package vn.cavemind.day;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import vn.cavemind.compat.Compat;
import vn.cavemind.director.Director;
import vn.cavemind.director.Dread;
import vn.cavemind.man.ManMind;
import vn.cavemind.memory.Moment;
import vn.cavemind.memory.Profile;
import vn.cavemind.sense.Sight;
import vn.cavemind.sense.Watch;
import vn.cavemind.world.Fx;
import vn.cavemind.world.Spots;

final class Field extends Piece {
    long nextCreep;
    int creeps;
    boolean leaving;

    @Override
    String id() {
        return "field";
    }

    @Override
    String vi() {
        return "người đứng giữa đồng (giữa ban ngày, không nấp, không biến mất khi bạn rời mắt)";
    }

    @Override
    boolean begin(ServerPlayer p, Watch w, Profile pr, long now) {
        Spots.Cand c = Spots.find(p, w, new Spots.Spec().ring(85, 135).dir(0, 180).body(0.6, 3.0).tries(60)
            .when(k -> !k.sight.on && k.vis >= 0.7 && !k.cover)
            .rank(k -> k.vis + (k.feet.getY() >= p.getY() ? 0.2 : 0)));
        if (c == null) {
            c = Spots.find(p, w, new Spots.Spec().ring(70, 120).dir(0, 180).body(0.6, 3.0).tries(50)
                .when(k -> !k.sight.on && k.vis >= 0.5).rank(k -> k.vis));
        }
        if (c == null) return false;
        Entity e = ManMind.make(p, Compat.MAN_STALKER, c.feet, true);
        if (e == null) return false;
        e.getPersistentData().putString(Compat.TAG, "man_day");
        body = e.getUUID();
        nextCreep = now + 600;
        return true;
    }

    @Override
    boolean tick(ServerPlayer p, Watch w, Profile pr, long now) {
        Entity e = body(p);
        if (!(e instanceof Mob m)) return false;
        ManMind.face(m, p.getEyePosition());
        boolean was = noticed;
        boolean on = watch(p, w, e);
        double dist = Math.sqrt(e.distanceToSqr(p));
        ServerLevel lvl = p.serverLevel();
        if (noticed && !was) {
            Director.scare(p, 8);
            pr.moment(new Moment(Moment.SEEN, Dread.day(lvl), Dread.time(lvl), p.blockPosition(), (int) dist, 90));
            if (p.getRandom().nextFloat() < 0.6f) Fx.stare(p, e, null, 400, 36);
        }
        if (dist < 40 || leaving) {
            leaving = true;
            if (away >= 3) {
                leaveToken(lvl, e);
                return false;
            }
            return now - start < 12000;
        }
        if (!on && now >= nextCreep && away > 100 && creeps < 6 && dist > 30) {
            Vec3 toward = p.position().subtract(e.position()).normalize();
            Vec3 goal = e.position().add(toward.scale(Math.min(18, dist - 26)));
            BlockPos feet = Spots.surface(lvl, (int) Math.floor(goal.x), (int) Math.floor(goal.z), 3);
            if (feet != null) {
                Spots.Cand k = Spots.eval(p, w, feet, 0.6, 3.0);
                if (!k.sight.on && k.vis >= 0.4) {
                    e.teleportTo(feet.getX() + 0.5, feet.getY(), feet.getZ() + 0.5);
                    creeps++;
                }
            }
            nextCreep = now + 500 + p.getRandom().nextInt(500);
        }
        long life = noticed ? 9000 : 6000;
        if (now - start > life && !on) return false;
        return now - start < life + 3000;
    }

    static void leaveToken(ServerLevel lvl, Entity e) {
        ItemEntity it = new ItemEntity(lvl, e.getX(), e.getY() + 0.1, e.getZ(), new ItemStack(Items.WITHER_ROSE));
        it.setDeltaMovement(Vec3.ZERO);
        it.setExtendedLifetime();
        lvl.addFreshEntity(it);
    }

    static boolean seen(ServerPlayer p, Watch w, Vec3 at) {
        return Sight.of(p, w, at).on;
    }
}
