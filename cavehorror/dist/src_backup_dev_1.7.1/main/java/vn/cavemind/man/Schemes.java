package vn.cavemind.man;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Container;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.Chicken;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.entity.animal.Rabbit;
import net.minecraft.world.entity.animal.Sheep;
import net.minecraft.world.entity.animal.goat.Goat;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.PickaxeItem;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.registries.ForgeRegistries;
import vn.cavemind.CaveMind;
import vn.cavemind.Cfg;
import vn.cavemind.compat.Compat;
import vn.cavemind.dev.Dev;
import vn.cavemind.director.Director;
import vn.cavemind.director.Dread;
import vn.cavemind.memory.MindData;
import vn.cavemind.memory.Moment;
import vn.cavemind.memory.Profile;
import vn.cavemind.sense.Sight;
import vn.cavemind.sense.Watch;
import vn.cavemind.world.Fx;
import vn.cavemind.world.Spots;

public final class Schemes {
    public static final String[] ALL = {"trail", "nest", "spoil", "harvest", "peep", "seal", "animal", "wake"};

    public static void tick(ServerPlayer p, Watch w, Profile pr, long now) {
        if (now % 10 == 0) clues(p, w, pr, now);
        if (now % 40 != 0) return;
        ServerLevel lvl = p.serverLevel();
        long day = Dread.day(lvl);
        if (pr.angerDay != day) {
            if (pr.angerDay >= 0) {
                pr.anger(-6 * (int) Math.min(5, day - pr.angerDay));
                pr.bold(-4 * (int) Math.min(5, day - pr.angerDay));
            }
            pr.angerDay = day;
        }
        if (pr.manLevel < 1 && !Dev.on()) return;
        if (pr.schemeNext < 0) pr.schemeNext = pr.playTicks + (Dev.on() ? 600 : 6000);
        if (pr.playTicks < pr.schemeNext) return;
        if (lvl.dimension() != Level.OVERWORLD || Compat.exempt(p)) return;
        String k = pick(p, w, pr);
        if (k == null) {
            pr.schemeNext = pr.playTicks + 1200;
            return;
        }
        if (run(p, w, pr, k, false)) {
            pr.schemes++;
            double gap = Dev.on() ? 1800 + p.getRandom().nextInt(1800) : (9000 + p.getRandom().nextInt(9000)) * (1.0 - Math.min(0.4, pr.manLevel * 0.04)) / Cfg.INTENSITY.get();
            pr.schemeNext = pr.playTicks + (long) gap;
        } else {
            pr.schemeNext = pr.playTicks + 400;
        }
    }

    static String pick(ServerPlayer p, Watch w, Profile pr) {
        int L = Dev.on() ? 10 : pr.manLevel;
        List<String> o = new ArrayList<>();
        List<Double> wt = new ArrayList<>();
        boolean home = pr.home != null && p.serverLevel().isLoaded(pr.home);
        boolean work = Cfg.MAN_SCHEMES.get(), steal = Cfg.MAN_STEAL.get();
        if (work && w.underground && L >= 3 && underCrumbs(w) >= 14) add(o, wt, "seal", 2.2);
        if (work && home && !pr.trailDone) add(o, wt, "trail", 1.3);
        if (work && home && L >= 2 && pr.trailFrom != null && pr.nest == null) add(o, wt, "nest", 1.2);
        if (steal && L >= 2 && !pr.chests.isEmpty()) add(o, wt, "spoil", 0.6);
        if (steal && home && L >= 2) add(o, wt, "harvest", 0.6);
        if (work && home && L >= 3 && pr.peep == null) add(o, wt, "peep", 0.9);
        Long took = pr.heroLast.get("man_animal");
        if (steal && home && L >= 4 && (took == null || Dread.day(p.serverLevel()) - took >= 5)) add(o, wt, "animal", 0.4);
        if (o.isEmpty()) return null;
        double sum = 0;
        for (double d : wt) sum += d;
        double x = p.getRandom().nextDouble() * sum;
        for (int i = 0; i < o.size(); i++) {
            x -= wt.get(i);
            if (x <= 0) return o.get(i);
        }
        return o.get(o.size() - 1);
    }

    private static void add(List<String> o, List<Double> w, String k, double v) {
        o.add(k);
        w.add(v);
    }

    public static boolean run(ServerPlayer p, Watch w, Profile pr, String k, boolean force) {
        boolean ok;
        try {
            ok = switch (k) {
                case "trail" -> trail(p, w, pr, force);
                case "nest" -> nest(p, w, pr, force);
                case "spoil" -> spoil(p, w, pr, force, null);
                case "harvest" -> harvest(p, w, pr, force);
                case "peep" -> peep(p, w, pr, force);
                case "seal" -> seal(p, w, pr, force);
                case "animal" -> animal(p, w, pr, force);
                case "wake" -> {
                    pr.wakeDay = WAKE_FORCED;
                    Dev.say(p, "Đêm nay khi bạn ngủ, hắn sẽ đánh thức bạn");
                    yield true;
                }
                default -> false;
            };
        } catch (Throwable t) {
            CaveMind.LOG.warn("[CaveMind] scheme {} failed", k, t);
            ok = false;
        }
        if (ok) CaveMind.debug("the Man worked on {} for {}", k, p.getGameProfile().getName());
        else if (force) Dev.say(p, "Mưu đồ \"" + vi(k) + "\" chưa làm được ở đây (thiếu nhà/giường/rương/ruộng, hoặc bạn đang nhìn)");
        return ok;
    }

    public static String vi(String k) {
        return switch (k) {
            case "trail" -> "lối mòn dẫn tới cửa sổ nhà bạn";
            case "nest" -> "ổ của hắn trong rừng";
            case "spoil" -> "đồ ăn trong rương bị tráo thành thịt thối";
            case "harvest" -> "một vệt giẫm nát xuyên qua ruộng";
            case "peep" -> "một lỗ nhìn trộm khoét vào tường cạnh giường";
            case "seal" -> "lối bạn vừa đi qua đã bị bịt kín";
            case "animal" -> "một con vật nuôi biến mất";
            case "wake" -> "bị đánh thức giữa đêm, cửa đã mở";
            case "lured" -> "bị dụ đi theo một âm thanh";
            case "hero_sign" -> "một tấm biển ai đó dựng, biết rõ thói quen của bạn";
            case "hero_tunnel" -> "đường hầm bạn bỏ dở đã có người đào tiếp";
            case "hero_build" -> "thứ Herobrine đã xây";
            case "hero_leafless" -> "cái cây gần nhà bị tuốt sạch lá";
            default -> k;
        };
    }

    static boolean unwatched(ServerPlayer p, Watch w, BlockPos site, double far) {
        Vec3 c = Vec3.atCenterOf(site);
        if (vn.cavemind.sense.Group.seenByOthers(p, c, 96)) return false;
        for (ServerPlayer o : vn.cavemind.sense.Group.others(p, 512)) if (o.distanceToSqr(c) < 12 * 12) return false;
        if (p.isSleeping()) return true;
        double d = c.distanceTo(p.getEyePosition());
        if (d >= far) return true;
        if (d < far * 0.45) return false;
        Sight si = Sight.of(p, w, c);
        return !si.on || !Sight.clear(p.level(), p.getEyePosition(), c, p);
    }

    static boolean seen(ServerPlayer p, Watch w, BlockPos site) {
        Vec3 c = Vec3.atCenterOf(site);
        Sight si = Sight.of(p, w, c);
        return si.on && Sight.clear(p.level(), p.getEyePosition(), c, p) || vn.cavemind.sense.Group.seenByOthers(p, c, 96);
    }

    private static void clue(ServerPlayer p, Profile pr, String kind, BlockPos at, int radius) {
        pr.clue(new Profile.Clue(kind, at.immutable(), Dread.day(p.serverLevel()), radius));
    }

    static void clues(ServerPlayer p, Watch w, Profile pr, long now) {
        if (pr.clues.isEmpty()) return;
        ServerLevel lvl = p.serverLevel();
        if (lvl.dimension() != Level.OVERWORLD) return;
        Vec3 eye = p.getEyePosition();
        for (Profile.Clue c : pr.clues) {
            if (c.found || c.radius <= 0) continue;
            Vec3 at = Vec3.atCenterOf(c.pos);
            if (at.distanceTo(eye) > c.radius || !lvl.isLoaded(c.pos)) continue;
            Sight si = Sight.of(p, w, at);
            if (!si.on || si.edge() > 0.85) continue;
            if (!Sight.clear(lvl, eye, at, p) && !Sight.clear(lvl, eye, at.add(0, 0.6, 0), p)) continue;
            if (++c.seen >= 2) {
                found(p, w, pr, c, now);
                return;
            }
        }
    }

    public static void opened(ServerPlayer p, BlockPos pos) {
        Profile pr = MindData.profile(p);
        ServerLevel lvl = p.serverLevel();
        if (pr.home != null && pr.home.distSqr(pos) < 40 * 40 && lvl.getBlockEntity(pos) instanceof Container c && c.getContainerSize() >= 9) {
            long key = pos.asLong();
            if (!pr.chests.contains(key)) {
                pr.chests.add(key);
                while (pr.chests.size() > 12) pr.chests.remove(0);
            }
        }
        for (Profile.Clue c : pr.clues) {
            if (!c.found && c.kind.equals("spoil") && c.pos.equals(pos)) {
                found(p, Watch.of(p), pr, c, p.server.getTickCount());
                return;
            }
        }
    }

    static void found(ServerPlayer p, Watch w, Profile pr, Profile.Clue c, long now) {
        c.found = true;
        ServerLevel lvl = p.serverLevel();
        pr.moment(new Moment(c.kind, Dread.day(lvl), Dread.time(lvl), c.pos, (int) Math.sqrt(c.pos.distSqr(p.blockPosition())), 0));
        Director.scare(p, 8);
        CaveMind.debug("{} discovered {} {} at {}", p.getGameProfile().getName(), c.kind.startsWith("hero") ? "Herobrine's" : "the Man's", c.kind, c.pos);
        Dev.say(p, "Bạn đã phát hiện: " + vi(c.kind) + (c.kind.startsWith("hero") ? "" : ". Hắn muốn xem phản ứng của bạn — thử quay lại nhìn xem"));
        if (c.kind.startsWith("hero")) return;
        if (Dev.on() || p.getRandom().nextFloat() < 0.6f) ManMind.witness(p, w, pr, c.pos, now);
    }

    public static void broke(ServerPlayer p, BlockPos pos, BlockState st) {
        Profile pr = MindData.profile(p);
        if (pr.nest != null && pr.nest.distSqr(pos) <= 9 && (st.getBlock() instanceof BedBlock || st.getBlock() instanceof ChestBlock || st.getBlock() instanceof CampfireBlock)) {
            pr.anger(60);
            pr.nest = null;
            Director.scare(p, 10);
            ServerLevel lvl = p.serverLevel();
            Fx.later(lvl, 30, () -> Fx.sound(p, "man:scream_loop", SoundSource.HOSTILE, p.position().add(Vec3.directionFromRotation(0, p.getYRot() + 180).scale(60)), 0.5f, 0.75f));
            Dev.say(p, "Bạn phá ổ của hắn. Ở rất xa có tiếng thét. Hắn sẽ NỔI ĐIÊN khi trời tối (giận " + pr.anger + "/100)");
        }
    }

    public static void placed(ServerPlayer p, BlockPos pos, BlockState st) {
        Profile pr = MindData.profile(p);
        if (pr.peep != null && pr.peep.equals(pos)) {
            pr.peep = null;
            Dev.say(p, "Bạn bịt lỗ nhìn trộm lại");
        } else if (pr.trailTo != null && pr.trailDone && pos.distSqr(pr.trailTo) < 16 && isTorch(st)) {
            pr.anger(10);
            Dev.say(p, "Bạn thắp sáng chỗ hắn hay đứng nhìn (giận " + pr.anger + "/100)");
        }
    }

    static boolean isTorch(BlockState st) {
        var id = ForgeRegistries.BLOCKS.getKey(st.getBlock());
        return id != null && (id.getPath().contains("torch") || id.getPath().contains("lantern"));
    }

    static boolean trail(ServerPlayer p, Watch w, Profile pr, boolean force) {
        ServerLevel lvl = p.serverLevel();
        if (pr.home == null || !lvl.isLoaded(pr.home) || pr.trailDone) return false;
        RandomSource r = p.getRandom();
        if (pr.trailTo == null) {
            pr.trailTo = viewSpot(lvl, pr.home);
            if (pr.trailTo == null) return false;
        }
        if (pr.trailFrom == null) {
            pr.trailFrom = edgeSpot(lvl, pr.home, pr.trailTo, r);
            if (pr.trailFrom == null) return false;
            pr.trailAt = pr.trailFrom;
        }
        if (!lvl.isLoaded(pr.trailAt)) return false;
        List<BlockPos> seg = new ArrayList<>();
        BlockPos cur = pr.trailAt;
        int n = 12 + r.nextInt(8);
        boolean reached = false;
        for (int i = 0; i < n; i++) {
            int dx = pr.trailTo.getX() - cur.getX(), dz = pr.trailTo.getZ() - cur.getZ();
            if (dx * dx + dz * dz <= 2) {
                reached = true;
                break;
            }
            int x = cur.getX(), z = cur.getZ();
            boolean alongX = Math.abs(dx) > 0 && (dz == 0 || r.nextDouble() < Math.abs(dx) / (double) (Math.abs(dx) + Math.abs(dz)));
            if (r.nextFloat() < 0.18f) alongX = !alongX;
            if (alongX) x += dx == 0 ? (r.nextBoolean() ? 1 : -1) : Integer.signum(dx);
            else z += dz == 0 ? (r.nextBoolean() ? 1 : -1) : Integer.signum(dz);
            BlockPos col = new BlockPos(x, 0, z);
            if (!lvl.isLoaded(col)) break;
            int y = lvl.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
            BlockPos g = new BlockPos(x, y - 1, z);
            seg.add(g);
            cur = new BlockPos(x, y, z);
        }
        if (seg.isEmpty() && !reached) return false;
        if (!force) {
            for (BlockPos g : seg) {
                double d = Math.sqrt(g.distSqr(p.blockPosition()));
                if (!p.isSleeping() && (d < 22 || d < 60 && seen(p, w, g.above()))) return false;
            }
        }
        int made = 0;
        for (BlockPos g : seg) if (pathify(lvl, g)) made++;
        pr.trailAt = cur;
        if (reached || cur.distSqr(pr.trailTo) <= 2) {
            pr.trailDone = true;
            BlockPos under = pr.trailTo.below();
            BlockState st = lvl.getBlockState(under);
            if (st.is(Blocks.GRASS_BLOCK) || st.is(Blocks.DIRT) || st.is(Blocks.DIRT_PATH)) lvl.setBlock(under, Blocks.COARSE_DIRT.defaultBlockState(), 3);
        }
        BlockPos mark = seg.isEmpty() ? pr.trailTo : seg.get(seg.size() / 2);
        clue(p, pr, "trail", mark.above(), 26);
        Dev.say(p, "Hắn đi một quãng lối mòn mới (" + made + " khối) về phía nhà bạn" + (pr.trailDone ? " — lối mòn đã chạm tới chỗ hắn đứng nhìn qua cửa sổ" : "")
            + ". Đầu mối ở " + mark.getX() + " " + mark.getY() + " " + mark.getZ());
        return made > 0 || pr.trailDone;
    }

    static boolean pathify(ServerLevel lvl, BlockPos g) {
        BlockState st = lvl.getBlockState(g);
        if (!(st.is(Blocks.GRASS_BLOCK) || st.is(Blocks.DIRT) || st.is(Blocks.PODZOL) || st.is(Blocks.COARSE_DIRT) || st.is(Blocks.MYCELIUM) || st.is(Blocks.ROOTED_DIRT))) return false;
        BlockPos up = g.above();
        BlockState u = lvl.getBlockState(up);
        if (!u.isAir()) {
            if (!u.canBeReplaced() || !u.getFluidState().isEmpty()) return false;
            lvl.removeBlock(up, false);
        }
        lvl.setBlock(g, Blocks.DIRT_PATH.defaultBlockState(), 3);
        return true;
    }

    static BlockPos viewSpot(ServerLevel lvl, BlockPos home) {
        BlockPos best = null;
        double bd = 1e9;
        for (int dy = -2; dy <= 4; dy++) {
            for (int dx = -10; dx <= 10; dx++) {
                for (int dz = -10; dz <= 10; dz++) {
                    BlockPos q = home.offset(dx, dy, dz);
                    BlockState st = lvl.getBlockState(q);
                    boolean glass = Sight.glassy(st);
                    boolean door = st.getBlock() instanceof DoorBlock;
                    if (!glass && !door) continue;
                    for (Direction d : Direction.Plane.HORIZONTAL) {
                        if (!lvl.canSeeSky(q.relative(d, 2))) continue;
                        BlockPos t = q.relative(d, glass ? 3 : 5);
                        int y = lvl.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, t.getX(), t.getZ());
                        BlockPos feet = new BlockPos(t.getX(), y, t.getZ());
                        if (Math.abs(y - q.getY()) > 4 || !Spots.standable(lvl, feet, 3)) continue;
                        double score = q.distSqr(home) + (glass ? 0 : 40);
                        if (score < bd) {
                            bd = score;
                            best = feet;
                        }
                    }
                }
            }
        }
        return best;
    }

    static BlockPos edgeSpot(ServerLevel lvl, BlockPos home, BlockPos view, RandomSource r) {
        double base = Math.atan2(view.getZ() - home.getZ(), view.getX() - home.getX());
        BlockPos best = null;
        int bestTrees = -1;
        for (int i = 0; i < 48; i++) {
            double a = base + (r.nextDouble() - 0.5) * Math.PI * 0.9;
            double d = 42 + r.nextDouble() * 18;
            int x = Mth.floor(view.getX() + Math.cos(a) * d), z = Mth.floor(view.getZ() + Math.sin(a) * d);
            if (!lvl.isLoaded(new BlockPos(x, 64, z))) continue;
            int y = lvl.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
            BlockPos feet = new BlockPos(x, y, z);
            if (!Spots.standable(lvl, feet, 3) || Math.abs(y - view.getY()) > 12) continue;
            BlockState below = lvl.getBlockState(feet.below());
            if (!(below.is(BlockTags.DIRT) || below.is(Blocks.GRASS_BLOCK))) continue;
            int trees = 0;
            for (int dx = -5; dx <= 5; dx += 2) {
                for (int dz = -5; dz <= 5; dz += 2) {
                    for (int dy = 0; dy <= 4; dy += 2) {
                        BlockState s = lvl.getBlockState(feet.offset(dx, dy, dz));
                        if (s.is(BlockTags.LOGS) || s.is(BlockTags.LEAVES)) trees++;
                    }
                }
            }
            if (trees > bestTrees) {
                bestTrees = trees;
                best = feet;
            }
        }
        return best;
    }

    static boolean nest(ServerPlayer p, Watch w, Profile pr, boolean force) {
        ServerLevel lvl = p.serverLevel();
        if (pr.trailFrom == null || pr.nest != null || pr.home == null || !lvl.isLoaded(pr.trailFrom)) return false;
        if (!force && !unwatched(p, w, pr.trailFrom, 56)) return false;
        Direction toHome = Direction.getNearest(pr.home.getX() - pr.trailFrom.getX(), 0, pr.home.getZ() - pr.trailFrom.getZ());
        if (toHome.getAxis().isVertical()) toHome = Direction.NORTH;
        BlockPos head = null;
        for (int i = 0; i < 25 && head == null; i++) {
            BlockPos c = pr.trailFrom.offset(i % 5 - 2, 0, i / 5 - 2);
            int y = lvl.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, c.getX(), c.getZ());
            BlockPos h = new BlockPos(c.getX(), y, c.getZ());
            BlockPos foot = h.relative(toHome.getOpposite());
            BlockPos chest = h.relative(toHome.getClockWise());
            if (free(lvl, h) && free(lvl, foot) && free(lvl, chest) && lvl.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, foot.getX(), foot.getZ()) == y
                && lvl.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, chest.getX(), chest.getZ()) == y) head = h;
        }
        if (head == null) return false;
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                if (Math.abs(dx) + Math.abs(dz) > 3) continue;
                BlockPos g = head.offset(dx, -1, dz);
                BlockState st = lvl.getBlockState(g);
                if (st.is(Blocks.GRASS_BLOCK) || st.is(Blocks.DIRT) || st.is(Blocks.PODZOL)) {
                    lvl.setBlock(g, (dx + dz & 1) == 0 ? Blocks.COARSE_DIRT.defaultBlockState() : Blocks.PODZOL.defaultBlockState(), 3);
                    BlockState up = lvl.getBlockState(g.above());
                    if (!up.isAir() && up.canBeReplaced()) lvl.removeBlock(g.above(), false);
                }
            }
        }
        Block bed = bedOf(lvl, pr.home);
        BlockPos foot = head.relative(toHome.getOpposite());
        lvl.setBlock(foot, bed.defaultBlockState().setValue(BedBlock.FACING, toHome).setValue(BedBlock.PART, BedPart.FOOT), 3);
        lvl.setBlock(head, bed.defaultBlockState().setValue(BedBlock.FACING, toHome).setValue(BedBlock.PART, BedPart.HEAD), 3);
        BlockPos chest = head.relative(toHome.getClockWise());
        lvl.setBlock(chest, Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING, toHome), 3);
        BlockPos fire = head.relative(toHome, 2);
        if (free(lvl, fire)) lvl.setBlock(fire, Blocks.CAMPFIRE.defaultBlockState().setValue(CampfireBlock.LIT, false), 3);
        pr.nest = head;
        fill(lvl, pr, chest, p.getRandom());
        clue(p, pr, "nest", head, 20);
        Dev.say(p, "Hắn dựng ổ ở đầu lối mòn: một cái giường cùng màu giường bạn, một rương và bếp lửa tắt — " + Dev.where(p, Vec3.atCenterOf(head))
            + " tại " + head.getX() + " " + head.getY() + " " + head.getZ());
        return true;
    }

    private static boolean free(ServerLevel lvl, BlockPos q) {
        BlockState st = lvl.getBlockState(q);
        BlockState below = lvl.getBlockState(q.below());
        return (st.isAir() || st.canBeReplaced() && st.getFluidState().isEmpty()) && lvl.getBlockState(q.above()).getCollisionShape(lvl, q.above()).isEmpty()
            && below.isFaceSturdy(lvl, q.below(), Direction.UP) && !below.is(BlockTags.LEAVES) && lvl.getFluidState(q.below()).isEmpty();
    }

    private static Block bedOf(ServerLevel lvl, BlockPos home) {
        BlockState st = lvl.getBlockState(home);
        if (st.getBlock() instanceof BedBlock b) return b;
        return Blocks.RED_BED;
    }

    static void fill(ServerLevel lvl, Profile pr, BlockPos chest, RandomSource r) {
        if (!(lvl.getBlockEntity(chest) instanceof Container c)) return;
        List<ItemStack> items = new ArrayList<>();
        for (int i = 0; i < pr.loot.size(); i++) items.add(ItemStack.of(pr.loot.getCompound(i)));
        pr.loot.clear();
        int torches = Math.min(24, pr.dStolen);
        if (torches > 0) items.add(new ItemStack(Items.TORCH, torches));
        items.add(new ItemStack(Items.BONE, 1 + r.nextInt(3)));
        List<Integer> slots = new ArrayList<>();
        for (int i = 0; i < c.getContainerSize(); i++) if (c.getItem(i).isEmpty()) slots.add(i);
        Collections.shuffle(slots, new java.util.Random(r.nextLong()));
        for (ItemStack st : items) {
            if (slots.isEmpty()) break;
            c.setItem(slots.remove(0), st);
        }
        c.setChanged();
    }

    static void deliver(ServerPlayer p, Profile pr, ItemStack st) {
        ServerLevel lvl = p.serverLevel();
        if (pr.nest != null && lvl.isLoaded(pr.nest)) {
            for (Direction d : Direction.Plane.HORIZONTAL) {
                BlockPos q = pr.nest.relative(d);
                if (lvl.getBlockEntity(q) instanceof Container c) {
                    for (int i = 0; i < c.getContainerSize(); i++) {
                        if (c.getItem(i).isEmpty()) {
                            c.setItem(i, st);
                            c.setChanged();
                            return;
                        }
                    }
                }
            }
        }
        pr.stash(st);
    }

    static boolean spoil(ServerPlayer p, Watch w, Profile pr, boolean force, BlockPos only) {
        ServerLevel lvl = p.serverLevel();
        List<Long> keys = new ArrayList<>(pr.chests);
        Collections.shuffle(keys, new java.util.Random(p.getRandom().nextLong()));
        for (long key : keys) {
            BlockPos q = BlockPos.of(key);
            if (only != null && !only.equals(q)) continue;
            if (!lvl.isLoaded(q)) continue;
            if (!(lvl.getBlockEntity(q) instanceof Container c)) {
                pr.chests.remove(key);
                continue;
            }
            if (!force && only == null && !unwatched(p, w, q, 26)) continue;
            if (!force && (p.getFoodData().getFoodLevel() < 10 || foodIn(p) < 8)) return false;
            int slot = -1, best = 0;
            for (int i = 0; i < c.getContainerSize(); i++) {
                ItemStack st = c.getItem(i);
                if (st.isEmpty() || !st.isEdible() || st.is(Items.ROTTEN_FLESH) || st.is(Items.SPIDER_EYE) || st.is(Items.POISONOUS_POTATO)) continue;
                if (st.getCount() > best) {
                    best = st.getCount();
                    slot = i;
                }
            }
            if (slot < 0) continue;
            ItemStack whole = c.getItem(slot);
            int n = Math.max(1, whole.getCount() / 2);
            ItemStack took = whole.copyWithCount(n);
            whole.shrink(n);
            c.setItem(slot, whole);
            ItemStack rot = new ItemStack(Items.ROTTEN_FLESH, n);
            boolean put = false;
            for (int i = 0; i < c.getContainerSize() && !put; i++) {
                if (c.getItem(i).isEmpty()) {
                    c.setItem(i, rot);
                    put = true;
                }
            }
            if (!put && whole.isEmpty()) c.setItem(slot, rot);
            c.setChanged();
            deliver(p, pr, took);
            clue(p, pr, "spoil", q, 0);
            Dev.say(p, "Hắn đã lục rương ở " + q.getX() + " " + q.getY() + " " + q.getZ() + ": " + took.getCount() + " " + took.getHoverName().getString()
                + " thành thịt thối. Mở rương ra là bạn sẽ thấy");
            return true;
        }
        return false;
    }

    static boolean harvest(ServerPlayer p, Watch w, Profile pr, boolean force) {
        ServerLevel lvl = p.serverLevel();
        if (pr.home == null) return false;
        List<BlockPos> crops = new ArrayList<>();
        for (int dy = -4; dy <= 4; dy++) {
            for (int dx = -24; dx <= 24; dx++) {
                for (int dz = -24; dz <= 24; dz++) {
                    BlockPos q = pr.home.offset(dx, dy, dz);
                    if (lvl.getBlockState(q).getBlock() instanceof CropBlock && lvl.getBlockState(q.below()).is(Blocks.FARMLAND)) crops.add(q);
                }
            }
        }
        if (crops.size() < 6) return false;
        RandomSource r = p.getRandom();
        BlockPos s = crops.get(r.nextInt(crops.size()));
        if (!force && !unwatched(p, w, s, 34)) return false;
        Direction d = Direction.Plane.HORIZONTAL.getRandomDirection(r);
        int made = 0;
        for (int k = -9; k <= 9; k++) {
            BlockPos q = s.relative(d, k);
            if (!lvl.getBlockState(q.below()).is(Blocks.FARMLAND)) continue;
            BlockState cs = lvl.getBlockState(q);
            if (!(cs.getBlock() instanceof CropBlock crop) || crop.getAge(cs) == 0) continue;
            lvl.setBlock(q, crop.getStateForAge(0), 3);
            made++;
        }
        if (made < 3) return false;
        clue(p, pr, "harvest", s, 20);
        Dev.say(p, "Một vệt thẳng xuyên qua ruộng của bạn bị giẫm về mầm non (" + made + " ô) ở " + s.getX() + " " + s.getY() + " " + s.getZ());
        return true;
    }

    static boolean peep(ServerPlayer p, Watch w, Profile pr, boolean force) {
        ServerLevel lvl = p.serverLevel();
        BlockPos bed = pr.home;
        if (bed == null || !lvl.isLoaded(bed)) return false;
        if (!force && !p.isSleeping() && Math.sqrt(bed.distSqr(p.blockPosition())) < 28) return false;
        BlockPos best = null;
        Direction bestOut = null;
        double bd = 1e9;
        for (int dy = 1; dy <= 2; dy++) {
            for (int dx = -7; dx <= 7; dx++) {
                for (int dz = -7; dz <= 7; dz++) {
                    BlockPos q = bed.offset(dx, dy, dz);
                    BlockState st = lvl.getBlockState(q);
                    if (!st.isCollisionShapeFullBlock(lvl, q) || st.hasBlockEntity() || Sight.glassy(st) || st.is(BlockTags.DOORS) || st.is(BlockTags.LEAVES)) continue;
                    float hard = st.getDestroySpeed(lvl, q);
                    if (hard <= 0 || hard > 3.6f) continue;
                    for (Direction out : Direction.Plane.HORIZONTAL) {
                        BlockPos in = q.relative(out.getOpposite()), o = q.relative(out);
                        if (!lvl.getBlockState(in).isAir() || lvl.canSeeSky(in)) continue;
                        if (!lvl.getBlockState(o).isAir() || !(lvl.canSeeSky(o) || lvl.canSeeSky(o.relative(out)))) continue;
                        BlockPos feet = o.below(2);
                        if (!Spots.standable(lvl, feet, 3)) continue;
                        double score = q.distSqr(bed);
                        if (score < 4) score += 20;
                        if (score < bd) {
                            bd = score;
                            best = q;
                            bestOut = out;
                        }
                    }
                }
            }
        }
        if (best == null) return false;
        BlockState was = lvl.getBlockState(best);
        lvl.removeBlock(best, false);
        pr.peep = best;
        pr.peepDir = bestOut.get2DDataValue();
        deliver(p, pr, new ItemStack(was.getBlock().asItem()));
        clue(p, pr, "peep", best, 11);
        Dev.say(p, "Hắn khoét một lỗ nhìn trộm vào tường gần giường bạn, ở " + best.getX() + " " + best.getY() + " " + best.getZ()
            + ". Đêm tới khi bạn ở trong nhà, hắn có thể ghé mắt vào đó");
        return true;
    }

    static int foodIn(ServerPlayer p) {
        int n = 0;
        for (ItemStack st : p.getInventory().items) if (st.isEdible() && !st.is(Items.ROTTEN_FLESH) && !st.is(Items.SPIDER_EYE)) n += st.getCount();
        return n;
    }

    static int underCrumbs(Watch w) {
        int n = 0;
        for (long[] c : w.crumbs) if (c[2] == 1) n++;
        return n;
    }

    static boolean hasPick(ServerPlayer p) {
        for (ItemStack st : p.getInventory().items) if (st.getItem() instanceof PickaxeItem) return true;
        return p.getOffhandItem().getItem() instanceof PickaxeItem;
    }

    static boolean seal(ServerPlayer p, Watch w, Profile pr, boolean force) {
        ServerLevel lvl = p.serverLevel();
        if (!w.underground || !hasPick(p)) return false;
        List<long[]> list = new ArrayList<>(w.crumbs);
        for (int i = list.size() - 2; i >= 1; i--) {
            long[] c = list.get(i);
            if (c[2] != 1) continue;
            BlockPos feet = BlockPos.of(c[0]);
            double d = Math.sqrt(feet.distSqr(p.blockPosition()));
            if (d < 12) continue;
            if (d > 48) break;
            BlockPos next = BlockPos.of(list.get(i + 1)[0]);
            Direction along = Direction.getNearest(next.getX() - feet.getX(), 0, next.getZ() - feet.getZ());
            if (along.getAxis().isVertical()) continue;
            BlockPos head = feet.above();
            if (!air(lvl, feet) || !air(lvl, head)) continue;
            Direction a = along.getClockWise(), b = along.getCounterClockWise();
            if (!solid(lvl, feet.relative(a)) || !solid(lvl, feet.relative(b)) || !solid(lvl, head.relative(a)) || !solid(lvl, head.relative(b))) continue;
            if (!solid(lvl, head.above()) || !solid(lvl, feet.below())) continue;
            if (seen(p, w, feet) || seen(p, w, head)) continue;
            BlockState mat = lvl.getBlockState(feet.relative(a));
            if (!(mat.is(BlockTags.BASE_STONE_OVERWORLD) || mat.is(Blocks.COBBLESTONE) || mat.is(BlockTags.DIRT) || mat.is(Blocks.GRAVEL))) mat = Blocks.COBBLESTONE.defaultBlockState();
            lvl.setBlock(feet, mat, 3);
            lvl.setBlock(head, mat, 3);
            int took = 0;
            for (BlockPos q : BlockPos.betweenClosed(feet.offset(-3, -1, -3), feet.offset(3, 3, 3))) {
                var id = ForgeRegistries.BLOCKS.getKey(lvl.getBlockState(q).getBlock());
                if (id != null && id.getPath().contains("torch") && !id.getPath().contains("redstone")) {
                    lvl.removeBlock(q, false);
                    took++;
                }
            }
            if (took > 0) deliver(p, pr, new ItemStack(Items.TORCH, took));
            clue(p, pr, "seal", feet, 7);
            BlockState fin = mat;
            Fx.later(lvl, 30 + p.getRandom().nextInt(30), () -> Fx.sound(p, Holder.direct(fin.getSoundType().getPlaceSound()), SoundSource.BLOCKS, Vec3.atCenterOf(feet), 0.5f, 0.7f));
            Dev.say(p, "Hắn bịt kín đường bạn vừa đi qua (cách " + (int) d + "m, " + feet.getX() + " " + feet.getY() + " " + feet.getZ() + ")"
                + (took > 0 ? " và lấy " + took + " ngọn đuốc gần đó" : "") + ". Có tiếng đá rất khẽ phía sau");
            return true;
        }
        return false;
    }

    private static boolean air(ServerLevel lvl, BlockPos q) {
        BlockState st = lvl.getBlockState(q);
        return st.getCollisionShape(lvl, q).isEmpty() && lvl.getFluidState(q).isEmpty() && !st.is(BlockTags.RAILS);
    }

    private static boolean solid(ServerLevel lvl, BlockPos q) {
        BlockState st = lvl.getBlockState(q);
        return st.isCollisionShapeFullBlock(lvl, q) && !st.hasBlockEntity();
    }

    static boolean animal(ServerPlayer p, Watch w, Profile pr, boolean force) {
        ServerLevel lvl = p.serverLevel();
        if (pr.home == null || !lvl.isLoaded(pr.home)) return false;
        List<Animal> list = lvl.getEntitiesOfClass(Animal.class, new AABB(pr.home).inflate(24, 8, 24), a -> (a instanceof Cow || a instanceof Sheep || a instanceof Pig
            || a instanceof Chicken || a instanceof Goat || a instanceof Rabbit) && !a.hasCustomName() && !a.isLeashed() && !a.isBaby() && !Compat.managed(a)
            && !(a instanceof TamableAnimal t && t.isTame()));
        if (list.size() < 2) return false;
        Collections.shuffle(list, new java.util.Random(p.getRandom().nextLong()));
        for (Animal a : list) {
            long same = list.stream().filter(o -> o.getType() == a.getType()).count();
            if (same < 4 && !force) continue;
            if (!force && !unwatched(p, w, a.blockPosition(), 34)) continue;
            List<ItemStack> rem = new ArrayList<>();
            rem.add(new ItemStack(Items.BONE, 2));
            if (a instanceof Sheep s) rem.add(new ItemStack(woolOf(s.getColor())));
            else if (a instanceof Cow) rem.add(new ItemStack(Items.LEATHER));
            else if (a instanceof Chicken) rem.add(new ItemStack(Items.FEATHER, 3));
            else if (a instanceof Rabbit) rem.add(new ItemStack(Items.RABBIT_HIDE));
            BlockPos at = a.blockPosition();
            String name = a.getType().getDescription().getString();
            a.discard();
            pr.heroLast.put("man_animal", Dread.day(lvl));
            for (ItemStack st : rem) deliver(p, pr, st);
            clue(p, pr, "animal", at, 14);
            Dev.say(p, "Hắn bắt đi một con " + name + " gần nhà bạn (ở " + at.getX() + " " + at.getY() + " " + at.getZ() + "). Xương của nó nằm trong ổ hắn");
            return true;
        }
        return false;
    }

    private static net.minecraft.world.level.ItemLike woolOf(DyeColor c) {
        Block b = ForgeRegistries.BLOCKS.getValue(new net.minecraft.resources.ResourceLocation("minecraft", c.getName() + "_wool"));
        return b != null ? b : Blocks.WHITE_WOOL;
    }

    public static final long WAKE_FORCED = -2;

    public static void sleeping(ServerPlayer p, Watch w, long now) {
        if (!Compat.manActive() || p.getSleepTimer() != 45) return;
        Profile pr = MindData.profile(p);
        ServerLevel lvl = p.serverLevel();
        long key = vn.cavemind.damned.Haunt.nightKey(lvl);
        if (Director.holds(p, "damned")) return;
        if (pr.wakeDay != WAKE_FORCED) {
            if (pr.wakeDay == key || !Cfg.MAN_WAKE.get()) return;
            if (pr.manLevel < 3 && !Dev.on()) return;
            Long lastWake = pr.heroLast.get("man_wake");
            if (lastWake != null && key - lastWake <= 1) return;
            for (ServerPlayer o : lvl.players()) if (o != p && o.isSleeping()) return;
            if (Cfg.MERCY.get() && Director.state(p).mercyUntil > now) return;
            pr.wakeDay = key;
            float chance = Dev.on() ? 0.7f : pr.manLevel >= 6 ? 0.5f : 0.3f;
            if (p.getRandom().nextFloat() > chance) return;
        }
        pr.wakeDay = key;
        pr.heroLast.put("man_wake", key);
        BlockPos door = Window.door(p);
        if (door != null) {
            BlockState st = lvl.getBlockState(door);
            Holder<net.minecraft.sounds.SoundEvent> knock = Compat.sound("the_knocker:door_knocking_1");
            if (knock != null) Fx.sound(p, knock, SoundSource.BLOCKS, Vec3.atCenterOf(door), 1.0f, 0.8f);
            else Fx.sound(p, Holder.direct(SoundEvents.WOOD_HIT), SoundSource.BLOCKS, Vec3.atCenterOf(door), 1.3f, 0.6f);
            if (st.getBlock() instanceof DoorBlock && !st.getValue(DoorBlock.OPEN)) {
                Fx.later(lvl, 30, () -> {
                    BlockState cur = lvl.getBlockState(door);
                    if (cur.getBlock() instanceof DoorBlock d2 && !cur.getValue(DoorBlock.OPEN)) d2.setOpen(null, lvl, cur, door, true);
                });
            }
        } else {
            Fx.sound(p, Compat.sound("the_knocker:window_knock_1", "minecraft:block.glass.hit"), SoundSource.BLOCKS, p.getEyePosition().add(2, 0.5, 0), 0.9f, 0.85f);
        }
        Fx.later(lvl, 36, () -> {
            if (p.isSleeping()) p.stopSleepInBed(true, true);
        });
        pr.moment(new Moment(Moment.WAKE, Dread.day(lvl), Dread.time(lvl), p.blockPosition(), door != null ? 1 : 0, 0));
        Director.scare(p, 12);
        CaveMind.debug("the Man woke {} up", p.getGameProfile().getName());
        Dev.say(p, "Có tiếng gõ — bạn bị đánh thức" + (door != null ? ". Cửa đã mở" : "") + ". Đêm nay bạn sẽ không ngủ yên được");
    }

    public static String status(Profile pr) {
        int open = 0;
        for (Profile.Clue c : pr.clues) if (!c.found) open++;
        return "anger=" + pr.anger + " bold=" + pr.bold + " schemes=" + pr.schemes + " clues_open=" + open + " nest=" + pr.nest + " trail=" + (pr.trailFrom == null ? "-" : pr.trailDone ? "done" : "growing")
            + " peep=" + pr.peep + " chests=" + pr.chests.size();
    }

    public static String devLine(ServerPlayer p, Profile pr) {
        int open = 0;
        for (Profile.Clue c : pr.clues) if (!c.found) open++;
        long left = Math.max(0, (pr.schemeNext - pr.playTicks) / 20);
        return "mưu đồ sau " + left + "s, " + open + " dấu vết chưa thấy, giận " + pr.anger + ", liều " + pr.bold;
    }

    private Schemes() {}
}
