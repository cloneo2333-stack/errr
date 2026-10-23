package vn.cavemind.director;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import vn.cavemind.CaveMind;
import vn.cavemind.Cfg;
import vn.cavemind.compat.Compat;
import vn.cavemind.sense.Group;
import vn.cavemind.sense.Watch;

public final class Tension {
    public enum Phase { BUILD_UP, PEAK, FADE, RELEASE }

    public enum Permit { AWAY, WITHDRAW, APPROACH, ENGAGE }

    public record Hint(Vec3 center, double radius) {}

    public static final class Mind {
        public double tension, menace, peakAt = 70;
        public Phase phase = Phase.BUILD_UP;
        public long phaseAt, lastStress = Long.MIN_VALUE / 4, lastPerceived, releaseUntil, boredAt, peakHold, heardAt = Long.MIN_VALUE / 4;
        public long seenAt = Long.MIN_VALUE / 4;
        public boolean dry, chased;
        public int cycles;
        public Hint hint;
        public final ArrayDeque<String> log = new ArrayDeque<>();
    }

    private static final Map<UUID, Mind> MINDS = new HashMap<>();
    static final double MENACE_CAP = 30;

    public static Mind mind(ServerPlayer p) {
        return MINDS.computeIfAbsent(p.getUUID(), k -> fresh(p));
    }

    private static Mind fresh(ServerPlayer p) {
        Mind m = new Mind();
        long now = p.server.getTickCount();
        m.phaseAt = now;
        m.lastPerceived = now;
        cycle(m, p.getRandom(), now);
        return m;
    }

    public static void forget(UUID id) {
        MINDS.remove(id);
    }

    public static void clear() {
        MINDS.clear();
    }

    public static boolean on() {
        return Cfg.DIRECTOR.get();
    }

    private static void cycle(Mind m, RandomSource r, long now) {
        double lo = Cfg.PEAK_MIN.get(), hi = Math.max(lo, Cfg.PEAK_MAX.get());
        m.peakAt = lo + r.nextDouble() * (hi - lo);
        m.dry = r.nextDouble() < Cfg.DRY_CHANCE.get();
        double a = Cfg.HINT_MIN.get(), b = Math.max(a, Cfg.HINT_MAX.get());
        m.boredAt = now + (long) ((a + r.nextDouble() * (b - a)) * 1200 / Math.max(0.25, Cfg.INTENSITY.get()));
        m.hint = null;
        m.cycles++;
    }

    public static void stress(ServerPlayer p, double amount, String why) {
        if (amount == 0) return;
        Mind m = mind(p);
        long now = p.server.getTickCount();
        m.tension = Mth.clamp(m.tension + amount, 0, 100);
        if (amount > 0) m.lastStress = now;
        note(m, now, why + " " + (amount > 0 ? "+" : "") + Math.round(amount));
    }

    public static void seen(ServerPlayer p, double dist, String what) {
        Mind m = mind(p);
        long now = p.server.getTickCount();
        perceived(p);
        if (now - m.seenAt < 200) {
            stress(p, 4, "thấy lại " + what);
            return;
        }
        m.seenAt = now;
        stress(p, Mth.clampedLerp(35, 18, (dist - 8) / 40.0), "thấy " + what);
    }

    public static void heard(ServerPlayer p, Vec3 at, float vol) {
        double d = p.getEyePosition().distanceTo(at);
        if (d > 24 * Math.max(0.3, Math.min(1.5, vol))) return;
        Mind m = mind(p);
        long now = p.server.getTickCount();
        if (now - m.heardAt < 60) return;
        m.heardAt = now;
        stress(p, Mth.clampedLerp(10, 4, d / 24.0), "nghe");
    }

    public static void chased(ServerPlayer p) {
        Mind m = mind(p);
        m.chased = true;
        perceived(p);
        m.tension = Math.min(100, m.tension + 2);
        m.lastStress = p.server.getTickCount();
    }

    public static void perceived(ServerPlayer p) {
        Mind m = mind(p);
        m.lastPerceived = p.server.getTickCount();
        m.hint = null;
    }

    public static void climax(ServerPlayer p) {
        Mind m = mind(p);
        if (m.phase == Phase.BUILD_UP || m.phase == Phase.PEAK) to(p, m, Phase.FADE, p.server.getTickCount(), "cao trào đã xảy ra");
    }

    public static void died(ServerPlayer p) {
        Mind m = mind(p);
        long now = p.server.getTickCount();
        m.tension *= 0.75;
        if (m.phase == Phase.PEAK) to(p, m, Phase.FADE, now, "chết");
    }

    static void note(Mind m, long now, String s) {
        m.log.addLast((now / 20) + "s " + s);
        while (m.log.size() > 12) m.log.removeFirst();
    }

    private static void to(ServerPlayer p, Mind m, Phase ph, long now, String why) {
        Phase old = m.phase;
        m.phase = ph;
        m.phaseAt = now;
        RandomSource r = p.getRandom();
        switch (ph) {
            case PEAK -> {
                double a = Cfg.PEAK_HOLD_MIN.get(), b = Math.max(a, Cfg.PEAK_HOLD_MAX.get());
                m.peakHold = (long) ((a + r.nextDouble() * (b - a)) * 20);
            }
            case RELEASE -> {
                int a = Cfg.RELAX_MIN.get(), b = Math.max(a, Cfg.RELAX_MAX.get());
                m.releaseUntil = now + (long) ((a + r.nextDouble() * (b - a)) * 1200);
            }
            case BUILD_UP -> cycle(m, r, now);
            default -> {}
        }
        note(m, now, old + " -> " + ph + " (" + why + ")");
        PaceLog.phase(p, old, ph, m.tension, why);
        CaveMind.debug("tension {}: {} -> {} ({}, {})", p.getGameProfile().getName(), old, ph, why, Math.round(m.tension));
    }

    public static void tick(ServerPlayer p, Watch w, long now) {
        if (now % 20 != 0) return;
        Mind m = mind(p);
        if (w.idle > 2400 || p.isSleeping()) {
            m.phaseAt += 20;
            m.releaseUntil += 20;
            m.lastPerceived += 20;
            m.boredAt += 20;
            return;
        }
        ServerLevel lvl = p.serverLevel();
        boolean over = lvl.dimension() == Level.OVERWORLD;
        if (!over) m.tension = Math.max(0, m.tension - 2);
        else {
            boolean alone = Group.isolation(p) > 40;
            if (w.light <= 4 && alone) m.tension = Math.min(100, m.tension + 0.05 * Cfg.INTENSITY.get());
            if (w.indoors && w.light >= 9 && !alone) m.tension = Math.max(0, m.tension - 0.1);
        }
        if (now - m.lastStress > 100) m.tension = Math.max(0, m.tension - (m.phase == Phase.RELEASE ? 2.0 : 1.0));
        double near = over ? threats(p) : 0;
        m.menace = m.menace * 0.95 + near;
        boolean chased = m.chased;
        m.chased = false;
        long in = now - m.phaseAt;
        switch (m.phase) {
            case BUILD_UP -> {
                if (m.tension >= m.peakAt) to(p, m, Phase.PEAK, now, "thang chạm đỉnh " + Math.round(m.peakAt));
                else if (m.menace > MENACE_CAP * 1.4) to(p, m, Phase.FADE, now, "sinh vật bám quá lâu");
            }
            case PEAK -> {
                long hold = chased ? Math.max(m.peakHold, Math.min(1800, in + 40)) : m.peakHold;
                if (in > hold || m.menace > MENACE_CAP && !chased || in > 1800) to(p, m, Phase.FADE, now, in > hold ? "giữ đỉnh đủ lâu" : "mối đe dọa vượt trần");
            }
            case FADE -> {
                boolean clear = m.tension < 35 && near == 0 && !chased;
                if (clear || in > 1800) to(p, m, Phase.RELEASE, now, clear ? "đã nguội" : "nhạt dần quá lâu");
            }
            case RELEASE -> {
                if (now >= m.releaseUntil && m.tension < 20) to(p, m, Phase.BUILD_UP, now, "hết khoảng lặng");
            }
        }
        if (now % 600 == 0) PaceLog.sample(p, m);
    }

    private static double threats(ServerPlayer p) {
        double sum = 0;
        for (Mob e : p.serverLevel().getEntitiesOfClass(Mob.class, p.getBoundingBox().inflate(48, 24, 48), Compat::managed)) {
            String role = Compat.role(e);
            if (role.startsWith("effigy")) continue;
            double d = e.distanceTo(p);
            if (d > 48) continue;
            double wgt = role.equals("man_hunt") || role.equals("damned_hunt") ? 2.0 : 1.0;
            if (d < 32) sum += wgt;
            else sum += 0.25;
        }
        Vec3 man = vn.cavemind.life.Life.where(p.server);
        if (man != null && !vn.cavemind.man.ManMind.anyActive() && man.distanceToSqr(p.position()) < 48 * 48) sum += 0.3;
        return sum;
    }

    public static Phase phase(ServerPlayer p) {
        return mind(p).phase;
    }

    public static double value(ServerPlayer p) {
        return mind(p).tension;
    }

    public static Permit permit(ServerPlayer p) {
        String why = limit(p);
        Permit base = basePermit(p);
        if (why == null) return base;
        Permit cap = why.startsWith("!") ? Permit.AWAY : Permit.APPROACH;
        return base.ordinal() > cap.ordinal() ? cap : base;
    }

    public static Permit basePermit(ServerPlayer p) {
        if (!on()) return Permit.ENGAGE;
        return switch (mind(p).phase) {
            case BUILD_UP -> Permit.APPROACH;
            case PEAK -> Permit.ENGAGE;
            case FADE -> Permit.WITHDRAW;
            case RELEASE -> Permit.AWAY;
        };
    }

    public static String limit(ServerPlayer p) {
        if (!p.isAlive() || Compat.exempt(p)) return "!không chơi";
        if (p.level().dimension() != Level.OVERWORLD) return "!ở chiều khác";
        Director.State s = Director.state(p);
        long now = p.server.getTickCount();
        if (Cfg.MERCY.get()) {
            if (now < s.mercyUntil) return "!vừa chết";
            long joined = now - s.joinedAt;
            if (joined < 600) return "!vừa vào game";
            if (Watch.of(p).idle > 2400) return "!đang treo máy";
            if (p.serverLevel().getRaidAt(p.blockPosition()) != null) return "!đang có raid";
            if (joined < 1800) return "vừa vào game";
            if (p.isOnFire() || p.isInLava() || p.getAirSupply() < p.getMaxAirSupply() / 2) return "đang gặp nguy";
            if (p.containerMenu != p.inventoryMenu) return "đang mở giao diện";
            if (p.isPassenger() || p.isFallFlying()) return "đang cưỡi hoặc bay";
        }
        if (p.serverLevel().getDifficulty() == net.minecraft.world.Difficulty.PEACEFUL) return "độ khó Hòa bình";
        return null;
    }

    public static boolean allows(ServerPlayer p, Permit need) {
        return permit(p).ordinal() >= need.ordinal();
    }

    public static Hint hint(ServerPlayer p) {
        Mind m = mind(p);
        long now = p.server.getTickCount();
        if (m.phase != Phase.BUILD_UP || m.dry || !allows(p, Permit.APPROACH)) return null;
        long since = Math.max(m.lastPerceived, m.phaseAt);
        if (now < m.boredAt || now - since < 2400) return null;
        double k = Mth.clamp((now - m.boredAt) / 7200.0, 0, 1);
        double radius = Mth.lerp(k, 160, 40);
        if (m.hint == null || m.hint.radius() - radius > 20) {
            RandomSource r = p.getRandom();
            double a = r.nextDouble() * Math.PI * 2, off = radius * (0.35 + r.nextDouble() * 0.5);
            Vec3 c = p.position().add(Math.cos(a) * off, 0, Math.sin(a) * off);
            m.hint = new Hint(c, radius);
            note(m, now, "gợi ý vùng r=" + Math.round(radius));
        }
        return m.hint;
    }

    public static String describe(ServerPlayer p) {
        Mind m = mind(p);
        long now = p.server.getTickCount();
        String lim = limit(p);
        StringBuilder sb = new StringBuilder();
        sb.append("pha ").append(vi(m.phase)).append(", thang ").append(Math.round(m.tension)).append("/").append(Math.round(m.peakAt))
            .append(", quyền ").append(vi(permit(p))).append(lim != null ? " (" + lim.replace("!", "") + ")" : "")
            .append(", đe dọa ").append(String.format(java.util.Locale.ROOT, "%.1f", m.menace))
            .append(", trong pha ").append((now - m.phaseAt) / 20).append("s");
        if (m.phase == Phase.RELEASE) sb.append(", lặng còn ").append(Math.max(0, (m.releaseUntil - now) / 20)).append("s");
        if (m.phase == Phase.BUILD_UP) {
            sb.append(m.dry ? ", vòng khô" : ", gợi ý sau " + Math.max(0, (m.boredAt - now) / 20) + "s");
            if (m.hint != null) sb.append(", vùng gợi ý r=").append(Math.round(m.hint.radius()));
        }
        return sb.toString();
    }

    public static String vi(Phase ph) {
        return switch (ph) {
            case BUILD_UP -> "Dồn nén";
            case PEAK -> "Đỉnh";
            case FADE -> "Nhạt dần";
            case RELEASE -> "Khoảng lặng";
        };
    }

    public static String vi(Permit pm) {
        return switch (pm) {
            case AWAY -> "đi xa";
            case WITHDRAW -> "phải lùi";
            case APPROACH -> "được tiến";
            case ENGAGE -> "được tấn công";
        };
    }

    public static boolean busyNear(ServerPlayer p, Entity e) {
        return e != null && e.distanceToSqr(p) < 48 * 48;
    }

    private Tension() {}
}
