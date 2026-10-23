package vn.cavemind.man;

import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public final class Scent {
    public enum Kind { TRAIL, WOUND, BLOOD, DEATH, SMOKE }

    static final class Cell {
        float trail, wound, blood, death;
        long tt, tw, tb, td;
        int x, y, z;
        boolean deep;
    }

    private static final Long2ObjectOpenHashMap<Cell> CELLS = new Long2ObjectOpenHashMap<>();
    private static long now;
    private static boolean rain;

    public static void clear() {
        CELLS.clear();
    }

    static long key(int cx, int cy, int cz) {
        return ((long) (cx & 0x3FFFFF) << 42) | ((long) (cy & 0xFFFFF) << 22) | (cz & 0x3FFFFF);
    }

    public static void mark(Level lvl, BlockPos p, Kind k, float strength, long t) {
        mark(lvl, p, k, strength, t, false);
    }

    public static void mark(Level lvl, BlockPos p, Kind k, float strength, long t, boolean deep) {
        if (lvl.dimension() != Level.OVERWORLD) return;
        now = t;
        int cx = p.getX() >> 1, cy = p.getY() >> 2, cz = p.getZ() >> 1;
        long key = key(cx, cy, cz);
        Cell c = CELLS.get(key);
        if (c == null) {
            if (CELLS.size() > 40000) return;
            c = new Cell();
            c.x = p.getX();
            c.y = p.getY();
            c.z = p.getZ();
            CELLS.put(key, c);
        }
        switch (k) {
            case TRAIL, SMOKE -> {
                c.trail = Math.min(40f, trail(c) + strength);
                c.tt = t;
                c.x = p.getX();
                c.y = p.getY();
                c.z = p.getZ();
                c.deep = deep;
            }
            case WOUND -> {
                c.wound = Math.min(40f, wound(c) + strength);
                c.tw = t;
            }
            case BLOOD -> {
                c.blood = Math.min(40f, blood(c) + strength);
                c.tb = t;
            }
            case DEATH -> {
                c.death = Math.min(40f, death(c) + strength);
                c.td = t;
            }
        }
    }

    private static float lin(float v, long t0, long tau) {
        long age = now - t0;
        if (age >= tau || v <= 0) return 0;
        return v * (1f - age / (float) tau);
    }

    static float trail(Cell c) {
        return lin(c.trail, c.tt, rain ? 1200 : 3600);
    }

    static float wound(Cell c) {
        return lin(c.wound, c.tw, 24000);
    }

    static float blood(Cell c) {
        return lin(c.blood, c.tb, 12000);
    }

    static float death(Cell c) {
        return lin(c.death, c.td, 24000);
    }

    static float score(Cell c) {
        return trail(c) + wound(c) + blood(c) + death(c);
    }

    static long fresh(Cell c) {
        return Math.max(Math.max(c.tt, c.tw), Math.max(c.tb, c.td));
    }

    public static void tick(ServerLevel over, long t) {
        now = t;
        rain = over.isRaining();
        if (t % 1200 != 0) return;
        CELLS.values().removeIf(c -> score(c) < 0.05f);
    }

    public static double strength(Vec3 at, int radius) {
        int cx = (int) Math.floor(at.x) >> 1, cy = (int) Math.floor(at.y) >> 2, cz = (int) Math.floor(at.z) >> 1;
        double best = 0;
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                for (int dy = -2; dy <= 2; dy++) {
                    Cell c = CELLS.get(key(cx + dx, cy + dy, cz + dz));
                    if (c == null) continue;
                    double s = score(c) / (1.0 + Math.sqrt(dx * dx + dz * dz) * 0.15);
                    if (s > best) best = s;
                }
            }
        }
        return best;
    }

    public static BlockPos uphill(Vec3 at, int radius) {
        int cx = (int) Math.floor(at.x) >> 1, cy = (int) Math.floor(at.y) >> 2, cz = (int) Math.floor(at.z) >> 1;
        Cell here = null;
        for (int dy = -1; dy <= 1 && here == null; dy++) here = CELLS.get(key(cx, cy + dy, cz));
        long hereT = here == null ? 0 : fresh(here);
        double hereS = here == null ? 0 : score(here);
        Cell best = null;
        double bs = hereS + 0.01;
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                if (dx == 0 && dz == 0) continue;
                for (int dy = -2; dy <= 2; dy++) {
                    Cell c = CELLS.get(key(cx + dx, cy + dy, cz + dz));
                    if (c == null) continue;
                    double s = score(c);
                    if (s < 0.1) continue;
                    long f = fresh(c);
                    double newer = f > hereT ? Math.min(3.0, (f - hereT) / 600.0) : -1.5;
                    double v = s + newer;
                    if (v > bs) {
                        bs = v;
                        best = c;
                    }
                }
            }
        }
        return best == null ? null : new BlockPos(best.x, best.y, best.z);
    }

    public static boolean deepAt(Vec3 at, int radius) {
        int cx = (int) Math.floor(at.x) >> 1, cy = (int) Math.floor(at.y) >> 2, cz = (int) Math.floor(at.z) >> 1;
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                for (int dy = -2; dy <= 2; dy++) {
                    Cell c = CELLS.get(key(cx + dx, cy + dy, cz + dz));
                    if (c != null && c.deep && score(c) > 0.3f) return true;
                }
            }
        }
        return false;
    }

    public static BlockPos strongest(Vec3 from, double range, double minScore) {
        return strongest(from, range, minScore, false);
    }

    public static BlockPos strongest(Vec3 from, double range, double minScore, boolean deepOnly) {
        Cell best = null;
        double bs = 0;
        double r2 = range * range;
        for (Long2ObjectMap.Entry<Cell> en : CELLS.long2ObjectEntrySet()) {
            Cell c = en.getValue();
            if (deepOnly && !c.deep) continue;
            double dx = c.x - from.x, dz = c.z - from.z;
            double d2 = dx * dx + dz * dz;
            if (d2 > r2) continue;
            double s = score(c);
            if (s < minScore) continue;
            double v = s * (1.0 + (fresh(c) - now) / 2400.0 * 0.5) / (1.0 + Math.sqrt(d2) / 96.0);
            if (v > bs) {
                bs = v;
                best = c;
            }
        }
        return best == null ? null : new BlockPos(best.x, best.y, best.z);
    }

    public static int size() {
        return CELLS.size();
    }

    private Scent() {}
}
