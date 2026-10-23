package vn.cavemind.man;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.phys.Vec3;
import vn.cavemind.CaveMind;
import vn.cavemind.Cfg;
import vn.cavemind.compat.Compat;
import vn.cavemind.director.Director;
import vn.cavemind.director.Dread;
import vn.cavemind.director.Tension;
import vn.cavemind.memory.MindData;
import vn.cavemind.memory.Moment;
import vn.cavemind.memory.Profile;
import vn.cavemind.net.Net;
import vn.cavemind.sense.Sight;
import vn.cavemind.sense.Watch;
import vn.cavemind.world.Fx;
import vn.cavemind.world.Spots;

public final class ManMind {
    public enum Tac { WATCH, PEEK, SHADOW, PERIPHERAL, AHEAD, BEHIND, WINDOW, KNOCK, HUNT, TRACK, DOING, PEEP, WITNESS, LURE, MARK, DOUSE, DRAG }

    enum Mode { STAND, STEP_ASIDE, RETREAT, STARE_WALK, LEAVING, SCUTTLE, FEINT, HOLD, FOGGED }

    static final double W = 0.6, H = 3.0;

    static final class Session {
        long start, end;
        int budget, seen, misses;
        Tac tac;
        long tacStart, nextTac;
        Mode mode = Mode.STAND;
        UUID body;
        boolean noticed;
        double perceive;
        int stare, away, onTicks;
        long noticedAt;
        double distAtNotice;
        boolean reacted;
        int relocations;
        boolean zoomed;
        int provoked;
        boolean hunted;
        boolean wantHunt;
        double lastVis;
        BlockPos hide;
        long modeAt;
        Vec3 anchor;
        long nextFollow;
        BlockPos door;
        int knocks;
        long knockAt;
        boolean doorOpened;
        Hunt hunt;
        final Deque<Tac> recent = new ArrayDeque<>();
        int turnsSeen;
        Lure lure;
        BlockPos site;
        String siteKind;
        long stepUntil;
        boolean tilted, twitched, soon;
        UUID marked;
        List<BlockPos> lights;
        int doused;
        long nextDouse;
        Vec3 dragTo, dragDir;
        boolean dragged;
        boolean relaxed;
        boolean walking;
        vn.cavemind.path.Walker walker;
        Float arriveYaw;
        String arrivePose;
        long travelUntil, retargetAt;
        String cause = "";
    }

    private static final Map<UUID, Session> SESSIONS = new HashMap<>();

    public static void forget(ServerPlayer p) {
        Session s = SESSIONS.remove(p.getUUID());
        if (s != null) {
            discard(p.serverLevel(), s.body);
            if (s.hunt != null) s.hunt.discard(p.serverLevel());
        }
    }

    public static void clear() {
        SESSIONS.clear();
    }

    public static boolean anyActive() {
        return !SESSIONS.isEmpty();
    }

    public static boolean active(ServerPlayer p) {
        return SESSIONS.containsKey(p.getUUID());
    }

    public static void tick(ServerPlayer p, Watch w, long now) {
        if (!Compat.manActive()) return;
        Profile pr = MindData.profile(p);
        Schemes.tick(p, w, pr, now);
        Session s = SESSIONS.get(p.getUUID());
        if (s == null) return;
        if (!p.isAlive() || vn.cavemind.compat.Compat.exempt(p) || p.level().dimension() != Level.OVERWORLD) {
            leave(p, pr, s, now, false);
            return;
        }
        if (s.hunt == null && now % 20 == 0 && crowded(p, s, now)) return;
        if (s.hunt != null) {
            if (now % 20 == 0 && s.hunt.phase > 0) {
                vn.cavemind.director.Tension.chased(p);
                if (vn.cavemind.director.Tension.permit(p).ordinal() < vn.cavemind.director.Tension.Permit.ENGAGE.ordinal() && pr.anger < 60 && !Meter.huntDue(p)) {
                    s.hunt.end = Math.min(s.hunt.end, now + 300);
                }
            }
            if (!s.hunt.tick(p, w, now)) {
                s.hunt.discard(p.serverLevel());
                s.hunt.record(p, pr);
                s.hunt = null;
                leave(p, pr, s, now, true);
            }
            return;
        }
        boolean urged = Meter.huntDue(p);
        if (!urged && now % 20 == 0 && vn.cavemind.director.Tension.permit(p).ordinal() <= vn.cavemind.director.Tension.Permit.WITHDRAW.ordinal()) {
            CaveMind.debug("the Man backs off from {}: {}", p.getGameProfile().getName(), Tension.vi(Tension.phase(p)));
            leave(p, pr, s, now, false);
            return;
        }
        if (s.tac == null) {
            if (s.walker != null) travel(p, w, pr, s, now);
            if (p.isSleeping()) {
                s.nextTac = Math.max(s.nextTac, now + 100);
                s.end = Math.max(s.end, now + 200);
                return;
            }
            if (urged && !s.hunted && now - s.start > 500 && huntAllowed(p, w, pr, s)) {
                startHunt(p, w, pr, s, now, (BlockPos) null, "đêm săn");
                return;
            }
            if (s.wantHunt && huntAllowed(p, w, pr, s)) {
                startHunt(p, w, pr, s, now, (BlockPos) null, pr.anger >= 60 ? "hắn đang giận" : "bị khiêu khích");
                return;
            }
            if (now >= s.end || s.seen >= s.budget) {
                if (!s.hunted && huntAllowed(p, w, pr, s) && p.getRandom().nextFloat() < 0.3f + pr.bold / 250f) {
                    startHunt(p, w, pr, s, now, (BlockPos) null, "kết thúc cuộc rình");
                    return;
                }
                leave(p, pr, s, now, false);
                return;
            }
            if (now >= s.nextTac) begin(p, w, pr, s, now);
            return;
        }
        boolean keep;
        try {
            keep = run(p, w, pr, s, now);
        } catch (Throwable t) {
            CaveMind.LOG.warn("[CaveMind] man tactic failed", t);
            keep = false;
        }
        if (!keep) endTac(p, s, now);
    }

    public static boolean arrive(ServerPlayer p, Entity body, String cause) {
        if (!Compat.manActive() || !SESSIONS.isEmpty() || Compat.exempt(p) || p.level().dimension() != Level.OVERWORLD) return false;
        Watch w = Watch.of(p);
        Profile pr = MindData.profile(p);
        long now = p.server.getTickCount();
        if (!Director.claim(p, "man")) return false;
        Session s = new Session();
        RandomSource r = p.getRandom();
        s.start = now;
        s.end = now + (4 + r.nextInt(5)) * 1200L;
        s.budget = 2 + r.nextInt(2) + pr.manLevel / 4;
        s.nextTac = now + 20 + r.nextInt(40);
        s.walking = true;
        s.body = body.getUUID();
        s.cause = cause;
        s.wantHunt = pr.anger >= 60 && (p.serverLevel().isNight() || w.underground);
        SESSIONS.put(p.getUUID(), s);
        if (Meter.due(pr) == 2) Meter.complete(p, 2);
        if (Meter.huntDue(p)) s.end = Math.max(s.end, now + 9600);
        if (body instanceof Mob m) vn.cavemind.world.Motion.pose(m, "cm_hold", now);
        if (Cfg.FOG.get()) Net.fx(p, Net.FOG, w.underground ? 0.25f : 0.45f, 600);
        vn.cavemind.director.PaceLog.encounter(p, "man", "arrive", cause);
        CaveMind.debug("the Man walks in toward {} from {}m ({}, level {})", p.getGameProfile().getName(), (int) body.distanceTo(p), cause, pr.manLevel);
        return true;
    }

    public static Vec3 where(net.minecraft.server.MinecraftServer server) {
        for (Map.Entry<UUID, Session> en : SESSIONS.entrySet()) {
            ServerPlayer p = server.getPlayerList().getPlayer(en.getKey());
            if (p == null) continue;
            Session s = en.getValue();
            Entity e = s.hunt != null ? s.hunt.body(p.serverLevel()) : body(p, s);
            if (e != null) return e.position();
        }
        return null;
    }

    public static Entity walkingBody(ServerPlayer p) {
        Session s = SESSIONS.get(p.getUUID());
        return s == null ? null : body(p, s);
    }

    public static String describe(ServerPlayer p) {
        Session s = SESSIONS.get(p.getUUID());
        if (s == null) return null;
        Entity e = body(p, s);
        long now = p.server.getTickCount();
        String where = e == null ? "không có thân" : (int) e.distanceTo(p) + "m";
        String tac = s.hunt != null ? "CƠN ĐIÊN" : s.tac == null ? "đang chọn việc" : vi(s.tac);
        String walk = s.walker != null ? (s.walker.frozen ? ", đứng im vì bị nhìn" : ", đang đi tới chỗ") : "";
        return tac + " (" + where + walk + "), đã bị thấy " + s.seen + " lần, còn " + Math.max(0, (s.end - now) / 20) + "s, vì " + s.cause;
    }

    static boolean home(ServerPlayer ringer, BlockPos at) {
        Profile pr = MindData.profile(ringer);
        if (pr.manPeace || !pr.lore.contains("s1") || !pr.lore.contains("s2") || !pr.lore.contains("s3")) return false;
        ServerLevel lvl = ringer.serverLevel();
        BlockPos st = vn.cavemind.lore.Lore.site(lvl, "s3");
        if (st == null || st.distSqr(at) > 14 * 14) return false;
        Session s = SESSIONS.get(ringer.getUUID());
        if (s == null || s.seen < 1 || s.hunt != null) return false;
        int tod = Dread.time(lvl);
        if (tod < 12500 || tod > 23500) return false;
        pr.manPeace = true;
        pr.anger = 0;
        Entity e = body(ringer, s);
        if (e != null) Fx.sound(ringer, "man:long_whisper", SoundSource.HOSTILE, e.getEyePosition(), 0.3f, 0.6f);
        ringer.displayClientMessage(net.minecraft.network.chat.Component.literal("Tiếng chuông sương gọi thợ săn về nhà. Lần này, hắn nghe.").withStyle(net.minecraft.ChatFormatting.GRAY, net.minecraft.ChatFormatting.ITALIC), true);
        if (Cfg.FOG.get()) Net.fx(ringer, Net.FOG, 0f, 200);
        if (e instanceof Mob m && s.walking) {
            s.walker = null;
            s.body = null;
            vn.cavemind.world.Motion.stop(m);
            net.minecraft.server.MinecraftServer server = ringer.server;
            vn.cavemind.path.Leaving.add(m, st, 0.26, "cm_walk", 52, gone -> vn.cavemind.life.Life.dissolved(server, gone));
        }
        leave(ringer, pr, s, ringer.server.getTickCount(), false);
        Compat.grant(ringer, "cavemind:ban_suong/goi_tho_san_ve_nha");
        vn.cavemind.lore.Endings.check(ringer);
        vn.cavemind.director.PaceLog.encounter(ringer, "man", "home", "the fog bell");
        CaveMind.debug("{} called the hunter home", ringer.getGameProfile().getName());
        return true;
    }

    public static void bell(ServerPlayer ringer, BlockPos at) {
        ServerLevel lvl = ringer.serverLevel();
        long now = ringer.server.getTickCount();
        Vec3 c = Vec3.atCenterOf(at);
        if (home(ringer, at)) return;
        if (SESSIONS.isEmpty()) vn.cavemind.life.Life.bell(ringer, at);
        for (Map.Entry<UUID, Session> en : new ArrayList<>(SESSIONS.entrySet())) {
            ServerPlayer p = ringer.server.getPlayerList().getPlayer(en.getKey());
            Session s = en.getValue();
            if (p == null || p.serverLevel() != lvl) continue;
            Entity e = s.hunt != null ? s.hunt.body(lvl) : s.body == null ? null : lvl.getEntity(s.body);
            boolean near = e != null && e.position().distanceTo(c) < 64 || p.position().distanceTo(c) < 40;
            if (!near) continue;
            Profile pr = MindData.profile(p);
            long day = Dread.day(lvl);
            if (pr.bellDay >= 0) pr.bellHeat = Math.max(0, pr.bellHeat - (day - pr.bellDay));
            pr.bellDay = day;
            Profile rp = MindData.profile(ringer);
            if (s.hunt != null) {
                if (s.hunt.stun(lvl, now)) {
                    pr.bellHeat += 0.5;
                    rp.moment(new Moment(Moment.BELL, day, Dread.time(lvl), at, 0, 1));
                }
                continue;
            }
            double chance = Mth.clamp(1.0 - pr.bellHeat * 0.28, 0.12, 1.0) * (MindData.profile(ringer).lore.contains("s1") ? 1.0 : 0.55);
            if (ringer.getRandom().nextDouble() < chance) {
                Meter.bell(p);
                pr.bellHeat += 1;
                pr.anger(3);
                vn.cavemind.life.Life.hurt(ringer.server, 15);
                if (e != null) Fx.sound(p, "man:vanish", SoundSource.HOSTILE, e.position(), 0.45f, 0.8f);
                if (Cfg.FOG.get()) Net.fx(p, Net.FOG, 1.7f, 14);
                Session mine = s;
                Fx.later(lvl, 30, () -> {
                    if (SESSIONS.get(p.getUUID()) == mine) {
                        leave(p, pr, mine, p.server.getTickCount(), false);
                        pr.manNext += 2400;
                    }
                });
                rp.moment(new Moment(Moment.BELL, day, Dread.time(lvl), at, 0, 1));
            } else {
                pr.anger(8);
                if (e instanceof Mob) mode(p, s, Mode.HOLD, now);
                rp.moment(new Moment(Moment.BELL, day, Dread.time(lvl), at, 0, 0));
            }
        }
    }

    public static void dismiss(ServerPlayer p) {
        Session s = SESSIONS.get(p.getUUID());
        if (s != null) leave(p, MindData.profile(p), s, p.server.getTickCount(), false);
    }

    private static void leave(ServerPlayer p, Profile pr, Session s, long now, boolean peak) {
        depart(p, s);
        if (s.hunt != null) s.hunt.discard(p.serverLevel());
        SESSIONS.remove(p.getUUID());
        if (Cfg.FOG.get()) Net.fx(p, Net.FOG, 0f, 400);
        pr.manSessions++;
        if (Meter.due(pr) == 3) Meter.complete(p, 3);
        if (s.hunted && Meter.due(pr) == 4) Meter.complete(p, 4);
        long day = Dread.day(p.serverLevel());
        if (pr.firstDay < 0) pr.firstDay = day;
        int cap = 1 + (int) ((day - pr.firstDay) * 0.8) + pr.manSessions / 3;
        if (s.seen > 0 && pr.manLevel < Math.min(10, cap)) pr.manLevel++;
        double rest = vn.cavemind.life.Life.left(p, s.seen > 0, peak || s.hunted);
        Director.release(p, "man", peak || s.hunted);
        vn.cavemind.director.PaceLog.encounter(p, "man", "leave", "seen " + s.seen + (s.hunted ? ", frenzy" : "") + ", rest " + (int) rest + "m");
        CaveMind.debug("the Man leaves {} (seen {}, level {}, rests {} min)", p.getGameProfile().getName(), s.seen, pr.manLevel, (int) rest);
    }

    static void depart(ServerPlayer p, Session s) {
        Entity e = body(p, s);
        s.walker = null;
        if (!s.walking || !(e instanceof Mob m)) {
            discard(p.serverLevel(), s.body);
            s.body = null;
            return;
        }
        s.body = null;
        vn.cavemind.world.Motion.stop(m);
        BlockPos away = vn.cavemind.life.Life.awayPoint(p, m.position());
        net.minecraft.server.MinecraftServer server = p.server;
        boolean crawler = Compat.is(m, Compat.MAN_CRAWLER);
        vn.cavemind.path.Leaving.add(m, away, crawler ? 1.1 : 0.34, crawler ? null : "cm_walk", 52, at -> vn.cavemind.life.Life.dissolved(server, at));
    }

    private static void begin(ServerPlayer p, Watch w, Profile pr, Session s, long now) {
        List<Tac> opts = new ArrayList<>();
        List<Double> wts = new ArrayList<>();
        ServerLevel lvl = p.serverLevel();
        boolean night = lvl.isNight() || lvl.isThundering();
        int L = pr.manLevel;
        int react = dominant(pr);
        double paranoia = w.turnArounds / Math.max(1.0, (now - s.start) / 1200.0 + 1);
        boolean grouped = vn.cavemind.sense.Group.grouped(p, 30);
        boolean weakNow = L >= 4 && (night || w.underground) && weak(p);
        if (!s.hunted && weakNow && pr.anger >= 30 && p.getRandom().nextFloat() < 0.35f && huntAllowed(p, w, pr, s)) {
            startHunt(p, w, pr, s, now, (BlockPos) null, "ngửi thấy bạn đang yếu, và hắn đang giận");
            return;
        }
        if (w.outside) {
            add(opts, wts, Tac.WATCH, (L >= 2 ? 0.55 : 1.0) * (react == 3 ? 1.3 : 1));
            add(opts, wts, Tac.PEEK, (L >= 1 ? 0.85 : 0.5) * (L >= 2 ? 0.7 : 1));
            if (L >= 2 && w.speed > 0.06) add(opts, wts, Tac.AHEAD, (react == 1 ? 1.6 : 0.8) * (paranoia > 3 ? 1.5 : 1));
        }
        if (w.underground && !w.confined) add(opts, wts, Tac.WATCH, 0.9);
        if ((w.outside || w.underground) && (w.speed > 0.04 || L == 0)) add(opts, wts, Tac.SHADOW, (L == 0 ? 0.6 : 0.9) * (paranoia > 3 ? 0.6 : paranoia < 1 ? 1.4 : 1));
        if (L >= 2) add(opts, wts, Tac.PERIPHERAL, 0.55 * (react == 3 ? 1.3 : 1) * (paranoia > 3 ? 1.4 : 1));
        if (w.confined && L >= 1 && (w.underground || night && L >= 3)) add(opts, wts, Tac.BEHIND, (w.underground ? 1.1 : 0.7) * (paranoia > 3 ? 0.5 : 1.3) * (react == 2 ? 1.3 : 1));
        if (w.indoors && night && L >= 2) add(opts, wts, Tac.WINDOW, 1.3 * (react == 1 ? 1.3 : 1));
        if (w.indoors && night && L >= 3 && Cfg.MAN_DOORS.get()) add(opts, wts, Tac.KNOCK, 0.8);
        if ((w.outside || w.underground) && L >= 1 && w.crumbs.size() >= 10) add(opts, wts, Tac.TRACK, 1.0 * (react == 1 ? 1.4 : 1));
        if (L >= 2 && (!pr.chests.isEmpty() || pr.nest != null || pr.home != null)) add(opts, wts, Tac.DOING, 1.1);
        if (night && w.indoors && L >= 3 && peepReady(p, pr)) add(opts, wts, Tac.PEEP, 2.4);
        if (L >= 4 && (night && w.outside || w.underground)) add(opts, wts, Tac.LURE, 0.7 * (react == 2 ? 1.8 : 1) * (1 + pr.bold / 60.0));
        if (weakNow) {
            add(opts, wts, Tac.TRACK, 1.2);
            if (w.confined) add(opts, wts, Tac.BEHIND, 0.8);
        }
        if (grouped) {
            for (int i = opts.size() - 1; i >= 0; i--) {
                Tac t = opts.get(i);
                if (t == Tac.SHADOW || t == Tac.BEHIND || t == Tac.PERIPHERAL || t == Tac.TRACK || t == Tac.PEEP) {
                    opts.remove(i);
                    wts.remove(i);
                }
            }
            boolean dark = night || w.underground;
            if (L >= 2 && dark && s.marked == null) add(opts, wts, Tac.MARK, 1.3);
            if (L >= 3 && dark) add(opts, wts, Tac.LURE, 1.4);
            if (L >= 4 && dark) add(opts, wts, Tac.DOUSE, 1.0);
            if (L >= 5 && dark && !s.dragged && Pack.straggler(p)) add(opts, wts, Tac.DRAG, 1.7);
        } else if (L >= 5 && (night || w.underground) && !s.dragged && Pack.straggler(p)) {
            add(opts, wts, Tac.DRAG, 1.7);
        }
        if (opts.isEmpty()) {
            if (fallback(p, w, pr, s, now)) {
                s.misses = 0;
                return;
            }
            if (++s.misses >= 14 && s.seen == 0) {
                giveUp(p, pr, s, now);
                return;
            }
            s.nextTac = now + 200;
            return;
        }
        for (int i = 0; i < opts.size(); i++) {
            Tac t = opts.get(i);
            double k = 1;
            if (Meter.due(pr) == 3 && (t == Tac.KNOCK || t == Tac.WINDOW || t == Tac.PEEP || t == Tac.BEHIND || t == Tac.DOUSE || t == Tac.LURE || t == Tac.MARK)) k *= 4;
            if (!s.recent.isEmpty() && s.recent.peekLast() == t) k *= 0.3;
            else if (s.recent.contains(t)) k *= 0.7;
            wts.set(i, wts.get(i) * k);
        }
        RandomSource r = p.getRandom();
        for (int attempt = 0; attempt < 3 && !opts.isEmpty(); attempt++) {
            double sum = 0;
            for (double d : wts) sum += d;
            double x = r.nextDouble() * sum;
            int pick = 0;
            for (; pick < wts.size() - 1; pick++) {
                x -= wts.get(pick);
                if (x <= 0) break;
            }
            Tac t = opts.get(pick);
            if (setup(p, w, pr, s, t, now)) {
                s.misses = 0;
                return;
            }
            opts.remove(pick);
            wts.remove(pick);
        }
        if (fallback(p, w, pr, s, now)) {
            s.misses = 0;
            return;
        }
        if (++s.misses >= 10 && s.seen == 0) {
            giveUp(p, pr, s, now);
            return;
        }
        s.nextTac = now + 100 + r.nextInt(120);
    }

    private static boolean fallback(ServerPlayer p, Watch w, Profile pr, Session s, long now) {
        if (w.indoors) {
            if (setup(p, w, pr, s, Tac.WINDOW, now)) return true;
            if (setup(p, w, pr, s, Tac.KNOCK, now)) return true;
        }
        s.relaxed = true;
        try {
            if (setup(p, w, pr, s, Tac.WATCH, now)) {
                CaveMind.debug("the Man falls back to simply standing where {} can see him", p.getGameProfile().getName());
                return true;
            }
            return w.underground && setup(p, w, pr, s, Tac.BEHIND, now);
        } finally {
            s.relaxed = false;
        }
    }

    private static void giveUp(ServerPlayer p, Profile pr, Session s, long now) {
        depart(p, s);
        if (s.hunt != null) s.hunt.discard(p.serverLevel());
        SESSIONS.remove(p.getUUID());
        if (Cfg.FOG.get()) Net.fx(p, Net.FOG, 0f, 400);
        vn.cavemind.life.Life.left(p, false, false);
        Director.abort(p, "man");
        CaveMind.debug("the Man gives up on {} (no tactic fits the place, {} misses)", p.getGameProfile().getName(), s.misses);
    }

    private static void add(List<Tac> o, List<Double> w, Tac t, double weight) {
        int i = o.indexOf(t);
        if (i >= 0) w.set(i, w.get(i) + weight);
        else {
            o.add(t);
            w.add(weight);
        }
    }

    private static int dominant(Profile pr) {
        int[] v = {pr.reactStare, pr.reactFlee, pr.reactApproach, pr.reactIgnore, pr.reactAttack};
        int best = -1, bv = 1;
        for (int i = 0; i < v.length; i++) {
            if (v[i] > bv) {
                bv = v[i];
                best = i;
            }
        }
        return best;
    }

    private static int range(Profile pr, int far) {
        return Math.max(26, far - pr.manLevel * 5);
    }

    private static boolean setup(ServerPlayer p, Watch w, Profile pr, Session s, Tac t, long now) {
        Spots.Cand c = null;
        Float faceYaw = null;
        String pose = null;
        boolean cave = w.underground;
        ServerLevel sl = p.serverLevel();
        boolean daylit = !cave && sl.isDay() && !sl.isThundering();
        int dark = daylit ? 15 : sl.isRaining() && !cave ? 13 : 9;
        int shade = daylit ? 15 : 8;
        switch (t) {
            case WATCH -> {
                if (s.relaxed) {
                    double sight = vn.cavemind.sense.Range.of(p, w);
                    c = Spots.find(p, w, new Spots.Spec().ring(cave ? 8 : Math.max(10, sight * 0.45), cave ? 26 : Math.max(18, sight * 0.85)).dir(0, 180).body(W, H).cave(cave).tries(70)
                        .when(k -> !k.sight.on && k.vis >= 0.2).rank(k -> k.vis + (k.cover ? 0.3 : 0) + (k.trunk ? 0.2 : 0)));
                    break;
                }
                int far = cave ? 40 : 88;
                int near = cave ? 18 : range(pr, 50);
                c = Spots.find(p, w, new Spots.Spec().ring(near, Math.max(near + 8, far - pr.manLevel * 3)).dir(0, 180).body(W, H).cave(cave).tries(60)
                    .when(k -> (hidden(k) || k.sight.edge() > 0.75 && k.dist > 50 && k.light <= 6) && k.vis >= 0.35 && k.light <= dark)
                    .rank(k -> k.vis * 0.4 + (k.cover ? 0.6 : 0) + (k.feet.getY() > p.getY() + 2 ? 0.4 : 0) + (9 - k.light) * 0.05 + Math.abs(k.rel) / 360.0));
            }
            case PEEK -> c = Spots.find(p, w, new Spots.Spec().ring(range(pr, 52), 70).dir(0, 180).body(W, H).tries(50)
                .when(k -> hidden(k) && k.trunk && k.vis >= 0.2 && k.vis <= 0.6 && k.light <= dark)
                .rank(k -> 1 - Math.abs(k.vis - 0.4) + Math.abs(k.rel) / 360.0));
            case AHEAD -> {
                double heading = Math.toDegrees(Math.atan2(-w.vel.x, w.vel.z));
                double rel = Mth.wrapDegrees(heading - p.getYRot());
                c = Spots.find(p, w, new Spots.Spec().ring(46, 72).dir(rel, 22).body(W, H).tries(40)
                    .when(k -> hidden(k) && k.vis >= 0.6 && k.light <= dark + 1)
                    .rank(k -> k.vis + pr.heatAt(k.feet.getX(), k.feet.getZ()) / 400.0));
            }
            case SHADOW -> c = Spots.find(p, w, new Spots.Spec().ring(16, 30).dir(180, 55).body(W, H).cave(cave).tries(36)
                .when(k -> !k.sight.on && k.light <= shade)
                .rank(k -> (k.cover ? 0.5 : 0) + (1 - k.vis) * 0.5 + (8 - k.light) * 0.04));
            case PERIPHERAL -> {
                double side = p.getRandom().nextBoolean() ? 1 : -1;
                if (Math.abs(w.yawSpeed) > 3) side = Math.signum(w.yawSpeed);
                double edge = Math.toDegrees(w.hHalf()) * 1.08;
                c = Spots.find(p, w, new Spots.Spec().ring(12, 26).dir(side * edge, 8).body(W, H).cave(cave).tries(30)
                    .when(k -> !k.sight.on && k.vis >= 0.6)
                    .rank(k -> k.vis + (8 - Math.min(8, k.light)) * 0.05));
            }
            case BEHIND -> c = Spots.find(p, w, new Spots.Spec().ring(5, 12).dir(180, 40).body(W, H).cave(cave).tries(40)
                .when(k -> !k.sight.on && k.vis >= 0.6)
                .rank(k -> k.vis - Math.abs(k.dist - 8) * 0.05 + (6 - Math.min(6, k.light)) * 0.05));
            case WINDOW -> c = Window.find(p, w);
            case MARK, DOUSE, DRAG -> {
                Entity cur = body(p, s);
                if (s.walking && cur != null && vn.cavemind.path.Walker.seenAtAll(vn.cavemind.path.Walker.viewers(cur), cur.getBoundingBox())) return false;
                discard(p.serverLevel(), s.body);
                s.body = null;
                s.walker = null;
                boolean ok = t == Tac.MARK ? Pack.setupMark(p, w, pr, s, now) : t == Tac.DOUSE ? Pack.setupDouse(p, w, pr, s, now) : Pack.setupDrag(p, w, pr, s, now);
                if (!ok) return false;
                start(s, t, now);
                return true;
            }
            case TRACK -> {
                Object[] tr = trackSpot(p, w);
                if (tr == null) return false;
                c = (Spots.Cand) tr[0];
                faceYaw = (Float) tr[1];
                pose = "cm_sniff";
            }
            case DOING -> {
                Object[] d = doingSite(p, w, pr);
                if (d == null) return false;
                c = (Spots.Cand) d[0];
                s.site = (BlockPos) d[1];
                s.siteKind = (String) d[2];
                faceYaw = yawTo(c.center, Vec3.atCenterOf(s.site));
                pose = s.siteKind.equals("nest") || s.siteKind.equals("view") ? "cm_sniff" : "cm_reach";
            }
            case PEEP -> {
                if (!peepReady(p, pr)) return false;
                Direction out = Direction.from2DDataValue(pr.peepDir);
                BlockPos feet = pr.peep.relative(out).below(2);
                if (!Spots.standable(p.serverLevel(), feet, 3) || Schemes.seen(p, w, pr.peep)) return false;
                c = Spots.eval(p, w, feet, W, H);
                faceYaw = out.getOpposite().toYRot();
            }
            case WITNESS -> c = Spots.find(p, w, new Spots.Spec().ring(24, 48).dir(180, 75).body(W, H).cave(cave).tries(50)
                .when(k -> hidden(k) && k.vis >= 0.4).rank(k -> k.vis + (k.cover ? 0.4 : 0) + (k.trunk ? 0.3 : 0)));
            case LURE -> {
                Lure l = Lure.make(p, w, now);
                if (l == null) return false;
                start(s, t, now);
                s.lure = l;
                return true;
            }
            case KNOCK -> {
                BlockPos d = Window.door(p);
                if (d == null) return false;
                Direction out = Window.outside(p.serverLevel(), d);
                BlockPos farFeet = null;
                if (out != null && (pr.manLevel >= 5 || s.walking)) {
                    BlockPos far = d.relative(out, 18 + p.getRandom().nextInt(6));
                    farFeet = Spots.surface(p.serverLevel(), far.getX(), far.getZ(), 3);
                    if (farFeet != null) c = Spots.eval(p, w, farFeet, W, H);
                }
                if (s.walking) {
                    BlockPos step = out == null ? null : Spots.nearFloor(p.serverLevel(), d.relative(out, 2), 3, 2);
                    if (step == null) return false;
                    start(s, t, now);
                    s.door = d;
                    s.knocks = 0;
                    s.knockAt = Long.MAX_VALUE;
                    s.doorOpened = false;
                    s.site = farFeet;
                    if (!walkTo(p, s, step)) return false;
                    if (s.walker == null) s.knockAt = now + 20;
                    return true;
                }
                s.door = d;
                s.knocks = 0;
                s.knockAt = now + 20;
                s.doorOpened = false;
                start(s, t, now);
                if (c != null && hidden(c)) spawnBody(p, s, c.feet);
                return true;
            }
            default -> {
                return false;
            }
        }
        if (c == null) {
            CaveMind.debug("no spot for {} near {}", t, p.getGameProfile().getName());
            return false;
        }
        BlockPos site = s.site;
        String siteKind = s.siteKind;
        start(s, t, now);
        s.site = site;
        s.siteKind = siteKind;
        if (t == Tac.WINDOW) s.hide = c.aux;
        if (!spawnBody(p, s, c.feet)) return false;
        Entity posed = body(p, s);
        if (s.walker != null) {
            s.arriveYaw = faceYaw;
            s.arrivePose = pose;
        } else {
            if (posed instanceof Mob pm && faceYaw != null) vn.cavemind.world.Motion.snapYaw(pm, faceYaw);
            if (posed != null && pose != null) vn.cavemind.world.Motion.pose(posed, pose, now);
            else if (posed != null && s.walking) vn.cavemind.world.Motion.stop(posed);
        }
        if (t == Tac.SHADOW) {
            s.anchor = p.position();
            Entity b = body(p, s);
            if (b != null) Fx.stare(p, b, null, 260, 22);
        } else if (t == Tac.WATCH || t == Tac.PEEK || t == Tac.WITNESS) {
            Entity b = body(p, s);
            if (b != null && p.getRandom().nextFloat() < 0.6f) Fx.stare(p, b, null, 300, 24);
        } else if (t == Tac.WINDOW && pr.manLevel >= 4 && p.getRandom().nextFloat() < 0.45f && s.hide != null) {
            Fx.sound(p, Compat.sound("the_knocker:window_knock_1", "minecraft:block.glass.hit"), SoundSource.BLOCKS, Vec3.atCenterOf(s.hide), 0.9f, 0.9f);
        }
        CaveMind.debug("man {} at {} for {} (dist {})", t, c.feet, p.getGameProfile().getName(), (int) c.dist);
        return true;
    }

    static Spots.Cand spotFor(ServerPlayer p, Watch w, Profile pr, Session s, Tac t) {
        boolean cave = w.underground;
        ServerLevel sl = p.serverLevel();
        boolean daylit = !cave && sl.isDay() && !sl.isThundering();
        int dark = daylit ? 15 : sl.isRaining() && !cave ? 13 : 9;
        int shade = daylit ? 15 : 8;
        return switch (t) {
            case AHEAD -> {
                if (w.speed < 0.04) yield null;
                double rel = Mth.wrapDegrees(Math.toDegrees(Math.atan2(-w.vel.x, w.vel.z)) - p.getYRot());
                yield Spots.find(p, w, new Spots.Spec().ring(46, 72).dir(rel, 22).body(W, H).tries(24)
                    .when(k -> hidden(k) && k.vis >= 0.6 && k.light <= dark + 1).rank(k -> k.vis));
            }
            case SHADOW -> Spots.find(p, w, new Spots.Spec().ring(16, 30).dir(180, 55).body(W, H).cave(cave).tries(24)
                .when(k -> !k.sight.on && k.light <= shade).rank(k -> (k.cover ? 0.5 : 0) + (1 - k.vis) * 0.5));
            case PERIPHERAL -> {
                double side = Math.abs(w.yawSpeed) > 3 ? Math.signum(w.yawSpeed) : p.getRandom().nextBoolean() ? 1 : -1;
                double edge = Math.toDegrees(w.hHalf()) * 1.08;
                yield Spots.find(p, w, new Spots.Spec().ring(12, 26).dir(side * edge, 8).body(W, H).cave(cave).tries(20)
                    .when(k -> !k.sight.on && k.vis >= 0.6).rank(k -> k.vis));
            }
            case BEHIND -> Spots.find(p, w, new Spots.Spec().ring(5, 12).dir(180, 40).body(W, H).cave(cave).tries(24)
                .when(k -> !k.sight.on && k.vis >= 0.6).rank(k -> k.vis - Math.abs(k.dist - 8) * 0.05));
            default -> null;
        };
    }

    static String vi(Tac t) {
        return switch (t) {
            case WATCH -> "đứng nhìn từ xa";
            case PEEK -> "nấp sau thân cây";
            case SHADOW -> "bám theo sau lưng (nghe tiếng bước vọng)";
            case PERIPHERAL -> "đứng ở rìa tầm mắt";
            case AHEAD -> "chặn trước đường bạn đi";
            case BEHIND -> "đứng ngay sau lưng";
            case WINDOW -> "nhìn qua cửa sổ";
            case KNOCK -> "gõ cửa";
            case HUNT -> "CƠN ĐIÊN";
            case TRACK -> "cúi rạp lần theo dấu chân bạn";
            case DOING -> "đang lục lọi đồ của bạn";
            case PEEP -> "ghé mắt vào lỗ nhìn trộm";
            case WITNESS -> "đứng xem bạn phát hiện ra việc hắn làm";
            case LURE -> "giả tiếng để dụ bạn tách khỏi nhóm";
            case MARK -> "đứng giữa bóng tối cho cả nhóm thấy, chọn con mồi";
            case DOUSE -> "tắt từng nguồn sáng quanh nhóm";
            case DRAG -> "lôi kẻ đi cuối vào bóng tối";
        };
    }

    static boolean hidden(Spots.Cand k) {
        return !k.sight.on || k.vis == 0;
    }

    private static void start(Session s, Tac t, long now) {
        s.tac = t;
        s.tacStart = now;
        s.mode = Mode.STAND;
        s.modeAt = now;
        s.noticed = false;
        s.perceive = 0;
        s.stare = s.away = s.onTicks = 0;
        s.reacted = false;
        s.relocations = 0;
        s.zoomed = false;
        s.hide = null;
        s.lure = null;
        s.site = null;
        s.siteKind = null;
        s.stepUntil = 0;
        s.tilted = false;
        s.twitched = false;
        s.recent.addLast(t);
        while (s.recent.size() > 3) s.recent.removeFirst();
    }

    private static void endTac(ServerPlayer p, Session s, long now) {
        if (s.tac == Tac.DOING && s.site != null && now - s.tacStart > 120) aftermath(p, s);
        Entity e = body(p, s);
        s.walker = null;
        if (s.walking && e instanceof Mob m && !Compat.is(e, Compat.MAN_CRAWLER)) {
            stow(p, s, m, now);
        } else {
            discard(p.serverLevel(), s.body);
            s.body = null;
        }
        s.tac = null;
        s.mode = Mode.STAND;
        s.nextTac = s.soon ? now + 90 + p.getRandom().nextInt(80) : s.walking ? now + 160 + p.getRandom().nextInt(360) : now + 300 + p.getRandom().nextInt(900);
        s.soon = false;
    }

    static void stow(ServerPlayer p, Session s, Mob m, long now) {
        Watch w = Watch.of(p);
        List<ServerPlayer> viewers = vn.cavemind.path.Walker.viewers(m);
        boolean seen = vn.cavemind.path.Walker.seenAtAll(viewers, m.getBoundingBox());
        double sight = vn.cavemind.sense.Range.of(p, w);
        BlockPos at = m.blockPosition();
        double rel = Mth.wrapDegrees(yawTo(p.position(), m.position()) - p.getYRot());
        Spots.Cand c = Spots.find(p, w, new Spots.Spec().ring(Math.max(16, sight * 0.5), Math.max(24, sight * 0.95)).dir(rel, 80).body(W, H).cave(w.underground).tries(40)
            .when(k -> !k.sight.on && k.vis == 0 && k.feet.distSqr(at) < 36 * 36 && !vn.cavemind.sense.Group.boxSeenByOthers(p, k.box, 96))
            .rank(k -> (k.cover ? 0.5 : 0) + (8 - Math.min(8, k.light)) * 0.05 - Math.sqrt(k.feet.distSqr(at)) * 0.02));
        vn.cavemind.world.Motion.stop(m);
        if (c == null) {
            vn.cavemind.world.Motion.pose(m, "cm_hold", now);
            return;
        }
        if (!seen && c.feet.distSqr(at) <= 16 * 16) {
            m.getNavigation().stop();
            m.teleportTo(c.feet.getX() + 0.5, c.feet.getY(), c.feet.getZ() + 0.5);
            vn.cavemind.world.Motion.pose(m, "cm_hold", now);
            return;
        }
        s.walker = new vn.cavemind.path.Walker(c.feet, 0.32, vn.cavemind.path.Stealth.Mode.SNEAK);
        s.travelUntil = now + 700;
        s.arrivePose = "cm_hold";
        s.arriveYaw = null;
    }

    static boolean spawnBody(ServerPlayer p, Session s, BlockPos feet) {
        if (s.walking) return walkTo(p, s, feet);
        discard(p.serverLevel(), s.body);
        s.body = null;
        Entity e = make(p, Compat.MAN_STALKER, feet, true);
        if (e == null) return false;
        s.body = e.getUUID();
        return true;
    }

    static boolean walkTo(ServerPlayer p, Session s, BlockPos feet) {
        long now = p.server.getTickCount();
        Entity cur = body(p, s);
        if (!(cur instanceof Mob) || Compat.is(cur, Compat.MAN_CRAWLER)) {
            if (cur != null && vn.cavemind.path.Walker.seenAtAll(vn.cavemind.path.Walker.viewers(cur), cur.getBoundingBox())) return false;
            discard(p.serverLevel(), s.body);
            s.body = null;
            BlockPos entry = vn.cavemind.life.Life.entryPoint(p, Vec3.atCenterOf(feet));
            if (entry == null) return false;
            Entity e = make(p, Compat.MAN_STALKER, entry, true);
            if (e == null) return false;
            s.body = e.getUUID();
            cur = e;
        }
        s.arriveYaw = null;
        s.arrivePose = null;
        double d = Math.sqrt(cur.blockPosition().distSqr(feet));
        if (d <= 1.6) {
            s.walker = null;
            return true;
        }
        List<ServerPlayer> viewers = vn.cavemind.path.Walker.viewers(cur);
        if (d <= 10 && !vn.cavemind.path.Walker.seenAtAll(viewers, cur.getBoundingBox())
            && !vn.cavemind.path.Walker.seenAtAll(viewers, Sight.body(feet, W, H))) {
            ((Mob) cur).getNavigation().stop();
            cur.teleportTo(feet.getX() + 0.5, feet.getY(), feet.getZ() + 0.5);
            s.walker = null;
            return true;
        }
        s.walker = new vn.cavemind.path.Walker(feet, d > 24 ? 0.36 : 0.28, vn.cavemind.path.Stealth.Mode.SNEAK);
        s.travelUntil = now + 240 + (long) (d * 16);
        s.retargetAt = now + 40;
        return true;
    }

    static boolean retargets(Tac t) {
        return t == Tac.SHADOW || t == Tac.BEHIND || t == Tac.PERIPHERAL || t == Tac.AHEAD;
    }

    static Boolean travel(ServerPlayer p, Watch w, Profile pr, Session s, long now) {
        Entity e = body(p, s);
        if (!(e instanceof Mob m)) {
            s.walker = null;
            return s.tac == null ? Boolean.TRUE : Boolean.FALSE;
        }
        if (s.tac != null && retargets(s.tac) && now >= s.retargetAt) {
            s.retargetAt = now + 40;
            Spots.Cand c = spotFor(p, w, pr, s, s.tac);
            if (c != null) s.walker.retarget(c.feet);
        }
        List<ServerPlayer> viewers = vn.cavemind.path.Walker.viewers(m);
        vn.cavemind.path.Walker.Step st = s.walker.tick(m, now, viewers);
        if (s.tac != null && s.tac != Tac.KNOCK && st == vn.cavemind.path.Walker.Step.MOVING) {
            Sight si = Sight.of(p, w, m.getBoundingBox().getCenter());
            if (si.on) {
                double vis = Sight.visible(p, m);
                if (vis > 0.12) s.perceive += 0.35 * vis * 1.6;
                if (s.perceive >= 6) st = vn.cavemind.path.Walker.Step.FROZEN;
            }
        }
        switch (st) {
            case ARRIVED -> {
                arrived(p, s, m, now);
                return null;
            }
            case FROZEN -> {
                if (s.tac == null || s.tac == Tac.KNOCK) return Boolean.TRUE;
                m.getNavigation().stop();
                s.walker = null;
                vn.cavemind.world.Motion.stop(m);
                s.tacStart = now;
                CaveMind.debug("{} catches the Man walking ({})", p.getGameProfile().getName(), s.tac);
                return null;
            }
            case FAILED -> {
                s.walker = null;
                vn.cavemind.world.Motion.stop(m);
                return s.tac == null ? Boolean.TRUE : Boolean.FALSE;
            }
            default -> {
                BlockPos g = s.walker.goal;
                if (g != null && m.blockPosition().distSqr(g) <= 100 && !vn.cavemind.path.Walker.seenAtAll(viewers, m.getBoundingBox())
                    && !vn.cavemind.path.Walker.seenAtAll(viewers, Sight.body(g, W, H))) {
                    m.getNavigation().stop();
                    m.teleportTo(g.getX() + 0.5, g.getY(), g.getZ() + 0.5);
                    arrived(p, s, m, now);
                    return null;
                }
                if (now > s.travelUntil) {
                    s.walker = null;
                    m.getNavigation().stop();
                    vn.cavemind.world.Motion.stop(m);
                    if (s.tac == null) return Boolean.TRUE;
                    return Sight.of(p, w, m.getBoundingBox().getCenter()).on || m.distanceTo(p) > 90 ? Boolean.FALSE : null;
                }
                return Boolean.TRUE;
            }
        }
    }

    static void arrived(ServerPlayer p, Session s, Mob m, long now) {
        m.getNavigation().stop();
        s.walker = null;
        vn.cavemind.world.Motion.stop(m);
        if (s.arriveYaw != null) vn.cavemind.world.Motion.snapYaw(m, s.arriveYaw);
        else vn.cavemind.world.Motion.snap(m, p.getEyePosition());
        if (s.arrivePose != null) vn.cavemind.world.Motion.pose(m, s.arrivePose, now);
        s.arriveYaw = null;
        s.arrivePose = null;
        if (s.tac != null) {
            s.tacStart = now;
            if (s.tac == Tac.SHADOW) s.anchor = p.position();
            if (s.tac == Tac.KNOCK) s.knockAt = now + 20;
        }
    }

    public static Entity make(ServerPlayer p, net.minecraft.resources.ResourceLocation id, BlockPos feet, boolean onlyOwner) {
        ServerLevel lvl = p.serverLevel();
        EntityType<?> type = Compat.type(id);
        if (type == null) return null;
        Entity e = type.create(lvl);
        if (e == null) return null;
        float yaw = yawTo(Vec3.atBottomCenterOf(feet), p.getEyePosition());
        e.moveTo(feet.getX() + 0.5, feet.getY(), feet.getZ() + 0.5, yaw, 0);
        Compat.mark(e, "man", p.getUUID(), onlyOwner);
        if (e instanceof Mob m) {
            m.goalSelector.removeAllGoals(g -> true);
            m.targetSelector.removeAllGoals(g -> true);
            m.setPersistenceRequired();
            m.setYHeadRot(yaw);
            m.setYBodyRot(yaw);
        }
        if (!lvl.addFreshEntity(e)) {
            Compat.ONLY_MAP.remove(e.getUUID());
            CaveMind.debug("{} was refused entry at {} (another mod cancelled the spawn)", id, feet);
            return null;
        }
        return e;
    }

    static Entity body(ServerPlayer p, Session s) {
        if (s.body == null) return null;
        Entity e = p.serverLevel().getEntity(s.body);
        if (e == null || !e.isAlive()) return null;
        return e;
    }

    static void discard(ServerLevel lvl, UUID id) {
        if (id == null) return;
        Entity e = lvl.getEntity(id);
        if (e != null) e.discard();
    }

    static float yawTo(Vec3 from, Vec3 to) {
        double dx = to.x - from.x, dz = to.z - from.z;
        return (float) (Mth.atan2(dz, dx) * (180f / Math.PI)) - 90f;
    }

    public static void face(Mob m, Vec3 at) {
        vn.cavemind.world.Motion.turn(m, at, 7.0f);
    }

    static void glide(Mob m, Vec3 dir, double speed) {
        Vec3 v = m.getDeltaMovement();
        Vec3 d = new Vec3(dir.x, 0, dir.z);
        if (d.lengthSqr() < 1e-6) return;
        d = d.normalize().scale(speed);
        m.setDeltaMovement(d.x, v.y, d.z);
    }

    private static boolean run(ServerPlayer p, Watch w, Profile pr, Session s, long now) {
        if (s.walker != null && s.tac != Tac.KNOCK) {
            Boolean t = travel(p, w, pr, s, now);
            if (t != null) return t;
        }
        if (s.tac == Tac.KNOCK) return Knock.run(p, w, pr, s, now);
        if (s.tac == Tac.LURE) return s.lure != null && s.lure.run(p, w, pr, s, now);
        if (s.tac == Tac.DOUSE) return Pack.runDouse(p, w, pr, s, now);
        Entity e = body(p, s);
        if (e == null) return false;
        if (!(e instanceof Mob m)) return false;
        if (s.tac == Tac.MARK) return Pack.runMark(p, w, pr, s, m, now);
        if (s.tac == Tac.DRAG) return Pack.runDrag(p, w, pr, s, m, now);
        if (e.level() != p.level() || e.distanceToSqr(p) > 140 * 140) return false;
        Sight si = Sight.of(p, w, e.getBoundingBox().getCenter().add(0, 0.6, 0));
        double vis = (now & 1) == 0 ? Sight.visible(p, e) : s.lastVis;
        s.lastVis = vis;
        double dist = Math.sqrt(e.distanceToSqr(p));
        boolean onScreen = si.on && vis > 0.15;
        boolean looking = onScreen && si.edge() < 0.62 && vis > 0.2;
        if (onScreen) s.onTicks++;
        s.away = onScreen ? 0 : s.away + 1;

        boolean posed = s.tac == Tac.PEEP || !s.noticed && (s.tac == Tac.TRACK || s.tac == Tac.DOING) || s.tac == Tac.DOING && now - s.noticedAt < 25;
        if (!posed && (s.mode == Mode.STAND || s.mode == Mode.STARE_WALK || s.mode == Mode.HOLD)) face(m, p.getEyePosition());
        if (s.tac == Tac.DOING && s.noticed && !s.twitched && now - s.noticedAt >= 25) {
            s.twitched = true;
            Compat.anim(e, "cm_twitch");
        }

        if (!s.noticed) {
            int light = p.serverLevel().getMaxLocalRawBrightness(e.blockPosition().above());
            double lightF = light >= 7 ? 1.0 : 0.6 + light * 0.06;
            double distF = Mth.clamp(1.45 - dist / 85.0, 0.35, 1.0);
            if (onScreen) s.perceive += (si.central() ? 1.0 : 0.35) * vis * lightF * distF * 1.6;
            else s.perceive = Math.max(0, s.perceive - 0.25);
            if (s.tac == Tac.PERIPHERAL) {
                Sight pred = Sight.predict(p, w, e.getBoundingBox().getCenter(), 3);
                if (pred.on && Math.abs(pred.sx) < 0.55 || now - s.tacStart > 100 + p.getRandom().nextInt(60)) {
                    if (onScreen && s.onTicks > 2) sighting(p, pr, s, now, dist, false);
                    return false;
                }
                return true;
            }
            if (s.tac == Tac.SHADOW) return Shadow.unnoticed(p, w, pr, s, m, si, vis, dist, now);
            if (dist < 6 && s.tac != Tac.PEEP) return false;
            if (s.perceive >= 6) {
                notice(p, pr, s, now, dist);
            } else {
                long limit = switch (s.tac) {
                    case AHEAD -> 900;
                    case BEHIND -> 800;
                    case WINDOW -> 1200;
                    default -> 1800;
                };
                if (now - s.tacStart > limit) {
                    if (!onScreen) return false;
                    if (now - s.tacStart > limit + 200) return false;
                }
                return true;
            }
        }

        if (!s.reacted && now - s.noticedAt > 90) classify(p, pr, s, dist);
        if (s.tac == Tac.PEEP) {
            if (s.away >= 3 || s.stare > 70) {
                Fx.sound(p, "man:vanish", SoundSource.HOSTILE, e.getEyePosition(), 0.45f, 0.8f);
                return false;
            }
            return now - s.noticedAt < 500;
        }

        switch (s.mode) {
            case STAND -> {
                if (s.away >= 4) {
                    if (s.zoomed && s.relocations < 2) {
                        s.zoomed = false;
                        if (relocate(p, w, pr, s, 18, 24)) return true;
                    }
                    if ((s.tac == Tac.WATCH || s.tac == Tac.AHEAD || s.tac == Tac.PEEK) && pr.manLevel >= 4 && s.relocations < 2 && p.getRandom().nextFloat() < 0.22f) {
                        double nd = Math.max(16, dist * (0.45 + p.getRandom().nextDouble() * 0.15));
                        if (relocate(p, w, pr, s, nd - 4, nd + 4)) return true;
                    }
                    return false;
                }
                if (p.isScoping() && si.central()) s.zoomed = true;
                if (s.tac == Tac.BEHIND) {
                    if (dist < 4.2) {
                        lunge(p, e);
                        return false;
                    }
                    if (now - s.noticedAt == 12) {
                        Fx.sound(p, p.getRandom().nextBoolean() ? "man:short_whisper_1" : "man:short_whisper_2", SoundSource.HOSTILE, e.getEyePosition(), 0.55f, 0.85f);
                    }
                }
                if (looking) s.stare++;
                int limit = Math.max(36, 110 - pr.manLevel * 7);
                if (s.stare == limit / 3) Compat.anim(e, "cm_tilt");
                if (s.stare > limit) {
                    pr.bold(2);
                    float roll = p.getRandom().nextFloat();
                    boolean dark = p.serverLevel().isNight() || w.underground || p.serverLevel().isThundering();
                    if (pr.manLevel >= 5 && dark && roll < 0.18f + pr.bold / 400f) return feint(p, s, e, now);
                    if (pr.manLevel >= 3 && roll > 0.76f) return scuttle(p, s, e, now);
                    boolean canWalk = (s.tac == Tac.WATCH || s.tac == Tac.AHEAD) && w.outside && pr.manLevel >= 2;
                    if (canWalk && p.getRandom().nextFloat() < 0.18f + pr.manLevel * 0.03f) {
                        mode(p, s, Mode.STARE_WALK, now);
                    } else {
                        s.hide = Spots.hideNear(p, w, e.blockPosition(), 4, W, H, w.underground);
                        mode(p, s, Mode.STEP_ASIDE, now);
                        if (s.hide != null) m.getNavigation().moveTo(s.hide.getX() + 0.5, s.hide.getY(), s.hide.getZ() + 0.5, 0.55);
                    }
                    return true;
                }
                boolean closing = dist < s.distAtNotice - Math.max(4, s.distAtNotice * 0.3) && dist < 30 && w.speed > 0.02;
                if (closing || dist < 5 && s.tac != Tac.BEHIND) return respond(p, w, pr, s, m, e, dist, now);
                return true;
            }
            case STEP_ASIDE -> {
                vn.cavemind.world.Motion.walk(m, now);
                if (vis == 0 || s.away >= 3) return false;
                if (s.hide == null) {
                    Vec3 away = e.position().subtract(p.position());
                    float yaw = yawTo(e.position(), e.position().add(away));
                    m.setYRot(yaw);
                    m.setYHeadRot(yaw);
                    m.setYBodyRot(yaw);
                    glide(m, away, 0.055);
                }
                return now - s.modeAt < 160;
            }
            case RETREAT -> {
                face(m, p.getEyePosition());
                vn.cavemind.world.Motion.walk(m, now);
                glide(m, e.position().subtract(p.position()), 0.07);
                if (dist < 9) {
                    Fx.sound(p, "man:vanish", SoundSource.HOSTILE, e.position(), 0.8f, 1.0f);
                    if (pr.manLevel >= 4 && p.getRandom().nextFloat() < 0.35f) s.nextTac = now;
                    return false;
                }
                if (s.away >= 2 || vis == 0) {
                    if (pr.manLevel >= 4 && p.getRandom().nextFloat() < 0.3f) {
                        endTac(p, s, now);
                        if (!setup(p, w, pr, s, w.confined || w.underground ? Tac.BEHIND : Tac.SHADOW, now)) return false;
                        return true;
                    }
                    return false;
                }
                return now - s.modeAt < 200;
            }
            case STARE_WALK -> {
                if (s.away >= 3) {
                    if (pr.manLevel >= 5 && s.relocations < 2 && p.getRandom().nextFloat() < 0.35f) {
                        double nd = Math.max(10, dist - 9);
                        if (relocate(p, w, pr, s, nd - 2, nd + 2)) {
                            mode(p, s, Mode.STAND, now);
                            return true;
                        }
                    }
                    return false;
                }
                if (now % 10 == 0) m.getNavigation().moveTo(p, 0.42);
                vn.cavemind.world.Motion.walk(m, now);
                if (dist <= 12.5) {
                    m.getNavigation().stop();
                    if (huntAllowed(p, w, pr, s)) {
                        startHunt(p, w, pr, s, now, e);
                        return true;
                    }
                    lunge(p, e);
                    return false;
                }
                return now - s.modeAt < 900;
            }
            case FOGGED -> {
                if (now - s.modeAt >= 24) {
                    if (Cfg.FOG.get()) Net.fx(p, Net.FOG, w.underground ? 0.25f : 0.45f, 160);
                    for (int i = 0; i < 3; i++) Dread.echo(p, w, 12 + i * 9);
                    s.soon = p.getRandom().nextFloat() < 0.5f;
                    return false;
                }
                return true;
            }
            case SCUTTLE -> {
                Vec3 away = e.position().add(e.position().subtract(p.position()).normalize().scale(30));
                if (now % 10 == 0) m.getNavigation().moveTo(away.x, away.y, away.z, 1.45);
                return !(s.away >= 3 || now - s.modeAt > 140);
            }
            case FEINT -> {
                if (now % 5 == 0) m.getNavigation().moveTo(p, 1.6);
                if (dist <= 6.5) {
                    m.getNavigation().stop();
                    Fx.sound(p, "man:vanish", SoundSource.HOSTILE, e.position(), 1.0f, 0.7f);
                    Net.fx(p, Net.SHAKE, 1.2f, 8);
                    Director.scare(p, 16);
                    return false;
                }
                return !(s.away >= 2 && now - s.modeAt > 10 || now - s.modeAt > 90);
            }
            case HOLD -> {
                if (s.away >= 3) return false;
                if (dist <= 11 && s.stepUntil == 0) {
                    s.stepUntil = now + 24;
                }
                if (now < s.stepUntil) {
                    vn.cavemind.world.Motion.walk(m, now);
                    glide(m, p.position().subtract(e.position()), 0.075);
                } else if (s.stepUntil > 0 && !s.tilted) {
                    s.tilted = true;
                    Compat.anim(e, "cm_tilt");
                }
                if (dist <= 4.2) {
                    if (pr.manLevel >= 6 || pr.anger >= 50) strike(p, e);
                    lunge(p, e);
                    return false;
                }
                return now - s.modeAt < 400;
            }
            default -> {
                return false;
            }
        }
    }

    static boolean respond(ServerPlayer p, Watch w, Profile pr, Session s, Mob m, Entity e, double dist, long now) {
        pr.bold(5);
        pr.anger(4);
        int L = pr.manLevel;
        double b = pr.bold / 100.0, a = pr.anger / 100.0;
        ServerLevel lvl = p.serverLevel();
        boolean dark = lvl.isNight() || w.underground || lvl.isThundering();
        String[] k = {"retreat", "fog", "scuttle", "feint", "hold", "frenzy"};
        double[] wt = {1.0 - b * 0.6, Cfg.FOG.get() ? 0.8 : 0, L >= 3 ? 0.6 + b * 0.4 : 0.2, L >= 4 && dark ? 0.4 + b * 0.9 : 0,
            0.5 + b * 0.3, (L >= 5 || a >= 0.4) && !s.hunted && huntAllowed(p, w, pr, s) ? b * 0.8 + a * 0.8 : 0};
        double sum = 0;
        for (int i = 0; i < k.length; i++) {
            if (k[i].equals(pr.lastResp)) wt[i] *= 0.25;
            sum += wt[i];
        }
        double x = p.getRandom().nextDouble() * sum;
        int pick = 0;
        for (; pick < k.length - 1; pick++) {
            x -= wt[pick];
            if (x <= 0 && wt[pick] > 0) break;
        }
        String r = k[pick];
        pr.lastResp = r;
        CaveMind.debug("{} closes in on the Man -> {}", p.getGameProfile().getName(), r);
        switch (r) {
            case "fog" -> {
                Net.fx(p, Net.FOG, dist < 12 ? 1.95f : 1.6f, 14);
                mode(p, s, Mode.FOGGED, now);
            }
            case "scuttle" -> {
                return scuttle(p, s, e, now);
            }
            case "feint" -> {
                return feint(p, s, e, now);
            }
            case "hold" -> mode(p, s, Mode.HOLD, now);
            case "frenzy" -> {
                startHunt(p, w, pr, s, now, e.blockPosition(), "bạn dám áp sát hắn");
                return true;
            }
            default -> mode(p, s, Mode.RETREAT, now);
        }
        return true;
    }

    static String viResp(String r) {
        return switch (r) {
            case "fog" -> "kéo sương dày đặc lại rồi biến mất trong đó";
            case "scuttle" -> "sụp xuống bò bốn chân, lao vụt đi";
            case "feint" -> "lao thẳng vào bạn (đòn nhử)";
            case "hold" -> "đứng lì, rồi bước lại gần bạn";
            case "frenzy" -> "NỔI ĐIÊN";
            default -> "lùi dần vào bóng tối, mắt không rời bạn";
        };
    }

    static boolean scuttle(ServerPlayer p, Session s, Entity e, long now) {
        Vec3 at = e.position();
        if (!swap(p, s, e, Compat.MAN_CRAWLER)) {
            mode(p, s, Mode.RETREAT, now);
            return true;
        }
        mode(p, s, Mode.SCUTTLE, now);
        Fx.sound(p, "man:vanish", SoundSource.HOSTILE, at, 0.5f, 1.4f);
        return true;
    }

    static boolean feint(ServerPlayer p, Session s, Entity e, long now) {
        Vec3 at = e.position();
        if (!swap(p, s, e, Compat.MAN_CRAWLER)) {
            mode(p, s, Mode.RETREAT, now);
            return true;
        }
        mode(p, s, Mode.FEINT, now);
        if (Cfg.SILENCE.get()) Net.fx(p, Net.SILENCE, 0, 40);
        Fx.sound(p, "man:scream_loop", SoundSource.HOSTILE, at, 0.9f, 1.25f);
        return true;
    }

    static boolean swap(ServerPlayer p, Session s, Entity old, ResourceLocation id) {
        float yaw = old.getYRot();
        Entity e = make(p, id, old.blockPosition(), true);
        if (e == null) return false;
        discard(p.serverLevel(), s.body);
        s.body = e.getUUID();
        if (e instanceof Mob m) {
            vn.cavemind.world.Motion.snapYaw(m, yaw);
            m.setAggressive(true);
        }
        return true;
    }

    static void strike(ServerPlayer p, Entity e) {
        if (e instanceof net.minecraft.world.entity.LivingEntity le) {
            p.hurt(p.serverLevel().damageSources().mobAttack(le), 3f);
        }
        p.knockback(0.5, e.getX() - p.getX(), e.getZ() - p.getZ());
        Net.fx(p, Net.SHAKE, 2f, 8);
    }

    static Object[] trackSpot(ServerPlayer p, Watch w) {
        ServerLevel lvl = p.serverLevel();
        List<long[]> list = new ArrayList<>(w.crumbs);
        double lo = w.underground ? 14 : 26, hi = w.underground ? 32 : 50;
        for (int i = list.size() - 2; i >= 0; i--) {
            BlockPos q = BlockPos.of(list.get(i)[0]);
            double d = Math.sqrt(q.distSqr(p.blockPosition()));
            if (d < lo || d > hi) {
                if (d > hi + 30) break;
                continue;
            }
            BlockPos feet = Spots.nearFloor(lvl, q, 3, 2);
            if (feet == null) continue;
            Spots.Cand k = Spots.eval(p, w, feet, W, H);
            if (!hidden(k) || k.vis < 0.3) continue;
            BlockPos nx = BlockPos.of(list.get(i + 1)[0]);
            return new Object[]{k, yawTo(Vec3.atBottomCenterOf(q), Vec3.atBottomCenterOf(nx))};
        }
        return null;
    }

    static Object[] doingSite(ServerPlayer p, Watch w, Profile pr) {
        ServerLevel lvl = p.serverLevel();
        List<Object[]> sites = new ArrayList<>();
        for (long l : pr.chests) sites.add(new Object[]{BlockPos.of(l), "chest"});
        if (pr.nest != null) sites.add(new Object[]{pr.nest, "nest"});
        if (pr.trailDone && pr.trailTo != null && pr.home != null) sites.add(new Object[]{pr.trailTo, "view"});
        if (pr.home != null && lvl.isLoaded(pr.home)) {
            BlockPos d = homeDoor(lvl, pr.home);
            if (d != null) sites.add(new Object[]{d, "door"});
        }
        java.util.Collections.shuffle(sites, new java.util.Random(p.getRandom().nextLong()));
        for (Object[] st : sites) {
            BlockPos site = (BlockPos) st[0];
            String kind = (String) st[1];
            if (!lvl.isLoaded(site)) continue;
            double d = Math.sqrt(site.distSqr(p.blockPosition()));
            if (d < 20 || d > 72) continue;
            List<BlockPos> tries = new ArrayList<>();
            if (kind.equals("view")) tries.add(site);
            else for (Direction dir : Direction.Plane.HORIZONTAL) for (int k = 1; k <= 2; k++) tries.add(site.relative(dir, k));
            for (BlockPos t : tries) {
                BlockPos feet = Spots.nearFloor(lvl, t, 3, 2);
                if (feet == null) continue;
                Spots.Cand c = Spots.eval(p, w, feet, W, H);
                if (!hidden(c) || c.vis < 0.3) continue;
                return new Object[]{c, kind.equals("view") ? pr.home : site, kind};
            }
        }
        return null;
    }

    static BlockPos homeDoor(ServerLevel lvl, BlockPos home) {
        BlockPos best = null;
        double bd = 1e9;
        for (BlockPos q : BlockPos.betweenClosed(home.offset(-8, -2, -8), home.offset(8, 3, 8))) {
            if (!isLowerDoor(lvl.getBlockState(q)) || Window.outside(lvl, q) == null) continue;
            double d = q.distSqr(home);
            if (d < bd) {
                bd = d;
                best = q.immutable();
            }
        }
        return best;
    }

    static void aftermath(ServerPlayer p, Session s) {
        Profile pr = MindData.profile(p);
        ServerLevel lvl = p.serverLevel();
        if (s.siteKind == null) return;
        switch (s.siteKind) {
            case "chest" -> Schemes.spoil(p, Watch.of(p), pr, true, s.site);
            case "door" -> {
                BlockState st = lvl.getBlockState(s.site);
                if (st.getBlock() instanceof DoorBlock db && !st.getValue(DoorBlock.OPEN)) {
                    db.setOpen(null, lvl, st, s.site, true);
                }
            }
            default -> {}
        }
        s.site = null;
        s.siteKind = null;
    }

    static boolean peepReady(ServerPlayer p, Profile pr) {
        if (pr.peep == null || pr.peepDir < 0) return false;
        ServerLevel lvl = p.serverLevel();
        if (!lvl.isLoaded(pr.peep) || pr.peep.distSqr(p.blockPosition()) > 14 * 14) return false;
        if (!lvl.getBlockState(pr.peep).isAir()) {
            pr.peep = null;
            return false;
        }
        return true;
    }

    static boolean weak(ServerPlayer p) {
        return p.getHealth() <= p.getMaxHealth() * 0.4f || p.getFoodData().getFoodLevel() <= 6;
    }

    public static void witness(ServerPlayer p, Watch w, Profile pr, BlockPos at, long now) {
        if (!Compat.manActive() || Compat.exempt(p) || p.level().dimension() != Level.OVERWORLD) return;
        Session s = SESSIONS.get(p.getUUID());
        if (s != null && (s.tac != null || s.hunt != null)) return;
        boolean fresh = s == null;
        if (fresh) {
            if (!SESSIONS.isEmpty() || !Director.free(p)) return;
            Vec3 man = vn.cavemind.life.Life.where(p.server);
            if (man != null && man.distanceToSqr(p.position()) > 200 * 200) return;
            if (!Director.claim(p, "man")) return;
            s = new Session();
            s.start = now;
            s.end = now + 1500;
            s.budget = 1;
            s.walking = true;
            s.cause = "xem bạn tìm thấy dấu vết";
            SESSIONS.put(p.getUUID(), s);
            if (Cfg.FOG.get()) Net.fx(p, Net.FOG, w.underground ? 0.25f : 0.4f, 160);
        }
        if (!setup(p, w, pr, s, Tac.WITNESS, now) && fresh) s.end = now;
    }

    static void retarget(ServerPlayer p, Session s, Tac t, long now) {
        if (!s.walking) {
            discard(p.serverLevel(), s.body);
            s.body = null;
        }
        s.walker = null;
        start(s, t, now);
    }

    public static void huntHit(ServerPlayer p, Entity e) {
        MindData.profile(p).bold(4);
        for (Map.Entry<UUID, Session> en : SESSIONS.entrySet()) {
            Session s = en.getValue();
            if (s.hunt == null || !e.getUUID().equals(s.hunt.crawler)) continue;
            ServerPlayer owner = p.server.getPlayerList().getPlayer(en.getKey());
            s.hunt.hit(owner != null ? owner : p, e, p.server.getTickCount());
            return;
        }
    }

    static boolean crowded(ServerPlayer p, Session s, long now) {
        if (!Cfg.MAN_SHY.get() || s.tac == null || vn.cavemind.sense.Group.isolation(p) > 22) return false;
        if (!(s.tac == Tac.SHADOW || s.tac == Tac.BEHIND || s.tac == Tac.PERIPHERAL || s.tac == Tac.TRACK || s.tac == Tac.PEEP)) return false;
        endTac(p, s, now);
        return true;
    }

    public static void died(ServerPlayer p, boolean calm) {
        Profile pr = MindData.profile(p);
        Session s = SESSIONS.get(p.getUUID());
        long now = p.server.getTickCount();
        if (s != null && s.hunt != null) {
            ServerPlayer next = s.hunt.nextVictim(p, p.serverLevel());
            if (next != null && !SESSIONS.containsKey(next.getUUID()) && Director.state(next).act == null) {
                s.hunt.kills++;
                s.hunt.target = next.getUUID();
                s.hunt.caught = true;
                SESSIONS.remove(p.getUUID());
                Director.release(p, "man", false);
                Director.claim(next, "man");
                SESSIONS.put(next.getUUID(), s);
                Pack.sayAll(Pack.members(next), "Hắn hạ " + p.getGameProfile().getName() + " rồi quay sang " + next.getGameProfile().getName()
                    + ". Nhóm đang tản ra, hãy tụ lại");
                s = null;
            }
        }
        boolean byHim = s != null && s.hunt != null;
        if (s != null) leave(p, pr, s, now, s.hunted);
        if (!calm) return;
        pr.manNext = Math.max(pr.manNext, pr.playTicks + Math.min(2, Cfg.DEATH_CALM.get()) * 1200L);
        if (byHim) {
            pr.manLastHuntDay = Dread.day(p.serverLevel()) - 1;
            pr.anger(-15);
        }
    }

    static void mode(ServerPlayer p, Session s, Mode m, long now) {
        String vi = switch (m) {
            case STEP_ASIDE -> "lách vào chỗ nấp";
            case RETREAT -> "lùi dần vào bóng tối, mắt vẫn nhìn bạn";
            case STARE_WALK -> "bắt đầu tiến về phía bạn (phá luật!)";
            default -> null;
        };
        s.mode = m;
        s.modeAt = now;
    }

    static void notice(ServerPlayer p, Profile pr, Session s, long now, double dist) {
        s.noticed = true;
        s.noticedAt = now;
        s.distAtNotice = dist;
        s.stare = 0;
        Entity nb = body(p, s);
        if (nb != null && s.tac == Tac.TRACK) {
            vn.cavemind.world.Motion.play(nb, "cm_rise", 70, now);
        } else if (nb != null && s.tac == Tac.PEEP) {
            Fx.sound(p, "man:long_whisper", SoundSource.HOSTILE, nb.getEyePosition(), 0.35f, 0.7f);
        }
        if ((s.tac == Tac.BEHIND || s.tac == Tac.SHADOW || dist < 20) && s.tac != Tac.TRACK && s.tac != Tac.PEEP && s.tac != Tac.DOING) {
            Entity e = body(p, s);
            if (e != null) Compat.anim(e, "cm_twitch");
        }
        sighting(p, pr, s, now, dist, true);
    }

    static void sighting(ServerPlayer p, Profile pr, Session s, long now, double dist, boolean full) {
        s.seen++;
        Meter.seen(p, full ? 4 : 2);
        pr.manSightings++;
        ServerLevel lvl = p.serverLevel();
        pr.moment(new Moment(Moment.SEEN, Dread.day(lvl), Dread.time(lvl), p.blockPosition(), (int) dist, s.tac == null ? 0 : s.tac.ordinal()));
        grant(p, "man:feelingwatched");
        Tension.seen(p, dist, "The Man");
        vn.cavemind.director.PaceLog.encounter(p, "man", "seen " + (s.tac == null ? "?" : s.tac.name()), (int) dist + "m, " + s.cause);
        if (full && pr.manSightings <= 2) {
            Fx.sound(p, "man:white_noise_passby", SoundSource.AMBIENT, p.getEyePosition(), 0.22f, 0.9f);
        }
        CaveMind.debug("{} sees the Man ({} at {}m)", p.getGameProfile().getName(), s.tac, (int) dist);
    }

    static void classify(ServerPlayer p, Profile pr, Session s, double dist) {
        s.reacted = true;
        if (s.provoked > 0) pr.reactAttack++;
        else if (dist < s.distAtNotice - 6) pr.reactApproach++;
        else if (dist > s.distAtNotice + 9) pr.reactFlee++;
        else if (s.stare > 50) pr.reactStare++;
        else pr.reactIgnore++;
    }

    static boolean relocate(ServerPlayer p, Watch w, Profile pr, Session s, double a, double b) {
        Entity e = body(p, s);
        Vec3 from = e != null ? e.position() : p.position();
        double rel = Mth.wrapDegrees(yawTo(p.position(), from) - p.getYRot());
        Spots.Cand c = Spots.find(p, w, new Spots.Spec().ring(Math.max(6, a), Math.max(a + 2, b)).dir(rel, 35).body(W, H).cave(w.underground).tries(30)
            .when(k -> !k.sight.on && k.vis >= 0.5).rank(k -> k.vis));
        if (c == null) return false;
        if (s.walking) {
            if (!walkTo(p, s, c.feet)) return false;
            if (e != null && s.walker == null) Compat.anim(e, "empty");
        } else if (e != null) {
            e.teleportTo(c.feet.getX() + 0.5, c.feet.getY(), c.feet.getZ() + 0.5);
            Compat.anim(e, "empty");
        } else if (!spawnBody(p, s, c.feet)) {
            return false;
        }
        s.relocations++;
        s.noticed = false;
        s.perceive = 0;
        s.stare = 0;
        s.away = 0;
        s.mode = Mode.STAND;
        CaveMind.debug("man relocates closer to {}m", (int) c.dist);
        return true;
    }

    static void lunge(ServerPlayer p, Entity e) {
        Fx.sound(p, p.getRandom().nextBoolean() ? "man:short_whisper_1" : "man:short_whisper_2", SoundSource.HOSTILE, e.getEyePosition(), 0.45f, 0.8f);
        Director.scare(p, 14);
    }

    public static void attacked(ServerPlayer p, Entity e) {
        Session s = SESSIONS.get(p.getUUID());
        Profile pr = MindData.profile(p);
        pr.manHurt++;
        vn.cavemind.life.Life.hurt(p.server, 25);
        if (s == null) {
            e.discard();
            return;
        }
        s.provoked++;
        Fx.sound(p, "man:vanish", SoundSource.HOSTILE, e.position(), 1.0f, 0.7f);
        e.discard();
        if (s.body != null && s.body.equals(e.getUUID())) {
            s.body = null;
            s.tac = null;
            s.nextTac = p.server.getTickCount() + 200 + p.getRandom().nextInt(400);
        }
        pr.anger(30);
        pr.bold(12);
        if (s.provoked >= 2 && pr.manLevel >= 3 || pr.anger >= 60) s.wantHunt = true;
        CaveMind.debug("{} attacked the Man (provoked {})", p.getGameProfile().getName(), s.provoked);
    }

    static boolean huntAllowed(ServerPlayer p, Watch w, Profile pr, Session s) {
        if (!Cfg.MAN_HUNT.get() || s.hunted || pr.manPeace) return false;
        ServerLevel lvl = p.serverLevel();
        if (lvl.getDifficulty() == net.minecraft.world.Difficulty.PEACEFUL) return false;
        if (Meter.huntDue(p) && Meter.dark(p, w)) return true;
        if (!Tension.allows(p, Tension.Permit.ENGAGE) && !(pr.anger >= 60 && Tension.allows(p, Tension.Permit.APPROACH))) return false;
        long day = Dread.day(lvl);
        boolean provoked = s.provoked >= 2 && pr.manLevel >= 3 || pr.anger >= 60;
        if (!provoked && pr.manLevel < Cfg.MAN_HUNT_LEVEL.get()) return false;
        if (!provoked && day - pr.manLastHuntDay < 2) return false;
        if (!(lvl.isNight() || w.underground || lvl.isThundering())) return false;
        if (pr.home != null && pr.home.distSqr(p.blockPosition()) < 18 * 18 && w.indoors && !provoked) return false;
        return Director.building(p) || provoked;
    }

    static void startHunt(ServerPlayer p, Watch w, Profile pr, Session s, long now, Entity from) {
        startHunt(p, w, pr, s, now, from != null ? from.blockPosition() : null, "hắn đã tiến sát tới bạn");
    }

    static void startHunt(ServerPlayer p, Watch w, Profile pr, Session s, long now, BlockPos at, String why) {
        Entity was = body(p, s);
        if (at == null && was != null && was.distanceToSqr(p) > 81) at = was.blockPosition();
        UUID stand = null;
        if (was instanceof Mob wm && s.walking && at != null && was.blockPosition().equals(at)) {
            wm.getNavigation().stop();
            Compat.anim(wm, "cm_twitch");
            stand = was.getUUID();
        } else {
            discard(p.serverLevel(), s.body);
        }
        s.body = null;
        s.walker = null;
        vn.cavemind.director.PaceLog.encounter(p, "man", "frenzy", why);
        s.tac = Tac.HUNT;
        s.hunted = true;
        s.wantHunt = false;
        s.lure = null;
        s.hunt = new Hunt(p, w, at, now, why);
        s.hunt.stand = stand;
        s.hunt.marked = s.marked;
        if (s.hunt.pack >= 2) Pack.sayAll(Pack.members(p), "CƠN ĐIÊN nhắm vào cả nhóm (" + s.hunt.pack + " người). Cần " + s.hunt.need()
            + " đòn để đẩy lùi hắn. Đứng sát nhau trong ánh sáng thì hắn chỉ lấy tối đa một mạng; tản ra thì hắn có thể lấy " + s.hunt.maxKills());
        pr.manHunts++;
        pr.manLastHuntDay = Dread.day(p.serverLevel());
        CaveMind.debug("the Man hunts {}", p.getGameProfile().getName());
    }

    static void grant(ServerPlayer p, String id) {
        Compat.grant(p, id);
    }

    static final class Knock {
        static boolean run(ServerPlayer p, Watch w, Profile pr, Session s, long now) {
            ServerLevel lvl = p.serverLevel();
            BlockState st = lvl.getBlockState(s.door);
            if (!(st.getBlock() instanceof DoorBlock)) return false;
            Entity e = body(p, s);
            if (s.walking && s.walker != null) {
                Boolean t = travel(p, w, pr, s, now);
                if (s.knocks == 0) {
                    if (t == Boolean.FALSE || now - s.tacStart > 1600) return false;
                    if (t == Boolean.TRUE) return true;
                }
            }
            if (e instanceof Mob m && s.walker == null && s.knocks < 3) face(m, Vec3.atCenterOf(s.door));
            if (s.walking && s.knocks >= 3 && s.walker == null && s.site != null && e instanceof Mob && e.blockPosition().distSqr(s.door) < 25) {
                s.walker = new vn.cavemind.path.Walker(s.site, 0.3, vn.cavemind.path.Stealth.Mode.SNEAK);
                s.travelUntil = now + 700;
                s.site = null;
            }
            boolean open = st.getValue(DoorBlock.OPEN);
            if (s.knocks < 3) {
                if (now >= s.knockAt) {
                    if (s.knocks == 0) {
                        String[] ks = {"the_knocker:door_knocking_1", "the_knocker:door_knocking_3"};
                        Holder<net.minecraft.sounds.SoundEvent> snd = Compat.sound(ks[p.getRandom().nextInt(ks.length)]);
                        if (snd != null) {
                            Fx.sound(p, snd, SoundSource.BLOCKS, Vec3.atCenterOf(s.door), 1.0f, 0.85f);
                            s.knocks = 3;
                            s.knockAt = now;
                            return true;
                        }
                    }
                    Fx.sound(p, Holder.direct(SoundEvents.WOOD_HIT), SoundSource.BLOCKS, Vec3.atCenterOf(s.door), 1.2f, 0.6f);
                    s.knocks++;
                    s.knockAt = now + 16;
                }
                return true;
            }
            if (open && !s.doorOpened) {
                s.doorOpened = true;
                Director.scare(p, 8);
                if (e != null) {
                    s.tac = Tac.WATCH;
                    s.noticed = false;
                    s.perceive = 0;
                    s.tacStart = now;
                    return true;
                }
                return false;
            }
            long since = now - s.knockAt;
            if (since > 700 && !s.doorOpened) {
                if (pr.manLevel >= 5 && p.getRandom().nextFloat() < 0.45f && Cfg.MAN_DOORS.get()) {
                    ((DoorBlock) st.getBlock()).setOpen(null, lvl, st, s.door, true);
                    Director.scare(p, 10);
                    CaveMind.debug("the door opens by itself for {}", p.getGameProfile().getName());
                }
                return false;
            }
            return since < 900;
        }
    }

    static boolean isLowerDoor(BlockState st) {
        return st.getBlock() instanceof DoorBlock && st.is(BlockTags.WOODEN_DOORS) && st.getValue(DoorBlock.HALF) == DoubleBlockHalf.LOWER;
    }

    private ManMind() {}
}
