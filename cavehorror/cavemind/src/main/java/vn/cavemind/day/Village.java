package vn.cavemind.day;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.behavior.BlockPosTracker;
import net.minecraft.world.entity.ai.behavior.EntityTracker;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.phys.Vec3;
import vn.cavemind.director.Director;
import vn.cavemind.director.Dread;
import vn.cavemind.memory.Moment;
import vn.cavemind.memory.Profile;
import vn.cavemind.sense.Watch;

final class Village extends Piece {
    final List<AbstractVillager> folk = new ArrayList<>();
    Vec3 focus;
    boolean told;

    @Override
    String id() {
        return "village";
    }

    @Override
    String vi() {
        return "làng đứng lặng (dân làng bất động, cùng nhìn về một hướng)";
    }

    @Override
    boolean begin(ServerPlayer p, Watch w, Profile pr, long now) {
        ServerLevel lvl = p.serverLevel();
        folk.addAll(lvl.getEntitiesOfClass(AbstractVillager.class, p.getBoundingBox().inflate(48, 16, 48), v -> !v.isBaby() && !v.isSleeping()));
        if (folk.size() < 3) return false;
        double ang = Math.toRadians(p.getYRot() + 120 + p.getRandom().nextInt(120));
        focus = p.position().add(-Math.sin(ang) * 120, 6, Math.cos(ang) * 120);
        return true;
    }

    @Override
    boolean tick(ServerPlayer p, Watch w, Profile pr, long now) {
        folk.removeIf(v -> !v.isAlive());
        if (folk.isEmpty()) return false;
        ServerLevel lvl = p.serverLevel();
        for (AbstractVillager v : folk) {
            var brain = v.getBrain();
            brain.eraseMemory(MemoryModuleType.WALK_TARGET);
            v.getNavigation().stop();
            if (v.distanceToSqr(p) < 6.5 * 6.5) {
                brain.setMemory(MemoryModuleType.LOOK_TARGET, new EntityTracker(p, true));
                if (!told) {
                    told = true;
                    Director.scare(p, 6);
                    pr.moment(new Moment(Moment.RITUAL, Dread.day(lvl), Dread.time(lvl), p.blockPosition(), folk.size(), 1));
                }
            } else {
                brain.setMemory(MemoryModuleType.LOOK_TARGET, new BlockPosTracker(focus));
            }
        }
        return now - start < 3000;
    }
}
