package vn.cavemind;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.EntityLeaveLevelEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.registries.ForgeRegistries;
import vn.cavemind.compat.Compat;
import vn.cavemind.damned.Effigy;
import vn.cavemind.damned.Haunt;
import vn.cavemind.damned.Shrine;
import vn.cavemind.director.Director;
import vn.cavemind.director.Dread;
import vn.cavemind.man.ManMind;
import vn.cavemind.memory.MindData;
import vn.cavemind.memory.Moment;
import vn.cavemind.memory.Profile;
import vn.cavemind.sense.Watch;
import vn.cavemind.world.Fx;

public final class Events {
    private static final ResourceLocation ROSE = new ResourceLocation("apollyon", "rose_of_life");
    private static final Set<UUID> SLEEPING = new HashSet<>();
    private static final java.util.Map<UUID, Long> BELL = new java.util.HashMap<>();

    @SubscribeEvent
    public static void serverTick(TickEvent.ServerTickEvent e) {
        if (e.phase != TickEvent.Phase.END) return;
        long now = e.getServer().getTickCount();
        Fx.tick(now);
        vn.cavemind.sense.Ears.tick(now);
        try {
            vn.cavemind.man.Scent.tick(e.getServer().overworld(), now);
            vn.cavemind.life.Life.tick(e.getServer());
            vn.cavemind.life.Child.tick(e.getServer());
            vn.cavemind.maze.Maze.server(e.getServer());
            vn.cavemind.gear.Chime.tick(e.getServer(), now);
            vn.cavemind.gear.Nights.tick(e.getServer(), now);
            vn.cavemind.path.Leaving.tick(e.getServer());
            if (now % 1200 == 600) vn.cavemind.lore.Lore.repair(e.getServer().overworld());
        } catch (Throwable t) {
            CaveMind.LOG.warn("[CaveMind] life tick failed", t);
        }
        try {
            if (now % 20 == 3) vn.cavemind.chapter.Tiers.snapshot(e.getServer());
            if (now % 1200 == 900 && Cfg.CHAPTERS.get()) vn.cavemind.lore.Points.repair(e.getServer().overworld());
        } catch (Throwable t) {
            CaveMind.LOG.warn("[CaveMind] chapter tick failed", t);
        }
        if (now % 600 == 0) vn.cavemind.director.PaceLog.flush(e.getServer());
    }

    @SubscribeEvent
    public static void playerTick(TickEvent.PlayerTickEvent e) {
        if (e.phase != TickEvent.Phase.END || !(e.player instanceof ServerPlayer p)) return;
        long now = p.server.getTickCount();
        Watch w = Watch.of(p);
        w.update(p);
        Profile pr = MindData.profile(p);
        pr.playTicks++;
        if (now % 100 == 0) pr.visit(p.blockPosition(), 1);
        if (w.stillTicks == 160 && w.light <= 4 && (ManMind.active(p) || pr.dStage >= 1)) {
            ServerLevel lvl = p.serverLevel();
            pr.stillInDark++;
            pr.moment(new Moment(Moment.UNSEEN, Dread.day(lvl), Dread.time(lvl), p.blockPosition(), 0, 0));
        }
        if (p.isSleeping()) {
            if (SLEEPING.add(p.getUUID())) Haunt.slept(p);
            Haunt.sleeping(p, w, now);
            vn.cavemind.man.Schemes.sleeping(p, w, now);
        } else if (SLEEPING.remove(p.getUUID())) {
            Haunt.woke(p);
        }
        Director.tick(p, w, now);
        ManMind.tick(p, w, now);
        Haunt.tick(p, w, now);
        vn.cavemind.damned.Game.tick(p, w, now);
        vn.cavemind.damned.Hundred.tick(p, w, now);
        vn.cavemind.maze.Maze.tick(p, w, now);
        Dread.tick(p, w, now);
        vn.cavemind.day.Daylight.tick(p, w, now);
        vn.cavemind.life.Glimpse.tick(p, w, now);
        vn.cavemind.hero.Herobrine.tick(p, w, now);
        vn.cavemind.lore.Lore.tick(p, w, pr, now);
        try {
            vn.cavemind.life.Homes.tick(p, w, pr, now);
            vn.cavemind.lore.Pages.tick(p, w, pr, now);
            vn.cavemind.chapter.Book.tick(p, pr, now);
        } catch (Throwable t) {
            CaveMind.LOG.warn("[CaveMind] 5.0 player tick failed", t);
        }
        try {
            vn.cavemind.man.Meter.tick(p, w, pr, now);
        } catch (Throwable t) {
            CaveMind.LOG.warn("[CaveMind] man bar tick failed", t);
        }
        try {
            vn.cavemind.chapter.Chapters.tick(p, w, pr, now);
        } catch (Throwable t) {
            CaveMind.LOG.warn("[CaveMind] chapter player tick failed", t);
        }
        if (now % 4 == 0) vn.cavemind.day.QuietFallen.tick(p, w, now);
        if (now % 5 == 2) hanging(p);
    }

    static void hanging(ServerPlayer p) {
        if (Compat.exempt(p) || !Compat.manActive()) return;
        ServerLevel lvl = p.serverLevel();
        for (net.minecraft.world.entity.Mob m : lvl.getEntitiesOfClass(net.minecraft.world.entity.Mob.class, p.getBoundingBox().inflate(16), x -> "man_hang".equals(Compat.role(x)))) {
            m.setTarget(null);
            if (m.distanceTo(p) > 11) continue;
            net.minecraft.world.phys.AABB box = m.getBoundingBox();
            net.minecraft.world.phys.Vec3 c = box.getCenter();
            lvl.sendParticles(net.minecraft.core.particles.ParticleTypes.CAMPFIRE_COSY_SMOKE, c.x, c.y, c.z, 18, box.getXsize() * 0.4, box.getYsize() * 0.4, box.getZsize() * 0.4, 0.01);
            Fx.soundAll(lvl, "minecraft:entity.phantom.flap", net.minecraft.sounds.SoundSource.HOSTILE, c, 0.6f, 0.5f);
            vn.cavemind.net.Net.fx(p, vn.cavemind.net.Net.SHAKE, 0.6f, 8);
            vn.cavemind.director.Tension.stress(p, 6, "thứ treo trên cây tan vào sương");
            m.discard();
        }
    }

    @SubscribeEvent
    public static void login(PlayerEvent.PlayerLoggedInEvent e) {
        if (!(e.getEntity() instanceof ServerPlayer p)) return;
        Profile pr = MindData.profile(p);
        if (pr.firstDay < 0) pr.firstDay = Dread.day(p.serverLevel());
        if (pr.home == null && pr.homes.isEmpty() && p.getRespawnPosition() != null && p.getRespawnDimension() == net.minecraft.world.level.Level.OVERWORLD) {
            vn.cavemind.life.Homes.slept(p, p.getRespawnPosition());
        }
        Director.joined(p);
        vn.cavemind.maze.Maze.login(p);
        vn.cavemind.chapter.Book.login(p);
        sweepFog(p.server);
    }

    @SubscribeEvent
    public static void logout(PlayerEvent.PlayerLoggedOutEvent e) {
        if (!(e.getEntity() instanceof ServerPlayer p)) return;
        ManMind.forget(p);
        Haunt.forget(p);
        vn.cavemind.damned.Game.forget(p);
        vn.cavemind.damned.Hundred.forget(p);
        vn.cavemind.maze.Maze.logout(p);
        vn.cavemind.gear.Barometer.forget(p.getUUID());
        Watch.forget(p.getUUID());
        Director.forget(p.getUUID());
        Dread.forget(p.getUUID());
        vn.cavemind.day.Daylight.forget(p);
        vn.cavemind.day.Echo.forget(p.getUUID());
        vn.cavemind.hero.Herobrine.forget(p);
        vn.cavemind.life.Glimpse.forget(p);
        vn.cavemind.chapter.Chapters.forget(p);
        SLEEPING.remove(p.getUUID());
    }

    @SubscribeEvent
    public static void started(net.minecraftforge.event.server.ServerStartedEvent e) {
        sweepFog(e.getServer());
    }

    static void sweepFog(net.minecraft.server.MinecraftServer server) {
        if (!vn.cavemind.hero.Herobrine.enabled() || !Cfg.HERO_REPLACE.get()) return;
        var src = server.createCommandSourceStack().withSuppressedOutput().withPermission(4);
        for (String c : new String[]{"kill @e[tag=herobrineEntity]", "kill @e[type=villager,tag=doorGhost]", "kill @e[type=marker,tag=footstepsEmitter]",
            "kill @e[type=marker,tag=torchTrace]", "kill @e[type=marker,tag=chestTrace]", "kill @e[type=marker,tag=candleTrace]", "kill @e[type=marker,tag=lanternTrace]",
            "kill @e[type=marker,tag=doorTrace]", "kill @e[type=marker,tag=bedTrace]", "kill @e[type=husk,tag=herobrine]"}) {
            try {
                server.getCommands().performPrefixedCommand(src, c);
            } catch (Throwable ignored) {
            }
        }
    }

    @SubscribeEvent
    public static void stopped(ServerStoppedEvent e) {
        ManMind.clear();
        Haunt.clear();
        vn.cavemind.damned.Game.clear();
        vn.cavemind.damned.Hundred.clear();
        vn.cavemind.life.Child.clear();
        vn.cavemind.maze.Maze.clear();
        Director.clear();
        Dread.clear();
        Fx.clear();
        Effigy.clear();
        vn.cavemind.day.Daylight.clear();
        vn.cavemind.day.Echo.clear();
        vn.cavemind.hero.Herobrine.clear();
        vn.cavemind.life.Life.clear();
        vn.cavemind.life.Glimpse.clear();
        vn.cavemind.path.Leaving.clear();
        vn.cavemind.man.Scent.clear();
        vn.cavemind.sense.Ears.clear();
        vn.cavemind.chapter.Chapters.clear();
        vn.cavemind.lore.Points.clear();
        vn.cavemind.gear.Nights.clear();
        vn.cavemind.world.Moon.force(null);
        vn.cavemind.director.PaceLog.flush(e.getServer());
        Compat.ONLY_MAP.clear();
        SLEEPING.clear();
        MindData.reset();
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void join(EntityJoinLevelEvent e) {
        if (e.getLevel().isClientSide()) return;
        Entity ent = e.getEntity();
        if (ent instanceof Player) return;
        if (!e.loadedFromDisk() && ent instanceof Mob && ent.getPersistentData().getBoolean(vn.cavemind.chapter.Tiers.LOCKED)) {
            e.setCanceled(true);
            return;
        }
        if (Compat.managed(ent)) {
            String role = Compat.role(ent);
            if (e.loadedFromDisk()) {
                boolean keep = role.equals("effigy")
                    || role.equals("effigy_morning") && ent.getPersistentData().getLong("cavemind_day") == Dread.day((ServerLevel) e.getLevel());
                if (!keep) {
                    e.setCanceled(true);
                    return;
                }
            }
            if (ent.getPersistentData().hasUUID(Compat.ONLY)) Compat.ONLY_MAP.put(ent.getUUID(), ent.getPersistentData().getUUID(Compat.ONLY));
            return;
        }
        if (Compat.damnedActive() && Compat.is(ent, Compat.DAMNED)) {
            Compat.mark(ent, "damned_stray", null, false);
            if (ent instanceof Mob m) {
                m.goalSelector.removeAllGoals(g -> true);
                m.targetSelector.removeAllGoals(g -> true);
            }
            if (e.getLevel() instanceof ServerLevel lvl) Fx.later(lvl, 1200 + lvl.getRandom().nextInt(1200), () -> {
                if (ent.isAlive()) ent.discard();
            });
            return;
        }
        if (Compat.manActive() && e.getLevel() instanceof ServerLevel lvl) {
            var id = net.minecraftforge.registries.ForgeRegistries.ENTITY_TYPES.getKey(ent.getType());
            if (id != null && id.getNamespace().equals("man")) {
                if (!id.getPath().equals("mftfhang")) {
                    e.setCanceled(true);
                    return;
                }
                Compat.mark(ent, "man_hang", null, false);
                if (ent instanceof Mob m) {
                    m.goalSelector.removeAllGoals(g -> true);
                    m.targetSelector.removeAllGoals(g -> true);
                    m.setTarget(null);
                }
                Fx.later(lvl, 2400 + lvl.getRandom().nextInt(1200), () -> {
                    if (ent.isAlive()) ent.discard();
                });
                return;
            }
        }
        if (!e.loadedFromDisk() && Director.headliner(ent) && e.getLevel() instanceof ServerLevel lvl) {
            if (!Director.allowJoin(lvl, ent)) e.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void spawning(net.minecraftforge.event.entity.living.MobSpawnEvent.FinalizeSpawn e) {
        try {
            vn.cavemind.chapter.Tiers.check(e);
        } catch (Throwable t) {
            CaveMind.LOG.warn("[CaveMind] chapter spawn check failed", t);
        }
    }

    @SubscribeEvent
    public static void leave(EntityLeaveLevelEvent e) {
        if (!Compat.ONLY_MAP.isEmpty()) Compat.ONLY_MAP.remove(e.getEntity().getUUID());
        if (!e.getLevel().isClientSide()) {
            Director.left(e.getEntity());
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void attacked(LivingAttackEvent e) {
        Entity target = e.getEntity();
        if (target.level().isClientSide() || !Compat.managed(target)) return;
        String role = Compat.role(target);
        Entity src = e.getSource().getEntity();
        ServerPlayer p = src instanceof ServerPlayer sp ? sp : null;
        switch (role) {
            case "man" -> {
                e.setCanceled(true);
                if (p != null) ManMind.attacked(p, target);
            }
            case "man_hunt" -> {
                e.setCanceled(true);
                if (p != null) ManMind.huntHit(p, target);
            }
            case "man_day", "damned_day", "twin" -> {
                e.setCanceled(true);
                if (p != null) {
                    vn.cavemind.world.Fx.sound(p, role.equals("man_day") ? "man:vanish" : "apollyon:deep_laugh", net.minecraft.sounds.SoundSource.HOSTILE, target.position(), 0.6f, 0.8f);
                    target.discard();
                }
            }
            case "damned", "damned_stray" -> {
                e.setCanceled(true);
                if (p != null) {
                    if (holdsRose(p)) Haunt.banish(p, "rose_strike");
                    else Haunt.hurt(p, target);
                }
            }
            case "damned_game" -> {
                e.setCanceled(true);
                if (p != null) {
                    if (holdsRose(p)) Haunt.banish(p, "rose_strike");
                    else vn.cavemind.damned.Game.attacked(p, target);
                }
            }
            case "damned_maze" -> {
                e.setCanceled(true);
                if (p != null) vn.cavemind.maze.Maze.attacked(p, target);
            }
            default -> {}
        }
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void interact(PlayerInteractEvent.EntityInteract e) {
        if (!(e.getEntity() instanceof ServerPlayer p)) return;
        Entity t = e.getTarget();
        if (!Compat.managed(t)) return;
        String role = Compat.role(t);
        if ((role.equals("damned") || role.equals("damned_stray") || role.equals("damned_game")) && holdsRose(p)) Haunt.banish(p, "rose");
    }

    private static boolean holdsRose(ServerPlayer p) {
        for (ItemStack s : new ItemStack[]{p.getMainHandItem(), p.getOffhandItem()}) {
            ResourceLocation id = ForgeRegistries.ITEMS.getKey(s.getItem());
            if (ROSE.equals(id)) return true;
        }
        return false;
    }

    @SubscribeEvent
    public static void placed(BlockEvent.EntityPlaceEvent e) {
        if (!(e.getEntity() instanceof ServerPlayer p)) return;
        BlockState st = e.getPlacedBlock();
        ResourceLocation id = ForgeRegistries.BLOCKS.getKey(st.getBlock());
        if (id == null) return;
        Profile pr = MindData.profile(p);
        Watch.of(p).hear(0.35f);
        vn.cavemind.sense.Ears.noise(p, net.minecraft.world.phys.Vec3.atCenterOf(e.getPos()), 8, vn.cavemind.sense.Ears.Kind.BUILD);
        if (id.getPath().contains("torch") && !id.getPath().contains("redstone")) pr.torch(e.getPos());
        if (id.getPath().contains("torch") || id.getPath().contains("campfire") || id.getPath().contains("lantern")) {
            vn.cavemind.man.Scent.mark(p.level(), e.getPos(), vn.cavemind.man.Scent.Kind.SMOKE, 1f, p.server.getTickCount(), Watch.of(p).underground);
        }
        var type = st.getSoundType(p.level(), e.getPos(), p);
        vn.cavemind.day.Echo.record(p, e.getPos(), null, type.getPlaceSound(), type.getVolume(), type.getPitch());
        if (ROSE.equals(id) && Shrine.near(pr, e.getPos(), 5)) Haunt.banish(p, "rose_at_shrine");
        vn.cavemind.man.Schemes.placed(p, e.getPos(), st);
        vn.cavemind.life.Life.built(p, e.getPos());
        vn.cavemind.life.Life.worked(p);
        vn.cavemind.hero.Echo.built(p);
        if (p.level().dimension() == net.minecraft.world.level.Level.OVERWORLD) pr.placedBlock(e.getPos(), net.minecraft.world.level.block.Block.getId(st));
        vn.cavemind.life.Homes.worked(p, e.getPos(), 0.03);
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void guard(BlockEvent.BreakEvent e) {
        if (!(e.getPlayer() instanceof ServerPlayer p) || !(e.getLevel() instanceof ServerLevel lvl)) return;
        if (vn.cavemind.maze.Maze.locked(p)) {
            e.setCanceled(true);
            return;
        }
        if (lvl.dimension() != net.minecraft.world.level.Level.OVERWORLD || p.isCreative()) return;
        BlockPos q = e.getPos();
        if (Cfg.CHAPTERS.get() && vn.cavemind.lore.Points.guarded(lvl, q)) {
            e.setCanceled(true);
            p.displayClientMessage(net.minecraft.network.chat.Component.literal("Nó không cho bạn đụng vào cái này.").withStyle(net.minecraft.ChatFormatting.DARK_GRAY, net.minecraft.ChatFormatting.ITALIC), true);
            return;
        }
        if (vn.cavemind.lore.Lore.inside(lvl, "p", "tlo", "thi", q) || vn.cavemind.lore.Lore.inside(lvl, "p", "wlo", "whi", q)) {
            e.setCanceled(true);
            p.displayClientMessage(net.minecraft.network.chat.Component.literal("Nó không cho bạn đụng vào cái này.").withStyle(net.minecraft.ChatFormatting.DARK_GRAY, net.minecraft.ChatFormatting.ITALIC), true);
            return;
        }
        if (vn.cavemind.lore.Lore.inside(lvl, "p", "lo", "hi", q)) {
            vn.cavemind.lore.Lore.damaged(lvl, "p");
            Profile pr = MindData.profile(p);
            long day = Dread.day(lvl);
            if (pr.dNameDay != day) {
                pr.dNameDay = day;
                pr.dDebt = Math.min(3, pr.dDebt + 1);
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void guardPlace(BlockEvent.EntityPlaceEvent e) {
        if (!(e.getEntity() instanceof ServerPlayer p) || !(e.getLevel() instanceof ServerLevel lvl)) return;
        if (vn.cavemind.maze.Maze.locked(p)) {
            e.setCanceled(true);
            return;
        }
        if (lvl.dimension() != net.minecraft.world.level.Level.OVERWORLD || p.isCreative()) return;
        BlockPos lid = vn.cavemind.lore.Lore.part(lvl, "p", "lid");
        BlockPos q = e.getPos();
        if (lid != null && q.getX() == lid.getX() && q.getZ() == lid.getZ() && q.getY() < lid.getY() && q.getY() > lid.getY() - 18) {
            e.setCanceled(true);
            Fx.sound(p, "minecraft:block.gravel.fall", net.minecraft.sounds.SoundSource.BLOCKS, net.minecraft.world.phys.Vec3.atCenterOf(q), 0.6f, 0.5f);
            p.displayClientMessage(net.minecraft.network.chat.Component.literal("Khối rơi xuống giếng và không chạm đáy.").withStyle(net.minecraft.ChatFormatting.DARK_GRAY, net.minecraft.ChatFormatting.ITALIC), true);
        }
    }

    @SubscribeEvent
    public static void broke(BlockEvent.BreakEvent e) {
        if (e.getPlayer() instanceof ServerPlayer p) {
            Watch.of(p).hear(0.6f);
            vn.cavemind.sense.Ears.noise(p, net.minecraft.world.phys.Vec3.atCenterOf(e.getPos()), 16, vn.cavemind.sense.Ears.Kind.MINE);
            vn.cavemind.man.Scent.mark(p.level(), e.getPos(), vn.cavemind.man.Scent.Kind.WOUND, 3f, p.server.getTickCount(), Watch.of(p).underground);
            var type = e.getState().getSoundType(p.level(), e.getPos(), p);
            vn.cavemind.day.Echo.record(p, e.getPos(), type.getHitSound(), type.getBreakSound(), type.getVolume(), type.getPitch());
            vn.cavemind.man.Schemes.broke(p, e.getPos(), e.getState());
            vn.cavemind.hero.Echo.dug(p);
            if (p.level().dimension() == net.minecraft.world.level.Level.OVERWORLD) vn.cavemind.hero.Echo.broke(p, e.getPos());
            if (p.level().dimension() == net.minecraft.world.level.Level.OVERWORLD) MindData.profile(p).brokeBlock(e.getPos());
            vn.cavemind.life.Life.worked(p);
        }
    }

    @SubscribeEvent
    public static void bed(net.minecraftforge.event.entity.player.PlayerSleepInBedEvent e) {
        if (!(e.getEntity() instanceof ServerPlayer p) || !vn.cavemind.man.Meter.huntDue(p)) return;
        e.setResult(net.minecraft.world.entity.player.Player.BedSleepingProblem.OTHER_PROBLEM);
        p.displayClientMessage(net.minecraft.network.chat.Component.literal("Bạn không dám nhắm mắt. Hắn đang ở ngoài kia.").withStyle(net.minecraft.ChatFormatting.DARK_RED, net.minecraft.ChatFormatting.ITALIC), true);
    }

    @SubscribeEvent
    public static void usedBlock(PlayerInteractEvent.RightClickBlock e) {
        if (!(e.getEntity() instanceof ServerPlayer p)) return;
        BlockState st = p.level().getBlockState(e.getPos());
        if (!vn.cavemind.maze.Maze.locked(p) && vn.cavemind.chapter.Chapters.use(p, e.getHand(), e.getPos().immutable(), st)) {
            e.setCanceled(true);
            e.setCancellationResult(net.minecraft.world.InteractionResult.SUCCESS);
            return;
        }
        if (st.getBlock() instanceof net.minecraft.world.level.block.BellBlock && p.server.getTickCount() - BELL.getOrDefault(p.getUUID(), -100L) > 40) {
            BELL.put(p.getUUID(), (long) p.server.getTickCount());
            BlockPos bp = e.getPos().immutable();
            vn.cavemind.sense.Ears.noise(p, net.minecraft.world.phys.Vec3.atCenterOf(bp), 64, vn.cavemind.sense.Ears.Kind.BELL);
            boolean ended = vn.cavemind.damned.Game.bell(p, bp);
            if (!ended && !vn.cavemind.maze.Maze.bell(p, bp)) vn.cavemind.man.ManMind.bell(p, bp);
        }
        if (st.hasBlockEntity()) {
            vn.cavemind.man.Schemes.opened(p, e.getPos().immutable());
            vn.cavemind.life.Homes.worked(p, e.getPos(), 0.08);
        }
        if (vn.cavemind.maze.Maze.locked(p) && !(st.getBlock() instanceof DoorBlock || st.getBlock() instanceof net.minecraft.world.level.block.ChestBlock
            || st.getBlock() instanceof net.minecraft.world.level.block.BellBlock || st.getBlock() instanceof net.minecraft.world.level.block.LecternBlock)) {
            e.setCanceled(true);
            return;
        }
        if (st.getBlock() instanceof DoorBlock || st.getBlock() instanceof TrapDoorBlock || st.getBlock() instanceof FenceGateBlock) {
            Watch.of(p).hear(0.55f);
            vn.cavemind.sense.Ears.noise(p, net.minecraft.world.phys.Vec3.atCenterOf(e.getPos()), 18, vn.cavemind.sense.Ears.Kind.DOOR);
            if (st.getBlock() instanceof DoorBlock) vn.cavemind.day.Echo.record(p, e.getPos(), null,
                st.getValue(DoorBlock.OPEN) ? net.minecraft.sounds.SoundEvents.WOODEN_DOOR_CLOSE : net.minecraft.sounds.SoundEvents.WOODEN_DOOR_OPEN, 1f, 1f);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void drops(net.minecraftforge.event.entity.living.LivingDropsEvent e) {
        if (!(e.getEntity() instanceof ServerPlayer p)) return;
        e.getDrops().removeIf(ie -> vn.cavemind.chapter.Book.is(ie.getItem()));
        if (vn.cavemind.maze.Maze.keepDrops(p, e.getDrops())) e.setCanceled(true);
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void xp(net.minecraftforge.event.entity.living.LivingExperienceDropEvent e) {
        if (e.getEntity() instanceof ServerPlayer p && vn.cavemind.maze.Maze.keepXp(p)) e.setCanceled(true);
    }

    @SubscribeEvent
    public static void cloned(PlayerEvent.Clone e) {
        if (e.isWasDeath() && e.getOriginal() instanceof ServerPlayer old && e.getEntity() instanceof ServerPlayer neu) vn.cavemind.maze.Maze.clone(old, neu);
    }

    @SubscribeEvent
    public static void picked(net.minecraftforge.event.entity.player.EntityItemPickupEvent e) {
        if (!(e.getEntity() instanceof ServerPlayer p)) return;
        if (vn.cavemind.lore.Pages.pickup(p, e.getItem())) {
            e.setCanceled(true);
            return;
        }
        if (vn.cavemind.maze.Maze.inMaze(p) && vn.cavemind.maze.Maze.pickup(p, e.getItem())) e.setCanceled(true);
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void toss(net.minecraftforge.event.entity.item.ItemTossEvent e) {
        if (!(e.getPlayer() instanceof ServerPlayer p) || !vn.cavemind.chapter.Book.is(e.getEntity().getItem())) return;
        ItemStack st = e.getEntity().getItem().copy();
        e.setCanceled(true);
        if (!p.getInventory().add(st)) vn.cavemind.chapter.Book.ensure(p);
        p.displayClientMessage(net.minecraft.network.chat.Component.literal("Cuốn sổ không chịu rời tay bạn.").withStyle(net.minecraft.ChatFormatting.DARK_GRAY, net.minecraft.ChatFormatting.ITALIC), true);
    }

    @SubscribeEvent
    public static void respawned(PlayerEvent.PlayerRespawnEvent e) {
        if (e.getEntity() instanceof ServerPlayer p) vn.cavemind.chapter.Book.ensure(p);
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void pearl(net.minecraftforge.event.entity.EntityTeleportEvent.EnderPearl e) {
        if (vn.cavemind.maze.Maze.inMaze(e.getPlayer()) && !e.getPlayer().isCreative()) e.setCanceled(true);
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void chorus(net.minecraftforge.event.entity.EntityTeleportEvent.ChorusFruit e) {
        if (e.getEntityLiving() instanceof ServerPlayer p && vn.cavemind.maze.Maze.locked(p)) e.setCanceled(true);
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void useItem(PlayerInteractEvent.RightClickItem e) {
        if (!(e.getEntity() instanceof ServerPlayer p) || !vn.cavemind.maze.Maze.locked(p)) return;
        ResourceLocation id = ForgeRegistries.ITEMS.getKey(e.getItemStack().getItem());
        if (id == null) return;
        String s = id.toString();
        if (s.contains("ender_pearl") || s.contains("chorus") || id.getNamespace().equals("waystones") || s.contains("warp") || s.contains("return_scroll") || s.contains("recall")) {
            e.setCanceled(true);
            p.displayClientMessage(net.minecraft.network.chat.Component.literal("Ở đây không có lối tắt nào.").withStyle(net.minecraft.ChatFormatting.DARK_GRAY, net.minecraft.ChatFormatting.ITALIC), true);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void fall(net.minecraftforge.event.entity.living.LivingFallEvent e) {
        if (e.getEntity() instanceof ServerPlayer p && vn.cavemind.maze.Maze.fell(p)) e.setCanceled(true);
    }

    @SubscribeEvent
    public static void died(LivingDeathEvent e) {
        if (!(e.getEntity() instanceof ServerPlayer p)) return;
        vn.cavemind.maze.Maze.died(p);
        ServerLevel lvl = p.serverLevel();
        Profile pr = MindData.profile(p);
        BlockPos at = p.blockPosition();
        pr.moment(new Moment(Moment.DIED, Dread.day(lvl), Dread.time(lvl), at, 0, 0));
        net.minecraft.world.damagesource.DamageSource src = e.getSource();
        boolean friendly = src.getEntity() instanceof net.minecraft.world.entity.player.Player k && k != p
            || src.is(net.minecraft.world.damagesource.DamageTypes.GENERIC_KILL);
        boolean calm = Cfg.MERCY.get() && (Cfg.FRIENDLY_FIRE.get() || !friendly);
        if (calm) Director.died(p);
        if (!friendly || Cfg.FRIENDLY_FIRE.get()) vn.cavemind.director.Tension.died(p);
        if (lvl.dimension() == net.minecraft.world.level.Level.OVERWORLD) {
            vn.cavemind.man.Scent.mark(lvl, at, vn.cavemind.man.Scent.Kind.DEATH, 20f, p.server.getTickCount(), Watch.of(p).underground);
        }
        ManMind.died(p, calm);
        vn.cavemind.hero.Herobrine.died(p, calm);
    }

    public static void retune(net.minecraft.server.MinecraftServer server) {
        Director.calm(server);
        double hero = Cfg.HERO_GAP_MIN.get() * 24000.0 / (Cfg.INTENSITY.get() * Cfg.HERO_FREQ.get());
        for (ServerPlayer p : server.getPlayerList().getPlayers()) {
            Profile pr = MindData.profile(p);
            if (pr.heroNext > pr.playTicks + hero) pr.heroNext = pr.playTicks + (long) hero;
        }
        CaveMind.LOG.info("[CaveMind] config reloaded, schedules retuned");
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void hurt(net.minecraftforge.event.entity.living.LivingHurtEvent e) {
        if (e.getEntity().level().isClientSide()) return;
        Entity src = e.getSource().getEntity();
        if (e.getEntity() instanceof ServerPlayer p) {
            float dmg = e.getAmount();
            boolean friend = src instanceof Player && src != p;
            vn.cavemind.director.Tension.stress(p, dmg * 1.5 * (friend ? 0.2 : 1.0), "mất máu");
            if (p.level().dimension() == net.minecraft.world.level.Level.OVERWORLD && dmg >= 1) {
                vn.cavemind.man.Scent.mark(p.level(), p.blockPosition(), vn.cavemind.man.Scent.Kind.BLOOD, Math.min(8f, 2f + dmg * 1.2f), p.server.getTickCount(), Watch.of(p).underground);
            }
            if (src != null) vn.cavemind.sense.Ears.noise(p, p.position(), 24, vn.cavemind.sense.Ears.Kind.FIGHT);
        } else if (src instanceof ServerPlayer p) {
            vn.cavemind.sense.Ears.noise(p, e.getEntity().position(), 20, vn.cavemind.sense.Ears.Kind.FIGHT);
        }
    }

    @SubscribeEvent
    public static void boom(net.minecraftforge.event.level.ExplosionEvent.Detonate e) {
        if (e.getLevel().isClientSide()) return;
        vn.cavemind.sense.Ears.noise(e.getLevel(), e.getExplosion().getPosition(), 64, vn.cavemind.sense.Ears.Kind.BOOM, e.getLevel().getServer().getTickCount());
    }

    @SubscribeEvent
    public static void chat(net.minecraftforge.event.ServerChatEvent e) {
        if (e.getPlayer() != null) vn.cavemind.hero.Echo.chat(e.getPlayer(), e.getRawText());
    }

    @SubscribeEvent
    public static void commands(RegisterCommandsEvent e) {
        vn.cavemind.cmd.Commands.register(e.getDispatcher());
    }

    private Events() {}
}
