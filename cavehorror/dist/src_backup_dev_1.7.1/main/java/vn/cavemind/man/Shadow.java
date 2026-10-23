package vn.cavemind.man;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import vn.cavemind.director.Dread;
import vn.cavemind.memory.Moment;
import vn.cavemind.memory.Profile;
import vn.cavemind.sense.Sight;
import vn.cavemind.sense.Watch;
import vn.cavemind.world.Fx;
import vn.cavemind.world.Spots;

final class Shadow {
    static boolean unnoticed(ServerPlayer p, Watch w, Profile pr, ManMind.Session s, Mob m, Sight si, double vis, double dist, long now) {
        ServerLevel lvl = p.serverLevel();
        if (s.perceive >= 6) {
            ManMind.notice(p, pr, s, now, dist);
            return true;
        }
        if (w.stepped && dist < 36) step(p, m, 3 + p.getRandom().nextInt(4));
        if (w.stillTicks == 5 && p.getRandom().nextFloat() < 0.35f && dist < 36) step(p, m, 2);
        if (w.lastTurnAround == w.ticks) {
            s.turnsSeen++;
            pr.turnsWhileWatched++;
            pr.moment(new Moment(Moment.TURNED, Dread.day(lvl), Dread.time(lvl), p.blockPosition(), (int) dist, 1));
        }
        boolean onScreen = si.on && vis > 0.1;
        Sight pred = Sight.predict(p, w, m.getBoundingBox().getCenter(), 3);
        if (!onScreen && pred.on && vis > 0.1 && Math.abs(w.yawSpeed) > 14f) {
            BlockPos hide = Spots.hideNear(p, w, m.blockPosition(), 5, ManMind.W, ManMind.H, w.underground);
            if (hide != null) {
                m.teleportTo(hide.getX() + 0.5, hide.getY(), hide.getZ() + 0.5);
                return true;
            }
        }
        if (now >= s.nextFollow && !onScreen && s.anchor != null && p.position().distanceToSqr(s.anchor) > 36) {
            s.nextFollow = now + 20;
            int shade = !w.underground && lvl.isDay() && !lvl.isThundering() ? 15 : 9;
            Spots.Cand c = Spots.find(p, w, new Spots.Spec().ring(16, 28).dir(180, 50).body(ManMind.W, ManMind.H).cave(w.underground).tries(24)
                .when(k -> !k.sight.on && k.light <= shade).rank(k -> (k.cover ? 0.5 : 0) + (1 - k.vis) * 0.3));
            if (c != null) {
                m.teleportTo(c.feet.getX() + 0.5, c.feet.getY(), c.feet.getZ() + 0.5);
                s.anchor = p.position();
            }
        }
        if (dist > 48 && !onScreen) {
            s.anchor = null;
            s.nextFollow = 0;
            Spots.Cand c = Spots.find(p, w, new Spots.Spec().ring(16, 28).dir(180, 50).body(ManMind.W, ManMind.H).cave(w.underground).tries(24)
                .when(k -> !k.sight.on).rank(k -> -k.vis));
            if (c == null) return false;
            m.teleportTo(c.feet.getX() + 0.5, c.feet.getY(), c.feet.getZ() + 0.5);
            s.anchor = p.position();
        }
        long len = 1000 + (p.getUUID().hashCode() & 0x3ff);
        if (now - s.tacStart > len && !onScreen) {
            if (s.turnsSeen > 0) {
                pr.moment(new Moment(Moment.UNSEEN, Dread.day(lvl), Dread.time(lvl), p.blockPosition(), (int) dist, s.turnsSeen));
            }
            return false;
        }
        return true;
    }

    static void step(ServerPlayer p, Entity body, int delay) {
        ServerLevel lvl = p.serverLevel();
        BlockPos floor = body.blockPosition().below();
        BlockState st = lvl.getBlockState(floor);
        SoundType type = st.getSoundType(lvl, floor, body);
        Vec3 at = body.position();
        Holder<net.minecraft.sounds.SoundEvent> snd = Holder.direct(type.getStepSound());
        Fx.later(lvl, delay, () -> Fx.sound(p, snd, SoundSource.HOSTILE, at, type.getVolume() * 0.6f, type.getPitch() * 0.85f));
    }

    private Shadow() {}
}
