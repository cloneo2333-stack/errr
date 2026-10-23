package vn.cavemind.cmd;

import com.mojang.authlib.GameProfile;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.BellBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CandleBlock;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.entity.SignText;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraft.world.phys.Vec3;
import vn.cavemind.compat.Compat;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import vn.cavemind.chapter.Chapters;
import vn.cavemind.chapter.Keys;
import vn.cavemind.lore.Points;
import vn.cavemind.memory.MindData;
import vn.cavemind.memory.Profile;
import vn.cavemind.sense.Watch;
import vn.cavemind.world.Fx;

public final class SelfTest {
    static final List<String> OUT = new ArrayList<>();

    @SubscribeEvent
    public static void commands(RegisterCommandsEvent e) {
        e.getDispatcher().register(net.minecraft.commands.Commands.literal("cmtest").requires(s -> s.hasPermission(2)).executes(c -> {
            ServerLevel lvl = c.getSource().getServer().overworld();
            OUT.clear();
            try {
                run(lvl);
            } catch (Throwable t) {
                log("EXCEPTION " + t);
                for (StackTraceElement st : t.getStackTrace()) log("  at " + st);
                flush(lvl);
            }
            c.getSource().sendSuccess(() -> Component.literal("selftest started"), false);
            return 1;
        }));
        e.getDispatcher().register(net.minecraft.commands.Commands.literal("cmmaze").executes(c -> {
            net.minecraft.server.level.ServerPlayer p = c.getSource().getPlayerOrException();
            return vn.cavemind.maze.Maze.testJoin(p) ? 1 : 0;
        }));
    }

    static void log(String s) {
        OUT.add(s);
        vn.cavemind.CaveMind.LOG.info("[SELFTEST] " + s);
    }

    static void check(String what, boolean ok) {
        log((ok ? "PASS " : "FAIL ") + what);
    }

    static void flush(ServerLevel lvl) {
        try (FileWriter w = new FileWriter(lvl.getServer().getServerDirectory().toPath().resolve("selftest.txt").toFile())) {
            for (String s : OUT) w.write(s + "\n");
        } catch (Exception ignored) {
        }
    }

    static long aligned(FakePlayer fp, long base) {
        long t = base - base % 20 + (fp.getId() & 15);
        return t <= base ? t + 20 : t;
    }

    static void run(ServerLevel lvl) {
        FakePlayer fp = FakePlayerFactory.get(lvl, new GameProfile(UUID.fromString("11111111-2222-3333-4444-555555555555"), "Tester"));
        fp.setPos(0.5, -60, 0.5);
        Profile pr = MindData.profile(fp);
        pr.chapter = 1;
        java.util.Arrays.fill(pr.gate, 0);
        fp.getInventory().clearContent();
        v5(lvl, fp, pr);
        try {
            v6(lvl, fp, pr);
        } catch (Throwable t) {
            check("v6 (" + t + ")", false);
        }
        try {
            v7(lvl, fp, pr);
        } catch (Throwable t) {
            check("v7 (" + t + ")", false);
        }
        check("tiers loaded", vn.cavemind.chapter.Tiers.tier(net.minecraft.world.entity.EntityType.ZOMBIE) == 1);
        check("invite 3 refused at chapter 1", Chapters.invite(fp, 3) == 0);
        check("invite 2 accepted", Chapters.invite(fp, 2) == 1);
        Points.Point g2 = Points.get(lvl, "g2"), w2 = Points.get(lvl, "w2");
        check("point g2 assigned", g2 != null);
        check("waypoint w2 assigned", w2 != null);
        boolean compass = false;
        for (ItemStack s : fp.getInventory().items) if (s.is(Items.COMPASS) && s.hasTag() && s.getTag().getInt("cavemind_gate") == 2) compass = true;
        check("compass given", compass);
        if (g2 == null) {
            flush(lvl);
            return;
        }
        double dist = Math.sqrt(g2.target.distSqr(BlockPos.ZERO));
        log("g2 target " + g2.target + " dist " + (int) dist + " w2 " + w2.target);
        check("g2 distance in range", dist > 250 && dist < 470);
        int cx = g2.target.getX() >> 4, cz = g2.target.getZ() >> 4;
        for (int x = -4; x <= 4; x++) for (int z = -4; z <= 4; z++) lvl.setChunkForced(cx + x, cz + z, true);
        Fx.later(lvl, 200, () -> step2(lvl, fp, pr));
    }

    static void v7(ServerLevel lvl, FakePlayer fp, Profile pr) {
        vn.cavemind.world.Moon.force("blood_moon");
        check("v7 moon can be forced", vn.cavemind.world.Moon.is(lvl, vn.cavemind.world.Moon.BLOOD));
        vn.cavemind.world.Moon.force(null);
        check("v7 no Lunar in dev means no moon", vn.cavemind.world.Moon.now(lvl).isEmpty());
        check("v7 barometer bands", vn.cavemind.gear.Barometer.level(250) == 0 && vn.cavemind.gear.Barometer.level(150) == 1 && vn.cavemind.gear.Barometer.level(50) == 2);
        var rm = lvl.getServer().getRecipeManager();
        boolean recipes = rm.byKey(new net.minecraft.resources.ResourceLocation("cavemind", "phong_vu_bieu_suong")).isPresent()
            && rm.byKey(new net.minecraft.resources.ResourceLocation("cavemind", "chuong_gio_xuong")).isPresent()
            && (rm.byKey(new net.minecraft.resources.ResourceLocation("cavemind", "may_thu_am")).isPresent() || rm.byKey(new net.minecraft.resources.ResourceLocation("cavemind", "may_thu_am_doi")).isPresent())
            && (rm.byKey(new net.minecraft.resources.ResourceLocation("cavemind", "bi_day_cot")).isPresent() || rm.byKey(new net.minecraft.resources.ResourceLocation("cavemind", "bi_day_cot_doi")).isPresent());
        check("v7 gear recipes load", recipes);
        BlockPos c = fp.blockPosition().offset(-6, 0, -6);
        lvl.setBlock(c.below(), net.minecraft.world.level.block.Blocks.STONE.defaultBlockState(), 3);
        int before = vn.cavemind.gear.Chime.count(lvl.getServer());
        lvl.setBlock(c, vn.cavemind.gear.Gear.CHIME_BLOCK.get().defaultBlockState(), 3);
        check("v7 chime remembered when placed", vn.cavemind.gear.Chime.count(lvl.getServer()) == before + 1);
        boolean g1 = vn.cavemind.gear.Chime.guard(lvl, c.offset(5, 0, 5), null), g2 = vn.cavemind.gear.Chime.guard(lvl, c.offset(5, 0, 5), null);
        check("v7 chime guards twice and still hangs", g1 && g2 && lvl.getBlockState(c).is(vn.cavemind.gear.Gear.CHIME_BLOCK.get()));
        boolean g3 = vn.cavemind.gear.Chime.guard(lvl, c.offset(5, 0, 5), null);
        check("v7 chime snaps on the third", g3 && lvl.getBlockState(c).isAir() && vn.cavemind.gear.Chime.count(lvl.getServer()) == before);
        check("v7 far from any chime is unguarded", !vn.cavemind.gear.Chime.guard(lvl, c.offset(60, 0, 0), null));
        for (net.minecraft.world.entity.item.ItemEntity ie : lvl.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class, new net.minecraft.world.phys.AABB(c).inflate(3))) ie.discard();
        lvl.setBlock(c.below(), net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 3);
        check("v7 decoys unlimited outside the maze", vn.cavemind.maze.Maze.decoy(fp));
        check("v7 book knows the moon", vn.cavemind.chapter.Book.build(fp, -1).has("moon"));
        BlockPos oldPage = pr.pageAt;
        pr.pageAt = fp.blockPosition().offset(40, 0, 0);
        var t1 = vn.cavemind.gear.Finder.target(fp, 1);
        check("v8 firefly finds the page", t1 != null && t1.pos().equals(pr.pageAt));
        pr.pageAt = oldPage;
        check("v8 firefly auto mode never crashes", vn.cavemind.gear.Finder.target(fp, 0) != null || true);
        boolean had = pr.gotFinder;
        pr.gotFinder = false;
        fp.getInventory().clearContent();
        vn.cavemind.chapter.Book.ensure(fp);
        boolean jar = false;
        for (ItemStack st : fp.getInventory().items) if (st.is(vn.cavemind.gear.Gear.FINDER.get())) jar = true;
        check("v8 a firefly jar is handed out once", jar && pr.gotFinder);
        fp.getInventory().clearContent();
        vn.cavemind.chapter.Book.ensure(fp);
        jar = false;
        for (ItemStack st : fp.getInventory().items) if (st.is(vn.cavemind.gear.Gear.FINDER.get())) jar = true;
        check("v8 the jar is not handed out twice", !jar);
        pr.gotFinder = had;
        fp.getInventory().clearContent();
    }

    static void v6(ServerLevel lvl, FakePlayer fp, Profile pr) {
        BlockPos o = fp.blockPosition();
        lvl.getChunk(o);
        net.minecraft.world.level.block.state.BlockState torch = net.minecraft.world.level.block.Blocks.TORCH.defaultBlockState();
        net.minecraft.world.level.block.state.BlockState fire = net.minecraft.world.level.block.Blocks.CAMPFIRE.defaultBlockState();
        BlockPos t1 = o.offset(3, 0, 0), t2 = o.offset(-4, 0, 2), t3 = o.offset(0, 0, 5);
        for (BlockPos q : new BlockPos[]{t1, t2, t3}) lvl.setBlock(q.below(), net.minecraft.world.level.block.Blocks.STONE.defaultBlockState(), 3);
        lvl.setBlock(t1, torch, 3);
        lvl.setBlock(t2, fire, 3);
        lvl.setBlock(t3, net.minecraft.world.level.block.Blocks.GLOWSTONE.defaultBlockState(), 3);
        check("v6 torch fakes to air", vn.cavemind.damned.Dark.fakeFor(lvl, t1, torch).isAir());
        check("v6 campfire fakes unlit", !vn.cavemind.damned.Dark.fakeFor(lvl, t2, fire).getValue(net.minecraft.world.level.block.CampfireBlock.LIT));
        check("v6 glowstone left alone", vn.cavemind.damned.Dark.fakeFor(lvl, t3, lvl.getBlockState(t3)) == null);
        check("v6 enchanting table left alone", vn.cavemind.damned.Dark.fakeFor(lvl, t3, net.minecraft.world.level.block.Blocks.ENCHANTING_TABLE.defaultBlockState()) == null);
        int n = vn.cavemind.damned.Dark.start(fp);
        check("v6 blackout catches nearby lights (" + n + ")", n >= 2 && vn.cavemind.damned.Dark.on(fp));
        vn.cavemind.damned.Dark.end(fp);
        check("v6 blackout ends", !vn.cavemind.damned.Dark.on(fp));
        check("v6 server lights untouched", lvl.getBlockState(t1).is(net.minecraft.world.level.block.Blocks.TORCH) && lvl.getBlockState(t2).getValue(net.minecraft.world.level.block.CampfireBlock.LIT));
        for (BlockPos q : new BlockPos[]{t1, t2, t3}) {
            lvl.setBlock(q, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 3);
            lvl.setBlock(q.below(), net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 3);
        }
        var kid = vn.cavemind.damned.Kids.test(fp, o.offset(6, 0, 0));
        check("v6 shadow kid is small, hidden and owner-only", kid != null && kid.isSmall() && kid.isInvisible() && Compat.ONLY_MAP.containsKey(kid.getUUID()));
        if (kid != null) kid.discard();
        vn.cavemind.compat.Radio.relay(fp, "…một…", 1);
        check("v6 radio relay without radios is quiet", !vn.cavemind.compat.Radio.carries(fp));
        BlockPos lid = vn.cavemind.lore.Lore.part(lvl, "p", "lid");
        if (lid != null) {
            Vec3 back = fp.position();
            fp.setPos(lid.getX() + 0.5, lid.getY() - 9, lid.getZ() + 0.5);
            fp.fallDistance = 12;
            check("v6 falling down the well never hurts", vn.cavemind.maze.Maze.fell(fp) && fp.fallDistance == 0);
            fp.setPos(back.x, back.y, back.z);
            check("v6 outside the well falls normally", !vn.cavemind.maze.Maze.fell(fp));
        } else log("v6 no playground here, well test skipped");
    }

    static void v5(ServerLevel lvl, FakePlayer fp, Profile pr) {
        pr.homes.clear();
        pr.home = null;
        BlockPos a = new BlockPos(100, 64, 100), b = new BlockPos(400, 64, 100);
        vn.cavemind.life.Homes.slept(fp, a);
        check("v5 home set after first night", a.equals(pr.home));
        vn.cavemind.life.Homes.slept(fp, b);
        check("v5 one night elsewhere keeps the home", a.equals(pr.home));
        for (Profile.Home h : pr.homes) if (h.pos.distSqr(a) < 100) h.score += 6;
        for (Profile.Home h : pr.homes) if (h.pos.distSqr(b) < 100) h.lastNight = -10;
        vn.cavemind.life.Homes.slept(fp, b);
        check("v5 two nights elsewhere still keeps a lived-in home", a.equals(pr.home));
        for (int n = 0; n < 4; n++) {
            for (Profile.Home h : pr.homes) if (h.pos.distSqr(b) < 100) h.lastNight = -20 - n;
            vn.cavemind.life.Homes.slept(fp, b);
        }
        check("v5 moving for good switches the home", b.equals(pr.home));
        log("v5 " + vn.cavemind.life.Homes.describe(pr));
        pr.homes.clear();
        pr.home = null;
        try {
            var o = vn.cavemind.chapter.Book.build(fp, -1);
            check("v5 book builds", o.has("goals") && o.getAsJsonArray("chapters").size() == 6);
            log("v5 book bytes " + o.toString().length());
        } catch (Throwable t) {
            check("v5 book builds (" + t + ")", false);
        }
        check("v5 pages texts", vn.cavemind.lore.Pages.TEXT.length == vn.cavemind.lore.Pages.TOTAL && vn.cavemind.lore.Pages.TOTAL == 24);
        int bad = 0, closets = 0, nooks = 0;
        for (long seed = 1; seed <= 40; seed++) {
            vn.cavemind.maze.core.Grid g = new vn.cavemind.maze.core.Grid(seed * 7919);
            closets += g.closets.size();
            nooks += g.nooks.size();
            int well = vn.cavemind.maze.core.Grid.cellAt(g.spawn[0], g.spawn[1]);
            if (!g.walkable(g.spawn[0], g.spawn[1]) || !g.walkable(g.chest[0] + 1, g.chest[1])) bad++;
            for (double[] t : g.toys) {
                if (!g.walkable(t[0], t[1])) bad++;
                if (g.pathCells(well, vn.cavemind.maze.core.Grid.cellAt(t[0], t[1])) == Integer.MAX_VALUE) bad++;
            }
            for (vn.cavemind.maze.core.Grid.Nook n : g.nooks) {
                int fx = n.x + new int[]{1, 0, -1, 0}[n.face], fz = n.z + new int[]{0, 1, 0, -1}[n.face];
                if (!g.walkable(fx + 0.5, fz + 0.5)) bad++;
            }
        }
        log("v5 grids: closets/maze " + closets / 40.0 + " nooks/maze " + nooks / 40.0);
        check("v5 40 mazes: toys reachable, nooks open to a floor", bad == 0);
        int before = MindData.get(lvl.getServer()).coop;
        vn.cavemind.chapter.Book.setMode(lvl.getServer(), 1, null);
        check("v5 together mode stored", vn.cavemind.chapter.Book.together(lvl.getServer()));
        vn.cavemind.chapter.Book.setMode(lvl.getServer(), before, null);
    }

    static void step2(ServerLevel lvl, FakePlayer fp, Profile pr) {
        try {
            Points.Point g2 = Points.get(lvl, "g2");
            fp.setPos(g2.target.getX() + 60.5, g2.target.getY() + 1, g2.target.getZ() + 0.5);
            Points.tick(lvl, fp);
            check("g2 built by tick", g2.built && g2.pos != null);
            if (!g2.built) {
                flush(lvl);
                return;
            }
            log("g2 built at " + g2.pos + " post " + g2.post + " lectern " + g2.lectern + " candles " + g2.candles);
            check("g2 has 4 candles", g2.candles.size() == 4);
            for (BlockPos q : g2.candles) check("candle block at " + q, lvl.getBlockState(q).getBlock() instanceof CandleBlock);
            check("post is a log", lvl.getBlockState(g2.post).is(net.minecraft.tags.BlockTags.LOGS));
            check("lectern has book", lvl.getBlockEntity(g2.lectern) instanceof net.minecraft.world.level.block.entity.LecternBlockEntity le && le.hasBook());
            check("post guarded", Points.guarded(lvl, g2.post.above()));
            Watch w = Watch.of(fp);
            fp.setPos(g2.pos.getX() + 0.5, g2.pos.getY() + 1, g2.pos.getZ() + 0.5);
            long now = aligned(fp, lvl.getServer().getTickCount());
            Chapters.tick(fp, w, pr, now);
            check("arrived", (pr.gate[2] & Chapters.ARRIVED) != 0);
            Chapters.tick(fp, w, pr, now + 20);
            check("no key before reading", (pr.gate[2] & Chapters.LIT) == 0);
            Chapters.use(fp, InteractionHand.MAIN_HAND, g2.lectern, lvl.getBlockState(g2.lectern));
            check("read", (pr.gate[2] & Chapters.READ) != 0);
            if (lvl.getBlockEntity(g2.lectern) instanceof net.minecraft.world.level.block.entity.LecternBlockEntity le0) le0.clearContent();
            check("refill click handled", Chapters.use(fp, InteractionHand.MAIN_HAND, g2.lectern, lvl.getBlockState(g2.lectern)));
            check("lectern refilled", lvl.getBlockEntity(g2.lectern) instanceof net.minecraft.world.level.block.entity.LecternBlockEntity le1 && le1.hasBook());
            for (BlockPos q : g2.candles) if (lvl.getBlockState(q).getBlock() instanceof CandleBlock) lvl.setBlock(q, lvl.getBlockState(q).setValue(CandleBlock.LIT, false), 3);
            for (int i = 0; i < g2.candles.size() - 1; i++) {
                BlockPos q = g2.candles.get(i);
                lvl.setBlock(q, lvl.getBlockState(q).setValue(CandleBlock.LIT, true), 3);
            }
            Chapters.tick(fp, w, pr, now + 40);
            check("not lit with 3 of 4", (pr.gate[2] & Chapters.LIT) == 0);
            BlockPos last = g2.candles.get(g2.candles.size() - 1);
            lvl.setBlock(last, lvl.getBlockState(last).setValue(CandleBlock.LIT, true), 3);
            Chapters.tick(fp, w, pr, now + 60);
            check("lit", (pr.gate[2] & Chapters.LIT) != 0);
            List<ItemEntity> keys = lvl.getEntitiesOfClass(ItemEntity.class, new AABB(g2.post).inflate(4), ie -> ie.getItem().is(Keys.LINH.get()));
            check("key dropped at post", keys.size() == 1);
            keys.forEach(ItemEntity::discard);
            BlockState post = lvl.getBlockState(g2.post);
            fp.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Keys.LINH.get()));
            lvl.setDayTime(6000);
            boolean handled = Chapters.use(fp, InteractionHand.MAIN_HAND, g2.post.above(), post);
            check("day refusal handled", handled);
            lvl.setDayTime(18000);
            handled = Chapters.use(fp, InteractionHand.MAIN_HAND, g2.post, post);
            check("night start handled (damned off in dev)", handled);
            Chapters.ended(fp, 2, true, g2.post, "test");
            check("chapter 2 unlocked", pr.chapter == 2 && (pr.gate[2] & Chapters.WON) != 0);
            boolean keyLeft = false, gift = false;
            for (ItemStack s : fp.getInventory().items) {
                if (s.is(Keys.LINH.get())) keyLeft = true;
                if (s.hasTag() && s.getTag().contains("cavemind_gift")) gift = true;
            }
            if (fp.getMainHandItem().is(Keys.LINH.get())) keyLeft = true;
            check("key consumed", !keyLeft);
            check("rare gift given", gift);
            check("invite 3 accepted", Chapters.invite(fp, 3) == 1);
            boolean book = false;
            for (ItemStack s : fp.getInventory().items) if (s.is(Items.WRITTEN_BOOK)) book = true;
            check("altar book given", book);
            fp.getInventory().items.replaceAll(st -> st.is(Items.WRITTEN_BOOK) ? ItemStack.EMPTY : st);
            Chapters.tick(fp, w, pr, now + 200);
            boolean again = false;
            for (ItemStack s2 : fp.getInventory().items) if (s2.is(Items.WRITTEN_BOOK) && s2.getTag().getInt("cavemind_gate") == 3) again = true;
            check("lost altar book given again", again);
            altar(lvl, fp, pr);
            check("invite 2 again is a no-op", Chapters.invite(fp, 2) == 1 && pr.chapter == 3);
            log("describe: " + Chapters.describe(fp));
            Points.Point wp = Points.get(lvl, "w2");
            int wx = wp.target.getX() >> 4, wz = wp.target.getZ() >> 4;
            for (int x = -2; x <= 2; x++) for (int z = -2; z <= 2; z++) lvl.setChunkForced(wx + x, wz + z, true);
            Fx.later(lvl, 40, () -> {
                fp.setPos(wp.target.getX() + 40.5, wp.target.getY() + 1, wp.target.getZ() + 0.5);
                Points.tick(lvl, fp);
                check("w2 built by tick", wp.built);
                log("w2 at " + wp.pos + " post " + wp.post);
                log("maze: " + vn.cavemind.maze.Maze.selftest(lvl.getServer()));
                meter(fp);
                log("DONE");
                flush(lvl);
            });
        } catch (Throwable t) {
            log("EXCEPTION " + t);
            for (StackTraceElement st : t.getStackTrace()) log("  at " + st);
            flush(lvl);
        }
    }

    static void meter(FakePlayer fp) {
        vn.cavemind.man.Meter.test = true;
        Profile pr = MindData.profile(fp);
        pr.manSightings = 1;
        pr.meter = 0;
        pr.meterStage = 0;
        check("bar empty no stage due", vn.cavemind.man.Meter.due(pr) == 0);
        vn.cavemind.man.Meter.add(fp, 30, "test");
        check("bar capped at first mark", Math.abs(pr.meter - 25) < 0.01 && vn.cavemind.man.Meter.due(pr) == 1);
        vn.cavemind.man.Meter.add(fp, 10, "test");
        check("bar waits while stage 1 pending", Math.abs(pr.meter - 25) < 0.01);
        vn.cavemind.man.Meter.complete(fp, 1);
        check("stage 1 done", pr.meterStage == 1 && vn.cavemind.man.Meter.due(pr) == 0);
        vn.cavemind.man.Meter.complete(fp, 3);
        check("out of order completion ignored", pr.meterStage == 1);
        vn.cavemind.man.Meter.bell(fp);
        check("bell never drops below the mark", pr.meter >= 25);
        vn.cavemind.man.Meter.add(fp, 200, "test");
        check("stage 2 due at 50", vn.cavemind.man.Meter.due(pr) == 2 && Math.abs(pr.meter - 50) < 0.01);
        check("visit wanted", vn.cavemind.man.Meter.wantsVisit(fp));
        vn.cavemind.man.Meter.complete(fp, 2);
        vn.cavemind.man.Meter.add(fp, 200, "test");
        vn.cavemind.man.Meter.complete(fp, 3);
        vn.cavemind.man.Meter.add(fp, 200, "test");
        check("hunt night due at 100", vn.cavemind.man.Meter.due(pr) == 4 && pr.meter >= 99.99);
        fp.serverLevel().getServer().setDifficulty(net.minecraft.world.Difficulty.NORMAL, true);
        check("hunt due blocks sleep", vn.cavemind.man.Meter.huntDue(fp));
        vn.cavemind.man.Meter.complete(fp, 4);
        check("after the hunt the bar restarts", pr.meterStage == 0 && pr.meter > 10 && pr.meter < 25);
        vn.cavemind.man.Meter.test = false;
    }

    static void altar(ServerLevel lvl, FakePlayer fp, Profile pr) {
        BlockPos base = new BlockPos(40, -60, 40);
        for (BlockPos c : new BlockPos[]{base, new BlockPos(-40, -60, -40)}) {
            for (BlockPos q : BlockPos.betweenClosed(c.offset(-9, 0, -9), c.offset(9, 7, 9))) lvl.setBlock(q, Blocks.AIR.defaultBlockState(), 3);
        }
        for (int y = 0; y < 3; y++) lvl.setBlock(base.above(y), Blocks.OAK_LOG.defaultBlockState(), 3);
        int[][] ring = {{2, 0}, {-2, 0}, {0, 2}, {0, -2}, {2, 2}, {-2, -2}};
        for (int i = 0; i < 5; i++) lvl.setBlock(base.offset(ring[i][0], 0, ring[i][1]), Blocks.CANDLE.defaultBlockState().setValue(CandleBlock.LIT, true), 3);
        lvl.setBlock(base.offset(5, 0, 0), Blocks.BELL.defaultBlockState().setValue(BellBlock.ATTACHMENT, net.minecraft.world.level.block.state.properties.BellAttachType.FLOOR), 3);
        lvl.setBlock(base.offset(-3, 0, 1), Blocks.OAK_SIGN.defaultBlockState(), 3);
        if (lvl.getBlockEntity(base.offset(-3, 0, 1)) instanceof SignBlockEntity sb) {
            SignText t = sb.getFrontText().setMessage(1, Component.literal("TUAN"));
            sb.setText(t, true);
        }
        fp.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Keys.TUAN.get()));
        lvl.setDayTime(18000);
        List<String> m5 = Chapters.altar(lvl, base, 3);
        log("missing with 5 candles: " + m5);
        check("altar with 5 candles incomplete", m5.size() == 1 && m5.get(0).contains("6"));
        check("click with 5 candles handled", Chapters.use(fp, InteractionHand.MAIN_HAND, base.above(), lvl.getBlockState(base.above())));
        lvl.setBlock(base.offset(ring[5][0], 0, ring[5][1]), Blocks.CANDLE.defaultBlockState().setValue(CandleBlock.LIT, true), 3);
        lvl.setDayTime(6000);
        List<String> md = Chapters.altar(lvl, base, 3);
        log("missing by day: " + md);
        check("altar underground playable by day", md.isEmpty());
        lvl.setDayTime(18000);
        List<String> mn = Chapters.altar(lvl, base, 3);
        log("missing at night: " + mn);
        check("altar at night complete", mn.isEmpty());
        check("deep rule for chapter 5", !Chapters.altar(lvl, base, 5).isEmpty());
        Chapters.ended(fp, 3, true, base, "test");
        check("chapter 3 unlocked after altar win", pr.chapter == 3);
        Points.altar(lvl, 3, fp.getUUID(), base);
        check("altar registered as post", Points.postAt(lvl, base.above()) != null);
        check("altar not guarded", !Points.guarded(lvl, base));
        BlockPos own = new BlockPos(-40, -60, -40);
        for (int y = 0; y < 3; y++) lvl.setBlock(own.above(y), Blocks.SPRUCE_LOG.defaultBlockState(), 3);
        fp.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.CANDLE));
        check("bare log column refused for free play", !Chapters.use(fp, InteractionHand.MAIN_HAND, own.above(), lvl.getBlockState(own.above())));
        lvl.setBlock(own.offset(2, 0, 1), Blocks.OAK_SIGN.defaultBlockState(), 3);
        if (lvl.getBlockEntity(own.offset(2, 0, 1)) instanceof SignBlockEntity sb) sb.setText(sb.getFrontText().setMessage(0, Component.literal("Cây Đếm")), true);
        check("self built counting tree accepted for free play", Chapters.use(fp, InteractionHand.MAIN_HAND, own.above(2), lvl.getBlockState(own.above(2))));
    }

    private SelfTest() {}
}
