package vn.cavemind.world;

import java.util.EnumSet;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.Vec3;

public final class StareGoal extends Goal {
    private static final Map<Mob, StareGoal> GOALS = new WeakHashMap<>();
    private final Mob mob;
    private Entity focus;
    private Vec3 point;
    private long until;

    private StareGoal(Mob mob) {
        this.mob = mob;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK, Flag.JUMP));
    }

    public static void apply(Mob mob, Entity focus, Vec3 point, int ticks) {
        StareGoal g = GOALS.get(mob);
        if (g == null) {
            g = new StareGoal(mob);
            GOALS.put(mob, g);
            mob.goalSelector.addGoal(0, g);
        }
        g.focus = focus;
        g.point = point;
        g.until = mob.level().getGameTime() + ticks;
    }

    @Override
    public boolean canUse() {
        return mob.level().getGameTime() < until;
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
    public void start() {
        mob.getNavigation().stop();
    }

    @Override
    public void tick() {
        Vec3 at = focus != null && focus.isAlive() ? focus.getEyePosition() : point;
        if (at == null) return;
        mob.getNavigation().stop();
        mob.getLookControl().setLookAt(at.x, at.y, at.z, 60f, 60f);
    }
}
