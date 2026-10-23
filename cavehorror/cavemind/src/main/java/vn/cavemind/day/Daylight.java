package vn.cavemind.day;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.function.Supplier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.level.Level;
import vn.cavemind.CaveMind;
import vn.cavemind.Cfg;
import vn.cavemind.compat.Compat;
import vn.cavemind.director.Director;
import vn.cavemind.director.Dread;
import vn.cavemind.memory.MindData;
import vn.cavemind.memory.Profile;
import vn.cavemind.sense.Watch;

public final class Daylight {
    public enum Theme { WATCHED, INTRUSION, PROCESSION, FOLLOWER }

    static final class State {
        long day = -1;
        Theme theme;
        long next;
        Piece run;
        int done;
        String last = "";
    }

    private static final Map<UUID, State> STATES = new HashMap<>();

    public static void forget(ServerPlayer p) {
        State s = STATES.remove(p.getUUID());
        if (s != null && s.run != null) s.run.end(p);
    }

    public static void clear() {
        STATES.clear();
    }

    static String vi(Theme t) {
        return switch (t) {
            case WATCHED -> "bị nhìn";
            case INTRUSION -> "bị xâm phạm";
            case PROCESSION -> "nghi lễ";
            case FOLLOWER -> "kẻ đi theo";
        };
    }

    public static void tick(ServerPlayer p, Watch w, long now) {
        if (!Cfg.DAYLIGHT.get()) return;
        State st = STATES.computeIfAbsent(p.getUUID(), k -> new State());
        if (st.run != null) {
            boolean keep;
            try {
                keep = st.run.tick(p, w, MindData.profile(p), now);
            } catch (Throwable t) {
                CaveMind.LOG.warn("[CaveMind] daylight piece failed", t);
                keep = false;
            }
            if (!keep) finish(p, st, now);
            return;
        }
        if (now % 20 != 0) return;
        ServerLevel lvl = p.serverLevel();
        if (lvl.dimension() != Level.OVERWORLD || Compat.exempt(p) || p.isSleeping()) return;
        int tod = Dread.time(lvl);
        if (tod < 300 || tod > 11600) return;
        long day = Dread.day(lvl);
        Profile pr = MindData.profile(p);
        if (st.day != day) {
            st.day = day;
            st.theme = theme(lvl, day, p);
            st.done = 0;
            if (st.next == 0 || st.next < now) st.next = now + (2400 + p.getRandom().nextInt(2400));
        }
        if (now < st.next) return;
        for (ServerPlayer o : vn.cavemind.sense.Group.others(p, 48)) {
            State so = STATES.get(o.getUUID());
            if (so != null && so.run != null) {
                st.next = now + 300;
                return;
            }
        }
        if (!Director.free(p)) {
            st.next = now + 300;
            return;
        }
        if (st.done >= (Math.max(1, (int) Math.round(3 * Cfg.INTENSITY.get())))) return;
        Piece pc = pick(p, w, pr, st, tod, day);
        if (pc == null) {
            st.next = now + 600;
            return;
        }
        if (pc.claims() && !Director.claim(p, "day")) {
            st.next = now + 300;
            return;
        }
        pc.start = now;
        boolean ok;
        try {
            ok = pc.begin(p, w, pr, now);
        } catch (Throwable t) {
            CaveMind.LOG.warn("[CaveMind] daylight piece failed to begin", t);
            ok = false;
        }
        if (ok) {
            st.run = pc;
            st.last = pc.id();
            st.done++;
            CaveMind.debug("daylight {} begins for {}", pc.id(), p.getGameProfile().getName());
        } else {
            pc.end(p);
            if (pc.claims()) Director.abort(p, "day");
            CaveMind.debug("daylight {} could not begin for {}", pc.id(), p.getGameProfile().getName());
            st.next = now + 400;
        }
    }

    private static void finish(ServerPlayer p, State st, long now) {
        Piece pc = st.run;
        st.run = null;
        pc.end(p);
        if (pc.claims()) Director.release(p, "day", false);
        st.next = now + ((long) ((4800 + p.getRandom().nextInt(4800)) / Cfg.INTENSITY.get()));
        CaveMind.debug("daylight {} ends for {}", pc.id(), p.getGameProfile().getName());
    }

    public static boolean force(ServerPlayer p, String id) {
        State st = STATES.computeIfAbsent(p.getUUID(), k -> new State());
        if (st.run != null) finish(p, st, p.server.getTickCount());
        Piece pc = make(id);
        if (pc == null) return false;
        Director.state(p).act = null;
        if (pc.claims()) Director.claim(p, "day");
        long now = p.server.getTickCount();
        pc.start = now;
        if (pc.begin(p, Watch.of(p), MindData.profile(p), now)) {
            st.run = pc;
            return true;
        }
        pc.end(p);
        if (pc.claims()) Director.abort(p, "day");
        return false;
    }

    static Piece make(String id) {
        return switch (id) {
            case "field" -> new Field();
            case "follows", "seek" -> new HideSeek();
            case "ritual" -> new Ritual();
            case "village" -> new Village();
            case "house" -> new House(false);
            case "inside" -> new House(true);
            case "echo" -> new Echo();
            case "twin" -> new Twin();
            case "noon" -> new Noon(false);
            case "quiet" -> new Noon(true);
            default -> null;
        };
    }

    static Theme theme(ServerLevel lvl, long day, ServerPlayer p) {
        Random r = new Random(lvl.getSeed() * 31 + day * 7919 + p.getUUID().getLeastSignificantBits());
        return Theme.values()[r.nextInt(Theme.values().length)];
    }

    private static Piece pick(ServerPlayer p, Watch w, Profile pr, State st, int tod, long day) {
        ServerLevel lvl = p.serverLevel();
        long days = pr.firstDay >= 0 ? day - pr.firstDay : 0;
        int tier = days >= 3 ? 2 : days >= 2 ? 1 : 0;
        List<Supplier<Piece>> opts = new ArrayList<>();
        List<java.lang.Double> wts = new ArrayList<>();
        Theme th = st.theme;
        double homeDist = pr.home == null ? -1 : Math.sqrt(pr.home.distSqr(p.blockPosition()));
        if (w.outside && tod >= 4300 && tod <= 7700) add(opts, wts, () -> new Noon(false), 1.0, th == Theme.WATCHED || th == Theme.PROCESSION);
        if (w.outside) add(opts, wts, () -> new Noon(true), 0.35, false);
        if (Echo.ready(p)) add(opts, wts, Echo::new, 0.9, th == Theme.INTRUSION || th == Theme.FOLLOWER);
        if (pr.home != null && homeDist >= 64 && homeDist <= 170 && lvl.isLoaded(pr.home)) add(opts, wts, () -> new House(false), 1.0, th == Theme.INTRUSION);
        if (tier >= 1) {
            if (w.outside && Compat.manLoaded() && !vn.cavemind.man.ManMind.anyActive() && manNear(p, 220)) add(opts, wts, Field::new, 1.1, th == Theme.WATCHED);
            int vill = lvl.getEntitiesOfClass(AbstractVillager.class, p.getBoundingBox().inflate(48, 16, 48)).size();
            if (vill >= 3) add(opts, wts, Village::new, 1.2, th == Theme.PROCESSION);
            int anim = lvl.getEntitiesOfClass(Animal.class, p.getBoundingBox().inflate(48, 16, 48), Ritual::eligible).size();
            if (anim >= 5) add(opts, wts, Ritual::new, 1.2, th == Theme.PROCESSION);
        }
        if (!vn.cavemind.damned.Game.on() && seekReady(p, pr) && Cfg.SEEK_FREQ.get() > 0 && w.outside && Compat.apollyonLoaded()) {
            add(opts, wts, HideSeek::new, 1.1 * Cfg.SEEK_FREQ.get(), th == Theme.FOLLOWER || th == Theme.WATCHED);
        }
        if (tier >= 2) {
            if (w.outside) add(opts, wts, Twin::new, 0.8, th == Theme.FOLLOWER || th == Theme.WATCHED);
            if (pr.home != null && homeDist >= 45 && homeDist <= 120 && lvl.isLoaded(pr.home) && Compat.apollyonLoaded()) {
                add(opts, wts, () -> new House(true), 0.9, th == Theme.INTRUSION);
            }
        }
        if (opts.isEmpty()) return null;
        List<Piece> made = new ArrayList<>();
        for (Supplier<Piece> s : opts) made.add(s.get());
        double sum = 0;
        for (int i = 0; i < made.size(); i++) {
            if (made.get(i).id().equals(st.last)) wts.set(i, wts.get(i) * 0.3);
            sum += wts.get(i);
        }
        double x = p.getRandom().nextDouble() * sum;
        for (int i = 0; i < made.size(); i++) {
            x -= wts.get(i);
            if (x <= 0) return made.get(i);
        }
        return made.get(made.size() - 1);
    }

    public static boolean seekReady(ServerPlayer p, Profile pr) {
        if (!Cfg.SEEK.get() || !Cfg.DAYLIGHT.get()) return false;
        if (pr.lore.contains("g") || pr.lore.contains("h")) return true;
        long days = pr.firstDay >= 0 ? Dread.day(p.serverLevel()) - pr.firstDay : 0;
        return days >= Cfg.SEEK_DAY.get();
    }

    static boolean manNear(ServerPlayer p, double r) {
        net.minecraft.world.phys.Vec3 at = vn.cavemind.life.Life.where(p.server);
        return at == null || at.distanceToSqr(p.position()) < r * r;
    }

    public static boolean busy(ServerPlayer p) {
        State st = STATES.get(p.getUUID());
        return st != null && st.run != null;
    }

    private static void add(List<Supplier<Piece>> o, List<java.lang.Double> w, Supplier<Piece> s, double weight, boolean themed) {
        o.add(s);
        w.add(weight * (themed ? 2.4 : 1.0));
    }

    private Daylight() {}
}
