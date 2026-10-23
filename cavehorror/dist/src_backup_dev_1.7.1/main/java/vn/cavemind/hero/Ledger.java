package vn.cavemind.hero;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import vn.cavemind.director.Dread;
import vn.cavemind.memory.Profile;
import vn.cavemind.sense.Watch;

public final class Ledger {
    public static final int STEP = 2, N = 7200;
    public static final int CROUCH = 1, SWING = 2, SPRINT = 4, GROUND = 8, UNDER = 16, AWAY = 32, ASLEEP = 64;

    static final class Tape {
        final float[] x = new float[N], y = new float[N], z = new float[N], yaw = new float[N], pitch = new float[N];
        final byte[] flags = new byte[N];
        final int[] item = new int[N];
        int head, size;
    }

    record Frame(Vec3 pos, float yaw, float pitch, int flags, int item) {
        boolean has(int f) {
            return (flags & f) != 0;
        }
    }

    private static final Map<UUID, Tape> TAPES = new HashMap<>();

    public static void record(ServerPlayer p, Watch w, Profile pr, long now) {
        if (now % STEP != 0) return;
        Tape t = TAPES.computeIfAbsent(p.getUUID(), k -> new Tape());
        int i = t.head;
        t.x[i] = (float) p.getX();
        t.y[i] = (float) p.getY();
        t.z[i] = (float) p.getZ();
        t.yaw[i] = p.getYRot();
        t.pitch[i] = p.getXRot();
        int f = 0;
        if (p.isCrouching()) f |= CROUCH;
        if (p.swinging) f |= SWING;
        if (p.isSprinting()) f |= SPRINT;
        if (p.onGround()) f |= GROUND;
        if (w.underground) f |= UNDER;
        if (p.level().dimension() != Level.OVERWORLD || p.isPassenger() || p.isFallFlying()) f |= AWAY;
        if (p.isSleeping()) f |= ASLEEP;
        t.flags[i] = (byte) f;
        t.item[i] = BuiltInRegistries.ITEM.getId(p.getMainHandItem().getItem());
        t.head = (i + 1) % N;
        t.size = Math.min(N, t.size + 1);
        if (now % 600 == 0 && p.level().dimension() == Level.OVERWORLD) pr.diary(p.blockPosition(), Dread.day(p.serverLevel()), Dread.time(p.serverLevel()));
    }

    static int size(ServerPlayer p) {
        Tape t = TAPES.get(p.getUUID());
        return t == null ? 0 : t.size;
    }

    static Frame frame(ServerPlayer p, int ago) {
        Tape t = TAPES.get(p.getUUID());
        if (t == null || ago < 0 || ago >= t.size) return null;
        int i = (t.head - 1 - ago + N * 2) % N;
        return new Frame(new Vec3(t.x[i], t.y[i], t.z[i]), t.yaw[i], t.pitch[i], t.flags[i] & 0xFF, t.item[i]);
    }

    public static void forget(UUID id) {
        TAPES.remove(id);
    }

    public static void clear() {
        TAPES.clear();
    }

    private Ledger() {}
}
