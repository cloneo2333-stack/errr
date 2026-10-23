package vn.cavemind.world;

import java.util.function.Predicate;
import java.util.function.ToDoubleFunction;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import vn.cavemind.sense.Sight;
import vn.cavemind.sense.Watch;

public final class Spots {
    public static final class Cand {
        public BlockPos feet;
        public Vec3 center;
        public AABB box;
        public double dist, vis, rel;
        public Sight sight;
        public int light;
        public boolean cover, trunk;
        public double score;
        public BlockPos aux;
    }

    public static final class Spec {
        public double rMin = 20, rMax = 40;
        public double ang = 180, spread = 180;
        public double width = 0.6, height = 3.0;
        public int samples = 36;
        public boolean cave;
        public boolean needVis;
        public Predicate<Cand> ok = c -> true;
        public ToDoubleFunction<Cand> score = c -> 0;

        public Spec ring(double a, double b) {
            rMin = a;
            rMax = b;
            return this;
        }

        public Spec dir(double a, double s) {
            ang = a;
            spread = s;
            return this;
        }

        public Spec body(double w, double h) {
            width = w;
            height = h;
            return this;
        }

        public Spec cave(boolean c) {
            cave = c;
            return this;
        }

        public Spec when(Predicate<Cand> p) {
            ok = p;
            return this;
        }

        public Spec rank(ToDoubleFunction<Cand> f) {
            score = f;
            return this;
        }

        public Spec tries(int n) {
            samples = n;
            return this;
        }
    }

    public static Cand find(ServerPlayer p, Watch w, Spec s) {
        ServerLevel lvl = p.serverLevel();
        RandomSource r = p.getRandom();
        Cand best = null;
        int h = Mth.ceil(s.height);
        int ground = 0, onScreen = 0, visSum = 0, bright = 0;
        double sight = vn.cavemind.sense.Range.of(p, w), rMin = s.rMin, rMax = s.rMax;
        if (rMax > sight * 0.9) {
            rMin = Math.min(s.rMin, sight * 0.5);
            rMax = Math.max(rMin + 6, sight * 0.9);
        }
        for (int i = 0; i < s.samples; i++) {
            double a = Math.toRadians(p.getYRot() + s.ang + (r.nextDouble() * 2 - 1) * s.spread);
            double d = rMin + r.nextDouble() * (rMax - rMin);
            int x = Mth.floor(p.getX() - Math.sin(a) * d);
            int z = Mth.floor(p.getZ() + Math.cos(a) * d);
            BlockPos feet = s.cave ? caveFloor(lvl, x, Mth.floor(p.getY()), z, h) : surface(lvl, x, z, h);
            if (feet == null) continue;
            if (s.cave && Math.abs(feet.getY() - p.getY()) > 7) continue;
            ground++;
            Cand c = eval(p, w, feet, s.width, s.height);
            c.rel = Mth.wrapDegrees((float) (Math.toDegrees(a) - p.getYRot()));
            if (c.sight.on) onScreen++;
            if (c.light > 9) bright++;
            visSum += (int) Math.round(c.vis * 100);
            if (!s.ok.test(c)) continue;
            c.score = s.score.applyAsDouble(c) + r.nextDouble() * 0.05;
            if (best == null || c.score > best.score) best = c;
        }
        if (best == null) {
            vn.cavemind.CaveMind.debug("spot search failed: tries={} ground={} onscreen={} bright={} avgvis={} cave={} ring={}-{}",
                s.samples, ground, onScreen, bright, ground == 0 ? 0 : visSum / ground, s.cave, (int) rMin, (int) rMax);
        }
        return best;
    }

    public static Cand eval(ServerPlayer p, Watch w, BlockPos feet, double width, double height) {
        ServerLevel lvl = p.serverLevel();
        Cand c = new Cand();
        c.feet = feet;
        c.box = Sight.body(feet, width, height);
        c.center = c.box.getCenter();
        c.dist = Math.sqrt(p.distanceToSqr(c.center));
        c.sight = Sight.of(p, w, c.center.add(0, height * 0.3, 0));
        c.vis = Sight.visible(p, c.box);
        c.light = lvl.getMaxLocalRawBrightness(feet.above());
        c.cover = cover(lvl, feet, (int) Math.ceil(height));
        c.trunk = trunkBeside(lvl, feet);
        return c;
    }

    public static BlockPos surface(ServerLevel lvl, int x, int z, int h) {
        if (!lvl.hasChunkAt(new BlockPos(x, 64, z))) return null;
        int y = lvl.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
        BlockPos feet = new BlockPos(x, y, z);
        BlockState below = lvl.getBlockState(feet.below());
        if (below.is(BlockTags.LOGS) || below.is(BlockTags.LEAVES)) return null;
        return standable(lvl, feet, h) ? feet : null;
    }

    public static BlockPos caveFloor(ServerLevel lvl, int x, int yHint, int z, int h) {
        if (!lvl.hasChunkAt(new BlockPos(x, yHint, z))) return null;
        for (int y = yHint + 5; y >= yHint - 9; y--) {
            BlockPos feet = new BlockPos(x, y, z);
            if (standable(lvl, feet, h)) return feet;
        }
        return null;
    }

    public static BlockPos nearFloor(ServerLevel lvl, BlockPos around, int h, int dy) {
        for (int k = 0; k <= dy; k++) {
            BlockPos a = around.below(k);
            if (standable(lvl, a, h)) return a;
            BlockPos b = around.above(k);
            if (k > 0 && standable(lvl, b, h)) return b;
        }
        return null;
    }

    public static boolean standable(ServerLevel lvl, BlockPos feet, int h) {
        BlockPos below = feet.below();
        BlockState b = lvl.getBlockState(below);
        if (!b.isFaceSturdy(lvl, below, Direction.UP) || !lvl.getFluidState(below).isEmpty()) return false;
        if (b.is(BlockTags.LEAVES)) return false;
        for (int i = 0; i < h; i++) {
            BlockPos q = feet.above(i);
            BlockState s = lvl.getBlockState(q);
            if (!s.getCollisionShape(lvl, q).isEmpty() || !lvl.getFluidState(q).isEmpty()) return false;
            if (s.is(net.minecraft.world.level.block.Blocks.FIRE) || s.is(net.minecraft.world.level.block.Blocks.LAVA)) return false;
        }
        return true;
    }

    public static boolean cover(ServerLevel lvl, BlockPos feet, int h) {
        for (Direction d : Direction.Plane.HORIZONTAL) {
            for (int i = 0; i < Math.min(h, 3); i++) {
                BlockPos q = feet.above(i).relative(d);
                BlockState s = lvl.getBlockState(q);
                if (s.is(BlockTags.LOGS) || s.is(BlockTags.LEAVES) || !s.getCollisionShape(lvl, q).isEmpty()) return true;
            }
        }
        return false;
    }

    public static boolean trunkBeside(ServerLevel lvl, BlockPos feet) {
        for (Direction d : Direction.Plane.HORIZONTAL) {
            if (lvl.getBlockState(feet.above().relative(d)).is(BlockTags.LOGS)) return true;
        }
        return false;
    }

    public static BlockPos hideNear(ServerPlayer p, Watch w, BlockPos from, int radius, double width, double height, boolean cave) {
        ServerLevel lvl = p.serverLevel();
        BlockPos best = null;
        double bestD = 1e9;
        int h = Mth.ceil(height);
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                if (dx * dx + dz * dz > radius * radius || dx == 0 && dz == 0) continue;
                BlockPos col = from.offset(dx, 0, dz);
                BlockPos feet = cave ? caveFloor(lvl, col.getX(), from.getY(), col.getZ(), h) : surface(lvl, col.getX(), col.getZ(), h);
                if (feet == null || Math.abs(feet.getY() - from.getY()) > 2) continue;
                AABB box = Sight.body(feet, width, height);
                if (Sight.visible(p, box) > 0.0) continue;
                double d = feet.distSqr(from);
                if (d < bestD) {
                    bestD = d;
                    best = feet;
                }
            }
        }
        return best;
    }

    private Spots() {}
}
