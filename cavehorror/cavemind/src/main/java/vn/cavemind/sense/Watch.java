package vn.cavemind.sense;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public final class Watch {
    private static final Map<UUID, Watch> ALL = new HashMap<>();

    public final UUID id;
    public float fov = 80f, aspect = 16f / 9f;
    public boolean modded;
    public float fogEnd = -1f;
    public int render = 12;
    public boolean shader;

    private final float[] yawHist = new float[24];
    private int yawIdx;
    public float yaw, yawSpeed, pitch;
    public int turnArounds;
    public long lastTurnAround = -1000;
    public Vec3 pos = Vec3.ZERO, vel = Vec3.ZERO;
    public double speed;
    public int stillTicks;
    public int idle;
    private float lastPitch;
    public float noise;
    public double stepAcc;
    public long lastStep;
    public boolean stepped;
    private boolean wasGround = true;
    public int light;
    public boolean underground, indoors, confined, outside;
    public int envTick;
    public long ticks;
    public final java.util.ArrayDeque<long[]> crumbs = new java.util.ArrayDeque<>();

    private Watch(UUID id) {
        this.id = id;
    }

    public static Watch of(ServerPlayer p) {
        return ALL.computeIfAbsent(p.getUUID(), Watch::new);
    }

    public static void forget(UUID id) {
        ALL.remove(id);
    }

    public void update(ServerPlayer p) {
        ticks++;
        Vec3 now = p.position();
        Vec3 d = now.subtract(pos);
        if (ticks > 1 && d.lengthSqr() < 64) {
            vel = vel.scale(0.6).add(d.scale(0.4));
        } else {
            vel = Vec3.ZERO;
        }
        pos = now;
        speed = Math.sqrt(vel.x * vel.x + vel.z * vel.z);
        stepped = false;
        if (p.onGround() && speed > 0.02) {
            stepAcc += Math.sqrt(d.x * d.x + d.z * d.z);
            if (stepAcc > (p.isSprinting() ? 2.1 : 1.65)) {
                stepAcc = 0;
                stepped = true;
                lastStep = ticks;
                double r = p.isCrouching() ? 3 : p.isSprinting() ? 24 : 12;
                Ears.noise(p, now, r, p.isSprinting() ? Ears.Kind.SPRINT : Ears.Kind.STEP);
            }
        }
        stillTicks = speed < 0.01 ? stillTicks + 1 : 0;

        float y = p.getYRot();
        float prev = yawHist[(yawIdx + yawHist.length - 1) % yawHist.length];
        yawSpeed = Mth.wrapDegrees(y - prev);
        yawHist[yawIdx] = y;
        float old = yawHist[(yawIdx + yawHist.length - 12) % yawHist.length];
        yawIdx = (yawIdx + 1) % yawHist.length;
        yaw = y;
        pitch = p.getXRot();
        boolean still = speed < 0.01 && Math.abs(yawSpeed) < 0.3 && Math.abs(pitch - lastPitch) < 0.3 && !p.swinging;
        idle = still ? idle + 1 : 0;
        lastPitch = pitch;
        if (ticks > 24 && Math.abs(Mth.wrapDegrees(y - old)) > 125f && ticks - lastTurnAround > 30) {
            lastTurnAround = ticks;
            turnArounds++;
            vn.cavemind.memory.MindData.profile(p).turnArounds++;
        }

        float n = 0;
        if (p.isSprinting()) n = Math.max(n, 0.55f);
        boolean ground = p.onGround();
        if (ground && !wasGround && p.fallDistance > 1.2f) Ears.noise(p, now, 14, Ears.Kind.LAND);
        else if (!ground && wasGround && vel.y > 0.05) Ears.noise(p, now, 14, Ears.Kind.JUMP);
        wasGround = ground;
        if (!p.onGround() && vel.y > 0.05) n = Math.max(n, 0.45f);
        if (p.isCrouching()) n *= 0.3f;
        noise = Math.max(noise * 0.93f, n);

        if (++envTick >= 10) {
            envTick = 0;
            senseEnv(p);
        }
        if (p.onGround() && ticks % 5 == 0) {
            BlockPos at = p.blockPosition();
            long[] last = crumbs.peekLast();
            if (last == null || BlockPos.of(last[0]).distSqr(at) >= 9) {
                crumbs.addLast(new long[]{at.asLong(), ticks, underground ? 1 : 0});
                while (crumbs.size() > 320) crumbs.removeFirst();
                vn.cavemind.man.Scent.mark(p.level(), at, vn.cavemind.man.Scent.Kind.TRAIL, p.isCrouching() ? 0.5f : 1f, p.server.getTickCount(), underground);
            }
        }
    }

    public void hear(float loud) {
        noise = Math.max(noise, loud);
    }

    private void senseEnv(ServerPlayer p) {
        ServerLevel lvl = p.serverLevel();
        BlockPos head = BlockPos.containing(p.getEyePosition());
        light = lvl.getMaxLocalRawBrightness(head);
        boolean sky = lvl.canSeeSky(head);
        int sea = lvl.getSeaLevel();
        int surface = lvl.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, head.getX(), head.getZ());
        underground = !sky && head.getY() < surface - 6 && (head.getY() < sea - 2 || lvl.getBrightness(net.minecraft.world.level.LightLayer.SKY, head) < 4);
        int roof = 0;
        for (int i = 1; i <= 8; i++) {
            if (solid(lvl, head.above(i))) {
                roof = i;
                break;
            }
        }
        int walls = 0;
        for (Direction dir : Direction.Plane.HORIZONTAL) {
            for (int i = 1; i <= 7; i++) {
                if (solid(lvl, head.relative(dir, i)) || solid(lvl, head.below().relative(dir, i))) {
                    walls++;
                    break;
                }
            }
        }
        indoors = !underground && roof > 0 && walls >= 3;
        confined = underground && walls >= 2 && roof > 0 && roof <= 4 || indoors;
        outside = !underground && !indoors;
    }

    private static boolean solid(Level lvl, BlockPos pos) {
        return !lvl.getBlockState(pos).getCollisionShape(lvl, pos).isEmpty();
    }

    public float hHalf() {
        return (float) Math.atan(Math.tan(Math.toRadians(fov * 0.5)) * aspect);
    }

    public float vHalf() {
        return (float) Math.toRadians(fov * 0.5);
    }
}
