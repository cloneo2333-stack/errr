package vn.cavemind.lore;

import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.BellBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.entity.BrushableBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BellAttachType;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.level.block.state.properties.SlabType;

public final class Playground {
    static final String[] NAMES = {"Linh", "Tuấn", "Mai", "Bảo", "Hà", "Nam", "Khoa"};

    static void build(Lore.B b, Map<String, Long> parts) {
        BlockState grass = Blocks.GRASS_BLOCK.defaultBlockState();
        b.flatten(-8, 8, -8, 8, grass);
        for (int[] q : new int[][]{{-2, 0}, {-1, 1}, {0, 0}, {1, -1}, {0, 2}, {-3, 3}, {3, -2}, {-5, -4}, {5, 1}, {2, 6}}) {
            b.set(q[0], 0, q[1], Blocks.COARSE_DIRT.defaultBlockState());
        }
        tree(b);
        well(b);
        BlockState fence = Blocks.DARK_OAK_FENCE.defaultBlockState();
        b.set(5, 1, -4, fence);
        b.set(5, 2, -4, fence);
        b.set(5, 3, -4, Blocks.BELL.defaultBlockState().setValue(BellBlock.FACING, Direction.EAST).setValue(BellBlock.ATTACHMENT, BellAttachType.FLOOR));
        BlockState plank = Blocks.DARK_OAK_PLANKS.defaultBlockState();
        for (int x = -3; x <= 4; x++) {
            b.set(x, 1, 7, plank);
            b.set(x, 2, 7, Blocks.DARK_OAK_FENCE.defaultBlockState());
            int i = x + 3;
            Component name = i < NAMES.length ? Component.literal(NAMES[i]) : Component.empty();
            b.wallSign(x, 1, 6, Blocks.DARK_OAK_WALL_SIGN, Direction.NORTH, Component.empty(), name, Component.empty(), Component.empty());
        }
        swing(b);
        slide(b);
        BlockState slab = Blocks.DARK_OAK_SLAB.defaultBlockState().setValue(SlabBlock.TYPE, SlabType.BOTTOM);
        b.set(2, 1, -6, fence);
        for (int x = 1; x <= 3; x++) b.set(x, 2, -6, slab);
        b.set(-2, 1, -6, fence);
        b.set(-2, 2, -6, fence);
        for (Direction d : Direction.Plane.HORIZONTAL) {
            b.set(-2 + d.getStepX(), 1, -6 + d.getStepZ(), Blocks.DARK_OAK_TRAPDOOR.defaultBlockState()
                .setValue(TrapDoorBlock.FACING, d).setValue(TrapDoorBlock.OPEN, true).setValue(TrapDoorBlock.HALF, Half.BOTTOM));
        }
        sand(b);
        parts.put("tree", b.at(0, 1, 4).asLong());
        parts.put("lid", b.at(-4, 1, -1).asLong());
        parts.put("bell", b.at(5, 3, -4).asLong());
        parts.put("name8", b.at(4, 1, 6).asLong());
        parts.put("chest", b.at(-1, 1, 3).asLong());
        parts.put("lo", corner(b, -8, -16, -8).asLong());
        parts.put("hi", corner(b, 8, 9, 8).asLong());
        parts.put("tlo", corner(b, -2, 1, 3).asLong());
        parts.put("thi", corner(b, 2, 8, 5).asLong());
        parts.put("wlo", corner(b, -5, -16, -2).asLong());
        parts.put("whi", corner(b, -3, 5, 0).asLong());
    }

    static BlockPos corner(Lore.B b, int x, int y, int z) {
        return b.at(x, y, z);
    }

    static void tree(Lore.B b) {
        BlockState log = Blocks.OAK_LOG.defaultBlockState();
        for (int y = 1; y <= 7; y++) b.set(0, y, 4, log);
        BlockState lx = log.setValue(RotatedPillarBlock.AXIS, Direction.Axis.X), lz = log.setValue(RotatedPillarBlock.AXIS, Direction.Axis.Z);
        b.set(1, 5, 4, lx);
        b.set(2, 6, 4, lx);
        b.set(-1, 6, 4, lx);
        b.set(-2, 7, 4, Blocks.OAK_FENCE.defaultBlockState());
        b.set(0, 7, 5, lz);
        b.set(0, 8, 5, Blocks.OAK_FENCE.defaultBlockState());
        b.set(2, 7, 4, Blocks.OAK_FENCE.defaultBlockState());
        b.wallSign(0, 2, 3, Blocks.OAK_WALL_SIGN, Direction.NORTH, Component.literal("ĐẾM TỚI"), Component.literal("MỘT TRĂM"),
            Component.literal("không được"), Component.literal("quay lại"));
        b.wallSign(1, 2, 4, Blocks.OAK_WALL_SIGN, Direction.EAST, Component.literal("IIIII IIIII"), Component.literal("IIIII IIIII"),
            Component.literal("IIIII IIIII"), Component.literal("IIIII IIIII"));
        b.wallSign(-1, 2, 4, Blocks.OAK_WALL_SIGN, Direction.WEST, Component.literal("IIIII III"), Component.empty(), Component.empty(), Component.empty());
        b.set(-1, 1, 3, Blocks.CHEST.defaultBlockState());
        b.fill(-1, 1, 3, book("Luật chơi", "...",
            "Một người đếm. Những người khác đi trốn.\n\nNgười đếm úp mặt vào cây và không được quay lại.",
            "Ai bị tìm thấy thì thua.\n\nAi thua thì đếm.",
            "Không được nhìn trộm.\nKhông được đánh.\nKhông được đứng chỗ sáng.\nKhông được bỏ về giữa chừng.",
            "Muốn thôi chơi thì rung chuông. Chuông là hết giờ chơi.",
            "Muốn vào chơi ở dưới kia thì đặt một ngọn nến đang cháy lên nắp giếng rồi rung chuông trường.",
            "Chúng mình đã đếm tới một trăm rồi về nhà.\n\nKhông ai quay lại."), new ItemStack(Items.CANDLE, 3));
    }

    static void well(Lore.B b) {
        BlockState ring = Blocks.MOSSY_STONE_BRICKS.defaultBlockState(), wall = Blocks.COBBLESTONE.defaultBlockState(), air = Blocks.AIR.defaultBlockState();
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                if (dx == 0 && dz == 0) continue;
                b.set(-4 + dx, 0, -1 + dz, ring);
                b.set(-4 + dx, 1, -1 + dz, ring);
            }
        }
        for (int y = 0; y >= -15; y--) {
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    if (dx == 0 && dz == 0) b.set(-4, y, -1, air);
                    else if (y < 0) b.set(-4 + dx, y, -1 + dz, wall);
                }
            }
        }
        b.set(-4, -16, -1, wall);
        b.set(-4, 1, -1, Blocks.IRON_TRAPDOOR.defaultBlockState().setValue(TrapDoorBlock.HALF, Half.TOP).setValue(TrapDoorBlock.OPEN, false));
        BlockState fence = Blocks.DARK_OAK_FENCE.defaultBlockState();
        for (int y = 2; y <= 4; y++) {
            b.set(-5, y, -1, fence);
            b.set(-3, y, -1, fence);
        }
        BlockState beam = Blocks.DARK_OAK_SLAB.defaultBlockState().setValue(SlabBlock.TYPE, SlabType.BOTTOM);
        for (int x = -5; x <= -3; x++) b.set(x, 5, -1, beam);
        b.set(-4, 4, -1, Blocks.CHAIN.defaultBlockState());
        b.wallSign(-4, 1, -3, Blocks.SPRUCE_WALL_SIGN, Direction.NORTH, Component.literal("GIẾNG"), Component.literal("THÔNG GIÓ"), Component.literal("SỐ 4"),
            Component.literal("cấm lại gần"));
    }

    static void swing(Lore.B b) {
        BlockState fence = Blocks.DARK_OAK_FENCE.defaultBlockState();
        for (int y = 1; y <= 3; y++) {
            b.set(-7, y, 2, fence);
            b.set(-7, y, 5, fence);
        }
        BlockState beam = Blocks.DARK_OAK_LOG.defaultBlockState().setValue(RotatedPillarBlock.AXIS, Direction.Axis.Z);
        for (int z = 2; z <= 5; z++) b.set(-7, 4, z, beam);
        b.set(-7, 3, 3, Blocks.CHAIN.defaultBlockState());
        b.set(-7, 3, 4, Blocks.CHAIN.defaultBlockState());
        BlockState seat = Blocks.DARK_OAK_SLAB.defaultBlockState().setValue(SlabBlock.TYPE, SlabType.TOP);
        b.set(-7, 2, 3, seat);
        b.set(-7, 2, 4, seat);
    }

    static void slide(Lore.B b) {
        BlockState st = Blocks.DARK_OAK_STAIRS.defaultBlockState().setValue(StairBlock.FACING, Direction.SOUTH);
        b.set(7, 1, 0, st);
        b.set(7, 2, 1, st);
        b.set(7, 3, 2, Blocks.DARK_OAK_PLANKS.defaultBlockState());
        b.set(6, 4, 2, Blocks.DARK_OAK_FENCE.defaultBlockState());
        b.set(8, 4, 2, Blocks.DARK_OAK_FENCE.defaultBlockState());
        BlockState hi = Blocks.SMOOTH_STONE_SLAB.defaultBlockState().setValue(SlabBlock.TYPE, SlabType.TOP);
        BlockState lo = Blocks.SMOOTH_STONE_SLAB.defaultBlockState().setValue(SlabBlock.TYPE, SlabType.BOTTOM);
        b.set(7, 3, 3, lo);
        b.set(7, 2, 4, hi);
        b.set(7, 2, 5, lo);
        b.set(7, 1, 6, hi);
        b.set(7, 1, 7, lo);
    }

    static void sand(Lore.B b) {
        for (int x = 2; x <= 5; x++) {
            for (int z = 0; z <= 2; z++) {
                b.set(x, 0, z, Blocks.SAND.defaultBlockState());
                b.set(x, -1, z, Blocks.SAND.defaultBlockState());
            }
        }
        BlockState rim = Blocks.OAK_SLAB.defaultBlockState().setValue(SlabBlock.TYPE, SlabType.BOTTOM);
        for (int x = 1; x <= 6; x++) {
            b.set(x, 1, -1, rim);
            b.set(x, 1, 3, rim);
        }
        for (int z = 0; z <= 2; z++) {
            b.set(1, 1, z, rim);
            b.set(6, 1, z, rim);
        }
        sus(b, 3, 0, 1, named(Items.IRON_NUGGET, "Kẹp tóc của Hà", "Dính đất cát, bé tí.", "Hà làm rơi lúc chạy về."));
        sus(b, 5, -1, 2, named(Items.PAPER, "Bức vẽ sáp màu", "Hai ông bố đứng cạnh giếng, giống hệt nhau.", "Một người không có bóng."));
    }

    static void sus(Lore.B b, int x, int y, int z, ItemStack item) {
        b.set(x, y, z, Blocks.SUSPICIOUS_SAND.defaultBlockState());
        if (b.lvl.getBlockEntity(b.at(x, y, z)) instanceof BrushableBlockEntity be) {
            CompoundTag t = be.saveWithoutMetadata();
            t.put("item", item.save(new CompoundTag()));
            be.load(t);
            be.setChanged();
        }
    }

    static ItemStack named(net.minecraft.world.item.Item item, String name, String... lore) {
        ItemStack st = new ItemStack(item);
        st.setHoverName(Component.literal(name).withStyle(s -> s.withItalic(false)));
        CompoundTag disp = st.getOrCreateTagElement("display");
        ListTag l = new ListTag();
        for (String line : lore) l.add(StringTag.valueOf(Component.Serializer.toJson(Component.literal(line).withStyle(s -> s.withItalic(true).withColor(0x8a8a8a)))));
        disp.put("Lore", l);
        st.getOrCreateTag().putBoolean("cavemind", true);
        return st;
    }

    static ItemStack book(String title, String author, String... pages) {
        ItemStack book = new ItemStack(Items.WRITTEN_BOOK);
        CompoundTag tag = book.getOrCreateTag();
        tag.putString("title", title);
        tag.putString("author", author);
        tag.putInt("generation", 3);
        ListTag list = new ListTag();
        for (String pg : pages) list.add(StringTag.valueOf(Component.Serializer.toJson(Component.literal(pg))));
        tag.put("pages", list);
        tag.putBoolean("resolved", true);
        tag.putBoolean("cavemind", true);
        return book;
    }

    private Playground() {}
}
