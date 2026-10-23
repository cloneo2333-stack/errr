package vn.cavemind.sense;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public final class Ears {
    public enum Kind { STEP, SPRINT, JUMP, LAND, MINE, BUILD, DOOR, FIGHT, BOOM, VOICE, TOY, CLOSET, BELL, EAT }

    public record Noise(Vec3 pos, double radius, Kind kind, UUID who, long at, ResourceKey<Level> dim) {
        public double reach(Vec3 ear) {
            return radius - pos.distanceTo(ear);
        }
    }

    private static final ArrayDeque<Noise> RECENT = new ArrayDeque<>();
    private static long now;

    public static void clear() {
        RECENT.clear();
    }

    public static void tick(long t) {
        now = t;
        while (!RECENT.isEmpty() && t - RECENT.peekFirst().at() > 1200) RECENT.removeFirst();
    }

    public static void noise(ServerPlayer p, Vec3 pos, double radius, Kind kind) {
        if (p == null || p.isSpectator()) return;
        double r = radius;
        if (Watch.of(p).underground) r *= 1.3;
        add(new Noise(pos, r, kind, p.getUUID(), p.server.getTickCount(), p.level().dimension()));
    }

    public static void noise(Level lvl, Vec3 pos, double radius, Kind kind, long at) {
        add(new Noise(pos, radius, kind, null, at, lvl.dimension()));
    }

    private static void add(Noise n) {
        RECENT.addLast(n);
        while (RECENT.size() > 768) RECENT.removeFirst();
    }

    public static Noise loudest(Level lvl, Vec3 ear, long t, long maxAge, double earMul) {
        Noise best = null;
        double bs = 0;
        ResourceKey<Level> dim = lvl.dimension();
        Iterator<Noise> it = RECENT.descendingIterator();
        while (it.hasNext()) {
            Noise n = it.next();
            long age = t - n.at();
            if (age > maxAge) break;
            if (n.dim() != dim) continue;
            double reach = n.radius() * earMul - n.pos().distanceTo(ear);
            if (reach <= 0) continue;
            double s = reach * (1.0 - age / (double) (maxAge + 1) * 0.6);
            if (s > bs) {
                bs = s;
                best = n;
            }
        }
        return best;
    }

    public static List<Noise> since(Level lvl, long t) {
        List<Noise> out = new ArrayList<>();
        ResourceKey<Level> dim = lvl.dimension();
        Iterator<Noise> it = RECENT.descendingIterator();
        while (it.hasNext()) {
            Noise n = it.next();
            if (n.at() <= t) break;
            if (n.dim() == dim) out.add(n);
        }
        return out;
    }

    public static Noise lastBy(UUID who, long maxAge) {
        Iterator<Noise> it = RECENT.descendingIterator();
        while (it.hasNext()) {
            Noise n = it.next();
            if (now - n.at() > maxAge) break;
            if (who.equals(n.who())) return n;
        }
        return null;
    }

    private Ears() {}
}
