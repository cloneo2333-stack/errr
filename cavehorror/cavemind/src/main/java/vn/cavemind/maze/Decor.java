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
import net.minecraft.world.level.block.CandleBlock;
import net.minecraft.world.level.block.ChainBlock;
import net.minecraft.world.level.block.LightningRodBlock;
import net.minecraft.world.level.block.PipeBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.TripWireBlock;
import net.minecraft.world.level.block.WallSignBlock;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.entity.SignText;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.level.block.state.properties.SlabType;
import vn.cavemind.gear.Vet;
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

    // Old yellowed lime plaster; now and then it has flaked off down to the brick.
    static BlockState plaster(double k) {
        return (k < 0.6 ? Blocks.SMOOTH_SANDSTONE : k < 0.74 ? Blocks.SANDSTONE : k < 0.86 ? Blocks.WHITE_TERRACOTTA : k < 0.94 ? Blocks.BRICKS : Blocks.PACKED_MUD).defaultBlockState();
    }

    // The lower metre: rotten dark panelling, or black brick sweating with moss.
    static BlockState wainscot(double k, boolean brick) {
        if (brick) return (k < 0.4 ? Blocks.DEEPSLATE_BRICKS : k < 0.65 ? Blocks.CRACKED_DEEPSLATE_BRICKS : k < 0.87 ? Blocks.MOSSY_COBBLESTONE : Blocks.MUD_BRICKS).defaultBlockState();
        return (k < 0.55 ? Blocks.STRIPPED_DARK_OAK_WOOD : k < 0.8 ? Blocks.DARK_OAK_PLANKS : k < 0.92 ? Blocks.SPRUCE_PLANKS : Blocks.PACKED_MUD).defaultBlockState();
    }

    static BlockState tile(double k) {
        return (k < 0.55 ? Blocks.QUARTZ_BRICKS : k < 0.7 ? Blocks.POLISHED_DIORITE : k < 0.8 ? Blocks.CALCITE : k < 0.92 ? Blocks.END_STONE_BRICKS : k < 0.97 ? Blocks.MUD_BRICKS
            : Blocks.CRACKED_STONE_BRICKS).defaultBlockState();
    }

    static BlockState wall(int room, int h, Random r, boolean brick) {
        double k = r.nextDouble();
        return switch (room) {
            case Grid.WELL -> h == 1 ? Blocks.MOSSY_STONE_BRICKS.defaultBlockState()
                : k < 0.25 ? Blocks.CRACKED_STONE_BRICKS.defaultBlockState() : k < 0.4 ? Blocks.MOSSY_STONE_BRICKS.defaultBlockState() : Blocks.STONE_BRICKS.defaultBlockState();
            // the classroom keeps its painted green-blue dado under the whitewash
            case Grid.CLASS -> h <= 2 ? (k < 0.86 ? Blocks.CYAN_TERRACOTTA : k < 0.95 ? Blocks.LIGHT_GRAY_TERRACOTTA : Blocks.BRICKS).defaultBlockState() : plaster(k);
            case Grid.DORM -> h == 1 ? wainscot(k, false) : k < 0.5 ? Blocks.WHITE_TERRACOTTA.defaultBlockState() : plaster(r.nextDouble());
            case Grid.STORE -> k < 0.45 ? Blocks.COBBLESTONE.defaultBlockState() : k < 0.7 ? Blocks.STONE.defaultBlockState() : k < 0.85 ? Blocks.ANDESITE.defaultBlockState() : Blocks.MOSSY_COBBLESTONE.defaultBlockState();
            case Grid.BATH -> tile(k);
            default -> h == 1 ? wainscot(k, brick) : h == 4 && k < 0.06 ? Blocks.DRIPSTONE_BLOCK.defaultBlockState() : plaster(r.nextDouble());
        };
    }

    static boolean plastered(int room) {
        return room < 0 || room == Grid.CLASS || room == Grid.DORM;
    }

    static void dress(Maze.Run run) {
        Grid g = run.core.g;
        ServerLevel lvl = run.lvl;
        Random r = new Random(g.seed ^ 0xD3C0L);
        for (int x = 0; x < Grid.S; x++) {
            for (int z = 0; z < Grid.S; z++) {
                if (g.tile[x][z] != Grid.WALL || x % Grid.P == 0 && z % Grid.P == 0) continue;
                int room = roomNear(g, x, z);
                boolean brick = Math.floorMod((x / Grid.P) * 7 + (z / Grid.P) * 13 + (x % Grid.P == 0 ? 5 : 0) + (int) g.seed, 3) == 0;
                for (int h = 1; h <= 4; h++) lvl.setBlock(run.at(x, F + h, z), wall(room, h, r, brick), 2 | 16);
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
        Random q = new Random(g.seed ^ 0xA9EDL);
        boarded(run, q);
        closets(run);
        vents(run, q);
        ceiling(run, q);
        deadEnds(run, q);
        grime(run, q);
        walls(run, q);
        well(run, q);
    }

    static boolean floor(Grid g, int x, int z) {
        return in(x, z) && g.tile[x][z] == Grid.FLOOR && !g.grate[x][z];
    }

    static boolean put(Maze.Run run, int x, int y, int z, BlockState st) {
        BlockPos q = run.at(x, y, z);
        if (!run.lvl.getBlockState(q).isAir()) return false;
        run.lvl.setBlock(q, st, 2 | 16);
        return true;
    }

    static BlockState axis(BlockState st, Direction.Axis a) {
        return st.hasProperty(BlockStateProperties.AXIS) ? st.setValue(BlockStateProperties.AXIS, a) : st;
    }

    static BlockState beam(Direction.Axis a) {
        return axis(vn.cavemind.world.Assets.get("decorative_blocks:dark_oak_beam", Blocks.STRIPPED_DARK_OAK_LOG.defaultBlockState()), a);
    }

    // Doorways nobody is meant to use any more: a frame, planks behind, rotten boards nailed across in an X.
    static void boarded(Maze.Run run, Random r) {
        Grid g = run.core.g;
        for (int c = 0; c < Grid.N * Grid.N; c++) {
            int i = Grid.ci(c), j = Grid.cj(c);
            for (int k = 0; k < 2; k++) {
                int ni = i + DX[k], nj = j + DZ[k];
                if (ni >= Grid.N || nj >= Grid.N || g.open[c][k] || r.nextDouble() > 0.11) continue;
                int d = Grid.cell(ni, nj);
                if (g.room[c] >= 0 && g.room[c] == g.room[d]) continue;
                int mx = k == 0 ? (i + 1) * Grid.P : i * Grid.P + 2, mz = k == 0 ? j * Grid.P + 2 : (j + 1) * Grid.P;
                int px = k == 0 ? 0 : 1, pz = k == 0 ? 1 : 0;
                if (g.tile[mx][mz] != Grid.WALL || g.tile[mx + px][mz + pz] != Grid.WALL || g.tile[mx - px][mz - pz] != Grid.WALL) continue;
                int ax = mx - DX[k], az = mz - DZ[k], bx = mx + DX[k], bz = mz + DZ[k];
                if (!floor(g, ax, az) || !floor(g, bx, bz)) continue;
                BlockState jamb = Blocks.STRIPPED_DARK_OAK_LOG.defaultBlockState().setValue(RotatedPillarBlock.AXIS, Direction.Axis.Y);
                for (int h = 1; h <= 3; h++) {
                    run.lvl.setBlock(run.at(mx + px, F + h, mz + pz), jamb, 2 | 16);
                    run.lvl.setBlock(run.at(mx - px, F + h, mz - pz), jamb, 2 | 16);
                }
                run.lvl.setBlock(run.at(mx, F + 3, mz), Blocks.STRIPPED_DARK_OAK_LOG.defaultBlockState().setValue(RotatedPillarBlock.AXIS, k == 0 ? Direction.Axis.Z : Direction.Axis.X), 2 | 16);
                run.lvl.setBlock(run.at(mx, F + 1, mz), Blocks.DARK_OAK_PLANKS.defaultBlockState(), 2 | 16);
                run.lvl.setBlock(run.at(mx, F + 2, mz), Blocks.DARK_OAK_PLANKS.defaultBlockState(), 2 | 16);
                for (int h = 1; h <= 2; h++) {
                    put(run, ax, F + h, az, Vet.wall(DIR[(k + 2) % 4], Vet.BOARDS));
                    put(run, bx, F + h, bz, Vet.wall(DIR[k], Vet.BOARDS));
                }
            }
        }
    }

    // A deep arc gouged into the floor in front of every closet, where the warped door scrapes open.
    static void closets(Maze.Run run) {
        Grid g = run.core.g;
        for (Grid.Closet c : g.closets) {
            int fx = c.x + DX[c.face], fz = c.z + DZ[c.face];
            if (floor(g, fx, fz)) put(run, fx, F + 1, fz, Vet.floor(DIR[(c.face + 2) % 4], Vet.ARC));
        }
    }

    // Where the Damned comes up: mud and handprints around the grate, and a pried-off cover leaning on the wall.
    static void vents(Maze.Run run, Random r) {
        Grid g = run.core.g;
        for (int x = 0; x < Grid.S; x++) {
            for (int z = 0; z < Grid.S; z++) {
                if (!g.grate[x][z]) continue;
                boolean cover = false;
                for (int dx = -1; dx <= 1; dx++) {
                    for (int dz = -1; dz <= 1; dz++) {
                        int qx = x + dx, qz = z + dz;
                        if ((dx != 0 || dz != 0) && floor(g, qx, qz) && !run.blocked.contains(qx * 4096 + qz) && r.nextDouble() < 0.45)
                            put(run, qx, F + 1, qz, Vet.floor(DIR[r.nextInt(4)], r.nextBoolean() ? Vet.MUD : Vet.HANDS));
                    }
                }
                for (int k = 0; k < 4 && !cover; k++) {
                    int qx = x + DX[k], qz = z + DZ[k];
                    if (!floor(g, qx, qz) || run.blocked.contains(qx * 4096 + qz)) continue;
                    for (int w = 0; w < 4; w++) {
                        int wx = qx + DX[w], wz = qz + DZ[w];
                        if (!in(wx, wz) || g.tile[wx][wz] != Grid.WALL) continue;
                        BlockState lid = Blocks.IRON_TRAPDOOR.defaultBlockState().setValue(TrapDoorBlock.OPEN, true).setValue(TrapDoorBlock.HALF, Half.BOTTOM)
                            .setValue(TrapDoorBlock.FACING, DIR[w].getOpposite());
                        BlockPos at = run.at(qx, F + 1, qz);
                        if (run.lvl.getBlockState(at).isAir() || run.lvl.getBlockState(at).is(vn.cavemind.gear.Gear.VET_SAN.get())) {
                            run.lvl.setBlock(at, lid, 2 | 16);
                            run.blocked.add(qx * 4096 + qz);
                            cover = true;
                        }
                        break;
                    }
                }
            }
        }
    }

    static boolean straight(Grid g, int c, boolean alongX) {
        return alongX ? g.open[c][0] && g.open[c][2] && !g.open[c][1] && !g.open[c][3] : g.open[c][1] && g.open[c][3] && !g.open[c][0] && !g.open[c][2];
    }

    // Low rotten beams across corridors and rooms, rusty pipes along the wall tops dripping onto puddles, sagging cables.
    static void ceiling(Maze.Run run, Random r) {
        Grid g = run.core.g;
        for (int c = 0; c < Grid.N * Grid.N; c++) {
            if (g.room[c] >= 0) continue;
            boolean ew = straight(g, c, true), ns = straight(g, c, false);
            if (!ew && !ns) continue;
            int x0 = Grid.ci(c) * Grid.P + 1, z0 = Grid.cj(c) * Grid.P + 1;
            if (r.nextDouble() < 0.4) {
                for (int t = 0; t < 3; t++) {
                    if (ew) put(run, x0 + 1, F + 4, z0 + t, beam(Direction.Axis.Z));
                    else put(run, x0 + t, F + 4, z0 + 1, beam(Direction.Axis.X));
                }
            }
            if (r.nextDouble() < 0.32) {
                int side = r.nextBoolean() ? 0 : 2;
                BlockState pipe = Blocks.LIGHTNING_ROD.defaultBlockState().setValue(LightningRodBlock.FACING, ew ? Direction.EAST : Direction.SOUTH);
                for (int t = -1; t <= 3; t++) {
                    int x = ew ? x0 + t : x0 + side, z = ew ? z0 + side : z0 + t;
                    if (in(x, z) && g.tile[x][z] == Grid.FLOOR) put(run, x, F + 4, z, pipe);
                }
                int dx = ew ? x0 + 1 + r.nextInt(2) - r.nextInt(2) : x0 + side, dz = ew ? z0 + side : z0 + 1 + r.nextInt(2) - r.nextInt(2);
                if (run.lvl.getBlockState(run.at(dx, F + 4, dz)).is(Blocks.LIGHTNING_ROD)) {
                    run.drips.add(run.at(dx, F + 4, dz));
                    if (floor(g, dx, dz)) put(run, dx, F + 1, dz, Vet.floor(DIR[r.nextInt(4)], Vet.PUDDLE));
                }
            }
            if (r.nextDouble() < 0.14) {
                int row = r.nextBoolean() ? 0 : 2;
                for (int t = 0; t < 3; t++) {
                    int x = ew ? x0 + row : x0 + t, z = ew ? z0 + t : z0 + row;
                    BlockState wire = Blocks.TRIPWIRE.defaultBlockState().setValue(ew ? TripWireBlock.NORTH : TripWireBlock.EAST, true).setValue(ew ? TripWireBlock.SOUTH : TripWireBlock.WEST, true);
                    put(run, x, F + 4, z, wire);
                }
            }
            if (r.nextDouble() < 0.1) {
                int x = x0 + r.nextInt(3), z = z0 + r.nextInt(3);
                BlockState chain = Blocks.CHAIN.defaultBlockState().setValue(ChainBlock.AXIS, Direction.Axis.Y);
                if (!g.lamp[x][z] && put(run, x, F + 4, z, chain)) put(run, x, F + 3, z, chain);
            }
        }
        for (int id = 1; id < g.rooms.length; id++) {
            int bx = g.rooms[id][0] * Grid.P, bz = g.rooms[id][1] * Grid.P;
            boolean alongX = r.nextBoolean();
            for (int a = 2; a <= 10; a += 4) {
                for (int t = 1; t <= 11; t++) {
                    int x = alongX ? bx + t : bx + a, z = alongX ? bz + a : bz + t;
                    if (in(x, z) && g.tile[x][z] == Grid.FLOOR && !g.lamp[x][z]) put(run, x, F + 4, z, beam(alongX ? Direction.Axis.X : Direction.Axis.Z));
                }
            }
        }
    }

    static BlockState piece(Random r, boolean desk) {
        BlockState st = desk ? vn.cavemind.world.Assets.pick(Blocks.SPRUCE_SLAB.defaultBlockState().setValue(SlabBlock.TYPE, SlabType.TOP), "refurbished_furniture:oak_desk", "refurbished_furniture:dark_oak_desk")
            : vn.cavemind.world.Assets.pick(Blocks.SPRUCE_STAIRS.defaultBlockState(), "refurbished_furniture:oak_chair", "refurbished_furniture:dark_oak_chair");
        st = vn.cavemind.world.Assets.face(st, DIR[r.nextInt(4)]);
        if (st.getBlock() instanceof StairBlock && r.nextBoolean()) st = st.setValue(StairBlock.HALF, Half.TOP);
        return st;
    }

    // Old school desks and chairs piled up against the end of blind corridors.
    static void deadEnds(Maze.Run run, Random r) {
        Grid g = run.core.g;
        java.util.Set<Integer> taken = new java.util.HashSet<>();
        for (Grid.Closet cl : g.closets) taken.add(cl.cell);
        for (Grid.Nook nk : g.nooks) taken.add(nk.cell);
        for (double[] t : g.toys) taken.add(Grid.cellAt(t[0], t[1]));
        for (int c = 0; c < Grid.N * Grid.N; c++) {
            if (g.room[c] >= 0 || taken.contains(c) || r.nextDouble() > 0.6) continue;
            int open = -1, n = 0;
            for (int k = 0; k < 4; k++) if (g.open[c][k]) {
                open = k;
                n++;
            }
            if (n != 1) continue;
            int far = (open + 2) % 4, cx = Grid.ci(c) * Grid.P + 2 + DX[far], cz = Grid.cj(c) * Grid.P + 2 + DZ[far];
            int px = DX[(far + 1) % 4], pz = DZ[(far + 1) % 4];
            for (int t = -1; t <= 1; t++) {
                int x = cx + px * t, z = cz + pz * t;
                if (!floor(g, x, z) || run.blocked.contains(x * 4096 + z) || g.lamp[x][z]) continue;
                boolean desk = t != 0 || r.nextBoolean();
                if (!put(run, x, F + 1, z, t == 0 && r.nextBoolean() ? vn.cavemind.world.Assets.get("dustydecorations:empty_barrel", Blocks.BARREL.defaultBlockState()) : piece(r, desk))) continue;
                if (r.nextDouble() < 0.75) put(run, x, F + 2, z, piece(r, !desk));
                run.blocked.add(x * 4096 + z);
            }
        }
    }

    // Floors: cracked and stained bathroom tiles, shallow puddles, wet patches, drag marks.
    static void grime(Maze.Run run, Random r) {
        Grid g = run.core.g;
        for (int x = 0; x < Grid.S; x++) {
            for (int z = 0; z < Grid.S; z++) {
                if (!floor(g, x, z)) continue;
                int room = g.room[Grid.cellAt(x + 0.5, z + 0.5)];
                double k = r.nextDouble();
                if (room == Grid.BATH) {
                    if (r.nextDouble() < 0.6) run.lvl.setBlock(run.at(x, F, z), tile(r.nextDouble()), 2 | 16);
                    if (k < 0.1 && !run.blocked.contains(x * 4096 + z)) put(run, x, F + 1, z, Vet.floor(DIR[r.nextInt(4)], Vet.PUDDLE));
                    continue;
                }
                if (run.blocked.contains(x * 4096 + z)) continue;
                if (k < 0.018) put(run, x, F + 1, z, Vet.floor(DIR[r.nextInt(4)], Vet.WET));
                else if (k < 0.028) put(run, x, F + 1, z, Vet.floor(DIR[r.nextInt(4)], Vet.PUDDLE));
                else if (k < 0.035 && room < 0) put(run, x, F + 1, z, Vet.floor(DIR[r.nextInt(4)], Vet.DRAG));
            }
        }
    }

    // Walls: black groundwater streaks from the ceiling to the floor, flaked plaster showing brick, cracks, mould, a hand, claw marks.
    static void walls(Maze.Run run, Random r) {
        Grid g = run.core.g;
        for (int x = 0; x < Grid.S; x++) {
            for (int z = 0; z < Grid.S; z++) {
                if (!floor(g, x, z)) continue;
                int room = g.room[Grid.cellAt(x + 0.5, z + 0.5)];
                for (int k = 0; k < 4; k++) {
                    int wx = x + DX[k], wz = z + DZ[k];
                    if (!in(wx, wz) || g.tile[wx][wz] != Grid.WALL && g.tile[wx][wz] != Grid.PILLAR) continue;
                    Direction out = DIR[k].getOpposite();
                    double v = r.nextDouble();
                    if (v < 0.08) {
                        int top = 4;
                        while (top > 1 && !run.lvl.getBlockState(run.at(x, F + top, z)).isAir()) top--;
                        for (int h = top; h >= 1; h--) put(run, x, F + h, z, Vet.wall(out, h == top ? Vet.STAIN_TOP : h == 1 ? Vet.STAIN_BOT : Vet.STAIN_MID));
                    } else if (v < 0.125 && plastered(room)) put(run, x, F + 2 + r.nextInt(2), z, Vet.wall(out, Vet.PEEL));
                    else if (v < 0.16) put(run, x, F + 3 + r.nextInt(2), z, Vet.wall(out, Vet.CRACK));
                    else if (v < 0.22) put(run, x, F + 1, z, Vet.wall(out, Vet.MOLD));
                    else if (v < 0.226) put(run, x, F + 2, z, Vet.wall(out, Vet.HAND));
                    else if (v < 0.232) put(run, x, F + 2, z, Vet.wall(out, Vet.CLAW));
                }
            }
        }
    }

    // Where everyone lands: decades of white candle wax spilled around the spot, the shaft open above with its chain.
    static void well(Maze.Run run, Random r) {
        Grid g = run.core.g;
        int sx = (int) Math.floor(g.spawn[0]), sz = (int) Math.floor(g.spawn[1]);
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                if (dx == 0 && dz == 0 || !floor(g, sx + dx, sz + dz)) continue;
                if (dx != 0 && dz != 0 && r.nextDouble() < 0.75) {
                    put(run, sx + dx, F + 1, sz + dz, Blocks.WHITE_CANDLE.defaultBlockState().setValue(CandleBlock.CANDLES, 2 + r.nextInt(3)).setValue(CandleBlock.LIT, false));
                } else put(run, sx + dx, F + 1, sz + dz, Vet.floor(DIR[r.nextInt(4)], Vet.WAX));
            }
        }
        BlockState chain = Blocks.CHAIN.defaultBlockState().setValue(ChainBlock.AXIS, Direction.Axis.Y);
        run.lvl.setBlock(run.at(sx, F + 5, sz), Blocks.AIR.defaultBlockState(), 2 | 16);
        for (int y = F + 4; y <= F + 7; y++) run.lvl.setBlock(run.at(sx, y, sz), chain, 2 | 16);
        run.lvl.setBlock(run.at(sx, F + 8, sz), Blocks.DEEPSLATE.defaultBlockState(), 2 | 16);
    }

    // Each toy waits in its own little scene, and the scene tells its story before you touch it.
    static void corners(Maze.Run run, Random r) {
        Grid g = run.core.g;
        ServerLevel lvl = run.lvl;
        for (int i = 0; i < 7; i++) {
            int x = (int) Math.floor(g.toys[i][0]), z = (int) Math.floor(g.toys[i][1]);
            if (!floor(g, x, z)) continue;
            Direction face = DIR[r.nextInt(4)];
            switch (i) {
                case 0 -> {
                    put(run, x, F + 1, z, Blocks.SPRUCE_STAIRS.defaultBlockState().setValue(StairBlock.FACING, face));
                    run.toyY[i] = F + 2.2;
                    scatter(run, r, x, z, vn.cavemind.world.Assets.get("dustydecorations:scattered_papers_2", null));
                }
                case 1 -> put(run, x, F + 1, z, Vet.floor(face, Vet.BOARD));
                case 2 -> {
                    put(run, x, F + 1, z, Blocks.SPRUCE_STAIRS.defaultBlockState().setValue(StairBlock.FACING, face).setValue(StairBlock.HALF, Half.TOP));
                    run.toyY[i] = F + 2.2;
                    scatter(run, r, x, z, Blocks.SPRUCE_STAIRS.defaultBlockState().setValue(StairBlock.FACING, DIR[r.nextInt(4)]));
                    scatter(run, r, x, z, vn.cavemind.world.Assets.get("dustydecorations:scattered_papers_2", null));
                }
                case 3 -> put(run, x, F + 1, z, Vet.floor(face, Vet.CHALK));
                case 5 -> {
                    BlockState chain = Blocks.CHAIN.defaultBlockState().setValue(ChainBlock.AXIS, Direction.Axis.Y);
                    if (put(run, x, F + 2, z, Blocks.SPRUCE_SLAB.defaultBlockState().setValue(SlabBlock.TYPE, SlabType.TOP))) {
                        put(run, x, F + 3, z, chain);
                        put(run, x, F + 4, z, chain);
                        run.toyY[i] = F + 3.2;
                        run.toyAt[i] = new double[]{x + 0.8, z + 0.5};
                    }
                    put(run, x, F + 1, z, Vet.floor(face, Vet.PUDDLE));
                }
                case 6 -> {
                    if (put(run, x, F + 1, z, Blocks.DARK_OAK_FENCE.defaultBlockState())) {
                        put(run, x, F + 2, z, Blocks.BROWN_CARPET.defaultBlockState());
                        run.toyY[i] = F + 2.3;
                    }
                    for (int n = 0; n < 2; n++) scatter(run, r, x, z, Blocks.CANDLE.defaultBlockState().setValue(CandleBlock.CANDLES, 1 + r.nextInt(3)).setValue(CandleBlock.LIT, false));
                    scatter(run, r, x, z, Vet.floor(DIR[r.nextInt(4)], Vet.WAX));
                }
                default -> {}
            }
            for (int dx = -1; dx <= 1; dx++) for (int dz = -1; dz <= 1; dz++) run.blocked.add((x + dx) * 4096 + z + dz);
        }
    }

    static void scatter(Maze.Run run, Random r, int x, int z, BlockState st) {
        if (st == null) return;
        Grid g = run.core.g;
        for (int t = 0; t < 6; t++) {
            int k = r.nextInt(4), qx = x + DX[k], qz = z + DZ[k];
            if (floor(g, qx, qz) && put(run, qx, F + 1, qz, st)) return;
        }
    }

    // Tall bookshelves leaning off the classroom and dormitory walls, leaving thin black gaps between them.
    static void shelves(Maze.Run run, Random r) {
        Grid g = run.core.g;
        for (int id : new int[]{Grid.CLASS, Grid.DORM}) {
            int bx = g.rooms[id][0] * Grid.P, bz = g.rooms[id][1] * Grid.P;
            for (int tries = 0, made = 0; tries < 20 && made < 2; tries++) {
                int side = r.nextInt(4), t = 2 + r.nextInt(7);
                int[] along = side % 2 == 0 ? new int[]{1, 0} : new int[]{0, 1};
                int x0 = switch (side) {
                    case 0, 2 -> bx + t;
                    case 1 -> bx + 1;
                    default -> bx + 11;
                };
                int z0 = switch (side) {
                    case 0 -> bz + 1;
                    case 2 -> bz + 11;
                    default -> bz + t;
                };
                int wx = side == 1 ? -1 : side == 3 ? 1 : 0, wz = side == 0 ? -1 : side == 2 ? 1 : 0;
                boolean ok = true;
                for (int s = 0; s < 3 && ok; s++) {
                    int x = x0 + along[0] * s, z = z0 + along[1] * s;
                    ok = floor(g, x, z) && !g.lamp[x][z] && !run.blocked.contains(x * 4096 + z) && in(x + wx, z + wz) && g.tile[x + wx][z + wz] == Grid.WALL
                        && run.lvl.getBlockState(run.at(x, F + 1, z)).isAir();
                }
                if (!ok) continue;
                Direction outward = wx < 0 ? Direction.EAST : wx > 0 ? Direction.WEST : wz < 0 ? Direction.SOUTH : Direction.NORTH;
                for (int s : new int[]{0, 2}) {
                    int x = x0 + along[0] * s, z = z0 + along[1] * s;
                    for (int h = 1; h <= 3; h++) run.lvl.setBlock(run.at(x, F + h, z), Blocks.BOOKSHELF.defaultBlockState(), 2 | 16);
                    put(run, x, F + 4, z, Blocks.SPRUCE_STAIRS.defaultBlockState().setValue(StairBlock.FACING, outward).setValue(StairBlock.HALF, Half.TOP));
                    run.blocked.add(x * 4096 + z);
                }
                run.blocked.add((x0 + along[0]) * 4096 + z0 + along[1]);
                made++;
            }
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
        int spx = (int) Math.floor(g.spawn[0]), spz = (int) Math.floor(g.spawn[1]), bx0 = (int) Math.floor(g.bell[0]), bz0 = (int) Math.floor(g.bell[1]);
        for (int dx = -1; dx <= 1; dx++) for (int dz = -1; dz <= 1; dz++) {
            run.blocked.add((spx + dx) * 4096 + spz + dz);
            run.blocked.add((bx0 + dx) * 4096 + bz0 + dz);
        }
        corners(run, new Random(g.seed ^ 0xC0FEL));
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
        shelves(run, r);
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
