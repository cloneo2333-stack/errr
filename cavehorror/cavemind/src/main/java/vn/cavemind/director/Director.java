package vn.cavemind.director;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.registries.ForgeRegistries;
import vn.cavemind.CaveMind;
import vn.cavemind.Cfg;
import vn.cavemind.sense.Watch;

public final class Director {
    public static final class State {
        public long breatheUntil;
        public String act;
        public long actSince;
        public long mercyUntil, joinedAt, diedAt = Long.MIN_VALUE / 4;
        public String why = "";
        public final List<WeakReference<Entity>> others = new ArrayList<>();
        public final Map<String, long[]> said = new HashMap<>();
    }

    private static final Map<UUID, State> STATES = new HashMap<>();

    public static State state(Player p) {
        return STATES.computeIfAbsent(p.getUUID(), k -> new State());
    }

    public static void forget(UUID id) {
        STATES.remove(id);
        Tension.forget(id);
    }

    public static void clear() {
        STATES.clear();
        Tension.clear();
    }

    public static void tick(ServerPlayer p, Watch w, long now) {
        Tension.tick(p, w, now);
        if (now % 20 != 0) return;
        State s = state(p);
        if (s.act != null && now - s.actSince > 12000) {
            CaveMind.debug("{}: act {} held too long, freed", p.getGameProfile().getName(), s.act);
            s.act = null;
        }
        cleanOthers(s, p);
        if (!s.others.isEmpty() && now % 40 == 0) {
            for (WeakReference<Entity> r : s.others) {
                Entity e = r.get();
                if (e != null && e.distanceToSqr(p) < 32 * 32 && vn.cavemind.sense.Sight.visible(p, e) > 0.2) {
                    Tension.stress(p, 3, "quái khác");
                    Tension.perceived(p);
                    break;
                }
            }
        }
    }

    public static boolean free(ServerPlayer p) {
        String w = blocked(p);
        state(p).why = w == null ? "" : w;
        return w == null;
    }

    public enum Weight { TRACE, GLIMPSE, SCENE, THREAT }

    public static String blocked(ServerPlayer p) {
        return blocked(p, Weight.SCENE);
    }

    public static boolean allows(ServerPlayer p, Weight wt) {
        return blocked(p, wt) == null;
    }

    public static String blocked(ServerPlayer p, Weight wt) {
        State s = state(p);
        long now = p.server.getTickCount();
        if (!p.isAlive()) return "đã chết";
        if (wt == Weight.TRACE) return null;
        if (s.act != null) return "đang có cảnh " + s.act;
        boolean heavy = wt.ordinal() >= Weight.SCENE.ordinal();
        if (heavy && vn.cavemind.life.Glimpse.active(p)) return "đang thoáng thấy";
        String m = mercy(p, wt);
        if (m != null) return m;
        if (heavy && now < s.breatheUntil) return "khoảng thở giữa hai cảnh";
        if (heavy && Cfg.DIRECTOR.get() && !s.others.isEmpty()) return "đang có quái khác quanh bạn";
        for (ServerPlayer o : vn.cavemind.sense.Group.others(p, heavy ? 64 : 40)) {
            State so = STATES.get(o.getUUID());
            if (so != null && so.act != null && !"man".equals(so.act)) return "người chơi gần đó đang có cảnh";
        }
        return null;
    }

    public static String mercy(ServerPlayer p, Weight wt) {
        if (wt == Weight.TRACE) return null;
        Tension.Permit need = wt == Weight.THREAT ? Tension.Permit.ENGAGE : Tension.Permit.APPROACH;
        Tension.Permit have = Tension.permit(p);
        if (have.ordinal() >= need.ordinal()) return null;
        String lim = Tension.limit(p);
        if (lim != null) return lim.replace("!", "");
        return "pha " + Tension.vi(Tension.phase(p)) + " chỉ cho " + Tension.vi(have);
    }

    public static void joined(ServerPlayer p) {
        state(p).joinedAt = p.server.getTickCount();
    }

    public static void calm(net.minecraft.server.MinecraftServer server) {
        long cap = server.getTickCount() + Cfg.DEATH_CALM.get() * 1200L;
        for (State s : STATES.values()) if (s.mercyUntil > cap) s.mercyUntil = cap;
    }

    public static void died(ServerPlayer p) {
        State s = state(p);
        long now = p.server.getTickCount();
        s.diedAt = now;
        s.mercyUntil = now + Cfg.DEATH_CALM.get() * 1200L;
    }

    public static boolean building(ServerPlayer p) {
        if (!Cfg.DIRECTOR.get()) return true;
        Tension.Phase ph = Tension.phase(p);
        return ph == Tension.Phase.PEAK || ph == Tension.Phase.BUILD_UP && Tension.value(p) > 45;
    }

    public static double tension(ServerPlayer p) {
        return Tension.value(p);
    }

    public static boolean claim(ServerPlayer p, String act) {
        State s = state(p);
        if (s.act != null && !s.act.equals(act)) return false;
        s.act = act;
        s.actSince = p.server.getTickCount();
        CaveMind.debug("{} claims {}", p.getGameProfile().getName(), act);
        return true;
    }

    public static boolean holds(ServerPlayer p, String act) {
        return act.equals(state(p).act);
    }

    public static void release(ServerPlayer p, String act, boolean peak) {
        State s = state(p);
        if (s.act == null || !s.act.equals(act)) return;
        s.act = null;
        long now = p.server.getTickCount();
        if (peak && Cfg.DIRECTOR.get()) Tension.climax(p);
        s.breatheUntil = now + (peak ? 1200 : 600) + p.getRandom().nextInt(900);
        CaveMind.debug("{} releases {} peak={}", p.getGameProfile().getName(), act, peak);
    }

    public static void abort(ServerPlayer p, String act) {
        State s = state(p);
        if (s.act == null || !s.act.equals(act)) return;
        s.act = null;
        CaveMind.debug("{} aborts {} (never started)", p.getGameProfile().getName(), act);
    }

    public static void scare(ServerPlayer p, double amount) {
        Tension.stress(p, amount, "sợ");
    }

    public static boolean headliner(Entity e) {
        ResourceLocation id = ForgeRegistries.ENTITY_TYPES.getKey(e.getType());
        if (id == null) return false;
        String full = id.toString(), wild = id.getNamespace() + ":*";
        for (String s : Cfg.HEADLINERS.get()) {
            if (s.equals(full) || s.equals(wild)) return true;
        }
        return false;
    }

    public static boolean allowJoin(ServerLevel lvl, Entity e) {
        if (!Cfg.DIRECTOR.get() || !Cfg.GATE.get()) return true;
        Player near = lvl.getNearestPlayer(e, 112);
        if (!(near instanceof ServerPlayer p) || vn.cavemind.compat.Compat.exempt(p)) return true;
        State s = state(p);
        cleanOthers(s, p);
        long now = p.server.getTickCount();
        ResourceLocation id = ForgeRegistries.ENTITY_TYPES.getKey(e.getType());
        int max = Cfg.MAX_HEADLINERS.get();
        Tension.Phase ph = Tension.phase(p);
        String cat, why;
        if (s.act != null) {
            cat = "act";
            why = "đang có cảnh " + s.act;
        } else if (ph == Tension.Phase.RELEASE || ph == Tension.Phase.FADE) {
            cat = "relax";
            why = "đang " + Tension.vi(ph);
        } else if (s.others.size() >= max) {
            cat = "full";
            why = "đã có " + others(s) + " quanh bạn (tối đa " + max + " con cùng lúc)";
        } else {
            s.others.add(new WeakReference<>(e));
            CaveMind.debug("gate lets {} near {}", id, p.getGameProfile().getName());
            return true;
        }
        long[] said = s.said.computeIfAbsent(id + "|" + cat, k -> new long[] {Long.MIN_VALUE / 2, 0});
        if (now - said[0] >= 1200) {
            CaveMind.debug("gate holds back {} near {}: {} (+{} quiet)", id, p.getGameProfile().getName(), why, said[1]);
            said[0] = now;
            said[1] = 0;
        } else {
            said[1]++;
        }
        return false;
    }

    private static String others(State s) {
        List<String> ids = new ArrayList<>();
        for (WeakReference<Entity> r : s.others) {
            Entity o = r.get();
            if (o != null) ids.add(String.valueOf(ForgeRegistries.ENTITY_TYPES.getKey(o.getType())));
        }
        return String.join(", ", ids);
    }

    public static void left(Entity e) {
        for (State s : STATES.values()) {
            if (s.others.isEmpty()) continue;
            s.others.removeIf(r -> {
                Entity o = r.get();
                return o == null || o == e;
            });
        }
    }

    private static void cleanOthers(State s, ServerPlayer p) {
        Iterator<WeakReference<Entity>> it = s.others.iterator();
        while (it.hasNext()) {
            Entity e = it.next().get();
            if (e == null || e.isRemoved() || !e.isAlive() || e.level() != p.level() || e.distanceToSqr(p) > 160 * 160) it.remove();
        }
    }

    private Director() {}
}
