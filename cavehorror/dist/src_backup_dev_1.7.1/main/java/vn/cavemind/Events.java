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
        Fx.tick(e.getServer().getTickCount());
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
        Dread.tick(p, w, now);
        vn.cavemind.day.Daylight.tick(p, w, now);
        vn.cavemind.hero.Herobrine.tick(p, w, now);
        vn.cavemind.lore.Lore.tick(p, w, pr, now);
        if (now % 4 == 0) vn.cavemind.day.QuietFallen.tick(p, w, now);
        if (now % 20 == 0 && vn.cavemind.dev.Dev.on()) hud(p);
        try {
            vn.cavemind.eye.Eye.tick(p, w, now);
        } catch (Throwable t) {
            CaveMind.LOG.warn("[CaveMind] eye tick failed", t);
        }
    }

    private static void hud(ServerPlayer p) {
        Director.State s = Director.state(p);
        String man = ManMind.devLine(p);
        String dmn = Haunt.devLine(p);
        String head = "Căng " + (int) s.tension + " " + s.phase + (s.why.isEmpty() ? "" : " (" + s.why + ")");
        String day = vn.cavemind.day.Daylight.devLine(p);
        if (vn.cavemind.hero.Herobrine.active(p)) {
            p.displayClientMessage(net.minecraft.network.chat.Component.literal(head + " | " + vn.cavemind.hero.Herobrine.devLine(p)).withStyle(net.minecraft.ChatFormatting.DARK_AQUA), true);
            return;
        }
        if (day.startsWith("Ban ngày: ")) {
            p.displayClientMessage(net.minecraft.network.chat.Component.literal(head + " | " + day).withStyle(net.minecraft.ChatFormatting.DARK_AQUA), true);
            return;
        }
        String line;
        if (ManMind.active(p)) line = head + " | " + man;
        else if (dmn.contains(": ") && !dmn.contains("chờ đêm") && !dmn.contains("cảnh lúc")) line = head + " | " + dmn;
        else line = head + " | " + man + " | " + dmn;
        p.displayClientMessage(net.minecraft.network.chat.Component.literal(line).withStyle(net.minecraft.ChatFormatting.DARK_AQUA), true);
    }

    @SubscribeEvent
    public static void login(PlayerEvent.PlayerLoggedInEvent e) {
        if (!(e.getEntity() instanceof ServerPlayer p)) return;
        Profile pr = MindData.profile(p);
        if (pr.firstDay < 0) pr.firstDay = Dread.day(p.serverLevel());
        if (pr.home == null && p.getRespawnPosition() != null) pr.home = p.getRespawnPosition();
        sweepFog(p.server);
    }

    @SubscribeEvent
    public static void logout(PlayerEvent.PlayerLoggedOutEvent e) {
        if (!(e.getEntity() instanceof ServerPlayer p)) return;
        ManMind.forget(p);
        Haunt.forget(p);
        Watch.forget(p.getUUID());
        Director.forget(p.getUUID());
        Dread.forget(p.getUUID());
        vn.cavemind.day.Daylight.forget(p);
        vn.cavemind.day.Echo.forget(p.getUUID());
        vn.cavemind.hero.Herobrine.forget(p);
        SLEEPING.remove(p.getUUID());
    }

    @SubscribeEvent
    public static void started(net.minecraftforge.event.server.ServerStartedEvent e) {
        vn.cavemind.eye.Eye.open(e.getServer());
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
        vn.cavemind.eye.Eye.close();
        ManMind.clear();
        Haunt.clear();
        Director.clear();
        Dread.clear();
        Fx.clear();
        Effigy.clear();
        vn.cavemind.day.Daylight.clear();
        vn.cavemind.day.Echo.clear();
        vn.cavemind.hero.Herobrine.clear();
        Compat.ONLY_MAP.clear();
        SLEEPING.clear();
        MindData.reset();
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void join(EntityJoinLevelEvent e) {
        if (e.getLevel().isClientSide()) return;
        Entity ent = e.getEntity();
        if (ent instanceof Player) return;
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
        if (!e.loadedFromDisk() && Director.headliner(ent) && e.getLevel() instanceof ServerLevel lvl) {
            if (!Director.allowJoin(lvl, ent)) e.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void leave(EntityLeaveLevelEvent e) {
        if (!Compat.ONLY_MAP.isEmpty()) Compat.ONLY_MAP.remove(e.getEntity().getUUID());
        if (!e.getLevel().isClientSide()) {
            Director.left(e.getEntity());
            vn.cavemind.eye.Eye.MANAGED.remove(e.getEntity().getUUID());
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
                    vn.cavemind.dev.Dev.say(p, "Bạn đánh nó — nó chỉ tan biến");
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
            default -> {}
        }
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void interact(PlayerInteractEvent.EntityInteract e) {
        if (!(e.getEntity() instanceof ServerPlayer p)) return;
        Entity t = e.getTarget();
        if (!Compat.managed(t)) return;
        String role = Compat.role(t);
        if ((role.equals("damned") || role.equals("damned_stray")) && holdsRose(p)) Haunt.banish(p, "rose");
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
        if (id.getPath().contains("torch") && !id.getPath().contains("redstone")) pr.torch(e.getPos());
        var type = st.getSoundType(p.level(), e.getPos(), p);
        vn.cavemind.day.Echo.record(p, e.getPos(), null, type.getPlaceSound(), type.getVolume(), type.getPitch());
        if (ROSE.equals(id) && Shrine.near(pr, e.getPos(), 5)) Haunt.banish(p, "rose_at_shrine");
        vn.cavemind.man.Schemes.placed(p, e.getPos(), st);
        if (p.level().dimension() == net.minecraft.world.level.Level.OVERWORLD) pr.placedBlock(e.getPos(), net.minecraft.world.level.block.Block.getId(st));
    }

    @SubscribeEvent
    public static void broke(BlockEvent.BreakEvent e) {
        if (e.getPlayer() instanceof ServerPlayer p) {
            Watch.of(p).hear(0.6f);
            var type = e.getState().getSoundType(p.level(), e.getPos(), p);
            vn.cavemind.day.Echo.record(p, e.getPos(), type.getHitSound(), type.getBreakSound(), type.getVolume(), type.getPitch());
            vn.cavemind.man.Schemes.broke(p, e.getPos(), e.getState());
            if (p.level().dimension() == net.minecraft.world.level.Level.OVERWORLD) MindData.profile(p).brokeBlock(e.getPos());
        }
    }

    @SubscribeEvent
    public static void usedBlock(PlayerInteractEvent.RightClickBlock e) {
        if (!(e.getEntity() instanceof ServerPlayer p)) return;
        BlockState st = p.level().getBlockState(e.getPos());
        if (st.getBlock() instanceof net.minecraft.world.level.block.BellBlock && p.server.getTickCount() - BELL.getOrDefault(p.getUUID(), -100L) > 40) {
            BELL.put(p.getUUID(), (long) p.server.getTickCount());
            vn.cavemind.man.ManMind.bell(p, e.getPos().immutable());
        }
        if (st.hasBlockEntity()) vn.cavemind.man.Schemes.opened(p, e.getPos().immutable());
        if (st.getBlock() instanceof DoorBlock || st.getBlock() instanceof TrapDoorBlock || st.getBlock() instanceof FenceGateBlock) {
            Watch.of(p).hear(0.55f);
            if (st.getBlock() instanceof DoorBlock) vn.cavemind.day.Echo.record(p, e.getPos(), null,
                st.getValue(DoorBlock.OPEN) ? net.minecraft.sounds.SoundEvents.WOODEN_DOOR_CLOSE : net.minecraft.sounds.SoundEvents.WOODEN_DOOR_OPEN, 1f, 1f);
        }
    }

    @SubscribeEvent
    public static void died(LivingDeathEvent e) {
        if (!(e.getEntity() instanceof ServerPlayer p)) return;
        ServerLevel lvl = p.serverLevel();
        Profile pr = MindData.profile(p);
        BlockPos at = p.blockPosition();
        pr.moment(new Moment(Moment.DIED, Dread.day(lvl), Dread.time(lvl), at, 0, 0));
        Director.died(p);
        ManMind.died(p);
        vn.cavemind.hero.Herobrine.died(p);
    }

    @SubscribeEvent
    public static void commands(RegisterCommandsEvent e) {
        vn.cavemind.cmd.Commands.register(e.getDispatcher());
    }

    private Events() {}
}
