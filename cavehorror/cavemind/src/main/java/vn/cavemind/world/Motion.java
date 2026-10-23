package vn.cavemind.world;

import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.Vec3;
import vn.cavemind.compat.Compat;

public final class Motion {
    private static final class S {
        String anim = "";
        long animEnd;
        float yaw = Float.NaN;
        long pauseUntil, nextPause;
        long nextPath;
        double phase;
        Vec3 last;
    }

    private static final Map<Entity, S> STATE = new WeakHashMap<>();

    private static S s(Entity e) {
        return STATE.computeIfAbsent(e, k -> new S());
    }

    public static void play(Entity e, String name, int ticks, long now) {
        S st = s(e);
        st.anim = name;
        st.animEnd = now + ticks;
        Compat.anim(e, name);
    }

    public static void stop(Entity e) {
        S st = s(e);
        if (!st.anim.isEmpty()) Compat.anim(e, "empty");
        st.anim = "";
    }

    public static boolean paused(Entity e, long now) {
        return now < s(e).pauseUntil;
    }

    public static void turn(Mob m, Vec3 at, float rate) {
        S st = s(m);
        float want = (float) (Mth.atan2(at.z - m.getZ(), at.x - m.getX()) * (180f / Math.PI)) - 90f;
        if (Float.isNaN(st.yaw)) st.yaw = m.getYRot();
        st.yaw += Mth.clamp(Mth.wrapDegrees(want - st.yaw), -rate, rate);
        m.setYRot(st.yaw);
        m.setYBodyRot(st.yaw);
        m.setYHeadRot(st.yaw);
        m.getLookControl().setLookAt(at.x, at.y, at.z, rate, rate);
    }

    public static void pose(Entity e, String name, long now) {
        if (!s(e).anim.equals(name)) play(e, name, 1 << 30, now);
    }

    public static String current(Entity e) {
        return s(e).anim;
    }

    public static void snap(Mob m, Vec3 at) {
        snapYaw(m, (float) (Mth.atan2(at.z - m.getZ(), at.x - m.getX()) * (180f / Math.PI)) - 90f);
    }

    public static void snapYaw(Mob m, float yaw) {
        s(m).yaw = yaw;
        m.setYRot(yaw);
        m.yRotO = yaw;
        m.setYBodyRot(yaw);
        m.yBodyRotO = yaw;
        m.setYHeadRot(yaw);
        m.yHeadRotO = yaw;
    }

    public static boolean faced(Mob m, Vec3 at, float within) {
        float want = (float) (Mth.atan2(at.z - m.getZ(), at.x - m.getX()) * (180f / Math.PI)) - 90f;
        return Math.abs(Mth.wrapDegrees(want - m.getYRot())) < within;
    }

    public static void drift(Mob m, Vec3 goal, Vec3 watch, double speed, long now) {
        S st = s(m);
        RandomSource r = m.getRandom();
        if (st.nextPause == 0) st.nextPause = now + 140 + r.nextInt(200);
        if (now < st.pauseUntil) {
            m.getNavigation().stop();
            turn(m, watch != null ? watch : goal, 2.0f);
            return;
        }
        if (now >= st.nextPause) {
            st.nextPause = now + 160 + r.nextInt(260);
            if (r.nextFloat() < 0.35f) {
                st.pauseUntil = now + 50 + r.nextInt(40);
                m.getNavigation().stop();
                play(m, "cm_pause", 70, now);
                return;
            }
        }
        st.phase += 0.025 + r.nextDouble() * 0.01;
        double v = speed * (0.72 + 0.38 * Math.sin(st.phase) + 0.1 * Math.sin(st.phase * 2.7));
        if (now >= st.nextPath) {
            m.getNavigation().moveTo(goal.x, goal.y, goal.z, v);
            st.nextPath = now + 12 + r.nextInt(10);
        } else {
            m.getNavigation().setSpeedModifier(v);
        }
        Vec3 pos = m.position();
        Vec3 dir = st.last == null ? goal.subtract(pos) : pos.subtract(st.last);
        st.last = pos;
        Vec3 face = dir.horizontalDistanceSqr() > 1e-4 ? pos.add(dir.normalize()) : goal;
        turn(m, face, 3.5f);
        driftAnim(m, now);
    }

    public static void driftAnim(Entity e, long now) {
        S st = s(e);
        if (now >= st.animEnd - 4 || !st.anim.startsWith("cm_drift")) {
            boolean a = !st.anim.equals("cm_drift_a");
            play(e, a ? "cm_drift_a" : "cm_drift_b", a ? 120 : 150, now);
        }
    }

    public static void walk(Mob m, long now) {
        S st = s(m);
        if (!st.anim.equals("cm_walk") || now >= st.animEnd - 2) play(m, "cm_walk", 52, now);
    }

    public static void loop(Entity e, String name, int ticks, long now) {
        S st = s(e);
        if (!st.anim.equals(name) || now >= st.animEnd - 2) play(e, name, ticks, now);
    }

    private Motion() {}
}
