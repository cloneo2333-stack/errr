package vn.cavemind.world;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import net.minecraft.core.Holder;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.behavior.BlockPosTracker;
import net.minecraft.world.entity.ai.behavior.EntityTracker;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import vn.cavemind.compat.Compat;

public final class Fx {
    private static final List<Delayed> LATER = new ArrayList<>();
    private static final List<Stare> STARES = new ArrayList<>();

    private record Delayed(long at, Runnable r) {}

    private static final class Stare {
        final ServerPlayer p;
        final Entity focus;
        final Vec3 point;
        long until;
        final List<Mob> mobs = new ArrayList<>();

        Stare(ServerPlayer p, Entity focus, Vec3 point, long until) {
            this.p = p;
            this.focus = focus;
            this.point = point;
            this.until = until;
        }
    }

    public static void sound(ServerPlayer p, Holder<SoundEvent> s, SoundSource src, Vec3 at, float vol, float pitch) {
        if (s == null || p == null) return;
        p.connection.send(new ClientboundSoundPacket(s, src, at.x, at.y, at.z, vol, pitch, p.getRandom().nextLong()));
    }

    public static void sound(ServerPlayer p, String id, SoundSource src, Vec3 at, float vol, float pitch) {
        sound(p, Compat.sound(id), src, at, vol, pitch);
    }

    public static void soundAll(ServerLevel lvl, String id, SoundSource src, Vec3 at, float vol, float pitch) {
        Holder<SoundEvent> s = Compat.sound(id);
        if (s == null) return;
        lvl.playSeededSound(null, at.x, at.y, at.z, s, src, vol, pitch, lvl.getRandom().nextLong());
    }

    public static void later(ServerLevel lvl, int ticks, Runnable r) {
        LATER.add(new Delayed(lvl.getServer().getTickCount() + ticks, r));
    }

    public static int stare(ServerPlayer p, Entity focus, Vec3 point, int ticks, double radius) {
        long until = p.server.getTickCount() + ticks;
        Stare s = new Stare(p, focus, point, until);
        AABB box = p.getBoundingBox().inflate(radius, 10, radius);
        for (Mob m : p.serverLevel().getEntitiesOfClass(Mob.class, box, m -> m instanceof Animal || m instanceof AbstractVillager)) {
            if (Compat.managed(m)) continue;
            StareGoal.apply(m, focus != null ? focus : point == null ? p : null, point, ticks);
            s.mobs.add(m);
        }
        if (!s.mobs.isEmpty()) STARES.add(s);
        return s.mobs.size();
    }

    public static void tick(long now) {
        if (!LATER.isEmpty()) {
            List<Delayed> due = new ArrayList<>();
            Iterator<Delayed> it = LATER.iterator();
            while (it.hasNext()) {
                Delayed d = it.next();
                if (d.at <= now) {
                    due.add(d);
                    it.remove();
                }
            }
            for (Delayed d : due) {
                try {
                    d.r.run();
                } catch (Throwable t) {
                    vn.cavemind.CaveMind.LOG.warn("[CaveMind] delayed task failed", t);
                }
            }
        }
        Iterator<Stare> si = STARES.iterator();
        while (si.hasNext()) {
            Stare s = si.next();
            if (now > s.until || s.p.isRemoved()) {
                si.remove();
                continue;
            }
            for (Mob m : s.mobs) {
                if (!m.isAlive()) continue;
                if (m instanceof AbstractVillager) {
                    Entity f = s.focus != null && s.focus.isAlive() ? s.focus : s.point == null ? s.p : null;
                    var brain = m.getBrain();
                    if (f != null) brain.setMemory(MemoryModuleType.LOOK_TARGET, new EntityTracker(f, true));
                    else brain.setMemory(MemoryModuleType.LOOK_TARGET, new BlockPosTracker(s.point));
                    brain.eraseMemory(MemoryModuleType.WALK_TARGET);
                    m.getNavigation().stop();
                }
                if (m instanceof Wolf w && w.isTame() && now % 30 == 0) {
                    m.playSound(net.minecraft.sounds.SoundEvents.WOLF_WHINE, 0.6f, 0.8f);
                }
            }
        }
    }

    public static void clear() {
        LATER.clear();
        STARES.clear();
    }

    private Fx() {}
}
