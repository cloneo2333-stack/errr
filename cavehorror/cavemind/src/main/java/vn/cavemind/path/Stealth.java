package vn.cavemind.path;

import it.unimi.dsi.fastutil.longs.Long2ByteOpenHashMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.PathNavigationRegion;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.BlockPathTypes;
import net.minecraft.world.level.pathfinder.Node;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.level.pathfinder.PathFinder;
import net.minecraft.world.level.pathfinder.WalkNodeEvaluator;
import net.minecraft.world.phys.Vec3;
import vn.cavemind.sense.Range;
import vn.cavemind.sense.Sight;
import vn.cavemind.sense.Watch;

public final class Stealth {
    public enum Mode { SNEAK, LIGHT, PLAIN }

    public interface Cost {
        float at(int x, int y, int z);
    }

    static final class Eval extends WalkNodeEvaluator {
        private final Cost cost;

        Eval(Cost cost) {
            this.cost = cost;
        }

        @Override
        public void prepare(PathNavigationRegion region, Mob m) {
            super.prepare(region, m);
            entityHeight = Math.max(1, (int) Math.ceil(m.getBbHeight() - 0.05f));
        }

        @Override
        protected Node findAcceptedNode(int x, int y, int z, int step, double floor, Direction dir, BlockPathTypes type) {
            Node n = super.findAcceptedNode(x, y, z, step, floor, dir, type);
            if (n != null && cost != null && n.costMalus >= 0 && n.type != null) {
                float extra = cost.at(n.x, n.y, n.z);
                if (extra > 0) n.costMalus = Math.max(n.costMalus, mob.getPathfindingMalus(n.type) + extra);
            }
            return n;
        }
    }

    static final class Eyes {
        final Vec3 eye;
        final double yaw, range;
        final ServerPlayer p;

        Eyes(ServerPlayer p) {
            this.p = p;
            this.eye = p.getEyePosition();
            this.yaw = Math.toRadians(p.getYRot());
            this.range = Math.min(96, Range.of(p) * 1.1 + 6);
        }
    }

    public static final class SneakCost implements Cost {
        final ServerLevel lvl;
        final List<Eyes> eyes = new ArrayList<>();
        final Long2ByteOpenHashMap cache = new Long2ByteOpenHashMap();
        final boolean day;
        final float seenCost, lightCost;
        int rays;

        public SneakCost(ServerLevel lvl, List<ServerPlayer> watchers, Mode mode) {
            this.lvl = lvl;
            for (ServerPlayer o : watchers) if (!o.isSpectator() && o.isAlive() && o.level() == lvl) eyes.add(new Eyes(o));
            this.day = lvl.isDay() && !lvl.isThundering();
            this.seenCost = mode == Mode.SNEAK ? 10f : 0f;
            this.lightCost = mode == Mode.PLAIN ? 0f : 3f;
            cache.defaultReturnValue((byte) -1);
        }

        @Override
        public float at(int x, int y, int z) {
            float c = 0;
            if (lightCost > 0) {
                BlockPos q = new BlockPos(x, y + 1, z);
                int bl = lvl.getBrightness(LightLayer.BLOCK, q);
                if (bl >= 8) c += lightCost * (bl - 6) / 4f;
                if (day && lvl.getBrightness(LightLayer.SKY, q) >= 13) c += 0.6f;
            }
            if (seenCost > 0 && !eyes.isEmpty()) {
                long k = BlockPos.asLong(x >> 1, y >> 1, z >> 1);
                byte v = cache.get(k);
                if (v < 0) {
                    v = (byte) (exposed(x, y, z) ? 1 : 0);
                    cache.put(k, v);
                }
                if (v == 1) c += seenCost;
                else if (open(x, y, z)) c += 0.5f;
            }
            return c;
        }

        boolean exposed(int x, int y, int z) {
            Vec3 pt = new Vec3(x + 0.5, y + 1.6, z + 0.5);
            for (Eyes e : eyes) {
                double dx = pt.x - e.eye.x, dz = pt.z - e.eye.z, dy = pt.y - e.eye.y;
                double d2 = dx * dx + dz * dz + dy * dy;
                if (d2 > e.range * e.range || d2 < 1) continue;
                double fx = -Math.sin(e.yaw), fz = Math.cos(e.yaw);
                double len = Math.sqrt(dx * dx + dz * dz);
                if (len > 1e-3 && (dx * fx + dz * fz) / len < 0.5) continue;
                if (rays++ > 900) return true;
                if (Sight.clear(lvl, e.eye, pt, e.p)) return true;
            }
            return false;
        }

        boolean open(int x, int y, int z) {
            for (Direction d : Direction.Plane.HORIZONTAL) {
                BlockPos q = new BlockPos(x, y + 1, z).relative(d);
                BlockState st = lvl.getBlockState(q);
                if (st.is(BlockTags.LEAVES) || st.is(BlockTags.LOGS) || !st.getCollisionShape(lvl, q).isEmpty()) return false;
            }
            return true;
        }
    }

    public static Path find(Mob m, BlockPos goal, Mode mode, List<ServerPlayer> watchers, boolean doors) {
        if (!(m.level() instanceof ServerLevel lvl)) return null;
        BlockPos start = m.blockPosition();
        double dist = Math.sqrt(start.distSqr(goal));
        if (dist > 160) return null;
        int margin = 20;
        BlockPos lo = new BlockPos(Math.min(start.getX(), goal.getX()) - margin, Math.min(start.getY(), goal.getY()) - 12, Math.min(start.getZ(), goal.getZ()) - margin);
        BlockPos hi = new BlockPos(Math.max(start.getX(), goal.getX()) + margin, Math.max(start.getY(), goal.getY()) + 12, Math.max(start.getZ(), goal.getZ()) + margin);
        Cost cost = mode == Mode.PLAIN ? null : new SneakCost(lvl, watchers, mode);
        Eval ev = new Eval(cost);
        ev.setCanPassDoors(true);
        ev.setCanOpenDoors(doors);
        ev.setCanFloat(false);
        int nodes = mode == Mode.PLAIN ? 900 : 1400;
        PathFinder pf = new PathFinder(ev, nodes);
        try {
            PathNavigationRegion region = new PathNavigationRegion(lvl, lo, hi);
            return pf.findPath(region, m, Set.of(goal), (float) (dist * 1.9 + 24), 1, 1.0f);
        } catch (Throwable t) {
            return null;
        }
    }

    public static boolean reaches(Path path, BlockPos goal, double tol) {
        if (path == null || path.getNodeCount() == 0) return false;
        Node end = path.getEndNode();
        return end != null && Math.sqrt(end.asBlockPos().distSqr(goal)) <= tol;
    }

    private Stealth() {}
}
