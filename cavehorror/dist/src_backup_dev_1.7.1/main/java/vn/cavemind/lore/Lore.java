package vn.cavemind.lore;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.BellBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.CandleBlock;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.LecternBlock;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.SkullBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StandingSignBlock;
import net.minecraft.world.level.block.WallSignBlock;
import net.minecraft.world.level.block.entity.LecternBlockEntity;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.entity.SignText;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.block.state.properties.BellAttachType;
import net.minecraft.world.level.block.state.properties.DoorHingeSide;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.RotationSegment;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.phys.Vec3;
import vn.cavemind.CaveMind;
import vn.cavemind.dev.Dev;
import vn.cavemind.director.Dread;
import vn.cavemind.memory.Moment;
import vn.cavemind.memory.Profile;
import vn.cavemind.sense.Watch;
import vn.cavemind.world.Fx;

public final class Lore {
    static final String[] ORDER = {"s1", "g", "s2", "h", "s3"};
    static final int[] DAY = {1, 2, 3, 4, 6};

    static final class Site {
        BlockPos pos;
        long day;
        boolean found;
    }

    static final class Data extends SavedData {
        final Map<String, Site> sites = new LinkedHashMap<>();

        static Data get(ServerLevel any) {
            return any.getServer().overworld().getDataStorage().computeIfAbsent(Data::load, Data::new, "cavemind_lore");
        }

        static Data load(CompoundTag t) {
            Data d = new Data();
            for (String k : t.getAllKeys()) {
                CompoundTag c = t.getCompound(k);
                Site s = new Site();
                s.pos = BlockPos.of(c.getLong("pos"));
                s.day = c.getLong("day");
                s.found = c.getBoolean("found");
                d.sites.put(k, s);
            }
            return d;
        }

        @Override
        public CompoundTag save(CompoundTag t) {
            sites.forEach((k, s) -> {
                CompoundTag c = new CompoundTag();
                c.putLong("pos", s.pos.asLong());
                c.putLong("day", s.day);
                c.putBoolean("found", s.found);
                t.put(k, c);
            });
            return t;
        }
    }

    public static void tick(ServerPlayer p, Watch w, Profile pr, long now) {
        if (p.level().dimension() != Level.OVERWORLD || p.isSpectator()) return;
        ServerLevel lvl = p.serverLevel();
        if (now % 20 == (p.getId() & 15)) discover(p, pr, lvl);
        if (now % 400 != (p.getId() * 7 & 255) || !w.outside || w.underground) return;
        Data d = Data.get(lvl);
        long day = Dread.day(lvl);
        for (int i = 0; i < ORDER.length; i++) {
            Site s = d.sites.get(ORDER[i]);
            if (s != null) continue;
            if (day < DAY[i] && !Dev.on()) return;
            if (i > 0) {
                Site prev = d.sites.get(ORDER[i - 1]);
                if (prev == null || !prev.found && day < prev.day + 2) return;
            }
            place(p, w, d, ORDER[i], 80, 130);
            return;
        }
    }

    public static String status(ServerPlayer p) {
        Data d = Data.get(p.serverLevel());
        StringBuilder b = new StringBuilder("lore:");
        for (String k : ORDER) {
            Site s = d.sites.get(k);
            b.append(' ').append(k).append('=').append(s == null ? "-" : s.pos.toShortString() + (s.found ? " found" : ""));
        }
        return b.toString();
    }

    public static boolean force(ServerPlayer p, String kind) {
        for (String k : ORDER) {
            if (k.equals(kind)) {
                Data d = Data.get(p.serverLevel());
                d.sites.remove(k);
                return place(p, Watch.of(p), d, k, 24, 40);
            }
        }
        return false;
    }

    private static void discover(ServerPlayer p, Profile pr, ServerLevel lvl) {
        Data d = Data.get(lvl);
        for (int i = 0; i < ORDER.length; i++) {
            Site s = d.sites.get(ORDER[i]);
            if (s == null || pr.lore.contains(ORDER[i])) continue;
            double dx = p.getX() - s.pos.getX() - 0.5, dz = p.getZ() - s.pos.getZ() - 0.5;
            if (dx * dx + dz * dz > 81 || Math.abs(p.getY() - s.pos.getY()) > 8) continue;
            pr.lore.add(ORDER[i]);
            s.found = true;
            d.setDirty();
            pr.moment(new Moment(Moment.LORE, Dread.day(lvl), Dread.time(lvl), s.pos, i + 1, 0));
            Fx.sound(p, Holder.direct(SoundEvents.AMBIENT_CAVE.value()), SoundSource.AMBIENT, Vec3.atCenterOf(s.pos), 0.5f, 0.7f);
            Dev.say(p, "Bạn tìm thấy địa điểm cốt truyện " + ORDER[i] + ". Đọc sách và biển ở đó");
        }
    }

    private static boolean place(ServerPlayer p, Watch w, Data d, String kind, double near, double far) {
        ServerLevel lvl = p.serverLevel();
        RandomSource r = p.getRandom();
        double base = w.speed > 0.05 ? Math.toDegrees(Math.atan2(-w.vel.x, w.vel.z)) : p.getYRot();
        for (int t = 0; t < 14; t++) {
            double yaw = Math.toRadians(base + (r.nextDouble() - 0.5) * 100);
            double dist = near + r.nextDouble() * (far - near);
            int x = Mth.floor(p.getX() - Math.sin(yaw) * dist), z = Mth.floor(p.getZ() + Math.cos(yaw) * dist);
            BlockPos o = flat(lvl, x, z, 6);
            if (o == null || !clear(lvl, o, near * 0.8)) continue;
            Direction f = Direction.fromYRot(Math.toDegrees(Math.atan2(-(p.getX() - o.getX()), p.getZ() - o.getZ())));
            B b = new B(lvl, o, f);
            switch (kind) {
                case "s1" -> station(b, 1, p);
                case "s2" -> station(b, 2, p);
                case "s3" -> station(b, 3, p);
                case "g" -> clearing(b);
                default -> house(b, p);
            }
            Site s = new Site();
            s.pos = o;
            s.day = Dread.day(lvl);
            d.sites.put(kind, s);
            d.setDirty();
            CaveMind.debug("lore site {} built at {}", kind, o);
            Dev.say(p, "Đã dựng địa điểm cốt truyện " + kind + " ở " + o.toShortString());
            return true;
        }
        return false;
    }

    private static boolean clear(ServerLevel lvl, BlockPos o, double min) {
        for (ServerPlayer q : lvl.players()) {
            if (q.position().distanceTo(Vec3.atCenterOf(o)) < min) return false;
        }
        return true;
    }

    private static BlockPos flat(ServerLevel lvl, int x, int z, int rad) {
        if (!lvl.hasChunkAt(new BlockPos(x - rad, 0, z - rad)) || !lvl.hasChunkAt(new BlockPos(x + rad, 0, z + rad))) return null;
        int lo = Integer.MAX_VALUE, hi = Integer.MIN_VALUE;
        List<Integer> hs = new ArrayList<>();
        for (int dx = -rad; dx <= rad; dx += 2) {
            for (int dz = -rad; dz <= rad; dz += 2) {
                int h = lvl.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x + dx, z + dz) - 1;
                BlockPos top = new BlockPos(x + dx, h, z + dz);
                BlockState st = lvl.getBlockState(top);
                if (!lvl.getFluidState(top).isEmpty() || !lvl.getFluidState(top.above()).isEmpty()) return null;
                if (!(st.is(BlockTags.DIRT) || st.is(BlockTags.SAND) || st.is(Blocks.SNOW_BLOCK) || st.is(Blocks.GRAVEL))) return null;
                lo = Math.min(lo, h);
                hi = Math.max(hi, h);
                hs.add(h);
            }
        }
        if (hi - lo > 3 || lo < lvl.getSeaLevel() - 2) return null;
        hs.sort(null);
        return new BlockPos(x, hs.get(hs.size() / 2), z);
    }

    static final class B {
        final ServerLevel lvl;
        final BlockPos o;
        final Direction f, right, back;
        final Rotation rot;

        B(ServerLevel lvl, BlockPos o, Direction f) {
            this.lvl = lvl;
            this.o = o;
            this.f = f;
            this.right = f.getClockWise();
            this.back = f.getOpposite();
            this.rot = switch (f) {
                case EAST -> Rotation.CLOCKWISE_90;
                case SOUTH -> Rotation.CLOCKWISE_180;
                case WEST -> Rotation.COUNTERCLOCKWISE_90;
                default -> Rotation.NONE;
            };
        }

        BlockPos at(int x, int y, int z) {
            return o.relative(right, x).relative(back, z).above(y);
        }

        void set(int x, int y, int z, BlockState s) {
            lvl.setBlock(at(x, y, z), s.rotate(rot), 2);
        }

        int ground(int x, int z) {
            BlockPos q = at(x, 0, z);
            return lvl.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, q.getX(), q.getZ()) - 1 - o.getY();
        }

        void flatten(int x0, int x1, int z0, int z1, BlockState top) {
            for (int x = x0; x <= x1; x++) {
                for (int z = z0; z <= z1; z++) {
                    int g = ground(x, z);
                    for (int y = Math.min(g, 0) - 1; y < 0; y++) set(x, y, z, Blocks.DIRT.defaultBlockState());
                    set(x, 0, z, top);
                    for (int y = 1; y <= 9; y++) set(x, y, z, Blocks.AIR.defaultBlockState());
                }
            }
        }

        void path(int x0, int z0, int x1, int z1) {
            int n = Math.max(Math.abs(x1 - x0), Math.abs(z1 - z0));
            for (int i = 0; i <= n; i++) {
                int x = Math.round(x0 + (x1 - x0) * i / (float) n), z = Math.round(z0 + (z1 - z0) * i / (float) n);
                int g = ground(x, z);
                BlockPos q = at(x, g, z);
                BlockState st = lvl.getBlockState(q);
                if (st.is(Blocks.GRASS_BLOCK) || st.is(Blocks.DIRT) || st.is(Blocks.PODZOL) || st.is(Blocks.COARSE_DIRT)) {
                    lvl.setBlock(q, Blocks.DIRT_PATH.defaultBlockState(), 2);
                    BlockState up = lvl.getBlockState(q.above());
                    if (up.canBeReplaced() && up.getFluidState().isEmpty()) lvl.setBlock(q.above(), Blocks.AIR.defaultBlockState(), 2);
                }
            }
        }

        void door(int x, int z, Block door, Direction facing, boolean open) {
            BlockState s = door.defaultBlockState().setValue(DoorBlock.FACING, facing).setValue(DoorBlock.OPEN, open).setValue(DoorBlock.HINGE, DoorHingeSide.LEFT);
            set(x, 1, z, s.setValue(DoorBlock.HALF, DoubleBlockHalf.LOWER));
            set(x, 2, z, s.setValue(DoorBlock.HALF, DoubleBlockHalf.UPPER));
        }

        void bed(int x, int z, Block bed, Direction head) {
            int hx = x + head.getStepX(), hz = z + head.getStepZ();
            BlockState s = bed.defaultBlockState().setValue(BedBlock.FACING, head);
            set(x, 1, z, s.setValue(BedBlock.PART, BedPart.FOOT));
            set(hx, 1, hz, s.setValue(BedBlock.PART, BedPart.HEAD));
        }

        void wallSign(int x, int y, int z, Block sign, Direction facing, Component... lines) {
            set(x, y, z, sign.defaultBlockState().setValue(WallSignBlock.FACING, facing));
            text(at(x, y, z), lines);
        }

        void standSign(int x, int z, Block sign, float localYaw, Component... lines) {
            float yaw = localYaw + rot.ordinal() * 90f;
            lvl.setBlock(at(x, 1, z), sign.defaultBlockState().setValue(StandingSignBlock.ROTATION, RotationSegment.convertToSegment(yaw)), 2);
            text(at(x, 1, z), lines);
        }

        void text(BlockPos q, Component... lines) {
            if (lvl.getBlockEntity(q) instanceof SignBlockEntity sb) {
                SignText t = sb.getFrontText();
                for (int i = 0; i < 4 && i < lines.length; i++) t = t.setMessage(i, lines[i]);
                sb.setText(t, true);
                sb.setWaxed(true);
                sb.setChanged();
            }
        }

        void fill(int x, int y, int z, ItemStack... items) {
            if (lvl.getBlockEntity(at(x, y, z)) instanceof Container c) {
                RandomSource r = lvl.getRandom();
                for (ItemStack it : items) {
                    for (int k = 0; k < 8; k++) {
                        int slot = r.nextInt(c.getContainerSize());
                        if (c.getItem(slot).isEmpty()) {
                            c.setItem(slot, it);
                            break;
                        }
                    }
                }
            }
        }

        void lectern(int x, int z, Direction facing, ItemStack book) {
            set(x, 1, z, Blocks.LECTERN.defaultBlockState().setValue(LecternBlock.FACING, facing).setValue(LecternBlock.HAS_BOOK, true));
            if (lvl.getBlockEntity(at(x, 1, z)) instanceof LecternBlockEntity le) le.setBook(book);
        }
    }

    static Component tr(String k) {
        return Component.translatable("cavemind.lore." + k);
    }

    static ItemStack book(String key, int pages, String author) {
        ItemStack book = new ItemStack(Items.WRITTEN_BOOK);
        CompoundTag tag = book.getOrCreateTag();
        tag.putString("title", "...");
        tag.putString("author", author);
        tag.putInt("generation", 3);
        ListTag list = new ListTag();
        for (int i = 1; i <= pages; i++) list.add(StringTag.valueOf(Component.Serializer.toJson(tr(key + ".p" + i))));
        tag.put("pages", list);
        tag.putBoolean("resolved", true);
        tag.putBoolean("cavemind", true);
        book.setHoverName(tr(key + ".title").copy().withStyle(s -> s.withItalic(false)));
        return book;
    }

    private static void station(B b, int n, ServerPlayer p) {
        BlockState plank = Blocks.SPRUCE_PLANKS.defaultBlockState(), log = Blocks.STRIPPED_SPRUCE_LOG.defaultBlockState();
        BlockState grass = Blocks.GRASS_BLOCK.defaultBlockState();
        b.flatten(-5, 5, -7, 4, grass);
        for (int x = -2; x <= 2; x++) {
            for (int z = -2; z <= 2; z++) {
                b.set(x, 0, z, plank);
                boolean wall = Math.abs(x) == 2 || Math.abs(z) == 2;
                boolean corner = Math.abs(x) == 2 && Math.abs(z) == 2;
                for (int y = 1; y <= 3; y++) {
                    if (wall) b.set(x, y, z, corner ? log : plank);
                }
            }
        }
        for (int x = -3; x <= 3; x++) {
            for (int z = -3; z <= 3; z++) b.set(x, 4, z, Blocks.SPRUCE_SLAB.defaultBlockState().setValue(SlabBlock.TYPE, SlabType.BOTTOM));
        }
        if (n < 3) b.door(0, -2, Blocks.SPRUCE_DOOR, Direction.SOUTH, true);
        else {
            b.set(0, 1, -2, Blocks.AIR.defaultBlockState());
            b.set(0, 2, -2, Blocks.AIR.defaultBlockState());
        }
        b.set(-2, 2, 0, switch (n) {
            case 1 -> Blocks.GLASS_PANE.defaultBlockState();
            case 2 -> Blocks.OAK_PLANKS.defaultBlockState();
            default -> Blocks.AIR.defaultBlockState();
        });
        if (n < 3) b.bed(1, 0, Blocks.RED_BED, Direction.SOUTH);
        else b.bed(4, -6, Blocks.RED_BED, Direction.NORTH);
        if (n >= 2) b.set(2, 1, 1, Blocks.AIR.defaultBlockState());
        b.lectern(-1, 1, Direction.NORTH, book("s" + n, 5, "V. Đ. Khiêm"));
        b.set(-1, 1, -1, Blocks.BARREL.defaultBlockState());
        switch (n) {
            case 1 -> b.fill(-1, 1, -1, new ItemStack(Items.BREAD, 3), new ItemStack(Items.ROTTEN_FLESH, 3), new ItemStack(Items.PAPER, 5));
            case 2 -> b.fill(-1, 1, -1, new ItemStack(Items.ROTTEN_FLESH, 9), new ItemStack(Items.OAK_PLANKS, 2), new ItemStack(Items.CANDLE, 1));
            default -> b.fill(-1, 1, -1, new ItemStack(Items.ROTTEN_FLESH, 14));
        }
        String[][] tally = {{"IIII I", "", "", ""}, {"IIII IIII", "IIII", "", ""}, {"IIII IIII", "IIII IIII", "", "II"}};
        Component[] lines = new Component[4];
        for (int i = 0; i < 4; i++) lines[i] = Component.literal(tally[n - 1][i]);
        b.wallSign(0, 2, 1, Blocks.SPRUCE_WALL_SIGN, Direction.NORTH, lines);
        b.set(-2, 1, -4, Blocks.STRIPPED_SPRUCE_LOG.defaultBlockState());
        b.set(-2, 2, -4, Blocks.STRIPPED_SPRUCE_LOG.defaultBlockState());
        BlockState bell = Blocks.BELL.defaultBlockState().setValue(BellBlock.FACING, Direction.EAST).setValue(BellBlock.ATTACHMENT, BellAttachType.FLOOR);
        if (n < 3) b.set(-2, 3, -4, bell);
        else b.set(-1, 1, -5, bell);
        b.set(2, 1, -4, Blocks.SPRUCE_FENCE.defaultBlockState());
        b.set(2, 2, -4, n < 3 ? Blocks.LANTERN.defaultBlockState() : Blocks.SOUL_LANTERN.defaultBlockState());
        b.set(-4, 1, -6, Blocks.CAMPFIRE.defaultBlockState().setValue(CampfireBlock.LIT, false));
        if (n == 1) b.path(-14, 0, -8, 0);
        else b.path(-16, 0, -3, 0);
        if (n == 3) b.path(0, -3, 4, -5);
    }

    private static void clearing(B b) {
        b.flatten(-5, 6, -5, 5, Blocks.GRASS_BLOCK.defaultBlockState());
        for (int y = 1; y <= 5; y++) b.set(0, y, 0, Blocks.STRIPPED_OAK_LOG.defaultBlockState());
        b.set(0, 6, 0, Blocks.OAK_FENCE.defaultBlockState());
        BlockState bx = Blocks.STRIPPED_OAK_LOG.defaultBlockState().setValue(net.minecraft.world.level.block.RotatedPillarBlock.AXIS, Direction.Axis.X);
        BlockState bz = Blocks.STRIPPED_OAK_LOG.defaultBlockState().setValue(net.minecraft.world.level.block.RotatedPillarBlock.AXIS, Direction.Axis.Z);
        b.set(1, 4, 0, bx);
        b.set(1, 5, 0, Blocks.OAK_FENCE.defaultBlockState());
        b.set(-1, 5, 0, bx);
        b.set(-1, 6, 0, Blocks.OAK_FENCE.defaultBlockState());
        b.set(0, 3, 1, bz);
        b.set(0, 4, 1, Blocks.OAK_FENCE.defaultBlockState());
        b.wallSign(0, 2, -1, Blocks.OAK_WALL_SIGN, Direction.NORTH, tr("g.sign1"), tr("g.sign2"), tr("g.sign3"), tr("g.sign4"));
        int[][] ring = {{3, 0}, {2, 2}, {0, 3}, {-2, 2}, {-3, 0}, {-2, -2}, {0, -3}, {2, -2}};
        RandomSource r = b.lvl.getRandom();
        for (int[] q : ring) {
            b.set(q[0], 1, q[1], Blocks.CANDLE.defaultBlockState().setValue(CandleBlock.CANDLES, 1 + r.nextInt(3)).setValue(CandleBlock.LIT, false));
        }
        for (int y = 1; y <= 3; y++) b.set(5, y, 3, Blocks.STRIPPED_BIRCH_LOG.defaultBlockState());
        b.standSign(-3, -4, Blocks.OAK_SIGN, 180f, tr("g.count1"), tr("g.count2"), tr("g.count3"), tr("g.count4"));
        b.set(1, 1, 1, Blocks.CHEST.defaultBlockState());
        b.fill(1, 1, 1, book("g", 6, "..."), new ItemStack(Items.RED_DYE, 2), new ItemStack(Items.STICK, 3));
        BlockPos stand = b.at(5, 1, 2);
        Vec3 to = Vec3.atCenterOf(b.at(5, 1, 3)).subtract(Vec3.atCenterOf(stand));
        float yaw = (float) Math.toDegrees(Math.atan2(-to.x, to.z));
        String nbt = "{Small:1b,NoBasePlate:1b,ShowArms:1b,Invulnerable:1b,Rotation:[" + yaw + "f,0f],"
            + "Pose:{Head:[38f,0f,0f],RightArm:[-118f,18f,0f],LeftArm:[-118f,-18f,0f],Body:[6f,0f,0f]},"
            + "ArmorItems:[{},{},{id:\"minecraft:leather_chestplate\",Count:1b,tag:{display:{color:9450776}}},{}]}";
        cmd(b.lvl, "summon minecraft:armor_stand " + (stand.getX() + 0.5) + " " + stand.getY() + " " + (stand.getZ() + 0.5) + " " + nbt);
    }

    private static void house(B b, ServerPlayer p) {
        BlockState plank = Blocks.BIRCH_PLANKS.defaultBlockState(), log = Blocks.STRIPPED_BIRCH_LOG.defaultBlockState();
        b.flatten(-5, 5, -5, 5, Blocks.GRASS_BLOCK.defaultBlockState());
        for (int x = -3; x <= 3; x++) {
            for (int z = -2; z <= 3; z++) {
                b.set(x, 0, z, plank);
                boolean wall = Math.abs(x) == 3 || z == -2 || z == 3;
                boolean corner = Math.abs(x) == 3 && (z == -2 || z == 3);
                for (int y = 1; y <= 3; y++) if (wall) b.set(x, y, z, corner ? log : plank);
            }
        }
        for (int x = -4; x <= 4; x++) {
            for (int z = -3; z <= 4; z++) b.set(x, 4, z, Blocks.BIRCH_SLAB.defaultBlockState().setValue(SlabBlock.TYPE, SlabType.BOTTOM));
        }
        b.door(0, -2, Blocks.BIRCH_DOOR, Direction.SOUTH, false);
        b.set(-3, 2, 0, Blocks.GLASS_PANE.defaultBlockState());
        b.set(3, 2, 0, Blocks.GLASS_PANE.defaultBlockState());
        for (int x = -2; x <= 0; x++) b.bed(x, 1, Blocks.WHITE_BED, Direction.SOUTH);
        b.set(1, 1, 2, plank);
        b.set(1, 2, 2, plank);
        b.set(1, 3, 2, plank);
        b.door(2, 1, Blocks.SPRUCE_DOOR, Direction.SOUTH, false);
        b.set(2, 1, 2, Blocks.CHEST.defaultBlockState());
        b.fill(2, 1, 2, book("h", 4, "..."), new ItemStack(Items.BONE, 3));
        b.set(2, 2, 2, Blocks.SKELETON_SKULL.defaultBlockState().setValue(SkullBlock.ROTATION, 8));
        String[] lost = {"Linh", "Tuấn", "Mai", "Bảo", "Hà", "Nam", "Khoa"};
        Component[] a = new Component[4], c = new Component[4];
        for (int i = 0; i < 4; i++) a[i] = struck(lost[i]);
        for (int i = 0; i < 3; i++) c[i] = struck(lost[4 + i]);
        c[3] = Component.literal(p.getGameProfile().getName()).withStyle(Style.EMPTY.withColor(0x5a0f0f));
        b.wallSign(1, 2, -1, Blocks.BIRCH_WALL_SIGN, Direction.SOUTH, a);
        b.wallSign(2, 2, -1, Blocks.BIRCH_WALL_SIGN, Direction.SOUTH, c);
        b.set(-2, 1, -1, Blocks.CANDLE.defaultBlockState().setValue(CandleBlock.CANDLES, 4).setValue(CandleBlock.LIT, false));
        b.set(0, 1, -3, Blocks.CANDLE.defaultBlockState().setValue(CandleBlock.LIT, true));
    }

    static Component struck(String s) {
        return Component.literal(s).withStyle(Style.EMPTY.withStrikethrough(true).withColor(0x3a3a3a));
    }

    static void cmd(ServerLevel lvl, String c) {
        try {
            lvl.getServer().getCommands().performPrefixedCommand(lvl.getServer().createCommandSourceStack().withLevel(lvl).withSuppressedOutput().withPermission(4), c);
        } catch (Throwable t) {
            CaveMind.LOG.warn("[CaveMind] lore command failed: {}", c, t);
        }
    }

    private Lore() {}
}
