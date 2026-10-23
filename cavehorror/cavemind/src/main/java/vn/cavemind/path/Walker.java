package vn.cavemind.path;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import vn.cavemind.compat.Compat;
import vn.cavemind.sense.Range;
import vn.cavemind.sense.Sight;
import vn.cavemind.sense.Watch;
import vn.cavemind.world.Motion;
import vn.cavemind.world.Spots;

public final class Walker {
    public enum Step { MOVING, ARRIVED, FROZEN, FAILED }

    public BlockPos goal;
    public double speed;
    public Stealth.Mode mode;
    public boolean doors, freezes = true;
    public String gait = "cm_walk";
    public int gaitLen = 52;
    public double arrive = 1.4;
    public boolean frozen;
    public int frozenTicks, hops, fails;
    Path path;
    long nextPath, checkAt, started, lastWatchAt = -10, lastPathAt = -100;
    boolean lastWatch;
    Vec3 checkPos;

    public Walker(BlockPos goal, double speed, Stealth.Mode mode) {
        this.goal = goal;
        this.speed = speed;
        this.mode = mode;
    }

    public Walker gait(String anim, int len) {
        gait = anim;
        gaitLen = len;
        return this;
    }

    public void retarget(BlockPos g) {
        if (g == null) return;
        if (goal == null || g.distSqr(goal) > 6) {
            goal = g;
            path = null;
            nextPath = 0;
        }
    }

    public long age(long now) {
        return started == 0 ? 0 : now - started;
    }

    public static List<ServerPlayer> viewers(Entity e) {
        List<ServerPlayer> out = new ArrayList<>();
        if (!(e.level() instanceof ServerLevel lvl)) return out;
        UUID only = Compat.ONLY_MAP.get(e.getUUID());
        for (ServerPlayer o : lvl.players()) {
            if (o.isSpectator() || !o.isAlive()) continue;
            if (only != null && !only.equals(o.getUUID())) continue;
            if (o.distanceToSqr(e) > 128 * 128) continue;
            out.add(o);
        }
        return out;
    }

    public static boolean watchedBy(ServerPlayer o, Entity e) {
        double d = o.distanceTo(e);
        if (d > Range.of(o) * 1.05 + 4) return false;
        Vec3 c = e.getBoundingBox().getCenter();
        Sight si = Sight.of(o, Watch.of(o), c);
        if (!si.on || si.edge() > 0.78) return false;
        return Sight.visible(o, e) > 0.2;
    }

    public static boolean seenAtAll(List<ServerPlayer> viewers, AABB box) {
        for (ServerPlayer o : viewers) {
            if (o.distanceToSqr(box.getCenter()) > 140 * 140) continue;
            Sight si = Sight.of(o, Watch.of(o), box.getCenter());
            if (si.on && Sight.visible(o, box) > 0) return true;
        }
        return false;
    }

    public boolean watched(Mob m, List<ServerPlayer> viewers, long now) {
        if (now - lastWatchAt < 2) return lastWatch;
        lastWatchAt = now;
        boolean w = false;
        for (ServerPlayer o : viewers) {
            if (watchedBy(o, m)) {
                w = true;
                break;
            }
        }
        lastWatch = w;
        return w;
    }

    public Step tick(Mob m, long now, List<ServerPlayer> viewers) {
        if (started == 0) started = now;
        if (goal == null) return Step.FAILED;
        if (freezes && watched(m, viewers, now)) {
            if (!frozen) {
                frozen = true;
                m.getNavigation().stop();
                m.setDeltaMovement(0, m.getDeltaMovement().y, 0);
                if (gait != null) Motion.stop(m);
            }
            frozenTicks++;
            return Step.FROZEN;
        }
        if (frozen) {
            frozen = false;
            path = null;
            nextPath = 0;
            checkPos = m.position();
            checkAt = now;
        }
        double dx = goal.getX() + 0.5 - m.getX(), dz = goal.getZ() + 0.5 - m.getZ();
        double dh = Math.sqrt(dx * dx + dz * dz);
        if (dh < arrive && Math.abs(goal.getY() - m.getY()) < 2.5) {
            m.getNavigation().stop();
            return Step.ARRIVED;
        }
        if (path == null || now >= nextPath || m.getNavigation().isDone() && now - lastPathAt > 15) {
            lastPathAt = now;
            Stealth.Mode use = fails >= 2 ? Stealth.Mode.PLAIN : mode;
            List<ServerPlayer> eyes = use == Stealth.Mode.SNEAK && m.level() instanceof ServerLevel sl ? new ArrayList<>(sl.players()) : viewers;
            Path np = Stealth.find(m, goal, use, eyes, doors);
            nextPath = now + 50 + m.getRandom().nextInt(25);
            if (np == null || np.getNodeCount() == 0) {
                fails++;
                path = null;
            } else {
                path = np;
                m.getNavigation().moveTo(np, speed);
                if (!Stealth.reaches(np, goal, 3.5)) fails++;
                else if (fails > 0 && use != Stealth.Mode.PLAIN) fails--;
            }
        }
        if (gait != null) Motion.loop(m, gait, gaitLen, now);
        if (doors) openDoor(m);
        if (checkPos == null) {
            checkPos = m.position();
            checkAt = now;
        }
        if (now - checkAt >= 60) {
            if (m.position().distanceTo(checkPos) < 1.0) {
                fails++;
                path = null;
                nextPath = 0;
            }
            checkPos = m.position();
            checkAt = now;
        }
        if (fails >= 4) {
            if (hop(m, viewers)) {
                hops++;
                fails = 2;
                path = null;
                nextPath = 0;
            }
            if (hops > 8 || fails >= 9) return Step.FAILED;
        }
        return Step.MOVING;
    }

    boolean hop(Mob m, List<ServerPlayer> viewers) {
        if (seenAtAll(viewers, m.getBoundingBox())) return false;
        ServerLevel lvl = (ServerLevel) m.level();
        BlockPos to = null;
        if (path != null && !path.isDone()) {
            int i = Math.min(path.getNodeCount() - 1, path.getNextNodeIndex() + 6);
            to = path.getNodePos(i);
        }
        if (to == null || to.distSqr(m.blockPosition()) < 4) {
            Vec3 dir = new Vec3(goal.getX() + 0.5 - m.getX(), 0, goal.getZ() + 0.5 - m.getZ());
            double len = dir.length();
            if (len < 1e-3) return false;
            Vec3 at = m.position().add(dir.scale(Math.min(6, len) / len));
            to = BlockPos.containing(at.x, Mth.lerp(0.5, m.getY(), goal.getY()), at.z);
        }
        int h = Mth.ceil(m.getBbHeight());
        BlockPos feet = Spots.nearFloor(lvl, to, h, 4);
        if (feet == null) return false;
        AABB box = m.getDimensions(m.getPose()).makeBoundingBox(feet.getX() + 0.5, feet.getY(), feet.getZ() + 0.5);
        if (seenAtAll(viewers, box)) return false;
        m.getNavigation().stop();
        m.teleportTo(feet.getX() + 0.5, feet.getY(), feet.getZ() + 0.5);
        return true;
    }

    void openDoor(Mob m) {
        if (path == null || path.isDone()) return;
        ServerLevel lvl = (ServerLevel) m.level();
        BlockPos next = path.getNextNodePos();
        if (next.distSqr(m.blockPosition()) > 5) return;
        for (BlockPos q : new BlockPos[]{next, next.above()}) {
            BlockState st = lvl.getBlockState(q);
            if (st.getBlock() instanceof DoorBlock db && st.is(BlockTags.WOODEN_DOORS) && !st.getValue(DoorBlock.OPEN)) {
                db.setOpen(m, lvl, st, q, true);
                return;
            }
            if (st.getBlock() instanceof FenceGateBlock && !st.getValue(FenceGateBlock.OPEN)) {
                lvl.setBlock(q, st.setValue(FenceGateBlock.OPEN, true), 10);
                return;
            }
        }
    }

    public void stop(Mob m) {
        m.getNavigation().stop();
        path = null;
    }
}
