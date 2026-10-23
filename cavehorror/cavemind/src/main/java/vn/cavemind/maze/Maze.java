package vn.cavemind.maze;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BellBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CandleBlock;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.LightBlock;
import net.minecraft.world.level.block.StandingSignBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.WallSignBlock;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.entity.SignText;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BellAttachType;
import net.minecraft.world.level.block.state.properties.DoorHingeSide;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.level.block.state.properties.RotationSegment;
import net.minecraft.world.phys.Vec3;
import vn.cavemind.CaveMind;
import vn.cavemind.Cfg;
import vn.cavemind.compat.Compat;
import vn.cavemind.director.Dread;
import vn.cavemind.director.PaceLog;
import vn.cavemind.maze.core.Brain;
import vn.cavemind.maze.core.Grid;
import vn.cavemind.maze.core.Round;
import vn.cavemind.memory.MindData;
import vn.cavemind.memory.Moment;
import vn.cavemind.memory.Profile;
import vn.cavemind.sense.Ears;
import vn.cavemind.sense.Watch;
import vn.cavemind.world.Fx;
import vn.cavemind.world.Motion;

public final class Maze {
    public enum Mode { PLAY, PUNISH }

    public static final ResourceKey<Level> DIM = ResourceKey.create(Registries.DIMENSION, new ResourceLocation(CaveMind.MODID, "gio_48"));
    static final int F = 64, SPACING = 2048;
    static final String TOY = "cavemind_toy", RUN = "cavemind_run", BATTERY = "cavemind_battery";

    record Ret(ResourceKey<Level> dim, Vec3 pos, float yaw) {}

    static final class Ink {
        final List<BlockPos> veins = new ArrayList<>();
        long next;
    }

    static final class Run {
        final int slot;
        final Round core;
        final Mode mode;
        final ServerLevel lvl;
        final int ox, oz;
        final Map<UUID, Integer> ids = new HashMap<>();
        final Map<UUID, Ret> back = new HashMap<>();
        final UUID[] toys = new UUID[7];
        UUID body;
        long start, lastNoise;
        Brain.Mode shown;
        double bx, bz, steps;
        boolean done;
        int nextId = 1;
        final List<BlockPos> lamps = new ArrayList<>();
        final List<Integer> lampLv = new ArrayList<>();
        final Set<Integer> dimmed = new HashSet<>();
        long nextAmbient, nextVoice;
        final Set<Integer> blocked = new HashSet<>();
        final Map<UUID, Integer> decoys = new HashMap<>();
        boolean crimson;
        final Map<Integer, Ink> ink = new HashMap<>();
        final Map<Integer, Long> tapped = new HashMap<>();
        long nextScrape;
        final List<BlockPos> drips = new ArrayList<>();
        final double[] toyY = new double[7];
        final double[][] toyAt = new double[7][];
        final Map<UUID, Set<Integer>> told = new HashMap<>();

        Run(int slot, Round core, Mode mode, ServerLevel lvl) {
            this.slot = slot;
            this.core = core;
            this.mode = mode;
            this.lvl = lvl;
            this.ox = 1024 + slot * SPACING;
            this.oz = 1024;
            java.util.Arrays.fill(toyY, F + 1.1);
        }

        BlockPos at(int x, int y, int z) {
            return new BlockPos(ox + x, y, oz + z);
        }

        Vec3 world(double x, double z, double y) {
            return new Vec3(ox + x, y, oz + z);
        }
    }

    private static final Map<Integer, Run> RUNS = new HashMap<>();
    private static final Map<UUID, Integer> WHO = new HashMap<>();
    private static final Map<UUID, List<ItemStack>> KEEP = new HashMap<>();
    private static final Set<UUID> DIED = new HashSet<>();
    private static BlockPos lid;
    private static long lidUntil;

    public static boolean on() {
        return Compat.damnedActive() && Cfg.MAZE.get();
    }

    static ServerLevel level(MinecraftServer s) {
        return s.getLevel(DIM);
    }

    public static boolean inMaze(ServerPlayer p) {
        return p.level().dimension() == DIM;
    }

    public static boolean locked(ServerPlayer p) {
        return inMaze(p) && !p.isCreative();
    }

    public static boolean active(ServerPlayer p) {
        return WHO.containsKey(p.getUUID());
    }

    public static void clear() {
        for (Run r : RUNS.values()) unforce(r);
        RUNS.clear();
        WHO.clear();
        lid = null;
        HINT.clear();
        Cam.clear();
    }

    public static String describe(ServerPlayer p) {
        Integer slot = WHO.get(p.getUUID());
        if (slot == null) return null;
        Run r = RUNS.get(slot);
        if (r == null) return null;
        Brain b = r.core.brain;
        return "Giờ Thứ Bốn Mươi Tám (" + (r.mode == Mode.PLAY ? "chơi" : "trừng phạt") + "), " + b.mode + ": " + b.why + ", còn " + (Round.TIME - r.core.t) / 20 + "s, đồ chơi "
            + (7 - r.core.remainingToys()) + "/7";
    }

    public static boolean drag(ServerPlayer p, Mode m) {
        if (!on()) return false;
        ServerLevel lvl = level(p.server);
        if (lvl == null) return false;
        Run r = open(p.server, lvl, m);
        if (r == null) return false;
        join(r, p, new Ret(p.level().dimension(), p.position(), p.getYRot()));
        PaceLog.encounter(p, "damned", "maze", m.name());
        return true;
    }

    static Run open(MinecraftServer server, ServerLevel lvl, Mode m) {
        int slot = 0;
        while (RUNS.containsKey(slot)) slot++;
        long seed = server.overworld().getRandom().nextLong();
        Round core = new Round(seed, m == Mode.PLAY ? Round.Mode.PLAY : Round.Mode.PUNISH);
        Run r = new Run(slot, core, m, lvl);
        r.crimson = vn.cavemind.world.Moon.is(server.overworld(), vn.cavemind.world.Moon.CRIMSON);
        r.start = server.getTickCount();
        r.lastNoise = r.start;
        try {
            force(r, true);
            hangers(r);
            build(r);
            spawnBody(r);
            spawnToys(r);
            spawnBatteries(r);
        } catch (Throwable t) {
            CaveMind.LOG.warn("[CaveMind] maze build failed", t);
            unforce(r);
            return null;
        }
        RUNS.put(slot, r);
        CaveMind.debug("maze {} opened in slot {} (seed {})", m, slot, seed);
        return r;
    }

    static void force(Run r, boolean on) {
        int x0 = (r.ox - 8) >> 4, x1 = (r.ox + Grid.S + 8) >> 4, z0 = (r.oz - 8) >> 4, z1 = (r.oz + Grid.S + 8) >> 4;
        for (int cx = x0; cx <= x1; cx++) {
            for (int cz = z0; cz <= z1; cz++) {
                r.lvl.setChunkForced(cx, cz, on);
                if (on) r.lvl.getChunk(cx, cz);
            }
        }
    }

    static void unforce(Run r) {
        try {
            force(r, false);
        } catch (Throwable ignored) {
        }
    }

    static void join(Run r, ServerPlayer p, Ret ret) {
        Round.Runner rn = r.core.add(r.nextId++);
        r.ids.put(p.getUUID(), rn.id);
        r.back.put(p.getUUID(), ret);
        WHO.put(p.getUUID(), r.slot);
        Profile pr = MindData.profile(p);
        pr.mazeRet = ret.pos() == null ? null : BlockPos.containing(ret.pos());
        Vec3 at = r.world(r.core.g.spawn[0], r.core.g.spawn[1], F + 1);
        p.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 60, 0, false, false));
        p.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 30, 0, false, false));
        Bag.stash(p);
        p.fallDistance = 0;
        p.teleportTo(r.lvl, at.x, at.y, at.z, 0f, 0f);
        p.fallDistance = 0;
        rn.x = r.core.g.spawn[0];
        rn.z = r.core.g.spawn[1];
        title(p, "Giờ Thứ Bốn Mươi Tám", r.mode == Mode.PLAY ? "Nó đếm bốn mươi tám nhịp. Đi trốn đi." : "Ai thua thì đếm.");
        if (r.crimson) Fx.later(r.lvl, 90, () -> say(p, "Trăng Thẫm: nó nghe rõ hơn mọi đêm, nhưng quà dưới này cũng quý hơn.", ChatFormatting.DARK_RED));
        Cam.enter(p);
        Fx.sound(p, Compat.sound("apollyon:cutscene_rise", "minecraft:ambient.cave"), SoundSource.AMBIENT, p.position(), 0.7f, 0.8f);
        if (!pr.mazeGuide) {
            pr.mazeGuide = true;
            Fx.later(r.lvl, 70, () -> {
                if (inMaze(p) && active(p)) vn.cavemind.chapter.Book.open(p, 4);
            });
        }
        for (ServerPlayer o : players(p.server, r)) if (o != p) say(o, p.getGameProfile().getName() + " cũng vừa rơi xuống đây.", ChatFormatting.GRAY);
    }

    static void title(ServerPlayer p, String big, String small) {
        p.connection.send(new net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket(10, 60, 20));
        p.connection.send(new net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket(Component.literal(small).withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC)));
        p.connection.send(new net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket(Component.literal(big).withStyle(ChatFormatting.DARK_RED)));
    }

    static void say(ServerPlayer p, String s, ChatFormatting c) {
        p.displayClientMessage(Component.literal(s).withStyle(c, ChatFormatting.ITALIC), true);
    }

    public static boolean bell(ServerPlayer p, BlockPos at) {
        if (inMaze(p)) {
            Run r = run(p);
            if (r == null) return true;
            Round.Runner rn = r.core.runner(r.ids.get(p.getUUID()));
            r.core.deliver(chestToys(r));
            r.core.bell(rn);
            if (r.core.delivered < 7) say(p, "Tiếng chuông vang khắp mê cung. Nó nghe thấy rồi.", ChatFormatting.DARK_GRAY);
            return true;
        }
        if (!on() || p.level().dimension() != Level.OVERWORLD) return false;
        ServerLevel ow = p.serverLevel();
        BlockPos b = vn.cavemind.lore.Lore.part(ow, "p", "bell");
        BlockPos l = vn.cavemind.lore.Lore.part(ow, "p", "lid");
        if (b == null || l == null) return false;
        BlockState c = ow.getBlockState(l.above());
        boolean lit = c.getBlock() instanceof CandleBlock && c.getValue(CandleBlock.LIT);
        if (!b.equals(at)) {
            if (lit && at.distSqr(l) < 40 * 40) say(p, "Không phải cái chuông này. Phải là chuông của trường, ngay trong sân chơi.", ChatFormatting.DARK_GRAY);
            return false;
        }
        if (!lit) {
            say(p, "Không có gì xảy ra. Phải có một ngọn nến đang cháy ngay trên nắp giếng.", ChatFormatting.DARK_GRAY);
            return true;
        }
        if (Compat.exempt(p)) {
            say(p, "Nó không chơi với người ở chế độ sáng tạo hay khán giả.", ChatFormatting.DARK_GRAY);
            return true;
        }
        unchain(ow, l);
        for (int k = 1; k <= 2; k++) {
            BlockState above = ow.getBlockState(l.above(k));
            if (above.getBlock() instanceof CandleBlock || !above.isAir() && above.canBeReplaced()) {
                ow.setBlock(l.above(k), Blocks.AIR.defaultBlockState(), 3);
            }
        }
        Fx.soundAll(ow, "minecraft:block.candle.extinguish", SoundSource.BLOCKS, Vec3.atCenterOf(l.above()), 0.8f, 0.7f);
        BlockState st = ow.getBlockState(l);
        if (st.getBlock() instanceof TrapDoorBlock) ow.setBlock(l, st.setValue(TrapDoorBlock.OPEN, true), 3);
        lid = l;
        boolean together = vn.cavemind.chapter.Book.together(p.server);
        lidUntil = p.server.getTickCount() + (together ? 1200 : 600);
        Fx.soundAll(ow, "minecraft:block.iron_trapdoor.open", SoundSource.BLOCKS, Vec3.atCenterOf(l), 1.0f, 0.6f);
        Fx.soundAll(ow, "apollyon:deep_laugh", SoundSource.HOSTILE, Vec3.atCenterOf(l).add(0, -6, 0), 0.6f, 1.3f);
        for (ServerPlayer o : ow.players()) {
            double d = Math.sqrt(o.distanceToSqr(Vec3.atCenterOf(l)));
            if (d < 32) say(o, "Ngọn nến rơi xuống giếng và nắp giếng mở ra. Nhảy xuống đi, nó chỉ mở một lúc.", ChatFormatting.GRAY);
            else if (together && o != p) o.displayClientMessage(Component.literal(p.getGameProfile().getName() + " vừa mở giếng số bốn ở hướng " + vn.cavemind.lore.Points.heading(o.blockPosition(), l)
                + ", cách " + (int) d + " khối. Tới kịp trong một phút thì nhảy xuống chơi cùng.").withStyle(ChatFormatting.LIGHT_PURPLE), false);
        }
        return true;
    }

    private static final java.util.Map<java.util.UUID, Integer> HINT = new java.util.HashMap<>();

    // Older playgrounds hung the chain one block above the lid, right in the shaft: nobody fit past it.
    static void unchain(ServerLevel ow, BlockPos l) {
        BlockPos low = l.above(2);
        if (!ow.isLoaded(low) || !ow.getBlockState(low).is(Blocks.CHAIN)) return;
        ow.setBlock(low, Blocks.AIR.defaultBlockState(), 3);
        BlockPos hi = l.above(3);
        BlockState up = ow.getBlockState(hi);
        if (up.isAir() || up.getBlock() instanceof net.minecraft.world.level.block.SlabBlock) ow.setBlock(hi, Blocks.CHAIN.defaultBlockState(), 3);
    }

    static void hint(ServerPlayer p, long now) {
        if (now % 20 != (p.getId() & 15)) return;
        ServerLevel ow = p.serverLevel();
        BlockPos l = vn.cavemind.lore.Lore.part(ow, "p", "lid");
        if (l == null || p.distanceToSqr(Vec3.atCenterOf(l)) > 14 * 14) {
            HINT.remove(p.getUUID());
            return;
        }
        unchain(ow, l);
        int state = 0;
        BlockState c = ow.getBlockState(l.above());
        if (c.getBlock() instanceof CandleBlock && c.getValue(CandleBlock.LIT)) state = 2;
        else {
            for (BlockPos q : BlockPos.betweenClosed(l.offset(-2, -1, -2), l.offset(2, 2, 2))) {
                BlockState s = ow.getBlockState(q);
                if (s.getBlock() instanceof CandleBlock && s.getValue(CandleBlock.LIT)) {
                    state = 1;
                    break;
                }
            }
        }
        Integer last = HINT.put(p.getUUID(), state);
        if (last != null && last == state) return;
        if (state == 2) say(p, "Ngọn nến cháy trên nắp giếng. Giờ rung cái chuông của trường trong sân chơi.", ChatFormatting.GRAY);
        else if (state == 1) say(p, "Ngọn nến phải đặt ngay trên nắp giếng sắt mới được.", ChatFormatting.DARK_GRAY);
    }

    static int chestToys(Run r) {
        BlockPos c = r.at((int) r.core.g.chest[0], F + 1, (int) r.core.g.chest[1]);
        if (!(r.lvl.getBlockEntity(c) instanceof Container box)) return 0;
        Set<Integer> seen = new HashSet<>();
        for (int i = 0; i < box.getContainerSize(); i++) {
            ItemStack st = box.getItem(i);
            if (st.hasTag() && st.getTag().contains(TOY) && st.getTag().getInt(RUN) == r.slot) seen.add(st.getTag().getInt(TOY));
        }
        return seen.size();
    }

    public static boolean decoy(ServerPlayer p) {
        Run r = run(p);
        if (r == null) return true;
        int n = r.decoys.getOrDefault(p.getUUID(), 0);
        if (n >= 2) return false;
        r.decoys.put(p.getUUID(), n + 1);
        return true;
    }

    public static Vec3 damned(ServerPlayer p) {
        Run r = run(p);
        if (r == null || r.body == null) return null;
        Entity e = r.lvl.getEntity(r.body);
        return e == null ? null : e.getBoundingBox().getCenter();
    }

    static Run run(ServerPlayer p) {
        Integer s = WHO.get(p.getUUID());
        return s == null ? null : RUNS.get(s);
    }

    private static final Map<UUID, Integer> STUCK = new HashMap<>();

    public static boolean inWell(ServerPlayer p) {
        if (p.level().dimension() != Level.OVERWORLD) return false;
        BlockPos l = vn.cavemind.lore.Lore.part(p.serverLevel(), "p", "lid");
        return l != null && Math.abs(p.getX() - l.getX() - 0.5) < 0.9 && Math.abs(p.getZ() - l.getZ() - 0.5) < 0.9 && p.getY() < l.getY() - 0.5 && p.getY() > l.getY() - 19;
    }

    public static boolean fell(ServerPlayer p) {
        if (!inWell(p)) return false;
        p.fallDistance = 0;
        return true;
    }

    static void well(ServerPlayer p) {
        if (!inWell(p)) {
            STUCK.remove(p.getUUID());
            return;
        }
        p.fallDistance = 0;
        if (!p.onGround() || Compat.exempt(p)) return;
        int n = STUCK.merge(p.getUUID(), 1, Integer::sum);
        if (n < 40) return;
        STUCK.remove(p.getUUID());
        BlockPos l = vn.cavemind.lore.Lore.part(p.serverLevel(), "p", "lid");
        Vec3 back = besideWell(p.serverLevel(), l);
        p.teleportTo(back.x, back.y, back.z);
        say(p, "Đáy giếng chỉ có đá. Lối xuống chỉ mở khi có nến cháy trên nắp và chuông trường rung.", ChatFormatting.GRAY);
    }

    static Vec3 besideWell(ServerLevel ow, BlockPos l) {
        Vec3 back = Vec3.atBottomCenterOf(l).add(2.5, 0, 0.5);
        BlockPos g = vn.cavemind.world.Spots.nearFloor(ow, BlockPos.containing(back.x, l.getY() + 1, back.z), 2, 3);
        return g != null ? Vec3.atBottomCenterOf(g) : back.add(0, 1, 0);
    }

    public static void tick(ServerPlayer p, Watch w, long now) {
        if (!on()) return;
        if (p.level().dimension() == Level.OVERWORLD && (now & 1) == 0) well(p);
        if (lid == null && p.level().dimension() == Level.OVERWORLD) hint(p, now);
        if (lid != null && p.level().dimension() == Level.OVERWORLD) {
            if (now > lidUntil) {
                closeLid(p.serverLevel());
            } else if (Math.abs(p.getX() - lid.getX() - 0.5) < 0.8 && Math.abs(p.getZ() - lid.getZ() - 0.5) < 0.8 && p.getY() < lid.getY() - 2
                && p.getY() > lid.getY() - 18 && !Compat.exempt(p) && !active(p)) {
                enterPlay(p);
                return;
            }
        }
        if (!inMaze(p)) {
            if (active(p)) out(p, "rời mê cung");
            return;
        }
        Run r = run(p);
        if (r == null) {
            recover(p);
            return;
        }
        Integer id = r.ids.get(p.getUUID());
        Round.Runner rn = id == null ? null : r.core.runner(id);
        if (rn == null) return;
        rn.x = p.getX() - r.ox;
        rn.z = p.getZ() - r.oz;
        rn.yaw = p.getYRot();
        rn.crouch = p.isCrouching();
        rn.sprint = p.isSprinting();
        rn.speed = w.speed;
        rn.light = holdsLight(p);
        int tx = (int) Math.floor(rn.x), tz = (int) Math.floor(rn.z);
        rn.closet = -1;
        rn.nook = tx >= 0 && tz >= 0 && tx < Grid.S && tz < Grid.S && r.core.g.tile[tx][tz] == Grid.NOOK ? r.core.g.nookIndex(tx, tz) : -1;
        if (rn.exhausted) {
            if (p.isSprinting()) p.setSprinting(false);
            if (now % 20 == 0) p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 30, 0, false, false, false));
        }
        if (tx >= 0 && tz >= 0 && tx < Grid.S && tz < Grid.S && r.core.g.tile[tx][tz] == Grid.CLOSET) {
            rn.closet = r.core.g.closetIndex(tx, tz);
            BlockState st = r.lvl.getBlockState(r.at(tx, F + 1, tz));
            rn.doorClosed = !(st.getBlock() instanceof DoorBlock) || !st.getValue(DoorBlock.OPEN);
        }
        if (w.idle > 2400) end(r, p, Round.End.AFK);
        if (now % 5 == 0) story(r, p, rn);
    }

    static final String[] STORY = {
        "Một chiếc ghế nhỏ quay vào tường. Linh bị phạt đứng ở đây, chiếc nơ vẫn buộc trên lưng ghế.",
        "Bàn ô ăn quan vẽ phấn. Tuấn chơi một mình tới tối, chờ một người tới lượt.",
        "Cái bàn gãy chân, giấy vương đầy sàn. Mai gấp hạc để ước, con thứ tám chưa kịp gấp xong.",
        "Vòng phấn trắng. Con quay của Bảo vẫn nằm giữa vòng, như chưa ai chịu thua.",
        "Trong tủ có mùi tóc ẩm. Hà đã trốn ở đây rất lâu, lâu lắm.",
        "Tấm ván treo bằng xích. Nam giấu nỏ thun lên cao để không đứa nào lấy được.",
        "Ghế đẩu, nến tắt, sáp đông thành lớp. Khoa thổi kèn cho tới khi không còn ai trả lời."
    };

    // The camera's light falling on a toy's little scene is enough for the scene to tell its story.
    static void story(Run r, ServerPlayer p, Round.Runner rn) {
        Set<Integer> seen = r.told.computeIfAbsent(p.getUUID(), k -> new HashSet<>());
        if (seen.size() >= 7) return;
        Vec3 eye = p.getEyePosition(), look = p.getLookAngle();
        double range = Cam.nightVision(p) ? 14 : 2.6;
        Grid g = r.core.g;
        for (int i = 0; i < 7; i++) {
            if (r.core.toyTaken[i] || seen.contains(i)) continue;
            Vec3 d = r.world(g.toys[i][0], g.toys[i][1], r.toyY[i] - 0.3).subtract(eye);
            double len = d.length();
            if (len > range || len < 0.3 || d.scale(1 / len).dot(look) < 0.93) continue;
            if (!g.clear(rn.x, rn.z, g.toys[i][0], g.toys[i][1], r.core.closetOpen)) continue;
            seen.add(i);
            say(p, STORY[i], ChatFormatting.GRAY);
            return;
        }
    }

    static boolean holdsLight(ServerPlayer p) {
        for (ItemStack st : new ItemStack[]{p.getMainHandItem(), p.getOffhandItem()}) {
            if (st.isEmpty()) continue;
            ResourceLocation id = BuiltInRegistries.ITEM.getKey(st.getItem());
            String s = id.getPath();
            if (s.contains("torch") || s.contains("lantern") || s.contains("glowstone") || s.contains("lamp") || s.contains("campfire") || s.contains("shroomlight")
                || s.contains("froglight") || s.equals("lava_bucket") || s.contains("candle") || s.contains("glow_berries")) return true;
        }
        return false;
    }

    static void enterPlay(ServerPlayer p) {
        ServerLevel lvl = level(p.server);
        if (lvl == null) {
            closeLid(p.serverLevel());
            return;
        }
        Run r = null;
        for (Run x : RUNS.values()) if (x.mode == Mode.PLAY && !x.done && x.core.t < Round.COUNT - 100) r = x;
        if (r == null) r = open(p.server, lvl, Mode.PLAY);
        if (r == null) {
            closeLid(p.serverLevel());
            return;
        }
        join(r, p, new Ret(Level.OVERWORLD, besideWell(p.serverLevel(), lid), p.getYRot()));
        PaceLog.encounter(p, "damned", "maze", "PLAY");
    }

    static void closeLid(ServerLevel ow) {
        if (lid == null) return;
        BlockState st = ow.getBlockState(lid);
        if (st.getBlock() instanceof TrapDoorBlock && st.getValue(TrapDoorBlock.OPEN)) {
            ow.setBlock(lid, st.setValue(TrapDoorBlock.OPEN, false), 3);
            Fx.soundAll(ow, "minecraft:block.iron_trapdoor.close", SoundSource.BLOCKS, Vec3.atCenterOf(lid), 1.0f, 0.6f);
        }
        lid = null;
        HINT.clear();
    }

    public static void server(MinecraftServer server) {
        if (RUNS.isEmpty()) return;
        long now = server.getTickCount();
        for (Iterator<Run> it = RUNS.values().iterator(); it.hasNext(); ) {
            Run r = it.next();
            try {
                step(server, r, now);
            } catch (Throwable t) {
                CaveMind.LOG.warn("[CaveMind] maze step failed", t);
                r.done = true;
            }
            if (r.done) {
                cleanup(r);
                it.remove();
            }
        }
    }

    static void step(MinecraftServer server, Run r, long now) {
        List<ServerPlayer> ps = players(server, r);
        if (ps.isEmpty()) {
            r.done = true;
            return;
        }
        for (Ears.Noise n : Ears.since(r.lvl, r.lastNoise)) {
            Integer id = n.who() == null ? null : r.ids.get(n.who());
            r.core.noise(n.pos().x - r.ox, n.pos().z - r.oz, n.radius() * (r.crimson ? 1.15 : 1.0), kind(n.kind()), id == null ? -1 : id);
        }
        r.lastNoise = now;
        r.core.tick();
        sync(r, ps, now);
        flicker(r, now);
        ambient(r, ps, now);
        for (Round.Event ev : r.core.events) event(r, ps, ev, now);
        r.core.events.clear();
        inkTick(r, ps, now);
        if (r.core.mode == Round.Mode.PLAY && r.core.t == Round.COUNT) for (ServerPlayer p : ps) say(p, "…bốn mươi tám. Đi tìm đây.", ChatFormatting.DARK_GRAY);
        if (r.core.brain.mode == Brain.Mode.COUNT && r.core.t % 20 == 0) {
            int beat = (int) (r.core.t / 20) + 1;
            for (ServerPlayer p : ps) say(p, "…" + vn.cavemind.damned.Game.num(Math.min(48, beat)) + "…", ChatFormatting.GRAY);
        }
        if (r.core.t > 0 && r.core.t % 2400 == 0 && r.core.end == Round.End.NONE) {
            for (ServerPlayer p : ps) Fx.sound(p, "minecraft:block.bell.use", SoundSource.AMBIENT, r.world(r.core.g.bell[0], r.core.g.bell[1], F + 3), 0.25f, 0.5f);
        }
        if (r.core.end != Round.End.NONE) finish(server, r, ps);
    }

    static int kind(Ears.Kind k) {
        return switch (k) {
            case SPRINT -> Round.N_SPRINT;
            case JUMP, LAND -> Round.N_JUMP;
            case DOOR, CLOSET -> Round.N_DOOR;
            case TOY -> Round.N_TOY;
            case VOICE -> Round.N_VOICE;
            case BELL -> Round.N_BELL;
            case FIGHT -> Round.N_HIT;
            default -> Round.N_STEP;
        };
    }

    static List<ServerPlayer> players(MinecraftServer server, Run r) {
        List<ServerPlayer> out = new ArrayList<>();
        for (UUID u : r.ids.keySet()) {
            ServerPlayer p = server.getPlayerList().getPlayer(u);
            if (p != null && inMaze(p) && WHO.getOrDefault(u, -1) == r.slot) out.add(p);
        }
        return out;
    }

    static void sync(Run r, List<ServerPlayer> ps, long now) {
        Entity e = r.body == null ? null : r.lvl.getEntity(r.body);
        if (e == null) {
            spawnBody(r);
            e = r.body == null ? null : r.lvl.getEntity(r.body);
            if (e == null) return;
        }
        Brain b = r.core.brain;
        double y = F + 1 + (b.y < -0.5 ? -2.2 : 0);
        Vec3 to = r.world(b.x, b.z, y);
        e.setPos(to.x, to.y, to.z);
        e.setDeltaMovement(Vec3.ZERO);
        if (e instanceof Mob m) {
            float yaw = (float) b.yaw;
            m.setYRot(yaw);
            m.setYHeadRot(yaw);
            m.setYBodyRot(yaw);
            if (b.mode != r.shown) {
                r.shown = b.mode;
                switch (b.mode) {
                    case COUNT -> Motion.pose(m, "cm_count", now);
                    case SWEEP -> Motion.play(m, r.lvl.getRandom().nextBoolean() ? "cm_search_a" : "cm_search_b", 60, now);
                    case OPEN -> Motion.pose(m, "cm_lean", now);
                    case AMBUSH -> Motion.pose(m, "cm_still", now);
                    case STUN -> Motion.play(m, "cm_notice", 30, now);
                    case CHASE, CUT, LAST -> {
                        Motion.stop(m);
                        m.setAggressive(b.mode == Brain.Mode.CHASE);
                    }
                    default -> {
                        Motion.stop(m);
                        m.setAggressive(false);
                    }
                }
            }
            if ((b.mode == Brain.Mode.SEARCH || b.mode == Brain.Mode.LAST) && b.y >= 0) Motion.driftAnim(m, now);
        }
        double mv = Math.hypot(b.x - r.bx, b.z - r.bz);
        r.bx = b.x;
        r.bz = b.z;
        r.steps += mv;
        if (r.steps > 1.6 && b.y >= 0) {
            r.steps = 0;
            boolean fast = b.mode == Brain.Mode.CHASE || b.mode == Brain.Mode.CUT || b.mode == Brain.Mode.LAST;
            for (ServerPlayer p : ps) {
                double d = p.position().distanceTo(e.position());
                if (d > (fast ? 30 : 22)) continue;
                float near = (float) Math.max(0.15, 1 - d / (fast ? 30 : 22));
                Fx.sound(p, Compat.sound("apollyon:step_sound", "minecraft:block.wood.step"), SoundSource.HOSTILE, e.position(), (fast ? 0.9f : 0.55f) * near + 0.1f, fast ? 1.15f : 0.8f);
                if (!fast && r.lvl.getRandom().nextFloat() < 0.18f) Fx.sound(p, "minecraft:block.wooden_door.close", SoundSource.HOSTILE, e.position(), 0.12f * near, 0.45f);
            }
        }
        scrape(r, ps, e, mv, now);
        voice(r, ps, e, now);
        int toys = 7 - r.core.remainingToys();
        int left = (int) Math.max(0, (Round.TIME - r.core.t) / 20);
        boolean hunting = b.mode == Brain.Mode.CHASE || b.mode == Brain.Mode.CUT || b.mode == Brain.Mode.LAST;
        for (ServerPlayer p : ps) {
            double d = p.position().distanceTo(e.position());
            float danger = (float) Math.max(0, Math.min(1, (16 - d) / 13.0));
            if (b.y < 0) danger *= 0.6f;
            if (hunting) danger = Math.max(danger, 0.72f + (float) Math.max(0, (20 - d) / 70.0));
            Integer rid = r.ids.get(p.getUUID());
            Round.Runner rr = rid == null ? null : r.core.runner(rid);
            boolean hiding = rr != null && r.core.tucked(rr);
            if (b.listening() && rr != null && rr.closet >= 0 && rr.closet == r.core.brain.goalCloset()) danger = Math.max(danger, 0.95f);
            boolean seen = rr != null && b.mode == Brain.Mode.CHASE && r.core.brain.focusId() == rr.id;
            Cam.tick(p, Math.min(1f, danger), toys, left, hiding, now, rr, seen, (float) d, rr == null ? 0 : rr.campStage);
            if (rr != null && Cam.panting(p) && now % 50 == (p.getId() % 50) && !r.core.tucked(rr)) r.core.noise(rr.x, rr.z, 4, Round.N_VOICE, rr.id);
        }
        if (now % 60 == 0 && b.y >= 0) {
            for (ServerPlayer p : ps) {
                double d = p.position().distanceTo(e.position());
                if (d < 7) vn.cavemind.damned.Haunt.breath(p, e.position().add(0, 1.4, 0), 0.35f);
            }
        }
    }

    static void event(Run r, List<ServerPlayer> ps, Round.Event ev, long now) {
        Vec3 at = r.world(ev.x(), ev.z(), F + 1.5);
        switch (ev.kind()) {
            case "open", "shut" -> {
                Grid.Closet c = r.core.g.closets.get(ev.a());
                BlockPos d = r.at(c.x, F + 1, c.z);
                BlockState st = r.lvl.getBlockState(d);
                if (st.getBlock() instanceof DoorBlock db) db.setOpen(null, r.lvl, st, d, ev.kind().equals("open"));
            }
            case "grate" -> {
                BlockPos g = r.at((int) Math.floor(ev.x()), F, (int) Math.floor(ev.z()));
                Fx.soundAll(r.lvl, ev.a() == 1 ? "minecraft:block.iron_trapdoor.open" : "minecraft:block.iron_trapdoor.close", SoundSource.BLOCKS, Vec3.atCenterOf(g).add(0, 0.6, 0), 0.9f, 0.7f);
                r.lvl.sendParticles(net.minecraft.core.particles.ParticleTypes.SMOKE, g.getX() + 0.5, g.getY() + 1.05, g.getZ() + 0.5, 4, 0.2, 0.02, 0.2, 0.0);
            }
            case "spot" -> {
                Entity e = r.body == null ? null : r.lvl.getEntity(r.body);
                if (e != null) Motion.play(e, "animation.theDamned.spotted", 10, now);
                for (ServerPlayer p : ps) {
                    Fx.sound(p, Compat.sound("apollyon:small_jumpscare", "minecraft:entity.warden.sonic_boom"), SoundSource.HOSTILE, at, 0.8f, 1.2f);
                    Integer id = r.ids.get(p.getUUID());
                    if (id != null && id == ev.a()) {
                        say(p, SPOTTED[r.lvl.getRandom().nextInt(SPOTTED.length)], ChatFormatting.DARK_RED);
                        vn.cavemind.net.Net.fx(p, vn.cavemind.net.Net.SHAKE, 1.2f, 12);
                    }
                }
            }
            case "listen" -> {
                for (ServerPlayer p : ps) {
                    if (p.position().distanceTo(at) > 16) continue;
                    Fx.sound(p, Compat.sound("apollyon:shallow_breathing", "minecraft:entity.horse.breathe"), SoundSource.HOSTILE, at, 0.55f, 0.6f);
                    Fx.sound(p, "minecraft:block.wooden_door.close", SoundSource.HOSTILE, at, 0.08f, 0.4f);
                    Integer id = r.ids.get(p.getUUID());
                    Round.Runner rr = id == null ? null : r.core.runner(id);
                    if (rr != null && rr.closet == ev.a()) say(p, "Nó đứng ngay ngoài cửa. Ngồi thụp xuống để nín thở.", ChatFormatting.DARK_RED);
                }
            }
            case "pass" -> {
                for (ServerPlayer p : ps) if (p.position().distanceTo(at) < 14) Fx.sound(p, "man:short_whisper_1", SoundSource.HOSTILE, at, 0.3f, 0.6f);
            }
            case "lunge" -> {
                for (ServerPlayer p : ps) if (p.position().distanceTo(at) < 24) Fx.sound(p, Compat.sound("apollyon:damage_noise", "minecraft:entity.warden.roar"), SoundSource.HOSTILE, at, 0.8f, 0.7f);
            }
            case "cry" -> {
                int i = ev.a();
                boolean first = r.core.cryAt == r.core.t;
                net.minecraft.util.RandomSource rnd = r.lvl.getRandom();
                for (ServerPlayer p : ps) {
                    double d = p.position().distanceTo(at);
                    if (d > 56) continue;
                    Fx.sound(p, vn.cavemind.world.Sounds.SOB, SoundSource.HOSTILE, at, 2.4f, 1.0f + rnd.nextFloat() * 0.08f);
                    Fx.sound(p, Compat.sound(TOY_CALL[i], "minecraft:block.amethyst_block.chime"), SoundSource.HOSTILE, at, 1.6f, 1.0f);
                    if (first) say(p, "Ở đâu đó, " + TOY_NAME[i] + " đang khóc thút thít. Nhặt nó lên trước khi nó tới dỗ.", ChatFormatting.GRAY);
                }
            }
            case "cry_call" -> {
                for (ServerPlayer p : ps) {
                    say(p, "Không ai dỗ " + TOY_NAME[ev.a()] + ". Tiếng khóc gọi nó tới đó rồi.", ChatFormatting.DARK_RED);
                    Fx.sound(p, Compat.sound("apollyon:deep_laugh", "minecraft:entity.warden.nearby_closer"), SoundSource.HOSTILE, damnedAt(r, at), 0.5f, 1.25f);
                }
            }
            case "betray" -> {
                for (ServerPlayer p : ps) {
                    Integer id = r.ids.get(p.getUUID());
                    boolean me = id != null && id == ev.a();
                    if (p.position().distanceTo(at) < 40) Fx.sound(p, Compat.sound("horror_element_mod:kids_laughing", "minecraft:entity.allay.ambient_without_item"), SoundSource.HOSTILE, at, 1.6f, 0.9f);
                    if (me) {
                        say(p, "…cậu ấy ở đằng kia kìa… món đồ chơi chỉ về phía bạn.", ChatFormatting.DARK_RED);
                        vn.cavemind.net.Net.fx(p, vn.cavemind.net.Net.SHAKE, 0.7f, 10);
                    } else if (ev.a() >= 0) say(p, "Món đồ chơi vừa chỉ chỗ một người.", ChatFormatting.DARK_GRAY);
                }
            }
            case "ink" -> {
                Ink k = r.ink.computeIfAbsent(ev.a(), x -> new Ink());
                seep(r, k, ev.x(), ev.z());
                k.next = now;
            }
            case "ink_clear" -> clearInk(r, ev.a());
            case "cough" -> {
                for (ServerPlayer p : ps) {
                    if (p.position().distanceTo(at) > 40) continue;
                    Integer id = r.ids.get(p.getUUID());
                    boolean me = id != null && id == ev.a();
                    Fx.sound(p, vn.cavemind.world.Sounds.COUGH, SoundSource.PLAYERS, at, me ? 1.0f : 1.8f, 0.95f + r.lvl.getRandom().nextFloat() * 0.1f);
                    if (me) {
                        say(p, "Bạn sặc khí lạnh và ho lên một tiếng. Nó nghe thấy rồi.", ChatFormatting.DARK_RED);
                        vn.cavemind.net.Net.fx(p, vn.cavemind.net.Net.MIST, 1f, 90);
                    }
                }
            }
            case "rip" -> {
                for (ServerPlayer p : ps) {
                    if (p.position().distanceTo(at) > 24) continue;
                    Fx.sound(p, "minecraft:entity.zombie.break_wooden_door", SoundSource.HOSTILE, at, 1.0f, 0.7f);
                    Fx.sound(p, Compat.sound("apollyon:small_jumpscare", "minecraft:entity.warden.roar"), SoundSource.HOSTILE, at, 0.7f, 1.3f);
                    if (p.position().distanceTo(at) < 4) vn.cavemind.net.Net.fx(p, vn.cavemind.net.Net.SHAKE, 1.4f, 14);
                }
            }
            case "gasp" -> {
                for (ServerPlayer p : ps) {
                    if (p.position().distanceTo(at) > 14) continue;
                    Integer id = r.ids.get(p.getUUID());
                    boolean me = id != null && id == ev.a();
                    Fx.sound(p, Compat.sound("apollyon:shallow_breathing", "minecraft:entity.player.hurt_drown"), SoundSource.PLAYERS, at, me ? 0.9f : 0.5f, 1.35f);
                    if (me) say(p, "Bạn thở hắt ra. Nó nghe thấy rồi.", ChatFormatting.DARK_RED);
                }
            }
            default -> {}
        }
    }

    static void finish(MinecraftServer server, Run r, List<ServerPlayer> ps) {
        Round.End end = r.core.end;
        if (end == Round.End.CAUGHT) {
            ServerPlayer lost = null;
            for (ServerPlayer p : ps) if (r.ids.get(p.getUUID()) == r.core.caughtId) lost = p;
            if (lost != null) lose(r, lost);
            boolean others = false;
            for (ServerPlayer p : ps) if (p != lost && active(p)) others = true;
            if (others) {
                r.core.end = Round.End.NONE;
                r.core.brain.stun(80);
                String name = lost == null ? "Một người" : lost.getGameProfile().getName();
                for (ServerPlayer p : ps) if (p != lost && active(p)) say(p, name + " bị bắt rồi. Nó đang tìm người tiếp theo.", ChatFormatting.DARK_RED);
                return;
            }
            r.done = true;
            return;
        }
        for (ServerPlayer p : ps) {
            switch (end) {
                case WIN_TOYS, WIN_TIME -> win(r, p, end == Round.End.WIN_TOYS);
                default -> {
                    say(p, "Nó chán rồi.", ChatFormatting.GRAY);
                    leave(r, p);
                }
            }
        }
        r.done = true;
    }

    static void win(Run r, ServerPlayer p, boolean toys) {
        Profile pr = MindData.profile(p);
        ServerLevel lvl = p.serverLevel();
        pr.dMazeWins++;
        if (toys) pr.dMazeToys++;
        if (r.mode == Mode.PUNISH) pr.dDebt = 0;
        Fx.sound(p, "minecraft:block.bell.resonate", SoundSource.AMBIENT, p.position(), 0.6f, 0.7f);
        title(p, toys ? "Nó được về nhà" : "Hết giờ chơi", toys ? "Bảy món đồ chơi đã về đủ." : "Chuông tự vang. Nó để bạn đi.");
        leave(r, p);
        Compat.grant(p, "cavemind:ban_suong/co_nguoi_choi_cung");
        Compat.grant(p, "cavemind:ban_suong/tron_dung_luat");
        if (toys) Compat.grant(p, "cavemind:ban_suong/bay_mon_do_choi");
        if (r.mode == Mode.PLAY) {
            double chance = r.crimson ? 0.8 : vn.cavemind.Cfg.RARE_CHANCE.get() * (toys ? 1.0 : 0.6);
            if (p.getRandom().nextDouble() < chance) {
                vn.cavemind.chapter.Gifts.give(p, vn.cavemind.chapter.Gifts.rare(p, pr.chapter, "đứa trẻ thứ tám"));
                p.displayClientMessage(Component.literal(r.crimson ? "Dưới ánh Trăng Thẫm, nó để lại một món quà hiếm." : "Nó để lại một món quà hiếm.").withStyle(ChatFormatting.GOLD), false);
            } else vn.cavemind.chapter.Gifts.give(p, vn.cavemind.chapter.Gifts.small(p, pr.chapter));
        }
        pr.moment(new Moment(Moment.MAZE, Dread.day(lvl), Dread.time(lvl), p.blockPosition(), toys ? 2 : 1, r.mode.ordinal()));
        vn.cavemind.life.Child.played(p.server, 70);
        PaceLog.encounter(p, "damned", "maze win", toys ? "toys" : "time");
    }

    static void lose(Run r, ServerPlayer p) {
        Profile pr = MindData.profile(p);
        pr.moment(new Moment(Moment.MAZE, Dread.day(p.serverLevel()), Dread.time(p.serverLevel()), p.blockPosition(), 0, r.mode.ordinal()));
        Entity e = r.body == null ? null : r.lvl.getEntity(r.body);
        if (e != null) Motion.play(e, "animation.theDamned.jumpscare", 40, p.server.getTickCount());
        Fx.sound(p, Compat.sound("apollyon:jumpscare_sound", "minecraft:entity.warden.roar"), SoundSource.HOSTILE, p.position(), 1.0f, 1.0f);
        vn.cavemind.net.Net.fx(p, vn.cavemind.net.Net.SHAKE, 2.4f, 20);
        PaceLog.encounter(p, "damned", "maze lost", r.mode.name());
        writeName(p);
        boolean peaceful = p.serverLevel().getDifficulty() == net.minecraft.world.Difficulty.PEACEFUL;
        if (r.mode == Mode.PUNISH && !peaceful) {
            title(p, "Ai thua thì đếm", "");
            DIED.add(p.getUUID());
            WHO.remove(p.getUUID());
            Round.Runner rn = r.core.runner(r.ids.get(p.getUUID()));
            if (rn != null) rn.out = true;
            stripToys(p);
            p.hurt(source(p), Float.MAX_VALUE);
            if (p.isAlive()) leave(r, p);
            return;
        }
        title(p, "Bạn thua", "Bạn bị bỏ lại ở giờ thứ bốn mươi tám một lúc.");
        leave(r, p);
    }

    static DamageSource source(ServerPlayer p) {
        ResourceKey<DamageType> key = ResourceKey.create(Registries.DAMAGE_TYPE, new ResourceLocation(CaveMind.MODID, "gio48"));
        var reg = p.level().registryAccess().registryOrThrow(Registries.DAMAGE_TYPE);
        return reg.getHolder(key).map(h -> new DamageSource((Holder<DamageType>) h)).orElse(p.damageSources().fellOutOfWorld());
    }

    static void writeName(ServerPlayer p) {
        ServerLevel ow = p.server.overworld();
        BlockPos s = vn.cavemind.lore.Lore.part(ow, "p", "name8");
        if (s == null || !ow.isLoaded(s) || !(ow.getBlockEntity(s) instanceof SignBlockEntity sb)) return;
        SignText t = sb.getFrontText().setMessage(1, Component.literal(p.getGameProfile().getName()).withStyle(s2 -> s2.withColor(0x5a0f0f)));
        sb.setText(t, true);
        sb.setChanged();
    }

    static void leave(Run r, ServerPlayer p) {
        Ret ret = r.back.get(p.getUUID());
        WHO.remove(p.getUUID());
        Integer id = r.ids.get(p.getUUID());
        Round.Runner rn = id == null ? null : r.core.runner(id);
        if (rn != null) rn.out = true;
        stripToys(p);
        home(p, ret);
    }

    static void home(ServerPlayer p, Ret ret) {
        Cam.exit(p);
        MinecraftServer s = p.server;
        ServerLevel dst = ret == null ? s.overworld() : s.getLevel(ret.dim());
        if (dst == null || dst.dimension() == DIM) dst = s.overworld();
        Vec3 at;
        if (ret != null && ret.pos() != null && ret.dim() != DIM) at = ret.pos();
        else {
            Profile pr = MindData.profile(p);
            BlockPos b = pr.mazeRet != null ? pr.mazeRet : p.getRespawnPosition() != null ? p.getRespawnPosition() : dst.getSharedSpawnPos();
            at = Vec3.atBottomCenterOf(b).add(0, 1, 0);
        }
        p.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 40, 0, false, false));
        p.fallDistance = 0;
        p.teleportTo(dst, at.x, at.y, at.z, ret == null ? p.getYRot() : ret.yaw(), 0f);
        p.fallDistance = 0;
        Bag.restore(p);
    }

    static void out(ServerPlayer p, String why) {
        Cam.exit(p);
        Run r = run(p);
        WHO.remove(p.getUUID());
        if (!inMaze(p) && p.isAlive()) Bag.restore(p);
        if (r == null) return;
        Integer id = r.ids.get(p.getUUID());
        Round.Runner rn = id == null ? null : r.core.runner(id);
        if (rn != null) rn.out = true;
        stripToys(p);
        CaveMind.debug("{} left the maze ({})", p.getGameProfile().getName(), why);
    }

    static void recover(ServerPlayer p) {
        Profile pr = MindData.profile(p);
        pr.dAbandon++;
        if (pr.dAbandon >= 2) pr.dDebt = Math.min(3, pr.dDebt + 1);
        stripToys(p);
        home(p, null);
        say(p, "Bạn tỉnh dậy cạnh miệng giếng. Ván chơi bị bỏ dở.", ChatFormatting.GRAY);
    }

    static void end(Run r, ServerPlayer p, Round.End why) {
        say(p, "Nó chán rồi.", ChatFormatting.GRAY);
        leave(r, p);
    }

    public static void logout(ServerPlayer p) {
        if (active(p)) out(p, "thoát game");
    }

    public static void login(ServerPlayer p) {
        if (inMaze(p) && !active(p)) recover(p);
    }

    public static void attacked(ServerPlayer p, Entity e) {
        Run r = run(p);
        if (r == null || !e.getUUID().equals(r.body)) return;
        r.core.brain.stun(20);
        Fx.soundAll(r.lvl, "apollyon:damage_noise", SoundSource.HOSTILE, e.position(), 0.9f, 1.5f);
    }

    public static boolean pickup(ServerPlayer p, ItemEntity it) {
        ItemStack st = it.getItem();
        if (st.hasTag() && st.getTag().contains(BATTERY)) {
            it.discard();
            Cam.battery(p);
            return true;
        }
        if (!st.hasTag() || !st.getTag().contains(TOY)) return false;
        Run r = run(p);
        if (r == null || st.getTag().getInt(RUN) != r.slot) return false;
        Round.Runner rn = r.core.runner(r.ids.get(p.getUUID()));
        int idx = st.getTag().getInt(TOY);
        if (rn != null && r.core.takeToy(rn, idx)) {
            Fx.soundAll(r.lvl, "minecraft:block.amethyst_block.chime", SoundSource.PLAYERS, p.position(), 0.8f, 1.2f + idx * 0.05f);
            say(p, "Món đồ chơi thứ " + (7 - r.core.remainingToys()) + " trên bảy. Mang về rương ở phòng giếng rồi rung chuông.", ChatFormatting.GRAY);
        }
        return false;
    }

    static void stripToys(ServerPlayer p) {
        var inv = p.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack st = inv.getItem(i);
            if (st.hasTag() && st.getTag().contains(TOY)) inv.setItem(i, ItemStack.EMPTY);
        }
    }

    public static void died(ServerPlayer p) {
        if (inMaze(p)) DIED.add(p.getUUID());
        if (active(p)) out(p, "chết");
    }

    public static boolean keepDrops(ServerPlayer p, java.util.Collection<ItemEntity> drops) {
        if (!DIED.contains(p.getUUID())) return false;
        List<ItemStack> keep = KEEP.computeIfAbsent(p.getUUID(), k -> new ArrayList<>());
        for (ItemEntity ie : drops) {
            ItemStack st = ie.getItem();
            if (st.hasTag() && st.getTag().contains(TOY) || Bag.kit(st)) continue;
            keep.add(st.copy());
        }
        return true;
    }

    public static boolean keepXp(ServerPlayer p) {
        return DIED.contains(p.getUUID());
    }

    public static void clone(ServerPlayer old, ServerPlayer neu) {
        if (!DIED.remove(old.getUUID())) {
            if (Bag.holding(neu) && !inMaze(neu)) Bag.restore(neu);
            return;
        }
        List<ItemStack> keep = KEEP.remove(old.getUUID());
        if (keep != null) for (ItemStack st : keep) if (!neu.getInventory().add(st)) neu.drop(st, false);
        if (!inMaze(neu)) Bag.restore(neu);
        neu.experienceLevel = old.experienceLevel;
        neu.experienceProgress = old.experienceProgress;
        neu.totalExperience = old.totalExperience;
        neu.displayClientMessage(Component.literal("Đồ của bạn vẫn còn. Nó chỉ lấy thời gian của bạn.").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC), true);
    }

    static void cleanup(Run r) {
        for (Integer id : new ArrayList<>(r.ink.keySet())) clearInk(r, id);
        if (r.body != null) {
            Entity e = r.lvl.getEntity(r.body);
            if (e != null) e.discard();
        }
        for (UUID t : r.toys) {
            if (t == null) continue;
            Entity e = r.lvl.getEntity(t);
            if (e != null) e.discard();
        }
        hangers(r);
        for (Entity e : r.lvl.getEntitiesOfClass(ItemEntity.class, new net.minecraft.world.phys.AABB(r.ox - 4, F - 4, r.oz - 4, r.ox + Grid.S + 4, F + 8, r.oz + Grid.S + 4))) e.discard();
        for (UUID u : new ArrayList<>(r.ids.keySet())) if (WHO.getOrDefault(u, -1) == r.slot) WHO.remove(u);
        unforce(r);
        CaveMind.debug("maze slot {} closed", r.slot);
    }

    static void spawnBody(Run r) {
        EntityType<?> type = Compat.type(Compat.DAMNED);
        if (type == null) return;
        Entity e = type.create(r.lvl);
        if (e == null) return;
        Brain b = r.core.brain;
        Vec3 at = r.world(b.x, b.z, F + 1);
        e.moveTo(at.x, at.y, at.z, (float) b.yaw, 0);
        Compat.mark(e, "damned_maze", null, false);
        e.noPhysics = true;
        e.setNoGravity(true);
        e.setInvulnerable(true);
        if (e instanceof Mob m) {
            m.goalSelector.removeAllGoals(x -> true);
            m.targetSelector.removeAllGoals(x -> true);
            m.setPersistenceRequired();
            m.setSilent(true);
        }
        if (r.lvl.addFreshEntity(e)) r.body = e.getUUID();
        r.shown = null;
    }

    static void spawnToys(Run r) {
        String[][] spec = {
            {"Nơ đỏ của Linh", "Buộc tóc, màu đỏ đã bạc.", "Sổ điểm danh", "Lớp một, giếng số bốn.\n\nLinh\nTuấn\nMai\nBảo\nHà\nNam\nKhoa\n\nTên thứ tám bị nước làm nhòe hết."},
            {"Bi ve của Tuấn", "Ba viên bi, một viên nứt.", "Vạch phấn", "IIIII IIIII IIIII IIIII\nIIIII IIIII IIIII IIIII\nIIIII III\n\nBốn mươi tám vạch. Không có vạch thứ bốn mươi chín."},
            {"Hạc giấy của Mai", "Gấp vội, cánh lệch.", "Nhật ký của Mai", "Hôm nay chúng con về sớm.\n\nĐứa thứ tám vẫn trốn. Không ai gọi nó về.\n\nMai xin lỗi."},
            {"Con quay gỗ của Bảo", "Dây quấn còn mới.", "Thông báo", "Từ hôm nay giếng thông gió số bốn đóng lại.\n\nTrẻ con không được lại gần.\n\nAi nghe tiếng đếm dưới giếng thì báo cho người lớn."},
            {"Kẹp tóc của Hà", "Rơi trong tủ.", "Thư của mẹ", "Con ơi,\n\nnếu nó tới thì con cầm bông hồng sống. Nó sẽ nhớ ra là nó buồn ngủ.\n\nĐừng bỏ nó lại một mình như người ta đã làm."},
            {"Nỏ thun của Nam", "Chạc gỗ, dây cao su đã giòn.", "Bức vẽ sáp màu", "Tám đứa trẻ đứng thành hàng.\n\nĐứa thứ tám không có mặt, chỉ có một vòng tròn đen."},
            {"Kèn harmonica của Khoa", "Thổi vào không ra tiếng.", "Trang cuối", "…bốn mươi bảy, bốn mươi tám.\n\n…bốn mươi bảy, bốn mươi tám.\n\n…bốn mươi bảy, bốn mươi tám."}
        };
        Item[] items = {Items.RED_DYE, Items.AMETHYST_SHARD, Items.PAPER, Items.BOWL, Items.IRON_NUGGET, Items.TRIPWIRE_HOOK, Items.IRON_INGOT};
        for (int i = 0; i < 7; i++) {
            double[] t = r.toyAt[i] != null ? r.toyAt[i] : r.core.g.toys[i];
            ItemStack st = new ItemStack(items[i]);
            st.setHoverName(Component.literal(spec[i][0]).withStyle(s -> s.withItalic(false).withColor(0xd8c8a8)));
            CompoundTag disp = st.getOrCreateTagElement("display");
            ListTag l = new ListTag();
            l.add(StringTag.valueOf(Component.Serializer.toJson(Component.literal(spec[i][1]).withStyle(s -> s.withItalic(true).withColor(0x8a8a8a)))));
            l.add(StringTag.valueOf(Component.Serializer.toJson(Component.literal("Đồ chơi thứ " + (i + 1) + " trên bảy").withStyle(s -> s.withItalic(true).withColor(0x6a5a4a)))));
            disp.put("Lore", l);
            st.getOrCreateTag().putInt(TOY, i);
            st.getOrCreateTag().putInt(RUN, r.slot);
            Vec3 at = r.world(t[0], t[1], r.toyY[i]);
            ItemEntity ie = new ItemEntity(r.lvl, at.x, at.y, at.z, st, 0, 0, 0);
            ie.setUnlimitedLifetime();
            ie.setPickUpDelay(0);
            if (r.lvl.addFreshEntity(ie)) r.toys[i] = ie.getUUID();
            ItemStack book = new ItemStack(Items.WRITTEN_BOOK);
            CompoundTag bt = book.getOrCreateTag();
            bt.putString("title", spec[i][2]);
            bt.putString("author", "...");
            bt.putInt("generation", 3);
            ListTag pages = new ListTag();
            pages.add(StringTag.valueOf(Component.Serializer.toJson(Component.literal(spec[i][3]))));
            bt.put("pages", pages);
            bt.putBoolean("resolved", true);
            bt.putBoolean("cavemind", true);
            ItemEntity be = new ItemEntity(r.lvl, at.x + 0.3, at.y, at.z - 0.3, book, 0, 0, 0);
            be.setUnlimitedLifetime();
            r.lvl.addFreshEntity(be);
        }
    }

    static void build(Run r) {
        Grid g = r.core.g;
        ServerLevel lvl = r.lvl;
        BlockState air = Blocks.AIR.defaultBlockState(), base = Blocks.DEEPSLATE_TILES.defaultBlockState(), under = Blocks.DEEPSLATE.defaultBlockState();
        BlockState wall = Blocks.DARK_OAK_PLANKS.defaultBlockState(), post = Blocks.STRIPPED_DARK_OAK_LOG.defaultBlockState(), roof = Blocks.DARK_OAK_PLANKS.defaultBlockState();
        BlockState fence = Blocks.DARK_OAK_FENCE.defaultBlockState(), pillar = Blocks.SPRUCE_PLANKS.defaultBlockState();
        boolean[][] vent = new boolean[Grid.S][Grid.S];
        for (Grid.Vent v : g.vents) {
            int n = (int) Math.ceil(v.len * 2) + 1;
            for (int i = 0; i <= n; i++) {
                double t = i / (double) n;
                int x = (int) Math.floor(v.ax + 0.5 + (v.bx - v.ax) * t), z = (int) Math.floor(v.az + 0.5 + (v.bz - v.az) * t);
                vent[x][z] = true;
            }
        }
        for (int x = -2; x <= Grid.S + 1; x++) {
            for (int z = -2; z <= Grid.S + 1; z++) {
                boolean in = x >= 0 && z >= 0 && x < Grid.S && z < Grid.S;
                for (int y = F - 3; y <= F + 6; y++) {
                    BlockState st;
                    if (!in) st = y >= F - 2 && y <= F + 5 ? Blocks.DEEPSLATE.defaultBlockState() : air;
                    else if (y <= F - 2) st = base;
                    else if (y == F - 1) st = vent[x][z] ? air : under;
                    else if (y == F) st = floor(r, x, z);
                    else if (y == F + 5) st = roof;
                    else st = column(g, x, z, y - F, wall, post, fence, pillar, air);
                    lvl.setBlock(r.at(x, y, z), st, 2 | 16);
                }
            }
        }
        for (Grid.Closet c : g.closets) {
            Direction face = Direction.from2DDataValue(new int[]{3, 0, 1, 2}[c.face]);
            BlockState d0 = switch (c.kind) {
                case Grid.LOCKER -> vn.cavemind.world.Assets.door("mcwdoors:metal_windowed_door", Blocks.DARK_OAK_DOOR.defaultBlockState());
                case Grid.STALL -> vn.cavemind.world.Assets.door("mcwdoors:birch_cottage_door", Blocks.BIRCH_DOOR.defaultBlockState());
                case Grid.CUPBOARD -> vn.cavemind.world.Assets.door("mcwdoors:dark_oak_barn_door", Blocks.DARK_OAK_DOOR.defaultBlockState());
                default -> vn.cavemind.world.Assets.door("mcwdoors:spruce_four_panel_door", Blocks.SPRUCE_DOOR.defaultBlockState());
            };
            BlockState door = d0.setValue(DoorBlock.FACING, face.getOpposite()).setValue(DoorBlock.HINGE, DoorHingeSide.LEFT).setValue(DoorBlock.OPEN, false);
            if (door.hasProperty(DoorBlock.POWERED)) door = door.setValue(DoorBlock.POWERED, false);
            BlockState side = switch (c.kind) {
                case Grid.LOCKER -> Blocks.LIGHT_GRAY_TERRACOTTA.defaultBlockState();
                case Grid.STALL -> Blocks.WHITE_TERRACOTTA.defaultBlockState();
                case Grid.CUPBOARD -> Blocks.BOOKSHELF.defaultBlockState();
                default -> Blocks.STRIPPED_DARK_OAK_LOG.defaultBlockState();
            };
            lvl.setBlock(r.at(c.x, F + 1, c.z), door.setValue(DoorBlock.HALF, DoubleBlockHalf.LOWER), 2 | 16);
            lvl.setBlock(r.at(c.x, F + 2, c.z), door.setValue(DoorBlock.HALF, DoubleBlockHalf.UPPER), 2 | 16);
            lvl.setBlock(r.at(c.x, F + 3, c.z), pillar, 2 | 16);
            lvl.setBlock(r.at(c.x, F + 4, c.z), pillar, 2 | 16);
            int k = (c.face + 2) % 4, px = new int[]{1, 0, -1, 0}[(k + 1) % 4], pz = new int[]{0, 1, 0, -1}[(k + 1) % 4];
            for (int sgn : new int[]{1, -1}) {
                for (int h = 1; h <= 3; h++) lvl.setBlock(r.at(c.x + px * sgn, F + h, c.z + pz * sgn), side, 2 | 16);
            }
        }
        for (Grid.Nook n : g.nooks) {
            Direction out = Direction.from2DDataValue(new int[]{3, 0, 1, 2}[n.face]);
            int k = (n.face + 2) % 4, px = new int[]{1, 0, -1, 0}[(k + 1) % 4], pz = new int[]{0, 1, 0, -1}[(k + 1) % 4];
            BlockState shelf = vn.cavemind.world.Assets.face(vn.cavemind.world.Assets.pick(Blocks.BOOKSHELF.defaultBlockState(), "dustydecorations:cluttered_shelf", "dustydecorations:small_bookshelf"), out);
            for (int sgn : new int[]{1, -1}) {
                int x = n.x + px * sgn, z = n.z + pz * sgn;
                if (x < 0 || z < 0 || x >= Grid.S || z >= Grid.S || g.tile[x][z] != Grid.PILLAR) continue;
                lvl.setBlock(r.at(x, F + 1, z), Blocks.BOOKSHELF.defaultBlockState(), 2 | 16);
                lvl.setBlock(r.at(x, F + 2, z), shelf, 2 | 16);
                lvl.setBlock(r.at(x, F + 3, z), Blocks.DARK_OAK_PLANKS.defaultBlockState(), 2 | 16);
            }
            lvl.setBlock(r.at(n.x, F + 3, n.z), Blocks.COBWEB.defaultBlockState(), 2 | 16);
            lvl.setBlock(r.at(n.x, F, n.z), Blocks.BLACK_CONCRETE_POWDER.defaultBlockState(), 2 | 16);
        }
        for (int x = 0; x < Grid.S; x++) {
            for (int z = 0; z < Grid.S; z++) {
                if (!g.lamp[x][z]) continue;
                int src = 0;
                for (int dx = -1; dx <= 1; dx++) for (int dz = -1; dz <= 1; dz++) {
                    int qx = x + dx, qz = z + dz;
                    if (qx >= 0 && qz >= 0 && qx < Grid.S && qz < Grid.S) src = Math.max(src, g.light[qx][qz]);
                }
                int lv = Math.max(1, Math.min(4, src));
                BlockState bulb = vn.cavemind.world.Assets.get("horror_element_mod:broken_light", Blocks.LIGHTNING_ROD.defaultBlockState().setValue(net.minecraft.world.level.block.LightningRodBlock.FACING, Direction.DOWN));
                lvl.setBlock(r.at(x, F + 4, z), bulb, 2 | 16);
                lvl.setBlock(r.at(x, F + 3, z), Blocks.LIGHT.defaultBlockState().setValue(LightBlock.LEVEL, lv), 2 | 16);
                r.lamps.add(r.at(x, F + 3, z));
                r.lampLv.add(lv);
            }
        }
        rooms(r);
        Decor.dress(r);
    }

    static BlockState floor(Run r, int x, int z) {
        Grid g = r.core.g;
        if (g.grate[x][z]) return Blocks.IRON_TRAPDOOR.defaultBlockState().setValue(TrapDoorBlock.HALF, Half.TOP).setValue(TrapDoorBlock.OPEN, false);
        int c = Grid.cellAt(x + 0.5, z + 0.5);
        int room = g.tile[x][z] == Grid.FLOOR || g.tile[x][z] == Grid.CLOSET ? g.room[c] : -1;
        return switch (room) {
            case Grid.WELL -> Blocks.STONE_BRICKS.defaultBlockState();
            case Grid.CLASS -> Blocks.OAK_PLANKS.defaultBlockState();
            case Grid.DORM -> Blocks.SPRUCE_PLANKS.defaultBlockState();
            case Grid.STORE -> Blocks.COBBLESTONE.defaultBlockState();
            case Grid.BATH -> Blocks.WHITE_TERRACOTTA.defaultBlockState();
            default -> Blocks.DARK_OAK_PLANKS.defaultBlockState();
        };
    }

    static BlockState column(Grid g, int x, int z, int h, BlockState wall, BlockState post, BlockState fence, BlockState pillar, BlockState air) {
        byte t = g.tile[x][z];
        return switch (t) {
            case Grid.WALL -> x % Grid.P == 0 && z % Grid.P == 0 ? post : wall;
            case Grid.PILLAR -> pillar;
            case Grid.FENCE -> h <= 3 ? fence : wall;
            case Grid.CLOSET, Grid.NOOK -> h >= 3 ? wall : air;
            default -> air;
        };
    }

    static void rooms(Run r) {
        Grid g = r.core.g;
        ServerLevel lvl = r.lvl;
        int wx = g.rooms[Grid.WELL][0] * Grid.P, wz = g.rooms[Grid.WELL][1] * Grid.P;
        lvl.setBlock(r.at((int) g.chest[0], F + 1, (int) g.chest[1]), Blocks.CHEST.defaultBlockState(), 2);
        int bx0 = (int) g.bell[0], bz0 = (int) g.bell[1];
        // the bronze bell hangs on a rusty chain from a rotten wooden frame
        BlockState post = Blocks.DARK_OAK_FENCE.defaultBlockState();
        for (int h = 1; h <= 3; h++) {
            lvl.setBlock(r.at(bx0, F + h, bz0 - 1), post, 2);
            lvl.setBlock(r.at(bx0, F + h, bz0 + 1), post, 2);
        }
        for (int dz = -1; dz <= 1; dz++) lvl.setBlock(r.at(bx0, F + 4, bz0 + dz), Blocks.STRIPPED_DARK_OAK_LOG.defaultBlockState().setValue(net.minecraft.world.level.block.RotatedPillarBlock.AXIS, Direction.Axis.Z), 2);
        lvl.setBlock(r.at(bx0, F + 3, bz0), Blocks.CHAIN.defaultBlockState(), 2);
        BlockPos bp = r.at(bx0, F + 2, bz0);
        lvl.setBlock(bp, Blocks.BELL.defaultBlockState().setValue(BellBlock.FACING, Direction.EAST).setValue(BellBlock.ATTACHMENT, BellAttachType.CEILING), 2);
        String[] rules = {"Nó đếm 48 nhịp.|Đi trốn đi.", "Đừng chạy.|Nó nghe|tiếng chân.", "Đừng mang lửa.|Nó thấy|ánh sáng.", "Trong tủ thì|ngồi thụp|và nín thở.",
            "Đừng nhìn nó.|Nhìn là|bị thấy.", "Tìm đủ bảy món|đồ chơi thì|nó được về nhà."};
        int placed = 0;
        for (int x = wx + 1; x <= wx + 11 && placed < rules.length; x += 2) {
            BlockPos q = r.at(x, F + 1, wz + 11);
            if (g.tile[x][wz + 12] == Grid.FLOOR) continue;
            BlockPos at = lvl.getBlockState(q.above()).isAir() ? q.above() : q;
            lvl.setBlock(at, vn.cavemind.gear.Scrawl.of(Direction.NORTH, vn.cavemind.gear.Scrawl.RULES + placed++), 2);
        }
        String[] names = {"Linh", "Tuấn", "Mai", "Bảo", "Hà", "Nam", "Khoa", ""};
        int n = 0;
        for (int x = wx + 1; x <= wx + 11 && n < names.length; x++) {
            if (g.tile[x][wz] == Grid.FLOOR) continue;
            BlockPos q = r.at(x, F + 2, wz + 1);
            if (!lvl.getBlockState(q).isAir()) continue;
            lvl.setBlock(q, vn.cavemind.gear.Scrawl.of(Direction.SOUTH, n == names.length - 1 ? vn.cavemind.gear.Scrawl.EIGHTH : vn.cavemind.gear.Scrawl.NAMES + n), 2);
            n++;
        }
        for (int id = 1; id < 4; id++) {
            if (id >= g.rooms.length) break;
            int bx = g.rooms[id][0] * Grid.P, bz = g.rooms[id][1] * Grid.P;
            for (int x = bx + 2; x <= bx + 10; x += 2) {
                for (int z = bz + 3; z <= bz + 9; z += 3) {
                    if (g.tile[x][z] != Grid.FLOOR || g.grate[x][z]) continue;
                    BlockState carpet = switch (id) {
                        case Grid.CLASS -> Blocks.BROWN_CARPET.defaultBlockState();
                        case Grid.DORM -> Blocks.WHITE_CARPET.defaultBlockState();
                        default -> Blocks.RAIL.defaultBlockState();
                    };
                    lvl.setBlock(r.at(x, F + 1, z), carpet, 2);
                }
            }
        }
        int cx = g.rooms[Grid.CLASS][0] * Grid.P, cz = g.rooms[Grid.CLASS][1] * Grid.P;
        for (int x = cx + 4; x <= cx + 8; x++) {
            if (g.tile[x][cz] == Grid.FLOOR) continue;
            lvl.setBlock(r.at(x, F + 2, cz), Blocks.BLACK_CONCRETE.defaultBlockState(), 2);
            lvl.setBlock(r.at(x, F + 3, cz), Blocks.BLACK_CONCRETE.defaultBlockState(), 2);
        }
        BlockPos board = r.at(cx + 6, F + 2, cz + 1);
        if (lvl.getBlockState(board).isAir()) lvl.setBlock(board, vn.cavemind.gear.Scrawl.of(Direction.SOUTH, vn.cavemind.gear.Scrawl.BOARD), 2);
        BlockPos tally = r.at(cx + 2, F + 2, cz + 1);
        if (lvl.getBlockState(tally).isAir() && g.tile[cx + 2][cz] != Grid.FLOOR) lvl.setBlock(tally, vn.cavemind.gear.Scrawl.of(Direction.SOUTH, vn.cavemind.gear.Scrawl.TALLY), 2);
        int sx = g.rooms[Grid.STORE][0] * Grid.P, sz = g.rooms[Grid.STORE][1] * Grid.P;
        for (int x = sx + 2; x <= sx + 10; x += 4) lvl.setBlock(r.at(x, F + 4, sz + 6), Blocks.CHAIN.defaultBlockState(), 2);
        Decor.furnish(r);
    }

    static void sign(ServerLevel lvl, BlockPos q, Direction facing, String[] lines) {
        lvl.setBlock(q, Blocks.SPRUCE_WALL_SIGN.defaultBlockState().setValue(WallSignBlock.FACING, facing), 2);
        if (lvl.getBlockEntity(q) instanceof SignBlockEntity sb) {
            SignText t = sb.getFrontText();
            for (int i = 0; i < 4 && i < lines.length; i++) t = t.setMessage(i, Component.literal(lines[i]));
            sb.setText(t, true);
            sb.setWaxed(true);
            sb.setChanged();
        }
    }

    public static boolean testJoin(ServerPlayer p) {
        ServerLevel lvl = level(p.server);
        if (lvl == null) return false;
        Run r = open(p.server, lvl, Mode.PLAY);
        if (r == null) return false;
        join(r, p, new Ret(Level.OVERWORLD, p.position(), p.getYRot()));
        return true;
    }

    public static String selftest(MinecraftServer server) {
        ServerLevel lvl = level(server);
        if (lvl == null) return "no gio_48 dimension";
        Run r = open(server, lvl, Mode.PLAY);
        if (r == null) return "open failed";
        net.minecraft.world.phys.AABB box = new net.minecraft.world.phys.AABB(r.ox - 4, F - 4, r.oz - 4, r.ox + Grid.S + 4, F + 8, r.oz + Grid.S + 4);
        int frames = lvl.getEntitiesOfClass(net.minecraft.world.entity.decoration.ItemFrame.class, box).size();
        int bats = lvl.getEntitiesOfClass(ItemEntity.class, box, ie -> ie.getItem().hasTag() && ie.getItem().getTag().contains(BATTERY)).size();
        int toys = lvl.getEntitiesOfClass(ItemEntity.class, box, ie -> ie.getItem().hasTag() && ie.getItem().getTag().contains(TOY)).size();
        int signs = 0, vines = 0, wires = 0;
        for (BlockPos q : BlockPos.betweenClosed(r.at(0, F + 1, 0), r.at(Grid.S - 1, F + 4, Grid.S - 1))) {
            BlockState st = lvl.getBlockState(q);
            if (st.getBlock() instanceof WallSignBlock || st.getBlock() instanceof vn.cavemind.gear.Scrawl) signs++;
            else if (st.is(Blocks.VINE) || st.is(Blocks.SCULK_VEIN)) vines++;
            else if (st.is(Blocks.REDSTONE_WIRE)) wires++;
        }
        flicker(r, 0);
        RUNS.remove(r.slot);
        vn.cavemind.world.Fx.later(lvl, 80, () -> {
            int b2 = lvl.getEntitiesOfClass(ItemEntity.class, box, ie -> ie.getItem().hasTag() && ie.getItem().getTag().contains(BATTERY)).size();
            int t2 = lvl.getEntitiesOfClass(ItemEntity.class, box, ie -> ie.getItem().hasTag() && ie.getItem().getTag().contains(TOY)).size();
            int f2 = lvl.getEntitiesOfClass(net.minecraft.world.entity.decoration.ItemFrame.class, box).size();
            vn.cavemind.CaveMind.LOG.info("[SELFTEST] maze after 4s: batteries " + b2 + " toys " + t2 + " frames " + f2 + " toy ids " + java.util.Arrays.stream(r.toys).filter(java.util.Objects::nonNull).count());
            cleanup(r);
        });
        return "origin " + r.ox + " " + F + " " + r.oz + " lamps " + r.lamps.size() + " frames " + frames + " batteries " + bats + " toys " + toys + " signs " + signs + " growth " + vines + " blood " + wires;
    }

    static void hangers(Run r) {
        for (Entity e : r.lvl.getEntitiesOfClass(net.minecraft.world.entity.decoration.HangingEntity.class, new net.minecraft.world.phys.AABB(r.ox - 4, F - 4, r.oz - 4, r.ox + Grid.S + 4, F + 8, r.oz + Grid.S + 4))) e.discard();
    }

    static void lamp(Run r, int i, int lv) {
        BlockPos q = r.lamps.get(i);
        r.lvl.setBlock(q, lv <= 0 ? Blocks.AIR.defaultBlockState() : Blocks.LIGHT.defaultBlockState().setValue(LightBlock.LEVEL, lv), 2);
    }

    static void flicker(Run r, long now) {
        if (r.lamps.isEmpty() || now % 2 != 0) return;
        net.minecraft.util.RandomSource rnd = r.lvl.getRandom();
        for (Iterator<Integer> it = r.dimmed.iterator(); it.hasNext(); ) {
            int i = it.next();
            if (rnd.nextFloat() < 0.4f) {
                lamp(r, i, r.lampLv.get(i));
                it.remove();
            }
        }
        if (rnd.nextFloat() < 0.3f) {
            int i = rnd.nextInt(r.lamps.size());
            if (r.dimmed.add(i)) lamp(r, i, 0);
        }
        Entity e = r.body == null ? null : r.lvl.getEntity(r.body);
        if (e == null) return;
        for (int i = 0; i < r.lamps.size(); i++) {
            if (r.lamps.get(i).distToCenterSqr(e.position()) > 49 || rnd.nextFloat() > 0.5f) continue;
            if (r.dimmed.add(i)) lamp(r, i, 0);
        }
    }

    static final String[] TOY_NAME = {"nơ đỏ của Linh", "bi ve của Tuấn", "hạc giấy của Mai", "con quay của Bảo", "kẹp tóc của Hà", "nỏ thun của Nam", "kèn của Khoa"};
    static final String[] TOY_CALL = {"minecraft:block.amethyst_block.chime", "minecraft:block.stone_button.click_on", "minecraft:item.book.page_turn", vn.cavemind.world.Sounds.TOP,
        "minecraft:block.amethyst_cluster.hit", vn.cavemind.world.Sounds.TWANG, vn.cavemind.world.Sounds.HARMONICA};
    static final String[] WHISPER = {"…chỗ này của tớ…", "…ra ngoài đi…", "…nó ngửi thấy rồi…", "…tớ cũng từng trốn ở đây…", "…đừng thở to thế…", "…đi đi, không là nó tới…"};

    static Vec3 damnedAt(Run r, Vec3 fallback) {
        Entity e = r.body == null ? null : r.lvl.getEntity(r.body);
        return e == null ? fallback : e.position().add(0, 1.5, 0);
    }

    // Black veins creep over the walls around someone who has stayed put too long.
    static void seep(Run r, Ink k, double x, double z) {
        Grid g = r.core.g;
        int tx = (int) Math.floor(x), tz = (int) Math.floor(z);
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                int qx = tx + dx, qz = tz + dz;
                if (!Decor.in(qx, qz) || g.tile[qx][qz] == Grid.WALL || g.tile[qx][qz] == Grid.PILLAR) continue;
                for (int d = 0; d < 4; d++) {
                    int wx = qx + Decor.DX[d], wz = qz + Decor.DZ[d];
                    if (!Decor.in(wx, wz) || g.tile[wx][wz] != Grid.WALL && g.tile[wx][wz] != Grid.PILLAR) continue;
                    for (int h = 1; h <= 3; h++) {
                        BlockPos q = r.at(qx, F + h, qz);
                        BlockState st = r.lvl.getBlockState(q);
                        var side = net.minecraft.world.level.block.PipeBlock.PROPERTY_BY_DIRECTION.get(Decor.DIR[d]);
                        if (st.isAir()) {
                            r.lvl.setBlock(q, Blocks.SCULK_VEIN.defaultBlockState().setValue(side, true), 2);
                            k.veins.add(q);
                        } else if (st.is(Blocks.SCULK_VEIN) && k.veins.contains(q)) r.lvl.setBlock(q, st.setValue(side, true), 2);
                    }
                }
            }
        }
    }

    static void clearInk(Run r, int id) {
        Ink k = r.ink.remove(id);
        if (k == null) return;
        for (BlockPos q : k.veins) if (r.lvl.getBlockState(q).is(Blocks.SCULK_VEIN)) r.lvl.setBlock(q, Blocks.AIR.defaultBlockState(), 2);
    }

    static void inkTick(Run r, List<ServerPlayer> ps, long now) {
        if (r.ink.isEmpty()) return;
        for (ServerPlayer p : ps) {
            Integer id = r.ids.get(p.getUUID());
            Ink k = id == null ? null : r.ink.get(id);
            if (k == null) continue;
            if (now % 10 == 0) r.lvl.sendParticles(net.minecraft.core.particles.ParticleTypes.SQUID_INK, p.getX(), p.getY() + 1.6, p.getZ(), 3, 0.45, 0.5, 0.45, 0.002);
            if (now < k.next) continue;
            k.next = now + 70 + r.lvl.getRandom().nextInt(40);
            say(p, WHISPER[r.lvl.getRandom().nextInt(WHISPER.length)], ChatFormatting.DARK_GRAY);
            double a = r.lvl.getRandom().nextDouble() * Math.PI * 2;
            Fx.sound(p, r.lvl.getRandom().nextBoolean() ? "man:short_whisper_1" : "man:short_whisper_2", SoundSource.HOSTILE, p.position().add(Math.cos(a) * 1.2, 1.4, Math.sin(a) * 1.2), 0.35f, 1.7f);
        }
    }

    static boolean besideWall(Grid g, double x, double z) {
        for (double ox : new double[]{-0.9, 0, 0.9}) {
            for (double oz : new double[]{-0.9, 0, 0.9}) {
                int ix = (int) Math.floor(x + ox), iz = (int) Math.floor(z + oz);
                if (Decor.in(ix, iz) && g.tile[ix][iz] == Grid.WALL) return true;
            }
        }
        return false;
    }

    // While searching it drags its nails along the plaster and knocks on the closets it brushes past.
    static void scrape(Run r, List<ServerPlayer> ps, Entity e, double mv, long now) {
        Brain b = r.core.brain;
        if (b.y < 0 || !(b.mode == Brain.Mode.SEARCH || b.mode == Brain.Mode.LAST || b.mode == Brain.Mode.SWEEP)) return;
        net.minecraft.util.RandomSource rnd = r.lvl.getRandom();
        Grid g = r.core.g;
        if (mv > 0.02 && now >= r.nextScrape && besideWall(g, b.x, b.z)) {
            r.nextScrape = now + 45 + rnd.nextInt(70);
            for (ServerPlayer p : ps) {
                double d = p.position().distanceTo(e.position());
                if (d > 24) continue;
                Fx.sound(p, vn.cavemind.world.Sounds.SCRAPE, SoundSource.HOSTILE, e.position().add(0, 1.3, 0), 0.9f, 0.8f + rnd.nextFloat() * 0.35f);
            }
        }
        for (int i = 0; i < g.closets.size(); i++) {
            Grid.Closet c = g.closets.get(i);
            if (Math.hypot(c.x + 0.5 - b.x, c.z + 0.5 - b.z) > 1.7 || now - r.tapped.getOrDefault(i, -1000L) < 160) continue;
            r.tapped.put(i, now);
            Vec3 at = Vec3.atCenterOf(r.at(c.x, F + 2, c.z));
            String knock = c.kind == Grid.LOCKER ? "minecraft:block.iron_trapdoor.close" : "minecraft:block.wood.hit";
            for (ServerPlayer p : ps) {
                if (p.position().distanceTo(at) > 20) continue;
                Fx.sound(p, knock, SoundSource.HOSTILE, at, c.kind == Grid.LOCKER ? 0.25f : 0.5f, c.kind == Grid.LOCKER ? 1.7f : 0.6f);
                Fx.later(r.lvl, 6, () -> Fx.sound(p, knock, SoundSource.HOSTILE, at, c.kind == Grid.LOCKER ? 0.18f : 0.4f, c.kind == Grid.LOCKER ? 1.8f : 0.55f));
            }
        }
    }

    static final String[] SPOTTED = {"THẤY RỒI NHÉ.", "Ở ĐÂY À.", "TỚI LƯỢT CẬU ĐẾM.", "ĐỪNG CHẠY."};
    static final String[] CALL = {"…ra đây đi…", "…mình nghe thấy cậu rồi…", "…cậu trốn giỏi quá…", "…bốn mươi tám… bốn mươi chín…", "…mình biết cậu ở gần đây…",
        "…đừng để mình một mình…", "…cậu có nghe tiếng tim mình không…", "…ai thua thì đếm…"};

    static void voice(Run r, List<ServerPlayer> ps, Entity e, long now) {
        Brain b = r.core.brain;
        if (b.mode == Brain.Mode.COUNT || b.mode == Brain.Mode.CHASE || b.y < 0 || now < r.nextVoice) return;
        net.minecraft.util.RandomSource rnd = r.lvl.getRandom();
        r.nextVoice = now + 260 + rnd.nextInt(360);
        String line = CALL[rnd.nextInt(CALL.length)];
        for (ServerPlayer p : ps) {
            double d = p.position().distanceTo(e.position());
            if (d > 18) continue;
            Fx.sound(p, rnd.nextBoolean() ? "man:short_whisper_2" : "man:short_whisper_1", SoundSource.HOSTILE, e.position().add(0, 1.5, 0), (float) Math.max(0.15, 0.6 - d / 40), 1.25f);
            if (d < 12) say(p, line, ChatFormatting.DARK_GRAY);
        }
    }

    static final String[][] AMB = {
        {"minecraft:block.pointed_dripstone.drip_water", "0.7", "0.9"}, {"minecraft:block.wooden_door.close", "0.5", "0.7"},
        {"minecraft:block.chain.step", "0.45", "0.6"}, {"apollyon:light_suspense", "0.3", "0.9"}, {"man:short_whisper_1", "0.22", "0.8"},
        {"minecraft:ambient.cave", "0.5", "0.9"}, {"minecraft:entity.zombie.attack_wooden_door", "0.18", "0.5"}, {"minecraft:block.wood.break", "0.3", "0.5"},
        {"minecraft:block.ladder.step", "0.4", "0.7"}, {"man:short_whisper_2", "0.2", "0.9"}
    };

    static void drips(Run r, List<ServerPlayer> ps, long now) {
        if (r.drips.isEmpty() || now % 7 != 0) return;
        net.minecraft.util.RandomSource rnd = r.lvl.getRandom();
        BlockPos q = r.drips.get(rnd.nextInt(r.drips.size()));
        boolean near = false;
        for (ServerPlayer p : ps) if (p.blockPosition().distSqr(q) < 24 * 24) near = true;
        if (!near) return;
        r.lvl.sendParticles(net.minecraft.core.particles.ParticleTypes.DRIPPING_WATER, q.getX() + 0.5, q.getY() + 0.05, q.getZ() + 0.5, 1, 0.08, 0, 0.08, 0);
        if (rnd.nextFloat() > 0.4f) return;
        for (ServerPlayer p : ps) {
            if (p.blockPosition().distSqr(q) < 16 * 16) Fx.sound(p, "minecraft:block.pointed_dripstone.drip_water", SoundSource.AMBIENT, Vec3.atCenterOf(q).add(0, -3, 0), 0.5f, 0.9f + rnd.nextFloat() * 0.2f);
        }
    }

    static void ambient(Run r, List<ServerPlayer> ps, long now) {
        drips(r, ps, now);
        if (now < r.nextAmbient) return;
        net.minecraft.util.RandomSource rnd = r.lvl.getRandom();
        boolean first = r.nextAmbient == 0;
        r.nextAmbient = now + 220 + rnd.nextInt(460);
        if (first) return;
        ServerPlayer p = ps.get(rnd.nextInt(ps.size()));
        String[] a = AMB[rnd.nextInt(AMB.length)];
        double ang = rnd.nextDouble() * Math.PI * 2, dist = 7 + rnd.nextDouble() * 12;
        Vec3 at = p.position().add(Math.cos(ang) * dist, 1.2, Math.sin(ang) * dist);
        Fx.sound(p, Compat.sound(a[0], "minecraft:ambient.cave"), SoundSource.AMBIENT, at, Float.parseFloat(a[1]), Float.parseFloat(a[2]) + rnd.nextFloat() * 0.1f);
    }

    static void spawnBatteries(Run r) {
        Grid g = r.core.g;
        java.util.Random rnd = new java.util.Random(g.seed ^ 0xBA77L);
        List<int[]> at = new ArrayList<>();
        for (int t = 0; t < 1200 && at.size() < 9; t++) {
            int x = 1 + rnd.nextInt(Grid.S - 2), z = 1 + rnd.nextInt(Grid.S - 2);
            if (g.tile[x][z] != Grid.FLOOR || g.grate[x][z] || g.lamp[x][z] || r.blocked.contains(x * 4096 + z)) continue;
            if (Math.hypot(x - g.spawn[0], z - g.spawn[1]) < 10 || g.lightAt(x + 0.5, z + 0.5) > 2) continue;
            boolean close = false;
            for (int[] q : at) if (Math.hypot(q[0] - x, q[1] - z) < 11) close = true;
            if (close) continue;
            at.add(new int[]{x, z});
            ItemStack st = new ItemStack(vn.cavemind.chapter.Keys.PIN.get());
            st.getOrCreateTag().putBoolean(BATTERY, true);
            ItemEntity ie = new ItemEntity(r.lvl, r.ox + x + 0.5, F + 1.1, r.oz + z + 0.5, st, 0, 0, 0);
            ie.setUnlimitedLifetime();
            ie.setPickUpDelay(0);
            r.lvl.addFreshEntity(ie);
        }
    }

    private Maze() {}
}
