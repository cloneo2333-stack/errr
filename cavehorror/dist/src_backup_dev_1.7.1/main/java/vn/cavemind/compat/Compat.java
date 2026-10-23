package vn.cavemind.compat;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.registries.ForgeRegistries;
import vn.cavemind.CaveMind;
import vn.cavemind.Cfg;

public final class Compat {
    public static final ResourceLocation MAN_STALKER = new ResourceLocation("man", "manfromthefog");
    public static final ResourceLocation MAN_CRAWLER = new ResourceLocation("man", "managgresive");
    public static final ResourceLocation MAN_BACK = new ResourceLocation("man", "manfromthefogback");
    public static final ResourceLocation DAMNED = new ResourceLocation("apollyon", "the_damned");
    public static final ResourceLocation FALLEN = new ResourceLocation("apollyon", "the_fallen");

    public static final String TAG = "cavemind";
    public static final String OWNER = "cavemind_owner";
    public static final String ONLY = "cavemind_only";

    public static final Map<java.util.UUID, java.util.UUID> ONLY_MAP = new java.util.concurrent.ConcurrentHashMap<>();
    private static final Map<Class<?>, Method> ANIM = new HashMap<>();
    private static final Map<String, Holder<SoundEvent>> SOUNDS = new HashMap<>();

    public static final java.util.Set<java.util.UUID> TEST = new java.util.HashSet<>();

    public static boolean exempt(net.minecraft.world.entity.player.Player p) {
        return p.isSpectator() || p.isCreative() && !TEST.contains(p.getUUID()) && !Cfg.DEV.get();
    }

    public static boolean manLoaded() {
        return ModList.get().isLoaded("man");
    }

    public static boolean apollyonLoaded() {
        return ModList.get().isLoaded("apollyon");
    }

    public static boolean heroLoaded() {
        return ModList.get().isLoaded("watching");
    }

    public static boolean manActive() {
        return Cfg.MAN.get() && manLoaded();
    }

    public static boolean damnedActive() {
        return Cfg.DAMNED.get() && apollyonLoaded();
    }

    public static void takeOver() {
        if (manActive()) {
            off("net.mcreator.man.procedures.TimerglobalProcedure");
            off("net.mcreator.man.procedures.LookProcedure");
            off("net.mcreator.man.procedures.NosleepProcedure");
        }
        if (damnedActive()) {
            off("apollyon.procedures.TheDamnedRisesProcedure");
            off("apollyon.procedures.TheDamnedTargetsProcedure");
        }
        if (apollyonLoaded() && Cfg.QUIET_FALLEN.get()) off("apollyon.procedures.TheFallenJumpscaresProcedure");
    }

    private static void off(String cls) {
        try {
            Class<?> c = Class.forName(cls, false, Compat.class.getClassLoader());
            MinecraftForge.EVENT_BUS.unregister(c);
            CaveMind.LOG.info("[CaveMind] took over {}", cls);
        } catch (Throwable t) {
            CaveMind.LOG.warn("[CaveMind] could not take over {}: {}", cls, t.toString());
        }
    }

    public static EntityType<?> type(ResourceLocation id) {
        return ForgeRegistries.ENTITY_TYPES.containsKey(id) ? ForgeRegistries.ENTITY_TYPES.getValue(id) : null;
    }

    public static boolean is(Entity e, ResourceLocation id) {
        EntityType<?> t = type(id);
        return t != null && e.getType() == t;
    }

    public static boolean managed(Entity e) {
        return e != null && e.getTags().contains(TAG);
    }

    public static String role(Entity e) {
        return managed(e) ? e.getPersistentData().getString(TAG) : "";
    }

    public static void mark(Entity e, String role, java.util.UUID owner, boolean onlyOwner) {
        e.addTag(TAG);
        e.getPersistentData().putString(TAG, role);
        if (owner != null) e.getPersistentData().putUUID(OWNER, owner);
        if (onlyOwner && owner != null) {
            e.getPersistentData().putUUID(ONLY, owner);
            ONLY_MAP.put(e.getUUID(), owner);
        }
    }

    public static void anim(Entity e, String name) {
        try {
            Method m = ANIM.computeIfAbsent(e.getClass(), c -> {
                try {
                    return c.getMethod("setAnimation", String.class);
                } catch (NoSuchMethodException ex) {
                    return null;
                }
            });
            if (m != null) m.invoke(e, name);
        } catch (Throwable ignored) {
        }
    }

    public static Holder<SoundEvent> sound(String id) {
        return SOUNDS.computeIfAbsent(id, k -> {
            ResourceLocation rl = new ResourceLocation(k);
            SoundEvent s = ForgeRegistries.SOUND_EVENTS.getValue(rl);
            if (s == null) return null;
            return ForgeRegistries.SOUND_EVENTS.getHolder(rl).map(h -> (Holder<SoundEvent>) h).orElse(Holder.direct(s));
        });
    }

    public static void grant(net.minecraft.server.level.ServerPlayer p, String id) {
        net.minecraft.advancements.Advancement adv = p.server.getAdvancements().getAdvancement(new ResourceLocation(id));
        if (adv == null) return;
        net.minecraft.advancements.AdvancementProgress prog = p.getAdvancements().getOrStartProgress(adv);
        if (prog.isDone()) return;
        for (String c : prog.getRemainingCriteria()) p.getAdvancements().award(adv, c);
    }

    public static Holder<SoundEvent> sound(String id, String fallback) {
        Holder<SoundEvent> h = sound(id);
        return h != null ? h : sound(fallback);
    }

    private Compat() {}
}
