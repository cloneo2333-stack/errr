package vn.cavemind.man;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;
import vn.cavemind.CaveMind;
import vn.cavemind.compat.Compat;
import vn.cavemind.director.Director;
import vn.cavemind.director.Dread;
import vn.cavemind.memory.Moment;
import vn.cavemind.memory.Profile;
import vn.cavemind.sense.Watch;
import vn.cavemind.world.Fx;
import vn.cavemind.world.Spots;

final class Lure {
    private static final String[] OUT = {"minecraft:entity.player.hurt", "minecraft:entity.villager.ambient", "minecraft:entity.cow.ambient",
        "minecraft:entity.sheep.ambient", "minecraft:block.chest.open", "minecraft:block.wooden_door.open", "minecraft:entity.villager.no"};
    private static final String[] CAVE = {"minecraft:entity.player.hurt", "minecraft:block.stone.break", "minecraft:block.chest.open",
        "minecraft:entity.villager.ambient", "minecraft:entity.player.hurt"};

    final String sound;
    final boolean cave;
    Vec3 at;
    int steps, need;
    long nextCall, lastProgress;
    double bearing;

    private Lure(String sound, boolean cave, Vec3 at, int need, long now) {
        this.sound = sound;
        this.cave = cave;
        this.at = at;
        this.need = need;
        this.nextCall = now + 30;
        this.lastProgress = now;
    }

    static Lure make(ServerPlayer p, Watch w, long now) {
        RandomSource r = p.getRandom();
        boolean cave = w.underground;
        String[] pool = cave ? CAVE : OUT;
        double side = r.nextBoolean() ? 1 : -1;
        Spots.Cand c = Spots.find(p, w, new Spots.Spec().ring(cave ? 18 : 26, cave ? 28 : 36).dir(side * (90 + r.nextInt(70)), 40).body(0.6, 2).cave(cave).tries(40)
            .when(k -> k.light <= 8 || k.cover).rank(k -> (8 - Math.min(8, k.light)) * 0.1 + (k.cover ? 0.3 : 0)));
        if (c == null) return null;
        Lure l = new Lure(pool[r.nextInt(pool.length)], cave, Vec3.atBottomCenterOf(c.feet), 3 + r.nextInt(2), now);
        l.bearing = Math.atan2(c.center.z - p.getZ(), c.center.x - p.getX());
        return l;
    }

    boolean run(ServerPlayer p, Watch w, Profile pr, ManMind.Session s, long now) {
        ServerLevel lvl = p.serverLevel();
        RandomSource r = p.getRandom();
        double dist = at.distanceTo(p.position());
        if (now >= nextCall) {
            float pitch = sound.contains("villager") || sound.contains("hurt") ? 0.82f + r.nextFloat() * 0.08f : 0.9f + r.nextFloat() * 0.15f;
            Fx.sound(p, Compat.sound(sound), SoundSource.NEUTRAL, at.add(0, 1.2, 0), cave ? 0.75f : 0.95f, pitch);
            nextCall = now + 70 + r.nextInt(70);
        }
        if (dist < 9) {
            steps++;
            lastProgress = now;
            if (steps >= need) return climax(p, w, pr, s, now);
            double a = bearing + (r.nextDouble() - 0.5) * 1.2;
            double d = 16 + r.nextDouble() * 8;
            int x = Mth.floor(at.x + Math.cos(a) * d), z = Mth.floor(at.z + Math.sin(a) * d);
            BlockPos feet = cave ? Spots.caveFloor(lvl, x, Mth.floor(at.y), z, 2) : Spots.surface(lvl, x, z, 2);
            if (feet == null) return climax(p, w, pr, s, now);
            bearing = Math.atan2(feet.getZ() - at.z, feet.getX() - at.x);
            at = Vec3.atBottomCenterOf(feet);
            nextCall = now + 25 + r.nextInt(30);
            return true;
        }
        if (now - lastProgress > 1800 || dist > 90) {
            return false;
        }
        return true;
    }

    private boolean climax(ServerPlayer p, Watch w, Profile pr, ManMind.Session s, long now) {
        ServerLevel lvl = p.serverLevel();
        pr.moment(new Moment(Moment.LURED, Dread.day(lvl), Dread.time(lvl), BlockPos.containing(at), steps, 0));
        Director.scare(p, 10);
        CaveMind.debug("{} followed the lure to {}", p.getGameProfile().getName(), at);
        boolean ambush = (pr.manLevel >= 7 || pr.anger >= 30 || pr.bold >= 50) && ManMind.huntAllowed(p, w, pr, s);
        if (ambush) {
            ManMind.startHunt(p, w, pr, s, now, BlockPos.containing(at), "dụ bạn vào bẫy");
            return true;
        }
        Spots.Cand c = Spots.find(p, w, new Spots.Spec().ring(10, 16).dir(0, 160).body(ManMind.W, ManMind.H).cave(cave).tries(40)
            .when(k -> ManMind.hidden(k) && k.vis >= 0.35).rank(k -> k.vis - k.center.distanceTo(at) * 0.05));
        if (c == null) return false;
        ManMind.retarget(p, s, ManMind.Tac.WATCH, now);
        if (!ManMind.spawnBody(p, s, c.feet)) return false;
        return true;
    }
}
