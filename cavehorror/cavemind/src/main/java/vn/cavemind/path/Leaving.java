package vn.cavemind.path;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import vn.cavemind.CaveMind;
import vn.cavemind.Cfg;
import vn.cavemind.net.Net;
import vn.cavemind.sense.Range;
import vn.cavemind.world.Fx;

public final class Leaving {
    static final class L {
        UUID body;
        ResourceKey<Level> dim;
        Walker w;
        long since;
        Consumer<Vec3> gone;
        boolean fogged;
        long fogAt;
    }

    private static final List<L> ALL = new ArrayList<>();

    public static void clear() {
        ALL.clear();
    }

    public static boolean any() {
        return !ALL.isEmpty();
    }

    public static void add(Mob m, BlockPos away, double speed, String gait, int gaitLen, Consumer<Vec3> gone) {
        if (m == null || !m.isAlive()) return;
        L l = new L();
        l.body = m.getUUID();
        l.dim = m.level().dimension();
        l.w = new Walker(away, speed, Stealth.Mode.SNEAK).gait(gait, gaitLen);
        l.since = m.level().getServer().getTickCount();
        l.gone = gone;
        ALL.add(l);
    }

    public static void tick(MinecraftServer server) {
        if (ALL.isEmpty()) return;
        long now = server.getTickCount();
        Iterator<L> it = ALL.iterator();
        while (it.hasNext()) {
            L l = it.next();
            ServerLevel lvl = server.getLevel(l.dim);
            Entity e = lvl == null ? null : lvl.getEntity(l.body);
            if (!(e instanceof Mob m) || !e.isAlive()) {
                it.remove();
                continue;
            }
            List<ServerPlayer> viewers = Walker.viewers(e);
            Walker.Step st;
            try {
                st = l.w.tick(m, now, viewers);
            } catch (Throwable t) {
                CaveMind.LOG.warn("[CaveMind] leaving body failed", t);
                st = Walker.Step.FAILED;
            }
            boolean seen = Walker.seenAtAll(viewers, e.getBoundingBox());
            double near = 1e9, range = 48;
            for (ServerPlayer o : viewers) {
                double d = o.distanceTo(e);
                if (d < near) {
                    near = d;
                    range = Range.of(o);
                }
            }
            long age = now - l.since;
            boolean far = near > Math.max(44, range * 1.35);
            if (l.fogged && now - l.fogAt > 22) {
                done(it, l, e);
                continue;
            }
            if (!seen && (far || age > 500 || st == Walker.Step.ARRIVED || st == Walker.Step.FAILED)) {
                done(it, l, e);
                continue;
            }
            if (!l.fogged && (l.w.frozenTicks > 520 || age > 2400)) {
                l.fogged = true;
                l.fogAt = now;
                for (ServerPlayer o : viewers) {
                    if (Cfg.FOG.get()) Net.fx(o, Net.FOG, 1.9f, 20);
                    Fx.sound(o, "man:vanish", SoundSource.HOSTILE, e.position(), 0.35f, 0.75f);
                }
            }
        }
    }

    private static void done(Iterator<L> it, L l, Entity e) {
        it.remove();
        Vec3 at = e.position();
        e.discard();
        if (l.gone != null) {
            try {
                l.gone.accept(at);
            } catch (Throwable ignored) {
            }
        }
    }

    private Leaving() {}
}
