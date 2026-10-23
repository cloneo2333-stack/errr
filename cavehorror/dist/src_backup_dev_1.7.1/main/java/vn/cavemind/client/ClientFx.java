package vn.cavemind.client;

import com.mojang.blaze3d.shaders.FogShape;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.FogRenderer;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.client.event.sound.PlaySoundEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import vn.cavemind.CaveMind;
import vn.cavemind.net.Net;

@Mod.EventBusSubscriber(modid = CaveMind.MODID, value = Dist.CLIENT)
public final class ClientFx {
    private static float fogTarget, fog, fogStep = 0.01f;
    private static int silenceTicks;
    private static int shakeTicks;
    private static float shakeAmp;
    private static int pulseTicks, pulseLen;
    private static float pulseAmp;
    private static float sentFov = -1, sentAspect = -1;
    private static int tick;

    public static void on(int kind, float a, int ticks) {
        switch (kind) {
            case Net.FOG -> {
                fogTarget = Mth.clamp(a, 0f, 2f);
                fogStep = Math.max(0.002f, Math.abs(fogTarget - fog) / Math.max(1, ticks));
            }
            case Net.SILENCE -> {
                silenceTicks = Math.max(silenceTicks, ticks);
                Minecraft mc = Minecraft.getInstance();
                mc.getMusicManager().stopPlaying();
                mc.getSoundManager().stop(null, SoundSource.AMBIENT);
                mc.getSoundManager().stop(null, SoundSource.MUSIC);
                mc.getSoundManager().stop(null, SoundSource.WEATHER);
            }
            case Net.SHAKE -> {
                shakeTicks = Math.max(shakeTicks, ticks);
                shakeAmp = a;
            }
            case Net.PULSE -> {
                pulseTicks = pulseLen = Math.max(1, ticks);
                pulseAmp = a;
            }
            default -> {}
        }
    }

    @SubscribeEvent
    public static void tick(TickEvent.ClientTickEvent e) {
        if (e.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            fog = fogTarget = 0;
            silenceTicks = 0;
            return;
        }
        if (fog < fogTarget) fog = Math.min(fogTarget, fog + fogStep);
        else if (fog > fogTarget) fog = Math.max(fogTarget, fog - fogStep);
        if (silenceTicks > 0) silenceTicks--;
        if (shakeTicks > 0) shakeTicks--;
        if (pulseTicks > 0) pulseTicks--;
        if (++tick % 40 == 0) sendView(mc);
    }

    @SubscribeEvent
    public static void login(ClientPlayerNetworkEvent.LoggingIn e) {
        sentFov = -1;
        fog = fogTarget = 0;
    }

    private static void sendView(Minecraft mc) {
        float fov = mc.options.fov().get();
        float aspect = mc.getWindow().getHeight() > 0 ? (float) mc.getWindow().getWidth() / mc.getWindow().getHeight() : 16f / 9f;
        if (Math.abs(fov - sentFov) > 0.5f || Math.abs(aspect - sentAspect) > 0.01f) {
            if (mc.getConnection() != null && Net.CH.isRemotePresent(mc.getConnection().getConnection())) {
                Net.CH.sendToServer(new Net.View(fov, aspect));
                sentFov = fov;
                sentAspect = aspect;
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void fog(ViewportEvent.RenderFog e) {
        if (fog <= 0.001f || e.getMode() != FogRenderer.FogMode.FOG_TERRAIN) return;
        float far = e.getFarPlaneDistance();
        float target = fog <= 1f ? Mth.lerp(fog, far, 20f) : Mth.lerp(fog - 1f, 20f, 6f);
        e.setFarPlaneDistance(Math.min(far, target));
        e.setNearPlaneDistance(Math.min(e.getNearPlaneDistance(), target * 0.1f));
        e.setFogShape(FogShape.SPHERE);
        e.setCanceled(true);
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void fogColor(ViewportEvent.ComputeFogColor e) {
        if (fog <= 0.001f) return;
        float k = fog * 0.55f;
        float grey = 0.52f;
        e.setRed(Mth.lerp(k, e.getRed(), grey));
        e.setGreen(Mth.lerp(k, e.getGreen(), grey));
        e.setBlue(Mth.lerp(k, e.getBlue(), grey + 0.03f));
    }

    @SubscribeEvent
    public static void camera(ViewportEvent.ComputeCameraAngles e) {
        float pt = (float) e.getPartialTick();
        if (shakeTicks > 0) {
            float t = (Minecraft.getInstance().player.tickCount + pt) * 1.7f;
            float a = shakeAmp * Math.min(1f, shakeTicks / 10f);
            e.setRoll(e.getRoll() + Mth.sin(t * 2.3f) * a);
            e.setYaw(e.getYaw() + Mth.sin(t * 1.3f) * a * 0.4f);
            e.setPitch(e.getPitch() + Mth.cos(t * 1.9f) * a * 0.3f);
        }
        if (pulseTicks > 0) {
            float f = 1f - pulseTicks / (float) pulseLen;
            e.setRoll(e.getRoll() + Mth.sin(f * Mth.PI) * pulseAmp);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void sound(PlaySoundEvent e) {
        if (silenceTicks <= 0) return;
        SoundInstance s = e.getSound();
        if (s == null) return;
        String ns = s.getLocation().getNamespace();
        SoundSource src = s.getSource();
        boolean scenery = ns.equals("minecraft") || ns.equals("ambientsounds") || ns.equals("drippyloadingscreen");
        if (src == SoundSource.MUSIC || src == SoundSource.RECORDS && ns.equals("minecraft")
            || scenery && (src == SoundSource.AMBIENT || src == SoundSource.WEATHER || src == SoundSource.NEUTRAL)) {
            e.setSound(null);
        }
    }

    private ClientFx() {}
}
