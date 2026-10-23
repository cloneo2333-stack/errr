package vn.cavemind.day;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import vn.cavemind.Cfg;
import vn.cavemind.compat.Compat;
import vn.cavemind.dev.Dev;
import vn.cavemind.director.Director;
import vn.cavemind.sense.Sight;
import vn.cavemind.sense.Watch;
import vn.cavemind.world.Fx;

public final class QuietFallen {
    private static final Map<UUID, Integer> LOOK = new HashMap<>();

    public static void tick(ServerPlayer p, Watch w, long now) {
        if (!Cfg.QUIET_FALLEN.get() || !Compat.apollyonLoaded()) return;
        EntityType<?> t = Compat.type(Compat.FALLEN);
        if (t == null) return;
        ServerLevel lvl = p.serverLevel();
        for (Entity e : lvl.getEntities((Entity) null, p.getBoundingBox().inflate(56, 24, 56), x -> x.getType() == t && x.isAlive())) {
            if (e.getTags().contains("cm_sinking")) continue;
            Sight si = Sight.of(p, w, e.getEyePosition());
            if (si.on && si.edge() < 0.55 && Sight.visible(p, e) > 0.3) {
                if (LOOK.merge(e.getUUID(), 1, Integer::sum) >= 3) sink(p, e);
            } else {
                LOOK.remove(e.getUUID());
            }
        }
    }

    static void sink(ServerPlayer p, Entity e) {
        LOOK.remove(e.getUUID());
        e.addTag("cm_sinking");
        Compat.grant(p, "apollyon:thats_not_an_enderman");
        Compat.anim(e, "animation.the_fallen.sink");
        e.setSilent(true);
        Director.scare(p, 10);
        Fx.later(p.serverLevel(), 16, () -> {
            if (e.isAlive()) e.discard();
        });
        Dev.say(p, "The Fallen lặng lẽ chìm xuống đất khi bạn nhìn nó (không còn ảnh jumpscare)");
    }

    private QuietFallen() {}
}
