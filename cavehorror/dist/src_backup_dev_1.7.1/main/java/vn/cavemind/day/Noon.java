package vn.cavemind.day;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import vn.cavemind.Cfg;
import vn.cavemind.compat.Compat;
import vn.cavemind.dev.Dev;
import vn.cavemind.director.Director;
import vn.cavemind.director.Dread;
import vn.cavemind.man.ManMind;
import vn.cavemind.memory.Moment;
import vn.cavemind.memory.Profile;
import vn.cavemind.net.Net;
import vn.cavemind.sense.Sight;
import vn.cavemind.sense.Watch;
import vn.cavemind.world.Fx;
import vn.cavemind.world.Spots;

final class Noon extends Piece {
    final boolean quiet;
    Vec3 point;
    boolean placed;

    Noon(boolean quiet) {
        this.quiet = quiet;
    }

    @Override
    String id() {
        return quiet ? "quiet" : "noon";
    }

    @Override
    String vi() {
        return quiet ? "khoảnh lặng (mọi âm thanh tắt... rồi không có gì)" : "buổi trưa im lặng (mọi thứ dừng lại, nhìn về một điểm)";
    }

    @Override
    boolean begin(ServerPlayer p, Watch w, Profile pr, long now) {
        if (Cfg.SILENCE.get()) Net.fx(p, Net.SILENCE, 0, quiet ? 260 : 340);
        if (quiet) return true;
        ServerLevel lvl = p.serverLevel();
        double ang = Math.toRadians(p.getYRot() + 100 + p.getRandom().nextInt(160));
        int x = (int) Math.floor(p.getX() - Math.sin(ang) * 60), z = (int) Math.floor(p.getZ() + Math.cos(ang) * 60);
        BlockPos g = Spots.surface(lvl, x, z, 3);
        point = g != null ? Vec3.atBottomCenterOf(g).add(0, 1.5, 0) : p.position().add(-Math.sin(ang) * 60, 2, Math.cos(ang) * 60);
        int n = Fx.stare(p, null, point, 320, 40);
        if (g != null && !Sight.of(p, w, point).on && Compat.manLoaded()) {
            Entity e = ManMind.make(p, Compat.MAN_STALKER, g, true);
            if (e != null) {
                e.getPersistentData().putString(Compat.TAG, "man_day");
                body = e.getUUID();
                placed = true;
            }
        }
        pr.moment(new Moment(Moment.NOON, Dread.day(lvl), Dread.time(lvl), p.blockPosition(), n, placed ? 1 : 0));
        Director.scare(p, 6);
        Dev.say(p, n + " sinh vật đang nhìn về cùng một điểm — " + Dev.where(p, point) + (placed ? " (hắn đứng đó, chỉ trong thoáng chốc)" : ""));
        return true;
    }

    @Override
    boolean tick(ServerPlayer p, Watch w, Profile pr, long now) {
        if (body != null) {
            Entity e = body(p);
            if (e == null) {
                body = null;
            } else {
                boolean was = noticed;
                watch(p, w, e);
                if (noticed && !was) Dev.say(p, "Bạn đã kịp thấy hắn ở đó");
                if (noticed ? away >= 3 : now - start > 30 && away > 0) discard(p.serverLevel());
            }
        }
        return now - start < (quiet ? 260 : 340);
    }
}
