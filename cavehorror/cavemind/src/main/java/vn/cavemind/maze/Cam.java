package vn.cavemind.maze;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import vn.cavemind.Cfg;
import vn.cavemind.maze.core.Round;
import vn.cavemind.net.Net;
import vn.cavemind.world.Fx;

public final class Cam {
    public static final double DRAIN = 1.0 / (20 * 95);
    static final UUID SLOW = UUID.fromString("6c1f0d3a-4a1e-4f7e-9b1a-0c48c48c4801");
    static final String HIDE = "§n§o§m§i§n§i§m§a§p", SHOW = "§r§e§s§e§t§x§a§e§r§o";

    static final class St {
        double battery = 0.5;
        boolean nv, warned;
        float danger, fear, stamina = 1;
        int toys, left;
        boolean hiding, exhausted, holding;
        float near = 99f;
        int camp;
    }

    private static final Map<UUID, St> ST = new HashMap<>();

    public static boolean on() {
        return Cfg.MAZE_CAM.get();
    }

    public static void clear() {
        ST.clear();
    }

    static void enter(ServerPlayer p) {
        St s = new St();
        ST.put(p.getUUID(), s);
        var speed = p.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED);
        if (speed != null && speed.getModifier(SLOW) == null) {
            speed.addTransientModifier(new net.minecraft.world.entity.ai.attributes.AttributeModifier(SLOW, "cavemind maze pace", -0.15,
                net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.MULTIPLY_TOTAL));
        }
        if (on()) p.displayClientMessage(Component.literal(HIDE), true);
        send(p, s);
        p.server.execute(() -> p.displayClientMessage(Component.literal("Máy quay cũ đang chạy. Bấm [N] để bật nhìn đêm, nó ăn pin rất nhanh.").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC), true));
    }

    static void exit(ServerPlayer p) {
        var speed = p.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED);
        if (speed != null) speed.removeModifier(SLOW);
        St s = ST.remove(p.getUUID());
        if (s == null) return;
        if (s.nv) p.removeEffect(MobEffects.NIGHT_VISION);
        p.removeEffect(MobEffects.MOVEMENT_SLOWDOWN);
        p.displayClientMessage(Component.literal(SHOW), true);
        Net.send(p, Net.Cam.OFF);
    }

    public static void forget(ServerPlayer p) {
        ST.remove(p.getUUID());
    }

    static boolean nightVision(ServerPlayer p) {
        St s = ST.get(p.getUUID());
        return s != null && s.nv;
    }

    public static boolean panting(ServerPlayer p) {
        St s = ST.get(p.getUUID());
        return s != null && (s.fear > 0.82f || s.exhausted);
    }

    public static void key(ServerPlayer p, int op) {
        St s = ST.get(p.getUUID());
        if (s == null || !Maze.inMaze(p) || !on()) return;
        if (s.nv) {
            nv(p, s, false);
            Fx.sound(p, "minecraft:ui.button.click", SoundSource.PLAYERS, p.position(), 0.25f, 1.6f);
        } else if (s.battery > 0.005) {
            nv(p, s, true);
            Fx.sound(p, "minecraft:block.beacon.power_select", SoundSource.PLAYERS, p.position(), 0.18f, 2.0f);
        } else {
            p.displayClientMessage(Component.literal("Hết pin. Tìm pin trong bóng tối.").withStyle(ChatFormatting.DARK_RED, ChatFormatting.ITALIC), true);
            Fx.sound(p, "minecraft:block.dispenser.fail", SoundSource.PLAYERS, p.position(), 0.3f, 1.8f);
        }
        send(p, s);
    }

    static void nv(ServerPlayer p, St s, boolean on) {
        s.nv = on;
        if (on) p.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 300, 0, false, false, false));
        else p.removeEffect(MobEffects.NIGHT_VISION);
    }

    static void tick(ServerPlayer p, float danger, int toys, int left, boolean hiding, long now, Round.Runner rn, boolean seen, float near, int camp) {
        St s = ST.get(p.getUUID());
        if (s == null) return;
        s.danger = danger;
        s.near = near;
        s.camp = camp;
        s.toys = toys;
        s.left = left;
        s.hiding = hiding;
        if (rn != null) {
            s.stamina = (float) rn.stamina;
            s.exhausted = rn.exhausted;
            s.holding = rn.holding;
        }
        float target = Math.max(danger * 0.95f, s.nv ? 0.06f : 0.3f);
        if (seen) target = 1f;
        if (hiding && danger < 0.4f) target = Math.min(target, 0.15f);
        s.fear += target > s.fear ? Math.min(0.03f, target - s.fear) : -Math.min(hiding ? 0.008f : 0.004f, s.fear - target);
        if (s.nv) {
            s.battery = Math.max(0, s.battery - DRAIN);
            if (now % 40 == 0) p.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 300, 0, false, false, false));
            if (s.battery < 0.15 && !s.warned) {
                s.warned = true;
                p.displayClientMessage(Component.literal("Pin yếu.").withStyle(ChatFormatting.DARK_RED, ChatFormatting.ITALIC), true);
            }
            if (s.battery <= 0) {
                nv(p, s, false);
                Fx.sound(p, "minecraft:block.beacon.deactivate", SoundSource.PLAYERS, p.position(), 0.35f, 1.7f);
                p.displayClientMessage(Component.literal("Máy quay tắt nhìn đêm. Hết pin.").withStyle(ChatFormatting.DARK_RED, ChatFormatting.ITALIC), true);
            }
        }
        if (now % 4 == 0) send(p, s);
    }

    public static boolean drain(ServerPlayer p, double amount) {
        St s = ST.get(p.getUUID());
        if (s == null || s.battery <= 0.005) return false;
        s.battery = Math.max(0, s.battery - amount);
        return true;
    }

    static void battery(ServerPlayer p) {
        St s = ST.get(p.getUUID());
        if (s == null) return;
        s.battery = Math.min(1.0, s.battery + 0.35);
        s.warned = false;
        Fx.sound(p, "minecraft:item.armor.equip_iron", SoundSource.PLAYERS, p.position(), 0.5f, 1.5f);
        p.displayClientMessage(Component.literal("Nhặt được pin. Máy quay còn " + (int) Math.round(s.battery * 100) + "%.").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC), true);
        send(p, s);
    }

    static void send(ServerPlayer p, St s) {
        Net.send(p, new Net.Cam(true, (float) s.battery, s.nv, s.danger, s.toys, s.left, s.hiding, s.stamina, s.fear, s.exhausted, s.holding, on(), s.near, s.camp));
    }

    private Cam() {}
}
