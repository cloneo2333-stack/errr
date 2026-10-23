package vn.cavemind.damned;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
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
import vn.cavemind.sense.Sight;
import vn.cavemind.sense.Watch;
import vn.cavemind.world.Fx;
import vn.cavemind.world.Spots;

public final class Haunt {
    public enum Scene { MIRROR, BECKON, VISIT, CORNER, PRESENCE, CANDLE, DOORWAY, STARE }

    static final double W = 0.6, H = 1.8;
    static final String ANIM_IDLE = "cm_profile";
    static final String ANIM_SPOTTED = "animation.theDamned.spotted";
    static final String ANIM_FOUND = "animation.theDamned.found";

    static final class Run {
        Scene scene;
        long start, modeAt;
        int mode;
        UUID body;
        boolean noticed;
        double perceive;
        int away, stare;
        Vec3 planeM, planeN;
        boolean frozen;
        BlockPos target;
        long lastLead;
        int lit;
        boolean wake;
        long nextBreath;
        BlockPos candle;
    }

    static final class Plan {
        long nightKey;
        int at;
        boolean traces;
        int tracesAt;
        boolean visit;
        boolean corner;
    }

    private static final Map<UUID, Run> RUNS = new HashMap<>();
    private static final Map<UUID, Plan> PLANS = new HashMap<>();
    private static final java.util.Set<UUID> NEAR = new java.util.HashSet<>();

    public static void forget(ServerPlayer p) {
        Run r = RUNS.remove(p.getUUID());
        if (r != null) discard(p.serverLevel(), r.body);
        PLANS.remove(p.getUUID());
    }

    public static void clear() {
        RUNS.clear();
        PLANS.clear();
        NEAR.clear();
    }

    public static long nightKey(ServerLevel lvl) {
        long t = lvl.getDayTime();
        long day = t / 24000L, tod = t % 24000L;
        return tod < 1000 ? day - 1 : day;
    }

    public static void tick(ServerPlayer p, Watch w, long now) {
        if (!Compat.damnedActive()) return;
        Run r = RUNS.get(p.getUUID());
        if (r != null) {
            boolean keep;
            try {
                keep = run(p, w, r, now);
            } catch (Throwable t) {
                CaveMind.LOG.warn("[CaveMind] haunting scene failed", t);
                keep = false;
            }
            if (!keep) finish(p, r, now);
            return;
        }
        if (now % 20 != 0) return;
        ServerLevel lvl = p.serverLevel();
        if (lvl.dimension() != Level.OVERWORLD || vn.cavemind.compat.Compat.exempt(p)) return;
        Profile pr = MindData.profile(p);
        long day = Dread.day(lvl);
        if (pr.dPeace) return;
        if ((day < Cfg.DAMNED_START_DAY.get() || pr.playTicks < 24000)) return;
        if (pr.dQuietUntilDay >= 0 && day < pr.dQuietUntilDay) return;
        int tod = Dread.time(lvl);
        long key = nightKey(lvl);
        Plan pl = PLANS.get(p.getUUID());
        boolean night = tod >= 13000 || tod < 1000;
        if (night && (pl == null || pl.nightKey != key) && pr.dLastNight != key) {
            pl = plan(p, pr, key);
            PLANS.put(p.getUUID(), pl);
            pr.dLastNight = key;
        }
        if (pr.dStage >= 2 && pr.dShrine == null && Cfg.DAMNED_SHRINE.get() && !night && now % 200 == 0) {
            BlockPos s = Shrine.build(p, pr);
            if (s != null) pr.dShrine = s;
        }
        if (pr.dShrine != null && now % 40 == 0) {
            boolean near = pr.dShrine.distSqr(p.blockPosition()) < 14 * 14;
            if (near) {
                if (NEAR.add(p.getUUID())) Shrine.approach(p, pr);
                Shrine.linger(p, pr);
            } else {
                NEAR.remove(p.getUUID());
            }
        }
        Effigy.tick(p, pr, tod);
        if (pl == null || pl.nightKey != key || !night) return;
        if (pl.traces && tod >= pl.tracesAt && Cfg.DAMNED_TRACES.get()) {
            if (Traces.run(p, w, pr)) pl.traces = false;
        }
        if (pl.at >= 0 && tod >= pl.at && tod < 23000) {
            if (!Director.free(p)) return;
            for (ServerPlayer o : vn.cavemind.sense.Group.others(p, 96)) {
                if (RUNS.containsKey(o.getUUID())) {
                    pl.at = tod + 600;
                    return;
                }
            }
            if (vn.cavemind.sense.Group.grouped(p, 24) && !vn.cavemind.life.Glimpse.damnedFor(p)) {
                pl.at = tod + 500 + p.getRandom().nextInt(500);
                return;
            }
            if (pr.dStage == 0) {
                Vec3 far = p.position().add(Vec3.directionFromRotation(0, p.getRandom().nextFloat() * 360).scale(70));
                Fx.sound(p, "apollyon:quieter_spawn", SoundSource.AMBIENT, far, 0.6f, 0.8f);
                Compat.grant(p, "apollyon:apollyon");
                advance(p, pr, true);
                pl.at = -1;
                CaveMind.debug("something calls from far away for {}", p.getGameProfile().getName());
                return;
            }
            Scene sc = choose(p, w, pr);
            if (sc != null && begin(p, w, pr, sc, now)) pl.at = -1;
            else pl.at = tod + 400 + p.getRandom().nextInt(800);
        }
    }

    private static Plan plan(ServerPlayer p, Profile pr, long key) {
        RandomSource r = p.getRandom();
        Plan pl = new Plan();
        pl.nightKey = key;
        boolean scene = r.nextDouble() < Math.min(1.0, Cfg.DAMNED_NIGHT_CHANCE.get() * Cfg.INTENSITY.get());
        pl.at = scene ? (13400 + r.nextInt(6500)) : -1;
        pl.traces = pr.dStage >= 0 && r.nextFloat() < 0.75f;
        pl.tracesAt = 14000 + r.nextInt(8000);
        pl.visit = Cfg.DAMNED_SLEEP.get() && (pr.dStage >= 3 && r.nextFloat() < 0.6f || pr.dStage == 2 && r.nextFloat() < 0.2f);
        pl.corner = pr.dStage >= 3 && r.nextFloat() < 0.5f;
        CaveMind.debug("haunting plan for {}: scene@{} traces={} visit={}", p.getGameProfile().getName(), pl.at, pl.traces, pl.visit);
        return pl;
    }

    private static Scene choose(ServerPlayer p, Watch w, Profile pr) {
        int st = pr.dStage;
        RandomSource r = p.getRandom();
        if (w.outside) {
            if (st >= 2 && pr.dShrine != null && !pr.dShrineFound && r.nextFloat() < 0.75f) return Scene.BECKON;
            if (r.nextFloat() < 0.25f) return Scene.STARE;
            return Scene.MIRROR;
        }
        if (w.indoors) {
            if (st >= 3 && r.nextFloat() < 0.45f) return Scene.DOORWAY;
            return r.nextBoolean() ? Scene.CANDLE : Scene.PRESENCE;
        }
        if (w.underground) return Scene.PRESENCE;
        return null;
    }

    public static String describe(ServerPlayer p) {
        if (!Compat.damnedActive()) return "tắt";
        Profile pr = MindData.profile(p);
        Run r = RUNS.get(p.getUUID());
        Plan pl = PLANS.get(p.getUUID());
        String now = r != null ? "đang " + vi(r.scene) : "không có cảnh";
        String plan = pl == null ? "chưa lên kế hoạch đêm nay" : pl.at >= 0 ? "cảnh đêm nay lúc " + pl.at : "đêm nay không còn cảnh";
        return now + ", giai đoạn " + pr.dStage + ", " + plan + (pr.dShrine != null ? ", có đền" : "");
    }

    public static boolean busy(ServerPlayer p) {
        return RUNS.containsKey(p.getUUID());
    }

    public static boolean summon(ServerPlayer p) {
        if (!Compat.damnedActive() || RUNS.containsKey(p.getUUID()) || p.level().dimension() != Level.OVERWORLD) return false;
        Watch w = Watch.of(p);
        Profile pr = MindData.profile(p);
        long now = p.server.getTickCount();
        if (pr.dStage == 0) {
            Vec3 far = p.position().add(Vec3.directionFromRotation(0, p.getRandom().nextFloat() * 360).scale(Math.min(60, vn.cavemind.sense.Range.of(p, w) * 1.4)));
            Fx.sound(p, "apollyon:quieter_spawn", SoundSource.AMBIENT, far, 0.6f, 0.8f);
            Compat.grant(p, "apollyon:apollyon");
            advance(p, pr, true);
            CaveMind.debug("something calls from far away for {} (stage)", p.getGameProfile().getName());
            return true;
        }
        Scene first = choose(p, w, pr);
        if (first != null && begin(p, w, pr, first, now)) return true;
        Scene[] alts = w.indoors ? new Scene[]{Scene.DOORWAY, Scene.CANDLE, Scene.PRESENCE} : w.underground ? new Scene[]{Scene.PRESENCE} : new Scene[]{Scene.MIRROR, Scene.STARE};
        for (Scene sc : alts) {
            if (sc == first || sc == Scene.DOORWAY && pr.dStage < 3) continue;
            if (begin(p, w, pr, sc, now)) return true;
        }
        return false;
    }

    public static boolean force(ServerPlayer p, Scene sc) {
        Run old = RUNS.remove(p.getUUID());
        if (old != null) finish(p, old, p.server.getTickCount());
        Director.state(p).act = null;
        Profile pr = MindData.profile(p);
        if (sc == Scene.BECKON && pr.dShrine == null) {
            BlockPos s = Shrine.build(p, pr);
            if (s == null) return false;
            pr.dShrine = s;
            pr.dShrineFound = false;
        }
        if (sc == Scene.VISIT || sc == Scene.CORNER) {
            Plan pl = PLANS.computeIfAbsent(p.getUUID(), k -> new Plan());
            pl.nightKey = nightKey(p.serverLevel());
            if (sc == Scene.VISIT) pl.visit = true;
            else pl.corner = true;
            return true;
        }
        return begin(p, Watch.of(p), pr, sc, p.server.getTickCount());
    }

    private static boolean begin(ServerPlayer p, Watch w, Profile pr, Scene sc, long now) {
        if (!Director.claim(p, "damned")) return false;
        Run r = new Run();
        r.scene = sc;
        r.start = r.modeAt = now;
        boolean ok = switch (sc) {
            case MIRROR -> Mirror.setup(p, w, r);
            case BECKON -> Beckon.setup(p, w, pr, r);
            case STARE -> Fx.stare(p, p, null, 170, 30) >= 2;
            case CANDLE -> Indoors.candle(p, w, r);
            case PRESENCE -> true;
            case DOORWAY -> Indoors.doorway(p, w, r);
            default -> false;
        };
        if (!ok) {
            discard(p.serverLevel(), r.body);
            Director.abort(p, "damned");
            CaveMind.debug("haunting {} could not start for {} (no place)", sc, p.getGameProfile().getName());
            return false;
        }
        RUNS.put(p.getUUID(), r);
        CaveMind.debug("haunting {} begins for {}", sc, p.getGameProfile().getName());
        Entity db = body(p, r);
        return true;
    }

    private static boolean run(ServerPlayer p, Watch w, Run r, long now) {
        if (!p.isAlive() || p.level().dimension() != Level.OVERWORLD) return false;
        Profile pr = MindData.profile(p);
        return switch (r.scene) {
            case MIRROR -> Mirror.run(p, w, pr, r, now);
            case BECKON -> Beckon.run(p, w, pr, r, now);
            case VISIT -> Visit.run(p, w, pr, r, now);
            case CORNER -> Visit.corner(p, w, pr, r, now);
            case CANDLE -> Indoors.runCandle(p, w, pr, r, now);
            case PRESENCE -> Indoors.runPresence(p, w, pr, r, now);
            case DOORWAY -> Indoors.runDoorway(p, w, pr, r, now);
            case STARE -> {
                if (now - r.start == 60) {
                    Fx.sound(p, "apollyon:shallow_breathing", SoundSource.HOSTILE,
                        p.getEyePosition().add(Vec3.directionFromRotation(0, p.getYRot()).scale(-1.4)), 0.3f, 0.9f);
                }
                if (now - r.start > 170) {
                    advance(p, pr, true);
                    yield false;
                }
                yield true;
            }
        };
    }

    private static void finish(ServerPlayer p, Run r, long now) {
        discard(p.serverLevel(), r.body);
        RUNS.remove(p.getUUID());
        boolean climax = r.scene == Scene.BECKON || r.scene == Scene.VISIT || r.scene == Scene.CORNER || r.scene == Scene.DOORWAY;
        Director.release(p, "damned", climax && r.noticed);
    }

    static void advance(ServerPlayer p, Profile pr, boolean noticed) {
        if (!noticed) return;
        pr.dNights++;
        int st = pr.dNights < 2 ? 0 : pr.dNights < 4 ? 1 : pr.dNights < 6 ? 2 : pr.dNights < 8 ? 3 : 4;
        if (st > pr.dStage) {
            pr.dStage = st;
            CaveMind.debug("haunting of {} reaches stage {}", p.getGameProfile().getName(), st);
        }
    }

    private static final Map<UUID, Long> GLIMPSED = new HashMap<>();

    public static void glimpsed(ServerPlayer p) {
        long key = nightKey(p.serverLevel());
        Long last = GLIMPSED.put(p.getUUID(), key);
        if (last != null && last == key) return;
        advance(p, MindData.profile(p), true);
    }

    public static void overnight(ServerPlayer p) {
        Profile pr = MindData.profile(p);
        if (!Cfg.DAMNED_TRACES.get() || pr.dStage < 1 && Dread.day(p.serverLevel()) < Cfg.DAMNED_START_DAY.get()) return;
        Traces.run(p, Watch.of(p), pr);
    }

    public static Entity spawn(ServerPlayer p, BlockPos feet, Vec3 face) {
        ServerLevel lvl = p.serverLevel();
        EntityType<?> type = Compat.type(Compat.DAMNED);
        if (type == null) return null;
        Entity e = type.create(lvl);
        if (e == null) return null;
        float yaw = yawTo(Vec3.atBottomCenterOf(feet), face);
        e.moveTo(feet.getX() + 0.5, feet.getY(), feet.getZ() + 0.5, yaw, 0);
        Compat.mark(e, "damned", p.getUUID(), true);
        if (e instanceof Mob m) {
            m.goalSelector.removeAllGoals(g -> true);
            m.targetSelector.removeAllGoals(g -> true);
            m.setPersistenceRequired();
            m.setYHeadRot(yaw);
            m.setYBodyRot(yaw);
            m.setSilent(true);
        }
        if (!lvl.addFreshEntity(e)) {
            Compat.ONLY_MAP.remove(e.getUUID());
            CaveMind.debug("the Damned was refused entry at {} (another mod cancelled the spawn)", feet);
            return null;
        }
        Compat.anim(e, ANIM_IDLE);
        return e;
    }

    static Entity respawn(ServerPlayer p, Run r, BlockPos feet, Vec3 face) {
        discard(p.serverLevel(), r.body);
        r.body = null;
        Entity e = spawn(p, feet, face);
        if (e != null) r.body = e.getUUID();
        return e;
    }

    static void still(Mob m, Vec3 at, boolean seen) {
        if (!seen) vn.cavemind.world.Motion.snap(m, at);
    }

    static String vi(Scene sc) {
        return switch (sc) {
            case MIRROR -> "gương soi (bắt chước mọi cử động của bạn)";
            case BECKON -> "dẫn lối tới đền thờ";
            case VISIT -> "viếng lúc ngủ";
            case CORNER -> "đứng trong góc";
            case PRESENCE -> "hơi thở sau lưng (đứng yên trong tối)";
            case CANDLE -> "ngọn nến tắt trước mắt";
            case DOORWAY -> "đứng ở ô cửa tối";
            case STARE -> "mọi sinh vật quay sang nhìn bạn";
        };
    }

    static Entity body(ServerPlayer p, Run r) {
        if (r.body == null) return null;
        Entity e = p.serverLevel().getEntity(r.body);
        return e != null && e.isAlive() ? e : null;
    }

    static void discard(ServerLevel lvl, UUID id) {
        if (id == null) return;
        Entity e = lvl.getEntity(id);
        if (e != null) e.discard();
    }

    static float yawTo(Vec3 from, Vec3 to) {
        return (float) (Mth.atan2(to.z - from.z, to.x - from.x) * (180f / Math.PI)) - 90f;
    }

    static void face(Mob m, Vec3 at) {
        vn.cavemind.world.Motion.turn(m, at, 5.0f);
    }

    static boolean perceive(ServerPlayer p, Watch w, Run r, Entity e) {
        Sight si = Sight.of(p, w, e.getBoundingBox().getCenter().add(0, 0.4, 0));
        double vis = Sight.visible(p, e);
        boolean on = si.on && vis > 0.15;
        r.away = on ? 0 : r.away + 1;
        if (!r.noticed) {
            if (on) r.perceive += (si.central() ? 1.0 : 0.35) * vis * 1.5;
            else r.perceive = Math.max(0, r.perceive - 0.2);
            if (r.perceive >= 6) {
                r.noticed = true;
                Director.scare(p, 6);
                vn.cavemind.director.Tension.seen(p, e.distanceTo(p), "The Damned");
                vn.cavemind.director.PaceLog.encounter(p, "damned", "seen " + r.scene, (int) e.distanceTo(p) + "m");
                Compat.grant(p, "apollyon:apollyon");
            }
        } else if (on && si.edge() < 0.6) {
            r.stare++;
        }
        return on;
    }

    public static void sleeping(ServerPlayer p, Watch w, long now) {
        if (!Compat.damnedActive() || !Cfg.DAMNED_SLEEP.get()) return;
        if (RUNS.containsKey(p.getUUID())) return;
        Plan pl = PLANS.get(p.getUUID());
        ServerLevel lvl = p.serverLevel();
        if (pl == null || pl.nightKey != nightKey(lvl)) return;
        if (pl.visit && p.getSleepTimer() == 22) {
            pl.visit = false;
            Run r = new Run();
            r.scene = Scene.VISIT;
            r.start = r.modeAt = now;
            if (Director.claim(p, "damned") && Visit.setup(p, w, r)) {
                RUNS.put(p.getUUID(), r);
                MindData.profile(p).dVisits++;
                CaveMind.debug("it visits {} in their sleep", p.getGameProfile().getName());
            } else {
                Director.abort(p, "damned");
            }
        }
    }

    public static void woke(ServerPlayer p) {
        if (!Compat.damnedActive()) return;
        Plan pl = PLANS.get(p.getUUID());
        if (pl == null || !pl.corner || RUNS.containsKey(p.getUUID())) return;
        ServerLevel lvl = p.serverLevel();
        int tod = Dread.time(lvl);
        if (tod > 2000 && tod < 12000 && pl.nightKey != nightKey(lvl) && pl.nightKey != Dread.day(lvl) - 1) return;
        pl.corner = false;
        Run r = new Run();
        r.scene = Scene.CORNER;
        r.start = r.modeAt = p.server.getTickCount();
        if (Director.claim(p, "damned") && Visit.setupCorner(p, Watch.of(p), r)) {
            RUNS.put(p.getUUID(), r);
            CaveMind.debug("it waits in the corner for {}", p.getGameProfile().getName());
        } else {
            Director.abort(p, "damned");
        }
    }

    public static void slept(ServerPlayer p) {
        Profile pr = MindData.profile(p);
        ServerLevel lvl = p.serverLevel();
        int facing = 0;
        var bed = p.getSleepingPos();
        if (bed.isPresent()) {
            BlockState st = lvl.getBlockState(bed.get());
            if (st.getBlock() instanceof BedBlock) facing = st.getValue(BedBlock.FACING).get2DDataValue();
            vn.cavemind.life.Homes.slept(p, bed.get());
        }
        pr.moment(new Moment(Moment.SLEPT, Dread.day(lvl), Dread.time(lvl), p.blockPosition(), facing, 0));
    }

    public static void hurt(ServerPlayer p, Entity e) {
        Profile pr = MindData.profile(p);
        ServerLevel lvl = p.serverLevel();
        pr.dHurt++;
        pr.dHurtDay = Dread.day(lvl);
        pr.moment(new Moment(Moment.HURT_IT, Dread.day(lvl), Dread.time(lvl), p.blockPosition(), 0, 0));
        Fx.sound(p, "apollyon:damage_noise", SoundSource.HOSTILE, e.position(), 0.8f, 0.6f);
        Fx.later(lvl, 12, () -> Fx.sound(p, "apollyon:deep_laugh", SoundSource.HOSTILE, e.position(), 0.5f, 0.7f));
        e.discard();
        Run r = RUNS.get(p.getUUID());
        if (r != null && e.getUUID().equals(r.body)) r.body = null;
    }

    public static void banish(ServerPlayer p, String how) {
        Profile pr = MindData.profile(p);
        ServerLevel lvl = p.serverLevel();
        Game.banished(p);
        Run r = RUNS.remove(p.getUUID());
        if (r != null) {
            Entity e = body(p, r);
            if (e != null) Fx.soundAll(lvl, "apollyon:used_rose", SoundSource.AMBIENT, e.position(), 1f, 1f);
            discard(lvl, r.body);
            Director.release(p, "damned", true);
        }
        if (pr.dShrine != null) Shrine.ruin(p, pr);
        Effigy.remove(p, pr);
        pr.dBanished++;
        pr.dStage = 0;
        pr.dNights = 0;
        pr.dShrine = null;
        pr.dShrineFound = false;
        pr.dQuietUntilDay = Dread.day(lvl) + 7 + p.getRandom().nextInt(6);
        PLANS.remove(p.getUUID());
        p.displayClientMessage(Component.translatable("cavemind.damned.banished"), true);
        CaveMind.debug("{} banished the Damned ({})", p.getGameProfile().getName(), how);
    }

    static BlockPos ground(ServerLevel lvl, double x, double z, double yHint) {
        int ix = Mth.floor(x), iz = Mth.floor(z);
        int y = lvl.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, ix, iz);
        if (Math.abs(y - yHint) <= 4) return new BlockPos(ix, y, iz);
        BlockPos c = Spots.caveFloor(lvl, ix, Mth.floor(yHint), iz, 2);
        return c;
    }

    static Direction compass(int data) {
        return Direction.from2DDataValue(data);
    }

    public static void breath(ServerPlayer p, Vec3 at, float vol) {
        Fx.sound(p, Compat.sound("apollyon:shallow_breathing", "minecraft:entity.warden.nearby_close"), SoundSource.HOSTILE, at, vol, 0.95f);
    }

    static void heartbeat(ServerPlayer p, float vol) {
        Fx.sound(p, Holder.direct(SoundEvents.WARDEN_HEARTBEAT), SoundSource.PLAYERS, p.getEyePosition(), vol, 0.9f);
    }

    static void fog(ServerPlayer p, float a, int t) {
        if (Cfg.FOG.get()) Net.fx(p, Net.FOG, a, t);
    }

    private Haunt() {}
}
