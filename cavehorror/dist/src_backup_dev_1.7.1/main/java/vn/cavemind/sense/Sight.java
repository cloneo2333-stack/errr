package vn.cavemind.sense;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.common.Tags;
import net.minecraftforge.registries.ForgeRegistries;

public final class Sight {
    public final double sx, sy, depth, angle;
    public final boolean on;

    private Sight(double sx, double sy, double depth, double angle, boolean on) {
        this.sx = sx;
        this.sy = sy;
        this.depth = depth;
        this.angle = angle;
        this.on = on;
    }

    public static Sight of(ServerPlayer p, Watch w, Vec3 pt) {
        return of(p.getEyePosition(), p.getYRot(), p.getXRot(), w, pt);
    }

    public static Sight of(Vec3 eye, float yawDeg, float pitchDeg, Watch w, Vec3 pt) {
        Vec3 f = Vec3.directionFromRotation(pitchDeg, yawDeg);
        Vec3 up = new Vec3(0, 1, 0);
        Vec3 r = f.cross(up);
        if (r.lengthSqr() < 1e-6) r = new Vec3(1, 0, 0);
        r = r.normalize();
        Vec3 u = r.cross(f).normalize();
        Vec3 d = pt.subtract(eye);
        double z = d.dot(f), x = d.dot(r), y = d.dot(u);
        double len = d.length();
        double ang = len < 1e-6 ? 0 : Math.toDegrees(Math.acos(Math.max(-1, Math.min(1, z / len))));
        if (z <= 0.05) return new Sight(x >= 0 ? 9 : -9, 0, z, ang, false);
        double th = Math.tan(w.hHalf()), tv = Math.tan(w.vHalf());
        double sx = x / z / th, sy = y / z / tv;
        boolean on = Math.abs(sx) <= 1.0 && Math.abs(sy) <= 1.0;
        return new Sight(sx, sy, z, ang, on);
    }

    public double edge() {
        return Math.max(Math.abs(sx), Math.abs(sy));
    }

    public boolean central() {
        return on && Math.abs(sx) < 0.3 && Math.abs(sy) < 0.4;
    }

    public static boolean clear(Level lvl, Vec3 from, Vec3 to, Entity viewer) {
        Boolean blocked = BlockGetter.traverseBlocks(from, to, lvl, (level, pos) -> {
            BlockState st = level.getBlockState(pos);
            if (st.isAir() || glassy(st)) return null;
            VoxelShape shape = st.getVisualShape(level, pos, CollisionContext.empty());
            if (shape.isEmpty()) return null;
            BlockHitResult hit = shape.clip(from, to, pos);
            if (hit == null) return null;
            return hit.getLocation().distanceToSqr(to) < 0.04 ? null : Boolean.TRUE;
        }, level -> Boolean.FALSE);
        return !blocked;
    }

    public static boolean glassy(BlockState st) {
        if (st.is(Tags.Blocks.GLASS) || st.is(Tags.Blocks.GLASS_PANES)) return true;
        ResourceLocation id = ForgeRegistries.BLOCKS.getKey(st.getBlock());
        if (id == null) return false;
        String path = id.getPath();
        return path.contains("glass") || path.contains("window") && !path.contains("shutter");
    }

    public static double visible(ServerPlayer p, AABB box) {
        Level lvl = p.level();
        Vec3 eye = p.getEyePosition();
        Vec3 c = box.getCenter();
        Vec3 side = new Vec3(c.z - eye.z, 0, eye.x - c.x);
        if (side.lengthSqr() > 1e-6) side = side.normalize().scale(box.getXsize() * 0.45);
        double h = box.getYsize();
        Vec3[] pts = {
            new Vec3(c.x, box.minY + h * 0.12, c.z),
            new Vec3(c.x, box.minY + h * 0.5, c.z),
            new Vec3(c.x, box.minY + h * 0.9, c.z),
            new Vec3(c.x, box.minY + h * 0.62, c.z).add(side),
            new Vec3(c.x, box.minY + h * 0.62, c.z).subtract(side)
        };
        int ok = 0;
        for (Vec3 pt : pts) if (clear(lvl, eye, pt, p)) ok++;
        return ok / (double) pts.length;
    }

    public static double visible(ServerPlayer p, Entity e) {
        return visible(p, e.getBoundingBox());
    }

    public static AABB body(BlockPos feet, double w, double h) {
        return new AABB(feet.getX() + 0.5 - w / 2, feet.getY(), feet.getZ() + 0.5 - w / 2,
            feet.getX() + 0.5 + w / 2, feet.getY() + h, feet.getZ() + 0.5 + w / 2);
    }

    public static Sight predict(ServerPlayer p, Watch w, Vec3 pt, int ticksAhead) {
        float yaw = p.getYRot() + w.yawSpeed * ticksAhead;
        return of(p.getEyePosition(), yaw, p.getXRot(), w, pt);
    }
}
