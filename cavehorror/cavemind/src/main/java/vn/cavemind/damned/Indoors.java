package vn.cavemind.damned;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.block.AbstractCandleBlock;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.Vec3;
import vn.cavemind.compat.Compat;
import vn.cavemind.director.Director;
import vn.cavemind.memory.Profile;
import vn.cavemind.sense.Sight;
import vn.cavemind.sense.Watch;
import vn.cavemind.world.Fx;
import vn.cavemind.world.Spots;

final class Indoors {
    static boolean candle(ServerPlayer p, Watch w, Haunt.Run r) {
        ServerLevel lvl = p.serverLevel();
        BlockPos at = p.blockPosition();
        for (int dy = -2; dy <= 3; dy++) {
            for (int dx = -8; dx <= 8; dx++) {
                for (int dz = -8; dz <= 8; dz++) {
                    BlockPos q = at.offset(dx, dy, dz);
                    if (lit(lvl.getBlockState(q))) {
                        r.candle = q;
                        return true;
                    }
                }
            }
        }
        return false;
    }

    static boolean lit(BlockState st) {
        return (st.getBlock() instanceof AbstractCandleBlock || st.getBlock() instanceof CampfireBlock)
            && st.hasProperty(BlockStateProperties.LIT) && st.getValue(BlockStateProperties.LIT);
    }

    static boolean runCandle(ServerPlayer p, Watch w, Profile pr, Haunt.Run r, long now) {
        ServerLevel lvl = p.serverLevel();
        BlockState st = lvl.getBlockState(r.candle);
        if (!lit(st)) return false;
        Vec3 c = Vec3.atCenterOf(r.candle);
        Sight si = Sight.of(p, w, c);
        boolean watching = si.on && si.edge() < 0.7 && Sight.clear(lvl, p.getEyePosition(), c, p);
        if (watching) r.stare++;
        if (r.stare > 30 || now - r.start > 2400 && !watching) {
            lvl.setBlock(r.candle, st.setValue(BlockStateProperties.LIT, false), 11);
            Fx.sound(p, Holder.direct(st.getBlock() instanceof CampfireBlock ? SoundEvents.FIRE_EXTINGUISH : SoundEvents.CANDLE_EXTINGUISH),
                SoundSource.BLOCKS, c, 1.0f, 0.8f);
            if (watching) {
                Director.scare(p, 14);
                Fx.later(lvl, 50, () -> Haunt.breath(p, p.getEyePosition().add(Vec3.directionFromRotation(0, p.getYRot()).scale(-1.2)), 0.4f));
                Haunt.advance(p, pr, true);
            }
            return false;
        }
        return now - r.start < 3600;
    }

    static boolean runPresence(ServerPlayer p, Watch w, Profile pr, Haunt.Run r, long now) {
        if (w.stillTicks > 60 && w.light <= 9) {
            Vec3 back = p.getEyePosition().add(Vec3.directionFromRotation(0, p.getYRot()).scale(-1.25));
            Haunt.breath(p, back, 0.42f);
            Director.scare(p, 12);
            if (r.mode++ >= 1 || p.getRandom().nextBoolean()) {
                Haunt.advance(p, pr, true);
                return false;
            }
            w.stillTicks = 0;
        }
        return now - r.start < 3000;
    }

    static boolean doorway(ServerPlayer p, Watch w, Haunt.Run r) {
        double side = p.getRandom().nextBoolean() ? 1 : -1;
        double edge = Math.toDegrees(w.hHalf()) * 1.1;
        Spots.Cand c = Spots.find(p, w, new Spots.Spec().ring(4, 10).dir(side * edge, 25).body(Haunt.W, Haunt.H).cave(true).tries(40)
            .when(k -> !k.sight.on && k.vis >= 0.6 && k.light <= 7 && !p.serverLevel().canSeeSky(k.feet.above()))
            .rank(k -> (7 - k.light) * 0.1 + k.vis));
        if (c == null) return false;
        Entity e = Haunt.spawn(p, c.feet, p.getEyePosition());
        if (e == null) return false;
        r.body = e.getUUID();
        Compat.anim(e, Haunt.ANIM_IDLE);
        return true;
    }

    static boolean runDoorway(ServerPlayer p, Watch w, Profile pr, Haunt.Run r, long now) {
        Entity e = Haunt.body(p, r);
        if (!(e instanceof Mob m)) return false;
        Haunt.face(m, p.getEyePosition());
        boolean on = Haunt.perceive(p, w, r, e);
        Sight pred = Sight.predict(p, w, e.getBoundingBox().getCenter(), 3);
        if (!r.noticed && pred.on && Math.abs(pred.sx) < 0.5 && Math.abs(w.yawSpeed) > 6) {
            Haunt.advance(p, pr, true);
            return false;
        }
        if (r.noticed) {
            if (r.mode == 0) {
                r.mode = 1;
                r.modeAt = now;
                Compat.anim(e, Haunt.ANIM_SPOTTED);
            }
            if (r.away >= 3 || e.distanceToSqr(p) < 6) {
                Haunt.advance(p, pr, true);
                return false;
            }
            return now - r.modeAt < 400;
        }
        return now - r.start < 1200;
    }

    private Indoors() {}
}
