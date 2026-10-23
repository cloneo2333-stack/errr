package vn.cavemind.hero;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.WallTorchBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import vn.cavemind.CaveMind;
import vn.cavemind.Cfg;
import vn.cavemind.compat.Compat;
import vn.cavemind.director.Director;
import vn.cavemind.director.Dread;
import vn.cavemind.man.ManMind;
import vn.cavemind.memory.MindData;
import vn.cavemind.memory.Moment;
import vn.cavemind.memory.Profile;
import vn.cavemind.net.Net;
import vn.cavemind.sense.Sight;
import vn.cavemind.sense.Watch;
import vn.cavemind.world.Fx;
import vn.cavemind.world.Spots;

public final class Herobrine {
    public enum Kind { HORIZON, WALKER, MINER, BUILDER, REPLAY, BED, SHOULDER, FACE }

    static final int SLOT = 1;
    static final int FLEE = 90, SNEAK = 91, STAND = 92, BLINK = 93;

    static final class Show {
        final Kind kind;
        long start, modeAt;
        int mode;
        Vec3 pos = Vec3.ZERO;
        float yaw, head, pitch;
        boolean spawned, crouch, eyes = true, shoulder, sleeping, reacted, done;
        String skin;
        boolean noticed;
        double perceive, distAtNotice;
        int away, stare;
        long noticedAt;
        List<Vec3> path;
        double along, speed;
        double stepAcc;
        BlockPos mouth, face, bed;
        Direction dir;
        int dug, maxDig, hits;
        long next;
        List<BlockPos> plan;
        List<BlockState> states;
        int built;
        boolean replica;
        int tapeAgo, tapeLen, played, lastFlags = -1, lastItem = -1;
        UUID source;
        float turnFrom, turnTo;
        int turnLeft;
        float face0;
        Vec3 faceAt;
        boolean faced;

        Show(Kind kind) {
            this.kind = kind;
        }
    }

    private static final Map<UUID, Show> SHOWS = new HashMap<>();
    private static final Map<UUID, String> LAST = new HashMap<>();

    public static boolean active(ServerPlayer p) {
        return SHOWS.containsKey(p.getUUID());
    }

    public static void forget(ServerPlayer p) {
        Show s = SHOWS.remove(p.getUUID());
        if (s != null && s.spawned) send(p, Net.Hero.REMOVE, s, 0, "", 0);
        Ledger.forget(p.getUUID());
        LAST.remove(p.getUUID());
        Echo.forget(p.getUUID());
    }

    public static void clear() {
        SHOWS.clear();
        LAST.clear();
        Ledger.clear();
        Echo.clear();
    }

    public static boolean enabled() {
        return Cfg.HERO.get() && Compat.heroLoaded();
    }

    private static final Map<UUID, Long> FACE_COOL = new HashMap<>();

    public static void tick(ServerPlayer p, Watch w, long now) {
        if (!enabled()) return;
        Profile pr = MindData.profile(p);
        Ledger.record(p, w, pr, now);
        if (now % 20 == 5 && !SHOWS.containsKey(p.getUUID())) maybeFace(p, w, pr, now);
        Show s = SHOWS.get(p.getUUID());
        if (s != null) {
            boolean keep;
            try {
                keep = !Compat.exempt(p) && p.isAlive() && p.level().dimension() == Level.OVERWORLD && run(p, w, pr, s, now);
            } catch (Throwable t) {
                CaveMind.LOG.warn("[CaveMind] herobrine show failed", t);
                keep = false;
            }
            if (!keep) finish(p, pr, s);
            return;
        }
        Echo.tick(p, w, pr, now);
    }

    static int tier(Profile pr, long day) {
        long d = pr.firstDay >= 0 ? day - pr.firstDay : 0;
        boolean miner = pr.lore.contains("m");
        if (d >= Cfg.HERO_TIER2.get() || miner && d >= Cfg.HERO_TIER1.get()) return 2;
        return d >= Cfg.HERO_TIER1.get() || miner ? 1 : 0;
    }

    static long gap(ServerPlayer p) {
        RandomSource r = p.getRandom();
        double a = Cfg.HERO_GAP_MIN.get(), b = Math.max(a, Cfg.HERO_GAP_MAX.get());
        return (long) ((a + r.nextDouble() * (b - a)) * 24000 / (Cfg.INTENSITY.get() * Cfg.HERO_FREQ.get()));
    }

    private static void plan(ServerPlayer p, Watch w, Profile pr, long now) {
        ServerLevel lvl = p.serverLevel();
        if (lvl.dimension() != Level.OVERWORLD || Compat.exempt(p)) return;
        long day = Dread.day(lvl);
        if (day < Cfg.HERO_START_DAY.get()) return;
        if (pr.heroNext < 0) pr.heroNext = pr.playTicks + gap(p) / 2;
        if (pr.playTicks < pr.heroNext) return;
        if (!SHOWS.isEmpty()) return;
        String k = pick(p, w, pr, day);
        if (k == null) {
            pr.heroNext = pr.playTicks + (1200);
            return;
        }
        boolean ok = start(p, w, pr, k, now, false);
        if (ok) {
            pr.heroLast.put(k, day);
            pr.heroShows++;
            pr.heroNext = pr.playTicks + gap(p);
            LAST.put(p.getUUID(), k);
        } else {
            pr.heroNext = pr.playTicks + (1200);
        }
    }

    public static String describe(ServerPlayer p) {
        if (!enabled()) return "tắt";
        Profile pr = MindData.profile(p);
        Show s = SHOWS.get(p.getUUID());
        String cur = s != null ? "đang " + vi(s.kind) : "đang im";
        long left = pr.heroNext < 0 ? -1 : Math.max(0, (pr.heroNext - pr.playTicks) / 20);
        return cur + ", lần tới sau " + left + "s chơi, đã hiện " + pr.heroShows + " lần, bị thấy " + pr.heroSeen;
    }

    public static boolean busy(ServerPlayer p) {
        return SHOWS.containsKey(p.getUUID());
    }

    public static boolean perform(ServerPlayer p) {
        if (!enabled() || SHOWS.containsKey(p.getUUID())) return false;
        Watch w = Watch.of(p);
        Profile pr = MindData.profile(p);
        long day = Dread.day(p.serverLevel()), now = p.server.getTickCount();
        java.util.Set<String> tried = new java.util.HashSet<>();
        for (int i = 0; i < 4; i++) {
            String k = pick(p, w, pr, day);
            if (k == null || !tried.add(k)) continue;
            if (start(p, w, pr, k, now, false)) {
                done(p, pr, k, day);
                return true;
            }
        }
        for (String k : new String[]{"sign", "tunnel", "leafless"}) {
            if (tried.contains(k) || !Cfg.HERO_EDITS.get()) continue;
            if (start(p, w, pr, k, now, false)) {
                done(p, pr, k, day);
                return true;
            }
        }
        return false;
    }

    private static void done(ServerPlayer p, Profile pr, String k, long day) {
        pr.heroLast.put(k, day);
        pr.heroShows++;
        pr.heroNext = pr.playTicks + gap(p);
        LAST.put(p.getUUID(), k);
    }

    static String pick(ServerPlayer p, Watch w, Profile pr, long day) {
        int tier = tier(pr, day);
        List<String> o = new ArrayList<>();
        List<Double> wt = new ArrayList<>();
        vn.cavemind.director.Tension.Phase ph = vn.cavemind.director.Tension.phase(p);
        boolean window = ph == vn.cavemind.director.Tension.Phase.BUILD_UP || ph == vn.cavemind.director.Tension.Phase.FADE;
        boolean stage = window && Director.state(p).act == null && vn.cavemind.director.Tension.limit(p) == null && !ManMind.active(p) && Net.modded(p);
        ServerLevel lvl = p.serverLevel();
        if (stage) {
            if (w.outside) add(o, wt, "horizon", 1.0);
            if (w.outside && tier >= 1) add(o, wt, "walker", 0.9);
            if (w.underground) add(o, wt, "miner", 1.3);
            if (w.outside && tier >= 1) add(o, wt, "builder", 0.8);
            if (tier >= 1 && Ledger.size(p) > (3600)) add(o, wt, "replay", 1.1);
            if (tier >= 2 && pr.home != null && lvl.getBlockState(pr.home).getBlock() instanceof BedBlock) {
                double d = Math.sqrt(pr.home.distSqr(p.blockPosition()));
                if (d > 30 && d < 140) add(o, wt, "bed", 0.8);
            }
            if (tier >= 2) add(o, wt, "shoulder", 0.35);
        }
        if (Cfg.HERO_EDITS.get() && ph != vn.cavemind.director.Tension.Phase.RELEASE) {
            add(o, wt, "sign", 0.9);
            if (pr.brokenLog.size() >= 8) add(o, wt, "tunnel", 0.8);
            if (pr.home != null) add(o, wt, "leafless", 0.5);
        }
        String last = LAST.get(p.getUUID());
        double sum = 0;
        for (int i = 0; i < o.size(); i++) {
            String k = o.get(i);
            long cd = k.equals("shoulder") ? 10 : k.equals("sign") || k.equals("tunnel") || k.equals("leafless") ? 3 : 4;
            Long used = pr.heroLast.get(k);
            if (used != null && day - used < cd) wt.set(i, 0.0);
            if (k.equals(last)) wt.set(i, wt.get(i) * 0.2);
            sum += wt.get(i);
        }
        if (sum <= 0) return null;
        double x = p.getRandom().nextDouble() * sum;
        for (int i = 0; i < o.size(); i++) {
            x -= wt.get(i);
            if (x <= 0 && wt.get(i) > 0) return o.get(i);
        }
        return null;
    }

    private static void add(List<String> o, List<Double> w, String k, double v) {
        o.add(k);
        w.add(v);
    }

    public static boolean start(ServerPlayer p, Watch w, Profile pr, String k, long now, boolean force) {
        switch (k) {
            case "sign", "tunnel", "leafless" -> {
                boolean ok = Traces.run(p, w, pr, k, force);
                return ok;
            }
            default -> {}
        }
        Kind kind;
        try {
            kind = Kind.valueOf(k.toUpperCase(java.util.Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return false;
        }
        if (!Net.modded(p)) return false;
        if (force) {
            Show old = SHOWS.remove(p.getUUID());
            if (old != null) finish(p, pr, old);
            Director.state(p).act = null;
        }
        if (!Director.claim(p, "hero")) return false;
        Show s = new Show(kind);
        s.start = s.modeAt = now;
        s.skin = Cfg.HERO_SKIN.get();
        boolean ok;
        try {
            ok = switch (kind) {
                case HORIZON -> setupHorizon(p, w, s);
                case WALKER -> setupWalker(p, w, s);
                case MINER -> setupMiner(p, w, s);
                case BUILDER -> setupBuilder(p, w, pr, s);
                case REPLAY -> setupReplay(p, w, pr, s);
                case BED -> setupBed(p, pr, s);
                case SHOULDER -> setupShoulder(p, s);
                case FACE -> setupFace(p, w, s);
            };
        } catch (Throwable t) {
            CaveMind.LOG.warn("[CaveMind] herobrine setup failed", t);
            ok = false;
        }
        if (!ok) {
            if (s.spawned) send(p, Net.Hero.REMOVE, s, 0, "", 0);
            Director.abort(p, "hero");
            CaveMind.debug("herobrine {} could not start for {}", kind, p.getGameProfile().getName());
            return false;
        }
        if (s.skin.equals(Cfg.HERO_SKIN.get()) && (kind == Kind.HORIZON || kind == Kind.WALKER) && s.spawned) {
            ServerPlayer twin = twin(p);
            if (twin != null && p.getRandom().nextFloat() < 0.4f) {
                s.skin = "player:" + twin.getUUID();
                s.eyes = false;
                send(p, Net.Hero.REMOVE, s, 0, "", 0);
                spawn(p, s, s.pos, s.yaw, null);
            }
        }
        SHOWS.put(p.getUUID(), s);
        CaveMind.debug("herobrine {} begins for {}", kind, p.getGameProfile().getName());
        return true;
    }

    static String vi(Kind k) {
        return switch (k) {
            case HORIZON -> "đứng im trên đường chân trời";
            case WALKER -> "đi ngang qua tầm mắt như một người chơi, dừng lại nhìn bạn";
            case MINER -> "đang đào hầm trong đá (nghe tiếng cuốc)";
            case BUILDER -> "đang xây gì đó ở đằng xa";
            case REPLAY -> "lặp lại đúng đường bạn đã đi vài phút trước";
            case BED -> "có người đang ngủ trên giường của bạn";
            case SHOULDER -> "đứng sau lưng bạn, chỉ thấy khi bật góc nhìn thứ ba (F5)";
            case FACE -> "bản sao của bạn bước tới đứng đối mặt";
        };
    }

    private static void finish(ServerPlayer p, Profile pr, Show s) {
        SHOWS.remove(p.getUUID());
        if (s.spawned) send(p, Net.Hero.REMOVE, s, 0, "", 0);
        Director.release(p, "hero", s.noticed);
        if (s.noticed) {
            ServerLevel lvl = p.serverLevel();
            pr.heroSeen++;
            pr.moment(new Moment(Moment.HERO, Dread.day(lvl), Dread.time(lvl), BlockPos.containing(s.pos), (int) s.distAtNotice, s.kind.ordinal()));
            Compat.grant(p, "watching:main/spotted_herobrine");
        }
        CaveMind.debug("herobrine {} ends for {} (seen={})", s.kind, p.getGameProfile().getName(), s.noticed);
    }

    public static void act(ServerPlayer p, int op, int slot) {
        Show s = SHOWS.get(p.getUUID());
        if (s == null || !s.spawned) return;
        Profile pr = MindData.profile(p);
        if (op == Net.HeroAct.HIT) {
            pr.bold(6);
            if (!s.noticed) notice(p, s, p.server.getTickCount());
            Net.fx(p, Net.PULSE, 2.5f, 6);
            String say = Echo.saying(p);
            if (say != null && Cfg.HERO_EDITS.get() && !s.sleeping && !s.shoulder) {
                List<net.minecraft.network.chat.Component> ls = new ArrayList<>();
                StringBuilder cur = new StringBuilder();
                for (String word : say.split(" ")) {
                    if (cur.length() + word.length() + 1 > 15 && cur.length() > 0) {
                        ls.add(net.minecraft.network.chat.Component.literal(cur.toString()));
                        cur.setLength(0);
                    }
                    if (cur.length() > 0) cur.append(' ');
                    cur.append(word.length() > 15 ? word.substring(0, 15) : word);
                    if (ls.size() >= 4) break;
                }
                if (cur.length() > 0 && ls.size() < 4) ls.add(net.minecraft.network.chat.Component.literal(cur.toString()));
                Traces.place(p, Watch.of(p), BlockPos.containing(s.pos), p.position(), ls.toArray(new net.minecraft.network.chat.Component[0]));
            }
            finish(p, pr, s);
        } else if (op == Net.HeroAct.SEEN && s.kind == Kind.SHOULDER && !s.noticed) {
            notice(p, s, p.server.getTickCount());
            s.mode = 1;
            s.modeAt = p.server.getTickCount();
            Director.scare(p, 14);
        }
    }

    static void send(ServerPlayer p, int op, Show s, int flags, String str, long data) {
        Net.hero(p, new Net.Hero(op, SLOT, s.pos.x, s.pos.y, s.pos.z, s.yaw, s.head, s.pitch, flags, str, data));
    }

    static int flags(Show s) {
        return (s.eyes ? 1 : 0) | (s.shoulder ? 2 : 0) | (s.crouch ? 4 : 0);
    }

    static void spawn(ServerPlayer p, Show s, Vec3 at, float yaw, String item) {
        s.pos = at;
        s.yaw = s.head = yaw;
        s.pitch = 0;
        send(p, Net.Hero.SPAWN, s, flags(s), "self".equals(s.skin) ? "self" : s.skin, 0);
        s.spawned = true;
        if (item != null) send(p, Net.Hero.ITEM, s, 0, item, 0);
    }

    static void move(ServerPlayer p, Show s, Vec3 at, float yaw, float head, float pitch, int steps) {
        s.pos = at;
        s.yaw = yaw;
        s.head = head;
        s.pitch = pitch;
        send(p, Net.Hero.MOVE, s, 0, "", steps);
    }

    static void state(ServerPlayer p, Show s) {
        send(p, Net.Hero.STATE, s, flags(s), "", 0);
    }

    static float yawTo(Vec3 from, Vec3 to) {
        return (float) (Mth.atan2(to.z - from.z, to.x - from.x) * (180f / Math.PI)) - 90f;
    }

    static float pitchTo(Vec3 from, Vec3 to) {
        double dx = to.x - from.x, dy = to.y - from.y, dz = to.z - from.z;
        return (float) -(Mth.atan2(dy, Math.sqrt(dx * dx + dz * dz)) * (180f / Math.PI));
    }

    static AABB box(Show s) {
        if (s.sleeping) return new AABB(s.pos.x - 1, s.pos.y - 0.1, s.pos.z - 1, s.pos.x + 1, s.pos.y + 0.5, s.pos.z + 1);
        double h = s.crouch ? 1.5 : 1.8;
        return new AABB(s.pos.x - 0.3, s.pos.y, s.pos.z - 0.3, s.pos.x + 0.3, s.pos.y + h, s.pos.z + 0.3);
    }

    static boolean perceive(ServerPlayer p, Watch w, Show s, long now) {
        AABB b = box(s);
        Sight si = Sight.of(p, w, b.getCenter().add(0, b.getYsize() * 0.3, 0));
        double vis = Sight.visible(p, b);
        boolean on = si.on && vis > 0.15;
        s.away = on ? 0 : s.away + 1;
        double dist = s.pos.distanceTo(p.position());
        if (!s.noticed) {
            double distF = Mth.clamp(1.5 - dist / 110.0, 0.35, 1.0);
            if (on) s.perceive += (si.central() ? 1.0 : 0.35) * vis * distF * 1.6;
            else s.perceive = Math.max(0, s.perceive - 0.2);
            if (s.perceive >= 5) notice(p, s, now);
        } else if (on && si.edge() < 0.6) {
            s.stare++;
        }
        return on;
    }

    static void notice(ServerPlayer p, Show s, long now) {
        s.noticed = true;
        s.noticedAt = now;
        s.distAtNotice = s.pos.distanceTo(p.position());
        s.stare = 0;
        vn.cavemind.director.Tension.seen(p, s.distAtNotice, "Herobrine");
        vn.cavemind.director.PaceLog.encounter(p, "hero", "seen " + s.kind, (int) s.distAtNotice + "m");
    }

    private static boolean run(ServerPlayer p, Watch w, Profile pr, Show s, long now) {
        if (s.kind == Kind.BED && !s.spawned) return armBed(p, w, pr, s, now);
        if (s.kind == Kind.SHOULDER) return runShoulder(p, w, s, now);
        if (s.kind == Kind.FACE) return runFace(p, w, pr, s, now);
        boolean on = perceive(p, w, s, now);
        double dist = s.pos.distanceTo(p.position());
        if (dist > 160) return false;
        if (s.mode >= FLEE) return react(p, w, pr, s, on, dist, now);
        if (s.noticed && !s.reacted && on && dist < s.distAtNotice - 10 && w.speed > 0.03 && s.kind != Kind.BED) {
            s.reacted = true;
            approach(p, pr, s, now);
            return true;
        }
        return switch (s.kind) {
            case HORIZON -> runHorizon(p, w, s, on, dist, now);
            case WALKER -> runWalker(p, w, s, on, dist, now);
            case MINER -> runMiner(p, w, pr, s, on, dist, now);
            case BUILDER -> runBuilder(p, w, pr, s, on, dist, now);
            case REPLAY -> runReplay(p, w, s, on, dist, now);
            case BED -> runBed(p, s, on, dist, now);
            default -> false;
        };
    }

    static void approach(ServerPlayer p, Profile pr, Show s, long now) {
        pr.bold(4);
        double b = pr.bold / 100.0;
        String[] k = {"blink", "flee", "sneak", "stand"};
        double[] wt = {1.0 - b * 0.5, 0.6, b * 0.9, 0.4 + b * 0.3};
        double sum = 0;
        for (double d : wt) sum += d;
        double x = p.getRandom().nextDouble() * sum;
        int i = 0;
        for (; i < k.length - 1; i++) {
            x -= wt[i];
            if (x <= 0) break;
        }
        s.mode = switch (k[i]) {
            case "flee" -> FLEE;
            case "sneak" -> SNEAK;
            case "stand" -> STAND;
            default -> BLINK;
        };
        s.modeAt = now;
        s.path = null;
    }

    static boolean react(ServerPlayer p, Watch w, Profile pr, Show s, boolean on, double dist, long now) {
        ServerLevel lvl = p.serverLevel();
        switch (s.mode) {
            case FLEE -> {
                if (s.path == null) {
                    Vec3 away = s.pos.subtract(p.position()).multiply(1, 0, 1).normalize();
                    s.path = line(lvl, s.pos, s.pos.add(away.scale(30)));
                    s.along = 0;
                    if (s.crouch) {
                        s.crouch = false;
                        state(p, s);
                    }
                }
                boolean more = walk(p, s, 0.29, null);
                return !((s.away >= 2 && now - s.modeAt > 8) || !more && s.away >= 1 || now - s.modeAt > 200);
            }
            case SNEAK -> {
                if (!s.crouch) {
                    s.crouch = true;
                    state(p, s);
                }
                if (dist < 6 || s.away >= 2) {
                    Fx.sound(p, Holder.direct(SoundEvents.ARMOR_EQUIP_LEATHER), SoundSource.PLAYERS, s.pos.add(0, 1, 0), 0.6f, 0.6f);
                    return false;
                }
                Vec3 to = p.position().subtract(s.pos).multiply(1, 0, 1);
                if (to.lengthSqr() > 1e-3) {
                    Vec3 nx = s.pos.add(to.normalize().scale(0.065));
                    BlockPos g = ground(lvl, nx);
                    Vec3 at = g != null ? new Vec3(nx.x, g.getY(), nx.z) : s.pos;
                    float y = yawTo(s.pos, p.position());
                    move(p, s, at, y, y, pitchTo(at.add(0, 1.5, 0), p.getEyePosition()), 2);
                }
                return now - s.modeAt < 400;
            }
            case STAND -> {
                face(p, s);
                return !(dist < 4.5 || s.away >= 3 || now - s.modeAt > 600);
            }
            default -> {
                face(p, s);
                return s.away < 1 && now - s.modeAt < 400;
            }
        }
    }

    static void face(ServerPlayer p, Show s) {
        float y = yawTo(s.pos, p.position());
        if (Math.abs(Mth.wrapDegrees(y - s.yaw)) > 1 || Math.abs(Mth.wrapDegrees(y - s.head)) > 1) {
            move(p, s, s.pos, y, y, pitchTo(s.pos.add(0, 1.6, 0), p.getEyePosition()), 3);
        }
    }

    static void turnTowards(ServerPlayer p, Show s, float target, float rate) {
        float d = Mth.wrapDegrees(target - s.yaw);
        float st = Mth.clamp(d, -rate, rate);
        float y = s.yaw + st;
        move(p, s, s.pos, y, y, s.pitch * 0.9f, 2);
    }

    static BlockPos ground(ServerLevel lvl, Vec3 at) {
        int x = Mth.floor(at.x), z = Mth.floor(at.z);
        BlockPos c = Spots.nearFloor(lvl, BlockPos.containing(at.x, at.y + 0.5, at.z), 2, 2);
        if (c != null) return c;
        int y = lvl.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
        return Math.abs(y - at.y) <= 2 ? new BlockPos(x, y, z) : null;
    }

    static List<Vec3> line(ServerLevel lvl, Vec3 a, Vec3 b) {
        List<Vec3> out = new ArrayList<>();
        Vec3 d = b.subtract(a).multiply(1, 0, 1);
        double len = d.length();
        if (len < 1e-3) return out;
        Vec3 u = d.normalize();
        double y = a.y;
        out.add(a);
        for (double t = 0.5; t <= len; t += 0.5) {
            Vec3 q = a.add(u.scale(t));
            BlockPos g = ground(lvl, new Vec3(q.x, y, q.z));
            if (g == null || Math.abs(g.getY() - y) > 1.1 || !lvl.getFluidState(g).isEmpty() || !lvl.getFluidState(g.below()).isEmpty()) break;
            y = g.getY();
            out.add(new Vec3(q.x, y, q.z));
        }
        return out;
    }

    static boolean walk(ServerPlayer p, Show s, double speed, Float lookYaw) {
        if (s.path == null || s.path.size() < 2) return false;
        s.along += speed / 0.5;
        int i = (int) s.along;
        if (i >= s.path.size() - 1) {
            Vec3 last = s.path.get(s.path.size() - 1);
            move(p, s, last, s.yaw, lookYaw != null ? lookYaw : s.yaw, 0, 3);
            return false;
        }
        double f = s.along - i;
        Vec3 a = s.path.get(i), b = s.path.get(i + 1);
        Vec3 at = a.lerp(b, f);
        float y = yawTo(a, b);
        double moved = at.distanceTo(s.pos);
        move(p, s, at, y, lookYaw != null ? lookYaw : y, 0, 3);
        s.stepAcc += moved;
        if (s.stepAcc > 1.7 && at.distanceTo(p.position()) < 34) {
            s.stepAcc = 0;
            ServerLevel lvl = p.serverLevel();
            BlockPos floor = BlockPos.containing(at.x, at.y - 0.5, at.z);
            SoundType st = lvl.getBlockState(floor).getSoundType(lvl, floor, null);
            Fx.sound(p, Holder.direct(st.getStepSound()), SoundSource.PLAYERS, at, st.getVolume() * 0.5f, st.getPitch());
        }
        return true;
    }

    private static boolean setupHorizon(ServerPlayer p, Watch w, Show s) {
        boolean day = p.serverLevel().isDay();
        double a = day ? 70 : 30, b = day ? 105 : 55;
        Spots.Cand c = Spots.find(p, w, new Spots.Spec().ring(a, b).dir(0, 130).body(0.6, 1.8).tries(70)
            .when(k -> !k.sight.on && k.vis >= 0.45 && k.feet.getY() >= p.getY() - 3 && Math.abs(k.feet.getY() - p.getY()) < 34)
            .rank(k -> (k.feet.getY() - p.getY()) * 0.04 + k.vis + (k.cover ? 0 : 0.2)));
        if (c == null) return false;
        Vec3 at = Vec3.atBottomCenterOf(c.feet);
        spawn(p, s, at, yawTo(at, p.position()), null);
        return true;
    }

    private static boolean runHorizon(ServerPlayer p, Watch w, Show s, boolean on, double dist, long now) {
        if (!s.noticed) {
            if (now % 10 == 0) {
                float y = yawTo(s.pos, p.position());
                float h = s.yaw + Mth.clamp(Mth.wrapDegrees(y - s.yaw), -60, 60);
                move(p, s, s.pos, s.yaw, h, pitchTo(s.pos.add(0, 1.6, 0), p.getEyePosition()), 10);
            }
            return now - s.start < 2600;
        }
        switch (s.mode) {
            case 0 -> {
                if (s.away >= 3) return false;
                if (s.stare >= 70) {
                    s.mode = 1;
                    s.modeAt = now;
                    Vec3 away = s.pos.subtract(p.position()).multiply(1, 0, 1).normalize();
                    s.path = line(p.serverLevel(), s.pos, s.pos.add(away.scale(14)));
                    s.along = 0;
                    s.turnTo = yawTo(p.position(), s.pos);
                }
                return now - s.noticedAt < 1200;
            }
            default -> {
                if (Math.abs(Mth.wrapDegrees(s.turnTo - s.yaw)) > 4 && now - s.modeAt < 30) {
                    turnTowards(p, s, s.turnTo, 7);
                    return true;
                }
                boolean more = walk(p, s, 0.17, null);
                return !(s.away >= 2 || !more && s.away >= 1 || now - s.modeAt > 500);
            }
        }
    }

    private static boolean setupWalker(ServerPlayer p, Watch w, Show s) {
        ServerLevel lvl = p.serverLevel();
        RandomSource r = p.getRandom();
        double side = Math.abs(w.yawSpeed) > 2 ? -Math.signum(w.yawSpeed) : r.nextBoolean() ? 1 : -1;
        for (int tries = 0; tries < 16; tries++) {
            if (tries == 8) side = -side;
            double sight = vn.cavemind.sense.Range.of(p, w);
            double d = sight * 0.55 + r.nextDouble() * sight * 0.3;
            double th = side * (w.hHalf() + Math.atan2(3 + r.nextDouble() * 4, d));
            double yaw = Math.toRadians(p.getYRot());
            Vec3 f = new Vec3(-Math.sin(yaw), 0, Math.cos(yaw));
            Vec3 right = new Vec3(-f.z, 0, f.x);
            Vec3 a = p.position().add(f.scale(d * Math.cos(th))).add(right.scale(d * Math.sin(th)));
            BlockPos g = Spots.surface(lvl, Mth.floor(a.x), Mth.floor(a.z), 2);
            if (g == null) continue;
            Vec3 start = Vec3.atBottomCenterOf(g);
            Spots.Cand c = Spots.eval(p, w, g, 0.6, 1.8);
            if (c.sight.on && c.vis > 0) continue;
            Vec3 u = right.scale(-side);
            List<Vec3> path = line(lvl, start, start.add(u.scale(44)));
            if (path.size() < 36) continue;
            s.path = path;
            s.along = 0;
            s.speed = 0.215;
            spawn(p, s, start, yawTo(start, start.add(u)), null);
            return true;
        }
        return false;
    }

    private static boolean runWalker(ServerPlayer p, Watch w, Show s, boolean on, double dist, long now) {
        switch (s.mode) {
            case 0 -> {
                if (s.noticed && now - s.noticedAt > 12) {
                    s.mode = 1;
                    s.modeAt = now;
                    s.next = 40 + p.getRandom().nextInt(40);
                    return true;
                }
                if (p.getRandom().nextInt(200) == 0) s.speed = s.speed > 0.25 ? 0.215 : 0.28;
                boolean more = walk(p, s, s.speed, null);
                if (!more) return s.away < 1 && now - s.start < 2400;
                return now - s.start < 3000;
            }
            case 1 -> {
                float look = yawTo(s.pos, p.position());
                float h = s.yaw + Mth.clamp(Mth.wrapDegrees(look - s.yaw), -85, 85);
                if (Math.abs(Mth.wrapDegrees(h - s.head)) > 2) move(p, s, s.pos, s.yaw, s.head + Mth.clamp(Mth.wrapDegrees(h - s.head), -9, 9),
                    pitchTo(s.pos.add(0, 1.6, 0), p.getEyePosition()), 2);
                if (now - s.modeAt > s.next) {
                    s.mode = 2;
                    s.modeAt = now;
                }
                return true;
            }
            default -> {
                boolean more = walk(p, s, 0.25, null);
                if (!more) return s.away < 1 && now - s.modeAt < 400;
                return now - s.modeAt < 1200;
            }
        }
    }

    private static boolean natural(ServerLevel lvl, BlockPos q) {
        BlockState st = lvl.getBlockState(q);
        if (st.hasBlockEntity() || !st.isCollisionShapeFullBlock(lvl, q)) return false;
        if (!(st.is(BlockTags.BASE_STONE_OVERWORLD) || st.is(BlockTags.DIRT) || st.is(Blocks.GRAVEL) || st.is(BlockTags.STONE_ORE_REPLACEABLES)
            || st.is(BlockTags.DEEPSLATE_ORE_REPLACEABLES) || st.is(Blocks.COBBLESTONE))) return false;
        for (Direction d : Direction.values()) if (!lvl.getFluidState(q.relative(d)).isEmpty()) return false;
        return true;
    }

    private static boolean setupMiner(ServerPlayer p, Watch w, Show s) {
        ServerLevel lvl = p.serverLevel();
        RandomSource r = p.getRandom();
        for (int tries = 0; tries < 60; tries++) {
            double a = r.nextDouble() * Math.PI * 2, d = 10 + r.nextDouble() * 14;
            BlockPos q = Spots.caveFloor(lvl, Mth.floor(p.getX() + Math.cos(a) * d), Mth.floor(p.getY()), Mth.floor(p.getZ() + Math.sin(a) * d), 2);
            if (q == null || Math.abs(q.getY() - p.getY()) > 6) continue;
            Direction dir = Direction.getNearest(q.getX() - p.getX(), 0, q.getZ() - p.getZ());
            if (dir.getAxis().isVertical()) continue;
            boolean ok = true;
            for (int k = 1; k <= 14 && ok; k++) ok = natural(lvl, q.relative(dir, k)) && natural(lvl, q.relative(dir, k).above());
            if (!ok) continue;
            if (Traces.seen(p, w, q) || Traces.seen(p, w, q.above())) continue;
            for (int k = 1; k <= 2; k++) {
                lvl.setBlock(q.relative(dir, k), Blocks.AIR.defaultBlockState(), 3);
                lvl.setBlock(q.relative(dir, k).above(), Blocks.AIR.defaultBlockState(), 3);
            }
            s.mouth = q;
            s.dir = dir;
            s.face = q.relative(dir, 3);
            s.dug = 2;
            s.maxDig = 16 + r.nextInt(10);
            s.next = 0;
            String pick = r.nextFloat() < 0.3f ? "minecraft:diamond_pickaxe" : "minecraft:iron_pickaxe";
            spawn(p, s, Vec3.atBottomCenterOf(q.relative(dir, 2)), dir.toYRot(), pick);
            return true;
        }
        return false;
    }

    private static boolean runMiner(ServerPlayer p, Watch w, Profile pr, Show s, boolean on, double dist, long now) {
        ServerLevel lvl = p.serverLevel();
        if (s.noticed && s.mode == 0) {
            s.mode = 1;
            s.modeAt = now;
        }
        switch (s.mode) {
            case 0 -> {
                if (s.dug >= s.maxDig) {
                    s.mode = 3;
                    s.modeAt = now;
                    Traces.clue(p, pr, Moment.HERO_TUNNEL, s.mouth, 10);
                    return true;
                }
                if (now < s.next) return true;
                BlockState st = lvl.getBlockState(s.face);
                if (s.hits < 3) {
                    send(p, Net.Hero.SWING, s, 0, "", 0);
                    SoundType t = st.getSoundType(lvl, s.face, null);
                    Fx.sound(p, Holder.direct(t.getHitSound()), SoundSource.BLOCKS, Vec3.atCenterOf(s.face), 1.6f, t.getPitch() * 0.8f);
                    s.hits++;
                    s.next = now + 5;
                    return true;
                }
                if (!natural(lvl, s.face) || !natural(lvl, s.face.above()) && !lvl.getBlockState(s.face.above()).isAir()) {
                    s.mode = 3;
                    s.modeAt = now;
                    return true;
                }
                send(p, Net.Hero.SWING, s, 0, "", 0);
                for (BlockPos q : new BlockPos[]{s.face, s.face.above()}) {
                    BlockState b = lvl.getBlockState(q);
                    if (b.isAir()) continue;
                    lvl.levelEvent(2001, q, Block.getId(b));
                    lvl.setBlock(q, Blocks.AIR.defaultBlockState(), 3);
                }
                s.dug++;
                s.hits = 0;
                s.next = now + 9 + p.getRandom().nextInt(9);
                Vec3 at = Vec3.atBottomCenterOf(s.face);
                move(p, s, at, s.dir.toYRot(), s.dir.toYRot(), 20, 4);
                if (s.dug % 7 == 0) {
                    Direction side = p.getRandom().nextBoolean() ? s.dir.getClockWise() : s.dir.getCounterClockWise();
                    BlockPos t = s.face.relative(s.dir.getOpposite(), 2).above();
                    if (lvl.getBlockState(t).isAir() && lvl.getBlockState(t.relative(side)).isFaceSturdy(lvl, t.relative(side), side.getOpposite())) {
                        lvl.setBlock(t, Blocks.WALL_TORCH.defaultBlockState().setValue(WallTorchBlock.FACING, side.getOpposite()), 3);
                        Fx.sound(p, Holder.direct(SoundEvents.WOOD_PLACE), SoundSource.BLOCKS, Vec3.atCenterOf(t), 1.2f, 1f);
                    }
                }
                s.face = s.face.relative(s.dir);
                return now - s.start < 6000;
            }
            case 1 -> {
                if (now - s.modeAt < 20) return true;
                float target = yawTo(s.pos, p.position());
                if (Math.abs(Mth.wrapDegrees(target - s.yaw)) > 5) {
                    turnTowards(p, s, target, 6);
                    return true;
                }
                s.mode = 2;
                s.modeAt = now;
                return true;
            }
            case 2 -> {
                face(p, s);
                return !(s.away >= 2 || dist < 5 || now - s.modeAt > 900);
            }
            default -> {
                if (s.noticed) {
                    s.mode = 1;
                    s.modeAt = now;
                    return true;
                }
                return now - s.modeAt < 1800;
            }
        }
    }

    private static boolean setupBuilder(ServerPlayer p, Watch w, Profile pr, Show s) {
        ServerLevel lvl = p.serverLevel();
        Spots.Cand c = Spots.find(p, w, new Spots.Spec().ring(38, 70).dir(0, 180).body(0.6, 1.8).tries(60)
            .when(k -> !k.sight.on && k.vis >= 0.4 && flat(lvl, k.feet)).rank(k -> k.vis));
        if (c == null) return false;
        BlockPos site = c.feet;
        List<BlockPos> plan = new ArrayList<>();
        List<BlockState> states = new ArrayList<>();
        s.replica = replica(lvl, pr, site, plan, states, p.getRandom());
        if (!s.replica) {
            plan.clear();
            states.clear();
            for (int dx = -1; dx <= 1; dx++) for (int dz = -1; dz <= 1; dz++) {
                plan.add(site.offset(dx, 0, dz));
                states.add(Blocks.SAND.defaultBlockState());
            }
            plan.add(site.above());
            states.add(Blocks.SAND.defaultBlockState());
        }
        s.plan = plan;
        s.states = states;
        s.built = 0;
        s.next = 0;
        Vec3 stand = Vec3.atBottomCenterOf(site.offset(-3, 0, -2));
        BlockPos g = ground(lvl, stand);
        if (g != null) stand = Vec3.atBottomCenterOf(g);
        spawn(p, s, stand, yawTo(stand, Vec3.atCenterOf(site)), BuiltInRegistries.ITEM.getKey(states.get(0).getBlock().asItem()).toString());
        return true;
    }

    private static boolean flat(ServerLevel lvl, BlockPos f) {
        for (int dx = -2; dx <= 2; dx++) for (int dz = -2; dz <= 2; dz++) {
            int y = lvl.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, f.getX() + dx, f.getZ() + dz);
            if (Math.abs(y - f.getY()) > 1) return false;
        }
        return lvl.getFluidState(f.below()).isEmpty();
    }

    private static boolean replica(ServerLevel lvl, Profile pr, BlockPos site, List<BlockPos> plan, List<BlockState> states, RandomSource r) {
        List<long[]> log = new ArrayList<>(pr.placedLog);
        if (log.size() < 10) return false;
        BlockPos anchor = BlockPos.of(log.get(log.size() - 1)[0]);
        List<long[]> cl = new ArrayList<>();
        for (int i = log.size() - 1; i >= 0 && cl.size() < 48; i--) {
            BlockPos q = BlockPos.of(log.get(i)[0]);
            if (Math.abs(q.getX() - anchor.getX()) <= 5 && Math.abs(q.getY() - anchor.getY()) <= 5 && Math.abs(q.getZ() - anchor.getZ()) <= 5) cl.add(log.get(i));
        }
        if (cl.size() < 8) return false;
        int mx = Integer.MAX_VALUE, my = Integer.MAX_VALUE, mz = Integer.MAX_VALUE;
        for (long[] e : cl) {
            BlockPos q = BlockPos.of(e[0]);
            mx = Math.min(mx, q.getX());
            my = Math.min(my, q.getY());
            mz = Math.min(mz, q.getZ());
        }
        Rotation rot = Rotation.values()[r.nextInt(4)];
        for (long[] e : cl) {
            BlockState st = Block.stateById((int) e[1]);
            if (st.isAir() || st.hasBlockEntity() || st.getBlock() instanceof DoorBlock || st.getBlock() instanceof BedBlock) continue;
            BlockPos q = BlockPos.of(e[0]);
            BlockPos off = new BlockPos(q.getX() - mx - 2, q.getY() - my, q.getZ() - mz - 2).rotate(rot);
            BlockPos at = site.offset(off);
            if (plan.contains(at)) continue;
            plan.add(at);
            states.add(st.rotate(rot));
        }
        if (plan.size() < 6) return false;
        List<Integer> idx = new ArrayList<>();
        for (int i = 0; i < plan.size(); i++) idx.add(i);
        idx.sort(Comparator.comparingInt(i -> plan.get(i).getY()));
        List<BlockPos> p2 = new ArrayList<>();
        List<BlockState> s2 = new ArrayList<>();
        for (int i : idx) {
            p2.add(plan.get(i));
            s2.add(states.get(i));
        }
        plan.clear();
        plan.addAll(p2);
        states.clear();
        states.addAll(s2);
        return true;
    }

    private static boolean runBuilder(ServerPlayer p, Watch w, Profile pr, Show s, boolean on, double dist, long now) {
        ServerLevel lvl = p.serverLevel();
        if (s.noticed && s.mode == 0) {
            s.mode = 1;
            s.modeAt = now;
        }
        switch (s.mode) {
            case 0 -> {
                if (s.built >= s.plan.size()) {
                    s.mode = 3;
                    s.modeAt = now;
                    Traces.clue(p, pr, Moment.HERO_BUILD, s.plan.get(0), 20);
                    return true;
                }
                if (now < s.next) return true;
                BlockPos q = s.plan.get(s.built);
                BlockState st = s.states.get(s.built);
                s.built++;
                s.next = now + 11 + p.getRandom().nextInt(7);
                BlockState cur = lvl.getBlockState(q);
                if (!cur.canBeReplaced() || !st.canSurvive(lvl, q)) return true;
                if (s.pos.distanceTo(Vec3.atCenterOf(q)) > 4.6) {
                    Vec3 dirTo = Vec3.atBottomCenterOf(q).subtract(s.pos).multiply(1, 0, 1);
                    Vec3 stand = Vec3.atBottomCenterOf(q).subtract(dirTo.normalize().scale(3));
                    BlockPos g = ground(lvl, stand);
                    if (g != null) move(p, s, Vec3.atBottomCenterOf(g), s.yaw, s.head, s.pitch, 8);
                }
                String item = BuiltInRegistries.ITEM.getKey(st.getBlock().asItem()).toString();
                send(p, Net.Hero.ITEM, s, 0, item, 0);
                float y = yawTo(s.pos, Vec3.atCenterOf(q));
                move(p, s, s.pos, y, y, pitchTo(s.pos.add(0, 1.6, 0), Vec3.atCenterOf(q)), 3);
                send(p, Net.Hero.SWING, s, 0, "", 0);
                lvl.setBlock(q, st, 3);
                SoundType t = st.getSoundType(lvl, q, null);
                Fx.sound(p, Holder.direct(t.getPlaceSound()), SoundSource.BLOCKS, Vec3.atCenterOf(q), 2.6f, t.getPitch() * 0.8f);
                return now - s.start < 5000;
            }
            case 1 -> {
                float target = yawTo(s.pos, p.position());
                if (Math.abs(Mth.wrapDegrees(target - s.yaw)) > 5 && now - s.modeAt < 60) {
                    turnTowards(p, s, target, 5);
                    return true;
                }
                s.mode = 2;
                s.modeAt = now;
                return true;
            }
            case 2 -> {
                face(p, s);
                return !(s.away >= 2 || dist < 28 || now - s.modeAt > 900);
            }
            default -> {
                face(p, s);
                if (s.noticed) {
                    s.mode = 2;
                    s.modeAt = now;
                }
                return now - s.modeAt < 900;
            }
        }
    }

    private static boolean setupReplay(ServerPlayer p, Watch w, Profile pr, Show s) {
        RandomSource r = p.getRandom();
        List<ServerPlayer> sources = new ArrayList<>();
        sources.add(p);
        for (ServerPlayer o : p.serverLevel().players()) if (o != p && !o.isSpectator()) sources.add(o);
        if (sources.size() > 1 && r.nextFloat() < 0.5f) Collections.swap(sources, 0, 1 + r.nextInt(sources.size() - 1));
        int min = 2400;
        for (ServerPlayer src : sources) {
            int size = Ledger.size(src);
            if (size <= min + 200) continue;
            for (int tries = 0; tries < 30; tries++) {
                int a0 = min + r.nextInt(size - min - 1);
                int len = Math.min(a0, 150 + r.nextInt(250));
                if (len < 100) continue;
                Ledger.Frame f0 = Ledger.frame(src, a0);
                if (f0 == null || f0.has(Ledger.AWAY) || f0.has(Ledger.ASLEEP)) continue;
                double d0 = f0.pos().distanceTo(p.position());
                if (d0 < 20 || d0 > 75) continue;
                AABB b0 = new AABB(f0.pos().x - 0.3, f0.pos().y, f0.pos().z - 0.3, f0.pos().x + 0.3, f0.pos().y + 1.8, f0.pos().z + 0.3);
                if (Sight.of(p, w, b0.getCenter()).on && Sight.visible(p, b0) > 0) continue;
                boolean ok = true;
                int visible = 0;
                for (int k = 0; k <= len && ok; k += 25) {
                    Ledger.Frame f = Ledger.frame(src, a0 - k);
                    if (f == null || f.has(Ledger.AWAY) || f.has(Ledger.ASLEEP)) {
                        ok = false;
                        break;
                    }
                    double d = f.pos().distanceTo(p.position());
                    if (d < 14 || d > 90) ok = false;
                    for (ServerPlayer o : p.serverLevel().players()) if (o != p && o.position().distanceTo(f.pos()) < 10) ok = false;
                    AABB b = new AABB(f.pos().x - 0.3, f.pos().y, f.pos().z - 0.3, f.pos().x + 0.3, f.pos().y + 1.8, f.pos().z + 0.3);
                    if (Sight.visible(p, b) >= 0.35) visible++;
                }
                if (!ok || visible < 2) continue;
                s.source = src.getUUID();
                s.tapeAgo = a0;
                s.tapeLen = len;
                s.played = 0;
                String face;
                if (src != p) {
                    s.skin = r.nextFloat() < 0.75f ? "player:" + src.getUUID() : s.skin;
                    face = s.skin.startsWith("player:") ? " — mang khuôn mặt của " + src.getGameProfile().getName() : " — theo dấu " + src.getGameProfile().getName();
                    s.eyes = !s.skin.startsWith("player:");
                } else if (tier(pr, Dread.day(p.serverLevel())) >= 2 && r.nextFloat() < 0.6f) {
                    s.skin = "self";
                    s.eyes = false;
                    face = " — mang khuôn mặt CỦA BẠN";
                } else {
                    face = "";
                }
                spawn(p, s, f0.pos(), f0.yaw(), BuiltInRegistries.ITEM.getKey(BuiltInRegistries.ITEM.byId(f0.item())).toString());
                s.lastItem = f0.item();
                return true;
            }
        }
        return false;
    }

    private static boolean runReplay(ServerPlayer p, Watch w, Show s, boolean on, double dist, long now) {
        if (s.noticed && s.mode == 0 && s.stare >= 40) {
            s.mode = 1;
            s.modeAt = now;
            s.turnTo = yawTo(s.pos, p.position());
        }
        switch (s.mode) {
            case 0 -> {
                if (now % Ledger.STEP != 0) return true;
                ServerPlayer src = s.source == null ? p : p.server.getPlayerList().getPlayer(s.source);
                Ledger.Frame f = src == null ? null : Ledger.frame(src, s.tapeAgo);
                if (f == null || f.has(Ledger.AWAY) || f.has(Ledger.ASLEEP) || s.played >= s.tapeLen) {
                    s.mode = 3;
                    s.modeAt = now;
                    return true;
                }
                s.played++;
                move(p, s, f.pos(), f.yaw(), f.yaw(), f.pitch(), Ledger.STEP);
                boolean crouch = f.has(Ledger.CROUCH);
                if (crouch != s.crouch) {
                    s.crouch = crouch;
                    state(p, s);
                }
                if (f.has(Ledger.SWING) && (s.lastFlags & Ledger.SWING) == 0) send(p, Net.Hero.SWING, s, 0, "", 0);
                s.lastFlags = f.flags();
                if (f.item() != s.lastItem) {
                    s.lastItem = f.item();
                    send(p, Net.Hero.ITEM, s, 0, BuiltInRegistries.ITEM.getKey(BuiltInRegistries.ITEM.byId(f.item())).toString(), 0);
                }
                if (dist < 8) return false;
                return true;
            }
            case 1 -> {
                if (Math.abs(Mth.wrapDegrees(s.turnTo - s.yaw)) > 4 && now - s.modeAt < 40) {
                    turnTowards(p, s, s.turnTo, 9);
                    return true;
                }
                if (!s.eyes) {
                    s.eyes = true;
                    state(p, s);
                }
                s.mode = 2;
                s.modeAt = now;
                return true;
            }
            case 2 -> {
                face(p, s);
                return !(s.away >= 2 || dist < 8 || now - s.modeAt > 700);
            }
            default -> {
                if (s.noticed && s.stare >= 20) {
                    s.mode = 1;
                    s.modeAt = now;
                    s.turnTo = yawTo(s.pos, p.position());
                    return true;
                }
                return !(s.away >= 2 && now - s.modeAt > 10 || now - s.modeAt > 600);
            }
        }
    }

    private static boolean setupBed(ServerPlayer p, Profile pr, Show s) {
        if (pr.home == null || !(p.serverLevel().getBlockState(pr.home).getBlock() instanceof BedBlock)) return false;
        s.bed = pr.home;
        if (tier(pr, Dread.day(p.serverLevel())) >= 2 && p.getRandom().nextFloat() < 0.5f) s.skin = "self";
        s.eyes = false;
        return true;
    }

    private static boolean armBed(ServerPlayer p, Watch w, Profile pr, Show s, long now) {
        if (now - s.start > (9000)) return false;
        if (now % 10 != 0) return true;
        ServerLevel lvl = p.serverLevel();
        if (!(lvl.getBlockState(s.bed).getBlock() instanceof BedBlock) || p.isSleeping()) return false;
        double d = Math.sqrt(s.bed.distSqr(p.blockPosition()));
        if (d < 6 || d > 36 || Traces.seen(p, w, s.bed)) return true;
        s.sleeping = true;
        spawn(p, s, Vec3.atBottomCenterOf(s.bed).add(0, 0.6, 0), 0, null);
        send(p, Net.Hero.SLEEP, s, 0, "", s.bed.asLong());
        s.modeAt = now;
        return true;
    }

    private static boolean runBed(ServerPlayer p, Show s, boolean on, double dist, long now) {
        if (p.isSleeping()) return false;
        if (s.mode == 0) {
            if (!s.noticed) return now - s.modeAt < 4000;
            if (s.stare >= 40 || dist <= 4.5) {
                ServerLevel lvl = p.serverLevel();
                BlockState st = lvl.getBlockState(s.bed);
                Vec3 standAt = Vec3.atBottomCenterOf(s.bed);
                if (st.getBlock() instanceof BedBlock) {
                    standAt = BedBlock.findStandUpPosition(net.minecraft.world.entity.EntityType.PLAYER, lvl, s.bed, st.getValue(BedBlock.FACING), 0).orElse(standAt);
                }
                s.sleeping = false;
                s.eyes = true;
                s.pos = standAt;
                s.yaw = s.head = yawTo(standAt, p.position());
                send(p, Net.Hero.WAKE, s, 0, "", 0);
                state(p, s);
                s.mode = 2;
                s.modeAt = now;
            }
            return true;
        }
        face(p, s);
        return !(s.away >= 2 || dist < 2.5 || now - s.modeAt > 700);
    }

    static void maybeFace(ServerPlayer p, Watch w, Profile pr, long now) {
        if (pr.heroPeace || !pr.lore.contains("m") || pr.heroSeen < 1 || Compat.exempt(p) || !Net.modded(p)) return;
        ServerLevel lvl = p.serverLevel();
        if (lvl.dimension() != Level.OVERWORLD || now < FACE_COOL.getOrDefault(p.getUUID(), 0L)) return;
        int tod = Dread.time(lvl);
        if (tod < 17000 || tod > 19200) return;
        BlockPos mine = vn.cavemind.lore.Lore.site(lvl, "m");
        if (mine == null) return;
        double dx = p.getX() - mine.getX(), dz = p.getZ() - mine.getZ();
        if (dx * dx + dz * dz > 22 * 22 || p.getY() > mine.getY() - 6) return;
        if (w.stillTicks < 60 || Director.state(p).act != null || ManMind.active(p)) return;
        if (start(p, w, pr, "face", now, false)) {
            FACE_COOL.put(p.getUUID(), now + 1200);
            vn.cavemind.director.PaceLog.encounter(p, "hero", "face", "midnight in the mine");
        }
    }

    private static boolean setupFace(ServerPlayer p, Watch w, Show s) {
        ServerLevel lvl = p.serverLevel();
        Vec3 f = Vec3.directionFromRotation(0, p.getYRot());
        for (int d = 10; d >= 5; d--) {
            Vec3 a = p.position().add(f.scale(d));
            BlockPos g = ground(lvl, a);
            if (g == null) continue;
            Vec3 at = Vec3.atBottomCenterOf(g);
            if (!Sight.clear(lvl, p.getEyePosition(), at.add(0, 1.6, 0), p)) continue;
            List<Vec3> path = line(lvl, at, p.position());
            if (path.size() < 6) continue;
            s.skin = "self";
            s.eyes = false;
            s.path = path;
            s.along = 0;
            spawn(p, s, at, yawTo(at, p.position()), null);
            return true;
        }
        return false;
    }

    private static boolean runFace(ServerPlayer p, Watch w, Profile pr, Show s, long now) {
        double dist = s.pos.distanceTo(p.position());
        switch (s.mode) {
            case 0 -> {
                face(p, s);
                float look = yawTo(p.position(), s.pos);
                if (Math.abs(Mth.wrapDegrees(look - p.getYRot())) > 50 || now - s.start > 900) return faceFail(p, s);
                if (dist <= 2.4) {
                    s.mode = 1;
                    s.modeAt = now;
                    s.face0 = p.getYRot();
                    s.faceAt = p.position();
                    p.displayClientMessage(net.minecraft.network.chat.Component.literal("Đừng chớp mắt.").withStyle(net.minecraft.ChatFormatting.GRAY, net.minecraft.ChatFormatting.ITALIC), true);
                    return true;
                }
                walk(p, s, 0.055, yawTo(s.pos, p.position()));
                return true;
            }
            case 1 -> {
                face(p, s);
                if (Math.abs(Mth.wrapDegrees(p.getYRot() - s.face0)) > 25 || p.position().distanceTo(s.faceAt) > 0.8) return faceFail(p, s);
                if (now - s.modeAt >= 400) {
                    s.mode = 2;
                    s.modeAt = now;
                    s.eyes = true;
                    state(p, s);
                    p.displayClientMessage(net.minecraft.network.chat.Component.literal("Nó chớp mắt trước.").withStyle(net.minecraft.ChatFormatting.WHITE, net.minecraft.ChatFormatting.ITALIC), true);
                    Fx.sound(p, Holder.direct(SoundEvents.AMETHYST_BLOCK_RESONATE), SoundSource.AMBIENT, s.pos.add(0, 1.6, 0), 0.8f, 0.5f);
                }
                return true;
            }
            default -> {
                if (now - s.modeAt < 40) return true;
                pr.heroPeace = true;
                pr.heroPressure = 0;
                s.noticed = true;
                Compat.grant(p, "cavemind:ban_suong/ai_chop_mat_truoc");
                vn.cavemind.lore.Endings.check(p);
                CaveMind.debug("{} outlasted their copy", p.getGameProfile().getName());
                return false;
            }
        }
    }

    private static boolean faceFail(ServerPlayer p, Show s) {
        p.displayClientMessage(net.minecraft.network.chat.Component.literal("Bạn chớp mắt trước.").withStyle(net.minecraft.ChatFormatting.DARK_GRAY, net.minecraft.ChatFormatting.ITALIC), true);
        FACE_COOL.put(p.getUUID(), p.server.getTickCount() + 24000L);
        return false;
    }

    private static boolean setupShoulder(ServerPlayer p, Show s) {
        s.shoulder = true;
        s.next = 600 + p.getRandom().nextInt(500);
        spawn(p, s, behind(p), p.getYRot(), null);
        return true;
    }

    static Vec3 behind(ServerPlayer p) {
        double yaw = Math.toRadians(p.getYRot());
        Vec3 f = new Vec3(-Math.sin(yaw), 0, Math.cos(yaw));
        return p.position().subtract(f.scale(2.3));
    }

    private static boolean runShoulder(ServerPlayer p, Watch w, Show s, long now) {
        move(p, s, behind(p), p.getYRot(), p.getYHeadRot(), p.getXRot() * 0.5f, 2);
        if (w.stepped && p.getRandom().nextFloat() < 0.3f) {
            Vec3 at = s.pos;
            ServerLevel lvl = p.serverLevel();
            Fx.later(lvl, 3, () -> Fx.sound(p, Holder.direct(SoundEvents.ARMOR_EQUIP_LEATHER), SoundSource.PLAYERS, at, 0.15f, 0.7f));
        }
        if (s.mode == 1) return now - s.modeAt < 120;
        return now - s.start < s.next;
    }

    static ServerPlayer twin(ServerPlayer p) {
        List<ServerPlayer> c = new ArrayList<>();
        for (ServerPlayer o : p.server.getPlayerList().getPlayers()) {
            if (o == p || o.isSpectator()) continue;
            if (o.level() != p.level() || o.distanceTo(p) > 40 || Watch.of(o).underground) c.add(o);
        }
        return c.isEmpty() ? null : c.get(p.getRandom().nextInt(c.size()));
    }

    public static void died(ServerPlayer p, boolean calm) {
        Profile pr = MindData.profile(p);
        Show s = SHOWS.get(p.getUUID());
        if (s != null) finish(p, pr, s);
        if (calm) pr.heroNext = Math.max(pr.heroNext, pr.playTicks + Cfg.DEATH_CALM.get() * 1200L);
    }

    public static void summoned(ServerPlayer p) {
        Profile pr = MindData.profile(p);
        pr.heroNext = pr.playTicks + 300 + p.getRandom().nextInt(600);
        pr.heroLast.remove("horizon");
        pr.heroLast.remove("walker");
        LAST.remove(p.getUUID());
        Director.scare(p, 10);
        CaveMind.debug("{} lit the shrine", p.getGameProfile().getName());
    }

    private Herobrine() {}
}
