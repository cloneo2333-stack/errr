package vn.cavemind.life;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import vn.cavemind.CaveMind;
import vn.cavemind.Cfg;
import vn.cavemind.compat.Compat;
import vn.cavemind.director.Director;
import vn.cavemind.director.Dread;
import vn.cavemind.memory.MindData;
import vn.cavemind.memory.Moment;
import vn.cavemind.memory.Profile;
import vn.cavemind.net.Net;
import vn.cavemind.sense.Group;
import vn.cavemind.sense.Sight;
import vn.cavemind.sense.Watch;
import vn.cavemind.world.Fx;
import vn.cavemind.world.Motion;
import vn.cavemind.world.Spots;

public final class Glimpse {
    public enum Kind { MAN_FAR, MAN_TUNNEL, MAN_FRIEND, MAN_WINDOW, DAMNED_FAR, DAMNED_WINDOW, SIT_IN, LAIR }

    static final class Run {
        Kind kind;
        UUID body;
        long start, seenAt;
        int look, away, phase, hold;
        Vec3 spot;
        boolean man;
        boolean twitched;
    }

    private static final Map<UUID, Run> RUNS = new HashMap<>();
    private static final Map<UUID, Long> NEXT = new HashMap<>();
    private static final Map<UUID, Long> BUMPED = new HashMap<>();

    public static void forget(ServerPlayer p) {
        Run r = RUNS.remove(p.getUUID());
        if (r != null) discard(p.serverLevel(), r.body);
        NEXT.remove(p.getUUID());
    }

    public static void clear() {
        RUNS.clear();
        NEXT.clear();
        BUMPED.clear();
    }

    public static boolean active(ServerPlayer p) {
        return RUNS.containsKey(p.getUUID());
    }

    public static void tick(ServerPlayer p, Watch w, long now) {
        Run r = RUNS.get(p.getUUID());
        if (r != null) {
            boolean keep;
            try {
                keep = run(p, w, r, now);
            } catch (Throwable t) {
                CaveMind.LOG.warn("[CaveMind] glimpse failed", t);
                keep = false;
            }
            if (!keep) {
                RUNS.remove(p.getUUID());
                discard(p.serverLevel(), r.body);
            }
            return;
        }
    }

    public static boolean manShowing() {
        for (Run r : RUNS.values()) if (r.man) return true;
        return false;
    }

    private static boolean busy(ServerPlayer p) {
        return !Director.allows(p, Director.Weight.GLIMPSE) || vn.cavemind.man.ManMind.active(p) || RUNS.containsKey(p.getUUID());
    }


    private static List<ServerPlayer> group(ServerPlayer p) {
        List<ServerPlayer> g = new ArrayList<>();
        g.add(p);
        for (ServerPlayer o : Group.others(p, 48)) if (!Compat.exempt(o)) g.add(o);
        return g;
    }

    private static ServerPlayer witness(List<ServerPlayer> g) {
        Vec3 c = Vec3.ZERO;
        for (ServerPlayer o : g) c = c.add(o.position());
        c = c.scale(1.0 / g.size());
        ServerPlayer best = null;
        double bs = -1;
        for (ServerPlayer o : g) {
            if (busy(o)) continue;
            double s = o.position().distanceTo(c) + (Watch.of(o).light <= 7 ? 5 : 0) + o.getRandom().nextDouble() * 6;
            if (s > bs) {
                bs = s;
                best = o;
            }
        }
        return best;
    }

    public static boolean damnedFor(ServerPlayer p) {
        if (!Compat.damnedActive()) return false;
        Profile pr = MindData.profile(p);
        long day = Dread.day(p.serverLevel());
        if (day < Cfg.DAMNED_START_DAY.get()) return false;
        if (pr.dQuietUntilDay >= 0 && day < pr.dQuietUntilDay) return false;
        long key = vn.cavemind.damned.Haunt.nightKey(p.serverLevel());
        ServerPlayer chosen = p;
        int best = pr.dStage;
        for (ServerPlayer o : Group.others(p, 48)) {
            if (Compat.exempt(o)) continue;
            int st = MindData.profile(o).dStage;
            long tie = (o.getUUID().getLeastSignificantBits() ^ key) - (chosen.getUUID().getLeastSignificantBits() ^ key);
            if (st > best || st == best && tie < 0) {
                best = st;
                chosen = o;
            }
        }
        return chosen == p;
    }

    private static boolean begin(ServerPlayer p, Watch w, long now, Kind only) {
        ServerLevel lvl = p.serverLevel();
        boolean night = lvl.isNight() || lvl.isThundering();
        int tod = Dread.time(lvl);
        boolean dusk = tod >= 11500 && tod < 13000;
        boolean manOk = Compat.manActive() && !vn.cavemind.man.ManMind.anyActive() && (night || dusk || w.underground || lvl.isRaining() || Cfg.MAN_DAYTIME.get());
        boolean damned = damnedFor(p);
        List<Kind> o = new ArrayList<>();
        List<Double> wt = new ArrayList<>();
        if (w.indoors) {
            if (night && damned) add(o, wt, Kind.DAMNED_WINDOW, 1.0);
            if (night && manOk) add(o, wt, Kind.MAN_WINDOW, 0.7);
        } else if (w.underground) {
            if (manOk) add(o, wt, Kind.MAN_TUNNEL, 1.0);
        } else {
            if (manOk) add(o, wt, Kind.MAN_FAR, 1.0);
            if (damned) add(o, wt, Kind.DAMNED_FAR, 0.7);
            if (manOk && (night || dusk || lvl.isRaining()) && !Group.others(p, 40).isEmpty()) add(o, wt, Kind.MAN_FRIEND, 1.3);
        }
        RandomSource r = p.getRandom();
        while (!o.isEmpty()) {
            double sum = 0;
            for (double d : wt) sum += d;
            double x = r.nextDouble() * sum;
            int i = 0;
            for (; i < o.size() - 1; i++) {
                x -= wt.get(i);
                if (x <= 0) break;
            }
            Kind k = o.remove(i);
            wt.remove(i);
            if (setup(p, w, k, now, null)) return true;
        }
        return false;
    }

    private static void add(List<Kind> o, List<Double> w, Kind k, double v) {
        o.add(k);
        w.add(v);
    }

    public static boolean force(ServerPlayer p) {
        if (busy(p) || Compat.exempt(p)) return false;
        long now = p.server.getTickCount();
        return begin(p, Watch.of(p), now, null);
    }

    private static BlockPos lairAt;

    public static boolean lair(ServerPlayer p, BlockPos lair) {
        if (!Compat.manActive() || Compat.exempt(p) || busy(p)) return false;
        lairAt = lair;
        try {
            return setup(p, Watch.of(p), Kind.LAIR, p.server.getTickCount(), null);
        } finally {
            lairAt = null;
        }
    }

    public static boolean sitIn(ServerPlayer p, LifeData.Base b) {
        if (!Compat.manActive() || Compat.exempt(p) || busy(p)) return false;
        return setup(p, Watch.of(p), Kind.SIT_IN, p.server.getTickCount(), b);
    }

    private static boolean setup(ServerPlayer p, Watch w, Kind k, long now, LifeData.Base b) {
        ServerLevel lvl = p.serverLevel();
        Spots.Cand c = null;
        boolean man = k != Kind.DAMNED_FAR && k != Kind.DAMNED_WINDOW;
        double h = man ? 3.0 : 1.8;
        switch (k) {
            case MAN_FAR -> {
                double rel = toward(p);
                boolean day = lvl.isDay() && !lvl.isThundering();
                c = Spots.find(p, w, new Spots.Spec().ring(46, 88).dir(rel, 80).body(0.6, h).tries(50)
                    .when(q -> !q.sight.on && q.vis >= 0.3 && (q.cover || q.trunk || q.light <= 8 || q.dist > 66 || !day))
                    .rank(q -> q.vis * 0.6 + (q.cover ? 0.4 : 0) + (q.trunk ? 0.3 : 0)));
            }
            case MAN_TUNNEL -> c = Spots.find(p, w, new Spots.Spec().ring(14, 30).dir(180, 55).body(0.6, h).cave(true).tries(50)
                .when(q -> !q.sight.on && q.light <= 6 && q.vis >= 0.45).rank(q -> q.vis + (6 - q.light) * 0.05 + q.dist * 0.01));
            case MAN_FRIEND -> c = behindFriend(p, w);
            case DAMNED_FAR -> c = Spots.find(p, w, new Spots.Spec().ring(28, 58).dir(toward(p), 120).body(0.6, h).tries(50)
                .when(q -> !q.sight.on && q.vis >= 0.35 && (q.cover || q.trunk || q.light <= 7)).rank(q -> q.vis + (q.trunk ? 0.4 : 0)));
            case MAN_WINDOW, DAMNED_WINDOW -> c = vn.cavemind.man.Window.find(p, w);
            case SIT_IN -> c = sitSpot(p, w, b);
            case LAIR -> c = lairSpot(p, w, lairAt);
        }
        if (c == null) return false;
        Entity e;
        if (man) e = vn.cavemind.man.ManMind.make(p, Compat.MAN_STALKER, c.feet, true);
        else e = vn.cavemind.damned.Haunt.spawn(p, c.feet, p.getEyePosition());
        if (e == null) return false;
        String pose = switch (k) {
            case DAMNED_FAR -> c.trunk ? "cm_peek" : p.getRandom().nextBoolean() ? "cm_low" : "cm_still";
            case DAMNED_WINDOW -> "cm_still";
            case MAN_TUNNEL, SIT_IN, LAIR -> "cm_hold";
            default -> "empty";
        };
        Compat.anim(e, pose);
        Run r = new Run();
        r.kind = k;
        r.body = e.getUUID();
        r.start = now;
        r.spot = c.center;
        r.man = man;
        r.hold = 30 + p.getRandom().nextInt(50);
        RUNS.put(p.getUUID(), r);
        CaveMind.debug("glimpse {} for {} at {}m", k, p.getGameProfile().getName(), (int) c.dist);
        return true;
    }

    private static double toward(ServerPlayer p) {
        Vec3 at = Life.where(p.server);
        if (at == null || at.distanceToSqr(p.position()) > 400 * 400 || p.getRandom().nextFloat() < 0.25f) return p.getRandom().nextDouble() * 360 - 180;
        double yaw = Mth.atan2(at.z - p.getZ(), at.x - p.getX()) * (180 / Math.PI) - 90;
        return Mth.wrapDegrees(yaw - p.getYRot());
    }

    private static Spots.Cand behindFriend(ServerPlayer p, Watch w) {
        ServerLevel lvl = p.serverLevel();
        RandomSource r = p.getRandom();
        for (ServerPlayer f : Group.others(p, 40)) {
            double fd = f.distanceTo(p);
            if (fd < 8 || Compat.exempt(f)) continue;
            Vec3 away = f.position().subtract(p.position()).multiply(1, 0, 1).normalize();
            Vec3 side = new Vec3(-away.z, 0, away.x);
            Watch fw = Watch.of(f);
            for (int i = 0; i < 14; i++) {
                Vec3 q = f.position().add(away.scale(7 + r.nextDouble() * 8)).add(side.scale((r.nextDouble() - 0.5) * 7));
                BlockPos feet = fw.underground ? Spots.caveFloor(lvl, Mth.floor(q.x), Mth.floor(f.getY()), Mth.floor(q.z), 3) : Spots.surface(lvl, Mth.floor(q.x), Mth.floor(q.z), 3);
                if (feet == null || Math.abs(feet.getY() - f.getY()) > 4) continue;
                Spots.Cand c = Spots.eval(p, w, feet, 0.6, 3.0);
                if (c.sight.on || c.vis < 0.35) continue;
                Sight fs = Sight.of(f, fw, c.center);
                if (fs.on && fs.edge() < 1.15) continue;
                return c;
            }
        }
        return null;
    }

    private static Spots.Cand lairSpot(ServerPlayer p, Watch w, BlockPos lair) {
        if (lair == null) return null;
        ServerLevel lvl = p.serverLevel();
        RandomSource r = p.getRandom();
        Spots.Cand best = null;
        for (int i = 0; i < 70; i++) {
            BlockPos q = lair.offset(r.nextInt(15) - 7, 0, r.nextInt(15) - 7);
            BlockPos feet = Spots.nearFloor(lvl, q, 3, 4);
            if (feet == null) continue;
            Spots.Cand c = Spots.eval(p, w, feet, 0.6, 3.0);
            if (c.dist < 7 || c.dist > 34 || c.sight.on || c.vis < 0.25) continue;
            boolean roof = !lvl.canSeeSky(feet.above(3));
            c.score = (roof ? 1.0 : 0) + (8 - Math.min(8, c.light)) * 0.1 + c.vis + r.nextDouble() * 0.1;
            if (best == null || c.score > best.score) best = c;
        }
        return best;
    }

    private static Spots.Cand sitSpot(ServerPlayer p, Watch w, LifeData.Base b) {
        ServerLevel lvl = p.serverLevel();
        RandomSource r = p.getRandom();
        Spots.Cand best = null;
        for (int i = 0; i < 60; i++) {
            BlockPos q = p.blockPosition().offset(r.nextInt(29) - 14, 0, r.nextInt(29) - 14);
            BlockPos feet = Spots.nearFloor(lvl, q, 3, 2);
            if (feet == null) continue;
            if (b != null && feet.distSqr(b.center) > (b.radius + 2) * (b.radius + 2)) continue;
            if (lvl.canSeeSky(feet.above(3))) continue;
            int light = lvl.getMaxLocalRawBrightness(feet.above());
            if (light > 8) continue;
            Spots.Cand c = Spots.eval(p, w, feet, 0.6, 3.0);
            if (c.dist < 5 || c.dist > 16 || c.sight.on || c.vis < 0.3) continue;
            c.score = (8 - light) * 0.2 + c.vis + r.nextDouble() * 0.1;
            if (best == null || c.score > best.score) best = c;
        }
        return best;
    }

    private static boolean run(ServerPlayer p, Watch w, Run r, long now) {
        ServerLevel lvl = p.serverLevel();
        if (!p.isAlive() || p.level().dimension() != Level.OVERWORLD) return false;
        Entity e = r.body == null ? null : lvl.getEntity(r.body);
        if (e == null || !e.isAlive()) return false;
        double dist = p.distanceTo(e);
        Sight si = Sight.of(p, w, e.getBoundingBox().getCenter());
        double vis = si.on ? Sight.visible(p, e) : 0;
        long age = now - r.start;
        if (e instanceof Mob m && (r.phase == 1 || vis == 0)) Motion.turn(m, p.getEyePosition(), r.phase == 1 ? 1.5f : 6f);
        double flee = switch (r.kind) {
            case MAN_FAR -> 30;
            case DAMNED_FAR -> 18;
            case MAN_TUNNEL -> 9;
            case MAN_FRIEND -> 6;
            case SIT_IN -> 4.5;
            case LAIR -> 7;
            default -> 2.5;
        };
        if (r.phase == 0) {
            if (vis > 0.12 && si.edge() < 0.92) r.look++;
            else r.look = Math.max(0, r.look - 1);
            if (r.look >= (r.kind == Kind.SIT_IN || r.kind == Kind.LAIR ? 8 : 14)) {
                r.phase = 1;
                r.seenAt = now;
                seen(p, r, e, dist);
                return true;
            }
            if (dist < flee) return vanish(p, r, e, false);
            if (age > 900) {
                missed(p, r, e);
                return false;
            }
            return true;
        }
        if (dist < flee) return vanish(p, r, e, true);
        r.away = vis == 0 ? r.away + 1 : 0;
        long held = now - r.seenAt;
        if (held > r.hold && r.away >= 6) return false;
        if (r.man && !r.twitched && held > r.hold + 60 && vis > 0) {
            r.twitched = true;
            Compat.anim(e, r.kind == Kind.SIT_IN ? "cm_tilt" : "cm_twitch");
        }
        if (!r.man && !r.twitched && held > r.hold + 40 && vis > 0) {
            r.twitched = true;
            Compat.anim(e, "cm_notice");
        }
        if (held > r.hold + 200) return vanish(p, r, e, true);
        return true;
    }

    private static boolean vanish(ServerPlayer p, Run r, Entity e, boolean seen) {
        if (r.man) {
            Fx.sound(p, "man:vanish", SoundSource.HOSTILE, e.position(), seen ? 0.45f : 0.2f, 0.8f);
            if (Cfg.FOG.get()) Net.fx(p, Net.FOG, 1.5f, 14);
        } else if (seen) {
            vn.cavemind.damned.Haunt.breath(p, e.getEyePosition(), 0.35f);
        }
        return false;
    }

    private static void seen(ServerPlayer p, Run r, Entity e, double dist) {
        ServerLevel lvl = p.serverLevel();
        Profile pr = MindData.profile(p);
        long day = Dread.day(lvl);
        Director.scare(p, r.kind == Kind.SIT_IN ? 10 : 4);
        vn.cavemind.director.Tension.seen(p, dist, r.man ? "The Man" : "The Damned");
        vn.cavemind.director.PaceLog.encounter(p, r.man ? "man" : "damned", "glimpse " + r.kind, (int) dist + "m");
        if (r.man) {
            vn.cavemind.man.Meter.seen(p, 3);
            pr.manSightings++;
            pr.moment(new Moment(r.kind == Kind.SIT_IN ? Moment.SITIN : Moment.SEEN, day, Dread.time(lvl), p.blockPosition(), (int) dist, 0));
            Compat.grant(p, "man:feelingwatched");
            Life.seen(p.server, e.position(), dist);
            Long last = BUMPED.get(p.getUUID());
            if (last == null || last != day) {
                long days = pr.firstDay >= 0 ? day - pr.firstDay : 0;
                int cap = 1 + (int) (days * 0.8) + pr.manSessions / 3;
                if (pr.manLevel < Math.min(10, cap)) {
                    pr.manLevel++;
                    BUMPED.put(p.getUUID(), day);
                }
            }
            if (r.kind == Kind.SIT_IN) Fx.sound(p, "man:long_whisper", SoundSource.HOSTILE, e.getEyePosition(), 0.3f, 0.7f);
        } else {
            if (r.kind == Kind.DAMNED_WINDOW) pr.moment(new Moment(Moment.WINDOW, day, Dread.time(lvl), p.blockPosition(), (int) dist, 0));
            vn.cavemind.damned.Haunt.glimpsed(p);
        }
        CaveMind.debug("{} caught a glimpse ({}, {}m)", p.getGameProfile().getName(), r.kind, (int) dist);
    }

    private static void missed(ServerPlayer p, Run r, Entity e) {
        if (!r.man || r.kind == Kind.SIT_IN) return;
        ServerLevel lvl = p.serverLevel();
        BlockPos at = e.blockPosition();
        int n = 0;
        for (int i = 0; i < 4; i++) {
            BlockPos q = at.offset(p.getRandom().nextInt(3) - 1, 0, p.getRandom().nextInt(3) - 1);
            if (Life.print(lvl, q, null, false)) n++;
        }
        if (n > 0) MindData.profile(p).clue(new Profile.Clue("prints", lvl.getHeightmapPos(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, at), Dread.day(lvl), 7));
    }

    private static void discard(ServerLevel lvl, UUID id) {
        if (id == null) return;
        Entity e = lvl.getEntity(id);
        if (e != null) e.discard();
    }

    private Glimpse() {}
}
