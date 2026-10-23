package vn.cavemind.day;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Container;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.AbstractCandleBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.WallTorchBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import vn.cavemind.compat.Compat;
import vn.cavemind.damned.Haunt;
import vn.cavemind.dev.Dev;
import vn.cavemind.director.Director;
import vn.cavemind.director.Dread;
import vn.cavemind.man.ManMind;
import vn.cavemind.memory.Moment;
import vn.cavemind.memory.Profile;
import vn.cavemind.sense.Sight;
import vn.cavemind.sense.Watch;
import vn.cavemind.world.Fx;
import vn.cavemind.world.Spots;

final class House extends Piece {
    final boolean inside;
    boolean spawned;

    House(boolean inside) {
        this.inside = inside;
    }

    @Override
    String id() {
        return inside ? "inside" : "house";
    }

    @Override
    String vi() {
        return inside ? "có người trong nhà bạn (thấy qua cửa sổ khi bạn đi về)" : "ngôi nhà bị chạm vào (lúc bạn đi vắng)";
    }

    @Override
    boolean claims() {
        return inside;
    }

    @Override
    boolean begin(ServerPlayer p, Watch w, Profile pr, long now) {
        if (pr.home == null || !p.serverLevel().isLoaded(pr.home)) return false;
        if (inside) return true;
        List<String> done = touch(p, pr);
        if (done.isEmpty()) return false;
        ServerLevel lvl = p.serverLevel();
        pr.moment(new Moment(Moment.HOME, Dread.day(lvl), Dread.time(lvl), pr.home, done.size(), 0));
        Dev.say(p, "Trong lúc bạn đi vắng, nhà bạn đã bị chạm vào: " + String.join(", ", done));
        return true;
    }

    @Override
    boolean tick(ServerPlayer p, Watch w, Profile pr, long now) {
        if (!inside) return false;
        ServerLevel lvl = p.serverLevel();
        double home = Math.sqrt(pr.home.distSqr(p.blockPosition()));
        if (!spawned) {
            if (home < 14) {
                Fx.sound(p, Holder.direct(SoundEvents.WOODEN_DOOR_CLOSE), SoundSource.BLOCKS, Vec3.atCenterOf(pr.home), 0.6f, 0.9f);
                Dev.say(p, "Không kịp thấy ai — chỉ nghe tiếng cửa đóng trong nhà");
                return false;
            }
            if (home < 48 && now % 10 == 0 && place(p, w, pr)) {
                spawned = true;
                Dev.say(p, "Có một bóng người đứng im trong nhà bạn — nhìn qua cửa sổ / cửa ra vào");
            }
            return now - start < 6000 && home < 160;
        }
        Entity e = body(p);
        if (!(e instanceof Mob m)) return false;
        ManMind.face(m, p.getEyePosition());
        boolean was = noticed;
        watch(p, w, e);
        if (noticed && !was) {
            Director.scare(p, 10);
            pr.moment(new Moment(Moment.HOME, Dread.day(lvl), Dread.time(lvl), pr.home, 0, 1));
        }
        double dist = Math.sqrt(e.distanceToSqr(p));
        if (dist < 9 || noticed && away >= 3) {
            Dev.say(p, "Bóng người trong nhà đã biến mất");
            return false;
        }
        return now - start < 8000;
    }

    private boolean place(ServerPlayer p, Watch w, Profile pr) {
        ServerLevel lvl = p.serverLevel();
        Vec3 eye = p.getEyePosition();
        Vec3 ahead = eye.add(Vec3.atCenterOf(pr.home).subtract(eye).normalize().scale(10));
        List<BlockPos> spots = new ArrayList<>();
        for (int dx = -6; dx <= 6; dx++) {
            for (int dz = -6; dz <= 6; dz++) {
                for (int dy = -2; dy <= 2; dy++) {
                    BlockPos q = pr.home.offset(dx, dy, dz);
                    if (!Spots.standable(lvl, q, 2) || lvl.canSeeSky(q.above(2))) continue;
                    spots.add(q);
                }
            }
        }
        Collections.shuffle(spots, new java.util.Random(p.getRandom().nextLong()));
        for (BlockPos q : spots) {
            AABB box = Sight.body(q, 0.6, 1.8);
            double now = Sight.visible(p, box);
            boolean onScreen = Sight.of(p, w, box.getCenter()).on;
            if (onScreen && now > 0) continue;
            if (from(lvl, ahead, box, p) < 0.3 && now < 0.3) continue;
            Entity e = Haunt.spawn(p, q, eye);
            if (e == null) return false;
            e.getPersistentData().putString(Compat.TAG, "damned_day");
            Compat.anim(e, "cm_profile");
            body = e.getUUID();
            return true;
        }
        return false;
    }

    static double from(ServerLevel lvl, Vec3 eye, AABB box, Entity viewer) {
        Vec3 c = box.getCenter();
        double h = box.getYsize();
        Vec3[] pts = {new Vec3(c.x, box.minY + h * 0.2, c.z), c, new Vec3(c.x, box.minY + h * 0.85, c.z)};
        int ok = 0;
        for (Vec3 pt : pts) if (Sight.clear(lvl, eye, pt, viewer)) ok++;
        return ok / 3.0;
    }

    static List<String> touch(ServerPlayer p, Profile pr) {
        ServerLevel lvl = p.serverLevel();
        RandomSource r = p.getRandom();
        BlockPos h = pr.home;
        List<BlockPos> doors = new ArrayList<>(), torches = new ArrayList<>(), candles = new ArrayList<>(), boxes = new ArrayList<>();
        for (int dy = -3; dy <= 4; dy++) {
            for (int dx = -10; dx <= 10; dx++) {
                for (int dz = -10; dz <= 10; dz++) {
                    BlockPos q = h.offset(dx, dy, dz);
                    BlockState st = lvl.getBlockState(q);
                    if (st.isAir()) continue;
                    if (st.getBlock() instanceof DoorBlock && st.is(BlockTags.WOODEN_DOORS) && st.getValue(DoorBlock.HALF) == DoubleBlockHalf.LOWER) doors.add(q);
                    else if (st.is(Blocks.TORCH) || st.is(Blocks.WALL_TORCH)) torches.add(q);
                    else if (st.getBlock() instanceof AbstractCandleBlock && st.hasProperty(BlockStateProperties.LIT) && !st.getValue(BlockStateProperties.LIT)) candles.add(q);
                    else if (st.hasBlockEntity()) {
                        BlockEntity be = lvl.getBlockEntity(q);
                        if (be instanceof Container c && c.getContainerSize() >= 9) boxes.add(q);
                    }
                }
            }
        }
        List<Runnable> acts = new ArrayList<>();
        List<String> names = new ArrayList<>();
        for (BlockPos d : doors) {
            BlockState st = lvl.getBlockState(d);
            if (!st.getValue(DoorBlock.OPEN)) {
                acts.add(() -> ((DoorBlock) st.getBlock()).setOpen(null, lvl, st, d, true));
                names.add("cửa bị mở");
                break;
            }
        }
        if (!torches.isEmpty()) {
            BlockPos t = torches.get(r.nextInt(torches.size()));
            acts.add(() -> {
                BlockState st = lvl.getBlockState(t);
                BlockState soul = st.is(Blocks.WALL_TORCH)
                    ? Blocks.SOUL_WALL_TORCH.defaultBlockState().setValue(WallTorchBlock.FACING, st.getValue(WallTorchBlock.FACING))
                    : Blocks.SOUL_TORCH.defaultBlockState();
                lvl.setBlock(t, soul, 3);
            });
            names.add("một ngọn đuốc thành đuốc linh hồn xanh");
        }
        if (!candles.isEmpty()) {
            acts.add(() -> {
                for (BlockPos c : candles) {
                    BlockState st = lvl.getBlockState(c);
                    if (st.hasProperty(BlockStateProperties.LIT)) lvl.setBlock(c, st.setValue(BlockStateProperties.LIT, true), 11);
                }
            });
            names.add("nến được thắp");
        }
        if (!boxes.isEmpty()) {
            BlockPos b = boxes.get(r.nextInt(boxes.size()));
            acts.add(() -> swap(lvl, b, r));
            names.add("hai món trong rương đổi chỗ");
        }
        for (BlockPos d : doors) {
            Direction f = lvl.getBlockState(d).getValue(DoorBlock.FACING);
            for (Direction s : new Direction[]{f, f.getOpposite()}) {
                BlockPos o = d.relative(s);
                if (lvl.canSeeSky(o) && lvl.getBlockState(o).isAir() && lvl.getBlockState(o.below()).is(BlockTags.DIRT)) {
                    acts.add(() -> lvl.setBlock(o, Blocks.POPPY.defaultBlockState(), 3));
                    names.add("một bông hoa trước cửa");
                    break;
                }
            }
            if (names.contains("một bông hoa trước cửa")) break;
        }
        List<ItemFrame> frames = lvl.getEntitiesOfClass(ItemFrame.class, new AABB(h).inflate(10, 4, 10), f -> !f.getItem().isEmpty());
        if (!frames.isEmpty()) {
            ItemFrame f = frames.get(r.nextInt(frames.size()));
            acts.add(() -> f.setRotation(f.getRotation() + 4));
            names.add("khung tranh bị lật ngược");
        }
        List<Integer> idx = new ArrayList<>();
        for (int i = 0; i < acts.size(); i++) idx.add(i);
        Collections.shuffle(idx, new java.util.Random(r.nextLong()));
        int n = Math.min(acts.size(), 2 + r.nextInt(2));
        List<String> out = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            acts.get(idx.get(i)).run();
            out.add(names.get(idx.get(i)));
        }
        return out;
    }

    static void swap(ServerLevel lvl, BlockPos b, RandomSource r) {
        if (!(lvl.getBlockEntity(b) instanceof Container c)) return;
        List<Integer> full = new ArrayList<>();
        for (int i = 0; i < c.getContainerSize(); i++) if (!c.getItem(i).isEmpty()) full.add(i);
        if (full.isEmpty()) return;
        int a = full.get(r.nextInt(full.size()));
        int to = r.nextInt(c.getContainerSize());
        if (to == a) to = (to + 1) % c.getContainerSize();
        ItemStack x = c.getItem(a).copy(), y = c.getItem(to).copy();
        c.setItem(a, y);
        c.setItem(to, x);
        c.setChanged();
    }
}
