package vn.cavemind.damned;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.Vec3;
import vn.cavemind.CaveMind;
import vn.cavemind.compat.Compat;
import vn.cavemind.memory.Profile;
import vn.cavemind.sense.Sight;
import vn.cavemind.sense.Watch;
import vn.cavemind.world.Spots;

final class Beckon {
    static boolean setup(ServerPlayer p, Watch w, Profile pr, Haunt.Run r) {
        if (pr.dShrine == null) return false;
        Vec3 shrine = Vec3.atBottomCenterOf(pr.dShrine);
        if (shrine.distanceTo(p.position()) > 260) return false;
        double rel = Mth.wrapDegrees(Haunt.yawTo(p.position(), shrine) - p.getYRot());
        Spots.Cand c = Spots.find(p, w, new Spots.Spec().ring(18, 26).dir(rel, 40).body(Haunt.W, Haunt.H).tries(40)
            .when(k -> !k.sight.on && k.vis >= 0.5).rank(k -> k.vis));
        if (c == null) return false;
        Entity e = Haunt.spawn(p, c.feet, p.getEyePosition());
        if (e == null) return false;
        r.body = e.getUUID();
        r.target = pr.dShrine;
        Haunt.fog(p, 0.35f, 200);
        return true;
    }

    static boolean run(ServerPlayer p, Watch w, Profile pr, Haunt.Run r, long now) {
        Entity e = Haunt.body(p, r);
        if (!(e instanceof Mob m) || pr.dShrine == null) return false;
        ServerLevel lvl = p.serverLevel();
        boolean on = Haunt.perceive(p, w, r, e);
        double dist = Math.sqrt(e.distanceToSqr(p));
        Vec3 shrine = Vec3.atBottomCenterOf(pr.dShrine);
        double toShrine = e.position().distanceTo(shrine);
        if (!r.noticed) {
            Haunt.still(m, p.getEyePosition(), on);
            if (now - r.start > 1800 && !on) return false;
            return now - r.start < 2600;
        }
        if (r.mode == 0) {
            r.mode = 1;
            r.modeAt = now;
            Compat.anim(e, Haunt.ANIM_SPOTTED);
            return true;
        }
        if (r.mode == 1) {
            Haunt.still(m, p.getEyePosition(), on);
            if (now - r.modeAt > 70) {
                Compat.anim(e, "empty");
                r.mode = 2;
                r.modeAt = now;
            }
            return true;
        }
        if (toShrine < 3.5) {
            if (r.mode != 4) {
                r.mode = 4;
                r.modeAt = now;
                m.getNavigation().stop();
            }
            Haunt.still(m, p.getEyePosition(), on);
            if (dist < 18 && r.lit < 99 && now % 6 == 0) {
                if (Shrine.lightOne(lvl, pr.dShrine)) {
                    r.lit++;
                    if (r.lit == 1) Haunt.breath(p, e.getEyePosition(), 0.45f);
                } else {
                    r.lit = 99;
                    r.modeAt = now;
                    Haunt.heartbeat(p, 0.7f);
                }
            }
            if (r.lit == 99 && (r.away >= 2 || dist < 3 || now - r.modeAt > 600)) {
                pr.dShrineFound = true;
                Shrine.approach(p, pr);
                Haunt.advance(p, pr, true);
                CaveMind.debug("{} followed it to the shrine", p.getGameProfile().getName());
                return false;
            }
            return now - r.modeAt < 2400;
        }
        if (dist < 6) {
            m.getNavigation().stop();
            if (r.away >= 2 || dist < 2.5) {
                if (dist < 2.5) Haunt.breath(p, p.getEyePosition(), 0.6f);
                Vec3 dir = shrine.subtract(e.position()).normalize();
                Vec3 ahead = e.position().add(dir.scale(Math.min(toShrine - 2, 13 + p.getRandom().nextInt(3))));
                BlockPos g = Haunt.ground(lvl, ahead.x, ahead.z, e.getY());
                if (g != null) {
                    BlockPos tp = Spots.nearFloor(lvl, g, 2, 3);
                    if (tp != null && Haunt.respawn(p, r, tp, p.getEyePosition()) != null) {
                        r.mode = 2;
                        r.modeAt = now;
                        return true;
                    }
                }
            }
            Haunt.still(m, p.getEyePosition(), on);
            return true;
        }
        boolean following = dist < 18;
        if (r.mode == 2) {
            if (following || now - r.modeAt > 200) {
                Vec3 dir = shrine.subtract(e.position());
                double step = Math.min(12, dir.length());
                Vec3 goal = e.position().add(dir.normalize().scale(step));
                BlockPos g = Haunt.ground(lvl, goal.x, goal.z, e.getY());
                if (g == null) g = BlockPos.containing(goal);
                BlockPos tp = Spots.nearFloor(lvl, g, 2, 4);
                r.target = tp != null ? tp : g;
                r.mode = 3;
                r.modeAt = now;
                r.lastLead = now;
            } else {
                Haunt.still(m, p.getEyePosition(), on);
            }
        } else if (r.mode == 3) {
            if (!on && r.away >= 8 && r.target != null && !r.target.equals(pr.dShrine)) {
                Vec3 at = Vec3.atBottomCenterOf(r.target);
                boolean wouldSee = Sight.of(p, w, at.add(0, 1, 0)).on && Sight.clear(lvl, p.getEyePosition(), at.add(0, 1, 0), p);
                if (!wouldSee && Spots.standable(lvl, r.target, 2) && Haunt.respawn(p, r, r.target, p.getEyePosition()) != null) {
                    r.mode = 2;
                    r.modeAt = now;
                    return true;
                }
            }
            if (dist > 26 || now - r.modeAt > 400) {
                r.mode = 2;
                r.modeAt = now;
            }
        }
        if (dist > 70) return false;
        if (r.away > 600) return false;
        return now - r.start < 12000;
    }

    static boolean seen(ServerPlayer p, Watch w, Entity e) {
        return Sight.of(p, w, e.getBoundingBox().getCenter()).on && Sight.visible(p, e) > 0;
    }

    private Beckon() {}
}
