package vn.cavemind.world;

import java.lang.reflect.Method;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import vn.cavemind.CaveMind;

public final class Moon {
    public static final String MINER = "miner_moon", BLOOD = "blood_moon", CRIMSON = "crimson_moon";
    private static Method get, active, id;
    private static boolean tried, ok;
    private static String cached = "";
    private static long cachedAt = -1000;
    private static String forced;

    static boolean hook() {
        if (tried) return ok;
        tried = true;
        try {
            Class<?> data = Class.forName("com.mrbysco.lunar.LunarPhaseData");
            get = data.getMethod("get", Level.class);
            active = data.getMethod("getActiveLunarEvent");
            id = Class.forName("com.mrbysco.lunar.api.ILunarEvent").getMethod("getID");
            ok = true;
            CaveMind.LOG.info("[CaveMind] Lunar linked: moon nights now wake the things in the fog");
        } catch (Throwable t) {
            ok = false;
        }
        return ok;
    }

    public static String now(ServerLevel lvl) {
        if (forced != null) return forced;
        ServerLevel ow = lvl.getServer().overworld();
        long t = ow.getGameTime();
        if (t - cachedAt < 40 && t >= cachedAt) return cached;
        cachedAt = t;
        cached = "";
        if (!hook()) return cached;
        try {
            Object d = get.invoke(null, ow);
            Object ev = d == null ? null : active.invoke(d);
            Object rl = ev == null ? null : id.invoke(ev);
            if (rl instanceof ResourceLocation r) cached = r.getPath();
        } catch (Throwable t2) {
            ok = false;
        }
        return cached;
    }

    public static boolean is(ServerLevel lvl, String moon) {
        return moon.equals(now(lvl));
    }

    public static void force(String moon) {
        forced = moon == null || moon.isEmpty() ? null : moon;
    }

    public static String vi(String moon) {
        return switch (moon) {
            case MINER -> "Trăng Thợ Mỏ";
            case BLOOD -> "Trăng Máu";
            case CRIMSON -> "Trăng Thẫm";
            default -> "";
        };
    }

    private Moon() {}
}
