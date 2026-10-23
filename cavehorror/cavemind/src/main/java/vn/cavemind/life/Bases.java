package vn.cavemind.life;

import java.util.List;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import vn.cavemind.memory.MindData;
import vn.cavemind.memory.Profile;

public final class Bases {
    static void refresh(LifeData d, List<ServerPlayer> ps) {
        for (ServerPlayer p : ps) {
            Profile pr = MindData.profile(p);
            BlockPos home = pr.home != null ? pr.home : p.getRespawnPosition();
            if (home == null) home = built(pr);
            if (home == null) continue;
            LifeData.Base mine = null;
            for (LifeData.Base b : d.bases) if (b.owners.contains(p.getUUID())) mine = b;
            LifeData.Base near = near(d, home, 40);
            if (near == null) {
                if (mine != null && mine.owners.size() == 1) {
                    mine.center = home;
                    near = mine;
                } else {
                    if (mine != null) mine.owners.remove(p.getUUID());
                    near = new LifeData.Base();
                    near.center = home;
                    d.bases.add(near);
                }
            } else if (mine != null && mine != near) {
                mine.owners.remove(p.getUUID());
            }
            near.owners.add(p.getUUID());
            near.radius = radius(near, pr);
        }
        d.bases.removeIf(b -> b.owners.isEmpty());
        while (d.bases.size() > 8) d.bases.remove(0);
        d.setDirty();
    }

    private static BlockPos built(Profile pr) {
        if (pr.placedLog.size() < 60) return null;
        long sx = 0, sy = 0, sz = 0;
        int n = 0;
        for (long[] e : pr.placedLog) {
            BlockPos q = BlockPos.of(e[0]);
            sx += q.getX();
            sy += q.getY();
            sz += q.getZ();
            n++;
        }
        BlockPos c = new BlockPos((int) (sx / n), (int) (sy / n), (int) (sz / n));
        int close = 0;
        for (long[] e : pr.placedLog) if (BlockPos.of(e[0]).distSqr(c) < 24 * 24) close++;
        return close >= 40 ? c : null;
    }

    private static int radius(LifeData.Base b, Profile pr) {
        int far = 12;
        for (long[] e : pr.placedLog) {
            BlockPos q = BlockPos.of(e[0]);
            double dd = Math.sqrt(q.distSqr(b.center));
            if (dd < 40) far = Math.max(far, (int) dd + 2);
        }
        return Math.max(b.radius == 0 ? 12 : Math.min(b.radius, 40), Math.min(36, far));
    }

    public static LifeData.Base near(LifeData d, BlockPos pos, int extra) {
        LifeData.Base best = null;
        double bd = Double.MAX_VALUE;
        for (LifeData.Base b : d.bases) {
            double dd = b.center.distSqr(pos);
            double lim = b.radius + extra;
            if (dd <= lim * lim && dd < bd) {
                bd = dd;
                best = b;
            }
        }
        return best;
    }

    public static LifeData.Base owned(LifeData d, ServerPlayer p) {
        UUID id = p.getUUID();
        for (LifeData.Base b : d.bases) if (b.owners.contains(id)) return b;
        return null;
    }

    private Bases() {}
}
