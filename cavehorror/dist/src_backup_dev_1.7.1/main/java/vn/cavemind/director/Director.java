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
import vn.cavemind.memory.MindData;
import vn.cavemind.memory.Profile;
import vn.cavemind.sense.Watch;

public final class Director {
    public enum Phase { CALM, BUILD, RELAX }

    public static final class State {
        public double tension, target;
        public Phase phase = Phase.CALM;
        public long phaseAt;
        public long relaxUntil;
        public long breatheUntil;
        public String act;
        public long actSince;
        public int buildTicks;
        public double scare;
        public long mercyUntil, scareDay = -1;
        public double scareToday;
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
    }

    public static void clear() {
        STATES.clear();
    }

    public static void tick(ServerPlayer p, Watch w, long now) {
        State s = state(p);
        if (now % 20 != 0) return;
        Profile pr = MindData.profile(p);
        ServerLevel lvl = p.serverLevel();
        double t = 10;
        boolean night = lvl.isNight();
        if (w.underground) t += 24;
        else if (night) t += 20;
        if (lvl.isThundering()) t += 15;
        else if (lvl.isRaining()) t += 7;
        t += Math.max(0, 12 - w.light) * 1.4;
        boolean alone = true;
        for (ServerPlayer o : lvl.players()) {
            if (o != p && !o.isSpectator() && o.distanceToSqr(p) < 40 * 40) {
                alone = false;
                break;
            }
        }
        if (alone) t += 9;
        else t -= 14;
        if (pr.home != null) {
            double d = Math.sqrt(pr.home.distSqr(p.blockPosition()));
            if (d < 20 && w.light >= 9) t -= 26;
            else t += Math.min(18, d / 12);
        }
        if (p.getHealth() < p.getMaxHealth() * 0.4f) t += 14;
        s.scare *= 0.97;
        t += s.scare;
        s.target = Math.max(0, Math.min(100, t));
        s.tension += (s.target - s.tension) * 0.04;

        if (s.act == null) {
            switch (s.phase) {
                case CALM -> {
                    if (s.tension > 38) s.buildTicks += 20;
                    else s.buildTicks = Math.max(0, s.buildTicks - 10);
                    if (s.buildTicks > 600) to(s, Phase.BUILD, now);
                }
                case BUILD -> {
                    if (s.tension < 22) to(s, Phase.CALM, now);
                }
                case RELAX -> {
                    if (now >= s.relaxUntil) {
                        s.tension *= 0.6;
                        to(s, Phase.CALM, now);
                    }
                }
            }
        }
        cleanOthers(s, p);
    }

    private static void to(State s, Phase ph, long now) {
        s.phase = ph;
        s.phaseAt = now;
        s.buildTicks = 0;
    }

    public static boolean free(ServerPlayer p) {
        String w = blocked(p);
        state(p).why = w == null ? "" : w;
        return w == null;
    }

    public static String blocked(ServerPlayer p) {
        State s = state(p);
        long now = p.server.getTickCount();
        boolean dev = vn.cavemind.dev.Dev.on();
        if (s.act != null) return "đang có cảnh " + s.act;
        if (Cfg.DIRECTOR.get()) {
            if (s.phase == Phase.RELAX) return "nghỉ sau cao trào";
            if (now < s.breatheUntil) return "khoảng thở giữa hai cảnh";
            if (!s.others.isEmpty()) return "đang có quái khác quanh bạn";
        }
        if (Cfg.MERCY.get()) {
            if (now < s.mercyUntil) return "bạn vừa chết, được yên một lúc";
            if (p.tickCount < (dev ? 400 : 3600)) return "vừa vào game";
            if (p.containerMenu != p.inventoryMenu) return "đang mở giao diện";
            if (p.isPassenger() || p.isFallFlying()) return "đang cưỡi/bay";
            if (p.getLastHurtByMob() != null && p.tickCount - p.getLastHurtByMobTimestamp() < 300) return "đang giao tranh";
            if (p.getLastHurtMob() != null && p.tickCount - p.getLastHurtMobTimestamp() < 200) return "đang giao tranh";
            if (p.isOnFire() || p.isInLava() || p.getAirSupply() < p.getMaxAirSupply() / 2) return "đang gặp nguy";
            if (Watch.of(p).idle > 2400) return "đang treo máy";
            if (p.serverLevel().getRaidAt(p.blockPosition()) != null) return "đang có raid";
            if (!dev && today(p, s) > budget()) return "đã đủ sợ cho hôm nay";
        }
        for (ServerPlayer o : vn.cavemind.sense.Group.others(p, 64)) {
            State so = STATES.get(o.getUUID());
            if (so != null && so.act != null) return "người chơi gần đó đang có cảnh";
        }
        return null;
    }

    static double budget() {
        return 160 * Cfg.INTENSITY.get();
    }

    private static double today(ServerPlayer p, State s) {
        long day = Dread.day(p.serverLevel());
        if (s.scareDay != day) {
            s.scareDay = day;
            s.scareToday = 0;
        }
        return s.scareToday;
    }

    public static void died(ServerPlayer p) {
        State s = state(p);
        long now = p.server.getTickCount();
        s.mercyUntil = now + (vn.cavemind.dev.Dev.on() ? 1200 : 12000);
        s.tension *= 0.3;
        s.scare = 0;
        vn.cavemind.dev.Dev.say(p, "Bạn chết — mọi thứ để bạn yên " + (s.mercyUntil - now) / 1200 + " phút");
    }

    public static boolean building(ServerPlayer p) {
        if (!Cfg.DIRECTOR.get()) return true;
        State s = state(p);
        return s.phase == Phase.BUILD || s.tension > 45;
    }

    public static double tension(ServerPlayer p) {
        return state(p).tension;
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
        if (peak && Cfg.DIRECTOR.get()) {
            int a = Cfg.RELAX_MIN.get(), b = Math.max(a, Cfg.RELAX_MAX.get());
            s.relaxUntil = now + (vn.cavemind.dev.Dev.on() ? 1200L : (a + p.getRandom().nextInt(b - a + 1)) * 1200L);
            vn.cavemind.dev.Dev.say(p, "Đạo diễn: cao trào xong → nghỉ ngơi " + (s.relaxUntil - now) / 1200 + " phút (không có gì đáng sợ)");
            to(s, Phase.RELAX, now);
        } else {
            s.breatheUntil = now + (vn.cavemind.dev.Dev.on() ? 300 : 900 + p.getRandom().nextInt(1500));
        }
        CaveMind.debug("{} releases {} peak={}", p.getGameProfile().getName(), act, peak);
    }

    public static void abort(ServerPlayer p, String act) {
        State s = state(p);
        if (s.act == null || !s.act.equals(act)) return;
        s.act = null;
        CaveMind.debug("{} aborts {} (never started)", p.getGameProfile().getName(), act);
    }

    public static void scare(ServerPlayer p, double amount) {
        State s = state(p);
        s.scare = Math.min(40, s.scare + amount);
        today(p, s);
        s.scareToday += amount;
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
        String cat, why;
        if (s.act != null) {
            cat = "act";
            why = "đang có cảnh " + s.act;
        } else if (s.phase == Phase.RELAX) {
            cat = "relax";
            why = "đang nghỉ sau cao trào, còn " + Math.max(1, (s.relaxUntil - now + 1199) / 1200) + " phút";
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
            vn.cavemind.dev.Dev.say(p, "Đạo diễn chặn " + id + ": " + why + (said[1] > 0 ? " (thêm " + said[1] + " lần trong phút qua)" : ""));
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
            boolean removed = s.others.removeIf(r -> {
                Entity o = r.get();
                return o == null || o == e;
            });
            if (removed && s.others.isEmpty() && s.act == null && e.getServer() != null) {
                s.breatheUntil = e.getServer().getTickCount() + 1200 + e.level().getRandom().nextInt(1800);
            }
        }
    }

    private static void cleanOthers(State s, ServerPlayer p) {
        Iterator<WeakReference<Entity>> it = s.others.iterator();
        boolean removed = false;
        while (it.hasNext()) {
            Entity e = it.next().get();
            if (e == null || e.isRemoved() || !e.isAlive() || e.level() != p.level() || e.distanceToSqr(p) > 160 * 160) {
                it.remove();
                removed = true;
            }
        }
        if (removed && s.others.isEmpty() && s.act == null) {
            s.breatheUntil = p.server.getTickCount() + 1200 + p.getRandom().nextInt(1800);
        }
    }

    public static com.google.gson.JsonObject eye(ServerPlayer p) {
        State s = state(p);
        com.google.gson.JsonObject o = new com.google.gson.JsonObject();
        o.addProperty("te", Math.round(s.tension));
        o.addProperty("ph", s.phase.name());
        if (s.act != null) o.addProperty("act", s.act);
        String why = blocked(p);
        if (why != null) o.addProperty("why", why);
        if (!s.others.isEmpty()) o.addProperty("oth", s.others.size());
        o.addProperty("sc", Math.round(s.scareToday));
        return o;
    }

    private Director() {}
}
