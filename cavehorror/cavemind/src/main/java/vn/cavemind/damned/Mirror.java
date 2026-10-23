package vn.cavemind.damned;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.Vec3;
import vn.cavemind.compat.Compat;
import vn.cavemind.memory.Profile;
import vn.cavemind.sense.Watch;
import vn.cavemind.world.Fx;
import vn.cavemind.world.Spots;

final class Mirror {
    static boolean setup(ServerPlayer p, Watch w, Haunt.Run r) {
        Spots.Cand c = Spots.find(p, w, new Spots.Spec().ring(24, 36).dir(0, 180).body(Haunt.W, Haunt.H).tries(40)
            .when(k -> !k.sight.on && k.vis >= 0.6 && Math.abs(k.feet.getY() - p.getY()) <= 3)
            .rank(k -> k.vis - Math.abs(k.feet.getY() - p.getY()) * 0.1 + (8 - Math.min(8, k.light)) * 0.03));
        if (c == null) return false;
        Entity e = Haunt.spawn(p, c.feet, p.getEyePosition());
        if (e == null) return false;
        r.body = e.getUUID();
        Compat.anim(e, "cm_still");
        Vec3 p0 = p.position(), d0 = e.position();
        r.planeM = p0.add(d0).scale(0.5);
        Vec3 n = new Vec3(p0.x - d0.x, 0, p0.z - d0.z);
        r.planeN = n.normalize();
        Haunt.fog(p, 0.3f, 200);
        return true;
    }

    static boolean run(ServerPlayer p, Watch w, Profile pr, Haunt.Run r, long now) {
        Entity e = Haunt.body(p, r);
        if (!(e instanceof Mob m)) return false;
        ServerLevel lvl = p.serverLevel();
        boolean on = Haunt.perceive(p, w, r, e);
        double dist = Math.sqrt(e.distanceToSqr(p));
        switch (r.mode) {
            case 0 -> {
                if (!r.frozen) {
                    Vec3 pp = p.position();
                    double d = pp.subtract(r.planeM).dot(r.planeN);
                    Vec3 refl = pp.subtract(r.planeN.scale(2 * d));
                    BlockPos g = Haunt.ground(lvl, refl.x, refl.z, e.getY());
                    if (g == null || !Spots.standable(lvl, g, 2) || Math.abs(g.getY() - e.getY()) > 1.1) {
                        r.frozen = true;
                    } else {
                        double yr = Math.toRadians(p.getYRot());
                        Vec3 f = new Vec3(-Math.sin(yr), 0, Math.cos(yr));
                        Vec3 f2 = f.subtract(r.planeN.scale(2 * f.dot(r.planeN)));
                        float yaw = (float) Math.toDegrees(Math.atan2(-f2.x, f2.z));
                        e.moveTo(refl.x, g.getY(), refl.z, yaw, p.getXRot());
                        m.setYHeadRot(yaw);
                        m.setYBodyRot(yaw);
                        if (w.stepped) Fx.sound(p, "apollyon:step_sound", SoundSource.HOSTILE, e.position(), 0.35f, 0.9f);
                        if (w.speed > 0.02) vn.cavemind.world.Motion.driftAnim(e, now);
                    }
                } else {
                    Haunt.face(m, p.getEyePosition());
                }
                if (r.noticed && on && w.stillTicks > 40 || dist < 12 || r.frozen && r.noticed) {
                    r.mode = 1;
                    r.modeAt = now;
                    Compat.anim(e, "cm_notice");
                }
                if (r.noticed && r.away > 40) {
                    Haunt.advance(p, pr, true);
                    return false;
                }
                if (!r.noticed && now - r.start > 1600 && !on) return false;
                return now - r.start < 2400;
            }
            case 1 -> {
                Haunt.face(m, p.getEyePosition());
                if (r.away >= 4) {
                    Haunt.advance(p, pr, r.noticed);
                    return false;
                }
                if (dist < 5) {
                    Haunt.breath(p, p.getEyePosition().add(p.getLookAngle().scale(0.8)), 0.5f);
                    Haunt.advance(p, pr, true);
                    return false;
                }
                if (now - r.modeAt > 140 && r.stare > 70) {
                    r.mode = 2;
                    r.modeAt = now;
                    r.planeM = e.position().add(e.position().subtract(p.position()).normalize().scale(20));
                }
                return now - r.modeAt < 700;
            }
            default -> {
                vn.cavemind.world.Motion.drift(m, r.planeM, null, 0.3, now);
                if (r.away >= 2 || !on) {
                    Haunt.advance(p, pr, true);
                    return false;
                }
                return now - r.modeAt < 320;
            }
        }
    }

    private Mirror() {}
}
