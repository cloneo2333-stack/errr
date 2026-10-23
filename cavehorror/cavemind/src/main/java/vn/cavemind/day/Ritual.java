package vn.cavemind.day;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.phys.Vec3;
import vn.cavemind.director.Director;
import vn.cavemind.director.Dread;
import vn.cavemind.memory.Moment;
import vn.cavemind.memory.Profile;
import vn.cavemind.sense.Sight;
import vn.cavemind.sense.Watch;
import vn.cavemind.world.Spots;

final class Ritual extends Piece {
    final List<Mob> members = new ArrayList<>();
    Vec3 center;
    int looked;
    long turnUntil;
    boolean turned;

    static boolean eligible(Animal a) {
        return !(a instanceof TamableAnimal t && t.isTame()) && !a.isBaby() && !a.isPassenger() && !a.isLeashed();
    }

    @Override
    String id() {
        return "ritual";
    }

    @Override
    String vi() {
        return "nghi lễ (thú vật đứng thành vòng, cùng nhìn vào khoảng trống)";
    }

    @Override
    boolean begin(ServerPlayer p, Watch w, Profile pr, long now) {
        ServerLevel lvl = p.serverLevel();
        List<Animal> all = lvl.getEntitiesOfClass(Animal.class, p.getBoundingBox().inflate(48, 16, 48), Ritual::eligible);
        if (all.size() < 5) return false;
        Vec3 sum = Vec3.ZERO;
        for (Animal a : all) sum = sum.add(a.position());
        Vec3 mid = sum.scale(1.0 / all.size());
        BlockPos at = null;
        for (int i = 0; i < 16 && at == null; i++) {
            double ang = p.getRandom().nextDouble() * Math.PI * 2;
            double r = i * 1.5;
            BlockPos c = Spots.surface(lvl, (int) Math.floor(mid.x + Math.cos(ang) * r), (int) Math.floor(mid.z + Math.sin(ang) * r), 2);
            if (c != null && c.distSqr(p.blockPosition()) > 14 * 14 && Spots.standable(lvl, c, 2)) at = c;
        }
        if (at == null) return false;
        center = Vec3.atBottomCenterOf(at);
        all.sort((a, b) -> Double.compare(a.distanceToSqr(center), b.distanceToSqr(center)));
        int n = Math.min(12, all.size());
        double radius = Math.max(3, Math.min(6, n * 0.55));
        for (int i = 0; i < n; i++) {
            Mob m = all.get(i);
            double ang = i * Math.PI * 2 / n;
            Vec3 slot = center.add(Math.cos(ang) * radius, 0, Math.sin(ang) * radius);
            RitualGoal.apply(m, slot, center, now + 4800);
            members.add(m);
        }
        return true;
    }

    @Override
    boolean tick(ServerPlayer p, Watch w, Profile pr, long now) {
        ServerLevel lvl = p.serverLevel();
        members.removeIf(m -> !m.isAlive());
        if (members.size() < 3) return false;
        double d = p.position().distanceTo(center);
        if (d < 7.5) {
            return false;
        }
        Sight si = Sight.of(p, w, center.add(0, 0.8, 0));
        boolean onCircle = si.on && si.edge() < 0.7 && d < 56 && Sight.clear(lvl, p.getEyePosition(), center.add(0, 1, 0), p);
        if (onCircle) looked++;
        if (looked == 20 && !turned) {
            turned = true;
            turnUntil = now + 70;
            for (Mob m : members) RitualGoal.turn(m, p, turnUntil);
            Director.scare(p, 8);
            pr.moment(new Moment(Moment.RITUAL, Dread.day(lvl), Dread.time(lvl), p.blockPosition(), members.size(), 0));
        }
        return now - start < 4800;
    }

    @Override
    void end(ServerPlayer p) {
        for (Mob m : members) RitualGoal.release(m);
        members.clear();
    }

    @Override
    boolean claims() {
        return true;
    }

    static final class RitualGoal extends Goal {
        private static final Map<Mob, RitualGoal> GOALS = new WeakHashMap<>();
        private final Mob mob;
        private Vec3 slot, center;
        private long until, turnUntil;
        private Entity turnTo;
        private long nextPath;

        private RitualGoal(Mob mob) {
            this.mob = mob;
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK, Flag.JUMP));
        }

        static void apply(Mob m, Vec3 slot, Vec3 center, long until) {
            RitualGoal g = GOALS.get(m);
            if (g == null) {
                g = new RitualGoal(m);
                GOALS.put(m, g);
                m.goalSelector.addGoal(0, g);
            }
            g.slot = slot;
            g.center = center;
            g.until = m.level().getGameTime() + (until - m.getServer().getTickCount());
            g.turnTo = null;
            g.turnUntil = 0;
        }

        static void turn(Mob m, Entity to, long untilServerTick) {
            RitualGoal g = GOALS.get(m);
            if (g == null) return;
            g.turnTo = to;
            g.turnUntil = m.level().getGameTime() + (untilServerTick - m.getServer().getTickCount());
        }

        static void release(Mob m) {
            RitualGoal g = GOALS.get(m);
            if (g != null) g.until = 0;
        }

        @Override
        public boolean canUse() {
            return slot != null && mob.level().getGameTime() < until;
        }

        @Override
        public boolean canContinueToUse() {
            return canUse();
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void stop() {
            mob.getNavigation().stop();
        }

        @Override
        public void tick() {
            long t = mob.level().getGameTime();
            double d2 = mob.position().distanceToSqr(slot.x, mob.getY(), slot.z);
            if (d2 > 1.4) {
                if (t >= nextPath) {
                    mob.getNavigation().moveTo(slot.x, slot.y, slot.z, 1.0);
                    nextPath = t + 20;
                }
            } else {
                mob.getNavigation().stop();
            }
            if (turnTo != null && t < turnUntil && turnTo.isAlive()) {
                mob.getLookControl().setLookAt(turnTo, 60f, 60f);
            } else if (d2 <= 4) {
                mob.getLookControl().setLookAt(center.x, center.y + 0.6, center.z, 30f, 30f);
            }
        }
    }
}
