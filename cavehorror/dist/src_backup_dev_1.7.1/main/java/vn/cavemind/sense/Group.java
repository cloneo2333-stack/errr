package vn.cavemind.sense;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class Group {
    public static List<ServerPlayer> others(ServerPlayer p, double r) {
        List<ServerPlayer> out = new ArrayList<>();
        for (ServerPlayer o : p.serverLevel().players()) {
            if (o == p || o.isSpectator() || !o.isAlive()) continue;
            if (o.distanceToSqr(p) <= r * r) out.add(o);
        }
        return out;
    }

    public static double isolation(ServerPlayer p) {
        double best = Double.MAX_VALUE;
        for (ServerPlayer o : p.serverLevel().players()) {
            if (o == p || o.isSpectator() || !o.isAlive()) continue;
            best = Math.min(best, o.distanceTo(p));
        }
        return best;
    }

    public static boolean grouped(ServerPlayer p, double r) {
        return isolation(p) <= r;
    }

    public static int count(ServerLevel lvl) {
        int n = 0;
        for (ServerPlayer o : lvl.players()) if (!o.isSpectator() && o.isAlive()) n++;
        return n;
    }

    public static boolean sees(ServerPlayer o, Vec3 pt, double max) {
        if (o.isSpectator() || !o.isAlive()) return false;
        Vec3 eye = o.getEyePosition();
        if (eye.distanceToSqr(pt) > max * max) return false;
        Sight si = Sight.of(o, Watch.of(o), pt);
        return si.on && Sight.clear(o.level(), eye, pt, o);
    }

    public static boolean seenByAny(ServerLevel lvl, Vec3 pt, double max) {
        for (ServerPlayer o : lvl.players()) if (sees(o, pt, max)) return true;
        return false;
    }

    public static boolean seenByOthers(ServerPlayer p, Vec3 pt, double max) {
        for (ServerPlayer o : p.serverLevel().players()) if (o != p && sees(o, pt, max)) return true;
        return false;
    }

    public static boolean boxSeenByOthers(ServerPlayer p, AABB box, double max) {
        for (ServerPlayer o : p.serverLevel().players()) {
            if (o == p || o.isSpectator() || o.distanceToSqr(box.getCenter()) > max * max) continue;
            if (Sight.of(o, Watch.of(o), box.getCenter()).on && Sight.visible(o, box) > 0) return true;
        }
        return false;
    }

    public static ServerPlayer nearestOther(ServerPlayer p) {
        ServerPlayer best = null;
        double bd = Double.MAX_VALUE;
        for (ServerPlayer o : p.serverLevel().players()) {
            if (o == p || o.isSpectator() || !o.isAlive()) continue;
            double d = o.distanceToSqr(p);
            if (d < bd) {
                bd = d;
                best = o;
            }
        }
        return best;
    }

    private Group() {}
}
