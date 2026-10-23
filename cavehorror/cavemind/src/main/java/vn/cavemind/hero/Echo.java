package vn.cavemind.hero;

import java.util.ArrayDeque;
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
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.TorchBlock;
import net.minecraft.world.level.block.WallTorchBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import vn.cavemind.CaveMind;
import vn.cavemind.Cfg;
import vn.cavemind.compat.Compat;
import vn.cavemind.director.Director;
import vn.cavemind.director.Dread;
import vn.cavemind.director.PaceLog;
import vn.cavemind.director.Tension;
import vn.cavemind.memory.MindData;
import vn.cavemind.memory.Profile;
import vn.cavemind.net.Net;
import vn.cavemind.sense.Group;
import vn.cavemind.sense.Watch;
import vn.cavemind.world.Fx;

public final class Echo {
    public enum Ch {
        STEPS(8, 3), FRIEND(10, 6), PICK(10, 4), TORCH(14, 6), DARK(12, 6), MINER(28, 14), SILHOUETTE(30, 14), MIMIC(34, 18), BED(42, 30), SHOULDER(40, 30),
        TRACE(18, 10), LEAVE(25, 240);

        final double cost;
        final int cd;

        Ch(double cost, int cd) {
            this.cost = cost;
            this.cd = cd;
        }
    }

    static final class Run {
        Ch ch;
        long until, next;
        int steps, beat, cycles, hits;
        boolean late;
        BlockPos rock;
        Vec3 from, to;
        int gap = 18;
    }

    private static final Map<UUID, Run> RUNS = new HashMap<>();
    private static final Map<UUID, long[]> ROCK = new HashMap<>();

    public static boolean broke(ServerPlayer p, BlockPos q) {
        long[] k = ROCK.get(p.getUUID());
        if (k == null) return false;
        long now = p.server.getTickCount();
        if (now > k[1]) {
            ROCK.remove(p.getUUID());
            return false;
        }
        if (BlockPos.of(k[0]).distManhattan(q) > 1) return false;
        ROCK.remove(p.getUUID());
        boolean miner = vn.cavemind.world.Moon.is(p.serverLevel(), vn.cavemind.world.Moon.MINER);
        if (!miner && p.getRandom().nextFloat() > 0.3f) {
            p.displayClientMessage(Component.literal("Chỉ là đá. Tiếng cuốc đã tắt.").withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC), true);
            return false;
        }
        net.minecraft.world.level.block.Block.popResource(p.serverLevel(), q, new net.minecraft.world.item.ItemStack(vn.cavemind.gear.Gear.ECHO.get(), miner && p.getRandom().nextBoolean() ? 2 : 1));
        Fx.sound(p, "minecraft:block.amethyst_block.resonate", SoundSource.BLOCKS, Vec3.atCenterOf(q), 0.8f, 0.6f);
        p.displayClientMessage(Component.literal("Trong lòng viên đá có một mảnh còn ấm, như vừa có ai gõ vào nó.").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC), true);
        return true;
    }
    private static final Map<UUID, Long> NEXT = new HashMap<>();
    private static final Map<UUID, ArrayDeque<Long>> DIGS = new HashMap<>();
    private static final Map<UUID, ArrayDeque<String>> CHAT = new HashMap<>();
    private static final Map<UUID, EnumMap<Ch, Long>> USED = new HashMap<>();

    public static void forget(UUID id) {
        RUNS.remove(id);
        NEXT.remove(id);
        DIGS.remove(id);
        USED.remove(id);
        Ghosts.forget(id);
    }

    public static void clear() {
        RUNS.clear();
        NEXT.clear();
        DIGS.clear();
        CHAT.clear();
        USED.clear();
        Ghosts.clear();
    }

    public static void dug(ServerPlayer p) {
        ArrayDeque<Long> d = DIGS.computeIfAbsent(p.getUUID(), k -> new ArrayDeque<>());
        d.addLast((long) p.server.getTickCount());
        while (d.size() > 14) d.removeFirst();
        if (p.level().dimension() == Level.OVERWORLD) {
            Profile pr = MindData.profile(p);
            boolean miner = Watch.of(p).underground && vn.cavemind.world.Moon.is(p.serverLevel(), vn.cavemind.world.Moon.MINER);
            pr.heroPressure = Math.min(100, pr.heroPressure + 0.15 * Cfg.HERO_FREQ.get() * (miner ? 3 : 1));
        }
    }

    public static void built(ServerPlayer p) {
        if (p.level().dimension() != Level.OVERWORLD) return;
        Profile pr = MindData.profile(p);
        pr.heroPressure = Math.min(100, pr.heroPressure + 0.1 * Cfg.HERO_FREQ.get());
    }

    public static void chat(ServerPlayer p, String msg) {
        String m = msg.strip();
        if (m.isEmpty() || m.startsWith("/")) return;
        ArrayDeque<String> c = CHAT.computeIfAbsent(p.getUUID(), k -> new ArrayDeque<>());
        c.addLast(m.length() > 60 ? m.substring(0, 60) : m);
        while (c.size() > 20) c.removeFirst();
        if (m.toLowerCase(java.util.Locale.ROOT).contains("herobrine")) {
            Profile pr = MindData.profile(p);
            ServerLevel lvl = p.server.overworld();
            pr.heroCalled = Dread.day(lvl) * 24000L + Dread.time(lvl);
        }
    }

    public static String saying(ServerPlayer p) {
        ArrayDeque<String> c = CHAT.get(p.getUUID());
        if (c == null || c.isEmpty()) return null;
        Map<String, Integer> n = new HashMap<>();
        String best = c.peekLast();
        int bn = 0;
        for (String s : c) {
            String k = s.toLowerCase(java.util.Locale.ROOT);
            int v = n.merge(k, 1, Integer::sum);
            if (v > bn) {
                bn = v;
                best = s;
            }
        }
        return best;
    }

    public static String describe(ServerPlayer p) {
        Profile pr = MindData.profile(p);
        Run r = RUNS.get(p.getUUID());
        long now = p.server.getTickCount();
        long gap = Math.max(0, (NEXT.getOrDefault(p.getUUID(), 0L) - now) / 20);
        return "áp lực ký ức " + (int) pr.heroPressure + (r != null ? ", đang vọng " + r.ch : "") + ", lần vọng tới sau ít nhất " + gap + "s";
    }

    public static void tick(ServerPlayer p, Watch w, Profile pr, long now) {
        Ghosts.tick(p, now);
        Run r = RUNS.get(p.getUUID());
        if (r != null) {
            boolean keep;
            try {
                keep = run(p, w, pr, r, now);
            } catch (Throwable t) {
                CaveMind.LOG.warn("[CaveMind] echo failed", t);
                keep = false;
            }
            if (!keep) RUNS.remove(p.getUUID());
            return;
        }
        if (now % 20 != 3) return;
        ServerLevel lvl = p.serverLevel();
        if (!Herobrine.enabled() || lvl.dimension() != Level.OVERWORLD || Compat.exempt(p)) return;
        double freq = Cfg.INTENSITY.get() * Cfg.HERO_FREQ.get();
        if (w.idle < 600) pr.heroPressure = Math.min(100, pr.heroPressure + (w.underground ? 0.03 : 0.012) * freq);
        if (Dread.day(lvl) < Cfg.HERO_START_DAY.get()) return;
        if (pr.heroPeace) {
            pr.heroPressure = Math.min(pr.heroPressure, 20);
            return;
        }
        if (now < NEXT.getOrDefault(p.getUUID(), now + 2400L)) {
            NEXT.putIfAbsent(p.getUUID(), now + 2400L);
            return;
        }
        Tension.Phase ph = Tension.phase(p);
        if (ph != Tension.Phase.BUILD_UP && ph != Tension.Phase.FADE) return;
        if (Tension.limit(p) != null || Director.state(p).act != null || vn.cavemind.man.ManMind.active(p) || Herobrine.busy(p) || p.isSleeping()) return;
        if (w.idle > 600) return;
        Ch ch = choose(p, w, pr);
        if (ch == null) {
            NEXT.put(p.getUUID(), now + 400);
            return;
        }
        if (!start(p, w, pr, ch, now)) {
            mark(p, ch, pr.playTicks - (long) (ch.cd * 1200 * 0.7));
            NEXT.put(p.getUUID(), now + 200);
            return;
        }
        pr.heroPressure = Math.max(0, pr.heroPressure - ch.cost);
        mark(p, ch, pr.playTicks);
        boolean big = ch.cost >= 25;
        RandomSource rnd = p.getRandom();
        long gap = (long) ((big ? 4800 + rnd.nextInt(3600) : 1800 + rnd.nextInt(1800)) / Math.max(0.25, freq));
        if (w.underground && vn.cavemind.world.Moon.is(lvl, vn.cavemind.world.Moon.MINER)) gap /= 2.5;
        NEXT.put(p.getUUID(), now + gap);
        PaceLog.encounter(p, "hero", "echo " + ch, "pressure " + (int) (pr.heroPressure + ch.cost));
        CaveMind.debug("echo {} for {} (pressure left {})", ch, p.getGameProfile().getName(), (int) pr.heroPressure);
    }

    static void mark(ServerPlayer p, Ch ch, long t) {
        USED.computeIfAbsent(p.getUUID(), k -> new EnumMap<>(Ch.class)).put(ch, t);
    }

    static boolean ready(ServerPlayer p, Profile pr, Ch ch) {
        Long t = USED.computeIfAbsent(p.getUUID(), k -> new EnumMap<>(Ch.class)).get(ch);
        return t == null || pr.playTicks - t >= ch.cd * 1200L / Math.max(0.25, Cfg.HERO_FREQ.get());
    }

    static boolean unlocked(ServerPlayer p, Profile pr, Ch ch) {
        long days = pr.firstDay >= 0 ? Dread.day(p.serverLevel()) - pr.firstDay : 0;
        boolean mine = pr.lore.contains("m");
        boolean seen = mine || days >= Cfg.HERO_TIER1.get();
        boolean close = mine && days >= Cfg.HERO_TIER1.get() || days >= Cfg.HERO_TIER2.get();
        return switch (ch) {
            case SILHOUETTE, MIMIC, MINER -> seen && Net.modded(p);
            case BED, SHOULDER -> close && Net.modded(p);
            case LEAVE -> Cfg.HERO_FAKE_LEAVE.get() && mine;
            case TRACE -> Cfg.HERO_EDITS.get();
            default -> true;
        };
    }

    static Ch choose(ServerPlayer p, Watch w, Profile pr) {
        boolean home = pr.home != null && pr.home.distSqr(p.blockPosition()) < 28 * 28;
        boolean grouped = Group.grouped(p, 30);
        List<Ch> order;
        if (w.underground) order = List.of(Ch.PICK, Ch.MINER, Ch.TORCH, Ch.STEPS, Ch.TRACE);
        else if (home) order = List.of(Ch.BED, Ch.DARK, Ch.FRIEND, Ch.LEAVE, Ch.TRACE, Ch.STEPS);
        else if (grouped) order = List.of(Ch.MIMIC, Ch.SILHOUETTE, Ch.FRIEND, Ch.STEPS, Ch.TRACE);
        else order = List.of(Ch.SILHOUETTE, Ch.STEPS, Ch.MIMIC, Ch.FRIEND, Ch.TRACE, Ch.SHOULDER);
        RandomSource r = p.getRandom();
        Ch fallback = null;
        for (Ch c : order) {
            if (c.cost > pr.heroPressure || !ready(p, pr, c) || !unlocked(p, pr, c)) continue;
            if (fallback == null) fallback = c;
            if (r.nextFloat() < 0.65f) return c;
        }
        return fallback;
    }

    static boolean start(ServerPlayer p, Watch w, Profile pr, Ch ch, long now) {
        RandomSource r = p.getRandom();
        Run run = new Run();
        run.ch = ch;
        switch (ch) {
            case STEPS -> {
                if (w.crumbs.size() < 8) return false;
                run.until = now + 300 + r.nextInt(300);
            }
            case FRIEND -> {
                ServerPlayer friend = farFriend(p);
                if (friend == null && !pr.lore.contains("m")) return false;
                Vec3 dir;
                if (pr.home != null && pr.home.distSqr(p.blockPosition()) < 80 * 80 && pr.home.distSqr(p.blockPosition()) > 25) dir = Vec3.atCenterOf(pr.home).subtract(p.position());
                else dir = Vec3.directionFromRotation(0, p.getYRot() + 180);
                dir = new Vec3(dir.x, 0, dir.z).normalize();
                run.from = p.position().add(dir.scale(15 + r.nextInt(5)));
                run.to = p.position().add(dir.scale(3));
                run.steps = 0;
                run.next = now + 10;
            }
            case PICK -> {
                BlockPos rock = rock(p);
                if (rock == null) return false;
                run.rock = rock;
                run.gap = rhythm(p);
                run.cycles = 4 + r.nextInt(4);
                run.next = now + 20;
                run.until = now + 1200;
            }
            case TORCH -> {
                return torch(p, w);
            }
            case DARK -> {
                return dark(p, pr);
            }
            case MINER -> {
                return Herobrine.start(p, w, pr, "miner", now, false);
            }
            case SILHOUETTE -> {
                return Herobrine.start(p, w, pr, r.nextBoolean() ? "walker" : "horizon", now, false) || Herobrine.start(p, w, pr, "horizon", now, false);
            }
            case MIMIC -> {
                return Herobrine.start(p, w, pr, "replay", now, false);
            }
            case BED -> {
                return Herobrine.start(p, w, pr, "bed", now, false);
            }
            case SHOULDER -> {
                return Herobrine.start(p, w, pr, "shoulder", now, false);
            }
            case TRACE -> {
                String[] ks = w.underground ? new String[]{"tunnel", "sign"} : new String[]{"sign", "leafless", "tunnel"};
                for (String k : ks) if (Traces.run(p, w, pr, k, false)) return true;
                return w.outside && Net.modded(p) && Herobrine.start(p, w, pr, "builder", now, false);
            }
            case LEAVE -> {
                ServerPlayer friend = farFriend(p);
                if (friend == null) return false;
                p.sendSystemMessage(Component.translatable("multiplayer.player.left", friend.getDisplayName()).withStyle(ChatFormatting.YELLOW));
                return true;
            }
        }
        RUNS.put(p.getUUID(), run);
        return true;
    }

    static ServerPlayer farFriend(ServerPlayer p) {
        List<ServerPlayer> c = new ArrayList<>();
        for (ServerPlayer o : p.server.getPlayerList().getPlayers()) {
            if (o == p || o.isSpectator()) continue;
            if (o.level() != p.level() || o.distanceTo(p) > 64) c.add(o);
        }
        return c.isEmpty() ? null : c.get(p.getRandom().nextInt(c.size()));
    }

    static boolean run(ServerPlayer p, Watch w, Profile pr, Run r, long now) {
        if (p.level().dimension() != Level.OVERWORLD || !p.isAlive()) return false;
        ServerLevel lvl = p.serverLevel();
        switch (r.ch) {
            case STEPS -> {
                if (now > r.until) return false;
                if (w.stepped) {
                    Vec3 at = behind(w, p, 6, 10);
                    if (at != null) {
                        int delay = 3 + p.getRandom().nextInt(3);
                        Fx.later(lvl, delay, () -> step(p, at, 0.42f));
                        r.steps++;
                    }
                }
                if (w.stillTicks == 3 && r.steps > 3 && !r.late) {
                    r.late = true;
                    Vec3 at = behind(w, p, 4, 6);
                    if (at != null) Fx.later(lvl, 5, () -> step(p, at, 0.5f));
                    return false;
                }
                return true;
            }
            case FRIEND -> {
                if (now < r.next) return true;
                if (r.steps >= 7) return false;
                double t = r.steps / 6.0;
                Vec3 at = r.from.lerp(r.to, t);
                step(p, at, 0.35f + (float) t * 0.25f);
                r.steps++;
                r.next = now + 7 + p.getRandom().nextInt(3);
                return true;
            }
            case PICK -> {
                if (p.position().distanceTo(Vec3.atCenterOf(r.rock)) < 4) {
                    ROCK.put(p.getUUID(), new long[]{r.rock.asLong(), now + 1200});
                    return false;
                }
                if (now > r.until) return false;
                if (now < r.next) return true;
                BlockState st = lvl.getBlockState(r.rock);
                SoundType t = st.getSoundType(lvl, r.rock, null);
                Vec3 at = Vec3.atCenterOf(r.rock);
                if (r.hits < 4) {
                    Fx.sound(p, Holder.direct(t.getHitSound()), SoundSource.BLOCKS, at, 0.7f, t.getPitch() * 0.75f);
                    r.hits++;
                    r.next = now + 5;
                } else {
                    Fx.sound(p, Holder.direct(t.getBreakSound()), SoundSource.BLOCKS, at, 0.6f, t.getPitch() * 0.8f);
                    r.hits = 0;
                    if (--r.cycles <= 0) return false;
                    r.next = now + r.gap;
                }
                return true;
            }
            default -> {
                return false;
            }
        }
    }

    static void step(ServerPlayer p, Vec3 at, float vol) {
        ServerLevel lvl = p.serverLevel();
        BlockPos floor = BlockPos.containing(at.x, at.y - 0.4, at.z);
        BlockState st = lvl.getBlockState(floor);
        if (st.isAir()) {
            floor = floor.below();
            st = lvl.getBlockState(floor);
        }
        SoundType t = st.getSoundType(lvl, floor, null);
        Fx.sound(p, Holder.direct(t.getStepSound()), SoundSource.PLAYERS, at, t.getVolume() * vol, t.getPitch() * 0.97f);
    }

    static Vec3 behind(Watch w, ServerPlayer p, double lo, double hi) {
        Object[] cs = w.crumbs.toArray();
        for (int i = cs.length - 1; i >= 0; i--) {
            long[] c = (long[]) cs[i];
            BlockPos q = BlockPos.of(c[0]);
            double d = Math.sqrt(q.distSqr(p.blockPosition()));
            if (d >= lo && d <= hi) return Vec3.atBottomCenterOf(q);
            if (d > hi + 6) break;
        }
        return null;
    }

    static int rhythm(ServerPlayer p) {
        ArrayDeque<Long> d = DIGS.get(p.getUUID());
        if (d == null || d.size() < 4) return 14 + p.getRandom().nextInt(10);
        long sum = 0;
        int n = 0;
        Long prev = null;
        for (long t : d) {
            if (prev != null) {
                long g = t - prev;
                if (g > 2 && g < 80) {
                    sum += g;
                    n++;
                }
            }
            prev = t;
        }
        return n == 0 ? 16 : (int) Mth.clamp(sum / n, 6, 40);
    }

    static BlockPos rock(ServerPlayer p) {
        ServerLevel lvl = p.serverLevel();
        RandomSource r = p.getRandom();
        BlockPos eye = BlockPos.containing(p.getEyePosition());
        for (int t = 0; t < 30; t++) {
            Direction d = Direction.Plane.HORIZONTAL.getRandomDirection(r);
            int dist = 7 + r.nextInt(6);
            BlockPos q = eye.relative(d, dist).above(r.nextInt(3) - 1);
            boolean thick = true;
            for (int k = 0; k <= 3 && thick; k++) thick = lvl.getBlockState(q.relative(d.getOpposite(), k)).isSolidRender(lvl, q.relative(d.getOpposite(), k));
            if (!thick || !lvl.isLoaded(q)) continue;
            return q;
        }
        return null;
    }

    static boolean torch(ServerPlayer p, Watch w) {
        ServerLevel lvl = p.serverLevel();
        Vec3 f = Vec3.directionFromRotation(0, p.getYRot());
        for (int d = 10; d <= 20; d++) {
            Vec3 c = p.getEyePosition().add(f.scale(d));
            BlockPos q = BlockPos.containing(c);
            for (int dy = -1; dy <= 1; dy++) {
                BlockPos at = q.above(dy);
                if (!lvl.isLoaded(at) || !lvl.getBlockState(at).isAir() || lvl.getBrightness(LightLayer.BLOCK, at) > 6) continue;
                for (Direction side : Direction.Plane.HORIZONTAL) {
                    BlockPos wall = at.relative(side);
                    if (!lvl.getBlockState(wall).isFaceSturdy(lvl, wall, side.getOpposite())) continue;
                    BlockState torch = Blocks.WALL_TORCH.defaultBlockState().setValue(WallTorchBlock.FACING, side.getOpposite());
                    if (Ghosts.show(p, at, torch, 1200, 7)) return true;
                }
            }
        }
        return false;
    }

    static boolean dark(ServerPlayer p, Profile pr) {
        ServerLevel lvl = p.serverLevel();
        List<Long> ts = new ArrayList<>(pr.placedTorches);
        java.util.Collections.shuffle(ts, new java.util.Random(p.getRandom().nextLong()));
        for (long l : ts) {
            BlockPos q = BlockPos.of(l);
            double d = Math.sqrt(q.distSqr(p.blockPosition()));
            if (d < 9 || d > 28 || !lvl.isLoaded(q)) continue;
            BlockState st = lvl.getBlockState(q);
            if (!(st.getBlock() instanceof TorchBlock) && !(st.getBlock() instanceof WallTorchBlock)) continue;
            if (Ghosts.show(p, q, Blocks.AIR.defaultBlockState(), 900, 5)) return true;
        }
        return false;
    }

    private Echo() {}
}
