package vn.cavemind.director;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.registries.ForgeRegistries;
import vn.cavemind.CaveMind;
import vn.cavemind.Cfg;
import vn.cavemind.compat.Compat;
import vn.cavemind.memory.MindData;
import vn.cavemind.memory.Moment;
import vn.cavemind.memory.Profile;
import vn.cavemind.sense.Sight;
import vn.cavemind.sense.Watch;
import vn.cavemind.world.Fx;

public final class Dread {
    private static final class Run {
        String kind;
        long until;
        Vec3 at;
        int step, cycles;
        long next;
        int extra;
        BlockState block;
    }

    private static final Map<UUID, Run> RUNS = new HashMap<>();
    private static final Map<UUID, Long> NEXT = new HashMap<>();
    private static final Map<UUID, Long> NEXT_BREATH = new HashMap<>();

    public static void forget(UUID id) {
        RUNS.remove(id);
        NEXT.remove(id);
        NEXT_BREATH.remove(id);
    }

    public static void clear() {
        RUNS.clear();
        NEXT.clear();
        NEXT_BREATH.clear();
    }

    public static void tick(ServerPlayer p, Watch w, long now) {
        if (!Cfg.CAVE_DREAD.get()) return;
        Run r = RUNS.get(p.getUUID());
        if (r != null) {
            if (now > r.until || !run(p, w, r, now)) RUNS.remove(p.getUUID());
            return;
        }
        if (now % 20 != 0) return;
        long next = NEXT.computeIfAbsent(p.getUUID(), k -> now + (vn.cavemind.dev.Dev.on() ? 600 : 2400 + p.getRandom().nextInt(3600)));
        if (now < next) return;
        if (!Director.free(p)) return;
        double t = Director.tension(p);
        RandomSource rnd = p.getRandom();
        if (!vn.cavemind.dev.Dev.on() && rnd.nextDouble() > 0.15 + t / 140.0) {
            NEXT.put(p.getUUID(), now + 400 + rnd.nextInt(600));
            return;
        }
        Run n = pick(p, w, rnd, now);
        if (n != null) {
            RUNS.put(p.getUUID(), n);
            Director.scare(p, 6);
            CaveMind.debug("dread {} for {}", n.kind, p.getGameProfile().getName());
            vn.cavemind.dev.Dev.say(p, "Sự kiện rợn: " + vi(n.kind));
        }
        NEXT.put(p.getUUID(), now + (vn.cavemind.dev.Dev.on() ? 1200 + rnd.nextInt(1200) : (long) ((3000 + rnd.nextInt(5400)) / Cfg.INTENSITY.get())));
    }

    public static void force(ServerPlayer p, String kind) {
        Watch w = Watch.of(p);
        Run n = make(p, w, kind, p.getRandom(), p.server.getTickCount());
        if (n != null) RUNS.put(p.getUUID(), n);
        vn.cavemind.dev.Dev.say(p, n != null ? "Sự kiện rợn: " + vi(kind) : "Sự kiện " + kind + " không chạy được ở đây (cần hang đá / đuốc đã đặt / thú quanh bạn...)");
    }

    static String vi(String kind) {
        return switch (kind) {
            case "dig" -> "tiếng ai đó đào trong đá, gần dần";
            case "knock" -> "tiếng gõ từ bên trong vách đá";
            case "steps" -> "tiếng bước chân vọng sau lưng, dừng khi bạn dừng";
            case "torch" -> "một ngọn đuốc bạn đặt phía sau đã biến mất";
            case "breath" -> "tiếng thở ngay sau gáy";
            case "gravel" -> "sỏi đá rơi trên đầu";
            case "whisper" -> "tiếng thì thầm trong vách";
            case "snap" -> "tiếng cành gãy trong rừng sau lưng";
            case "stare" -> "thú vật quanh bạn đứng im nhìn bạn";
            default -> kind;
        };
    }

    private static Run pick(ServerPlayer p, Watch w, RandomSource rnd, long now) {
        if (w.underground) {
            long nb = NEXT_BREATH.getOrDefault(p.getUUID(), 0L);
            if (w.stillTicks > 160 && w.light <= 3 && now > nb && rnd.nextFloat() < 0.5f) {
                NEXT_BREATH.put(p.getUUID(), now + 72000 + rnd.nextInt(72000));
                return make(p, w, "breath", rnd, now);
            }
            String[] k = {"dig", "dig", "steps", "steps", "torch", "knock", "gravel", "whisper"};
            for (int i = 0; i < 4; i++) {
                Run r = make(p, w, k[rnd.nextInt(k.length)], rnd, now);
                if (r != null) return r;
            }
            return null;
        }
        if (p.serverLevel().isNight() && w.outside) {
            String[] k = {"snap", "stare", "steps"};
            return make(p, w, k[rnd.nextInt(k.length)], rnd, now);
        }
        return null;
    }

    private static Run make(ServerPlayer p, Watch w, String kind, RandomSource rnd, long now) {
        ServerLevel lvl = p.serverLevel();
        Run r = new Run();
        r.kind = kind;
        r.next = now + 10;
        switch (kind) {
            case "dig", "knock" -> {
                Vec3 at = inRock(p, rnd, 11, 19);
                if (at == null) return null;
                r.at = at;
                r.block = lvl.getBlockState(BlockPos.containing(at));
                r.cycles = kind.equals("dig") ? 3 + rnd.nextInt(3) : 1 + rnd.nextInt(2);
                r.until = now + 2400;
            }
            case "steps" -> {
                r.until = now + 240 + rnd.nextInt(200);
                r.extra = 0;
            }
            case "torch" -> {
                if (!Cfg.TORCH_THEFT.get() || !stealTorch(p, w, 34)) return null;
                r.until = now;
            }
            case "breath" -> {
                Vec3 back = p.getEyePosition().add(Vec3.directionFromRotation(0, p.getYRot()).scale(-1.3));
                Fx.sound(p, Compat.sound("apollyon:shallow_breathing", "minecraft:entity.warden.nearby_close"), SoundSource.HOSTILE, back, 0.45f, 0.95f);
                MindData.profile(p).moment(new Moment(Moment.UNSEEN, day(lvl), time(lvl), p.blockPosition(), 1, 0));
                r.until = now;
            }
            case "gravel" -> {
                BlockPos up = p.blockPosition().above(3 + rnd.nextInt(3));
                Fx.sound(p, Holder.direct(SoundEvents.GRAVEL_BREAK), SoundSource.BLOCKS, Vec3.atCenterOf(up), 0.5f, 0.6f);
                lvl.sendParticles(p, new BlockParticleOption(ParticleTypes.FALLING_DUST, Blocks.GRAVEL.defaultBlockState()), false,
                    p.getX(), p.getY() + 3, p.getZ(), 18, 1.5, 0.5, 1.5, 0);
                r.until = now;
            }
            case "whisper" -> {
                Vec3 at = inRock(p, rnd, 2, 4);
                if (at == null) return null;
                Fx.sound(p, Compat.sound(rnd.nextBoolean() ? "man:short_whisper_1" : "man:short_whisper_2", "minecraft:ambient.cave"),
                    SoundSource.AMBIENT, at, 0.28f, 0.9f);
                r.until = now;
            }
            case "snap" -> {
                Vec3 back = p.position().add(Vec3.directionFromRotation(0, p.getYRot() + 150 + rnd.nextInt(60)).scale(14 + rnd.nextInt(10)));
                Fx.sound(p, Holder.direct(SoundEvents.BAMBOO_BREAK), SoundSource.AMBIENT, back, 0.7f, 0.55f);
                r.until = now;
            }
            case "stare" -> {
                int n = Fx.stare(p, p, null, 120 + rnd.nextInt(80), 26);
                if (n < 2) return null;
                r.until = now;
            }
            default -> {
                return null;
            }
        }
        return r;
    }

    private static boolean run(ServerPlayer p, Watch w, Run r, long now) {
        ServerLevel lvl = p.serverLevel();
        switch (r.kind) {
            case "dig", "knock" -> {
                if (p.position().distanceTo(r.at) < 6) return false;
                if (now < r.next) return true;
                SoundType st = r.block.getSoundType(lvl, BlockPos.containing(r.at), null);
                boolean knock = r.kind.equals("knock");
                int hits = knock ? 3 : 6;
                if (r.step < hits) {
                    Fx.sound(p, Holder.direct(st.getHitSound()), SoundSource.BLOCKS, r.at, knock ? 0.9f : 0.75f, (knock ? 0.55f : 0.5f) + p.getRandom().nextFloat() * 0.1f);
                    r.step++;
                    r.next = now + (knock ? 14 : 5 + p.getRandom().nextInt(3));
                } else {
                    if (!knock) Fx.sound(p, Holder.direct(st.getBreakSound()), SoundSource.BLOCKS, r.at, 0.8f, 0.7f);
                    r.step = 0;
                    if (--r.cycles <= 0) {
                        MindData.profile(p).moment(new Moment(Moment.LISTENED, day(lvl), time(lvl), p.blockPosition(), (int) p.position().distanceTo(r.at), 0));
                        return false;
                    }
                    Vec3 toward = p.position().subtract(r.at).normalize();
                    Vec3 nx = r.at.add(toward.scale(1.2));
                    if (lvl.getBlockState(BlockPos.containing(nx)).isSolidRender(lvl, BlockPos.containing(nx))) r.at = nx;
                    r.next = now + 40 + p.getRandom().nextInt(50);
                }
                return true;
            }
            case "steps" -> {
                if (w.stepped) {
                    echo(p, w, 4 + p.getRandom().nextInt(4));
                    r.extra = 1 + p.getRandom().nextInt(2);
                } else if (w.stillTicks == 4 && r.extra > 0) {
                    echo(p, w, 3);
                    r.extra--;
                    if (r.extra == 0 && p.getRandom().nextFloat() < 0.5f) return false;
                }
                return true;
            }
            default -> {
                return false;
            }
        }
    }

    public static void echo(ServerPlayer p, Watch w, int delay) {
        ServerLevel lvl = p.serverLevel();
        Vec3 dir = w.vel.lengthSqr() > 1e-4 ? new Vec3(w.vel.x, 0, w.vel.z).normalize() : Vec3.directionFromRotation(0, p.getYRot());
        double back = 11 + p.getRandom().nextDouble() * 4;
        Vec3 at = p.position().subtract(dir.scale(back));
        BlockPos floor = BlockPos.containing(at.x, p.getY() - 0.5, at.z);
        BlockState st = lvl.getBlockState(floor);
        if (st.isAir()) st = lvl.getBlockState(p.blockPosition().below());
        SoundType type = st.getSoundType(lvl, floor, null);
        Holder<SoundEvent> s = Holder.direct(type.getStepSound());
        Vec3 fin = at;
        Fx.later(lvl, delay, () -> Fx.sound(p, s, SoundSource.HOSTILE, fin, type.getVolume() * 0.55f, type.getPitch() * 0.92f));
    }

    private static Vec3 inRock(ServerPlayer p, RandomSource rnd, int a, int b) {
        ServerLevel lvl = p.serverLevel();
        for (int i = 0; i < 20; i++) {
            double ang = rnd.nextDouble() * Math.PI * 2;
            double d = a + rnd.nextDouble() * (b - a);
            BlockPos q = BlockPos.containing(p.getX() + Math.cos(ang) * d, p.getY() + rnd.nextInt(5) - 1, p.getZ() + Math.sin(ang) * d);
            BlockState s = lvl.getBlockState(q);
            if (s.isSolidRender(lvl, q) && (s.is(BlockTags.BASE_STONE_OVERWORLD) || s.is(BlockTags.DEEPSLATE_ORE_REPLACEABLES) || s.is(BlockTags.STONE_ORE_REPLACEABLES))) {
                return Vec3.atCenterOf(q);
            }
        }
        return null;
    }

    public static boolean stealTorch(ServerPlayer p, Watch w, double minDist) {
        Profile pr = MindData.profile(p);
        ServerLevel lvl = p.serverLevel();
        Iterator<Long> it = pr.placedTorches.descendingIterator();
        int scanned = 0;
        while (it.hasNext() && scanned++ < 120) {
            BlockPos q = BlockPos.of(it.next());
            if (!lvl.isLoaded(q)) continue;
            BlockState s = lvl.getBlockState(q);
            var key = ForgeRegistries.BLOCKS.getKey(s.getBlock());
            if (key == null || !key.getPath().contains("torch")) {
                it.remove();
                continue;
            }
            Vec3 c = Vec3.atCenterOf(q);
            double d = c.distanceTo(p.getEyePosition());
            if (d < minDist || d > 90) continue;
            Sight si = Sight.of(p, w, c);
            if (si.on && si.edge() < 1.15) continue;
            if (vn.cavemind.sense.Group.seenByOthers(p, c, 96)) continue;
            if (Sight.clear(lvl, p.getEyePosition(), c, p) && si.on) continue;
            lvl.removeBlock(q, false);
            it.remove();
            pr.dStolen++;
            pr.moment(new Moment(Moment.TOOK, day(lvl), time(lvl), q, (int) d, 0));
            Fx.sound(p, Holder.direct(SoundEvents.FIRE_EXTINGUISH), SoundSource.BLOCKS, c, 0.35f, 1.4f);
            CaveMind.debug("took a torch at {} from {}", q, p.getGameProfile().getName());
            return true;
        }
        return false;
    }

    public static long day(ServerLevel lvl) {
        return lvl.getDayTime() / 24000L;
    }

    public static int time(ServerLevel lvl) {
        return (int) (lvl.getDayTime() % 24000L);
    }

    static int mth(double v) {
        return Mth.floor(v);
    }

    private Dread() {}
}
