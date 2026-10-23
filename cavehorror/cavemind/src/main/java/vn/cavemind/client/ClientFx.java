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
    private static float seenFog = -1, sentFog = -1;
    private static int sentRender = -1;
    private static int sentShader = -1;
    private static Boolean irisApi;
    private static java.lang.reflect.Method irisInUse;
    private static Object irisInst;

    public static void on(int kind, float a, int ticks) {
        switch (kind) {
            case Net.FOG -> {
                fogTarget = Mth.clamp(a, 0f, 2f);
                fogStep = Math.max(0.002f, Math.abs(fogTarget - fog) / Math.max(1, ticks));
                if (fogTarget < fog) fogStep = Math.max(fogStep, (fog - fogTarget) / 60f);
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
            case Net.DARK -> SeekFx.dark(a, ticks);
            case Net.HOLD -> SeekFx.hold(a, ticks);
            case Net.SCAN -> SeekFx.scan(a, ticks);
            case Net.MIST -> MazeHud.mist(a);
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
        if (++tick % 40 == 0) {
            sendView(mc);
            sendSense(mc);
        }
    }

    @SubscribeEvent
    public static void login(ClientPlayerNetworkEvent.LoggingIn e) {
        sentFov = -1;
        sentFog = -1;
        sentRender = -1;
        sentShader = -1;
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

    private static void sendSense(Minecraft mc) {
        if (mc.getConnection() == null || !Net.CH.isRemotePresent(mc.getConnection().getConnection())) return;
        int render = mc.options.getEffectiveRenderDistance();
        boolean shader = shaders();
        float f = seenFog > 0 ? seenFog : render * 16f;
        if (Math.abs(f - sentFog) < 3f && render == sentRender && (shader ? 1 : 0) == sentShader) return;
        Net.CH.sendToServer(new Net.Sense(f, render, shader));
        sentFog = f;
        sentRender = render;
        sentShader = shader ? 1 : 0;
    }

    private static boolean shaders() {
        try {
            if (irisApi == null) {
                irisApi = false;
                for (String cn : new String[]{"net.irisshaders.iris.api.v0.IrisApi", "net.coderbot.iris.apiimpl.IrisApiV0Impl"}) {
                    try {
                        Class<?> c = Class.forName(cn);
                        irisInst = c.getMethod("getInstance").invoke(null);
                        irisInUse = irisInst.getClass().getMethod("isShaderPackInUse");
                        irisApi = true;
                        break;
                    } catch (Throwable ignored) {
                    }
                }
            }
            return irisApi && (Boolean) irisInUse.invoke(irisInst);
        } catch (Throwable t) {
            return false;
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST, receiveCanceled = true)
    public static void measure(ViewportEvent.RenderFog e) {
        if (e.getMode() != FogRenderer.FogMode.FOG_TERRAIN || fog > 0.001f) return;
        float far = e.getFarPlaneDistance();
        seenFog = seenFog < 0 ? far : seenFog + (far - seenFog) * 0.05f;
    }

    static boolean maze() {
        Minecraft mc = Minecraft.getInstance();
        return mc.level != null && mc.level.dimension().location().getNamespace().equals(CaveMind.MODID) && mc.level.dimension().location().getPath().equals("gio_48");
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void fog(ViewportEvent.RenderFog e) {
        if (maze()) {
            float far, near;
            if (MazeHud.pitchBlack()) {
                // camera off: past two metres there is nothing but black, whatever the gamma says
                far = MazeHud.BLACK_FAR;
                near = MazeHud.BLACK_NEAR;
            } else if (MazeHud.nightVision()) {
                far = MazeHud.NV_FAR * (1f - MazeHud.dim());
                near = far * 0.3f;
            } else {
                far = fog > 0.001f ? 12f : 22f;
                near = 0.5f;
            }
            e.setFarPlaneDistance(Math.min(e.getFarPlaneDistance(), far));
            e.setNearPlaneDistance(Math.min(e.getNearPlaneDistance(), near));
            e.setFogShape(FogShape.SPHERE);
            e.setCanceled(true);
            return;
        }
        if (SeekFx.dark > 0.001f && fog <= 1.001f && e.getMode() == FogRenderer.FogMode.FOG_TERRAIN) {
            float far = SeekFx.fogFar(e.getFarPlaneDistance());
            e.setFarPlaneDistance(far);
            e.setNearPlaneDistance(Math.min(e.getNearPlaneDistance(), far * 0.15f));
            e.setFogShape(FogShape.SPHERE);
            e.setCanceled(true);
            return;
        }
        if (fog <= 1.001f || e.getMode() != FogRenderer.FogMode.FOG_TERRAIN) return;
        float far = e.getFarPlaneDistance();
        float target = Mth.lerp(Math.min(1f, fog - 1f), far, Math.max(24f, far * 0.22f));
        e.setFarPlaneDistance(Math.min(far, target));
        e.setNearPlaneDistance(Math.min(e.getNearPlaneDistance(), target * 0.1f));
        e.setFogShape(FogShape.SPHERE);
        e.setCanceled(true);
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void fogColor(ViewportEvent.ComputeFogColor e) {
        if (maze()) {
            float k = MazeHud.showing() ? 0f : 1f;
            e.setRed(0.035f * k);
            e.setGreen(0.03f * k);
            e.setBlue(0.028f * k);
            return;
        }
        if (SeekFx.dark > 0.001f) {
            float d = SeekFx.dark * 0.85f;
            e.setRed(Mth.lerp(d, e.getRed(), 0.055f));
            e.setGreen(Mth.lerp(d, e.getGreen(), 0.06f));
            e.setBlue(Mth.lerp(d, e.getBlue(), 0.075f));
        }
        if (fog <= 0.001f) return;
        float k = Math.min(1f, fog) * 0.35f;
        float grey = (e.getRed() + e.getGreen() + e.getBlue()) / 3f * 1.04f;
        e.setRed(Mth.lerp(k, e.getRed(), grey));
        e.setGreen(Mth.lerp(k, e.getGreen(), grey));
        e.setBlue(Mth.lerp(k, e.getBlue(), grey + 0.02f));
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
