package vn.cavemind.life;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import vn.cavemind.CaveMind;
import vn.cavemind.Cfg;
import vn.cavemind.compat.Compat;
import vn.cavemind.damned.Game;
import vn.cavemind.director.Director;
import vn.cavemind.director.Dread;
import vn.cavemind.director.Tension;
import vn.cavemind.memory.MindData;
import vn.cavemind.memory.Profile;
import vn.cavemind.sense.Ears;
import vn.cavemind.sense.Range;
import vn.cavemind.sense.Watch;

public final class Child {
    private static long tryAfter;
    private static String why = "";

    public static void clear() {
        tryAfter = 0;
        why = "";
    }

    public static boolean on() {
        return Game.on();
    }

    public static void played(MinecraftServer server, double relief) {
        LifeData d = LifeData.get(server);
        d.lonely = Math.max(0, d.lonely - relief);
        d.childRestUntil = server.overworld().getGameTime() + 2400;
        d.setDirty();
    }

    static BlockPos home(ServerLevel lvl, LifeData d, List<ServerPlayer> ps) {
        for (String k : new String[]{"p", "g", "h"}) {
            BlockPos s = vn.cavemind.lore.Lore.site(lvl, k);
            if (s != null) return s;
        }
        if (d.childHome != null) return d.childHome;
        BlockPos from = !d.bases.isEmpty() ? d.bases.get(0).center : ps.get(0).blockPosition();
        RandomSource r = lvl.getRandom();
        double a = r.nextDouble() * Math.PI * 2, dist = 120 + r.nextInt(60);
        int x = from.getX() + (int) (Math.cos(a) * dist), z = from.getZ() + (int) (Math.sin(a) * dist);
        int y = lvl.hasChunkAt(new BlockPos(x, 64, z)) ? lvl.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z) : from.getY();
        d.childHome = new BlockPos(x, y, z);
        return d.childHome;
    }

    public static Vec3 where(MinecraftServer server) {
        LifeData d = LifeData.get(server);
        return d.childPlaced ? new Vec3(d.cx, d.cy, d.cz) : null;
    }

    public static void tick(MinecraftServer server) {
        if (!on()) return;
        long now = server.getTickCount();
        if (now % 20 != 7) return;
        ServerLevel lvl = server.overworld();
        List<ServerPlayer> ps = Life.players(lvl);
        if (ps.isEmpty()) return;
        LifeData d = LifeData.get(server);
        BlockPos home = home(lvl, d, ps);
        if (!d.childPlaced) {
            d.cx = home.getX() + 0.5;
            d.cy = home.getY();
            d.cz = home.getZ() + 0.5;
            d.childPlaced = true;
        }
        int tod = Dread.time(lvl);
        boolean night = tod >= 13000 && tod < 23200;
        double rate = 0.03 * Cfg.INTENSITY.get() * Math.max(0.1, Cfg.SEEK_FREQ.get()) * (night ? 1.6 : 1.0);
        d.lonely = Math.min(100, d.lonely + rate);
        Vec3 body = Game.where(server);
        if (body != null || Game.anyActive()) {
            if (body != null) {
                d.cx = body.x;
                d.cy = body.y;
                d.cz = body.z;
            }
            d.setDirty();
            return;
        }
        Vec3 at = new Vec3(d.cx, d.cy, d.cz);
        ServerPlayer target = pick(lvl, ps, at);
        long gt = lvl.getGameTime();
        Vec3 goal;
        double speed;
        if (target == null || d.lonely < 40 || gt < d.childRestUntil) {
            RandomSource r = lvl.getRandom();
            goal = Vec3.atBottomCenterOf(home).add((r.nextDouble() - 0.5) * 24, 0, (r.nextDouble() - 0.5) * 24);
            speed = 1.4;
            why = target == null ? "không ai chơi cùng" : d.lonely < 40 ? "chơi một mình quanh nhà" : "vừa chơi xong";
        } else {
            Ears.Noise n = Ears.loudest(lvl, at, now, 400, 2.0);
            goal = n != null && n.who() != null && n.who().equals(target.getUUID()) ? n.pos() : target.position();
            speed = 1.8 + d.lonely / 50.0;
            why = "đi tìm " + target.getGameProfile().getName() + " để rủ chơi";
        }
        Vec3 step = new Vec3(goal.x - at.x, 0, goal.z - at.z);
        double len = step.length();
        if (len > 600) {
            at = goal.subtract(step.scale(300 / len));
            step = Vec3.ZERO;
            len = 0;
        }
        if (len > 0.01) step = step.scale(Math.min(len, speed) / len);
        double nx = at.x + step.x, nz = at.z + step.z, ny = goal.y;
        BlockPos col = BlockPos.containing(nx, 64, nz);
        if (goal.y >= lvl.getSeaLevel() - 6 && lvl.hasChunkAt(col)) ny = lvl.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, col.getX(), col.getZ());
        d.cx = nx;
        d.cy = ny;
        d.cz = nz;
        d.setDirty();
        if (target != null && d.lonely >= 58 && gt >= d.childRestUntil && now >= tryAfter) invite(lvl, d, target, now);
    }

    static ServerPlayer pick(ServerLevel lvl, List<ServerPlayer> ps, Vec3 at) {
        long day = Dread.day(lvl);
        if (day < Cfg.SEEK_DAY.get()) return null;
        ServerPlayer best = null;
        double bs = -1e9;
        for (ServerPlayer p : ps) {
            Profile pr = MindData.profile(p);
            if (pr.dQuietUntilDay >= 0 && day < pr.dQuietUntilDay) continue;
            if (pr.dPeace) continue;
            if (pr.playTicks < 12000) continue;
            if (!Glimpse.damnedFor(p)) continue;
            double s = pr.dStage * 4 + pr.dDebt * 6 - at.distanceTo(p.position()) * 0.02 + p.getRandom().nextDouble();
            if (s > bs) {
                bs = s;
                best = p;
            }
        }
        return best;
    }

    static void invite(ServerLevel lvl, LifeData d, ServerPlayer p, long now) {
        Watch w = Watch.of(p);
        double reach = Math.max(40, Range.of(p, w) * 1.6) + 8;
        double dist = Math.sqrt(Math.pow(p.getX() - d.cx, 2) + Math.pow(p.getZ() - d.cz, 2));
        if (dist > reach) return;
        tryAfter = now + 200;
        if (!Tension.allows(p, Tension.Permit.APPROACH) || !Director.free(p) || w.indoors || p.isSleeping()) return;
        if (vn.cavemind.man.ManMind.active(p) || vn.cavemind.hero.Herobrine.busy(p) || vn.cavemind.damned.Haunt.busy(p)) return;
        Profile pr = MindData.profile(p);
        String cause = "nó cô đơn (" + (int) d.lonely + ")" + (pr.dDebt > 0 ? ", bạn còn nợ nó một ván" : "");
        if (Game.start(p, cause)) {
            tryAfter = now + 2400;
            CaveMind.debug("the Damned found {} ({})", p.getGameProfile().getName(), why);
        }
    }

    public static String describe(MinecraftServer server, ServerPlayer p) {
        if (!on()) return "tắt";
        LifeData d = LifeData.get(server);
        Vec3 at = where(server);
        String dist = at == null ? "?" : (int) Math.sqrt(Math.pow(at.x - p.getX(), 2) + Math.pow(at.z - p.getZ(), 2)) + "m";
        return why + ", cách bạn " + dist + ", cô đơn " + (int) d.lonely;
    }

    private Child() {}
}
