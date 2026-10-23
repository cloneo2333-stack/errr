package vn.cavemind.dev;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import vn.cavemind.Cfg;

public final class Dev {
    public static boolean on() {
        return Cfg.DEV.get();
    }

    public static void say(ServerPlayer p, String vi) {
        if (p != null) vn.cavemind.eye.Eye.say(p, vi);
        if (!on() || p == null) return;
        p.sendSystemMessage(Component.literal("[DEV] ").withStyle(ChatFormatting.DARK_AQUA)
            .append(Component.literal(vi).withStyle(ChatFormatting.GRAY)));
    }

    public static void glow(Entity e) {
        if (on()) e.setGlowingTag(true);
    }

    public static String where(ServerPlayer p, Vec3 at) {
        double dx = at.x - p.getX(), dz = at.z - p.getZ(), dy = at.y - p.getY();
        double dist = Math.sqrt(dx * dx + dz * dz);
        float yawTo = (float) Math.toDegrees(Math.atan2(-dx, dz));
        float rel = Mth.wrapDegrees(yawTo - p.getYRot());
        String side = rel >= 0 ? "phải" : "trái";
        float a = Math.abs(rel);
        String dir = a < 30 ? "phía trước" : a < 80 ? "trước-" + side : a < 110 ? "bên " + side : a < 150 ? "sau-" + side : "phía sau";
        String compass = compass(yawTo);
        String h = dy > 4 ? ", cao hơn " + (int) dy : dy < -4 ? ", thấp hơn " + (int) -dy : "";
        return "cách " + (int) Math.round(dist) + "m, " + dir + " (" + compass + h + ")";
    }

    static String compass(float yaw) {
        float y = Mth.wrapDegrees(yaw);
        if (y >= -22.5 && y < 22.5) return "nam";
        if (y >= 22.5 && y < 67.5) return "tây nam";
        if (y >= 67.5 && y < 112.5) return "tây";
        if (y >= 112.5 && y < 157.5) return "tây bắc";
        if (y >= -67.5 && y < -22.5) return "đông nam";
        if (y >= -112.5 && y < -67.5) return "đông";
        if (y >= -157.5 && y < -112.5) return "đông bắc";
        return "bắc";
    }

    private Dev() {}
}
