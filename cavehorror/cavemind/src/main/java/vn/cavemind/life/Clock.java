package vn.cavemind.life;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;

public final class Clock {
    public static long day(MinecraftServer server) {
        ServerLevel ow = server.overworld();
        LifeData d = LifeData.get(server);
        long raw = ow.getDayTime() / 24000L;
        if (d.clockRaw == Long.MIN_VALUE) {
            d.clockRaw = raw;
            d.clockOffset = Math.max(0, ow.getGameTime() / 24000L - raw);
            d.setDirty();
        } else if (raw != d.clockRaw) {
            if (raw < d.clockRaw) d.clockOffset += d.clockRaw - raw;
            d.clockRaw = raw;
            d.setDirty();
        }
        return raw + d.clockOffset;
    }

    private Clock() {}
}
