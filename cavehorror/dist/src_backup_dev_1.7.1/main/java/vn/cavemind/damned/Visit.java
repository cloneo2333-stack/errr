package vn.cavemind.damned;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.phys.Vec3;
import vn.cavemind.compat.Compat;
import vn.cavemind.director.Director;
import vn.cavemind.memory.Profile;
import vn.cavemind.net.Net;
import vn.cavemind.sense.Watch;
import vn.cavemind.world.Spots;

final class Visit {
    static boolean setup(ServerPlayer p, Watch w, Haunt.Run r) {
        ServerLevel lvl = p.serverLevel();
        var bedPos = p.getSleepingPos();
        if (bedPos.isEmpty()) return false;
        BlockState st = lvl.getBlockState(bedPos.get());
        if (!(st.getBlock() instanceof BedBlock)) return false;
        Direction toHead = st.getValue(BedBlock.FACING);
        BlockPos head = st.getValue(BedBlock.PART) == BedPart.HEAD ? bedPos.get() : bedPos.get().relative(toHead);
        BlockPos foot = head.relative(toHead.getOpposite());
        BlockPos[] tries = {
            foot.relative(toHead.getOpposite()),
            foot.relative(toHead.getClockWise()),
            foot.relative(toHead.getCounterClockWise()),
            head.relative(toHead.getClockWise()),
            head.relative(toHead.getCounterClockWise())
        };
        for (BlockPos t : tries) {
            BlockPos feet = Spots.nearFloor(lvl, t, 2, 1);
            if (feet == null) continue;
            Entity e = Haunt.spawn(p, feet, Vec3.atCenterOf(head));
            if (e == null) continue;
            r.body = e.getUUID();
            r.nextBreath = r.start + 18;
            r.wake = p.getRandom().nextFloat() < 0.6f;
            return true;
        }
        return false;
    }

    static boolean run(ServerPlayer p, Watch w, Profile pr, Haunt.Run r, long now) {
        Entity e = Haunt.body(p, r);
        if (!(e instanceof Mob m)) return false;
        long t = now - r.start;
        Vec3 eye = p.getEyePosition();
        Haunt.face(m, eye);
        if (r.mode == 0) {
            if (t == 2) Compat.anim(e, Haunt.ANIM_FOUND);
            if (now >= r.nextBreath) {
                Haunt.breath(p, e.getEyePosition(), 0.65f);
                r.nextBreath = now + 34 + p.getRandom().nextInt(16);
            }
            if (t % 22 == 0) Haunt.heartbeat(p, 0.5f + t / 200f);
            if (!p.isSleeping()) {
                r.mode = 1;
                r.modeAt = now;
                Director.scare(p, 25);
                return true;
            }
            if (r.wake && p.getSleepTimer() >= 92) {
                p.stopSleepInBed(true, true);
                Net.fx(p, Net.SHAKE, 1.6f, 16);
                r.mode = 1;
                r.modeAt = now;
                Director.scare(p, 30);
                return true;
            }
            if (!r.wake && p.getSleepTimer() >= 96) {
                Haunt.advance(p, pr, true);
                return false;
            }
            return t < 400;
        }
        boolean on = Haunt.perceive(p, w, r, e);
        if (on && !r.noticed) r.noticed = true;
        if (now - r.modeAt > 8 && r.away >= 3) {
            Haunt.advance(p, pr, true);
            return false;
        }
        if (e.distanceToSqr(p) < 2.2) {
            Haunt.breath(p, eye, 0.7f);
            Haunt.advance(p, pr, true);
            return false;
        }
        return now - r.modeAt < 300;
    }

    static boolean setupCorner(ServerPlayer p, Watch w, Haunt.Run r) {
        ServerLevel lvl = p.serverLevel();
        BlockPos at = p.blockPosition();
        BlockPos best = null;
        Direction bestA = null, bestB = null;
        double bd = 0;
        for (int dx = -6; dx <= 6; dx++) {
            for (int dz = -6; dz <= 6; dz++) {
                for (int dy = -1; dy <= 1; dy++) {
                    BlockPos q = at.offset(dx, dy, dz);
                    if (!Spots.standable(lvl, q, 2) || lvl.canSeeSky(q.above())) continue;
                    for (Direction a : Direction.Plane.HORIZONTAL) {
                        Direction b = a.getClockWise();
                        if (solid(lvl, q.above().relative(a)) && solid(lvl, q.above().relative(b)) && solid(lvl, q.relative(a)) && solid(lvl, q.relative(b))) {
                            double d = q.distSqr(at);
                            if (d > 4 && d > bd && vn.cavemind.sense.Sight.visible(p, vn.cavemind.sense.Sight.body(q, 0.6, 1.8)) > 0.4) {
                                bd = d;
                                best = q;
                                bestA = a;
                                bestB = b;
                            }
                        }
                    }
                }
            }
        }
        if (best == null) return false;
        Vec3 corner = Vec3.atBottomCenterOf(best).add(bestA.getStepX() + bestB.getStepX(), 1.5, bestA.getStepZ() + bestB.getStepZ());
        Entity e = Haunt.spawn(p, best, corner);
        if (e == null) return false;
        r.body = e.getUUID();
        r.mode = 0;
        r.planeM = corner;
        return true;
    }

    static boolean corner(ServerPlayer p, Watch w, Profile pr, Haunt.Run r, long now) {
        Entity e = Haunt.body(p, r);
        if (!(e instanceof Mob m)) return false;
        boolean on = Haunt.perceive(p, w, r, e);
        double d2 = e.distanceToSqr(p);
        if (r.mode == 0) {
            Haunt.face(m, r.planeM);
            if (d2 < 6.5 && on) {
                r.mode = 1;
                r.modeAt = now;
                Haunt.face(m, p.getEyePosition());
                Haunt.breath(p, e.getEyePosition(), 0.55f);
                return true;
            }
            if (r.noticed && r.away >= 6) {
                Haunt.advance(p, pr, true);
                return false;
            }
            return now - r.start < 1400;
        }
        Haunt.face(m, p.getEyePosition());
        if (r.away >= 2 || now - r.modeAt > 60) {
            Haunt.advance(p, pr, true);
            return false;
        }
        return true;
    }

    private static boolean solid(ServerLevel lvl, BlockPos q) {
        return lvl.getBlockState(q).isSolidRender(lvl, q);
    }

    private Visit() {}
}
