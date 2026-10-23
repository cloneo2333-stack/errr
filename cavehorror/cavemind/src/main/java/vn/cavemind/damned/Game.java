package vn.cavemind.damned;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
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
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.CandleBlock;
import net.minecraft.world.level.block.StandingSignBlock;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.entity.SignText;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.RotationSegment;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import vn.cavemind.CaveMind;
import vn.cavemind.Cfg;
import vn.cavemind.compat.Compat;
import vn.cavemind.director.Director;
import vn.cavemind.director.Dread;
import vn.cavemind.director.PaceLog;
import vn.cavemind.director.Tension;
import vn.cavemind.memory.MindData;
import vn.cavemind.memory.Moment;
import vn.cavemind.memory.Profile;
import vn.cavemind.net.Net;
import vn.cavemind.path.Stealth;
import vn.cavemind.path.Walker;
import vn.cavemind.sense.Ears;
import vn.cavemind.sense.Range;
import vn.cavemind.sense.Sight;
import vn.cavemind.sense.Watch;
import vn.cavemind.world.Fx;
import vn.cavemind.world.Motion;
import vn.cavemind.world.Spots;

public final class Game {
    public enum S { INVITE, COUNT, SEEK, WARN, MARK, HUNT, LOST, CAUGHT, RETREAT, DONE }

    static final Map<S, Integer> LIMIT = new EnumMap<>(S.class);

    static {
        LIMIT.put(S.INVITE, 400);
        LIMIT.put(S.COUNT, 1200);
        LIMIT.put(S.SEEK, 3000);
        LIMIT.put(S.WARN, 600);
        LIMIT.put(S.MARK, 900);
        LIMIT.put(S.HUNT, 1800);
        LIMIT.put(S.LOST, 900);
        LIMIT.put(S.CAUGHT, 70);
        LIMIT.put(S.RETREAT, 600);
    }

    static final double W = 0.6, H = 1.8;
    private static final Map<UUID, Game> GAMES = new HashMap<>();
    private static final String[] U = {"", "một", "hai", "ba", "bốn", "năm", "sáu", "bảy", "tám", "chín"};

    final UUID player;
    S s = S.INVITE;
    long since, start;
    UUID body;
    int strikes, beat, stare, away, hideTicks, lightTicks, lostTicks, finds, placements;
    long nextBeat, placedAt, lastSeenAt, nextPlace, telegraphUntil, lastLos, holdUntil;
    boolean posed;
    int hideDelay;
    double perceive, lastDist = 40;
    boolean noticed, hunted, caught, why1, warnedSign, los, stuckShown, slept, active, victory;
    // owed: strikes carried in from games you lost or ran from; lost: it found you; payUp: hunt once the pointing is over
    boolean lost, marked, payUp;
    int gate, owed, watched, grip;
    String reason = "";
    String cause = "";
    String ending = "";
    BlockPos tree;
    Vec3 lastSeen;
    Walker walker;
    final List<Vec3> sweep = new ArrayList<>();
    final List<UUID> kids = new ArrayList<>();
    int kidStage, holdStage;
    long kidAt, holdAt;
    UUID victim;
    float holdYaw, holdPitch;
    double holdTurn;
    boolean inspected;
    static final int HOLD_T = 120;

    private Game(UUID player) {
        this.player = player;
    }

    public static boolean on() {
        return Compat.damnedActive() && Cfg.SEEK.get();
    }

    public static boolean active(ServerPlayer p) {
        return GAMES.containsKey(p.getUUID());
    }

    public static boolean anyActive() {
        return !GAMES.isEmpty();
    }

    public static Vec3 where(net.minecraft.server.MinecraftServer server) {
        for (Game g : GAMES.values()) {
            ServerPlayer p = server.getPlayerList().getPlayer(g.player);
            if (p == null) continue;
            Entity e = g.body(p);
            if (e != null) return e.position();
        }
        return null;
    }

    public static void forget(ServerPlayer p) {
        Game g = GAMES.remove(p.getUUID());
        if (g == null) return;
        if (g.strikes >= 2 || g.s == S.HUNT || g.s == S.MARK || g.s == S.LOST) {
            Profile pr = MindData.profile(p);
            pr.dDebt = Math.min(3, pr.dDebt + 1);
        }
        if (g.active && g.victory) vn.cavemind.chapter.Chapters.ended(p, g.gate, true, g.tree, g.ending);
        g.discard(p.serverLevel());
        Kids.clear(p.serverLevel(), g.kids);
        Dark.forget(p);
        Director.abort(p, "damned");
        Director.release(p, "damned", false);
    }

    public static void clear() {
        GAMES.clear();
        Dark.clear();
    }

    public static String describe(ServerPlayer p) {
        Game g = GAMES.get(p.getUUID());
        if (g == null) return null;
        long now = p.server.getTickCount();
        Entity e = g.body(p);
        return "trò chơi: " + vi(g.s) + " (" + (now - g.since) / 20 + "s, lỗi " + g.strikes + (e != null ? ", cách " + (int) e.distanceTo(p) + "m" : "") + ")";
    }

    static String vi(S s) {
        return switch (s) {
            case INVITE -> "mời chơi";
            case COUNT -> "đang đếm";
            case SEEK -> "đi tìm";
            case WARN -> "cảnh cáo";
            case MARK -> "đánh dấu";
            case HUNT -> "SĂN";
            case LOST -> "mất dấu";
            case CAUGHT -> "bắt được";
            case RETREAT -> "rút lui";
            case DONE -> "xong";
        };
    }

    public static String num(int n) {
        if (n <= 0) return "không";
        if (n < 10) return U[n];
        if (n == 10) return "mười";
        int t = n / 10, u = n % 10;
        String head = t == 1 ? "mười" : U[t] + " mươi";
        if (u == 0) return head;
        if (u == 5) return head + " lăm";
        if (t > 1 && u == 1) return head + " mốt";
        if (t > 1 && u == 4) return head + " tư";
        return head + " " + U[u];
    }

    public static boolean start(ServerPlayer p, String cause) {
        if (!on() || GAMES.containsKey(p.getUUID()) || Compat.exempt(p) || p.level().dimension() != Level.OVERWORLD) return false;
        if (!Director.claim(p, "damned")) return false;
        Game g = new Game(p.getUUID());
        long now = p.server.getTickCount();
        g.start = g.since = now;
        g.cause = cause;
        if (!g.invite(p, Watch.of(p), now)) {
            Director.abort(p, "damned");
            return false;
        }
        GAMES.put(p.getUUID(), g);
        Profile pr = MindData.profile(p);
        g.owe(pr);
        pr.dGames++;
        PaceLog.encounter(p, "damned", "invite", cause);
        CaveMind.debug("the Damned invites {} to play ({})", p.getGameProfile().getName(), cause);
        return true;
    }

    public static boolean startActive(ServerPlayer p, BlockPos post, int gate) {
        if (!on() || GAMES.containsKey(p.getUUID()) || Compat.exempt(p) || p.level().dimension() != Level.OVERWORLD) return false;
        if (!Director.claim(p, "damned")) return false;
        Game g = new Game(p.getUUID());
        long now = p.server.getTickCount();
        g.start = g.since = now;
        g.active = true;
        g.gate = gate;
        g.cause = gate > 0 ? "bạn rủ nó chơi ván của chương " + gate : "bạn rủ nó chơi";
        if (!g.atPost(p, post.immutable(), now)) {
            Director.abort(p, "damned");
            return false;
        }
        GAMES.put(p.getUUID(), g);
        g.owe(MindData.profile(p));
        MindData.profile(p).dGames++;
        Tension.stress(p, 6, "rủ nó chơi");
        PaceLog.encounter(p, "damned", "active", g.cause);
        CaveMind.debug("{} invites the Damned to play at {} (gate {})", p.getGameProfile().getName(), post, gate);
        return true;
    }

    // A game you lost or walked out on is not forgotten: the next one starts with those strikes already counted,
    // so it skips the warning (one owed) or goes straight for you on the first slip (two owed). Winning pays one back.
    void owe(Profile pr) {
        owed = strikes = Math.min(2, pr.dDebt);
    }

    boolean atPost(ServerPlayer p, BlockPos post, long now) {
        ServerLevel lvl = p.serverLevel();
        BlockPos best = null;
        double far = -1;
        for (Direction d : Direction.Plane.HORIZONTAL) {
            for (int k = 1; k <= 2; k++) {
                BlockPos feet = Spots.nearFloor(lvl, post.relative(d, k), 2, 2);
                if (feet == null) continue;
                double dist = Vec3.atBottomCenterOf(feet).distanceTo(p.position());
                if (dist > far) {
                    far = dist;
                    best = feet;
                }
                break;
            }
        }
        if (best == null) return false;
        float yaw = HideSeekYaw.toward(Vec3.atBottomCenterOf(best), Vec3.atBottomCenterOf(post));
        if (put(p, best, yaw, "cm_count", now) == null) return false;
        tree = post;
        noticed = true;
        s = S.COUNT;
        beat = 0;
        nextBeat = now + 40;
        return true;
    }

    public static void tick(ServerPlayer p, Watch w, long now) {
        Game g = GAMES.get(p.getUUID());
        if (g == null) return;
        boolean keep;
        try {
            keep = g.step(p, w, now);
        } catch (Throwable t) {
            CaveMind.LOG.warn("[CaveMind] damned game failed", t);
            keep = false;
        }
        if (!keep) g.finish(p, now);
    }

    Entity body(ServerPlayer p) {
        if (body == null) return null;
        Entity e = p.serverLevel().getEntity(body);
        return e != null && e.isAlive() ? e : null;
    }

    void discard(ServerLevel lvl) {
        if (body == null) return;
        Entity e = lvl.getEntity(body);
        if (e != null) e.discard();
        body = null;
    }

    Entity put(ServerPlayer p, BlockPos feet, float yaw, String anim, long now) {
        ServerLevel lvl = p.serverLevel();
        discard(lvl);
        EntityType<?> type = Compat.type(Compat.DAMNED);
        if (type == null) return null;
        Entity e = type.create(lvl);
        if (e == null) return null;
        e.moveTo(feet.getX() + 0.5, feet.getY(), feet.getZ() + 0.5, yaw, 0);
        Compat.mark(e, "damned_game", p.getUUID(), false);
        if (e instanceof Mob m) {
            m.goalSelector.removeAllGoals(x -> true);
            m.targetSelector.removeAllGoals(x -> true);
            m.setPersistenceRequired();
            m.setSilent(true);
            Motion.snapYaw(m, yaw);
        }
        if (!lvl.addFreshEntity(e)) return null;
        body = e.getUUID();
        if (anim != null) Motion.pose(e, anim, now);
        placedAt = now;
        away = 0;
        stare = 0;
        hideDelay = 8 + p.getRandom().nextInt(26);
        return e;
    }

    static List<ServerPlayer> viewers(Entity e) {
        return Walker.viewers(e);
    }

    boolean seen(Entity e) {
        return Walker.seenAtAll(viewers(e), e.getBoundingBox());
    }

    static boolean looking(ServerPlayer p, Watch w, Entity e) {
        if (e.distanceTo(p) > Range.of(p, w) * 1.05 + 4) return false;
        Sight si = Sight.of(p, w, e.getBoundingBox().getCenter());
        return si.on && si.edge() < 0.6 && Sight.visible(p, e) > 0.25;
    }

    static boolean onScreen(ServerPlayer p, Watch w, Entity e) {
        Sight si = Sight.of(p, w, e.getBoundingBox().getCenter());
        return si.on && Sight.visible(p, e) > 0.12;
    }

    static boolean dark(ServerPlayer p) {
        if (Watch.held(p) > 6) return false;
        if (Dark.on(p)) return Dark.dark(p);
        ServerLevel lvl = p.serverLevel();
        BlockPos eye = BlockPos.containing(p.getEyePosition());
        return lvl.getMaxLocalRawBrightness(eye) <= 6 || !lvl.canSeeSky(eye) && lvl.getBrightness(LightLayer.BLOCK, eye) <= 6;
    }

    static int bright(ServerPlayer p) {
        ServerLevel lvl = p.serverLevel();
        BlockPos eye = BlockPos.containing(p.getEyePosition());
        if (Dark.on(p)) return Math.max(Watch.held(p), lvl.getBrightness(LightLayer.SKY, eye) - Math.max(lvl.getSkyDarken(), 7));
        return Math.max(Watch.held(p), lvl.getMaxLocalRawBrightness(eye));
    }

    static boolean hiding(ServerPlayer p, Watch w) {
        return p.isCrouching() && w.speed < 0.035 && dark(p);
    }

    boolean to(ServerPlayer p, S next, long now, String why) {
        CaveMind.debug("damned game {}: {} -> {} ({})", p.getGameProfile().getName(), s, next, why);
        s = next;
        since = now;
        reason = why;
        if ((next == S.RETREAT || next == S.CAUGHT) && ending.isEmpty()) ending = why;
        if (next == S.MARK) marked = true;
        stare = 0;
        lightTicks = 0;
        Entity e = body(p);
        if (e instanceof Mob m) {
            m.getNavigation().stop();
            m.setAggressive(false);
        }
        walker = null;
        return next != S.DONE;
    }

    boolean step(ServerPlayer p, Watch w, long now) {
        if (!p.isAlive() || p.level().dimension() != Level.OVERWORLD || Compat.exempt(p)) return false;
        if (s == S.DONE) return false;
        if (now - since > LIMIT.getOrDefault(s, 600)) return timeout(p, w, now);
        Entity e = body(p);
        if (e == null && s != S.SEEK && s != S.MARK && s != S.RETREAT) {
            if (s == S.CAUGHT) return false;
            return to(p, S.RETREAT, now, "mất thân");
        }
        if (p.isSleeping() && !slept && (s == S.COUNT || s == S.SEEK || s == S.WARN)) {
            slept = true;
            strike(p, w, now, "ngủ giữa ván chơi");
        } else if (!p.isSleeping()) slept = false;
        return switch (s) {
            case INVITE -> invite(p, w, e, now);
            case COUNT -> count(p, w, e, now);
            case SEEK -> seek(p, w, e, now);
            case WARN -> warn(p, w, e, now);
            case MARK -> mark(p, w, e, now);
            case HUNT -> hunt(p, w, e, now);
            case LOST -> lost(p, w, e, now);
            case CAUGHT -> caught(p, w, e, now);
            case RETREAT -> retreat(p, w, e, now);
            default -> false;
        };
    }

    boolean timeout(ServerPlayer p, Watch w, long now) {
        return switch (s) {
            case INVITE -> to(p, S.COUNT, now, "bắt đầu đếm dù chưa ai thấy");
            case COUNT -> to(p, S.SEEK, now, "đếm quá lâu");
            case SEEK -> {
                Entity e = body(p);
                // Keeping it on screen freezes it, so watching it for the whole round is not hiding: it knew where your eyes were.
                if (e != null && watched * 2 > LIMIT.get(S.SEEK)) {
                    tell(p, "Bạn nhìn nó suốt cả ván. Nó biết bạn ở đâu.");
                    yield found(p, w, e, now);
                }
                win(p, now, "nó tìm chán rồi bỏ đi");
                yield to(p, S.RETREAT, now, "hết giờ tìm");
            }
            case WARN -> {
                if (ignoring(p, w)) {
                    strike(p, w, now, "vẫn bỏ đi");
                    yield true;
                }
                yield to(p, S.COUNT, now, "bạn quay lại chơi");
            }
            case MARK -> {
                if (Tension.allows(p, Tension.Permit.ENGAGE) && !peaceful(p)) yield startHunt(p, w, now);
                MindData.profile(p).dDebt = Math.min(3, MindData.profile(p).dDebt + 1);
                yield to(p, S.RETREAT, now, "chưa tới lúc, nó nhớ món nợ");
            }
            case HUNT -> to(p, S.RETREAT, now, "hết giờ chơi");
            case LOST -> to(p, S.RETREAT, now, "mất dấu hẳn");
            case CAUGHT -> false;
            default -> to(p, S.DONE, now, "hết hạn");
        };
    }

    boolean invite(ServerPlayer p, Watch w, long now) {
        double sight = Range.of(p, w);
        boolean test = cause.equals("lệnh thử");
        double a = test ? 9 : Math.max(10, sight * 0.5), b = test ? 26 : Math.max(16, sight * 0.72);
        Spots.Cand c = Spots.find(p, w, new Spots.Spec().ring(a, b).dir(0, 180).body(W, H).cave(w.underground).tries(test ? 160 : 70)
            .when(k -> !k.sight.on && (test || k.vis >= 0.35) && Math.abs(k.feet.getY() - p.getY()) < 10 && !vn.cavemind.sense.Group.boxSeenByOthers(p, k.box, 96)
                && (test || post(p.serverLevel(), k.feet) != null))
            .rank(k -> k.vis + (k.trunk ? 0.6 : 0) + (post(p.serverLevel(), k.feet) != null ? 0.8 : 0) + (8 - Math.min(8, k.light)) * 0.03));
        if (c == null) return false;
        Direction face = post(p.serverLevel(), c.feet);
        float yaw = face != null ? face.toYRot() : HideSeekYaw.toward(c.center, p.position());
        if (put(p, c.feet, yaw, "cm_count", now) == null) return false;
        tree = face != null ? c.feet.relative(face) : c.feet;
        return true;
    }

    static Direction post(ServerLevel lvl, BlockPos feet) {
        for (Direction d : Direction.Plane.HORIZONTAL) {
            BlockState st = lvl.getBlockState(feet.above().relative(d));
            if (st.is(BlockTags.LOGS)) return d;
        }
        for (Direction d : Direction.Plane.HORIZONTAL) {
            BlockPos q = feet.above().relative(d);
            if (lvl.getBlockState(q).isFaceSturdy(lvl, q, d.getOpposite())) return d;
        }
        return null;
    }

    boolean invite(ServerPlayer p, Watch w, Entity e, long now) {
        if (onScreen(p, w, e)) {
            perceive += 0.6;
            if (!noticed && perceive >= 4) {
                noticed = true;
                Tension.seen(p, e.distanceTo(p), "The Damned");
            }
        }
        if (noticed && now - since > 30 || now - since > 220 && e.distanceTo(p) < Range.of(p, w) * 1.2) {
            beat = 0;
            nextBeat = now;
            return to(p, S.COUNT, now, noticed ? "bạn đã thấy nó" : "nó bắt đầu đếm");
        }
        return true;
    }

    boolean count(ServerPlayer p, Watch w, Entity e, long now) {
        Profile pr = MindData.profile(p);
        if (now >= nextBeat) {
            beat++;
            nextBeat = now + 20;
            String line = "…" + num(beat) + "…";
            if (beat == 1 && Dark.start(p) > 0 && pr.dGames <= 3) line += "  (đèn quanh đây tắt hết: núp sau cây, bụi, đá hay trong nhà rồi ngồi thụp xuống)";
            else if (beat == 1 && pr.dGames <= 2) line += "  (nó đang đếm: đi trốn đi, đừng nhìn nó)";
            say(p, line, ChatFormatting.GRAY);
            if (beat == 1 && owed > 0) tell(p, owed == 1 ? "Bạn còn nợ nó một ván. Lần này nó không nhắc nữa: phạm luật là nó nhớ mặt bạn."
                : "Bạn nợ nó hai ván. Phạm luật một lần thôi là nó săn bạn.");
            vn.cavemind.compat.Radio.relay(p, "…" + num(beat) + "…", beat);
            if (beat % 6 == 1 && e.distanceTo(p) < 40) Haunt.breath(p, e.getEyePosition(), 0.25f);
            Tension.stress(p, 0.4, "tiếng đếm");
        }
        if (looking(p, w, e) && beat >= 6) {
            if (++stare >= 40) {
                strike(p, w, now, "nhìn trộm lúc nó đang đếm");
                return true;
            }
        } else {
            stare = Math.max(0, stare - 2);
        }
        if (beat >= 48) {
            if (ignoring(p, w)) {
                strike(p, w, now, "bạn bỏ đi giữa ván chơi");
                return true;
            }
            say(p, "…bốn mươi tám. Đi tìm đây.", ChatFormatting.DARK_GRAY);
            vn.cavemind.compat.Radio.relay(p, "…bốn mươi tám. Đi tìm đây.", 48);
            if (active || pr.dGames >= 2) kids.addAll(Kids.hide(p, w, p.getRandom().nextFloat() < 0.5f ? 2 : 1));
            if (!kids.isEmpty() && pr.dGames <= 3) Fx.later(p.serverLevel(), 60, () -> say(p, "Có những đứa trẻ khác cũng đang trốn quanh đây.", ChatFormatting.GRAY));
            lastDist = e.distanceTo(p);
            nextPlace = now + 20 + p.getRandom().nextInt(30);
            Vec3 at = e.position();
            Fx.sound(p, Compat.sound("apollyon:light_suspense", "minecraft:ambient.cave"), SoundSource.HOSTILE, at, 0.5f, 1.1f);
            return to(p, S.SEEK, now, "đếm xong");
        }
        return true;
    }

    boolean ignoring(ServerPlayer p, Watch w) {
        Entity e = body(p);
        double sight = Range.of(p, w);
        Vec3 from = e != null ? e.position() : tree != null ? Vec3.atCenterOf(tree) : p.position();
        if (from.distanceTo(p.position()) > Math.max(56, sight * 2)) return true;
        if (hiding(p, w)) return false;
        Ears.Noise n = Ears.lastBy(p.getUUID(), 160);
        return n != null && (n.kind() == Ears.Kind.MINE || n.kind() == Ears.Kind.BUILD);
    }

    boolean seek(ServerPlayer p, Watch w, Entity e, long now) {
        Profile pr = MindData.profile(p);
        ServerLevel lvl = p.serverLevel();
        if (holdStage == 1) return hold(p, w, e, now);
        int need = (240 + Math.min(4, pr.hidden) * 80 + finds * 40) * (active || pr.lore.contains("g") || pr.lore.contains("h") || pr.lore.contains("p") ? 1 : 2) + gate * 30;
        boolean on = e != null && onScreen(p, w, e);
        hideTicks = hiding(p, w) && !on ? hideTicks + 1 : Math.max(0, hideTicks - 3);
        if (hideTicks >= need) {
            win(p, now, "bạn trốn đúng luật");
            return to(p, S.RETREAT, now, "nó không tìm ra bạn");
        }
        boolean scene = kidStage == 1 || kidStage == 2;
        if (!inspected && !scene && hideTicks >= need - 200 && hiding(p, w) && (active || pr.dGames >= 2) && inspect(p, w, now)) return true;
        if (now % 20 == 0) {
            for (UUID id : kids) {
                Entity k = Kids.get(lvl, id);
                if (k == null) continue;
                Vec3 c = k.getBoundingBox().getCenter();
                lvl.sendParticles(p, net.minecraft.core.particles.ParticleTypes.WHITE_ASH, true, c.x, c.y, c.z, 6, 0.35, 0.4, 0.35, 0.01);
            }
        }
        if (kidStage == 0 && !kids.isEmpty() && now - since > 260 && hideTicks > 80 && kidStart(p, w, now)) return true;
        if (scene) return kidScene(p, w, now);
        if (e != null) {
            double dist = e.distanceTo(p);
            if (on) watched++;
            if (on && looking(p, w, e)) {
                // crouching in the dark buys time (24 s instead of 12) but staring back still gives you away
                stare += hiding(p, w) ? 1 : 2;
                if (stare >= 480) {
                    say(p, "Nhìn thẳng vào nó là nó thấy bạn.", ChatFormatting.DARK_GRAY);
                    return found(p, w, e, now);
                }
            } else {
                stare = Math.max(0, stare - 3);
            }
            if (on) {
                away = 0;
                if (dist < 24 && bright(p) >= 11 && !dark(p)) {
                    if (++lightTicks >= 120) {
                        strike(p, w, now, "đứng giữa chỗ sáng lúc nó đang tìm");
                        return true;
                    }
                }
            } else {
                away++;
            }
            Ears.Noise heard = Ears.lastBy(p.getUUID(), 20);
            if (heard != null && heard.kind() == Ears.Kind.VOICE && heard.radius() >= 6 && heard.pos().distanceTo(e.position()) < 40) {
                away = Math.max(away, hideDelay);
                nextPlace = Math.min(nextPlace, now);
                lastDist = Math.min(lastDist, 10);
            }
            if (dist < 2.6 || dist < 5.5 && Sight.clear(lvl, e.getEyePosition(), p.getEyePosition(), e) && !hiding(p, w)) return found(p, w, e, now);
            if (away >= hideDelay && !seen(e) && now >= nextPlace) {
                Vec3 from = e.position();
                lastDist = dist;
                discard(lvl);
                if (!place(p, w, now)) nextPlace = now + 20;
                else steps(p, from, body(p));
            }
            return true;
        }
        if (now >= nextPlace) {
            if (!place(p, w, now)) nextPlace = now + 20;
        }
        return true;
    }

    boolean place(ServerPlayer p, Watch w, long now) {
        RandomSource r = p.getRandom();
        Vec3 guess = p.position();
        Ears.Noise n = Ears.lastBy(p.getUUID(), 600);
        if (n != null && n.pos().distanceTo(p.position()) < 24) guess = n.pos();
        else guess = guess.add((r.nextDouble() - 0.5) * 16, 0, (r.nextDouble() - 0.5) * 16);
        double d = Mth.clamp(lastDist * (0.6 + r.nextDouble() * 0.25), 2.5, 44);
        Vec3 target = guess;
        Spots.Cand c = Spots.find(p, w, new Spots.Spec().ring(Math.max(2.2, d - Math.min(5, d * 0.4)), d + Math.min(5, d * 0.4) + 0.5).dir(0, 180).body(W, H).cave(w.underground).tries(50)
            .when(k -> !k.sight.on && k.vis >= 0.3 && Math.abs(k.feet.getY() - p.getY()) < 10 && !vn.cavemind.sense.Group.boxSeenByOthers(p, k.box, 96))
            .rank(k -> -k.center.distanceTo(target) * 0.05 + k.vis * 0.5 + (k.trunk ? 0.4 : 0) + (k.cover ? 0.2 : 0)));
        if (c == null) return false;
        String[] poses = {"cm_search_a", "cm_search_b", "cm_peek", "cm_low", "cm_still"};
        String anim = c.trunk && r.nextFloat() < 0.4f ? "cm_peek" : poses[r.nextInt(poses.length)];
        float yaw = HideSeekYaw.toward(c.center, target) + (anim.startsWith("cm_search") ? 60 + r.nextFloat() * 240 : 0);
        if (put(p, c.feet, yaw, anim, now) == null) return false;
        placements++;
        nextPlace = now + 30 + r.nextInt(40);
        return true;
    }

    boolean found(ServerPlayer p, Watch w, Entity e, long now) {
        Profile pr = MindData.profile(p);
        if (e instanceof Mob m) Motion.snap(m, p.getEyePosition());
        Motion.play(e, "cm_point", 1 << 30, now);
        holdUntil = now + 64;
        Vec3 hand = e.position().add(0, 1.3, 0);
        ServerLevel lvl = p.serverLevel();
        for (int t : new int[]{38, 44, 50}) Fx.later(lvl, t, () -> Fx.sound(p, Holder.direct(SoundEvents.BONE_BLOCK_HIT), SoundSource.HOSTILE, hand, 0.35f, 0.55f));
        say(p, "Tìm thấy rồi.", ChatFormatting.DARK_GRAY);
        Tension.seen(p, e.distanceTo(p), "The Damned");
        finds++;
        pr.dFound++;
        pr.moment(new Moment(Moment.SEEK, Dread.day(lvl), Dread.time(lvl), e.blockPosition(), placements, 2));
        PaceLog.encounter(p, "damned", "found you", placements + " moves");
        vn.cavemind.life.Child.played(p.server, 40);
        // Losing costs a game owed. Already owing one, the loser has to count: it hunts you once it has pointed.
        lost = true;
        boolean owing = pr.dDebt > 0;
        pr.dDebt = Math.min(3, pr.dDebt + 1);
        payUp = owing;
        if (!payUp) Fx.later(lvl, 70, () -> tell(p, "Bạn thua ván này. Giờ bạn nợ nó một ván."));
        return to(p, S.RETREAT, now, "tìm thấy bạn");
    }

    void win(ServerPlayer p, long now, String why) {
        Profile pr = MindData.profile(p);
        ServerLevel lvl = p.serverLevel();
        victory = true;
        pr.hidden++;
        pr.dWins++;
        pr.moment(new Moment(Moment.HID, Dread.day(lvl), Dread.time(lvl), p.blockPosition(), placements, 0));
        Entity e = body(p);
        if (e != null) Fx.sound(p, Holder.direct(SoundEvents.CANDLE_EXTINGUISH), SoundSource.HOSTILE, e.position(), 0.5f, 0.6f);
        PaceLog.encounter(p, "damned", "you won", why);
        Compat.grant(p, "cavemind:ban_suong/co_nguoi_choi_cung");
        vn.cavemind.life.Child.played(p.server, 55);
    }

    void strike(ServerPlayer p, Watch w, long now, String why) {
        Profile pr = MindData.profile(p);
        strikes++;
        boolean firstEver = pr.dPunish == 0;
        PaceLog.encounter(p, "damned", "strike " + strikes, why);
        if (strikes == 1 || firstEver && strikes < 3) {
            if (strikes == 1) warnTo(p, w, now, why);
            else {
                pr.dPunish++;
                to(p, S.WARN, now, why);
                say(p, firstEver ? "Lần này thôi." : "Đừng.", ChatFormatting.DARK_RED);
            }
            return;
        }
        pr.dPunish++;
        if (strikes == 2) {
            to(p, S.MARK, now, why);
            say(p, "Nó nhớ mặt bạn rồi.", ChatFormatting.DARK_RED);
            return;
        }
        if (Tension.allows(p, Tension.Permit.ENGAGE) && !peaceful(p)) startHunt(p, w, now);
        else {
            pr.dDebt = Math.min(3, pr.dDebt + 1);
            to(p, S.MARK, now, why + " (chưa tới lúc săn)");
        }
    }

    void warnTo(ServerPlayer p, Watch w, long now, String why) {
        to(p, S.WARN, now, why);
        Entity e = body(p);
        ServerLevel lvl = p.serverLevel();
        String line = why.contains("nhìn") ? "Không được nhìn." : why.contains("bỏ") || why.contains("đi") ? "Đừng bỏ đi." : why.contains("sáng") ? "Ra khỏi chỗ sáng." : "Không được làm thế.";
        say(p, line, ChatFormatting.DARK_RED);
        if (e != null) {
            Motion.play(e, "cm_owl", 1 << 30, now);
            Vec3 at = e.getEyePosition();
            for (int i = 0; i < 3; i++) Fx.later(lvl, 18 + i * 17, () -> Fx.sound(p, Holder.direct(SoundEvents.BAMBOO_WOOD_BREAK), SoundSource.HOSTILE, at, 0.22f, 0.5f));
            Haunt.breath(p, e.getEyePosition(), 0.4f);
        }
        douse(lvl, p.blockPosition(), 12);
        Profile pr = MindData.profile(p);
        if (pr.dPunish == 0 && !warnedSign) {
            warnedSign = true;
            BlockPos at = tree != null ? tree : e != null ? e.blockPosition() : null;
            if (at != null) note(lvl, at);
        }
    }

    boolean warn(ServerPlayer p, Watch w, Entity e, long now) {
        if (now - since < 40) return true;
        double sight = Range.of(p, w);
        boolean back = e.distanceTo(p) < sight * 1.5 && (hiding(p, w) || p.isCrouching());
        if (back && now - since > 80) {
            beat = 0;
            nextBeat = now + 20;
            say(p, "…", ChatFormatting.GRAY);
            if (e instanceof Mob m && tree != null) Motion.snap(m, Vec3.atCenterOf(tree));
            Motion.pose(e, "cm_count", now);
            return to(p, S.COUNT, now, "bạn quay lại chơi, nó đếm lại");
        }
        if (looking(p, w, e) && now - since > 120) {
            if (++stare >= 50) strike(p, w, now, "lại nhìn trộm");
        }
        return true;
    }

    boolean mark(ServerPlayer p, Watch w, Entity e, long now) {
        ServerLevel lvl = p.serverLevel();
        if (now % 40 == 0) Haunt.heartbeat(p, 0.6f);
        if (now % 60 == 0) {
            for (Animal a : lvl.getEntitiesOfClass(Animal.class, p.getBoundingBox().inflate(24, 8, 24), a -> !Compat.managed(a))) {
                Vec3 from = e != null ? e.position() : p.position();
                Vec3 run = a.position().add(a.position().subtract(from).normalize().scale(14));
                a.getNavigation().moveTo(run.x, run.y, run.z, 1.6);
            }
        }
        hideTicks = hiding(p, w) && (e == null || !onScreen(p, w, e)) ? hideTicks + 1 : Math.max(0, hideTicks - 2);
        if (hideTicks >= 260) {
            say(p, "…", ChatFormatting.GRAY);
            vn.cavemind.life.Child.played(p.server, 20);
            return to(p, S.RETREAT, now, "bạn chịu trốn đúng luật");
        }
        if (e == null) {
            if (now >= nextPlace) closer(p, w, now, 14);
            return true;
        }
        boolean on = onScreen(p, w, e);
        away = on ? 0 : away + 1;
        if (on && looking(p, w, e)) {
            Motion.pose(e, "cm_lean", now);
        }
        double dist = e.distanceTo(p);
        if (away >= 12 && !seen(e) && dist > 4.5) {
            discard(lvl);
            closer(p, w, now, Math.max(4, dist * 0.5));
        }
        return true;
    }

    void closer(ServerPlayer p, Watch w, long now, double d) {
        Spots.Cand c = Spots.find(p, w, new Spots.Spec().ring(Math.max(3.5, d - 2), d + 2).dir(180, 110).body(W, H).cave(w.underground).tries(40)
            .when(k -> !k.sight.on && k.vis >= 0.4 && Math.abs(k.feet.getY() - p.getY()) < 6 && !vn.cavemind.sense.Group.boxSeenByOthers(p, k.box, 96))
            .rank(k -> k.vis));
        if (c == null) {
            nextPlace = now + 20;
            return;
        }
        put(p, c.feet, HideSeekYaw.toward(c.center, p.getEyePosition()), "cm_still", now);
        Tension.stress(p, 3, "nó lại gần hơn");
    }

    boolean startHunt(ServerPlayer p, Watch w, long now) {
        ServerLevel lvl = p.serverLevel();
        Entity e = body(p);
        if (e == null) {
            closer(p, w, now, 18);
            e = body(p);
            if (e == null) return to(p, S.RETREAT, now, "không có chỗ để săn");
        }
        hunted = true;
        to(p, S.HUNT, now, "lần phạt thứ ba");
        telegraphUntil = now + 60;
        lastSeen = p.position();
        lastSeenAt = now;
        say(p, "AI THUA THÌ ĐẾM", ChatFormatting.DARK_RED);
        Fx.sound(p, Compat.sound("apollyon:intense_rise", "minecraft:entity.warden.roar"), SoundSource.HOSTILE, e.position(), 1.2f, 1.0f);
        if (Cfg.SILENCE.get()) Net.fx(p, Net.SILENCE, 0, 60);
        douse(lvl, p.blockPosition(), 12);
        p.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 70, 0, false, false));
        Motion.play(e, "animation.theDamned.spotted", 60, now);
        PaceLog.encounter(p, "damned", "hunt", reason);
        return true;
    }

    boolean hunt(ServerPlayer p, Watch w, Entity e, long now) {
        ServerLevel lvl = p.serverLevel();
        if (!(e instanceof Mob m)) return to(p, S.RETREAT, now, "mất thân");
        if (now % 20 == 0) Tension.chased(p);
        if (now < telegraphUntil) {
            Motion.snap(m, p.getEyePosition());
            return true;
        }
        if (now == telegraphUntil || walker == null) {
            Motion.stop(m);
            Fx.soundAll(lvl, "apollyon:gutteral_roar", SoundSource.HOSTILE, e.position(), 2.2f, 1.15f);
            walker = new Walker(p.blockPosition(), Walker.pace(m, 0.3), Stealth.Mode.PLAIN);
            walker.freezes = false;
            walker.doors = true;
            walker.gait = null;
            walker.arrive = 0.6;
        }
        if (now % 120 == 0) Fx.sound(p, Compat.sound("apollyon:chase_music", "minecraft:entity.warden.heartbeat"), SoundSource.HOSTILE, e.position(), 0.8f, 1.0f);
        double dist = e.distanceTo(p);
        if (now % 3 == 0) los = Sight.clear(lvl, e.getEyePosition(), p.getEyePosition(), e) && dist < 48;
        if (los) {
            lastSeen = p.position();
            lastSeenAt = now;
            lostTicks = 0;
        } else {
            Ears.Noise n = Ears.lastBy(p.getUUID(), 30);
            if (n != null && n.pos().distanceTo(e.position()) < n.radius()) {
                lastSeen = n.pos();
                lastSeenAt = now - 20;
            }
            lostTicks++;
        }
        if (dist < 1.4) return grab(p, w, m, now);
        boolean escaped = !los && (lostTicks >= 120 && hiding(p, w) || lostTicks >= 240);
        if (escaped) {
            sweep.clear();
            return to(p, S.LOST, now, "bạn cắt được tầm nhìn của nó");
        }
        m.setAggressive(dist < 9);
        // blocks a tick: a little faster than your sprint while far off, a little slower up close (walking never gets you away)
        walker.speed = Walker.pace(m, dist > 14 ? 0.31 : 0.27);
        if (now % 10 == 0) walker.retarget(BlockPos.containing(lastSeen));
        Walker.Step st = walker.tick(m, now, viewers(e));
        if (st == Walker.Step.FAILED) {
            walker = null;
            if (!seen(e)) closer(p, w, now, Math.max(6, dist * 0.7));
        }
        // it crawls up walls and pillars after you instead of waiting at the bottom
        if (m.horizontalCollision && p.getY() > m.getY() + 0.6 && dist < 16) {
            Vec3 v = m.getDeltaMovement();
            m.setDeltaMovement(v.x * 0.6, 0.24, v.z * 0.6);
            m.fallDistance = 0;
        }
        stuck(p, m, now);
        return true;
    }

    void stuck(ServerPlayer p, Mob m, long now) {
        if (walker == null || walker.fails < 3 || !seen(m)) {
            stuckShown = false;
            return;
        }
        if (!stuckShown) {
            stuckShown = true;
            Motion.play(m, "animation.theDamned.spotted", 40, now);
            Fx.sound(p, "apollyon:damage_noise", SoundSource.HOSTILE, m.position(), 1.0f, 1.3f);
        }
    }

    boolean grab(ServerPlayer p, Watch w, Mob m, long now) {
        caught = true;
        grip = 4;
        to(p, S.CAUGHT, now, "chạm được vào bạn");
        Vec3 front = p.getEyePosition().add(Vec3.directionFromRotation(0, p.getYRot()).scale(1.1));
        m.getNavigation().stop();
        m.teleportTo(front.x, p.getY(), front.z);
        Motion.snap(m, p.getEyePosition());
        Motion.play(m, "animation.theDamned.jumpscare", 60, now);
        Fx.sound(p, Compat.sound("apollyon:jumpscare_sound", "minecraft:entity.warden.roar"), SoundSource.HOSTILE, m.position(), 1.0f, 1.0f);
        Net.fx(p, Net.SHAKE, 2.4f, 20);
        p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 70, 6, false, false));
        p.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 80, 0, false, false));
        Profile pr = MindData.profile(p);
        pr.dCaught++;
        ServerLevel lvl = p.serverLevel();
        pr.moment(new Moment(Moment.CAUGHT, Dread.day(lvl), Dread.time(lvl), p.blockPosition(), strikes, 0));
        PaceLog.encounter(p, "damned", "caught", reason);
        return true;
    }

    private static final Map<UUID, Long> WRENCH_DAY = new HashMap<>();

    // In the two and a half seconds it holds someone before dragging them under, a friend can tear them loose: a heavy
    // blow does it at once, full swings take two. Ai thua thì đếm: the one who interfered takes over the game owed.
    void wrench(ServerPlayer owner, ServerPlayer by, int w, boolean heavy, long now) {
        grip -= w;
        Entity e = body(owner);
        if (grip > 0 || e == null) return;
        ServerLevel lvl = owner.serverLevel();
        Vec3 at = e.position();
        owner.removeEffect(MobEffects.MOVEMENT_SLOWDOWN);
        owner.removeEffect(MobEffects.DARKNESS);
        Fx.soundAll(lvl, "apollyon:deep_laugh", SoundSource.HOSTILE, at, 0.7f, 0.6f);
        Fx.soundAll(lvl, "minecraft:block.candle.extinguish", SoundSource.HOSTILE, at, 1.0f, 0.5f);
        Profile op = MindData.profile(owner), bp = MindData.profile(by);
        op.dDebt = Math.max(0, op.dDebt - 1);
        bp.dDebt = Math.min(3, bp.dDebt + 1);
        tell(owner, by.getGameProfile().getName() + " giằng bạn khỏi tay nó.");
        tell(by, "Bạn giằng " + owner.getGameProfile().getName() + " khỏi tay nó. Ai thua thì đếm: giờ bạn nợ nó một ván.");
        long day = Dread.day(lvl);
        Long last = WRENCH_DAY.get(by.getUUID());
        if ((last == null || last != day) && by.getRandom().nextFloat() < (heavy ? 0.6f : 0.3f)) {
            WRENCH_DAY.put(by.getUUID(), day);
            net.minecraft.world.entity.item.ItemEntity ie = new net.minecraft.world.entity.item.ItemEntity(lvl, at.x, at.y + 0.5, at.z,
                new net.minecraft.world.item.ItemStack(vn.cavemind.gear.Gear.ECHO.get()));
            ie.setDefaultPickUpDelay();
            lvl.addFreshEntity(ie);
        }
        holdUntil = now + 20;
        PaceLog.encounter(owner, "damned", "wrenched free", by.getGameProfile().getName());
        to(owner, S.RETREAT, now, "bạn bè giằng bạn lại");
    }

    boolean caught(ServerPlayer p, Watch w, Entity e, long now) {
        if (e instanceof Mob m) Motion.snap(m, p.getEyePosition());
        if (now - since < 50) return true;
        discard(p.serverLevel());
        vn.cavemind.life.Child.played(p.server, 30);
        if (!vn.cavemind.maze.Maze.drag(p, vn.cavemind.maze.Maze.Mode.PUNISH)) {
            if (!peaceful(p)) p.hurt(p.serverLevel().damageSources().magic(), Math.min(p.getHealth() - 1f, 10f));
            say(p, "Ai thua thì đếm.", ChatFormatting.DARK_RED);
        }
        return to(p, S.DONE, now, "kéo đi");
    }

    boolean lost(ServerPlayer p, Watch w, Entity e, long now) {
        ServerLevel lvl = p.serverLevel();
        if (!(e instanceof Mob m)) return to(p, S.RETREAT, now, "mất thân");
        double dist = e.distanceTo(p);
        if (now % 4 == 0 && dist < 20 && Sight.clear(lvl, e.getEyePosition(), p.getEyePosition(), e) && !hiding(p, w)) {
            to(p, S.HUNT, now, "thấy lại bạn");
            telegraphUntil = now;
            lastSeen = p.position();
            return true;
        }
        if (sweep.isEmpty()) {
            Vec3 c = lastSeen != null ? lastSeen : p.position();
            RandomSource r = p.getRandom();
            for (int i = 0; i < 5; i++) {
                double a = r.nextDouble() * Math.PI * 2, d = 4 + r.nextDouble() * 7;
                sweep.add(c.add(Math.cos(a) * d, 0, Math.sin(a) * d));
            }
            walker = new Walker(BlockPos.containing(sweep.get(0)), Walker.pace(m, 0.2), Stealth.Mode.PLAIN);
            walker.freezes = false;
            walker.doors = true;
            walker.gait = null;
        }
        m.setAggressive(false);
        if (walker == null) return true;
        Walker.Step st = walker.tick(m, now, viewers(e));
        if (st == Walker.Step.ARRIVED || st == Walker.Step.FAILED) {
            sweep.remove(0);
            Motion.play(m, p.getRandom().nextBoolean() ? "cm_search_a" : "cm_search_b", 50, now);
            if (sweep.isEmpty()) return to(p, S.RETREAT, now, "lục tung xung quanh không thấy");
            walker = new Walker(BlockPos.containing(sweep.get(0)), Walker.pace(m, 0.2), Stealth.Mode.PLAIN);
            walker.freezes = false;
            walker.doors = true;
            walker.gait = null;
        }
        return true;
    }

    boolean retreat(ServerPlayer p, Watch w, Entity e, long now) {
        if (e == null) return to(p, S.DONE, now, "đã đi");
        ServerLevel lvl = p.serverLevel();
        if (now < holdUntil) {
            if (e instanceof Mob m) Motion.snap(m, p.getEyePosition());
            return true;
        }
        if (payUp) {
            payUp = false;
            if (Tension.allows(p, Tension.Permit.ENGAGE) && !peaceful(p)) return startHunt(p, w, now);
            tell(p, "Bạn lại thua. Nó chưa đuổi theo, nhưng nó nhớ cả hai ván.");
        }
        if (e instanceof Mob m && !posed) {
            posed = true;
            m.getNavigation().stop();
            m.setAggressive(false);
            Motion.pose(m, "cm_profile", now);
        }
        long held = now - Math.max(since, holdUntil);
        boolean watched = seen(e);
        if (held > 30 && !watched) {
            BlockPos at = e.blockPosition();
            discard(lvl);
            candle(lvl, at);
            return to(p, S.DONE, now, "để lại nến tắt");
        }
        if (watched && held > 24) {
            vanish(lvl, e);
            return to(p, S.DONE, now, "tắt như ngọn nến");
        }
        return true;
    }

    void vanish(ServerLevel lvl, Entity e) {
        AABB box = e.getBoundingBox();
        Vec3 c = box.getCenter();
        lvl.sendParticles(net.minecraft.core.particles.ParticleTypes.SQUID_INK, c.x, c.y, c.z, 46, box.getXsize() * 0.45, box.getYsize() * 0.4, box.getZsize() * 0.45, 0.02);
        lvl.sendParticles(net.minecraft.core.particles.ParticleTypes.SMOKE, c.x, box.maxY - 0.2, c.z, 12, 0.2, 0.1, 0.2, 0.01);
        Fx.soundAll(lvl, "minecraft:block.candle.extinguish", SoundSource.HOSTILE, c, 1.0f, 0.55f);
        BlockPos at = e.blockPosition();
        discard(lvl);
        candle(lvl, at);
    }

    void finish(ServerPlayer p, long now) {
        GAMES.remove(p.getUUID());
        discard(p.serverLevel());
        Kids.clear(p.serverLevel(), kids);
        if (holdStage == 1) Net.fx(p, Net.HOLD, 0f, 0);
        Dark.end(p);
        Director.release(p, "damned", hunted || caught || marked);
        Profile pr = MindData.profile(p);
        if (strikes == owed && !hunted && !lost) pr.dDebt = Math.max(0, pr.dDebt - 1);
        if (pr.dStage < 1) pr.dStage = 1;
        if (active) vn.cavemind.chapter.Chapters.ended(p, gate, victory, tree, ending.isEmpty() ? reason : ending);
        CaveMind.debug("damned game with {} ends ({}, strikes {})", p.getGameProfile().getName(), reason, strikes);
    }

    public static void banished(ServerPlayer p) {
        Game g = GAMES.remove(p.getUUID());
        if (g == null) return;
        Entity e = g.body(p);
        if (e != null) Fx.soundAll(p.serverLevel(), "apollyon:used_rose", SoundSource.AMBIENT, e.position(), 1f, 1f);
        g.discard(p.serverLevel());
        Kids.clear(p.serverLevel(), g.kids);
        if (g.holdStage == 1) Net.fx(p, Net.HOLD, 0f, 0);
        Dark.end(p);
        Director.release(p, "damned", true);
        vn.cavemind.life.Child.played(p.server, 60);
    }

    public static void attacked(ServerPlayer p, Entity e, vn.cavemind.sense.Swing.Blow b) {
        for (Game g : GAMES.values()) {
            if (!e.getUUID().equals(g.body)) continue;
            ServerPlayer owner = p.server.getPlayerList().getPlayer(g.player);
            if (owner == null) return;
            long now = p.server.getTickCount();
            int w = b == null ? 1 : b.weight();
            Fx.sound(p, "apollyon:damage_noise", SoundSource.HOSTILE, e.position(), 0.8f, 1.4f);
            if (e instanceof Mob m) {
                m.getNavigation().stop();
                if (g.walker != null) g.walker.stop(m);
            }
            if (g.s == S.HUNT || g.s == S.LOST) {
                // a heavy blow holds it for two and a half seconds, a full one for a second, a rushed one hardly at all
                g.telegraphUntil = Math.max(g.telegraphUntil, now + (w >= 4 ? 50 : w >= 2 ? 20 : 6));
                if (w >= 4) {
                    Vec3 push = e.position().subtract(p.position()).multiply(1, 0, 1);
                    if (push.lengthSqr() > 1e-4) {
                        push = push.normalize().scale(0.7);
                        e.setDeltaMovement(push.x, 0.3, push.z);
                        e.hurtMarked = true;
                    }
                }
                return;
            }
            if (g.s == S.CAUGHT && p != owner) {
                g.wrench(owner, p, w, b != null && b.heavy(), now);
                return;
            }
            if (g.s == S.CAUGHT || g.s == S.RETREAT) return;
            g.strike(owner, Watch.of(owner), now, "đánh nó");
            return;
        }
    }

    public static boolean bell(ServerPlayer ringer, BlockPos at) {
        boolean any = false;
        for (Game g : new ArrayList<>(GAMES.values())) {
            ServerPlayer p = ringer.server.getPlayerList().getPlayer(g.player);
            if (p == null || p.level() != ringer.level()) continue;
            Entity e = g.body(p);
            Vec3 c = Vec3.atCenterOf(at);
            boolean near = p.position().distanceTo(c) < 32 || e != null && e.position().distanceTo(c) < 32;
            if (!near || g.s == S.CAUGHT || g.s == S.DONE || g.s == S.RETREAT) continue;
            long now = ringer.server.getTickCount();
            g.say(p, "Hết giờ chơi.", ChatFormatting.GRAY);
            vn.cavemind.life.Child.played(ringer.server, 25);
            g.to(p, S.RETREAT, now, "chuông hết giờ chơi");
            any = true;
        }
        return any;
    }

    void say(ServerPlayer p, String text, ChatFormatting color) {
        p.displayClientMessage(Component.literal(text).withStyle(color, ChatFormatting.ITALIC), true);
    }

    // in chat, so the counting on the action bar does not wipe it
    static void tell(ServerPlayer p, String text) {
        p.displayClientMessage(Component.literal(text).withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC), false);
    }

    static boolean peaceful(ServerPlayer p) {
        return p.serverLevel().getDifficulty() == net.minecraft.world.Difficulty.PEACEFUL;
    }

    static void douse(ServerLevel lvl, BlockPos c, int r) {
        for (BlockPos q : BlockPos.betweenClosed(c.offset(-r, -4, -r), c.offset(r, 4, r))) {
            BlockState st = lvl.getBlockState(q);
            if (st.getBlock() instanceof CandleBlock && st.getValue(CandleBlock.LIT)) lvl.setBlock(q, st.setValue(CandleBlock.LIT, false), 3);
            else if (st.getBlock() instanceof CampfireBlock && st.getValue(CampfireBlock.LIT)) lvl.setBlock(q, st.setValue(CampfireBlock.LIT, false), 3);
        }
    }

    static void candle(ServerLevel lvl, BlockPos at) {
        for (BlockPos q : new BlockPos[]{at, at.above(), at.below()}) {
            if (!lvl.getBlockState(q).canBeReplaced() || !lvl.getFluidState(q).isEmpty()) continue;
            if (!lvl.getBlockState(q.below()).isFaceSturdy(lvl, q.below(), Direction.UP)) continue;
            lvl.setBlock(q, Blocks.CANDLE.defaultBlockState().setValue(CandleBlock.LIT, false), 3);
            return;
        }
    }

    static void note(ServerLevel lvl, BlockPos near) {
        for (Direction d : Direction.Plane.HORIZONTAL) {
            BlockPos q = near.relative(d);
            BlockPos g = Spots.nearFloor(lvl, q, 1, 2);
            if (g == null || !lvl.getBlockState(g).canBeReplaced()) continue;
            lvl.setBlock(g, Blocks.SPRUCE_SIGN.defaultBlockState().setValue(StandingSignBlock.ROTATION, RotationSegment.convertToSegment(d.toYRot())), 3);
            if (lvl.getBlockEntity(g) instanceof SignBlockEntity sb) {
                SignText t = sb.getFrontText();
                String[] lines = {"Nó chỉ muốn", "có người", "chơi cùng.", "Đừng bỏ đi."};
                for (int i = 0; i < 4; i++) t = t.setMessage(i, Component.literal(lines[i]));
                sb.setText(t, true);
                sb.setWaxed(true);
                sb.setChanged();
            }
            return;
        }
    }

    void steps(ServerPlayer p, Vec3 from, Entity to) {
        if (to == null) return;
        Vec3 dest = to.position();
        if (Math.min(from.distanceTo(p.position()), dest.distanceTo(p.position())) > 28) return;
        ServerLevel lvl = p.serverLevel();
        int n = 3 + p.getRandom().nextInt(3);
        for (int i = 1; i <= n; i++) {
            Vec3 at = from.lerp(dest, i / (double) (n + 1));
            Fx.later(lvl, i * (4 + p.getRandom().nextInt(3)), () -> Fx.sound(p, Compat.sound("apollyon:step_sound", "minecraft:block.grass.step"),
                SoundSource.HOSTILE, at, 0.32f, 0.8f + p.getRandom().nextFloat() * 0.1f));
        }
    }

    boolean kidStart(ServerPlayer p, Watch w, long now) {
        ServerLevel lvl = p.serverLevel();
        for (UUID id : kids) {
            Entity k = Kids.get(lvl, id);
            if (k == null) continue;
            if (Sight.of(p, w, k.getBoundingBox().getCenter()).on && Sight.visible(p, k) > 0.12) continue;
            Vec3 kp = k.position();
            Vec3 back = kp.add(new Vec3(kp.x - p.getX(), 0, kp.z - p.getZ()).normalize().scale(1.2));
            BlockPos feet = Spots.nearFloor(lvl, BlockPos.containing(back), 1, 2);
            if (feet == null || Sight.of(p, w, Vec3.atCenterOf(feet.above())).on) continue;
            if (put(p, feet, HideSeekYaw.toward(Vec3.atBottomCenterOf(feet), kp), "cm_low", now) == null) continue;
            victim = id;
            kidStage = 1;
            kidAt = now;
            Tension.stress(p, 3, "có tiếng sột soạt chỗ đứa trẻ trốn");
            return true;
        }
        return false;
    }

    boolean kidScene(ServerPlayer p, Watch w, long now) {
        ServerLevel lvl = p.serverLevel();
        Entity e = body(p);
        if (e == null) {
            kidStage = 3;
            return true;
        }
        if (kidStage == 1 && now - kidAt >= 24) {
            Entity k = victim == null ? null : Kids.get(lvl, victim);
            if (k != null) {
                Vec3 at = k.position().add(0, 0.6, 0);
                for (int t : new int[]{0, 5, 11}) Fx.later(lvl, t, () -> Fx.sound(p, Holder.direct(SoundEvents.BONE_BLOCK_BREAK), SoundSource.HOSTILE, at, 0.9f, 0.45f + p.getRandom().nextFloat() * 0.1f));
                Fx.sound(p, "minecraft:entity.fox.scream", SoundSource.HOSTILE, at, 0.55f, 1.75f);
                Fx.later(lvl, 3, () -> Fx.sound(p, "apollyon:small_jumpscare", SoundSource.HOSTILE, at, 0.3f, 1.4f));
                Kids.gone(lvl, k, true);
                kids.remove(victim);
            }
            Motion.play(e, "cm_still", 40, now);
            say(p, "Bắt được một đứa rồi.", ChatFormatting.DARK_GRAY);
            Tension.stress(p, 10, "một đứa trẻ bị bắt");
            kidStage = 2;
            kidAt = now;
        } else if (kidStage == 2 && now - kidAt >= 34) {
            if (e instanceof Mob m) Motion.snap(m, p.getEyePosition());
            Motion.play(e, "cm_owl", 50, now);
            Haunt.breath(p, e.getEyePosition(), 0.3f);
            Fx.sound(p, Compat.sound("apollyon:light_suspense", "minecraft:ambient.cave"), SoundSource.HOSTILE, e.position(), 0.6f, 0.8f);
            lastDist = Math.min(lastDist, Math.max(6, e.distanceTo(p) * 0.65));
            nextPlace = now + 50;
            away = 0;
            kidStage = 3;
        }
        return true;
    }

    boolean inspect(ServerPlayer p, Watch w, long now) {
        inspected = true;
        Spots.Cand c = Spots.find(p, w, new Spots.Spec().ring(1.8, 3.4).dir(180, 120).body(W, H).cave(w.underground).tries(70)
            .when(k -> !k.sight.on && Math.abs(k.feet.getY() - p.getY()) < 3 && !vn.cavemind.sense.Group.boxSeenByOthers(p, k.box, 96))
            .rank(k -> (k.cover ? 1 : 0) + (k.trunk ? 0.5 : 0) + k.vis * 0.2));
        if (c == null) c = Spots.find(p, w, new Spots.Spec().ring(1.5, 5).dir(0, 180).body(W, H).cave(w.underground).tries(80)
            .when(k -> Math.abs(k.feet.getY() - p.getY()) < 3).rank(k -> (k.sight.on ? 0 : 1) + (k.cover ? 0.5 : 0)));
        if (c == null) return false;
        Entity e = put(p, c.feet, HideSeekYaw.toward(c.center, p.getEyePosition()), "cm_lean", now);
        if (e == null) return false;
        Profile pr = MindData.profile(p);
        holdStage = 1;
        holdAt = now;
        holdYaw = p.getYRot();
        holdPitch = p.getXRot();
        holdTurn = 0;
        Net.fx(p, Net.HOLD, pr.dHolds == 0 ? 1f : 0.5f, HOLD_T);
        say(p, pr.dHolds == 0 ? "Nó ở ngay sát bạn. Giữ Shift, đừng bước, đừng nhìn quanh." : "…", ChatFormatting.DARK_RED);
        pr.dHolds++;
        Fx.sound(p, "apollyon:shallow_breathing", SoundSource.HOSTILE, e.getEyePosition(), 0.9f, 0.9f);
        Tension.stress(p, 8, "nó soi sát chỗ trốn");
        PaceLog.encounter(p, "damned", "inspect", "close");
        return true;
    }

    boolean hold(ServerPlayer p, Watch w, Entity e, long now) {
        if (e == null) {
            holdStage = 2;
            Net.fx(p, Net.HOLD, 0f, 0);
            return true;
        }
        long t = now - holdAt;
        holdTurn += Math.abs(Mth.wrapDegrees(p.getYRot() - holdYaw)) + Math.abs(p.getXRot() - holdPitch);
        holdYaw = p.getYRot();
        holdPitch = p.getXRot();
        if (t < 12) holdTurn = 0;
        if (t % 26 == 0) Haunt.breath(p, e.getEyePosition(), 0.45f);
        if (t == 60) {
            if (e instanceof Mob m) Motion.snap(m, p.getEyePosition());
            Motion.play(e, "cm_owl", 40, now);
        }
        Profile pr = MindData.profile(p);
        double limit = pr.dHoldFails == 0 ? 70 : 45;
        if (t >= 12) {
            if (!p.isCrouching()) return holdFail(p, w, e, now, "thở hắt ra");
            if (holdTurn > limit) return holdFail(p, w, e, now, "quay đầu nhìn nó");
            if (w.speed > 0.045) return holdFail(p, w, e, now, "cử động");
        }
        if (t >= HOLD_T) {
            holdStage = 2;
            Net.fx(p, Net.HOLD, 0f, 0);
            win(p, now, "nín thở qua lượt soi");
            holdUntil = now;
            return to(p, S.RETREAT, now, "nó đứng sát bên mà không thấy bạn");
        }
        return true;
    }

    boolean holdFail(ServerPlayer p, Watch w, Entity e, long now, String why) {
        holdStage = 2;
        Net.fx(p, Net.HOLD, -1f, 0);
        Profile pr = MindData.profile(p);
        boolean first = pr.dHoldFails == 0;
        pr.dHoldFails++;
        say(p, "Bạn " + why + ".", ChatFormatting.DARK_RED);
        if (!first && e instanceof Mob m && Tension.allows(p, Tension.Permit.ENGAGE) && !peaceful(p)) return grab(p, w, m, now);
        if (first) Fx.later(p.serverLevel(), 70, () -> p.displayClientMessage(Component.literal("Lần sau: giữ Shift, đứng im, không xoay chuột cho tới khi tiếng thở đi xa.").withStyle(ChatFormatting.GRAY), false));
        return found(p, w, e, now);
    }

    static final class HideSeekYaw {
        static float toward(Vec3 from, Vec3 to) {
            return (float) (Mth.atan2(to.z - from.z, to.x - from.x) * (180f / Math.PI)) - 90f;
        }
    }
}
