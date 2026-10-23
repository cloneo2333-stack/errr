package vn.cavemind.man;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import vn.cavemind.sense.Sight;
import vn.cavemind.sense.Watch;
import vn.cavemind.world.Spots;

final class Window {
    static Spots.Cand find(ServerPlayer p, Watch w) {
        ServerLevel lvl = p.serverLevel();
        BlockPos eye = BlockPos.containing(p.getEyePosition());
        Spots.Cand best = null;
        for (int dy = -2; dy <= 2; dy++) {
            for (int dx = -9; dx <= 9; dx++) {
                for (int dz = -9; dz <= 9; dz++) {
                    BlockPos q = eye.offset(dx, dy, dz);
                    if (!Sight.glassy(lvl.getBlockState(q))) continue;
                    for (Direction d : Direction.Plane.HORIZONTAL) {
                        double side = (p.getX() - (q.getX() + 0.5)) * d.getStepX() + (p.getZ() - (q.getZ() + 0.5)) * d.getStepZ();
                        if (side >= -0.3) continue;
                        BlockPos out = q.relative(d);
                        if (!lvl.getBlockState(out).getCollisionShape(lvl, out).isEmpty()) continue;
                        if (!lvl.canSeeSky(out.relative(d)) && !lvl.canSeeSky(out.relative(d, 2))) continue;
                        for (int k = 1; k <= 2; k++) {
                            BlockPos guess = q.relative(d, k).below(1);
                            BlockPos feet = Spots.nearFloor(lvl, guess, 3, 3);
                            if (feet == null) continue;
                            if (feet.getY() > q.getY() || feet.getY() < q.getY() - 3) continue;
                            Spots.Cand c = Spots.eval(p, w, feet, ManMind.W, ManMind.H);
                            if (c.vis < 0.2 || c.sight.on && c.vis > 0) continue;
                            c.aux = q;
                            c.score = c.vis + (1.0 - c.dist / 14.0) + (p.getRandom().nextDouble() * 0.1);
                            if (best == null || c.score > best.score) best = c;
                        }
                    }
                }
            }
        }
        return best;
    }

    static BlockPos door(ServerPlayer p) {
        ServerLevel lvl = p.serverLevel();
        BlockPos at = p.blockPosition();
        BlockPos best = null;
        double bd = 1e9;
        for (int dy = -2; dy <= 2; dy++) {
            for (int dx = -10; dx <= 10; dx++) {
                for (int dz = -10; dz <= 10; dz++) {
                    BlockPos q = at.offset(dx, dy, dz);
                    BlockState st = lvl.getBlockState(q);
                    if (!ManMind.isLowerDoor(st) || st.getValue(DoorBlock.OPEN)) continue;
                    if (outside(lvl, q) == null) continue;
                    double d = q.distSqr(at);
                    if (d < bd) {
                        bd = d;
                        best = q;
                    }
                }
            }
        }
        return best;
    }

    static Direction outside(ServerLevel lvl, BlockPos door) {
        BlockState st = lvl.getBlockState(door);
        if (!(st.getBlock() instanceof DoorBlock)) return null;
        Direction f = st.getValue(DoorBlock.FACING);
        for (Direction d : new Direction[]{f, f.getOpposite()}) {
            if (lvl.canSeeSky(door.relative(d)) || lvl.canSeeSky(door.relative(d, 2))) return d;
        }
        return null;
    }

    private Window() {}
}
