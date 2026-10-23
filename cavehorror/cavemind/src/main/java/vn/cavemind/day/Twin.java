package vn.cavemind.day;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import vn.cavemind.compat.Compat;
import vn.cavemind.director.Director;
import vn.cavemind.director.Dread;
import vn.cavemind.man.ManMind;
import vn.cavemind.memory.Moment;
import vn.cavemind.memory.Profile;
import vn.cavemind.sense.Watch;
import vn.cavemind.world.Spots;

final class Twin extends Piece {
    Vec3 planeM, planeN;
    int mode;
    long modeAt;

    @Override
    String id() {
        return "twin";
    }

    @Override
    String vi() {
        return "kẻ giống bạn (một dân làng bắt chước mọi cử động của bạn)";
    }

    @Override
    boolean begin(ServerPlayer p, Watch w, Profile pr, long now) {
        if (!p.onGround()) return false;
        Spots.Cand c = Spots.find(p, w, new Spots.Spec().ring(26, 36).dir(0, 180).body(0.6, 1.95).tries(40)
            .when(k -> !k.sight.on && k.vis >= 0.6 && Math.abs(k.feet.getY() - p.getY()) <= 3).rank(k -> k.vis));
        if (c == null) return false;
        ServerLevel lvl = p.serverLevel();
        Villager v = EntityType.VILLAGER.create(lvl);
        if (v == null) return false;
        v.moveTo(c.feet.getX() + 0.5, c.feet.getY(), c.feet.getZ() + 0.5, 0, 0);
        v.setVillagerData(v.getVillagerData().setProfession(VillagerProfession.NITWIT));
        v.setNoAi(true);
        v.setSilent(true);
        v.setInvulnerable(true);
        v.setPersistenceRequired();
        Compat.mark(v, "twin", p.getUUID(), true);
        if (!lvl.addFreshEntity(v)) return false;
        body = v.getUUID();
        Vec3 p0 = p.position(), d0 = v.position();
        planeM = p0.add(d0).scale(0.5);
        planeN = new Vec3(p0.x - d0.x, 0, p0.z - d0.z).normalize();
        return true;
    }

    @Override
    boolean tick(ServerPlayer p, Watch w, Profile pr, long now) {
        Entity e = body(p);
        if (!(e instanceof Villager v)) return false;
        ServerLevel lvl = p.serverLevel();
        boolean was = noticed;
        boolean on = watch(p, w, e);
        double dist = Math.sqrt(e.distanceToSqr(p));
        if (noticed && !was) {
            Director.scare(p, 6);
            pr.moment(new Moment(Moment.TWIN, Dread.day(lvl), Dread.time(lvl), p.blockPosition(), (int) dist, 0));
        }
        switch (mode) {
            case 0 -> {
                Vec3 pp = p.position();
                double d = pp.subtract(planeM).dot(planeN);
                Vec3 r = pp.subtract(planeN.scale(2 * d));
                int y = lvl.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, (int) Math.floor(r.x), (int) Math.floor(r.z));
                BlockPos g = new BlockPos((int) Math.floor(r.x), y, (int) Math.floor(r.z));
                if (Math.abs(y - e.getY()) <= 1.1 && Spots.standable(lvl, g, 2)) {
                    double yr = Math.toRadians(p.getYRot());
                    Vec3 f = new Vec3(-Math.sin(yr), 0, Math.cos(yr));
                    Vec3 f2 = f.subtract(planeN.scale(2 * f.dot(planeN)));
                    float yaw = (float) Math.toDegrees(Math.atan2(-f2.x, f2.z));
                    e.moveTo(r.x, y + Math.max(0, p.getY() - lvl.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, p.getBlockX(), p.getBlockZ())), r.z, yaw, p.getXRot());
                    v.setYHeadRot(yaw);
                    v.setYBodyRot(yaw);
                }
                if (noticed && stare > 80 || dist < 11) {
                    mode = 1;
                    modeAt = now;
                }
                if (noticed && away > 60) return false;
                return now - start < 3600;
            }
            case 1 -> {
                ManMind.face(v, p.getEyePosition());
                if (dist < 9) {
                    v.setNoAi(false);
                    Vec3 aw = e.position().add(e.position().subtract(p.position()).normalize().scale(24));
                    v.getNavigation().moveTo(aw.x, aw.y, aw.z, 0.6);
                    mode = 2;
                    modeAt = now;
                }
                if (away > 40) return false;
                return now - modeAt < 1200;
            }
            default -> {
                if (!on && now - modeAt > 30) return false;
                return now - modeAt < 400;
            }
        }
    }
}
