package vn.cavemind.sense;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

public final class Range {
    public static double of(ServerPlayer p) {
        return of(p, Watch.of(p));
    }

    public static double of(ServerPlayer p, Watch w) {
        ServerLevel lvl = p.serverLevel();
        double r = w.fogEnd > 0 ? w.fogEnd * 0.8 : 72;
        r = Math.min(r, w.render * 16 - 10);
        if (w.shader) r = Math.min(r, 46);
        if (w.underground) r = Math.min(r, 30);
        else {
            if (lvl.isNight()) r *= w.light >= 8 ? 0.8 : 0.62;
            if (lvl.isThundering()) r *= 0.7;
            else if (lvl.isRaining()) r *= 0.82;
        }
        return Math.max(16, Math.min(96, r));
    }

    private Range() {}
}
