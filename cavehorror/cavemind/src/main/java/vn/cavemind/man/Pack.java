package vn.cavemind.man;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.block.AbstractCandleBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.registries.ForgeRegistries;
import vn.cavemind.CaveMind;
import vn.cavemind.compat.Compat;
import vn.cavemind.director.Director;
import vn.cavemind.director.Dread;
import vn.cavemind.memory.MindData;
import vn.cavemind.memory.Moment;
import vn.cavemind.memory.Profile;
import vn.cavemind.net.Net;
import vn.cavemind.sense.Group;
import vn.cavemind.sense.Sight;
import vn.cavemind.sense.Watch;
import vn.cavemind.world.Fx;
import vn.cavemind.world.Spots;

final class Pack {
    static final double NEAR = 30;

    static List<ServerPlayer> members(ServerPlayer p) {
        List<ServerPlayer> out = new ArrayList<>();
        out.add(p);
        for (ServerPlayer o : Group.others(p, NEAR)) if (!Compat.exempt(o)) out.add(o);
        return out;
    }

    static ServerPlayer pickPrey(ServerPlayer p) {
        List<ServerPlayer> g = members(p);
        if (g.size() < 2) return p;
        Vec3 c = centroid(g);
        ServerPlayer best = p;
        double bs = -1e9;
        for (ServerPlayer o : g) {
            if (!Director.free(o) && o != p) continue;
            double edge = o.position().distanceTo(c);
            double hurt = 1 - o.getHealth() / o.getMaxHealth();
            double s = edge * 0.15 + hurt * 2 + (Watch.of(o).light <= 6 ? 0.6 : 0) + o.getRandom().nextDouble() * 0.8;
            if (s > bs) {
                bs = s;
                best = o;
            }
        }
        return best;
    }

    static Vec3 centroid(List<ServerPlayer> g) {
        Vec3 c = Vec3.ZERO;
        for (ServerPlayer o : g) c = c.add(o.position());
        return c.scale(1.0 / g.size());
    }

    static void sayAll(List<ServerPlayer> g, String msg) {
    }

    static boolean straggler(ServerPlayer p) {
        double iso = Group.isolation(p);
        return iso >= 10 && iso < 40;
    }

    static boolean setupMark(ServerPlayer p, Watch w, Profile pr, ManMind.Session s, long now) {
        List<ServerPlayer> g = members(p);
        if (g.size() < 2) return false;
        Spots.Cand c = Spots.find(p, w, new Spots.Spec().ring(20, 38).dir(0, 180).body(ManMind.W, ManMind.H).cave(w.underground).tries(60)
            .when(k -> !k.sight.on && k.vis >= 0.4 && (k.light <= 8 || k.cover) && !Group.boxSeenByOthers(p, k.box, 80)).rank(k -> k.vis + (8 - k.light) * 0.05));
        if (c == null) return false;
        Entity e = ManMind.make(p, Compat.MAN_STALKER, c.feet, false);
        if (e == null) return false;
        s.body = e.getUUID();
        s.marked = null;
        sayAll(g, "Hắn đứng trong bóng tối, cả nhóm đều có thể thấy hắn. Hắn sẽ chọn một người");
        return true;
    }

    static boolean runMark(ServerPlayer p, Watch w, Profile pr, ManMind.Session s, Mob m, long now) {
        List<ServerPlayer> g = members(p);
        boolean any = false;
        ServerPlayer looker = null;
        for (ServerPlayer o : g) {
            Sight si = Sight.of(o, Watch.of(o), m.getBoundingBox().getCenter());
            if (si.on && Sight.visible(o, m) > 0.15) {
                any = true;
                if (si.central()) looker = o;
            }
        }
        s.away = any ? 0 : s.away + 1;
        if (!s.noticed) {
            if (looker != null) s.perceive += 1.2;
            else if (any) s.perceive += 0.4;
            else s.perceive = Math.max(0, s.perceive - 0.2);
            if (s.perceive >= 6) {
                s.noticed = true;
                s.noticedAt = now;
                s.marked = p.getUUID();
                Compat.anim(m, "cm_reach");
                for (ServerPlayer o : g) {
                    MindData.profile(o).moment(new Moment(Moment.SEEN, Dread.day(o.serverLevel()), Dread.time(o.serverLevel()), o.blockPosition(), (int) o.distanceTo(m), 9));
                    Director.scare(o, o == p ? 16 : 8);
                }
                pr.anger(10);
                Fx.soundAll(p.serverLevel(), "man:long_whisper", SoundSource.HOSTILE, m.getEyePosition(), 0.8f, 0.6f);
                sayAll(g, "Hắn giơ tay chỉ vào " + p.getGameProfile().getName() + ". Đêm nay hắn chọn người đó");
            }
            ManMind.face(m, p.getEyePosition());
            return now - s.tacStart < 1600;
        }
        ManMind.face(m, p.getEyePosition());
        if (s.away >= 3 || now - s.noticedAt > 200) {
            Fx.soundAll(p.serverLevel(), "man:vanish", SoundSource.HOSTILE, m.position(), 0.8f, 0.8f);
            if (p.getRandom().nextFloat() < 0.5f) s.wantHunt = true;
            return false;
        }
        return true;
    }

    static boolean lit(BlockState st) {
        var id = ForgeRegistries.BLOCKS.getKey(st.getBlock());
        if (id == null) return false;
        String path = id.getPath();
        if (path.contains("redstone")) return false;
        if (st.getBlock() instanceof CampfireBlock || st.getBlock() instanceof AbstractCandleBlock) {
            return st.hasProperty(BlockStateProperties.LIT) && st.getValue(BlockStateProperties.LIT);
        }
        return path.contains("torch") || path.contains("lantern");
    }

    static boolean setupDouse(ServerPlayer p, Watch w, Profile pr, ManMind.Session s, long now) {
        List<ServerPlayer> g = members(p);
        if (g.size() < 2) return false;
        ServerLevel lvl = p.serverLevel();
        Vec3 c = centroid(g);
        if (pr.home != null && pr.home.distSqr(BlockPos.containing(c)) < 16 * 16) return false;
        List<BlockPos> lights = new ArrayList<>();
        BlockPos base = BlockPos.containing(c);
        for (BlockPos q : BlockPos.betweenClosed(base.offset(-18, -6, -18), base.offset(18, 8, 18))) {
            if (lit(lvl.getBlockState(q))) lights.add(q.immutable());
        }
        if (lights.size() < 3) return false;
        lights.sort(Comparator.comparingDouble((BlockPos q) -> -q.distToCenterSqr(c)));
        s.lights = lights.subList(0, Math.min(7, lights.size()));
        s.doused = 0;
        s.nextDouse = now + 30;
        sayAll(g, "Những nguồn sáng quanh nhóm sắp tắt dần, từ xa tới gần");
        return true;
    }

    static boolean runDouse(ServerPlayer p, Watch w, Profile pr, ManMind.Session s, long now) {
        ServerLevel lvl = p.serverLevel();
        if (s.lights == null || s.doused >= s.lights.size()) {
            List<ServerPlayer> g = members(p);
            for (ServerPlayer o : g) Director.scare(o, 8);
            if (ManMind.huntAllowed(p, w, pr, s)) {
                s.wantHunt = true;
                sayAll(g, "Ánh sáng cuối cùng đã tắt");
            }
            return false;
        }
        if (now < s.nextDouse) return true;
        BlockPos q = s.lights.get(s.doused++);
        s.nextDouse = now + 14 + p.getRandom().nextInt(14);
        BlockState st = lvl.getBlockState(q);
        if (!lit(st)) return true;
        if (st.getBlock() instanceof CampfireBlock || st.getBlock() instanceof AbstractCandleBlock) {
            lvl.setBlock(q, st.setValue(BlockStateProperties.LIT, false), 3);
        } else {
            lvl.removeBlock(q, false);
            pr.dStolen++;
        }
        Fx.soundAll(lvl, SoundEvents.FIRE_EXTINGUISH.getLocation().toString(), SoundSource.BLOCKS, Vec3.atCenterOf(q), 0.6f, 0.7f);
        return true;
    }

    static boolean setupDrag(ServerPlayer p, Watch w, Profile pr, ManMind.Session s, long now) {
        if (!straggler(p) || w.light > 7 && !shaded(p) || p.isInWater() || p.isPassenger()) return false;
        ServerLevel lvl = p.serverLevel();
        ServerPlayer near = Group.nearestOther(p);
        if (near == null) return false;
        Vec3 away = p.position().subtract(near.position()).multiply(1, 0, 1);
        if (away.lengthSqr() < 1e-3) return false;
        away = away.normalize();
        Vec3 dest = p.position();
        for (int i = 1; i <= 8; i++) {
            Vec3 q = p.position().add(away.scale(i));
            BlockPos f = Spots.nearFloor(lvl, BlockPos.containing(q), 2, 1);
            if (f == null || !lvl.getFluidState(f).isEmpty() || !lvl.getFluidState(f.below()).isEmpty()) break;
            dest = Vec3.atBottomCenterOf(f);
        }
        if (dest.distanceTo(p.position()) < 2.5) return false;
        BlockPos at = Spots.nearFloor(lvl, BlockPos.containing(dest.add(away.scale(1.2))), 1, 1);
        if (at == null) at = BlockPos.containing(dest);
        Entity e = ManMind.make(p, Compat.MAN_CRAWLER, at, false);
        if (e == null) return false;
        e.getPersistentData().putString(Compat.TAG, "man");
        s.body = e.getUUID();
        s.dragTo = dest;
        s.dragDir = away;
        s.grip = GRIP;
        if (e instanceof Mob m) {
            m.setAggressive(true);
            vn.cavemind.world.Motion.snap(m, p.getEyePosition());
        }
        Compat.anim(e, "cm_twitch");
        return true;
    }

    // The grab now lasts about three seconds instead of under one: a hard yank, then a slow drag towards the dark,
    // long enough for someone to turn round and cut him loose (cut()).
    static final int DRAG_T = 64, GRIP = 4;

    static boolean runDrag(ServerPlayer p, Watch w, Profile pr, ManMind.Session s, Mob m, long now) {
        long t = now - s.tacStart;
        ServerLevel lvl = p.serverLevel();
        if (t == 2) {
            Fx.soundAll(lvl, "man:scream_loop", SoundSource.HOSTILE, m.position(), 1.6f, 1.3f);
            Net.fx(p, Net.SHAKE, 2.5f, 14);
            p.hurt(lvl.damageSources().mobAttack((LivingEntity) m), 2f);
            List<ServerPlayer> g = new ArrayList<>(Group.others(p, 50));
            g.add(p);
            sayAll(g, "Hắn túm lấy " + p.getGameProfile().getName() + " từ phía sau và lôi vào bóng tối");
            for (ServerPlayer o : g) Director.scare(o, o == p ? 25 : 12);
        }
        if (t >= 3 && t < DRAG_T && s.dragDir != null) {
            boolean yank = t <= 6;
            boolean there = s.dragTo != null && p.position().distanceToSqr(s.dragTo) < 0.36;
            Vec3 v = there ? Vec3.ZERO : s.dragDir.scale(yank ? 0.38 : 0.1);
            p.setDeltaMovement(v.x, Math.max(p.getDeltaMovement().y, 0.02), v.z);
            p.hurtMarked = true;
            Vec3 behind = p.position().add(s.dragDir.scale(1.1));
            m.teleportTo(behind.x, m.getY(), behind.z);
            if (t % 16 == 0) Fx.soundAll(lvl, "minecraft:block.gravel.step", SoundSource.HOSTILE, p.position(), 0.7f, 0.6f);
            if (t == 40) p.hurt(lvl.damageSources().mobAttack((LivingEntity) m), 1f);
        }
        if (t >= DRAG_T) {
            Fx.soundAll(lvl, "man:vanish", SoundSource.HOSTILE, m.position(), 1f, 0.8f);
            pr.moment(new Moment(Moment.FRENZY, Dread.day(lvl), Dread.time(lvl), p.blockPosition(), 1, 0));
            s.dragged = true;
            if (ManMind.huntAllowed(p, w, pr, s) && p.getRandom().nextFloat() < 0.65f) s.wantHunt = true;
            return false;
        }
        return true;
    }

    private static final java.util.Map<UUID, Long> CUT_DAY = new java.util.HashMap<>();

    // A blow at the arm that holds someone. A heavy blow cuts it at once, full swings from a friend take two, rushed
    // swings four; the one being dragged can only hit behind them, so their own blows count half. Cutting someone
    // loose costs the rescuer: he lets go, but now he wants them (marked for the next frenzy, angrier with them), and
    // once a day the cut leaves a fog bone shard behind.
    static void cut(ServerPlayer victim, ServerPlayer by, ManMind.Session s, Mob m, vn.cavemind.sense.Swing.Blow b, long now) {
        ServerLevel lvl = victim.serverLevel();
        int w = b == null ? 1 : b.weight();
        if (by == victim) w /= 2;
        if (w <= 0) return;
        s.grip -= w;
        Fx.soundAll(lvl, "minecraft:entity.player.attack.crit", SoundSource.HOSTILE, m.position(), 0.8f, 0.6f);
        if (s.grip > 0) {
            vn.cavemind.compat.Compat.anim(m, "cm_twitch");
            return;
        }
        Vec3 at = m.position();
        Fx.soundAll(lvl, "man:scream_loop", SoundSource.HOSTILE, at, 1.4f, 1.6f);
        Fx.soundAll(lvl, "man:vanish", SoundSource.HOSTILE, at, 1f, 0.7f);
        lvl.sendParticles(net.minecraft.core.particles.ParticleTypes.LARGE_SMOKE, at.x, at.y + 0.8, at.z, 18, 0.3, 0.4, 0.3, 0.02);
        victim.setDeltaMovement(0, victim.getDeltaMovement().y, 0);
        victim.hurtMarked = true;
        Net.fx(victim, Net.SHAKE, 1.2f, 8);
        if (by != victim) Net.fx(by, Net.SHAKE, 1.2f, 8);
        m.discard();
        s.body = null;
        s.tac = null;
        s.dragged = true;
        s.nextTac = now + 200 + victim.getRandom().nextInt(300);
        if (by != victim) {
            s.marked = by.getUUID();
            MindData.profile(by).anger(10);
            Profile vp = MindData.profile(victim);
            if (ManMind.huntAllowed(victim, Watch.of(victim), vp, s)) s.wantHunt = true;
            victim.displayClientMessage(net.minecraft.network.chat.Component.literal(by.getGameProfile().getName() + " chặt đứt tay hắn, kéo bạn lại.")
                .withStyle(net.minecraft.ChatFormatting.GRAY, net.minecraft.ChatFormatting.ITALIC), true);
            by.displayClientMessage(net.minecraft.network.chat.Component.literal("Bạn chặt đứt tay hắn. Hắn nhìn bạn rất lâu trước khi tan đi.")
                .withStyle(net.minecraft.ChatFormatting.DARK_RED, net.minecraft.ChatFormatting.ITALIC), true);
        }
        long day = Dread.day(lvl);
        Long last = CUT_DAY.get(by.getUUID());
        if ((last == null || last != day) && victim.getRandom().nextFloat() < (b != null && b.heavy() ? 0.7f : 0.35f)) {
            CUT_DAY.put(by.getUUID(), day);
            net.minecraft.world.entity.item.ItemEntity ie = new net.minecraft.world.entity.item.ItemEntity(lvl, at.x, at.y + 0.5, at.z,
                new net.minecraft.world.item.ItemStack(vn.cavemind.gear.Gear.BONE.get()));
            ie.setDefaultPickUpDelay();
            lvl.addFreshEntity(ie);
        }
        vn.cavemind.director.PaceLog.encounter(victim, "man", "drag cut", by.getGameProfile().getName());
        CaveMind.debug("{} cut {} loose from the Man", by.getGameProfile().getName(), victim.getGameProfile().getName());
    }

    private Pack() {}

    static boolean shaded(ServerPlayer p) {
        BlockPos eye = BlockPos.containing(p.getEyePosition());
        return Watch.held(p) <= 7 && !p.serverLevel().canSeeSky(eye) && p.serverLevel().getBrightness(net.minecraft.world.level.LightLayer.BLOCK, eye) <= 7;
    }
}
