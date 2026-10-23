package vn.cavemind.client;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.systems.RenderSystem;
import java.util.Random;
import java.util.Set;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.EffectInstance;
import net.minecraft.client.renderer.PostChain;
import net.minecraft.client.renderer.PostPass;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.HitResult;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;
import vn.cavemind.CaveMind;
import vn.cavemind.net.Net;
import vn.cavemind.world.Sounds;

@Mod.EventBusSubscriber(modid = CaveMind.MODID, value = Dist.CLIENT)
public final class MazeHud {
    static final KeyMapping KEY = new KeyMapping("key.cavemind.camera", KeyConflictContext.IN_GAME, InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_N, "key.categories.cavemind");
    static final ResourceLocation POST = new ResourceLocation(CaveMind.MODID, "shaders/post/camcorder.json");
    static final Set<ResourceLocation> HIDE = Set.of(VanillaGuiOverlay.CROSSHAIR.id(), VanillaGuiOverlay.HOTBAR.id(), VanillaGuiOverlay.PLAYER_HEALTH.id(),
        VanillaGuiOverlay.ARMOR_LEVEL.id(), VanillaGuiOverlay.FOOD_LEVEL.id(), VanillaGuiOverlay.EXPERIENCE_BAR.id(), VanillaGuiOverlay.AIR_LEVEL.id(),
        VanillaGuiOverlay.JUMP_BAR.id(), VanillaGuiOverlay.MOUNT_HEALTH.id(), VanillaGuiOverlay.ITEM_NAME.id(), VanillaGuiOverlay.POTION_ICONS.id());

    static boolean on, nv, hiding, exhausted, holding, cam;
    static float slats, battery, danger, stamina = 1, fear, shown, fade, shownStamina = 1, shownFear;
    static int toys, left, tick, nextBeat, nextBreath, nextStatic;
    static long last, since;
    static final Random R = new Random();
    static PostChain chain;
    static PostPass pass;
    static boolean broken;
    static int cw, ch;

    static final float BLACK_FAR = 2.0f, BLACK_NEAR = 0.35f, NV_FAR = 18f;
    static final int FOCUS_TICKS = 9;
    static float near = 99f, shownAura, shownDim, lcdK, shownInk, mist, humVol, ghost, tearY = 0.5f;
    static int camp, focus, focusReady, tear, nextTear, lastMemo = -1, seenIdx;
    static boolean wasHolding, focusSoon;
    static final double[] SEEN = {30, 30, 30, 30, 30};
    static TextureTarget history;
    static Hum hum;

    public static void on(Net.Cam c) {
        Minecraft mc = Minecraft.getInstance();
        if (c.on() && !on) {
            since = System.currentTimeMillis();
            shown = 0;
        }
        if (c.nv() != nv && on && c.on()) static_(mc, 0.18f);
        if (c.nv() && !nv && on && c.on()) focusSoon = true;
        on = c.on();
        nv = c.nv();
        battery = c.battery();
        danger = c.danger();
        toys = c.toys();
        left = c.left();
        hiding = c.hiding();
        stamina = c.stamina();
        fear = c.fear();
        exhausted = c.exhausted();
        holding = c.holding();
        cam = c.cam();
        near = c.near();
        camp = c.camp();
        last = System.currentTimeMillis();
    }

    // camera on but night vision off: the fog closes in at two metres
    static boolean pitchBlack() {
        return active() && cam && !nv;
    }

    static boolean nightVision() {
        return active() && cam && nv;
    }

    static float dim() {
        return shownDim;
    }

    static float lcd() {
        return active() ? lcdK : 0f;
    }

    static void mist(float a) {
        mist = Math.max(mist, Mth.clamp(a, 0f, 1f));
    }

    static boolean active() {
        return on && ClientFx.maze() && System.currentTimeMillis() - last < 4000;
    }

    public static boolean showing() {
        return active();
    }

    @SubscribeEvent
    public static void logout(ClientPlayerNetworkEvent.LoggingOut e) {
        on = false;
        close();
        if (history != null) {
            history.destroyBuffers();
            history = null;
        }
    }

    @SubscribeEvent
    public static void tick(TickEvent.ClientTickEvent e) {
        if (e.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        while (KEY.consumeClick()) {
            if (active() && mc.getConnection() != null && Net.CH.isRemotePresent(mc.getConnection().getConnection())) Net.CH.sendToServer(new Net.CamKey(0));
        }
        boolean act = active();
        fade = Mth.clamp(fade + (act && cam ? 0.05f : -0.04f), 0f, 1f);
        if (fade <= 0f && chain != null) close();
        if (!act) {
            mist = ghost = shownAura = shownDim = lcdK = shownInk = humVol = 0f;
            tear = focus = 0;
            near = 99f;
            return;
        }
        tick++;
        presence(mc);
        lens(mc);
        shown = Math.min(1f, shown + 0.04f);
        shownStamina += (stamina - shownStamina) * 0.25f;
        shownFear += (fear - shownFear) * 0.15f;
        slats += ((hiding ? 1f : 0f) - slats) * 0.2f;
        if (exhausted) {
            mc.player.setSprinting(false);
            mc.options.keySprint.setDown(false);
        }
        float scare = Math.max(danger, fear);
        if (scare > 0.12f && tick >= nextBeat) {
            nextBeat = tick + (int) (34 - 20 * scare);
            play(mc, SoundEvents.WARDEN_HEARTBEAT, 0.95f + scare * 0.15f, 0.12f + scare * 0.7f);
        }
        boolean crouch = mc.player.isCrouching(), sprint = mc.player.isSprinting();
        if (!holding && tick >= nextBreath && (exhausted || crouch && scare > 0.3f || sprint && scare > 0.45f || scare > 0.75f)) {
            nextBreath = tick + (exhausted ? 34 : sprint ? 46 : 80) + R.nextInt(20);
            play(mc, sound("apollyon:shallow_breathing", SoundEvents.HORSE_BREATHE), exhausted ? 1.25f : 1.0f, exhausted ? 0.5f : crouch ? 0.25f : 0.38f);
        }
        if (cam && danger > 0.8f && tick >= nextStatic) {
            nextStatic = tick + 120 + R.nextInt(80);
            static_(mc, 0.2f);
        }
    }

    // The Damned's pressure: a sub-bass hum inside 15 m, the night vision choking and black veins inside 8 m.
    static void presence(Minecraft mc) {
        float aura = near < 8f ? Mth.clamp((8f - near) / 7f, 0f, 1f) : 0f;
        float dim = near < 8f ? 0.3f * Mth.clamp((8f - near) / 3f, 0f, 1f) : 0f;
        shownAura += (aura - shownAura) * 0.08f;
        shownDim += (dim - shownDim) * 0.1f;
        lcdK += ((cam && !nv ? 1f : 0f) - lcdK) * 0.2f;
        shownInk += ((camp >= 1 ? 1f : 0f) - shownInk) * 0.03f;
        float h = near < 15f ? (float) Math.pow((15f - near) / 15f, 1.3) * 0.9f : 0f;
        humVol += (h - humVol) * 0.08f;
        if (humVol > 0.02f && (hum == null || !mc.getSoundManager().isActive(hum))) {
            hum = new Hum(sound(Sounds.HUM, SoundEvents.BEACON_AMBIENT));
            mc.getSoundManager().play(hum);
        }
    }

    // Lens behaviour: autofocus hunting, breath fogging the glass, tape tearing when the fear runs high.
    static void lens(Minecraft mc) {
        if (focus > 0) focus--;
        if (mist > 0f) mist = Math.max(0f, mist - 0.011f);
        if (hiding && wasHolding && !holding) mist(0.9f);
        wasHolding = holding;
        if (hiding && exhausted && tick % 50 == 0) mist(0.65f);
        if (focusSoon) {
            focusSoon = false;
            hunt(mc);
        }
        if (tick % 4 == 0 && mc.player != null) {
            HitResult hit = mc.player.pick(28.0, 1f, false);
            double d = hit.getLocation().distanceTo(mc.player.getEyePosition());
            double shortest = d;
            for (double v : SEEN) shortest = Math.min(shortest, v);
            SEEN[seenIdx++ % SEEN.length] = d;
            if (cam && d > 9 && shortest < 4.5 && tick >= focusReady) hunt(mc);
        }
        if (tear > 0) tear--;
        ghost = Math.max(0f, ghost - 0.025f);
        if (cam && shownFear > 0.7f && tick >= nextTear) {
            nextTear = tick + 40 + R.nextInt(70);
            tear = 7;
            tearY = 0.12f + R.nextFloat() * 0.76f;
            ghost = 0.6f;
            play(mc, sound(Sounds.TAPE, SoundEvents.UI_BUTTON_CLICK.value()), 0.9f + R.nextFloat() * 0.2f, 0.22f);
        }
    }

    static void hunt(Minecraft mc) {
        focus = FOCUS_TICKS;
        focusReady = tick + 70;
        play(mc, sound(Sounds.FOCUS, SoundEvents.UI_BUTTON_CLICK.value()), 0.95f + R.nextFloat() * 0.1f, 0.3f);
    }

    static float focusBlur(float pt) {
        if (focus <= 0) return 0f;
        float t = Mth.clamp((focus - pt) / FOCUS_TICKS, 0f, 1f);
        float v = t > 0.6f ? 1f : t > 0.42f ? 0.4f : t > 0.24f ? 0.8f : t / 0.24f * 0.45f;
        return nv ? v : v * 0.6f;
    }

    static final class Hum extends AbstractTickableSoundInstance {
        Hum(SoundEvent s) {
            super(s, SoundSource.HOSTILE, RandomSource.create());
            looping = true;
            delay = 0;
            volume = 0.02f;
            relative = true;
            attenuation = SoundInstance.Attenuation.NONE;
        }

        @Override
        public boolean canStartSilent() {
            return true;
        }

        @Override
        public void tick() {
            if (!active()) {
                stop();
                return;
            }
            volume = Math.max(0.0001f, humVol);
        }
    }

    // F1 would strip the camcorder off the screen; survival players keep the HUD.
    @SubscribeEvent
    public static void noHideGui(TickEvent.RenderTickEvent e) {
        if (e.phase != TickEvent.Phase.START) return;
        Minecraft mc = Minecraft.getInstance();
        if (!mc.options.hideGui || mc.player == null || mc.player.isCreative() || mc.player.isSpectator()) return;
        mc.options.hideGui = false;
    }

    static void static_(Minecraft mc, float vol) {
        play(mc, sound("man:white_noise_passby", SoundEvents.UI_BUTTON_CLICK.value()), 1.4f, vol);
    }

    static SoundEvent sound(String id, SoundEvent fallback) {
        SoundEvent s = ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation(id));
        return s != null ? s : fallback;
    }

    static void play(Minecraft mc, SoundEvent s, float pitch, float vol) {
        mc.getSoundManager().play(SimpleSoundInstance.forUI(s, pitch, vol));
    }

    @SubscribeEvent
    public static void hide(RenderGuiOverlayEvent.Pre e) {
        if (active() && HIDE.contains(e.getOverlay().id())) e.setCanceled(true);
    }

    @SubscribeEvent
    public static void sway(ViewportEvent.ComputeCameraAngles e) {
        if (!active()) return;
        float t = (tick + (float) e.getPartialTick()) * 0.05f;
        float scare = Math.max(danger, fear);
        float shake = scare > 0.55f ? (scare - 0.55f) * 1.4f : 0f;
        float k = cam ? 1f : 0.4f;
        e.setRoll(e.getRoll() + (Mth.sin(t * 0.9f) * 0.4f + Mth.sin(t * 7.3f) * shake * 0.3f) * k);
        e.setPitch(e.getPitch() + (Mth.sin(t * 0.63f) * 0.18f + Mth.cos(t * 6.1f) * shake * 0.22f) * k);
        e.setYaw(e.getYaw() + Mth.sin(t * 0.47f) * 0.2f * k);
    }

    @SubscribeEvent
    public static void fov(ViewportEvent.ComputeFov e) {
        if (active() && cam && e.usedConfiguredFov()) e.setFOV(e.getFOV() * (1.06 + fear * 0.04));
    }

    static void remember(Minecraft mc) {
        RenderTarget main = mc.getMainRenderTarget();
        if (history == null || history.width != main.width || history.height != main.height) {
            if (history != null) history.destroyBuffers();
            history = new TextureTarget(main.width, main.height, false, Minecraft.ON_OSX);
        }
        GlStateManager._glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, main.frameBufferId);
        GlStateManager._glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, history.frameBufferId);
        GlStateManager._glBlitFrameBuffer(0, 0, main.width, main.height, 0, 0, history.width, history.height, GL11.GL_COLOR_BUFFER_BIT, GL11.GL_NEAREST);
        GlStateManager._glBindFramebuffer(GL30.GL_FRAMEBUFFER, 0);
    }

    static void close() {
        if (chain != null) {
            try {
                chain.close();
            } catch (Throwable ignored) {
            }
        }
        chain = null;
        pass = null;
        cw = ch = 0;
    }

    static boolean chain(Minecraft mc) {
        if (broken) return false;
        int w = mc.getWindow().getWidth(), h = mc.getWindow().getHeight();
        try {
            if (chain == null) {
                chain = new PostChain(mc.getTextureManager(), mc.getResourceManager(), mc.getMainRenderTarget(), POST);
                var swap = chain.getTempTarget("swap");
                pass = chain.addPass("cavemind:camcorder", mc.getMainRenderTarget(), swap);
                pass.addAuxAsset("HistorySampler", () -> history != null ? history.getColorTextureId() : mc.getMainRenderTarget().getColorTextureId(), w, h);
                chain.addPass("blit", swap, mc.getMainRenderTarget());
                cw = ch = 0;
            }
            if (w != cw || h != ch) {
                chain.resize(w, h);
                cw = w;
                ch = h;
            }
            return true;
        } catch (Throwable t) {
            CaveMind.LOG.warn("[CaveMind] camcorder post effect disabled", t);
            broken = true;
            close();
            return false;
        }
    }

    static void post(Minecraft mc, float pt) {
        if (fade <= 0f || !chain(mc)) return;
        EffectInstance fx = pass.getEffect();
        float scare = Math.max(danger, fear);
        float breathe = Mth.sin((tick + pt) * (0.08f + scare * 0.12f)) * 0.5f + 0.5f;
        fx.safeGetUniform("Warp").set(0.17f + scare * 0.07f + breathe * scare * 0.03f);
        fx.safeGetUniform("Chroma").set(0.0035f + danger * 0.006f + SeekFx.scan * 0.012f);
        fx.safeGetUniform("Grain").set((nv ? 0.075f : 0.045f + danger * 0.03f) + SeekFx.scan * 0.32f);
        fx.safeGetUniform("Night").set(nv ? (battery < 0.1f && R.nextFloat() < 0.12f ? 0.55f : 1f) : 0f);
        fx.safeGetUniform("Danger").set(danger * (0.55f + 0.45f * breathe));
        fx.safeGetUniform("Fear").set(fear);
        fx.safeGetUniform("Seed").set((tick % 97) + R.nextFloat());
        fx.safeGetUniform("Fade").set(fade);
        fx.safeGetUniform("Time").set((tick + pt) / 20f);
        fx.safeGetUniform("Focus").set(focusBlur(pt));
        fx.safeGetUniform("Mist").set(mist);
        fx.safeGetUniform("Tear").set(tear > 0 ? Math.min(1f, 0.3f + (tear - pt) / 3f) : 0f);
        fx.safeGetUniform("TearY").set(tearY);
        fx.safeGetUniform("Ghost").set(ghost);
        fx.safeGetUniform("Aura").set(shownAura);
        fx.safeGetUniform("Dim").set(shownDim);
        fx.safeGetUniform("Lcd").set(lcdK);
        fx.safeGetUniform("Ink").set(shownInk);
        try {
            if (tick != lastMemo && tick % 10 == 0) {
                lastMemo = tick;
                remember(mc);
            }
            RenderSystem.disableBlend();
            RenderSystem.disableDepthTest();
            RenderSystem.resetTextureMatrix();
            chain.process(pt);
        } catch (Throwable t) {
            CaveMind.LOG.warn("[CaveMind] camcorder post effect failed", t);
            broken = true;
            close();
        }
        mc.getMainRenderTarget().bindWrite(true);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.enableDepthTest();
    }

    static void render(GuiGraphics g, float pt, int w, int h) {
        Minecraft mc = Minecraft.getInstance();
        post(mc, pt);
        if (!active()) return;
        float k = shown;
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        if (slats > 0.02f) {
            int sa = (int) (slats * 225) << 24;
            int gap = Math.max(4, h / 26), bar = gap * 2;
            for (int y = 0; y < h; y += gap + bar) g.fillGradient(0, y, w, y + bar, sa, (int) (slats * 190) << 24);
            g.fill(0, 0, w / 7, h, sa);
            g.fill(w - w / 7, 0, w, h, sa);
            scratches(g, w, h, slats);
        }
        if (cam && danger > 0.7f && R.nextFloat() < danger * 0.2f) {
            int y = R.nextInt(Math.max(1, h)), bh = 1 + R.nextInt(2);
            g.fill(0, y, w, y + bh, (40 + R.nextInt(60)) << 24 | 0xFFFFFF);
        }
        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
        ui(g, mc.font, w, h, k);
    }

    // Fingernail gouges on the inside of the door, left by whoever hid here before.
    static void scratches(GuiGraphics g, int w, int h, float k) {
        Random r = new Random(4848);
        int a = (int) (k * 120);
        for (int i = 0; i < 24; i++) {
            float x0 = r.nextFloat() * w, y0 = r.nextFloat() * h * 0.85f;
            float ang = (float) (Math.PI * (0.32 + r.nextFloat() * 0.36));
            float len = h * (0.07f + r.nextFloat() * 0.17f);
            int col = (a * (55 + r.nextInt(45)) / 100) << 24 | (r.nextBoolean() ? 0xC8B49A : 0x9A8268);
            int nails = 3 + r.nextInt(2), steps = Math.max(2, (int) (len / 2));
            for (int n = 0; n < nails; n++) {
                float ox = n * 3.2f, oy = n * 0.9f;
                for (int s = 0; s < steps; s++) {
                    float t = s / (float) steps;
                    int px = (int) (x0 + ox + Mth.cos(ang) * len * t + Mth.sin(t * 9f + i) * 1.2f), py = (int) (y0 + oy + Mth.sin(ang) * len * t);
                    g.fill(px, py, px + 1, py + 2, col);
                }
            }
        }
    }

    static void bar(GuiGraphics g, Font f, int x, int y, int bw, String label, float v, int col, int a, boolean blink) {
        int ta = blink ? (int) (a * (0.45f + 0.55f * (0.5f + 0.5f * Mth.sin(tick * 0.5f)))) : a;
        g.drawString(f, label, x, y, ta << 24 | 0xC8C8C8, false);
        int bx = x + 26, by = y + 3;
        g.fill(bx - 1, by - 1, bx + bw + 1, by + 4, (a * 2 / 3) << 24);
        int fill = Math.round(bw * Mth.clamp(v, 0, 1));
        g.fill(bx, by, bx + fill, by + 3, ta << 24 | col);
        for (int i = 1; i < 4; i++) g.fill(bx + bw * i / 4, by, bx + bw * i / 4 + 1, by + 3, (a / 2) << 24);
    }

    static void ui(GuiGraphics g, Font f, int w, int h, float k) {
        int a = (int) (k * 210);
        int white = a << 24 | 0xE8E8E8, m = 12, l = 16;
        if (cam) {
            g.fill(m, m, m + l, m + 1, white);
            g.fill(m, m, m + 1, m + l, white);
            g.fill(w - m - l, m, w - m, m + 1, white);
            g.fill(w - m - 1, m, w - m, m + l, white);
            g.fill(m, h - m - 1, m + l, h - m, white);
            g.fill(m, h - m - l, m + 1, h - m, white);
            g.fill(w - m - l, h - m - 1, w - m, h - m, white);
            g.fill(w - m - 1, h - m - l, w - m, h - m, white);
            long ms = System.currentTimeMillis() - since;
            if ((ms / 600) % 2 == 0) g.fill(m + 8, m + 9, m + 13, m + 14, a << 24 | 0xD01818);
            g.drawString(f, "REC", m + 17, m + 8, white, false);
            long s = ms / 1000;
            g.pose().pushPose();
            g.pose().translate(m + 8, m + 20, 0);
            g.pose().scale(0.75f, 0.75f, 1);
            g.drawString(f, String.format("%02d:%02d:%02d", s / 3600, s / 60 % 60, s % 60), 0, 0, a << 24 | 0xA8A8A8, false);
            g.pose().popPose();
            int bx = w - m - 34, by = m + 8;
            int col = battery < 0.2f ? a << 24 | 0xD03030 : white;
            g.fill(bx, by, bx + 22, by + 1, col);
            g.fill(bx, by + 9, bx + 22, by + 10, col);
            g.fill(bx, by, bx + 1, by + 10, col);
            g.fill(bx + 21, by, bx + 22, by + 10, col);
            g.fill(bx + 22, by + 3, bx + 24, by + 7, col);
            int segs = (int) Math.ceil(battery * 4 - 0.001);
            for (int i = 0; i < segs; i++) g.fill(bx + 2 + i * 5, by + 2, bx + 6 + i * 5, by + 8, col);
            String pct = Math.round(battery * 100) + "%";
            g.drawString(f, pct, bx - 3 - f.width(pct), by + 1, col, false);
            if (nv) g.drawString(f, "NV", bx + 2, by + 13, a << 24 | 0x60F060, false);
            if (battery < 0.1f && (ms / 400) % 2 == 0) g.drawString(f, "PIN YẾU", w / 2 - f.width("PIN YẾU") / 2, m + 8, a << 24 | 0xD03030, false);
        }
        int bw = 70, bx = m + 8, by = h - m - 30;
        bar(g, f, bx, by, bw, "SỨC", shownStamina, exhausted ? 0x8A3A3A : 0xD8D8D8, a, exhausted);
        bar(g, f, bx, by + 11, bw, "SỢ", shownFear, shownFear > 0.75f ? 0xE03030 : 0x9A2A2A, a, shownFear > 0.85f);
        int cx = w / 2, ty = h - m - 14;
        for (int i = 0; i < 7; i++) {
            int x = cx - 7 * 6 + i * 12;
            boolean got = i < toys;
            g.fill(x, ty, x + 8, ty + 8, (a / 2) << 24);
            if (got) g.fill(x + 1, ty + 1, x + 7, ty + 7, a << 24 | 0xD8C8A8);
            else g.fill(x + 1, ty + 1, x + 7, ty + 7, (a / 3) << 24 | 0x404040);
        }
        String time = left / 60 + ":" + String.format("%02d", left % 60);
        g.drawString(f, time, cx - f.width(time) / 2, ty - 11, a << 24 | (left < 60 ? 0xD06060 : 0x9A9A9A), false);
        String hint = (cam ? "[" + KEY.getTranslatedKeyMessage().getString() + "] nhìn đêm   " : "") + "[" + BookClient.KEY.getTranslatedKeyMessage().getString() + "] sổ";
        g.pose().pushPose();
        g.pose().translate(w - m - 8 - f.width(hint) * 0.75f, h - m - 12, 0);
        g.pose().scale(0.75f, 0.75f, 1);
        g.drawString(f, hint, 0, 0, a << 24 | 0x707070, false);
        g.pose().popPose();
        if (holding) {
            String s = "đang nín thở";
            int ta = (int) (a * (0.6f + 0.4f * Mth.sin(tick * 0.3f)));
            g.drawString(f, s, cx - f.width(s) / 2, h / 2 + 24, ta << 24 | 0xC0C0C0, false);
        } else if (exhausted) {
            String s = "hụt hơi";
            g.drawString(f, s, cx - f.width(s) / 2, h / 2 + 24, a << 24 | 0xB05050, false);
        }
    }

    @Mod.EventBusSubscriber(modid = CaveMind.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
    public static final class Setup {
        @SubscribeEvent
        public static void keys(RegisterKeyMappingsEvent e) {
            e.register(KEY);
            e.register(BookClient.KEY);
        }

        @SubscribeEvent
        public static void sky(net.minecraftforge.client.event.RegisterDimensionSpecialEffectsEvent e) {
            e.register(new ResourceLocation(CaveMind.MODID, "gio_48"), new MazeSky());
        }

        @SubscribeEvent
        public static void overlays(RegisterGuiOverlaysEvent e) {
            e.registerBelowAll("maze_cam", (gui, g, pt, w, h) -> render(g, pt, w, h));
            e.registerBelowAll("seek_fx", (gui, g, pt, w, h) -> SeekFx.render(g, pt, w, h));
            e.registerAbove(VanillaGuiOverlay.BOSS_EVENT_PROGRESS.id(), "man_bar", (gui, g, pt, w, h) -> ManBar.render(g, pt, w, h));
        }

        @SubscribeEvent
        public static void setup(net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent e) {
            e.enqueueWork(() -> net.minecraft.client.renderer.item.ItemProperties.register(vn.cavemind.gear.Gear.BARO.get(), new net.minecraft.resources.ResourceLocation("cavemind", "fog"),
                (st, lvl, ent, seed) -> st.hasTag() ? st.getTag().getInt(vn.cavemind.gear.Barometer.FOG) / 2f : 0f));
            e.enqueueWork(() -> net.minecraft.client.renderer.item.ItemProperties.register(vn.cavemind.gear.Gear.LISTEN.get(), new net.minecraft.resources.ResourceLocation("cavemind", "scan"),
                (st, lvl, ent, seed) -> SeekFx.scanTicks > 0 ? (SeekFx.scan > 0.35f ? 1f : 0.5f) : ent != null && ent.isUsingItem() && ent.getUseItem().is(st.getItem()) ? 0.5f : 0f));
        }
    }

    private MazeHud() {}
}
