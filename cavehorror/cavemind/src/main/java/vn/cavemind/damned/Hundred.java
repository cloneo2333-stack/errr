package vn.cavemind.damned;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CandleBlock;
import net.minecraft.world.phys.Vec3;
import vn.cavemind.CaveMind;
import vn.cavemind.compat.Compat;
import vn.cavemind.director.Director;
import vn.cavemind.director.Dread;
import vn.cavemind.director.PaceLog;
import vn.cavemind.memory.MindData;
import vn.cavemind.memory.Moment;
import vn.cavemind.memory.Profile;
import vn.cavemind.sense.Sight;
import vn.cavemind.sense.Watch;
import vn.cavemind.world.Fx;
import vn.cavemind.world.Motion;

public final class Hundred {
    static final class Rite {
        long start, next;
        int beat;
        float yaw;
        Vec3 at;
        UUID body;
        boolean done;
        long doneAt;
    }

    private static final Map<UUID, Rite> RITES = new HashMap<>();
    private static final Map<UUID, Long> READY = new HashMap<>();
    private static final Map<UUID, Long> COOL = new HashMap<>();

    public static boolean active(ServerPlayer p) {
        return RITES.containsKey(p.getUUID());
    }

    public static void forget(ServerPlayer p) {
        Rite r = RITES.remove(p.getUUID());
        if (r != null) discard(p, r);
        READY.remove(p.getUUID());
    }

    public static void clear() {
        RITES.clear();
        READY.clear();
        COOL.clear();
    }

    static void discard(ServerPlayer p, Rite r) {
        if (r.body == null) return;
        Entity e = p.serverLevel().getEntity(r.body);
        if (e != null) e.discard();
        r.body = null;
        Director.release(p, "damned", true);
    }

    static boolean eligible(Profile pr) {
        return !pr.dPeace && pr.dMazeWins >= 1 && pr.dMazeToys >= 1;
    }

    public static void tick(ServerPlayer p, Watch w, long now) {
        Rite r = RITES.get(p.getUUID());
        if (r != null) {
            if (!run(p, w, r, now)) {
                RITES.remove(p.getUUID());
                discard(p, r);
            }
            return;
        }
        if (now % 10 != 0 || p.level().dimension() != Level.OVERWORLD || Compat.exempt(p)) return;
        Profile pr = MindData.profile(p);
        if (!eligible(pr) || now < COOL.getOrDefault(p.getUUID(), 0L)) return;
        ServerLevel lvl = p.serverLevel();
        BlockPos tree = vn.cavemind.lore.Lore.part(lvl, "p", "tree");
        if (tree == null) return;
        int tod = Dread.time(lvl);
        if (tod < 13000 || tod > 23000) return;
        Vec3 trunk = Vec3.atBottomCenterOf(tree);
        double dx = trunk.x - p.getX(), dz = trunk.z - p.getZ();
        double d = Math.sqrt(dx * dx + dz * dz);
        if (d > 2.4 || Math.abs(p.getY() - tree.getY()) > 2) {
            READY.remove(p.getUUID());
            return;
        }
        float want = (float) Math.toDegrees(Math.atan2(-dx, dz));
        if (Math.abs(Mth.wrapDegrees(want - p.getYRot())) > 35 || w.speed > 0.02) {
            READY.remove(p.getUUID());
            return;
        }
        long since = READY.computeIfAbsent(p.getUUID(), k -> now);
        if (now - since < 50) return;
        if (Game.active(p) || !Director.claim(p, "damned")) return;
        Rite rt = new Rite();
        rt.start = now;
        rt.next = now + 20;
        rt.yaw = want;
        rt.at = p.position();
        RITES.put(p.getUUID(), rt);
        READY.remove(p.getUUID());
        p.displayClientMessage(Component.literal("Bạn úp mặt vào cây và bắt đầu đếm.").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC), true);
        PaceLog.encounter(p, "damned", "hundred", "start");
        CaveMind.debug("{} starts counting to one hundred", p.getGameProfile().getName());
    }

    static boolean run(ServerPlayer p, Watch w, Rite r, long now) {
        if (!p.isAlive() || p.level().dimension() != Level.OVERWORLD) return false;
        ServerLevel lvl = p.serverLevel();
        if (r.done) {
            Entity e = r.body == null ? null : lvl.getEntity(r.body);
            long t = now - r.doneAt;
            if (t == 30) say(p, "Tìm thấy rồi. Về nhà thôi.", ChatFormatting.WHITE);
            if (e instanceof Mob m && t > 30) {
                BlockPos lid = vn.cavemind.lore.Lore.part(lvl, "p", "lid");
                // about 0.9 m/s so it actually reaches the well (7-9 m) before it fades; 0.15 as a raw multiplier made it 0.35 m/s
                if (lid != null && t % 10 == 0) m.getNavigation().moveTo(lid.getX() + 0.5, lid.getY() + 1, lid.getZ() + 0.5, vn.cavemind.path.Walker.pace(m, 0.045));
            }
            return t < 260;
        }
        boolean turned = Math.abs(Mth.wrapDegrees(r.yaw - p.getYRot())) > 60 || p.position().distanceTo(r.at) > 1.6;
        Entity e = r.body == null ? null : lvl.getEntity(r.body);
        if (e != null) {
            Sight si = Sight.of(p, w, e.getBoundingBox().getCenter());
            if (si.on && Sight.visible(p, e) > 0.2) turned = true;
        }
        if (turned) {
            fail(p, r, e);
            return false;
        }
        if (now < r.next) return true;
        r.beat++;
        r.next = now + 20;
        say(p, "…" + Game.num(r.beat) + "…", ChatFormatting.GRAY);
        if (r.beat == 6 && r.body == null) {
            Vec3 back = p.position().add(Vec3.directionFromRotation(0, r.yaw).scale(-2.4));
            BlockPos feet = vn.cavemind.world.Spots.nearFloor(lvl, BlockPos.containing(back), 2, 2);
            if (feet != null) {
                Entity b = Haunt.spawn(p, feet, p.getEyePosition());
                if (b != null) {
                    r.body = b.getUUID();
                    Motion.pose(b, "cm_still", now);
                }
            }
        }
        if (r.beat % 9 == 0) Haunt.breath(p, p.getEyePosition().add(Vec3.directionFromRotation(0, r.yaw).scale(-1.3)), 0.45f);
        if (r.beat == 33 || r.beat == 71) {
            say(p, "…quay lại đi…", ChatFormatting.DARK_GRAY);
            Fx.sound(p, "apollyon:shallow_breathing", SoundSource.HOSTILE, p.getEyePosition().add(Vec3.directionFromRotation(0, r.yaw).scale(-0.8)), 0.6f, 1.2f);
        }
        if (r.beat == 88) Fx.sound(p, "apollyon:deep_laugh", SoundSource.HOSTILE, p.getEyePosition().add(Vec3.directionFromRotation(0, r.yaw).scale(-1.0)), 0.3f, 1.4f);
        if (r.beat >= 100) {
            win(p, r, now);
            return true;
        }
        return true;
    }

    static void fail(ServerPlayer p, Rite r, Entity e) {
        say(p, "Bạn đã quay lại. Nó sẽ đợi một đêm khác.", ChatFormatting.DARK_RED);
        if (e != null) Fx.sound(p, "apollyon:deep_laugh", SoundSource.HOSTILE, e.position(), 0.6f, 0.8f);
        COOL.put(p.getUUID(), p.server.getTickCount() + 24000L);
        PaceLog.encounter(p, "damned", "hundred", "turned at " + r.beat);
    }

    static void win(ServerPlayer p, Rite r, long now) {
        ServerLevel lvl = p.serverLevel();
        Profile pr = MindData.profile(p);
        say(p, "…một trăm.", ChatFormatting.WHITE);
        r.done = true;
        r.doneAt = now;
        pr.dPeace = true;
        pr.dStage = 0;
        pr.dDebt = 0;
        pr.moment(new Moment(Moment.LORE, Dread.day(lvl), Dread.time(lvl), p.blockPosition(), 100, 0));
        BlockPos tree = vn.cavemind.lore.Lore.part(lvl, "p", "tree");
        if (tree != null) {
            for (Direction d : Direction.Plane.HORIZONTAL) {
                BlockPos q = tree.relative(d, 2);
                if (lvl.getBlockState(q).canBeReplaced() && lvl.getBlockState(q.below()).isFaceSturdy(lvl, q.below(), Direction.UP)) {
                    lvl.setBlock(q, Blocks.CANDLE.defaultBlockState().setValue(CandleBlock.CANDLES, 2).setValue(CandleBlock.LIT, true), 3);
                }
            }
        }
        Fx.sound(p, "minecraft:block.bell.resonate", SoundSource.AMBIENT, p.position(), 0.5f, 1.2f);
        Compat.grant(p, "cavemind:ban_suong/dem_toi_mot_tram");
        vn.cavemind.life.Child.played(p.server, 100);
        vn.cavemind.lore.Endings.check(p);
        PaceLog.encounter(p, "damned", "hundred", "done");
        CaveMind.debug("{} counted to one hundred: the eighth child goes home", p.getGameProfile().getName());
    }

    static void say(ServerPlayer p, String s, ChatFormatting c) {
        p.displayClientMessage(Component.literal(s).withStyle(c, ChatFormatting.ITALIC), true);
    }

    private Hundred() {}
}
