package vn.cavemind.gear;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;
import vn.cavemind.director.Dread;
import vn.cavemind.lore.Lore;
import vn.cavemind.lore.Points;
import vn.cavemind.world.Fx;
import vn.cavemind.world.Moon;

public final class Nights {
    private static String told = "";
    private static long toldDay = -1;

    public static void clear() {
        told = "";
        toldDay = -1;
    }

    public static void tick(MinecraftServer server, long now) {
        if (now % 20 != 11) return;
        ServerLevel ow = server.overworld();
        String moon = Moon.now(ow);
        int tod = Dread.time(ow);
        boolean night = tod >= 13000 && tod < 23000;
        if (!night || moon.isEmpty()) return;
        long day = Dread.day(ow);
        if (!(moon.equals(told) && day == toldDay)) {
            told = moon;
            toldDay = day;
            String line = switch (moon) {
                case Moon.MINER -> "Trăng Thợ Mỏ: dưới lòng đất, đá vọng lại tiếng cuốc của một người không có ở đó.";
                case Moon.CRIMSON -> "Trăng Thẫm: những cột khói đỏ bốc lên từ sân chơi và các cây đếm, kèm tiếng trẻ con cười khẽ.";
                default -> null;
            };
            if (line != null) for (ServerPlayer p : ow.players()) p.displayClientMessage(Component.literal(line).withStyle(ChatFormatting.DARK_PURPLE, ChatFormatting.ITALIC), false);
        }
        if (moon.equals(Moon.CRIMSON)) {
            smoke(ow);
            if (now % 200 == 11) leak(ow);
        }
    }

    static List<BlockPos> spots(ServerLevel ow) {
        List<BlockPos> spots = new ArrayList<>();
        BlockPos tree = Lore.part(ow, "p", "tree");
        if (tree != null) spots.add(tree);
        for (Points.Point pt : Points.all(ow)) if (pt.built && pt.post != null) spots.add(pt.post);
        return spots;
    }

    static void smoke(ServerLevel ow) {
        List<BlockPos> spots = spots(ow);
        for (ServerPlayer p : ow.players()) {
            for (BlockPos q : spots) if (ow.isLoaded(q) && p.distanceToSqr(Vec3.atCenterOf(q)) < 160 * 160) Signal.smoke(p, q.above(4), true);
        }
    }

    static void leak(ServerLevel ow) {
        List<BlockPos> spots = new ArrayList<>();
        BlockPos tree = Lore.part(ow, "p", "tree");
        if (tree != null) spots.add(tree);
        BlockPos lid = Lore.part(ow, "p", "lid");
        if (lid != null) spots.add(lid);
        for (Points.Point pt : Points.all(ow)) if (pt.built && pt.post != null) spots.add(pt.post);
        var r = ow.getRandom();
        for (ServerPlayer p : ow.players()) {
            for (BlockPos q : spots) {
                double d = Math.sqrt(p.distanceToSqr(Vec3.atCenterOf(q)));
                if (d > 120 || d < 6 || r.nextFloat() > 0.5f) continue;
                Vec3 dir = Vec3.atCenterOf(q).subtract(p.position()).normalize();
                Vec3 at = p.position().add(dir.scale(Math.min(d, 18)));
                float vol = (float) Math.max(0.15, 0.6 - d / 300);
                Fx.sound(p, "minecraft:block.amethyst_block.chime", SoundSource.AMBIENT, at, vol, 0.5f + r.nextFloat() * 0.2f);
                Fx.later(ow, 14 + r.nextInt(20), () -> Fx.sound(p, "minecraft:entity.allay.ambient_without_item", SoundSource.AMBIENT, at, vol * 0.7f, 0.55f));
                break;
            }
        }
    }

    private Nights() {}
}
