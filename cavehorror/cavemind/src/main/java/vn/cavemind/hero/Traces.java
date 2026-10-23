package vn.cavemind.hero;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.StandingSignBlock;
import net.minecraft.world.level.block.WallTorchBlock;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.entity.SignText;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.RotationSegment;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import vn.cavemind.CaveMind;
import vn.cavemind.director.Dread;
import vn.cavemind.memory.Moment;
import vn.cavemind.memory.Profile;
import vn.cavemind.sense.Sight;
import vn.cavemind.sense.Watch;
import vn.cavemind.world.Spots;

final class Traces {
    static String vi(String k) {
        return switch (k) {
            case "sign" -> "tấm biển biết rõ thói quen của bạn";
            case "tunnel" -> "đường hầm bạn bỏ dở đã được đào tiếp";
            case "leafless" -> "cái cây gần nhà bị tuốt sạch lá";
            default -> k;
        };
    }

    static boolean run(ServerPlayer p, Watch w, Profile pr, String k, boolean force) {
        try {
            return switch (k) {
                case "sign" -> sign(p, w, pr, force);
                case "tunnel" -> tunnel(p, w, pr, force);
                case "leafless" -> leafless(p, w, pr, force);
                default -> false;
            };
        } catch (Throwable t) {
            CaveMind.LOG.warn("[CaveMind] herobrine trace {} failed", k, t);
            return false;
        }
    }

    static void clue(ServerPlayer p, Profile pr, String kind, BlockPos at, int radius) {
        pr.clue(new Profile.Clue(kind, at.immutable(), Dread.day(p.serverLevel()), radius));
    }

    static boolean seen(ServerPlayer p, Watch w, BlockPos q) {
        Vec3 c = Vec3.atCenterOf(q);
        Sight si = Sight.of(p, w, c);
        return si.on && Sight.clear(p.level(), p.getEyePosition(), c, p) || vn.cavemind.sense.Group.seenByOthers(p, c, 96);
    }

    private static boolean far(ServerPlayer p, Watch w, BlockPos q, double min, boolean force) {
        if (force) return true;
        for (ServerPlayer o : vn.cavemind.sense.Group.others(p, 512)) if (o.distanceToSqr(Vec3.atCenterOf(q)) < min * min * 0.5) return false;
        if (vn.cavemind.sense.Group.seenByOthers(p, Vec3.atCenterOf(q), 96)) return false;
        if (p.isSleeping()) return true;
        return Math.sqrt(q.distSqr(p.blockPosition())) >= min && !seen(p, w, q) && !seen(p, w, q.above());
    }

    static boolean sign(ServerPlayer p, Watch w, Profile pr, boolean force) {
        List<String> kinds = new ArrayList<>(List.of("saw", "grave", "back", "sleep", "mined", "walked"));
        Collections.shuffle(kinds, new java.util.Random(p.getRandom().nextLong()));
        if (pr.heroCalled >= 0) kinds.add(0, "called");
        for (String k : kinds) {
            if (signOf(p, w, pr, k, force)) return true;
        }
        return false;
    }

    private static boolean signOf(ServerPlayer p, Watch w, Profile pr, String k, boolean force) {
        ServerLevel lvl = p.serverLevel();
        long nowAbs = lvl.getDayTime();
        switch (k) {
            case "called" -> {
                long day = Dread.day(lvl);
                if (day <= pr.heroCalled / 24000L) return false;
                List<long[]> list = new ArrayList<>(w.crumbs);
                for (int i = list.size() - 1; i >= 0; i--) {
                    BlockPos at = BlockPos.of(list.get(i)[0]);
                    double d = Math.sqrt(at.distSqr(p.blockPosition()));
                    if (d < 24) continue;
                    if (d > 70) break;
                    if (!far(p, w, at, 24, force)) continue;
                    if (place(p, w, at, p.position(), Component.literal("Lúc " + clock((int) (pr.heroCalled % 24000L))), Component.literal("bạn gọi tên tôi."),
                        Component.literal("Tôi nghe thấy."), Component.empty())) {
                        pr.heroCalled = -1;
                        done(p, pr, at, "called");
                        return true;
                    }
                }
            }
            case "saw" -> {
                List<long[]> d = new ArrayList<>(pr.diary);
                Collections.shuffle(d, new java.util.Random(p.getRandom().nextLong()));
                for (long[] e : d) {
                    long age = nowAbs - (e[1] * 24000 + e[2]);
                    if (age < (12000)) continue;
                    BlockPos at = BlockPos.of(e[0]);
                    if (!lvl.isLoaded(at) || !far(p, w, at, 36, force)) continue;
                    if (place(p, w, at, p.position(), Component.translatable("cavemind.hero.sign.saw1"), Component.translatable("cavemind.hero.sign.saw2"),
                        Component.translatable("cavemind.hero.sign.when", e[1] + 1, clock((int) e[2])), Component.empty())) {
                        done(p, pr, at, "saw");
                        return true;
                    }
                }
            }
            case "grave" -> {
                for (int i = pr.moments.size() - 1; i >= 0; i--) {
                    Moment m = pr.moments.get(i);
                    if (!m.kind().equals(Moment.DIED)) continue;
                    BlockPos at = m.pos();
                    if (!lvl.isLoaded(at) || !far(p, w, at, 30, force)) return false;
                    if (place(p, w, at, p.position(), Component.translatable("cavemind.hero.sign.died1"), Component.translatable("cavemind.hero.sign.died2"),
                        Component.translatable("cavemind.hero.sign.when", m.day() + 1, clock(m.time())), Component.empty())) {
                        BlockPos under = at.below();
                        BlockState st = lvl.getBlockState(under);
                        if (st.is(Blocks.GRASS_BLOCK) || st.is(Blocks.DIRT)) lvl.setBlock(under, Blocks.COARSE_DIRT.defaultBlockState(), 3);
                        done(p, pr, at, "grave");
                        return true;
                    }
                    return false;
                }
            }
            case "back" -> {
                long best = 0;
                int bv = 0;
                for (Map.Entry<Long, Integer> e : pr.heat.entrySet()) {
                    if (e.getValue() <= bv) continue;
                    long key = e.getKey();
                    BlockPos c = new BlockPos((int) (key >> 32) * 16 + 8, 64, (int) key * 16 + 8);
                    if (pr.home != null && Math.abs(c.getX() - pr.home.getX()) < 48 && Math.abs(c.getZ() - pr.home.getZ()) < 48) continue;
                    best = e.getKey();
                    bv = e.getValue();
                }
                if (bv < 6) return false;
                int cx = (int) (best >> 32), cz = (int) best;
                BlockPos at = Spots.surface(lvl, cx * 16 + 8, cz * 16 + 8, 2);
                if (at == null || !far(p, w, at, 40, force)) return false;
                if (place(p, w, at, p.position(), Component.translatable("cavemind.hero.sign.back1"), Component.translatable("cavemind.hero.sign.back2"),
                    Component.translatable("cavemind.hero.sign.back3"), Component.translatable("cavemind.hero.sign.back4"))) {
                    done(p, pr, at, "back");
                    return true;
                }
            }
            case "sleep" -> {
                if (pr.home == null) return false;
                Moment slept = null;
                for (int i = pr.moments.size() - 1; i >= 0 && slept == null; i--) if (pr.moments.get(i).kind().equals(Moment.SLEPT)) slept = pr.moments.get(i);
                if (slept == null) return false;
                RandomSource r = p.getRandom();
                for (int t = 0; t < 20; t++) {
                    double a = r.nextDouble() * Math.PI * 2;
                    BlockPos at = Spots.surface(lvl, Mth.floor(pr.home.getX() + Math.cos(a) * 9), Mth.floor(pr.home.getZ() + Math.sin(a) * 9), 2);
                    if (at == null || !lvl.canSeeSky(at) || !far(p, w, at, 36, force)) continue;
                    if (place(p, w, at, Vec3.atCenterOf(pr.home), Component.translatable("cavemind.hero.sign.sleep1"), Component.translatable("cavemind.hero.sign.sleep2"),
                        Component.translatable("cavemind.dir." + Direction.from2DDataValue(slept.a()).getName()), Component.empty())) {
                        done(p, pr, at, "sleep");
                        return true;
                    }
                }
            }
            case "mined" -> {
                int n = 0;
                for (Block b : new Block[]{Blocks.STONE, Blocks.DEEPSLATE, Blocks.ANDESITE, Blocks.DIORITE, Blocks.GRANITE, Blocks.TUFF}) n += p.getStats().getValue(Stats.BLOCK_MINED.get(b));
                if (n < 64) return false;
                BlockPos at = null;
                long[] prev = null;
                for (long[] c : w.crumbs) {
                    if (prev != null && prev[2] == 0 && c[2] == 1) at = BlockPos.of(prev[0]);
                    prev = c;
                }
                if (at == null || !lvl.isLoaded(at) || !far(p, w, at, 30, force)) return false;
                if (place(p, w, at, p.position(), Component.translatable("cavemind.hero.sign.mined1"), Component.translatable("cavemind.hero.sign.mined2", n),
                    Component.translatable("cavemind.hero.sign.mined3"), Component.translatable("cavemind.hero.sign.mined4"))) {
                    done(p, pr, at, "mined");
                    return true;
                }
            }
            case "walked" -> {
                int cm = p.getStats().getValue(Stats.CUSTOM.get(Stats.WALK_ONE_CM)) + p.getStats().getValue(Stats.CUSTOM.get(Stats.SPRINT_ONE_CM));
                if (cm < 50000) return false;
                List<long[]> list = new ArrayList<>(w.crumbs);
                for (int i = list.size() - 1; i >= 0; i--) {
                    BlockPos at = BlockPos.of(list.get(i)[0]);
                    double d = Math.sqrt(at.distSqr(p.blockPosition()));
                    if (d < 40) continue;
                    if (d > 90) break;
                    if (list.get(i)[2] == 1 || !far(p, w, at, 40, force)) continue;
                    if (place(p, w, at, p.position(), Component.translatable("cavemind.hero.sign.walked1"), Component.translatable("cavemind.hero.sign.walked2", cm / 100),
                        Component.translatable("cavemind.hero.sign.walked3"), Component.translatable("cavemind.hero.sign.walked4"))) {
                        done(p, pr, at, "walked");
                        return true;
                    }
                }
            }
            default -> {}
        }
        return false;
    }

    private static void done(ServerPlayer p, Profile pr, BlockPos at, String what) {
        clue(p, pr, Moment.HERO_SIGN, at, 10);
        CaveMind.debug("herobrine left a sign ({}) at {} for {}", what, at, p.getGameProfile().getName());
    }

    static boolean place(ServerPlayer p, Watch w, BlockPos at, Vec3 viewer, Component... lines) {
        ServerLevel lvl = p.serverLevel();
        BlockPos feet = Spots.standable(lvl, at, 2) ? at : Spots.nearFloor(lvl, at, 2, 3);
        if (feet == null) return false;
        BlockState here = lvl.getBlockState(feet);
        if (!here.canBeReplaced() || !lvl.getFluidState(feet).isEmpty()) return false;
        Vec3 c = Vec3.atBottomCenterOf(feet);
        float yaw = Herobrine.yawTo(viewer, c);
        BlockState st = Blocks.DARK_OAK_SIGN.defaultBlockState().setValue(StandingSignBlock.ROTATION, RotationSegment.convertToSegment(yaw + 180f));
        lvl.setBlock(feet, st, 3);
        if (lvl.getBlockEntity(feet) instanceof SignBlockEntity sb) {
            SignText t = sb.getFrontText();
            for (int i = 0; i < Math.min(4, lines.length); i++) t = t.setMessage(i, lines[i]);
            sb.setText(t, true);
            sb.setWaxed(true);
            sb.setChanged();
            lvl.sendBlockUpdated(feet, st, st, 3);
        }
        return true;
    }

    static String clock(int tod) {
        int h = (tod / 1000 + 6) % 24;
        int min = (tod % 1000) * 60 / 1000;
        return String.format("%02d:%02d", h, min);
    }

    static boolean natural(ServerLevel lvl, BlockPos q) {
        BlockState st = lvl.getBlockState(q);
        if (st.hasBlockEntity() || !st.isCollisionShapeFullBlock(lvl, q)) return false;
        if (!(st.is(BlockTags.BASE_STONE_OVERWORLD) || st.is(BlockTags.DIRT) || st.is(Blocks.GRAVEL) || st.is(BlockTags.STONE_ORE_REPLACEABLES)
            || st.is(BlockTags.DEEPSLATE_ORE_REPLACEABLES))) return false;
        for (Direction d : Direction.values()) if (!lvl.getFluidState(q.relative(d)).isEmpty()) return false;
        return true;
    }

    private static boolean air(ServerLevel lvl, BlockPos q) {
        return lvl.getBlockState(q).getCollisionShape(lvl, q).isEmpty() && lvl.getFluidState(q).isEmpty();
    }

    static int spacing(Profile pr) {
        List<Long> t = new ArrayList<>(pr.placedTorches);
        double sum = 0;
        int n = 0;
        for (int i = 1; i < t.size(); i++) {
            double d = Math.sqrt(BlockPos.of(t.get(i)).distSqr(BlockPos.of(t.get(i - 1))));
            if (d >= 2 && d <= 20) {
                sum += d;
                n++;
            }
        }
        return n == 0 ? 8 : Mth.clamp((int) Math.round(sum / n), 4, 14);
    }

    static boolean tunnel(ServerPlayer p, Watch w, Profile pr, boolean force) {
        ServerLevel lvl = p.serverLevel();
        List<Long> log = new ArrayList<>(pr.brokenLog);
        Set<Long> set = new HashSet<>(log);
        RandomSource r = p.getRandom();
        for (int i = log.size() - 1; i >= Math.max(0, log.size() - 160); i--) {
            BlockPos q = BlockPos.of(log.get(i));
            if (!lvl.isLoaded(q) || lvl.canSeeSky(q)) continue;
            for (Direction d : Direction.Plane.HORIZONTAL) {
                int back = 0;
                while (back < 12 && set.contains(q.relative(d, -(back + 1)).asLong())) back++;
                if (back < 4) continue;
                BlockPos end = q;
                int fwd = 0;
                while (fwd < 40 && set.contains(end.relative(d).asLong())) {
                    end = end.relative(d);
                    fwd++;
                }
                BlockPos feet = set.contains(end.below().asLong()) && air(lvl, end.below()) ? end.below() : end;
                if (!air(lvl, feet) || !air(lvl, feet.above())) continue;
                BlockPos next = feet.relative(d);
                if (!natural(lvl, next) || !natural(lvl, next.above())) continue;
                if (!far(p, w, next, 32, force)) continue;
                int len = 10 + r.nextInt(8), every = spacing(pr), carved = 0;
                Direction side = d.getClockWise();
                for (int k = 1; k <= len; k++) {
                    BlockPos a = feet.relative(d, k);
                    if (!natural(lvl, a) || !natural(lvl, a.above())) break;
                    lvl.setBlock(a, Blocks.AIR.defaultBlockState(), 3);
                    lvl.setBlock(a.above(), Blocks.AIR.defaultBlockState(), 3);
                    carved++;
                    if (k % every == 0 && lvl.getBlockState(a.above().relative(side)).isFaceSturdy(lvl, a.above().relative(side), side.getOpposite())) {
                        lvl.setBlock(a.above(), Blocks.WALL_TORCH.defaultBlockState().setValue(WallTorchBlock.FACING, side.getOpposite()), 3);
                    }
                }
                if (carved < 4) continue;
                clue(p, pr, Moment.HERO_TUNNEL, next, 8);
                CaveMind.debug("herobrine continued a tunnel at {} for {}", next, p.getGameProfile().getName());
                return true;
            }
        }
        return false;
    }

    static boolean leafless(ServerPlayer p, Watch w, Profile pr, boolean force) {
        ServerLevel lvl = p.serverLevel();
        BlockPos h = pr.home;
        if (h == null || !lvl.isLoaded(h)) return false;
        if (!force && !p.isSleeping() && Math.sqrt(h.distSqr(p.blockPosition())) < 40) return false;
        BlockPos start = null;
        double bd = 1e9;
        for (BlockPos q : BlockPos.betweenClosed(h.offset(-20, -4, -20), h.offset(20, 16, 20))) {
            BlockState st = lvl.getBlockState(q);
            if (!(st.getBlock() instanceof LeavesBlock) || st.getValue(LeavesBlock.PERSISTENT)) continue;
            if (!lvl.getBlockState(q.below()).is(BlockTags.LOGS) && !lvl.getBlockState(q.relative(Direction.NORTH)).is(BlockTags.LOGS)
                && !lvl.getBlockState(q.relative(Direction.SOUTH)).is(BlockTags.LOGS) && !lvl.getBlockState(q.relative(Direction.EAST)).is(BlockTags.LOGS)
                && !lvl.getBlockState(q.relative(Direction.WEST)).is(BlockTags.LOGS)) continue;
            double d = q.distSqr(h);
            if (d < 36 || d >= bd) continue;
            bd = d;
            start = q.immutable();
        }
        if (start == null || !far(p, w, start, 40, force)) return false;
        Deque<BlockPos> open = new ArrayDeque<>();
        Set<BlockPos> seen = new HashSet<>();
        open.add(start);
        seen.add(start);
        int removed = 0;
        while (!open.isEmpty() && removed < 320) {
            BlockPos q = open.poll();
            BlockState st = lvl.getBlockState(q);
            if (!(st.getBlock() instanceof LeavesBlock) || st.getValue(LeavesBlock.PERSISTENT)) continue;
            lvl.setBlock(q, Blocks.AIR.defaultBlockState(), 2);
            removed++;
            for (Direction d : Direction.values()) {
                BlockPos n = q.relative(d);
                if (n.distSqr(start) > 36 || !seen.add(n)) continue;
                open.add(n);
            }
        }
        if (removed < 15) return false;
        clue(p, pr, Moment.HERO_LEAFLESS, start, 30);
        CaveMind.debug("herobrine stripped a tree at {} for {}", start, p.getGameProfile().getName());
        return true;
    }

    private Traces() {}
}
