package vn.cavemind.life;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Container;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CandleBlock;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.TorchBlock;
import net.minecraft.world.level.block.WallTorchBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import vn.cavemind.CaveMind;
import vn.cavemind.Cfg;
import vn.cavemind.compat.Compat;
import vn.cavemind.director.Director;
import vn.cavemind.director.Dread;
import vn.cavemind.director.Tension;
import vn.cavemind.man.ManMind;
import vn.cavemind.man.Scent;
import vn.cavemind.memory.MindData;
import vn.cavemind.memory.Profile;
import vn.cavemind.sense.Ears;
import vn.cavemind.sense.Group;
import vn.cavemind.sense.Range;
import vn.cavemind.sense.Sight;
import vn.cavemind.sense.Watch;
import vn.cavemind.world.Fx;
import vn.cavemind.world.Spots;

public final class Life {
    private static long lastDayTime = -1;
    private static final Map<UUID, Long> NEXT_CUE = new HashMap<>();
    private static final Map<UUID, Long> AWAY_SINCE = new HashMap<>();
    private static long nextAct;
    private static long decideAt;
    private static long enterAfter;
    private static double orbit;
    private static Vec3 target;
    private static String why = "";

    public static boolean on() {
        return Cfg.LIFE.get() && Compat.manActive();
    }

    public static void clear() {
        lastDayTime = -1;
        NEXT_CUE.clear();
        AWAY_SINCE.clear();
        LAIR_DAY.clear();
        nextAct = 0;
        decideAt = 0;
        enterAfter = 0;
        target = null;
        LifeData.reset();
    }

    public static void tick(MinecraftServer server) {
        if (!on()) return;
        ServerLevel lvl = server.overworld();
        long now = server.getTickCount();
        long dt = lvl.getDayTime();
        if (lastDayTime >= 0 && dt - lastDayTime > 2000) skipped(server, lvl, lastDayTime, dt);
        lastDayTime = dt;
        if (now % 20 != 0) return;
        List<ServerPlayer> ps = players(lvl);
        if (ps.isEmpty()) return;
        LifeData d = LifeData.get(server);
        if (now % 600 == 0 || d.bases.isEmpty() && now % 100 == 0) Bases.refresh(d, ps);
        long day = Dread.day(lvl);
        if (d.lastDay != day) {
            if (d.lastDay >= 0) dawn(d, day);
            d.lastDay = day;
        }
        double freq = Cfg.INTENSITY.get() * Cfg.MAN_FREQ.get();
        d.hunger = Math.min(100, d.hunger + 0.025 * freq);
        d.caution = Math.max(0, d.caution - 0.02);
        d.curiosity = Math.max(0, d.curiosity - 0.004);
        int tod = Dread.time(lvl);
        if (bloodDay != day && tod >= 13000 && tod < 22000 && vn.cavemind.world.Moon.is(lvl, vn.cavemind.world.Moon.BLOOD)) {
            bloodDay = day;
            d.hunger = 100;
            d.caution *= 0.5;
            d.restUntil = 0;
            for (ServerPlayer p : ps) p.displayClientMessage(net.minecraft.network.chat.Component.literal("Dưới Trăng Máu, có thứ gì đó đói khát bước ra khỏi sương.").withStyle(net.minecraft.ChatFormatting.DARK_RED, net.minecraft.ChatFormatting.ITALIC), false);
            CaveMind.debug("blood moon: the Man is starving");
        }
        BlockPos station = vn.cavemind.lore.Lore.site(lvl, "s1");
        if (station != null && !station.equals(d.lair)) {
            d.lair = station;
            CaveMind.debug("the Man moves into the fog surveyor's station at {}", station);
        }
        if (d.lair == null) d.lair = pickLair(lvl, d, ps);
        if (now % 100 == 0) lairVisits(lvl, d, ps);
        if (!d.placed) d.at(d.lair.getX() + 0.5, d.lair.getY(), d.lair.getZ() + 0.5);
        Vec3 body = ManMind.where(server);
        if (body != null || ManMind.anyActive() || Glimpse.manShowing()) {
            if (body != null) d.at(body.x, body.y, body.z);
            if (!d.goal.equals("hunt")) {
                d.goal = "hunt";
                d.goalSince = now;
            }
            decideAt = Math.min(decideAt, now + 200);
        } else {
            if (now >= decideAt || d.goal.equals("hunt")) {
                decide(lvl, d, ps, now);
                decideAt = now + 300;
            }
            move(lvl, d, ps, now);
            if (!vn.cavemind.path.Leaving.any()) enter(lvl, d, ps, now);
            cues(lvl, d, ps, now);
        }
        if (d.goal.equals("study")) study(lvl, d, ps, now);
        armSits(lvl, d, ps, now);
        d.setDirty();
    }

    static List<ServerPlayer> players(ServerLevel lvl) {
        List<ServerPlayer> out = new ArrayList<>();
        for (ServerPlayer p : lvl.players()) if (!Compat.exempt(p) && p.isAlive()) out.add(p);
        return out;
    }

    public static Vec3 where(MinecraftServer server) {
        if (!on()) return null;
        LifeData d = LifeData.get(server);
        return d.placed ? new Vec3(d.x, d.y, d.z) : null;
    }

    public static String describe(MinecraftServer server, ServerPlayer p) {
        if (!on()) return "tắt";
        LifeData d = LifeData.get(server);
        Vec3 at = where(server);
        String dist = at == null ? "?" : (int) Math.sqrt(Math.pow(at.x - p.getX(), 2) + Math.pow(at.z - p.getZ(), 2)) + "m";
        long gt = server.overworld().getGameTime();
        String rest = gt < d.restUntil ? ", nghỉ thêm " + (d.restUntil - gt) / 20 + "s" : "";
        return vi(d.goal) + " (" + why + "), cách bạn " + dist + ", đói " + (int) d.hunger + ", thận trọng " + (int) d.caution
            + ", tò mò " + (int) d.curiosity + rest + ", mùi " + Scent.size() + " ô";
    }

    static String vi(String g) {
        return switch (g) {
            case "rest" -> "nghỉ ở trạm";
            case "withdraw" -> "rút về";
            case "track" -> "lần theo mùi";
            case "listen" -> "đi nghe ngóng";
            case "prowl" -> "lang thang";
            case "study" -> "nghiên cứu căn cứ";
            case "hunt" -> "đang ở gần một người";
            default -> g;
        };
    }

    public static double left(ServerPlayer p, boolean seen, boolean peak) {
        if (!on()) return 0;
        LifeData d = LifeData.get(p.server);
        RandomSource r = p.getRandom();
        double a = Cfg.MAN_REST_MIN.get(), b = Math.max(a, Cfg.MAN_REST_MAX.get());
        double mins = (a + r.nextDouble() * (b - a)) / Math.max(0.1, Cfg.INTENSITY.get() * Cfg.MAN_FREQ.get());
        if (peak) mins *= 1.4;
        if (!seen) mins *= 0.6;
        d.restUntil = p.server.overworld().getGameTime() + (long) (mins * 1200);
        if (seen) d.hunger = Math.max(0, d.hunger - 40);
        d.goal = "withdraw";
        why = "vừa rời " + p.getGameProfile().getName();
        decideAt = p.server.getTickCount() + 200;
        enterAfter = p.server.getTickCount() + 600;
        d.setDirty();
        return mins;
    }

    public static void dissolved(MinecraftServer server, Vec3 at) {
        if (!on() || at == null) return;
        LifeData d = LifeData.get(server);
        d.at(at.x, at.y, at.z);
        decideAt = Math.min(decideAt, server.getTickCount() + 100);
    }

    public static void seen(MinecraftServer server, Vec3 at, double dist) {
        if (!on()) return;
        LifeData d = LifeData.get(server);
        d.hunger = Math.max(0, d.hunger - 30);
        d.caution = Math.min(100, d.caution + (dist < 16 ? 8 : 2));
        if (at != null) d.at(at.x, at.y, at.z);
    }

    public static void hurt(MinecraftServer server, double amount) {
        if (!on()) return;
        LifeData d = LifeData.get(server);
        d.caution = Math.min(100, d.caution + amount);
        decideAt = 0;
    }

    public static void bell(ServerPlayer ringer, BlockPos at) {
        if (!on()) return;
        LifeData d = LifeData.get(ringer.server);
        Vec3 man = where(ringer.server);
        if (man == null || man.distanceToSqr(Vec3.atCenterOf(at)) > 160 * 160) return;
        ServerLevel lvl = ringer.serverLevel();
        long day = Dread.day(lvl);
        if (d.bellDay >= 0) d.bellHeat = Math.max(0, d.bellHeat - (day - d.bellDay));
        d.bellDay = day;
        double chance = Mth.clamp(1.0 - d.bellHeat * 0.25, 0.15, 1.0) * (MindData.profile(ringer).lore.contains("s1") ? 1.0 : 0.55);
        if (ringer.getRandom().nextDouble() < chance) {
            d.bellHeat += 1;
            d.caution = Math.min(100, d.caution + 15);
            d.restUntil = Math.max(d.restUntil, lvl.getGameTime() + 2400 + ringer.getRandom().nextInt(2400));
            d.goal = "withdraw";
            why = "nghe chuông sương";
            decideAt = ringer.server.getTickCount() + 400;
            CaveMind.debug("the Man hears the fog bell and turns back");
        } else {
            d.curiosity = Math.min(100, d.curiosity + 10);
            why = "chuông không còn làm hắn sợ";
        }
        d.setDirty();
    }

    public static void built(ServerPlayer p, BlockPos pos) {
        if (!on() || p.level().dimension() != Level.OVERWORLD) return;
        LifeData d = LifeData.get(p.server);
        LifeData.Base b = Bases.near(d, pos, 8);
        if (b == null) return;
        b.builds++;
        d.curiosity = Math.min(100, d.curiosity + 0.15);
    }

    public static void worked(ServerPlayer p) {
        if (p.level().dimension() != Level.OVERWORLD) return;
        LifeData d = LifeData.get(p.server);
        d.memory = Math.min(100, d.memory + 0.12);
    }

    private static BlockPos pickLair(ServerLevel lvl, LifeData d, List<ServerPlayer> ps) {
        for (ServerPlayer p : ps) {
            Profile pr = MindData.profile(p);
            if (pr.nest != null) return pr.nest;
        }
        BlockPos from = !d.bases.isEmpty() ? d.bases.get(0).center : ps.get(0).blockPosition();
        RandomSource r = lvl.getRandom();
        double a = r.nextDouble() * Math.PI * 2, dist = 150 + r.nextInt(70);
        int x = from.getX() + (int) (Math.cos(a) * dist), z = from.getZ() + (int) (Math.sin(a) * dist);
        int y = lvl.hasChunkAt(new BlockPos(x, 64, z)) ? lvl.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z) : from.getY();
        return new BlockPos(x, y, z);
    }

    private static void dawn(LifeData d, long day) {
        d.nightsAlive++;
        d.curiosity = Math.min(100, d.curiosity + 6);
        if (!d.goal.equals("hunt")) d.goal = "rest";
        decideAt = 0;
    }

    private static boolean clearDay(ServerLevel lvl) {
        int tod = Dread.time(lvl);
        return tod < 11200 && tod > 300 && !lvl.isRaining() && !lvl.isThundering();
    }

    static long bloodDay = -1;

    private static void decide(ServerLevel lvl, LifeData d, List<ServerPlayer> ps, long now) {
        int tod = Dread.time(lvl);
        boolean night = tod >= 13000 && tod < 23200 || lvl.isThundering();
        boolean dusk = tod >= 11200 && tod < 13000;
        boolean clear = clearDay(lvl);
        boolean dayWant = Cfg.MAN_DAYTIME.get() && d.hunger > 65;
        Vec3 at = new Vec3(d.x, d.y, d.z);
        LifeData.Base b = focusBase(d, ps);
        RandomSource r = lvl.getRandom();
        String g;
        Vec3 t = null;
        String w;
        Ears.Noise n = Ears.loudest(lvl, at, now, 600, 1.6);
        double sc = Scent.strength(at, 6);
        boolean deepScent = Scent.deepAt(at, 6);
        ServerPlayer urge = null;
        for (ServerPlayer p : ps) {
            if (!vn.cavemind.man.Meter.wantsVisit(p) || !vn.cavemind.man.Meter.dark(p, Watch.of(p))) continue;
            if (lvl.getGameTime() < d.restUntil && !vn.cavemind.man.Meter.huntDue(p)) continue;
            urge = p;
        }
        if (urge != null) {
            g = "prowl";
            t = urge.position();
            w = "bị thôi thúc tới chỗ " + urge.getGameProfile().getName();
            d.restUntil = 0;
            if (at.distanceTo(urge.position()) > 110) {
                double a = r.nextDouble() * Math.PI * 2;
                d.at(urge.getX() + Math.cos(a) * 80, urge.getY(), urge.getZ() + Math.sin(a) * 80);
            }
        } else if (night && b != null && d.caution < 75 && vn.cavemind.world.Moon.is(lvl, vn.cavemind.world.Moon.BLOOD) && r.nextDouble() < 0.7) {
            g = "study";
            w = "Trăng Máu: đói, lao về phía căn cứ";
        } else if (lvl.getGameTime() < d.restUntil) {
            g = "withdraw";
            w = why.isEmpty() ? "nghỉ sau lần gặp" : why;
        } else if (d.caution > 75) {
            g = "rest";
            w = "quá thận trọng";
        } else if (sc > 0.6 && (!clear || deepScent || dayWant)) {
            g = "track";
            w = "ngửi thấy mùi " + String.format(java.util.Locale.ROOT, "%.1f", sc);
        } else if (n != null && (!clear || n.pos().y < lvl.getSeaLevel() - 6 || dayWant)) {
            g = "listen";
            t = n.pos();
            w = "nghe " + noiseVi(n.kind()) + " cách " + (int) n.pos().distanceTo(at) + "m";
        } else if (night && b != null && r.nextDouble() < 0.4 + d.curiosity / 160.0) {
            g = "study";
            w = "tò mò về căn cứ";
        } else {
            Tension.Hint h = null;
            for (ServerPlayer p : ps) {
                Tension.Hint ph = Tension.hint(p);
                if (ph != null && (h == null || ph.radius() < h.radius())) h = ph;
            }
            BlockPos far = Scent.strongest(at, 480, 0.4, clear && !dayWant);
            if (h != null && (!clear || dayWant || far != null && far.getY() < lvl.getSeaLevel() - 6)) {
                g = "prowl";
                t = h.center();
                w = "đi về vùng có hơi người (r=" + (int) h.radius() + ")";
            } else if (far != null && (night || dusk || lvl.isRaining() || dayWant || far.getY() < lvl.getSeaLevel() - 6) && d.hunger > 20) {
                g = "prowl";
                t = Vec3.atBottomCenterOf(far);
                w = "đánh hơi thấy mùi xa";
            } else if ((night || dusk) && b != null) {
                g = "study";
                w = "về phía căn cứ";
            } else {
                g = "rest";
                w = clear ? "trời sáng, hắn ở trong trạm" : "không có gì";
            }
        }
        if (!g.equals(d.goal)) {
            d.goal = g;
            d.goalSince = now;
            CaveMind.debug("the Man's life: {} ({}; hunger {}, caution {})", g, w, (int) d.hunger, (int) d.caution);
        }
        target = t;
        why = w;
    }

    static String noiseVi(Ears.Kind k) {
        return switch (k) {
            case MINE -> "tiếng đào";
            case BUILD -> "tiếng đặt khối";
            case DOOR -> "tiếng cửa";
            case FIGHT -> "tiếng đánh nhau";
            case BOOM -> "tiếng nổ";
            case SPRINT -> "tiếng chạy";
            case JUMP, LAND -> "tiếng nhảy";
            case BELL -> "tiếng chuông";
            case VOICE -> "giọng nói";
            default -> "tiếng bước";
        };
    }

    static LifeData.Base focusBase(LifeData d, List<ServerPlayer> ps) {
        LifeData.Base best = null;
        double bs = -1;
        for (LifeData.Base b : d.bases) {
            int on = 0;
            for (ServerPlayer p : ps) if (b.owners.contains(p.getUUID())) on++;
            if (on == 0) continue;
            double s = on + b.builds * 0.002 + (6 - b.study) * 0.1;
            if (s > bs) {
                bs = s;
                best = b;
            }
        }
        return best;
    }

    private static ServerPlayer player(MinecraftServer s, UUID id) {
        return id == null ? null : s.getPlayerList().getPlayer(id);
    }

    private static void move(ServerLevel lvl, LifeData d, List<ServerPlayer> ps, long now) {
        Vec3 goal;
        double speed;
        LifeData.Base b = focusBase(d, ps);
        Vec3 at = new Vec3(d.x, d.y, d.z);
        switch (d.goal) {
            case "study" -> {
                if (b == null) {
                    decideAt = 0;
                    return;
                }
                orbit += 0.018 + lvl.getRandom().nextDouble() * 0.012;
                double r = b.radius + 9 + 4 * Math.sin(orbit * 3.1);
                goal = Vec3.atBottomCenterOf(b.center).add(Math.cos(orbit) * r, 0, Math.sin(orbit) * r);
                speed = 1.6;
            }
            case "track" -> {
                BlockPos up = Scent.uphill(at, 5);
                if (up == null) {
                    decideAt = 0;
                    return;
                }
                goal = Vec3.atBottomCenterOf(up);
                speed = 3.0;
            }
            case "listen", "prowl" -> {
                if (target == null) {
                    decideAt = 0;
                    return;
                }
                goal = target;
                speed = d.goal.equals("listen") ? 3.4 : 2.6;
                if (new Vec3(goal.x - at.x, 0, goal.z - at.z).length() < 8) decideAt = 0;
            }
            case "withdraw" -> {
                goal = Vec3.atBottomCenterOf(d.lair);
                speed = 3.0;
            }
            default -> {
                goal = Vec3.atBottomCenterOf(d.lair);
                speed = 2.2;
            }
        }
        Vec3 step = new Vec3(goal.x - at.x, 0, goal.z - at.z);
        double len = step.length();
        if (len > 700) {
            d.at(goal.x - step.x / len * 350, goal.y, goal.z - step.z / len * 350);
            return;
        }
        if (len > 0.01) step = step.scale(Math.min(len, speed) / len);
        double nx = at.x + step.x, nz = at.z + step.z, ny = goal.y;
        BlockPos col = BlockPos.containing(nx, 64, nz);
        boolean deep = goal.y < lvl.getSeaLevel() - 6;
        if (!deep && lvl.hasChunkAt(col)) ny = lvl.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, col.getX(), col.getZ());
        else if (deep) ny = at.y + Mth.clamp(goal.y - at.y, -speed, speed);
        d.at(nx, ny, nz);
        if (d.goal.equals("study") && b != null && lvl.isLoaded(col) && b.study >= 1 && lvl.getRandom().nextFloat() < 0.06f) {
            print(lvl, BlockPos.containing(nx, ny, nz), b, false);
        }
    }

    private static void enter(ServerLevel lvl, LifeData d, List<ServerPlayer> ps, long now) {
        if (now < enterAfter || ManMind.anyActive()) return;
        if (d.goal.equals("withdraw") || d.goal.equals("rest") && d.caution > 75) return;
        Vec3 at = new Vec3(d.x, d.y, d.z);
        ServerPlayer best = null;
        double bd = 1e9;
        for (ServerPlayer p : ps) {
            Profile pr = MindData.profile(p);
            if (pr.playTicks < Cfg.MAN_FIRST_MIN.get() * 1200L) continue;
            if (pr.manPeace && p.getRandom().nextFloat() > 0.08f) continue;
            Watch w = Watch.of(p);
            boolean urged = vn.cavemind.man.Meter.wantsVisit(p) && vn.cavemind.man.Meter.dark(p, w);
            if (!urged && !Tension.allows(p, Tension.Permit.APPROACH) || Director.state(p).act != null) continue;
            boolean clear = clearDay(lvl) && !w.underground;
            if (!urged && clear && (!Cfg.MAN_DAYTIME.get() || d.hunger < 55 && pr.manLevel < 2)) continue;
            double reach = Math.max(40, Range.of(p, w) * 1.3) + 16;
            double dx = p.getX() - at.x, dz = p.getZ() - at.z;
            double dist = Math.sqrt(dx * dx + dz * dz);
            if (dist > reach || Math.abs(p.getY() - at.y) > 48 && !w.underground) continue;
            double s = dist - Math.min(30, Group.isolation(p)) * 0.5 - (urged ? 1000 : 0);
            if (s < bd) {
                bd = s;
                best = p;
            }
        }
        if (best == null) return;
        BlockPos e = entryPoint(best, at);
        if (e == null) {
            enterAfter = now + 100;
            return;
        }
        Entity body = ManMind.make(best, Compat.MAN_STALKER, e, true);
        if (body == null) {
            enterAfter = now + 300;
            return;
        }
        String cause = vi(d.goal) + ": " + why;
        if (!ManMind.arrive(best, body, cause)) {
            body.discard();
            enterAfter = now + 300;
            return;
        }
        d.at(e.getX() + 0.5, e.getY(), e.getZ() + 0.5);
        d.goal = "hunt";
        d.focus = best.getUUID();
        enterAfter = now + 600;
    }

    public static BlockPos entryPoint(ServerPlayer p, Vec3 toward) {
        ServerLevel lvl = p.serverLevel();
        Watch w = Watch.of(p);
        double sight = Range.of(p, w);
        double dist = Mth.clamp(sight * 1.3, 36, 110);
        RandomSource r = p.getRandom();
        double base = toward == null || toward.distanceToSqr(p.position()) < 9 ? r.nextDouble() * Math.PI * 2
            : Math.atan2(toward.z - p.getZ(), toward.x - p.getX());
        for (int i = 0; i < 28; i++) {
            double a = base + (r.nextDouble() - 0.5) * Math.toRadians(i < 12 ? 50 : 140);
            double dd = dist + r.nextDouble() * 14 - 3;
            int x = Mth.floor(p.getX() + Math.cos(a) * dd), z = Mth.floor(p.getZ() + Math.sin(a) * dd);
            BlockPos feet = w.underground ? Spots.caveFloor(lvl, x, Mth.floor(p.getY()), z, 3) : Spots.surface(lvl, x, z, 3);
            if (feet == null || w.underground && Math.abs(feet.getY() - p.getY()) > 10) continue;
            AABB box = Sight.body(feet, 0.6, 3.0);
            if (seenByAnyone(lvl, box, 22)) continue;
            return feet;
        }
        return null;
    }

    static boolean seenByAnyone(ServerLevel lvl, AABB box, double tooClose) {
        Vec3 c = box.getCenter();
        for (ServerPlayer o : lvl.players()) {
            if (o.isSpectator()) continue;
            if (o.position().distanceToSqr(c) < tooClose * tooClose) return true;
            if (o.position().distanceToSqr(c) > 160 * 160) continue;
            Sight si = Sight.of(o, Watch.of(o), c);
            if (si.on && Sight.visible(o, box) > 0) return true;
        }
        return false;
    }

    public static BlockPos awayPoint(ServerPlayer p, Vec3 from) {
        ServerLevel lvl = p.serverLevel();
        Watch w = Watch.of(p);
        double sight = Range.of(p, w);
        Vec3 dir = new Vec3(from.x - p.getX(), 0, from.z - p.getZ());
        if (dir.lengthSqr() < 1) {
            LifeData d = LifeData.get(p.server);
            dir = d.lair == null ? new Vec3(1, 0, 0) : new Vec3(d.lair.getX() - p.getX(), 0, d.lair.getZ() - p.getZ());
        }
        dir = dir.normalize();
        double far = Math.max(50, sight * 1.6);
        for (int i = 0; i < 10; i++) {
            Vec3 at = p.position().add(dir.yRot((float) ((lvl.getRandom().nextDouble() - 0.5) * 0.9)).scale(far + i * 3));
            BlockPos q = w.underground ? Spots.caveFloor(lvl, Mth.floor(at.x), Mth.floor(from.y), Mth.floor(at.z), 3)
                : Spots.surface(lvl, Mth.floor(at.x), Mth.floor(at.z), 3);
            if (q != null) return q;
        }
        Vec3 at = from.add(dir.scale(24));
        return BlockPos.containing(at.x, from.y, at.z);
    }

    private static void cues(ServerLevel lvl, LifeData d, List<ServerPlayer> ps, long now) {
        if (d.goal.equals("rest")) return;
        Vec3 at = new Vec3(d.x, d.y + 1, d.z);
        for (ServerPlayer p : ps) {
            double dist = p.position().distanceTo(at);
            if (dist > 64 || dist < 10) continue;
            if (ManMind.active(p) || !Director.allows(p, Director.Weight.GLIMPSE)) continue;
            long next = NEXT_CUE.getOrDefault(p.getUUID(), now + 400L);
            if (now < next) {
                NEXT_CUE.putIfAbsent(p.getUUID(), next);
                continue;
            }
            RandomSource r = p.getRandom();
            NEXT_CUE.put(p.getUUID(), now + (long) ((900 + r.nextInt(1500)) / Cfg.INTENSITY.get()));
            Watch w = Watch.of(p);
            float roll = r.nextFloat();
            if (roll < 0.4f) {
                for (int i = 0; i < 3 + r.nextInt(3); i++) {
                    Vec3 s = at.add((r.nextDouble() - 0.5) * 2, 0, (r.nextDouble() - 0.5) * 2);
                    String snd = w.underground ? "minecraft:block.stone.step" : "minecraft:block.grass.step";
                    Fx.later(lvl, i * (9 + r.nextInt(5)), () -> Fx.sound(p, snd, SoundSource.HOSTILE, s, 0.55f, 0.8f));
                }
            } else if (roll < 0.65f) {
                Fx.sound(p, "minecraft:block.wood.break", SoundSource.HOSTILE, at, 0.5f, 1.6f);
            } else if (roll < 0.9f) {
                if (Fx.stare(p, null, at, 160, 34) == 0) Fx.sound(p, "minecraft:block.grass.step", SoundSource.HOSTILE, at, 0.4f, 0.7f);
            } else if (lvl.isNight()) {
                Fx.sound(p, "man:long_whisper", SoundSource.HOSTILE, at, 0.18f, 0.7f);
            }
            vn.cavemind.director.PaceLog.encounter(p, "man", "cue", (int) dist + "m, " + vi(d.goal));
        }
    }

    private static void study(ServerLevel lvl, LifeData d, List<ServerPlayer> ps, long now) {
        LifeData.Base b = focusBase(d, ps);
        if (b == null || !loaded(lvl, b)) return;
        double dist = Math.sqrt(Math.pow(d.x - b.center.getX(), 2) + Math.pow(d.z - b.center.getZ(), 2));
        if (dist > b.radius + 30) return;
        b.studyTicks += 20;
        long night = vn.cavemind.damned.Haunt.nightKey(lvl);
        if (b.studyTicks >= 2400 && b.lastStudyNight != night) {
            b.lastStudyNight = night;
            b.studyTicks = 0;
            advance(lvl, d, b);
        }
        if (now < nextAct) return;
        nextAct = now + (long) ((1200 + lvl.getRandom().nextInt(1800)) / Cfg.INTENSITY.get());
        act(lvl, d, b, false);
    }

    private static void advance(ServerLevel lvl, LifeData d, LifeData.Base b) {
        int cap = 2 + (int) (d.nightsAlive * 0.9 * Cfg.INTENSITY.get());
        if (b.study < Math.min(6, cap)) b.study++;
        d.curiosity = Math.max(0, d.curiosity - 12);
        for (UUID u : b.owners) {
            Profile pr = MindData.profile(u);
            if (pr == null) continue;
            long days = pr.firstDay >= 0 ? Dread.day(lvl) - pr.firstDay : 0;
            int lcap = 1 + (int) (days * 0.8) + pr.manSessions / 3;
            if (pr.manLevel < Math.min(10, lcap)) pr.manLevel++;
        }
        CaveMind.debug("the Man studied base at {} (stage {})", b.center, b.study);
    }

    private static boolean act(ServerLevel lvl, LifeData d, LifeData.Base b, boolean overnight) {
        RandomSource r = lvl.getRandom();
        List<String> o = new ArrayList<>();
        o.add("prints");
        if (b.study >= 1 && Cfg.MAN_STEAL.get()) o.add("crops");
        if (b.study >= 2 && Cfg.MAN_DOORS.get()) o.add("door");
        if (b.study >= 3) o.add("torch");
        boolean empty = overnight || ownersAway(lvl, b);
        if (b.study >= 4 && empty && Cfg.LIFE_INSIDE.get()) {
            o.add("inside");
            o.add("inside");
        }
        java.util.Collections.shuffle(o, new java.util.Random(r.nextLong()));
        for (String k : o) {
            boolean ok = switch (k) {
                case "prints" -> ring(lvl, b, 4 + b.study, overnight);
                case "crops" -> crops(lvl, b, overnight);
                case "door" -> door(lvl, b, overnight);
                case "torch" -> {
                    long key = vn.cavemind.damned.Haunt.nightKey(lvl);
                    if (b.torchNight == key || !torches(lvl, d, b, overnight)) yield false;
                    b.torchNight = key;
                    yield true;
                }
                case "inside" -> inside(lvl, b, overnight);
                default -> false;
            };
            if (ok) {
                CaveMind.debug("the Man left {} at base {}", k, b.center);
                return true;
            }
        }
        return false;
    }

    private static boolean ownersAway(ServerLevel lvl, LifeData.Base b) {
        for (ServerPlayer p : lvl.players()) {
            if (Compat.exempt(p)) continue;
            if (p.isSleeping()) continue;
            if (p.blockPosition().distSqr(b.center) < (b.radius + 40) * (b.radius + 40)) return false;
        }
        return true;
    }

    static boolean hidden(ServerLevel lvl, BlockPos q, boolean overnight) {
        Vec3 c = Vec3.atCenterOf(q);
        for (ServerPlayer p : lvl.players()) {
            if (p.isSpectator()) continue;
            double dd = p.position().distanceToSqr(c);
            if (dd < (overnight ? 4 : 100)) return false;
            if (overnight || p.isSleeping()) continue;
            if (Group.sees(p, c, 96)) return false;
        }
        return true;
    }

    static boolean print(ServerLevel lvl, BlockPos near, LifeData.Base b, boolean overnight) {
        BlockPos top = lvl.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, near);
        BlockPos g = top.below();
        BlockState gs = lvl.getBlockState(g);
        if (!gs.is(Blocks.GRASS_BLOCK) && !gs.is(Blocks.PODZOL) && !gs.is(Blocks.MYCELIUM)) return false;
        if (b != null && g.distSqr(b.center) < (b.radius + 2) * (b.radius + 2)) return false;
        if (!hidden(lvl, g, overnight)) return false;
        BlockState above = lvl.getBlockState(top);
        if (above.is(Blocks.SNOW)) lvl.removeBlock(top, false);
        else if (!above.isAir() && above.canBeReplaced() && above.getFluidState().isEmpty()) lvl.removeBlock(top, false);
        else if (!above.isAir()) return false;
        lvl.setBlock(g, Blocks.COARSE_DIRT.defaultBlockState(), 3);
        if (b != null) b.prints++;
        return true;
    }

    private static boolean ring(ServerLevel lvl, LifeData.Base b, int n, boolean overnight) {
        RandomSource r = lvl.getRandom();
        double start = (b.prints * 0.11) % (Math.PI * 2);
        int done = 0;
        BlockPos first = null;
        for (int i = 0; i < n * 2 && done < n; i++) {
            double a = start + i * (2.6 / (b.radius + 8));
            double rr = b.radius + 6 + r.nextInt(3);
            BlockPos q = BlockPos.containing(b.center.getX() + Math.cos(a) * rr, b.center.getY(), b.center.getZ() + Math.sin(a) * rr);
            if (!lvl.isLoaded(q)) continue;
            if (print(lvl, q, b, overnight)) {
                done++;
                if (first == null) first = q;
            }
        }
        if (done > 0 && first != null) clue(lvl, b, "prints", lvl.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, first), 9);
        return done > 0;
    }

    private static boolean crops(ServerLevel lvl, LifeData.Base b, boolean overnight) {
        RandomSource r = lvl.getRandom();
        List<BlockPos> farm = new ArrayList<>();
        int rad = b.radius + 10;
        for (BlockPos q : BlockPos.betweenClosed(b.center.offset(-rad, -4, -rad), b.center.offset(rad, 4, rad))) {
            if (lvl.getBlockState(q).getBlock() instanceof CropBlock && lvl.getBlockState(q.below()).is(Blocks.FARMLAND)) farm.add(q.immutable());
        }
        if (farm.size() < 6) return false;
        BlockPos s = farm.get(r.nextInt(farm.size()));
        Direction dir = Direction.Plane.HORIZONTAL.getRandomDirection(r);
        int done = 0;
        for (int k = 0; k < 5; k++) {
            BlockPos q = s.relative(dir, k);
            if (!(lvl.getBlockState(q).getBlock() instanceof CropBlock) || !hidden(lvl, q, overnight)) continue;
            lvl.removeBlock(q, false);
            lvl.setBlock(q.below(), Blocks.DIRT.defaultBlockState(), 3);
            done++;
        }
        if (done > 0) clue(lvl, b, "harvest", s, 7);
        return done > 0;
    }

    private static boolean door(ServerLevel lvl, LifeData.Base b, boolean overnight) {
        List<BlockPos> doors = new ArrayList<>();
        int rad = b.radius;
        for (BlockPos q : BlockPos.betweenClosed(b.center.offset(-rad, -4, -rad), b.center.offset(rad, 5, rad))) {
            BlockState st = lvl.getBlockState(q);
            if (st.getBlock() instanceof DoorBlock && st.getValue(DoorBlock.HALF) == DoubleBlockHalf.LOWER && !st.getValue(DoorBlock.OPEN)
                && !st.is(Blocks.IRON_DOOR) && outdoor(lvl, q)) doors.add(q.immutable());
            else if (st.getBlock() instanceof FenceGateBlock && !st.getValue(FenceGateBlock.OPEN)) doors.add(q.immutable());
        }
        java.util.Collections.shuffle(doors, new java.util.Random(lvl.getRandom().nextLong()));
        for (BlockPos q : doors) {
            if (!hidden(lvl, q, overnight)) continue;
            BlockState st = lvl.getBlockState(q);
            if (st.getBlock() instanceof DoorBlock db) db.setOpen(null, lvl, st, q, true);
            else lvl.setBlock(q, st.setValue(FenceGateBlock.OPEN, true), 10);
            if (!overnight) {
                for (ServerPlayer p : lvl.players()) {
                    if (p.blockPosition().distSqr(q) < 40 * 40) Fx.sound(p, "minecraft:block.wooden_door.open", SoundSource.BLOCKS, Vec3.atCenterOf(q), 0.5f, 0.85f);
                }
            }
            clue(lvl, b, "door", q, 5);
            return true;
        }
        return false;
    }

    static boolean loaded(ServerLevel lvl, LifeData.Base b) {
        int r = b.radius + 12;
        return lvl.hasChunksAt(b.center.offset(-r, 0, -r), b.center.offset(r, 0, r));
    }

    static boolean outdoor(ServerLevel lvl, BlockPos door) {
        for (Direction d : Direction.Plane.HORIZONTAL) {
            if (lvl.canSeeSky(door.relative(d)) || lvl.canSeeSky(door.relative(d, 2))) return true;
        }
        return false;
    }

    private static boolean torches(ServerLevel lvl, LifeData d, LifeData.Base b, boolean overnight) {
        RandomSource r = lvl.getRandom();
        List<BlockPos> lit = new ArrayList<>();
        int rad = b.radius + 6;
        for (BlockPos q : BlockPos.betweenClosed(b.center.offset(-rad, -4, -rad), b.center.offset(rad, 6, rad))) {
            BlockState st = lvl.getBlockState(q);
            if ((st.getBlock() instanceof TorchBlock || st.getBlock() instanceof WallTorchBlock) && !st.is(Blocks.REDSTONE_TORCH) && !st.is(Blocks.REDSTONE_WALL_TORCH)
                && lvl.canSeeSky(q.above())) lit.add(q.immutable());
        }
        if (lit.size() < 3) return false;
        lit.sort((a, c) -> Double.compare(c.distSqr(b.center), a.distSqr(b.center)));
        int take = Math.min(2, lit.size() - 2), done = 0;
        for (int i = 0; i < lit.size() && done < take; i++) {
            BlockPos q = lit.get(i);
            if (!hidden(lvl, q, overnight)) continue;
            BlockPos dst = nextTrail(lvl, d, b);
            if (dst == null) break;
            lvl.removeBlock(q, false);
            lvl.setBlock(dst, Blocks.TORCH.defaultBlockState(), 3);
            b.torchTrail = dst;
            b.torchesTaken++;
            done++;
        }
        if (done > 0 && b.torchTrail != null) clue(lvl, b, "torches", b.torchTrail, 10);
        return done > 0;
    }

    private static BlockPos nextTrail(ServerLevel lvl, LifeData d, LifeData.Base b) {
        Vec3 c = Vec3.atBottomCenterOf(b.center);
        Vec3 dir = new Vec3(d.lair.getX() - c.x, 0, d.lair.getZ() - c.z);
        if (dir.lengthSqr() < 1) dir = new Vec3(1, 0, 0);
        dir = dir.normalize();
        double from = b.torchTrail == null ? b.radius + 14 : Math.sqrt(Math.pow(b.torchTrail.getX() - c.x, 2) + Math.pow(b.torchTrail.getZ() - c.z, 2)) + 7;
        for (int k = 0; k < 4; k++) {
            Vec3 at = c.add(dir.scale(from + k * 2));
            BlockPos col = BlockPos.containing(at.x, 64, at.z);
            if (!lvl.isLoaded(col)) return null;
            BlockPos top = lvl.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, col);
            BlockState below = lvl.getBlockState(top.below());
            if (below.is(BlockTags.LEAVES) || !lvl.getFluidState(top.below()).isEmpty()) continue;
            if (!lvl.getBlockState(top).isAir()) continue;
            if (!Blocks.TORCH.defaultBlockState().canSurvive(lvl, top)) continue;
            return top;
        }
        return null;
    }

    private static boolean inside(ServerLevel lvl, LifeData.Base b, boolean overnight) {
        RandomSource r = lvl.getRandom();
        List<BlockPos> beds = new ArrayList<>(), boxes = new ArrayList<>();
        int rad = b.radius;
        for (BlockPos q : BlockPos.betweenClosed(b.center.offset(-rad, -4, -rad), b.center.offset(rad, 6, rad))) {
            BlockState st = lvl.getBlockState(q);
            if (st.getBlock() instanceof BedBlock && st.getValue(BedBlock.PART) == BedPart.HEAD) beds.add(q.immutable());
            else if (st.hasBlockEntity() && lvl.getBlockEntity(q) instanceof Container c && c.getContainerSize() >= 9) boxes.add(q.immutable());
        }
        boolean did = false;
        if (!beds.isEmpty() && r.nextFloat() < 0.7f) {
            BlockPos bed = beds.get(r.nextInt(beds.size()));
            for (Direction dir : Direction.Plane.HORIZONTAL) {
                BlockPos q = bed.relative(dir);
                if (!lvl.getBlockState(q).isAir() || !lvl.getBlockState(q.below()).isFaceSturdy(lvl, q.below(), Direction.UP)) continue;
                if (!hidden(lvl, q, overnight)) continue;
                lvl.setBlock(q, Blocks.CANDLE.defaultBlockState().setValue(CandleBlock.CANDLES, 1 + r.nextInt(3)).setValue(CandleBlock.LIT, true), 3);
                clue(lvl, b, "inside", q, 6);
                did = true;
                break;
            }
        }
        if (boxes.size() >= 2 && (!did || r.nextFloat() < 0.5f)) {
            BlockPos a = boxes.get(r.nextInt(boxes.size())), c = boxes.get(r.nextInt(boxes.size()));
            if (!a.equals(c) && hidden(lvl, a, overnight) && hidden(lvl, c, overnight)
                && lvl.getBlockEntity(a) instanceof Container ca && lvl.getBlockEntity(c) instanceof Container cc) {
                int from = -1;
                for (int i = 0; i < ca.getContainerSize(); i++) if (!ca.getItem(i).isEmpty() && r.nextFloat() < 0.5f) {
                    from = i;
                    break;
                }
                int to = -1;
                for (int i = 0; i < cc.getContainerSize(); i++) if (cc.getItem(i).isEmpty()) {
                    to = i;
                    if (r.nextFloat() < 0.4f) break;
                }
                if (from >= 0 && to >= 0) {
                    ItemStack st = ca.removeItemNoUpdate(from);
                    cc.setItem(to, st);
                    ca.setChanged();
                    cc.setChanged();
                    clue(lvl, b, "inside", c, 4);
                    did = true;
                }
            }
        }
        if (did) {
            b.insideVisits++;
            if (b.study >= 4) b.sitArmedDay = Dread.day(lvl);
        }
        return did;
    }

    // A visit to one player's home for the THE MAN bar, done where nobody is looking (anywhere, when 'freely': the owner is far away).
    // Returns what he left behind, or null if he found nothing he could do.
    public static String homeVisit(ServerPlayer p, BlockPos home, boolean freely) {
        ServerLevel lvl = p.serverLevel();
        LifeData d = LifeData.get(p.server);
        LifeData.Base b = Bases.owned(d, p);
        if (b == null || b.center.distSqr(home) > 40 * 40) {
            b = new LifeData.Base();
            b.center = home;
            b.radius = 14;
            b.owners.add(p.getUUID());
        }
        if (!loaded(lvl, b)) return null;
        List<String> o = new ArrayList<>(List.of("door", "torch", "inside", "prints"));
        java.util.Collections.shuffle(o, new java.util.Random(lvl.getRandom().nextLong()));
        for (String k : o) {
            boolean ok = switch (k) {
                case "door" -> Cfg.MAN_DOORS.get() && door(lvl, b, freely);
                case "torch" -> d.lair != null && torches(lvl, d, b, freely);
                case "inside" -> Cfg.LIFE_INSIDE.get() && inside(lvl, b, freely);
                default -> ring(lvl, b, 6, freely);
            };
            if (ok) return k;
        }
        return candle(lvl, b, freely) ? "candle" : null;
    }

    // The fallback that always fits somewhere: a lit candle on a floor near the home point, indoors if possible.
    static boolean candle(ServerLevel lvl, LifeData.Base b, boolean freely) {
        RandomSource r = lvl.getRandom();
        for (int pass = 0; pass < 2; pass++) {
            for (int i = 0; i < 60; i++) {
                BlockPos q = b.center.offset(r.nextInt(11) - 5, r.nextInt(5) - 2, r.nextInt(11) - 5);
                if (!lvl.getBlockState(q).isAir() || !lvl.getBlockState(q.below()).isFaceSturdy(lvl, q.below(), Direction.UP)) continue;
                if (pass == 0 && lvl.canSeeSky(q) || !hidden(lvl, q, freely)) continue;
                lvl.setBlock(q, Blocks.CANDLE.defaultBlockState().setValue(CandleBlock.CANDLES, 1 + r.nextInt(3)).setValue(CandleBlock.LIT, true), 3);
                clue(lvl, b, "inside", q, 6);
                return true;
            }
        }
        return false;
    }

    // Someone with no home yet: he circles the spot they are standing on, out of sight.
    public static boolean campVisit(ServerPlayer p) {
        ServerLevel lvl = p.serverLevel();
        RandomSource r = lvl.getRandom();
        BlockPos c = p.blockPosition();
        double start = r.nextDouble() * Math.PI * 2;
        int done = 0;
        BlockPos first = null;
        for (int i = 0; i < 24 && done < 6; i++) {
            double a = start + i * 0.26, rr = 14 + r.nextInt(4);
            BlockPos q = BlockPos.containing(c.getX() + Math.cos(a) * rr, c.getY(), c.getZ() + Math.sin(a) * rr);
            if (!lvl.isLoaded(q) || !print(lvl, q, null, false)) continue;
            done++;
            if (first == null) first = q;
        }
        if (done < 3) return false;
        MindData.profile(p).clue(new Profile.Clue("prints", lvl.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, first), Dread.day(lvl), 9));
        return true;
    }

    private static void clue(ServerLevel lvl, LifeData.Base b, String kind, BlockPos at, int radius) {
        for (UUID u : b.owners) {
            Profile pr = MindData.profile(u);
            if (pr != null) pr.clue(new Profile.Clue(kind, at.immutable(), Dread.day(lvl), radius));
        }
    }

    private static void skipped(MinecraftServer server, ServerLevel lvl, long from, long to) {
        long ft = from % 24000L;
        if (!(ft >= 12000 || ft < 500) || to - from > 30000) return;
        List<ServerPlayer> ps = players(lvl);
        if (ps.isEmpty()) return;
        vn.cavemind.man.Meter.slept(ps);
        LifeData d = LifeData.get(server);
        Bases.refresh(d, ps);
        long key = ft < 1000 ? from / 24000L - 1 : from / 24000L;
        for (LifeData.Base b : d.bases) {
            boolean here = false;
            for (ServerPlayer p : ps) if (b.owners.contains(p.getUUID())) here = true;
            if (!here || !loaded(lvl, b)) continue;
            if (b.lastStudyNight != key) {
                b.lastStudyNight = key;
                advance(lvl, d, b);
            }
            ring(lvl, b, 8 + b.study * 2, true);
            int acts = 1 + b.study / 2;
            for (int i = 0; i < acts; i++) act(lvl, d, b, true);
            if (b.study >= 5 && Cfg.MAN_SCHEMES.get()) {
                for (ServerPlayer p : ps) {
                    if (!b.owners.contains(p.getUUID())) continue;
                    Profile pr = MindData.profile(p);
                    String k = pr.nest == null && pr.trailFrom != null ? "nest" : pr.peep == null ? "peep" : null;
                    if (k != null && vn.cavemind.man.Schemes.run(p, Watch.of(p), pr, k, false)) break;
                }
            }
            if (Compat.damnedActive() && b.lastDamnedNight != key) {
                b.lastDamnedNight = key;
                for (ServerPlayer p : ps) if (b.owners.contains(p.getUUID())) vn.cavemind.damned.Haunt.overnight(p);
            }
            CaveMind.debug("the night was skipped; base {} found changed (stage {})", b.center, b.study);
        }
        d.at(d.lair.getX() + 0.5, d.lair.getY(), d.lair.getZ() + 0.5);
        d.goal = "rest";
        decideAt = 0;
    }

    private static final Map<UUID, Long> LAIR_DAY = new HashMap<>();

    private static void lairVisits(ServerLevel lvl, LifeData d, List<ServerPlayer> ps) {
        if (d.lair == null || !lvl.isLoaded(d.lair)) return;
        long day = Dread.day(lvl);
        for (ServerPlayer p : ps) {
            if (p.blockPosition().distSqr(d.lair) > 30 * 30) continue;
            Long last = LAIR_DAY.get(p.getUUID());
            if (last != null && last == day) continue;
            if (vn.cavemind.man.ManMind.anyActive() || !Director.allows(p, Director.Weight.GLIMPSE)) continue;
            if (d.lair.distSqr(BlockPos.containing(d.x, d.y, d.z)) > 40 * 40) continue;
            if (Glimpse.lair(p, d.lair)) {
                LAIR_DAY.put(p.getUUID(), day);
                d.caution = Math.min(100, d.caution + 10);
            }
        }
    }

    private static void armSits(ServerLevel lvl, LifeData d, List<ServerPlayer> ps, long now) {
        for (ServerPlayer p : ps) {
            LifeData.Base b = Bases.owned(d, p);
            if (b == null) continue;
            boolean in = p.blockPosition().distSqr(b.center) < b.radius * b.radius;
            if (!in) {
                if (p.blockPosition().distSqr(b.center) > (b.radius + 50) * (b.radius + 50)) AWAY_SINCE.putIfAbsent(p.getUUID(), now);
                continue;
            }
            Long away = AWAY_SINCE.remove(p.getUUID());
            boolean back = away != null && now - away > 2400;
            int tod = Dread.time(lvl);
            boolean dark = tod >= 12500 && tod < 23300;
            if (!dark || b.sitArmedDay < 0 || !Cfg.LIFE_INSIDE.get()) continue;
            if (!back && !(b.study >= 6 && lvl.getRandom().nextFloat() < 0.002f)) continue;
            if (ManMind.anyActive() || Math.sqrt(Math.pow(d.x - b.center.getX(), 2) + Math.pow(d.z - b.center.getZ(), 2)) > b.radius + 140) continue;
            if (Glimpse.sitIn(p, b)) {
                b.sitArmedDay = -1;
                b.sitIns++;
                d.at(b.center.getX() + 0.5, b.center.getY(), b.center.getZ() + 0.5);
            }
        }
    }

    private Life() {}
}
