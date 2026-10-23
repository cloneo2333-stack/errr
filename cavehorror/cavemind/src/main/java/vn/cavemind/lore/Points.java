package vn.cavemind.lore;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.LongTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BiomeTags;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.BellBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CandleBlock;
import net.minecraft.world.level.block.LadderBlock;
import net.minecraft.world.level.block.LecternBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.entity.LecternBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BellAttachType;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.phys.Vec3;
import vn.cavemind.CaveMind;

public final class Points {
    public static final String[] CHILD = {"", "", "Linh", "Tuấn", "Mai", "Bảo", "Nam"};

    public static final class Point {
        public String id;
        public int gate;
        public BlockPos target, pos, post, lectern, home;
        public int face = -1, tries;
        public boolean built;
        public UUID owner;
        public final List<BlockPos> candles = new ArrayList<>();

        public boolean waypoint() {
            return id.startsWith("w");
        }

        public boolean altar() {
            return id.startsWith("a");
        }

        public boolean gatePoint() {
            return id.startsWith("g");
        }

        CompoundTag save() {
            CompoundTag c = new CompoundTag();
            c.putString("id", id);
            c.putInt("gate", gate);
            c.putLong("target", target.asLong());
            if (pos != null) c.putLong("pos", pos.asLong());
            if (post != null) c.putLong("post", post.asLong());
            if (lectern != null) c.putLong("lectern", lectern.asLong());
            if (home != null) c.putLong("home", home.asLong());
            c.putInt("face", face);
            c.putInt("tries", tries);
            c.putBoolean("built", built);
            if (owner != null) c.putUUID("owner", owner);
            ListTag l = new ListTag();
            for (BlockPos q : candles) l.add(LongTag.valueOf(q.asLong()));
            c.put("candles", l);
            return c;
        }

        static Point load(CompoundTag c) {
            Point p = new Point();
            p.id = c.getString("id");
            p.gate = c.getInt("gate");
            p.target = BlockPos.of(c.getLong("target"));
            if (c.contains("pos")) p.pos = BlockPos.of(c.getLong("pos"));
            if (c.contains("post")) p.post = BlockPos.of(c.getLong("post"));
            if (c.contains("lectern")) p.lectern = BlockPos.of(c.getLong("lectern"));
            if (c.contains("home")) p.home = BlockPos.of(c.getLong("home"));
            p.face = c.getInt("face");
            p.tries = c.getInt("tries");
            p.built = c.getBoolean("built");
            if (c.hasUUID("owner")) p.owner = c.getUUID("owner");
            ListTag l = c.getList("candles", Tag.TAG_LONG);
            for (int i = 0; i < l.size(); i++) p.candles.add(BlockPos.of(((LongTag) l.get(i)).getAsLong()));
            return p;
        }
    }

    static final class Data extends SavedData {
        final Map<String, Point> points = new LinkedHashMap<>();

        static Data get(ServerLevel any) {
            return any.getServer().overworld().getDataStorage().computeIfAbsent(Data::load, Data::new, "cavemind_chuong");
        }

        static Data load(CompoundTag t) {
            Data d = new Data();
            ListTag l = t.getList("points", Tag.TAG_COMPOUND);
            for (int i = 0; i < l.size(); i++) {
                Point p = Point.load(l.getCompound(i));
                d.points.put(p.id, p);
            }
            return d;
        }

        @Override
        public CompoundTag save(CompoundTag t) {
            ListTag l = new ListTag();
            for (Point p : points.values()) l.add(p.save());
            t.put("points", l);
            return t;
        }
    }

    public static Point get(ServerLevel lvl, String id) {
        return Data.get(lvl).points.get(id);
    }

    public static Collection<Point> all(ServerLevel lvl) {
        return Data.get(lvl).points.values();
    }

    public static void dirty(ServerLevel lvl) {
        Data.get(lvl).setDirty();
    }

    public static Point assign(ServerLevel lvl, int gate, BlockPos home) {
        Data d = Data.get(lvl);
        Point g = d.points.get("g" + gate);
        if (g != null) return g;
        double[] range = switch (gate) {
            case 2 -> new double[]{300, 420};
            case 4 -> new double[]{520, 680};
            default -> new double[]{760, 940};
        };
        List<Double> avoid = new ArrayList<>();
        for (Point o : d.points.values()) {
            if (!o.gatePoint() || o.home == null) continue;
            avoid.add(Math.atan2(o.target.getZ() - o.home.getZ(), o.target.getX() - o.home.getX()));
        }
        RandomSource r = RandomSource.create(lvl.getSeed() ^ (gate * 0x9E3779B97F4A7C15L) ^ home.asLong());
        BlockPos t = choose(lvl, home, range[0], range[1], avoid, r, 9);
        g = new Point();
        g.id = "g" + gate;
        g.gate = gate;
        g.target = t;
        g.home = home;
        d.points.put(g.id, g);
        double f = 0.42 + r.nextDouble() * 0.12;
        BlockPos mid = new BlockPos(Mth.floor(home.getX() + (t.getX() - home.getX()) * f), t.getY(), Mth.floor(home.getZ() + (t.getZ() - home.getZ()) * f));
        Point w = new Point();
        w.id = "w" + gate;
        w.gate = gate;
        w.target = choose(lvl, mid, 0, 40, List.of(), r, 4);
        w.home = home;
        d.points.put(w.id, w);
        d.setDirty();
        CaveMind.debug("chapter {} meeting point at {}, waypoint at {}", gate, t, w.target);
        return g;
    }

    public static Point altar(ServerLevel lvl, int gate, UUID owner, BlockPos post) {
        Data d = Data.get(lvl);
        String id = "a" + gate + ":" + owner;
        Point a = d.points.get(id);
        if (a == null) {
            a = new Point();
            a.id = id;
            a.gate = gate;
            a.owner = owner;
            d.points.put(id, a);
        }
        a.target = a.pos = a.post = post.immutable();
        a.built = true;
        d.setDirty();
        return a;
    }

    static BlockPos choose(ServerLevel lvl, BlockPos from, double dmin, double dmax, List<Double> avoid, RandomSource r, int rad) {
        ChunkGenerator gen = lvl.getChunkSource().getGenerator();
        RandomState rs = lvl.getChunkSource().randomState();
        BlockPos best = null;
        int bestSpread = Integer.MAX_VALUE;
        for (int t = 0; t < 36; t++) {
            double a = r.nextDouble() * Math.PI * 2;
            boolean near = false;
            for (double o : avoid) if (Math.abs(Mth.wrapDegrees(Math.toDegrees(a - o))) < 70) near = true;
            if (near && t < 24) continue;
            double dist = dmin + r.nextDouble() * (dmax - dmin);
            int x = Mth.floor(from.getX() + Math.cos(a) * dist), z = Mth.floor(from.getZ() + Math.sin(a) * dist);
            int lo = Integer.MAX_VALUE, hi = Integer.MIN_VALUE;
            boolean wet = false;
            for (int[] o : new int[][]{{0, 0}, {-rad, -rad}, {rad, -rad}, {-rad, rad}, {rad, rad}}) {
                int sx = x + o[0], sz = z + o[1];
                int top = gen.getBaseHeight(sx, sz, Heightmap.Types.WORLD_SURFACE_WG, lvl, rs);
                int floor = gen.getBaseHeight(sx, sz, Heightmap.Types.OCEAN_FLOOR_WG, lvl, rs);
                if (top > floor) wet = true;
                lo = Math.min(lo, floor);
                hi = Math.max(hi, floor);
            }
            if (wet || lo < lvl.getSeaLevel() + 1 || hi > 150) continue;
            net.minecraft.core.Holder<Biome> b = lvl.getUncachedNoiseBiome(x >> 2, lo >> 2, z >> 2);
            if (b.is(BiomeTags.IS_OCEAN) || b.is(BiomeTags.IS_RIVER) || b.is(BiomeTags.IS_DEEP_OCEAN)) continue;
            int spread = hi - lo;
            if (spread < bestSpread) {
                bestSpread = spread;
                best = new BlockPos(x, lo, z);
            }
            if (spread <= 3) break;
        }
        if (best != null) return best;
        double a = r.nextDouble() * Math.PI * 2, dist = (dmin + dmax) / 2;
        int x = Mth.floor(from.getX() + Math.cos(a) * dist), z = Mth.floor(from.getZ() + Math.sin(a) * dist);
        return new BlockPos(x, Math.max(lvl.getSeaLevel() + 1, gen.getBaseHeight(x, z, Heightmap.Types.OCEAN_FLOOR_WG, lvl, rs)), z);
    }

    private static final Map<String, Long> NEXT = new java.util.HashMap<>();

    public static void clear() {
        NEXT.clear();
        ANGEL.clear();
    }

    public static void tick(ServerLevel lvl, ServerPlayer p) {
        Data d = Data.get(lvl);
        long now = lvl.getGameTime();
        for (Point pt : d.points.values()) {
            if (pt.built || pt.altar()) continue;
            double dx = p.getX() - pt.target.getX(), dz = p.getZ() - pt.target.getZ();
            if (dx * dx + dz * dz > 88 * 88) continue;
            if (now < NEXT.getOrDefault(pt.id, 0L)) continue;
            NEXT.put(pt.id, now + 100);
            if (build(lvl, d, pt, p)) return;
        }
    }

    static boolean crowded(ServerLevel lvl, int x, int z, double r) {
        for (ServerPlayer q : lvl.players()) {
            double dx = q.getX() - x, dz = q.getZ() - z;
            if (dx * dx + dz * dz < r * r) return true;
        }
        return false;
    }

    static boolean build(ServerLevel lvl, Data d, Point pt, ServerPlayer p) {
        int rad = pt.waypoint() ? 4 : 12;
        int reach = pt.waypoint() ? 16 : 28;
        BlockPos o = null;
        int bestScore = Integer.MAX_VALUE;
        for (int ox = -reach; ox <= reach; ox += 4) {
            for (int oz = -reach; oz <= reach; oz += 4) {
                int x = pt.target.getX() + ox, z = pt.target.getZ() + oz;
                if (crowded(lvl, x, z, rad + 5)) continue;
                int s = flatness(lvl, x, z, rad);
                if (s < 0) continue;
                int score = s * 6 + Math.abs(ox) + Math.abs(oz);
                if (score < bestScore) {
                    bestScore = score;
                    o = new BlockPos(x, height(lvl, x, z, rad), z);
                }
            }
        }
        if (o == null) {
            if (!loaded(lvl, pt.target.getX(), pt.target.getZ(), rad + reach) || crowded(lvl, pt.target.getX(), pt.target.getZ(), rad + 5)) return false;
            if (++pt.tries < 6) {
                d.setDirty();
                return false;
            }
            o = new BlockPos(pt.target.getX(), Math.max(lvl.getSeaLevel(), lvl.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, pt.target.getX(), pt.target.getZ()) - 1), pt.target.getZ());
        }
        if (bestScore != Integer.MAX_VALUE && bestScore > 40 && ++pt.tries < 4) {
            d.setDirty();
            return false;
        }
        if (crowded(lvl, o.getX(), o.getZ(), pt.waypoint() ? 16 : 26) && pt.tries++ < 8) {
            d.setDirty();
            return false;
        }
        Direction f = Direction.fromYRot(Math.toDegrees(Math.atan2(-(p.getX() - o.getX()), p.getZ() - o.getZ())));
        Lore.B b = new Lore.B(lvl, o, f);
        pt.candles.clear();
        if (pt.waypoint()) waypoint(b, pt, d);
        else switch (pt.gate) {
            case 2 -> farm(b, pt);
            case 4 -> tower(b, pt);
            default -> school(b, pt);
        }
        pt.pos = o;
        pt.face = f.get2DDataValue();
        pt.built = true;
        d.setDirty();
        CaveMind.debug("chapter point {} built at {}", pt.id, o);
        return true;
    }

    public static boolean buildNow(ServerLevel lvl, String id, BlockPos o, Direction f) {
        Data d = Data.get(lvl);
        Point pt = d.points.get(id);
        if (pt == null) {
            pt = new Point();
            pt.id = id;
            pt.gate = id.length() > 1 ? Mth.clamp(id.charAt(1) - '0', 2, 6) : 2;
            pt.target = o;
            pt.home = o;
            d.points.put(id, pt);
        }
        pt.target = o;
        Lore.B b = new Lore.B(lvl, o, f);
        pt.candles.clear();
        if (pt.waypoint()) waypoint(b, pt, d);
        else switch (pt.gate) {
            case 2 -> farm(b, pt);
            case 4 -> tower(b, pt);
            default -> school(b, pt);
        }
        pt.pos = o;
        pt.face = f.get2DDataValue();
        pt.built = true;
        d.setDirty();
        return true;
    }

    static boolean loaded(ServerLevel lvl, int x, int z, int rad) {
        return lvl.hasChunkAt(new BlockPos(x - rad, 0, z - rad)) && lvl.hasChunkAt(new BlockPos(x + rad, 0, z + rad))
            && lvl.hasChunkAt(new BlockPos(x - rad, 0, z + rad)) && lvl.hasChunkAt(new BlockPos(x + rad, 0, z - rad));
    }

    static int height(ServerLevel lvl, int x, int z, int rad) {
        List<Integer> hs = new ArrayList<>();
        for (int dx = -rad; dx <= rad; dx += 3) {
            for (int dz = -rad; dz <= rad; dz += 3) hs.add(lvl.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x + dx, z + dz) - 1);
        }
        hs.sort(null);
        return hs.get(hs.size() / 2);
    }

    static int flatness(ServerLevel lvl, int x, int z, int rad) {
        if (!loaded(lvl, x, z, rad)) return -1;
        int lo = Integer.MAX_VALUE, hi = Integer.MIN_VALUE;
        for (int dx = -rad; dx <= rad; dx += 3) {
            for (int dz = -rad; dz <= rad; dz += 3) {
                int h = lvl.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x + dx, z + dz) - 1;
                BlockPos top = new BlockPos(x + dx, h, z + dz);
                if (!lvl.getFluidState(top).isEmpty() || !lvl.getFluidState(top.above()).isEmpty()) return -1;
                BlockState st = lvl.getBlockState(top);
                if (st.is(BlockTags.LEAVES) || st.is(BlockTags.LOGS) || st.is(Blocks.ICE) || st.is(Blocks.PACKED_ICE)) return -1;
                lo = Math.min(lo, h);
                hi = Math.max(hi, h);
            }
        }
        if (lo < lvl.getSeaLevel() - 1) return -1;
        return hi - lo > 7 ? -1 : hi - lo;
    }

    public static Point postAt(ServerLevel lvl, BlockPos q) {
        for (Point pt : Data.get(lvl).points.values()) {
            if (!pt.built || pt.post == null) continue;
            if (pt.post.getX() == q.getX() && pt.post.getZ() == q.getZ() && q.getY() >= pt.post.getY() - 1 && q.getY() <= pt.post.getY() + 5) return pt;
        }
        return null;
    }

    public static Point lecternAt(ServerLevel lvl, BlockPos q) {
        for (Point pt : Data.get(lvl).points.values()) {
            if (pt.built && q.equals(pt.lectern)) return pt;
        }
        return null;
    }

    public static boolean guarded(ServerLevel lvl, BlockPos q) {
        for (Point pt : Data.get(lvl).points.values()) {
            if (!pt.built || pt.altar()) continue;
            if (pt.post != null && pt.post.getX() == q.getX() && pt.post.getZ() == q.getZ() && q.getY() >= pt.post.getY() && q.getY() <= pt.post.getY() + 4) return true;
            if (pt.candles.contains(q) || q.equals(pt.lectern)) return true;
        }
        return false;
    }

    public static int lit(ServerLevel lvl, Point pt) {
        int n = 0;
        for (BlockPos q : pt.candles) {
            BlockState st = lvl.getBlockState(q);
            if (st.getBlock() instanceof CandleBlock && st.getValue(CandleBlock.LIT)) n++;
        }
        return n;
    }

    public static void repair(ServerLevel lvl) {
        Data d = Data.get(lvl);
        for (Point pt : d.points.values()) {
            if (!pt.built || pt.altar() || pt.pos == null || !lvl.isLoaded(pt.pos)) continue;
            boolean seen = false;
            for (ServerPlayer q : lvl.players()) if (q.position().distanceTo(Vec3.atCenterOf(pt.pos)) < 40) seen = true;
            if (seen) continue;
            for (BlockPos q : pt.candles) {
                BlockState st = lvl.getBlockState(q);
                if (st.getBlock() instanceof CandleBlock) {
                    if (st.getValue(CandleBlock.LIT)) lvl.setBlock(q, st.setValue(CandleBlock.LIT, false), 3);
                } else if (st.canBeReplaced() && lvl.getFluidState(q).isEmpty()) {
                    lvl.setBlock(q, Blocks.CANDLE.defaultBlockState().setValue(CandleBlock.CANDLES, 1 + (q.hashCode() & 1)), 3);
                }
            }
            if (pt.lectern != null && lvl.getBlockState(pt.lectern).getBlock() instanceof LecternBlock && lvl.getBlockEntity(pt.lectern) instanceof LecternBlockEntity le && !le.hasBook()) {
                refill(lvl, pt.lectern, le, pt.gate);
            }
        }
    }

    public static void refill(ServerLevel lvl, BlockPos pos, LecternBlockEntity le, int gate) {
        le.setBook(journal(gate));
        BlockState st = lvl.getBlockState(pos);
        if (st.getBlock() instanceof LecternBlock && !st.getValue(LecternBlock.HAS_BOOK)) LecternBlock.resetBookState(null, lvl, pos, st, true);
        le.setChanged();
    }

    public static ItemStack compass(ServerLevel lvl, Point pt) {
        return compass(lvl, pt, pt.target);
    }

    public static ItemStack compass(ServerLevel lvl, Point pt, BlockPos to) {
        ItemStack c = new ItemStack(Items.COMPASS);
        CompoundTag t = c.getOrCreateTag();
        t.put("LodestonePos", net.minecraft.nbt.NbtUtils.writeBlockPos(to));
        net.minecraft.resources.ResourceLocation dim = lvl.dimension().location();
        t.putString("LodestoneDimension", dim.toString());
        t.putBoolean("LodestoneTracked", false);
        t.putBoolean("cavemind", true);
        t.putInt("cavemind_gate", pt.gate);
        c.setHoverName(Component.literal("La bàn sương: điểm hẹn chương " + pt.gate).withStyle(s -> s.withItalic(false).withColor(0xb9a7e0)));
        CompoundTag disp = c.getOrCreateTagElement("display");
        ListTag lore = new ListTag();
        for (String line : new String[]{"Kim chỉ về nơi " + CHILD[pt.gate] + " giấu món đồ chơi.", "Đi theo kim, đọc biển ở cây đếm giữa đường."}) {
            lore.add(net.minecraft.nbt.StringTag.valueOf(Component.Serializer.toJson(Component.literal(line).withStyle(s -> s.withItalic(true).withColor(0x8a8a8a)))));
        }
        disp.put("Lore", lore);
        return c;
    }

    public static String heading(BlockPos from, BlockPos to) {
        double a = Math.toDegrees(Math.atan2(to.getX() - from.getX(), -(to.getZ() - from.getZ())));
        String[] w = {"Bắc", "Đông Bắc", "Đông", "Đông Nam", "Nam", "Tây Nam", "Tây", "Tây Bắc"};
        int i = Math.floorMod((int) Math.round(a / 45.0), 8);
        return w[i];
    }

    static void candle(Lore.B b, Point pt, int x, int y, int z, int n) {
        b.set(x, y, z, Blocks.CANDLE.defaultBlockState().setValue(CandleBlock.CANDLES, n).setValue(CandleBlock.LIT, false));
        pt.candles.add(b.at(x, y, z).immutable());
    }

    static void lectern(Lore.B b, Point pt, int x, int y, int z, Direction facing) {
        b.set(x, y, z, Blocks.LECTERN.defaultBlockState().setValue(LecternBlock.FACING, facing).setValue(LecternBlock.HAS_BOOK, true));
        BlockPos q = b.at(x, y, z);
        if (b.lvl.getBlockEntity(q) instanceof LecternBlockEntity le) le.setBook(journal(pt.gate));
        pt.lectern = q.immutable();
    }

    static void ladder(Lore.B b, int x, int y0, int y1, int z, Direction facing) {
        for (int y = y0; y <= y1; y++) b.set(x, y, z, Blocks.LADDER.defaultBlockState().setValue(LadderBlock.FACING, facing));
    }

    static void box(Lore.B b, int x0, int y0, int z0, int x1, int y1, int z1, BlockState s) {
        for (int x = x0; x <= x1; x++) for (int y = y0; y <= y1; y++) for (int z = z0; z <= z1; z++) b.set(x, y, z, s);
    }

    static void deadTree(Lore.B b, Point pt, int x, int z, int h, Block log) {
        BlockState l = log.defaultBlockState();
        for (int y = 1; y <= h; y++) b.set(x, y, z, l);
        BlockState lx = l.setValue(RotatedPillarBlock.AXIS, Direction.Axis.X), lz = l.setValue(RotatedPillarBlock.AXIS, Direction.Axis.Z);
        b.set(x + 1, h - 2, z, lx);
        b.set(x + 2, h - 1, z, lx);
        b.set(x + 2, h, z, Blocks.OAK_FENCE.defaultBlockState());
        b.set(x - 1, h - 1, z, lx);
        b.set(x, h, z + 1, lz);
        b.set(x, h + 1, z + 1, Blocks.OAK_FENCE.defaultBlockState());
        b.wallSign(x, 2, z - 1, Blocks.OAK_WALL_SIGN, Direction.NORTH, Component.literal("CÂY ĐẾM"), Component.literal("úp mặt vào đây"),
            Component.literal("đếm tới 48"), Component.literal("rồi đi tìm"));
        pt.post = b.at(x, 1, z).immutable();
    }

    static Component[] lines(String... s) {
        Component[] c = new Component[4];
        for (int i = 0; i < 4; i++) c[i] = i < s.length ? Component.literal(s[i]) : Component.empty();
        return c;
    }

    private static void waypoint(Lore.B b, Point pt, Data d) {
        b.flatten(-3, 3, -3, 3, Blocks.GRASS_BLOCK.defaultBlockState());
        deadTree(b, pt, 0, 0, 4, Blocks.STRIPPED_OAK_LOG);
        candle(b, pt, 1, 1, -1, 2);
        candle(b, pt, -1, 1, 1, 1);
        b.set(2, 1, 1, Blocks.SPRUCE_FENCE.defaultBlockState());
        b.set(2, 2, 1, Blocks.SOUL_LANTERN.defaultBlockState());
        Point g = d.points.get("g" + pt.gate);
        BlockPos at = b.at(0, 0, 0);
        String dir = g != null ? heading(at, g.target) : "?";
        int dist = g != null ? (int) Math.sqrt(at.distSqr(g.target)) : 0;
        b.standSign(-1, -2, Blocks.OAK_SIGN, 180f, lines("ĐIỂM HẸN", "hướng " + dir, "còn " + dist + " khối", "đồ chơi của " + CHILD[pt.gate]));
        b.standSign(1, -3, Blocks.OAK_SIGN, 180f, lines("trời tối thì", "cầm một cây nến", "chạm vào cây đếm", "để rủ nó chơi"));
    }

    private static void farm(Lore.B b, Point pt) {
        BlockState grass = Blocks.GRASS_BLOCK.defaultBlockState(), plank = Blocks.SPRUCE_PLANKS.defaultBlockState(), log = Blocks.STRIPPED_SPRUCE_LOG.defaultBlockState();
        BlockState cob = Blocks.COBBLESTONE.defaultBlockState(), air = Blocks.AIR.defaultBlockState(), mossy = Blocks.MOSSY_COBBLESTONE.defaultBlockState();
        b.flatten(-10, 10, -8, 10, grass);
        b.path(0, -8, -5, -2);
        b.path(0, -8, 3, -6);
        b.path(-5, -2, 5, 2);
        box(b, -8, -5, -1, -2, -1, 5, cob);
        box(b, -7, -4, 0, -3, -2, 3, air);
        for (int x = -8; x <= -2; x++) {
            for (int z = -1; z <= 5; z++) {
                b.set(x, 0, z, plank);
                boolean wall = x == -8 || x == -2 || z == -1 || z == 5;
                boolean corner = (x == -8 || x == -2) && (z == -1 || z == 5);
                for (int y = 1; y <= 3; y++) b.set(x, y, z, wall ? (corner ? log : plank) : air);
            }
        }
        BlockState slab = Blocks.SPRUCE_SLAB.defaultBlockState().setValue(SlabBlock.TYPE, SlabType.BOTTOM);
        for (int x = -9; x <= -1; x++) for (int z = -2; z <= 6; z++) b.set(x, 4, z, slab);
        for (int x = -7; x <= -3; x++) for (int z = -2; z <= 6; z++) b.set(x, 5, z, slab);
        for (int z = -2; z <= 6; z++) b.set(-5, 6, z, slab);
        b.door(-5, -1, Blocks.SPRUCE_DOOR, Direction.SOUTH, true);
        b.set(-8, 2, 2, Blocks.GLASS_PANE.defaultBlockState());
        b.set(-2, 2, 2, Blocks.GLASS_PANE.defaultBlockState());
        candle(b, pt, -7, 1, 0, 2);
        lectern(b, pt, -7, 1, 2, Direction.EAST);
        b.set(-7, 1, 3, Blocks.BARREL.defaultBlockState());
        b.fill(-7, 1, 3, new ItemStack(Items.FLINT_AND_STEEL), new ItemStack(Items.BREAD, 3), new ItemStack(Items.CANDLE, 2));
        for (int[] q : new int[][]{{-6, 1, 4}, {-7, 1, 4}, {-7, 2, 4}, {-5, 1, 4}}) b.set(q[0], q[1], q[2], Blocks.HAY_BLOCK.defaultBlockState());
        b.set(-3, 0, 4, Blocks.SPRUCE_TRAPDOOR.defaultBlockState().setValue(TrapDoorBlock.HALF, Half.TOP).setValue(TrapDoorBlock.OPEN, false));
        ladder(b, -3, -4, -1, 4, Direction.NORTH);
        b.set(-7, -4, 3, Blocks.COBWEB.defaultBlockState());
        b.set(-3, -2, 0, Blocks.COBWEB.defaultBlockState());
        b.set(-5, -4, 3, Blocks.WHITE_CARPET.defaultBlockState());
        b.set(-6, -4, 3, Blocks.WHITE_CARPET.defaultBlockState());
        b.set(-7, -4, 0, Blocks.BARREL.defaultBlockState());
        b.fill(-7, -4, 0, Playground.named(Items.PAPER, "Bức vẽ của Linh", "Một cái cây khô, một bạn nhỏ úp mặt vào cây.", "Phía sau có bảy cái bóng."));
        candle(b, pt, -7, -4, 1, 1);
        for (int y = 1; y >= -7; y--) {
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    if (dx == 0 && dz == 0) b.set(2, y, -4, air);
                    else if (y >= 0) b.set(2 + dx, y, -4 + dz, mossy);
                    else b.set(2 + dx, y, -4 + dz, cob);
                }
            }
        }
        b.set(2, -7, -4, cob);
        b.set(2, -8, -4, cob);
        ladder(b, 2, -6, 1, -4, Direction.NORTH);
        b.set(1, -6, -4, air);
        b.set(1, -7, -4, cob);
        b.set(0, -6, -4, cob);
        b.set(1, -6, -5, cob);
        b.set(1, -6, -3, cob);
        b.set(1, -5, -4, cob);
        candle(b, pt, 1, -6, -4, 3);
        b.set(1, 2, -4, Blocks.SPRUCE_FENCE.defaultBlockState());
        b.set(3, 2, -4, Blocks.SPRUCE_FENCE.defaultBlockState());
        for (int x = 1; x <= 3; x += 2) b.set(x, 3, -4, Blocks.SPRUCE_SLAB.defaultBlockState().setValue(SlabBlock.TYPE, SlabType.TOP));
        b.wallSign(2, 1, -6, Blocks.SPRUCE_WALL_SIGN, Direction.NORTH, lines("GIẾNG CẠN", "đừng thả gì", "xuống đây", ""));
        deadTree(b, pt, 5, 3, 6, Blocks.OAK_LOG);
        b.set(8, 1, 8, Blocks.OAK_LOG.defaultBlockState());
        candle(b, pt, 8, 2, 8, 2);
        for (int[] q : new int[][]{{8, 1, -3}, {8, 2, -3}, {7, 1, -3}, {-1, 1, 7}, {0, 1, 7}, {-1, 1, 8}, {9, 1, 4}}) b.set(q[0], q[1], q[2], Blocks.HAY_BLOCK.defaultBlockState());
        for (int[] q : new int[][]{{-9, 1, -3}, {-9, 1, -4}, {6, 1, -6}}) b.set(q[0], q[1], q[2], Blocks.BARREL.defaultBlockState());
        BlockState fence = Blocks.SPRUCE_FENCE.defaultBlockState();
        for (int x = -10; x <= 10; x++) if (Math.floorMod(x * 5 + 3, 7) != 0) b.set(x, 1, 10, fence);
        for (int z = -6; z <= 9; z++) if (Math.floorMod(z * 3 + 1, 6) != 0) b.set(10, 1, z, fence);
        b.standSign(0, -7, Blocks.SPRUCE_SIGN, 180f, lines("NHÀ CỦA LINH", "ai trốn kỹ", "người ấy thắng", ""));
    }

    private static void tower(Lore.B b, Point pt) {
        BlockState bricks = Blocks.STONE_BRICKS.defaultBlockState(), cracked = Blocks.CRACKED_STONE_BRICKS.defaultBlockState(), mossy = Blocks.MOSSY_STONE_BRICKS.defaultBlockState();
        BlockState plank = Blocks.SPRUCE_PLANKS.defaultBlockState(), air = Blocks.AIR.defaultBlockState(), cob = Blocks.COBBLESTONE.defaultBlockState();
        b.flatten(-9, 9, -8, 9, Blocks.GRASS_BLOCK.defaultBlockState());
        for (int[] q : new int[][]{{-4, -3}, {2, -2}, {4, 7}, {-6, 6}, {6, 0}, {-2, 8}, {0, -6}}) b.set(q[0], 0, q[1], Blocks.GRAVEL.defaultBlockState());
        b.path(0, -8, -1, 0);
        for (int x = -3; x <= 1; x++) {
            for (int z = 1; z <= 5; z++) {
                b.set(x, 0, z, bricks);
                boolean wall = x == -3 || x == 1 || z == 1 || z == 5;
                for (int y = 1; y <= 13; y++) {
                    int k = Math.floorMod(x * 7 + y * 3 + z * 5, 9);
                    b.set(x, y, z, wall ? (k == 0 ? cracked : k == 4 ? mossy : bricks) : air);
                }
            }
        }
        for (int x = -2; x <= 0; x++) {
            for (int z = 2; z <= 4; z++) {
                b.set(x, 5, z, plank);
                b.set(x, 10, z, plank);
            }
        }
        ladder(b, 0, 1, 11, 4, Direction.NORTH);
        b.door(-1, 1, Blocks.DARK_OAK_DOOR, Direction.SOUTH, true);
        for (int[] q : new int[][]{{-3, 7, 3}, {1, 7, 3}, {-3, 12, 3}, {-1, 7, 5}}) b.set(q[0], q[1], q[2], Blocks.IRON_BARS.defaultBlockState());
        for (int[] q : new int[][]{{1, 13, 2}, {1, 13, 3}, {1, 13, 4}, {1, 13, 5}, {0, 13, 5}, {1, 12, 5}, {1, 12, 4}, {-1, 13, 5}}) b.set(q[0], q[1], q[2], air);
        for (int x = -3; x <= -1; x++) for (int z = 1; z <= 3; z++) b.set(x, 14, z, Blocks.STONE_BRICK_SLAB.defaultBlockState().setValue(SlabBlock.TYPE, SlabType.BOTTOM));
        for (int x = -2; x <= 0; x++) for (int y = 11; y <= 13; y++) b.set(x, y, 1, Blocks.SMOOTH_QUARTZ.defaultBlockState());
        b.set(-1, 12, 1, Blocks.POLISHED_BLACKSTONE.defaultBlockState());
        candle(b, pt, 0, 1, 2, 2);
        lectern(b, pt, -2, 1, 2, Direction.EAST);
        b.set(-2, 1, 4, Blocks.BARREL.defaultBlockState());
        b.fill(-2, 1, 4, new ItemStack(Items.FLINT_AND_STEEL), new ItemStack(Items.CANDLE, 2), new ItemStack(Items.CLOCK));
        candle(b, pt, -2, 6, 4, 1);
        b.set(-2, 6, 2, Blocks.COBWEB.defaultBlockState());
        candle(b, pt, -2, 11, 4, 3);
        b.set(-2, 11, 2, Blocks.BELL.defaultBlockState().setValue(BellBlock.FACING, Direction.EAST).setValue(BellBlock.ATTACHMENT, BellAttachType.FLOOR));
        BlockState slab = Blocks.STONE_BRICK_SLAB.defaultBlockState().setValue(SlabBlock.TYPE, SlabType.BOTTOM);
        BlockState stair = Blocks.STONE_BRICK_STAIRS.defaultBlockState().setValue(StairBlock.FACING, Direction.WEST);
        for (int[] q : new int[][]{{3, 1, 2}, {4, 1, 3}, {3, 1, 4}, {2, 1, 6}, {6, 1, 3}, {5, 1, 1}, {-5, 1, 5}, {-4, 1, 7}}) b.set(q[0], q[1], q[2], slab);
        for (int[] q : new int[][]{{3, 1, 3}, {4, 1, 4}, {3, 2, 3}}) b.set(q[0], q[1], q[2], cracked);
        b.set(5, 1, 4, stair);
        for (int[] q : new int[][]{{4, 1, 6}, {6, 1, 6}, {5, 1, 7}, {4, 2, 6}, {6, 2, 6}, {5, 2, 7}}) b.set(q[0], q[1], q[2], cob);
        b.set(5, 2, 6, Blocks.STONE_BRICK_SLAB.defaultBlockState().setValue(SlabBlock.TYPE, SlabType.TOP));
        candle(b, pt, 5, 1, 6, 2);
        deadTree(b, pt, -6, -2, 6, Blocks.DARK_OAK_LOG);
        candle(b, pt, -7, 1, -1, 1);
        b.set(-4, 1, -5, Blocks.SPRUCE_FENCE.defaultBlockState());
        b.set(-4, 2, -5, Blocks.LANTERN.defaultBlockState());
        b.standSign(1, -6, Blocks.SPRUCE_SIGN, 180f, lines("THÁP ĐỒNG HỒ", "dừng lúc", "11 giờ 48", "đừng chỉnh lại"));
        String nbt = "{Facing:" + b.f.get3DDataValue() + "b,Fixed:1b,Invulnerable:1b,Item:{id:\"minecraft:clock\",Count:1b}}";
        BlockPos fr = b.at(-1, 12, 0);
        Lore.cmd(b.lvl, "summon minecraft:item_frame " + fr.getX() + " " + fr.getY() + " " + fr.getZ() + " " + nbt);
    }

    private static void school(Lore.B b, Point pt) {
        BlockState plank = Blocks.BIRCH_PLANKS.defaultBlockState(), pillar = Blocks.STRIPPED_DARK_OAK_LOG.defaultBlockState(), floor = Blocks.DARK_OAK_PLANKS.defaultBlockState();
        BlockState air = Blocks.AIR.defaultBlockState(), glass = Blocks.GLASS_PANE.defaultBlockState();
        b.flatten(-12, 12, -10, 11, Blocks.GRASS_BLOCK.defaultBlockState());
        b.path(0, -10, 0, 0);
        b.path(0, -4, 6, -4);
        for (int x = -9; x <= 9; x++) {
            for (int z = 1; z <= 9; z++) {
                b.set(x, 0, z, floor);
                boolean wall = x == -9 || x == 9 || z == 1 || z == 9;
                boolean post = (x == -9 || x == 9) && (z == 1 || z == 5 || z == 9) || (z == 1 || z == 9) && Math.floorMod(x, 6) == 3;
                for (int y = 1; y <= 4; y++) b.set(x, y, z, wall ? (post ? pillar : plank) : air);
            }
        }
        BlockState slab = Blocks.SPRUCE_SLAB.defaultBlockState().setValue(SlabBlock.TYPE, SlabType.BOTTOM);
        for (int x = -10; x <= 10; x++) for (int z = 0; z <= 10; z++) b.set(x, 5, z, slab);
        for (int x = -8; x <= 8; x++) for (int z = 2; z <= 8; z++) b.set(x, 6, z, slab);
        for (int x = -6; x <= 6; x++) for (int z = 4; z <= 6; z++) b.set(x, 7, z, slab);
        for (int x : new int[]{-6, -3, 3, 6}) {
            for (int y = 2; y <= 3; y++) {
                b.set(x, y, 1, glass);
                b.set(x, y, 9, glass);
            }
        }
        b.door(0, 1, Blocks.DARK_OAK_DOOR, Direction.SOUTH, false);
        for (int z = 3; z <= 8; z++) for (int y = 1; y <= 4; y++) b.set(0, y, z, z == 5 || z == 6 ? (y <= 2 ? air : plank) : plank);
        BlockState desk = Blocks.SPRUCE_SLAB.defaultBlockState().setValue(SlabBlock.TYPE, SlabType.TOP);
        BlockState chair = Blocks.BIRCH_STAIRS.defaultBlockState().setValue(StairBlock.FACING, Direction.NORTH);
        for (int side : new int[]{-1, 1}) {
            for (int dz : new int[]{4, 6}) {
                for (int k = 1; k <= 3; k++) {
                    int x = side * (k * 2 + 1);
                    if (Math.abs(x) > 8) continue;
                    b.set(x, 1, dz, desk);
                    b.set(x, 1, dz - 1, chair);
                }
            }
        }
        candle(b, pt, -7, 2, 4, 1);
        candle(b, pt, -3, 2, 6, 2);
        candle(b, pt, 3, 2, 4, 1);
        candle(b, pt, 7, 2, 6, 3);
        for (int x = -7; x <= -2; x++) for (int y = 2; y <= 3; y++) b.set(x, y, 9, Blocks.BLACK_CONCRETE.defaultBlockState());
        for (int x = 2; x <= 7; x++) for (int y = 2; y <= 3; y++) b.set(x, y, 9, Blocks.BLACK_CONCRETE.defaultBlockState());
        b.wallSign(-5, 3, 8, Blocks.DARK_OAK_WALL_SIGN, Direction.NORTH, lines("BÀI HỌC CUỐI", "đếm tới một trăm", "không được", "quay lại"));
        b.wallSign(5, 3, 8, Blocks.DARK_OAK_WALL_SIGN, Direction.NORTH, lines("ĐIỂM DANH", "Linh  Tuấn  Mai", "Bảo  Hà  Nam", "Khoa  ..."));
        b.set(5, 1, 8, floor);
        b.set(6, 1, 8, Blocks.DARK_OAK_SLAB.defaultBlockState().setValue(SlabBlock.TYPE, SlabType.TOP));
        candle(b, pt, 5, 2, 8, 1);
        lectern(b, pt, 3, 1, 2, Direction.WEST);
        b.set(-3, 1, 2, Blocks.BARREL.defaultBlockState());
        b.fill(-3, 1, 2, new ItemStack(Items.FLINT_AND_STEEL), new ItemStack(Items.CANDLE, 3), new ItemStack(Items.PAPER, 4));
        b.set(-8, 1, 8, Blocks.COBWEB.defaultBlockState());
        b.set(8, 4, 2, Blocks.COBWEB.defaultBlockState());
        BlockState fence = Blocks.DARK_OAK_FENCE.defaultBlockState(), log = Blocks.STRIPPED_DARK_OAK_LOG.defaultBlockState();
        for (int[] c : new int[][]{{-7, -6}, {-5, -6}, {-7, -4}, {-5, -4}}) for (int y = 1; y <= 6; y++) b.set(c[0], y, c[1], y <= 3 || y >= 5 ? fence : log);
        for (int y = 1; y <= 3; y++) b.set(-6, y, -5, log);
        for (int x = -7; x <= -5; x++) for (int z = -6; z <= -4; z++) b.set(x, 4, z, x == -6 && z == -4 ? Blocks.LADDER.defaultBlockState().setValue(LadderBlock.FACING, Direction.SOUTH) : Blocks.DARK_OAK_PLANKS.defaultBlockState());
        ladder(b, -6, 1, 3, -4, Direction.SOUTH);
        for (int x = -7; x <= -5; x++) for (int z = -6; z <= -4; z++) b.set(x, 7, z, Blocks.DARK_OAK_SLAB.defaultBlockState().setValue(SlabBlock.TYPE, SlabType.BOTTOM));
        b.set(-6, 5, -6, Blocks.BELL.defaultBlockState().setValue(BellBlock.FACING, Direction.EAST).setValue(BellBlock.ATTACHMENT, BellAttachType.FLOOR));
        candle(b, pt, -7, 5, -5, 2);
        deadTree(b, pt, 6, -5, 7, Blocks.OAK_LOG);
        for (int x = -12; x <= 12; x++) if (x < -1 || x > 1) b.set(x, 1, -10, Blocks.SPRUCE_FENCE.defaultBlockState());
        b.standSign(2, -9, Blocks.SPRUCE_SIGN, 180f, lines("TRƯỜNG LÀNG", "lớp Một", "giờ ra chơi", "đã hết"));
    }

    private static final Map<String, Long> ANGEL = new java.util.HashMap<>();

    public static void angel(ServerLevel lvl, ServerPlayer p, Point pt) {
        if (pt.candles.size() < 3 || pt.pos == null) return;
        var type = vn.cavemind.compat.Compat.type(new net.minecraft.resources.ResourceLocation("weeping_angels", "weeping_angel"));
        if (type == null) return;
        long now = lvl.getGameTime();
        if (now < ANGEL.getOrDefault(pt.id, 0L)) return;
        BlockPos c = pt.candles.get(2);
        if (!lvl.isLoaded(c) || p.getY() > c.getY() - 2) return;
        ANGEL.put(pt.id, now + 1200);
        if (!lvl.getEntities(type, new net.minecraft.world.phys.AABB(c).inflate(36), e -> true).isEmpty()) return;
        BlockPos spot = null;
        for (BlockPos q : BlockPos.betweenClosed(c.offset(-2, 0, -2), c.offset(2, 0, 2))) {
            if (q.equals(c) || !lvl.getBlockState(q).isAir() || !lvl.getBlockState(q.above()).isAir()) continue;
            if (!lvl.getBlockState(q.below()).isFaceSturdy(lvl, q.below(), Direction.UP)) continue;
            spot = q.immutable();
            break;
        }
        if (spot == null) return;
        var e = type.create(lvl);
        if (e == null) return;
        e.moveTo(spot.getX() + 0.5, spot.getY(), spot.getZ() + 0.5, (float) (Mth.atan2(c.getZ() - spot.getZ(), c.getX() - spot.getX()) * Mth.RAD_TO_DEG) - 90f, 0);
        e.getPersistentData().putBoolean("cavemind_guard", true);
        if (e instanceof net.minecraft.world.entity.Mob m) m.finalizeSpawn(lvl, lvl.getCurrentDifficultyAt(spot), net.minecraft.world.entity.MobSpawnType.STRUCTURE, null, null);
        if (!lvl.addFreshEntity(e)) return;
        ANGEL.put(pt.id, now + 24000);
        p.displayClientMessage(Component.literal("Trên tầng chuông có tiếng đá cọ vào nhau. Lên tới đó thì đừng rời mắt khỏi nó.").withStyle(net.minecraft.ChatFormatting.DARK_GRAY, net.minecraft.ChatFormatting.ITALIC), true);
        vn.cavemind.CaveMind.debug("a weeping angel guards Mai's bell at {}", spot);
    }

    public static ItemStack journal(int gate) {
        ItemStack b = text(gate);
        b.getOrCreateTag().putInt("cavemind_gate", gate);
        return b;
    }

    static ItemStack text(int gate) {
        return switch (gate) {
            case 2 -> Playground.book("Nhật ký của Linh", "Linh",
                "Mẹ dặn không được chơi ở giếng cạn. Nhưng trốn dưới đó thì không ai tìm ra.",
                "Hôm nay có bạn mới đếm ở cây khô. Bạn ấy đếm rất chậm và không bao giờ nhìn trộm.",
                "Mình giấu con quay trong hốc cây đếm. Ai thắp đủ bốn ngọn nến thì con quay sẽ rơi xuống:\n\n• cửa nhà kho\n• dưới hầm\n• đáy giếng\n• gốc cây sau vườn",
                "Muốn rủ bạn ấy chơi thì đợi trời tối, cầm con quay và chạm vào cây đếm. Bạn ấy sẽ úp mặt vào cây.",
                "Luật: không nhìn trộm, không đứng chỗ sáng, không đánh, không bỏ về giữa chừng. Ngồi thụp xuống trong bóng tối và nín thở.",
                "Mình thắng rồi. Bạn ấy cười rất lâu.\n\nTừ hôm đó mình không về nhà nữa.");
            case 4 -> Playground.book("Sổ tay của Mai", "Mai",
                "Đồng hồ trên tháp dừng lúc mười một giờ bốn mươi tám. Từ hôm đó trời tối nhanh hơn.",
                "Mai giữ chiếc đồng hồ đồ chơi. Nó vẫn chạy, nhưng chạy ngược.",
                "Năm ngọn nến:\n\n• chân tháp\n• tầng hai\n• tầng ba, cạnh chuông\n• dưới đống đá đổ\n• sau cây đếm\n\nCạnh chuông có một bức tượng. Nó chỉ đứng yên khi có người nhìn.",
                "Khi năm ngọn nến cùng cháy, chiếc đồng hồ sẽ rơi xuống chỗ cây đếm.",
                "Bạn ấy chỉ ra chơi khi trời tối. Cầm đồng hồ, chạm vào cây đếm. Chuông trên tháp là hết giờ chơi, đừng rung khi chưa muốn thôi.",
                "Mai trốn trên đỉnh tháp. Bạn ấy đếm tới bốn mươi tám rồi leo lên từng bậc.");
            case 6 -> Playground.book("Sổ liên lạc của Nam", "Nam",
                "Lớp chỉ còn một mình Nam. Các bạn đi trốn và không ai về.",
                "Cô giáo bảo đếm tới một trăm. Nam mới đếm tới bốn mươi tám thì trời tối.",
                "Sáu ngọn nến:\n\n• bốn trên bàn học\n• một trên bàn cô giáo\n• một trên tháp chuông\n\nThắp hết thì còi của Nam sẽ rơi ở cây đếm.",
                "Đây là ván cuối. Bạn ấy sẽ tìm kỹ hơn mọi lần. Trốn thật sâu, thật tối, và đừng thở.",
                "Nếu bạn thắng, Nam được về nhà.\n\nNếu bạn thua, bạn sẽ đếm thay Nam.");
            case 3 -> Playground.book("Cách dựng điểm hẹn", "...",
                "Tuấn không có nhà để trốn. Tuấn bảo: ở đâu có cây đếm thì ở đó là sân chơi.",
                "Làm viên bi ve của Tuấn: kính ở bốn cạnh, mảnh đá mắt mèo lửa ở giữa, bốn góc là mẩu kim loại tối.",
                "Dựng điểm hẹn:\n\n• một cây đếm: ba khúc gỗ chồng lên nhau\n• sáu ngọn nến đang cháy quanh gốc\n• một cái chuông gần đó\n• một tấm biển ghi tên Tuấn",
                "Chỉ chơi khi trời tối, hoặc ở nơi không thấy trời. Cầm viên bi, chạm vào cây đếm.");
            default -> Playground.book("Cách dựng điểm hẹn", "...",
                "Bảo thả diều thì dây đứt. Con diều bay xuống tận đáy thế giới.",
                "Làm con diều của Bảo: giấy làm khung, dây ở hai góc, mảnh lăng kính thời gian ở góc trên.",
                "Dựng điểm hẹn thật sâu, dưới độ cao 0:\n\n• một cây đếm: ba khúc gỗ chồng lên nhau\n• tám ngọn nến đang cháy quanh gốc\n• một cái chuông gần đó\n• một tấm biển ghi tên Bảo",
                "Cầm con diều, chạm vào cây đếm. Dưới sâu thì không có ngày, nó luôn chịu chơi.");
        };
    }

    private Points() {}
}
