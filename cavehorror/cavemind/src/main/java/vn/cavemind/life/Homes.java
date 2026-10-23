package vn.cavemind.life;

import java.util.Comparator;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import vn.cavemind.CaveMind;
import vn.cavemind.director.Dread;
import vn.cavemind.memory.MindData;
import vn.cavemind.memory.Profile;
import vn.cavemind.sense.Watch;

public final class Homes {
    static final int MERGE = 24, KEEP = 6;
    static final double NIGHT = 4.0, SETTLE = 6.0, SWITCH = 1.4;

    static Profile.Home near(Profile pr, BlockPos pos, int r) {
        Profile.Home best = null;
        double bd = (double) r * r;
        for (Profile.Home h : pr.homes) {
            double d = h.pos.distSqr(pos);
            if (d <= bd) {
                bd = d;
                best = h;
            }
        }
        return best;
    }

    static Profile.Home add(Profile pr, BlockPos pos) {
        Profile.Home h = near(pr, pos, MERGE);
        if (h != null) return h;
        h = new Profile.Home();
        h.pos = pos.immutable();
        pr.homes.add(h);
        if (pr.homes.size() > KEEP) {
            pr.homes.sort(Comparator.comparingDouble(o -> -o.score));
            Profile.Home cur = pr.home == null ? null : near(pr, pr.home, MERGE);
            while (pr.homes.size() > KEEP) {
                for (int i = pr.homes.size() - 1; i >= 0; i--) {
                    Profile.Home o = pr.homes.get(i);
                    if (o != cur && o != h) {
                        pr.homes.remove(i);
                        break;
                    }
                }
            }
        }
        return h;
    }

    static void pull(Profile.Home h, BlockPos pos, double w) {
        double k = w / Math.max(w, h.score + w);
        h.pos = new BlockPos((int) Math.round(h.pos.getX() + (pos.getX() - h.pos.getX()) * k), (int) Math.round(h.pos.getY() + (pos.getY() - h.pos.getY()) * k),
            (int) Math.round(h.pos.getZ() + (pos.getZ() - h.pos.getZ()) * k));
    }

    public static void seed(Profile pr) {
        if (!pr.homes.isEmpty() || pr.home == null) return;
        Profile.Home h = new Profile.Home();
        h.pos = pr.home;
        h.bed = pr.home;
        h.score = SETTLE;
        h.nights = 2;
        pr.homes.add(h);
    }

    public static void slept(ServerPlayer p, BlockPos bed) {
        if (p.level().dimension() != Level.OVERWORLD || bed == null) return;
        Profile pr = MindData.profile(p);
        seed(pr);
        long day = Dread.day(p.serverLevel());
        Profile.Home h = add(pr, bed);
        if (h.lastNight != day) {
            h.score += NIGHT;
            h.nights++;
            h.lastNight = day;
        }
        h.bed = bed.immutable();
        pull(h, bed, NIGHT);
        choose(p, pr);
    }

    // Building and using chests count toward a home even for someone who never sleeps in a bed.
    public static void worked(ServerPlayer p, BlockPos at, double w) {
        if (p.level().dimension() != Level.OVERWORLD) return;
        Profile pr = MindData.profile(p);
        seed(pr);
        add(pr, at).score += w;
        choose(p, pr);
    }

    public static void tick(ServerPlayer p, Watch w, Profile pr, long now) {
        if (now % 200 != (p.getId() % 200) || p.level().dimension() != Level.OVERWORLD || p.isSpectator()) return;
        seed(pr);
        ServerLevel lvl = p.serverLevel();
        long day = Dread.day(lvl);
        if (pr.homeDay != day) {
            if (pr.homeDay >= 0) for (Profile.Home h : pr.homes) h.score *= Math.pow(0.88, Math.min(10, day - pr.homeDay));
            pr.homeDay = day;
            pr.homes.removeIf(h -> h.score < 0.3 && (pr.home == null || h.pos.distSqr(pr.home) > MERGE * MERGE));
        }
        // so does staying a long time under a roof or by your own light
        if (w.indoors || w.ambient >= 8 && !w.outside) {
            Profile.Home h = near(pr, p.blockPosition(), 16);
            if (h == null) h = add(pr, p.blockPosition());
            h.score += 0.06;
        }
        choose(p, pr);
    }

    static void choose(ServerPlayer p, Profile pr) {
        Profile.Home best = null;
        for (Profile.Home h : pr.homes) if (best == null || h.score > best.score) best = h;
        if (best == null) return;
        Profile.Home cur = pr.home == null ? null : near(pr, pr.home, MERGE);
        BlockPos anchor = best.bed != null ? best.bed : best.pos;
        if (cur == null) {
            if (best.score >= NIGHT) set(p, pr, anchor, "định cư");
            return;
        }
        if (cur == best) {
            if (cur.bed != null && !cur.bed.equals(pr.home)) pr.home = cur.bed;
            return;
        }
        if (best.score > cur.score * SWITCH + 2 && (best.nights >= 2 || cur.nights == 0 && best.score >= SETTLE * 2)) set(p, pr, anchor, "dọn nhà");
    }

    static void set(ServerPlayer p, Profile pr, BlockPos at, String why) {
        BlockPos old = pr.home;
        pr.home = at.immutable();
        MindData.dirty(p.server);
        CaveMind.debug("{}'s home {} -> {} ({})", p.getGameProfile().getName(), old, at, why);
    }

    public static String describe(Profile pr) {
        StringBuilder sb = new StringBuilder("nhà " + (pr.home == null ? "chưa rõ" : pr.home.toShortString()));
        for (Profile.Home h : pr.homes) sb.append(" | ").append(h.pos.toShortString()).append(" ").append(Math.round(h.score * 10) / 10.0).append("đ ").append(h.nights).append(" đêm");
        return sb.toString();
    }

    private Homes() {}
}
