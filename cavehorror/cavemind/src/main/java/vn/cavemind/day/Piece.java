package vn.cavemind.day;

import java.util.UUID;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import vn.cavemind.memory.Profile;
import vn.cavemind.sense.Sight;
import vn.cavemind.sense.Watch;

abstract class Piece {
    long start;
    UUID body;
    boolean noticed;
    double perceive;
    int away, stare;
    boolean fake;

    abstract String id();

    abstract String vi();

    abstract boolean begin(ServerPlayer p, Watch w, Profile pr, long now);

    abstract boolean tick(ServerPlayer p, Watch w, Profile pr, long now);

    void end(ServerPlayer p) {
        discard(p.serverLevel());
    }

    boolean claims() {
        return true;
    }

    Entity body(ServerPlayer p) {
        if (body == null) return null;
        Entity e = p.serverLevel().getEntity(body);
        return e != null && e.isAlive() ? e : null;
    }

    void discard(ServerLevel lvl) {
        if (body == null) return;
        Entity e = lvl.getEntity(body);
        if (e != null) e.discard();
        body = null;
    }

    boolean watch(ServerPlayer p, Watch w, Entity e) {
        Sight si = Sight.of(p, w, e.getBoundingBox().getCenter().add(0, e.getBbHeight() * 0.25, 0));
        double vis = Sight.visible(p, e);
        boolean on = si.on && vis > 0.15;
        away = on ? 0 : away + 1;
        if (!noticed) {
            double dist = Math.sqrt(e.distanceToSqr(p));
            double distF = Math.max(0.35, Math.min(1.0, 1.5 - dist / 100.0));
            if (on) perceive += (si.central() ? 1.0 : 0.35) * vis * distF * 1.6;
            else perceive = Math.max(0, perceive - 0.2);
            if (perceive >= 6) noticed = true;
        } else if (on && si.edge() < 0.6) {
            stare++;
        }
        return on;
    }
}
