package vn.cavemind.maze;

import java.util.Random;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChainBlock;
import net.minecraft.world.level.block.PipeBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.WallSignBlock;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.entity.SignText;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import vn.cavemind.maze.core.Grid;

final class Decor {
    static final int F = Maze.F;
    static final int[] DX = {1, 0, -1, 0}, DZ = {0, 1, 0, -1};
    static final Direction[] DIR = {Direction.EAST, Direction.SOUTH, Direction.WEST, Direction.NORTH};
    static final String[][] SCRAWL = {
        {"", "bốn mươi tám"}, {"ĐỪNG", "NHÌN"}, {"nó ở", "sau lưng"}, {"IIII IIII", "IIII IIII", "IIII IIII", "IIII IIII"},
        {"Khoa ơi", "về đi"}, {"", "mẹ ơi"}, {"tôi đếm", "xong rồi"}, {"ai thua", "thì đếm"}, {"trốn", "trong tủ"},
        {"", "đừng chạy"}, {"Linh", "ở đâu"}, {"không có", "lối ra"}, {"nó nghe", "thấy hết"}, {"", "…bốn mươi bảy"},
        {"tắt đèn", "đi"}, {"tám", "đứa trẻ"}
    };
    static final Item[] FRAMED = {Items.PAPER, Items.BONE, Items.STICK, Items.RED_DYE, Items.FEATHER, Items.STRING, Items.BOWL, Items.WRITABLE_BOOK};

    static boolean in(int x, int z) {
        return x >= 0 && z >= 0 && x < Grid.S && z < Grid.S;
    }

    static int roomNear(Grid g, int x, int z) {
        for (int k = 0; k < 4; k++) {
            int nx = x + DX[k], nz = z + DZ[k];
            if (!in(nx, nz) || g.tile[nx][nz] != Grid.FLOOR) continue;
            int room = g.room[Grid.cellAt(nx + 0.5, nz + 0.5)];
            if (room >= 0) return room;
        }
        return -1;
    }

    static BlockState wall(int room, int h, Random r) {
        double k = r.nextDouble();
        return switch (room) {
            case Grid.WELL -> h == 1 ? Blocks.MOSSY_STONE_BRICKS.defaultBlockState()
                : k < 0.25 ? Blocks.CRACKED_STONE_BRICKS.defaultBlockState() : k < 0.4 ? Blocks.MOSSY_STONE_BRICKS.defaultBlockState() : Blocks.STONE_BRICKS.defaultBlockState();
            case Grid.CLASS -> h == 1 || h == 4 ? Blocks.SPRUCE_PLANKS.defaultBlockState()
                : k < 0.6 ? Blocks.WHITE_TERRACOTTA.defaultBlockState() : k < 0.85 ? Blocks.LIGHT_GRAY_TERRACOTTA.defaultBlockState() : Blocks.BROWN_TERRACOTTA.defaultBlockState();
            case Grid.DORM -> h == 1 || h == 4 ? Blocks.DARK_OAK_PLANKS.defaultBlockState()
                : k < 0.55 ? Blocks.LIGHT_GRAY_TERRACOTTA.defaultBlockState() : k < 0.85 ? Blocks.GRAY_TERRACOTTA.defaultBlockState() : Blocks.BROWN_TERRACOTTA.defaultBlockState();
            case Grid.STORE -> k < 0.45 ? Blocks.COBBLESTONE.defaultBlockState() : k < 0.7 ? Blocks.STONE.defaultBlockState() : k < 0.85 ? Blocks.ANDESITE.defaultBlockState() : Blocks.MOSSY_COBBLESTONE.defaultBlockState();
            default -> h == 1 || h == 4 ? Blocks.DARK_OAK_PLANKS.defaultBlockState()
                : k < 0.78 ? Blocks.BROWN_TERRACOTTA.defaultBlockState() : k < 0.93 ? Blocks.PACKED_MUD.defaultBlockState() : Blocks.MUD_BRICKS.defaultBlockState();
        };
    }

    static void dress(Maze.Run run) {
        Grid g = run.core.g;
        ServerLevel lvl = run.lvl;
        Random r = new Random(g.seed ^ 0xD3C0L);
        for (int x = 0; x < Grid.S; x++) {
            for (int z = 0; z < Grid.S; z++) {
                if (g.tile[x][z] != Grid.WALL || x % Grid.P == 0 && z % Grid.P == 0) continue;
                int room = roomNear(g, x, z);
                for (int h = 1; h <= 4; h++) lvl.setBlock(run.at(x, F + h, z), wall(room, h, r), 2 | 16);
            }
        }
        for (int x = 0; x < Grid.S; x++) {
            for (int z = 0; z < Grid.S; z++) {
                if (g.tile[x][z] != Grid.FLOOR || g.grate[x][z]) continue;
                int room = g.room[Grid.cellAt(x + 0.5, z + 0.5)];
                if (room < 0) {
                    double k = r.nextDouble();
                    if (k < 0.1) lvl.setBlock(run.at(x, F, z), Blocks.SPRUCE_PLANKS.defaultBlockState(), 2 | 16);
                    else if (k < 0.15) lvl.setBlock(run.at(x, F, z), Blocks.STRIPPED_DARK_OAK_WOOD.defaultBlockState(), 2 | 16);
                }
                if (lvl.getBlockState(run.at(x, F + 4, z)).isAir()) {
                    double c = r.nextDouble();
                    if (c < 0.035) lvl.setBlock(run.at(x, F + 4, z), Blocks.HANGING_ROOTS.defaultBlockState(), 2 | 16);
                    else if (c < 0.05) lvl.setBlock(run.at(x, F + 4, z), Blocks.CHAIN.defaultBlockState().setValue(ChainBlock.AXIS, Direction.Axis.Y), 2 | 16);
                }
                for (int k = 0; k < 4; k++) {
                    int wx = x + DX[k], wz = z + DZ[k];
                    if (!in(wx, wz) || g.tile[wx][wz] != Grid.WALL) continue;
                    Direction out = DIR[k].getOpposite();
                    BooleanProperty side = PipeBlock.PROPERTY_BY_DIRECTION.get(DIR[k]);
                    double v = r.nextDouble();
                    if (v < 0.05) {
                        BlockPos q = run.at(x, F + 3 + r.nextInt(2), z);
                        BlockState st = lvl.getBlockState(q);
                        if (st.isAir()) lvl.setBlock(q, Blocks.VINE.defaultBlockState().setValue(side, true), 2 | 16);
                        else if (st.is(Blocks.VINE)) lvl.setBlock(q, st.setValue(side, true), 2 | 16);
                    } else if (v < 0.09) {
                        BlockPos q = run.at(x, F + 1 + r.nextInt(3), z);
                        BlockState st = lvl.getBlockState(q);
                        if (st.isAir()) lvl.setBlock(q, Blocks.SCULK_VEIN.defaultBlockState().setValue(side, true), 2 | 16);
                    } else if (v < 0.105) {
                        BlockPos q = run.at(x, F + 2, z);
                        if (lvl.getBlockState(q).isAir()) lvl.setBlock(q, vn.cavemind.gear.Scrawl.of(out, vn.cavemind.gear.Scrawl.CORRIDOR + r.nextInt(16)), 2);
                    } else if (v < 0.115) {
                        BlockPos q = run.at(x, F + 2, z);
                        if (lvl.getBlockState(q).isAir()) frame(lvl, q, out, new ItemStack(FRAMED[r.nextInt(FRAMED.length)]), r);
                    }
                }
            }
        }
        for (int t = 0; t < 14; t++) {
            int x = 1 + r.nextInt(Grid.S - 2), z = 1 + r.nextInt(Grid.S - 2);
            int len = 4 + r.nextInt(7), dir = r.nextInt(4);
            for (int i = 0; i < len; i++) {
                if (!in(x, z) || g.tile[x][z] != Grid.FLOOR || g.grate[x][z]) break;
                BlockPos q = run.at(x, F + 1, z);
                if (lvl.getBlockState(q).isAir() && r.nextFloat() < 0.75f) lvl.setBlock(q, Blocks.REDSTONE_WIRE.defaultBlockState(), 2 | 16);
                if (r.nextFloat() < 0.25f) dir = (dir + (r.nextBoolean() ? 1 : 3)) % 4;
                x += DX[dir];
                z += DZ[dir];
            }
        }
        for (Grid.Closet c : g.closets) {
            BlockPos top = run.at(c.x, F + 4, c.z);
            if (r.nextFloat() < 0.5f) lvl.setBlock(top, Blocks.STRIPPED_DARK_OAK_LOG.defaultBlockState().setValue(RotatedPillarBlock.AXIS, Direction.Axis.Y), 2 | 16);
        }
    }

    static final String[][] FURNITURE = {
        {},
        {"refurbished_furniture:dark_oak_desk", "refurbished_furniture:dark_oak_desk", "refurbished_furniture:dark_oak_chair", "dustydecorations:bookstack", "horror_element_mod:broken_tv", "dustydecorations:small_bookshelf"},
        {"creepydecorations:broken_doll", "dustydecorations:plushy", "dustydecorations:plushy_3", "dustydecorations:plushy_5", "dustydecorations:small_bookshelf", "refurbished_furniture:dark_oak_drawer"},
        {"refurbished_furniture:dark_oak_crate", "dustydecorations:burlap_sack", "dustydecorations:burlap_sack_stack", "dustydecorations:empty_barrel", "dustydecorations:jars"},
        {"refurbished_furniture:light_gray_toilet", "refurbished_furniture:light_gray_basin", "refurbished_furniture:light_gray_basin", "horror_element_mod:broken_tv"}
    };

    static void furnish(Maze.Run run) {
        Grid g = run.core.g;
        ServerLevel lvl = run.lvl;
        Random r = new Random(g.seed ^ 0xF0F0L);
        for (double[] t : g.toys) run.blocked.add((int) Math.floor(t[0]) * 4096 + (int) Math.floor(t[1]));
        for (double[] t : new double[][]{g.chest, g.bell, g.spawn}) run.blocked.add((int) Math.floor(t[0]) * 4096 + (int) Math.floor(t[1]));
        for (int id = 1; id < g.rooms.length && id < FURNITURE.length; id++) {
            int bx = g.rooms[id][0] * Grid.P, bz = g.rooms[id][1] * Grid.P;
            for (int x = bx + 1; x <= bx + 11; x++) {
                for (int z = bz + 1; z <= bz + 11; z++) {
                    if (g.tile[x][z] != Grid.FLOOR || g.grate[x][z] || g.lamp[x][z] || run.blocked.contains(x * 4096 + z)) continue;
                    int wallDir = -1;
                    boolean bad = false;
                    for (int k = 0; k < 4; k++) {
                        int nx = x + DX[k], nz = z + DZ[k];
                        if (!in(nx, nz)) continue;
                        byte t = g.tile[nx][nz];
                        if (t == Grid.CLOSET || t == Grid.NOOK || t == Grid.PILLAR) bad = true;
                        if (t == Grid.WALL) {
                            int ox = nx + DX[k], oz = nz + DZ[k];
                            if (in(ox, oz) && g.tile[ox][oz] == Grid.FLOOR && g.room[Grid.cellAt(ox + 0.5, oz + 0.5)] != id && (nx % Grid.P != 0 || nz % Grid.P != 0)) wallDir = k;
                            else if (wallDir < 0) wallDir = k;
                        }
                    }
                    if (bad || wallDir < 0) continue;
                    boolean doorway = false;
                    for (int k = 0; k < 4; k++) {
                        int nx = x + DX[k], nz = z + DZ[k];
                        if (in(nx, nz) && g.tile[nx][nz] == Grid.FLOOR && g.room[Grid.cellAt(nx + 0.5, nz + 0.5)] != id) doorway = true;
                        int sx = x + DX[k] * 2, sz = z + DZ[k] * 2;
                        if (in(sx, sz) && g.tile[x + DX[k]][z + DZ[k]] == Grid.FLOOR && g.tile[sx][sz] == Grid.FLOOR && g.room[Grid.cellAt(sx + 0.5, sz + 0.5)] != id) doorway = true;
                    }
                    if (doorway || r.nextFloat() > 0.3f) continue;
                    String[] set = FURNITURE[id];
                    BlockState st = vn.cavemind.world.Assets.get(set[r.nextInt(set.length)], null);
                    if (st == null) continue;
                    st = vn.cavemind.world.Assets.face(st, DIR[wallDir].getOpposite());
                    BlockPos q = run.at(x, F + 1, z);
                    if (!lvl.getBlockState(q).isAir() && !lvl.getBlockState(q).canBeReplaced()) continue;
                    lvl.setBlock(q, st, 2 | 16);
                    run.blocked.add(x * 4096 + z);
                }
            }
        }
        BlockState paper = vn.cavemind.world.Assets.get("dustydecorations:scattered_papers_2", null);
        BlockState doll = vn.cavemind.world.Assets.get("creepydecorations:broken_doll", null);
        for (int i = 0; i < 26; i++) {
            int x = 1 + r.nextInt(Grid.S - 2), z = 1 + r.nextInt(Grid.S - 2);
            if (g.tile[x][z] != Grid.FLOOR || g.grate[x][z] || run.blocked.contains(x * 4096 + z)) continue;
            int walls = 0, wd = 0;
            for (int k = 0; k < 4; k++) if (g.tile[x + DX[k]][z + DZ[k]] == Grid.WALL) {
                walls++;
                wd = k;
            }
            if (walls == 0 || g.room[Grid.cellAt(x + 0.5, z + 0.5)] >= 0) continue;
            BlockState st = i % 5 == 0 ? doll : paper;
            if (st == null) continue;
            BlockPos q = run.at(x, F + 1, z);
            if (!lvl.getBlockState(q).isAir()) continue;
            lvl.setBlock(q, vn.cavemind.world.Assets.face(st, DIR[wd].getOpposite()), 2 | 16);
            run.blocked.add(x * 4096 + z);
        }
    }

    static void sign(ServerLevel lvl, BlockPos q, Direction facing, String[] lines) {
        lvl.setBlock(q, Blocks.DARK_OAK_WALL_SIGN.defaultBlockState().setValue(WallSignBlock.FACING, facing), 2);
        if (lvl.getBlockEntity(q) instanceof SignBlockEntity sb) {
            SignText t = sb.getFrontText().setColor(net.minecraft.world.item.DyeColor.RED);
            for (int i = 0; i < 4 && i < lines.length; i++) t = t.setMessage(i, Component.literal(lines[i]));
            sb.setText(t, true);
            sb.setWaxed(true);
            sb.setChanged();
        }
    }

    static void frame(ServerLevel lvl, BlockPos q, Direction facing, ItemStack item, Random r) {
        ItemFrame f = new ItemFrame(lvl, q, facing);
        f.setItem(item, false);
        f.setRotation(r.nextInt(8));
        f.setInvulnerable(true);
        net.minecraft.nbt.CompoundTag t = new net.minecraft.nbt.CompoundTag();
        f.saveWithoutId(t);
        t.putBoolean("Fixed", true);
        f.load(t);
        if (f.survives()) lvl.addFreshEntity(f);
    }

    private Decor() {}
}
