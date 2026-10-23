package vn.cavemind.damned;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CandleBlock;
import net.minecraft.world.level.block.LecternBlock;
import net.minecraft.world.level.block.StandingSignBlock;
import net.minecraft.world.level.block.entity.BarrelBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.LecternBlockEntity;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.entity.SignText;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.registries.ForgeRegistries;
import vn.cavemind.CaveMind;
import vn.cavemind.compat.Compat;
import vn.cavemind.director.Dread;
import vn.cavemind.memory.Profile;
import vn.cavemind.sense.Sight;
import vn.cavemind.sense.Watch;
import vn.cavemind.world.Fx;

public final class Shrine {
    private static final int[][] RING = {{2, 0}, {-2, 0}, {0, 2}, {0, -2}, {2, 2}, {2, -2}, {-2, 2}, {-2, -2}, {1, 2}, {-1, 2}, {1, -2}, {-1, -2}};

    public static BlockPos build(ServerPlayer p, Profile pr) {
        ServerLevel lvl = p.serverLevel();
        BlockPos origin = pr.home != null ? pr.home : p.blockPosition();
        RandomSource r = p.getRandom();
        Watch w = Watch.of(p);
        for (int i = 0; i < 80; i++) {
            double a = r.nextDouble() * Math.PI * 2;
            double d = 45 + r.nextDouble() * 50;
            int x = Mth.floor(origin.getX() + Math.cos(a) * d), z = Mth.floor(origin.getZ() + Math.sin(a) * d);
            if (!lvl.hasChunkAt(new BlockPos(x, 64, z))) continue;
            BlockPos c = new BlockPos(x, lvl.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z), z);
            if (!flat(lvl, c) || manMade(lvl, c)) continue;
            Vec3 center = Vec3.atCenterOf(c.above());
            if (center.distanceTo(p.getEyePosition()) < 90 && Sight.of(p, w, center).on && Sight.clear(lvl, p.getEyePosition(), center, p)) continue;
            Direction f = Direction.getNearest(origin.getX() - c.getX(), 0, origin.getZ() - c.getZ());
            if (f.getAxis().isVertical()) f = Direction.NORTH;
            place(p, pr, lvl, c, f, origin);
            CaveMind.debug("the Damned built a shrine for {} at {}", p.getGameProfile().getName(), c);
            return c;
        }
        return null;
    }

    private static boolean flat(ServerLevel lvl, BlockPos c) {
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                int y = lvl.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, c.getX() + dx, c.getZ() + dz);
                if (Math.abs(y - c.getY()) > 0) return false;
                BlockPos g = new BlockPos(c.getX() + dx, y - 1, c.getZ() + dz);
                BlockState st = lvl.getBlockState(g);
                if (!lvl.getFluidState(g).isEmpty() || st.is(BlockTags.LEAVES) || st.is(BlockTags.LOGS) || !st.isFaceSturdy(lvl, g, Direction.UP)) return false;
                for (int k = 0; k < 2; k++) {
                    BlockPos q = new BlockPos(c.getX() + dx, y + k, c.getZ() + dz);
                    BlockState s = lvl.getBlockState(q);
                    if (!s.isAir() && !s.canBeReplaced()) return false;
                    if (!lvl.getFluidState(q).isEmpty()) return false;
                }
            }
        }
        return true;
    }

    private static boolean manMade(ServerLevel lvl, BlockPos c) {
        for (int dx = -7; dx <= 7; dx++) {
            for (int dz = -7; dz <= 7; dz++) {
                for (int dy = -2; dy <= 4; dy++) {
                    BlockPos q = c.offset(dx, dy, dz);
                    BlockState s = lvl.getBlockState(q);
                    if (s.isAir()) continue;
                    if (s.is(BlockTags.PLANKS) || s.is(BlockTags.DOORS) || s.is(BlockTags.BEDS) || s.hasBlockEntity() || Traces.isTorch(s)) return true;
                }
            }
        }
        return false;
    }

    private static void place(ServerPlayer p, Profile pr, ServerLevel lvl, BlockPos c, Direction f, BlockPos origin) {
        RandomSource r = p.getRandom();
        BlockPos lecternAt = c.relative(f, 2);
        BlockPos barrelAt = c.relative(f.getOpposite(), 2);
        BlockPos signAt = c.relative(f.getClockWise(), 2);
        for (int[] o : RING) {
            BlockPos q = c.offset(o[0], 0, o[1]);
            if (q.equals(lecternAt) || q.equals(barrelAt) || q.equals(signAt)) continue;
            if (!replaceable(lvl, q)) continue;
            lvl.setBlock(q, Blocks.BLACK_CANDLE.defaultBlockState().setValue(CandleBlock.CANDLES, 1 + r.nextInt(3)).setValue(CandleBlock.LIT, false), 3);
        }
        if (replaceable(lvl, lecternAt)) {
            BlockState lec = Blocks.LECTERN.defaultBlockState().setValue(LecternBlock.FACING, f);
            lvl.setBlock(lecternAt, lec, 3);
            LecternBlock.tryPlaceBook(null, lvl, lecternAt, lvl.getBlockState(lecternAt), Journal.write(p, pr, false));
        }
        if (replaceable(lvl, barrelAt)) {
            lvl.setBlock(barrelAt, Blocks.BARREL.defaultBlockState().setValue(BlockStateProperties.FACING, Direction.UP), 3);
            BlockEntity be = lvl.getBlockEntity(barrelAt);
            if (be instanceof BarrelBlockEntity barrel) fill(p, pr, barrel);
        }
        if (replaceable(lvl, signAt)) {
            int rot = Mth.floor((Haunt.yawTo(Vec3.atCenterOf(signAt), Vec3.atCenterOf(origin)) + 180f) * 16f / 360f + 0.5) & 15;
            lvl.setBlock(signAt, Blocks.DARK_OAK_SIGN.defaultBlockState().setValue(StandingSignBlock.ROTATION, rot), 3);
            BlockEntity be = lvl.getBlockEntity(signAt);
            if (be instanceof SignBlockEntity sign) {
                SignText t = sign.getFrontText().setMessage(1, Component.literal(p.getGameProfile().getName()))
                    .setMessage(2, Component.translatable("cavemind.shrine.sign"));
                sign.setText(t, true);
                sign.setWaxed(true);
                sign.setChanged();
            }
        }
        Effigy.make(p, c, Vec3.atCenterOf(origin), "effigy");
    }

    private static void fill(ServerPlayer p, Profile pr, BarrelBlockEntity barrel) {
        int slot = 0;
        int torches = Math.max(1, pr.dStolen);
        while (torches > 0 && slot < 20) {
            int n = Math.min(64, torches);
            barrel.setItem(slot++, new ItemStack(Items.TORCH, n));
            torches -= n;
        }
        ItemStack tag = new ItemStack(Items.NAME_TAG);
        tag.setHoverName(Component.literal(p.getGameProfile().getName()));
        barrel.setItem(slot++, tag);
        Item sust = ForgeRegistries.ITEMS.getValue(new ResourceLocation("apollyon", "sustenance"));
        if (sust != null && sust != Items.AIR) barrel.setItem(slot++, new ItemStack(sust, 3));
        barrel.setItem(slot, new ItemStack(Items.ROTTEN_FLESH, 7));
        barrel.setChanged();
    }

    private static boolean replaceable(ServerLevel lvl, BlockPos q) {
        BlockState s = lvl.getBlockState(q);
        if (!s.isAir() && !s.canBeReplaced()) return false;
        BlockPos b = q.below();
        return lvl.getBlockState(b).isFaceSturdy(lvl, b, Direction.UP);
    }

    public static boolean lightOne(ServerLevel lvl, BlockPos c) {
        for (int[] o : RING) {
            BlockPos q = c.offset(o[0], 0, o[1]);
            BlockState s = lvl.getBlockState(q);
            if (s.getBlock() instanceof CandleBlock && !s.getValue(CandleBlock.LIT)) {
                lvl.setBlock(q, s.setValue(CandleBlock.LIT, true), 11);
                lvl.playSound(null, q, SoundEvents.FLINTANDSTEEL_USE, SoundSource.BLOCKS, 0.4f, 0.6f);
                return true;
            }
        }
        return false;
    }

    public static void approach(ServerPlayer p, Profile pr) {
        ServerLevel lvl = p.serverLevel();
        BlockPos c = pr.dShrine;
        if (c == null || !lvl.isLoaded(c)) return;
        double d = Math.sqrt(c.distSqr(p.blockPosition()));
        if (d < 7 && !pr.dShrineFound) {
            pr.dShrineFound = true;
            Haunt.advance(p, pr, true);
        }
        long day = Dread.day(lvl);
        for (Direction f : Direction.Plane.HORIZONTAL) {
            BlockPos q = c.relative(f, 2);
            if (!(lvl.getBlockEntity(q) instanceof LecternBlockEntity lec)) continue;
            ItemStack book = lec.getBook();
            boolean ours = !book.isEmpty() && book.hasTag() && book.getTag().getBoolean("cavemind");
            if (ours || book.isEmpty()) {
                if (book.isEmpty()) {
                    BlockState st = lvl.getBlockState(q);
                    LecternBlock.tryPlaceBook(null, lvl, q, st, Journal.write(p, pr, false));
                } else {
                    lec.setBook(Journal.write(p, pr, false));
                    lec.setChanged();
                    lvl.sendBlockUpdated(q, lvl.getBlockState(q), lvl.getBlockState(q), 3);
                }
            }
        }
    }

    public static void linger(ServerPlayer p, Profile pr) {
        ServerLevel lvl = p.serverLevel();
        BlockPos c = pr.dShrine;
        if (c == null || !lvl.isLoaded(c)) return;
        double d = Math.sqrt(c.distSqr(p.blockPosition()));
        if (d < 7 && !pr.dShrineFound) {
            pr.dShrineFound = true;
            Haunt.advance(p, pr, true);
        }
        if (lvl.isNight() && pr.dStage >= 3 && d < 11 && p.getRandom().nextFloat() < 0.5f) {
            if (lightOne(lvl, c)) Fx.sound(p, "apollyon:shallow_breathing", SoundSource.HOSTILE, Vec3.atCenterOf(c.above()), 0.25f, 0.9f);
        }
    }

    public static void ruin(ServerPlayer p, Profile pr) {
        ServerLevel lvl = p.serverLevel();
        BlockPos c = pr.dShrine;
        if (c == null || !lvl.isLoaded(c)) return;
        for (int[] o : RING) {
            BlockPos q = c.offset(o[0], 0, o[1]);
            BlockState s = lvl.getBlockState(q);
            if (s.getBlock() instanceof CandleBlock && s.getValue(CandleBlock.LIT)) lvl.setBlock(q, s.setValue(CandleBlock.LIT, false), 11);
        }
        for (ArmorStand as : lvl.getEntitiesOfClass(ArmorStand.class, new AABB(c).inflate(3), a -> "effigy".equals(Compat.role(a)))) {
            ItemStack head = as.getItemBySlot(EquipmentSlot.HEAD);
            if (!head.isEmpty()) {
                as.setItemSlot(EquipmentSlot.HEAD, ItemStack.EMPTY);
                lvl.addFreshEntity(new ItemEntity(lvl, as.getX(), as.getY() + 1.6, as.getZ(), head));
            }
        }
        for (Direction f : Direction.Plane.HORIZONTAL) {
            BlockPos q = c.relative(f, 2);
            if (lvl.getBlockEntity(q) instanceof LecternBlockEntity lec) {
                lec.setBook(Journal.write(p, pr, true));
                lec.setChanged();
                lvl.sendBlockUpdated(q, lvl.getBlockState(q), lvl.getBlockState(q), 3);
            }
        }
        lvl.playSound(null, c, Compat.sound("apollyon:used_rose", "minecraft:block.respawn_anchor.deplete").value(), SoundSource.AMBIENT, 1f, 0.8f);
    }

    public static boolean near(Profile pr, BlockPos q, double r) {
        return pr.dShrine != null && pr.dShrine.distSqr(q) <= r * r;
    }

    private Shrine() {}
}
