package vn.cavemind.day;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;
import vn.cavemind.director.Dread;
import vn.cavemind.memory.Moment;
import vn.cavemind.memory.Profile;
import vn.cavemind.sense.Sight;
import vn.cavemind.sense.Watch;
import vn.cavemind.world.Fx;

public final class Echo extends Piece {
    record Rec(long t, BlockPos pos, SoundEvent hit, SoundEvent sound, float vol, float pitch) {}

    private static final Map<UUID, Deque<Rec>> LOG = new HashMap<>();

    public static void record(ServerPlayer p, BlockPos pos, SoundEvent hit, SoundEvent sound, float vol, float pitch) {
        Deque<Rec> d = LOG.computeIfAbsent(p.getUUID(), k -> new ArrayDeque<>());
        d.addLast(new Rec(p.server.getTickCount(), pos.immutable(), hit, sound, vol, pitch));
        while (d.size() > 96) d.removeFirst();
    }

    public static void forget(UUID id) {
        LOG.remove(id);
    }

    public static void clear() {
        LOG.clear();
    }

    static boolean ready(ServerPlayer p) {
        return pick(p, false) != null;
    }

    static List<Rec> pick(ServerPlayer p, boolean strict) {
        Deque<Rec> d = LOG.get(p.getUUID());
        if (d == null || d.size() < 6) return null;
        long now = p.server.getTickCount();
        List<Rec> all = new ArrayList<>(d);
        for (int i = all.size() - 1; i >= 5; i--) {
            Rec last = all.get(i);
            if (now - last.t() > 36000) break;
            List<Rec> run = new ArrayList<>();
            run.add(last);
            for (int j = i - 1; j >= 0 && run.size() < 12; j--) {
                Rec r = all.get(j);
                if (run.get(0).t() - r.t() > 400 || r.pos().distSqr(last.pos()) > 14 * 14) break;
                run.add(0, r);
            }
            if (run.size() < 6) continue;
            double d2 = Vec3.atCenterOf(last.pos()).distanceTo(p.position());
            if (d2 < 20 || d2 > 70) continue;
            return run;
        }
        return null;
    }

    @Override
    String id() {
        return "echo";
    }

    @Override
    String vi() {
        return "tiếng vọng của chính bạn (ai đó lặp lại đúng việc bạn đã làm, đúng chỗ)";
    }

    @Override
    boolean claims() {
        return false;
    }

    @Override
    boolean begin(ServerPlayer p, Watch w, Profile pr, long now) {
        List<Rec> run = pick(p, true);
        if (run == null) return false;
        ServerLevel lvl = p.serverLevel();
        Vec3 c = Vec3.atCenterOf(run.get(run.size() - 1).pos());
        if (Sight.of(p, w, c).on && Sight.clear(lvl, p.getEyePosition(), c, p)) return false;
        long t0 = run.get(0).t();
        long acc = 20;
        long prev = t0;
        for (Rec r : run) {
            acc += Math.min(60, Math.max(4, r.t() - prev));
            prev = r.t();
            Vec3 at = Vec3.atCenterOf(r.pos());
            if (r.hit() != null) {
                for (int k = 0; k < 4; k++) {
                    long when = acc - 16 + k * 4;
                    Holder<SoundEvent> h = Holder.direct(r.hit());
                    Fx.later(lvl, (int) Math.max(1, when), () -> Fx.sound(p, h, SoundSource.BLOCKS, at, r.vol() * 0.5f, r.pitch() * 0.8f));
                }
            }
            Holder<SoundEvent> s = Holder.direct(r.sound());
            Fx.later(lvl, (int) acc, () -> Fx.sound(p, s, SoundSource.BLOCKS, at, r.vol() * 0.85f, r.pitch()));
        }
        pr.moment(new Moment(Moment.ECHOED, Dread.day(lvl), Dread.time(lvl), run.get(0).pos(), run.size(), 0));
        return true;
    }

    @Override
    boolean tick(ServerPlayer p, Watch w, Profile pr, long now) {
        return false;
    }
}
