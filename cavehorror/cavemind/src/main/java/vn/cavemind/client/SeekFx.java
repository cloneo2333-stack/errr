package vn.cavemind.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import vn.cavemind.CaveMind;

@Mod.EventBusSubscriber(modid = CaveMind.MODID, value = Dist.CLIENT)
public final class SeekFx {
    static float dark, darkTarget, darkStep = 0.02f;
    static int hold, holdLen, flash, scanTicks;
    static float scan, scanTarget;
    static boolean holdLong;

    static void dark(float a, int ticks) {
        darkTarget = Mth.clamp(a, 0f, 1f);
        darkStep = Math.max(0.004f, Math.abs(darkTarget - dark) / Math.max(1, ticks));
    }

    static void scan(float a, int ticks) {
        scanTarget = Mth.clamp(a, 0f, 1f);
        scanTicks = ticks;
    }

    static void hold(float a, int ticks) {
        if (a > 0) {
            hold = holdLen = Math.max(1, ticks);
            holdLong = a >= 1f;
        } else {
            hold = 0;
            if (a < 0) flash = 16;
        }
    }

    @SubscribeEvent
    public static void tick(TickEvent.ClientTickEvent e) {
        if (e.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            dark = darkTarget = 0;
            hold = flash = 0;
            return;
        }
        if (dark < darkTarget) dark = Math.min(darkTarget, dark + darkStep);
        else if (dark > darkTarget) dark = Math.max(darkTarget, dark - darkStep);
        if (flash > 0) flash--;
        if (scanTicks > 0) scanTicks--;
        else scanTarget = 0;
        scan += (scanTarget - scan) * 0.35f;
        if (scan < 0.005f) scan = 0;
        if (hold > 0) {
            hold--;
            int beat = hold > holdLen / 2 ? 16 : 11;
            if (hold % beat == 0) mc.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.WARDEN_HEARTBEAT, 0.8f, 0.55f));
        }
    }

    @SubscribeEvent
    public static void login(ClientPlayerNetworkEvent.LoggingIn e) {
        dark = darkTarget = 0;
        hold = flash = 0;
    }

    static float fogFar(float far) {
        return dark <= 0.001f ? far : Mth.lerp(dark, far, Math.min(far, 34f));
    }

    static void render(GuiGraphics g, float pt, int w, int h) {
        if (ClientFx.maze()) {
            if (scanTicks > 0) meter(g, w, h);
            return;
        }
        if (dark > 0.001f) {
            int a = (int) (dark * 70);
            g.fill(0, 0, w, h, a << 24 | 0x05070c);
            int edge = (int) (dark * 120);
            g.fillGradient(0, 0, w, h / 4, edge << 24, 0);
            g.fillGradient(0, h - h / 4, w, h, 0, edge << 24);
        }
        if (flash > 0) g.fill(0, 0, w, h, (int) (flash / 16f * 90) << 24 | 0x400000);
        if (hold <= 0) return;
        Minecraft mc = Minecraft.getInstance();
        Font f = mc.font;
        float t = (mc.player.tickCount + pt) * 0.35f;
        float k = hold / (float) holdLen;
        int pulse = (int) (60 + 40 * Math.abs(Mth.sin(t)));
        g.fill(0, 0, w, h, pulse << 24);
        g.fillGradient(0, 0, w, h / 3, 160 << 24, 0);
        g.fillGradient(0, h - h / 3, w, h, 0, 160 << 24);
        int bw = 140, x = (w - bw) / 2, y = h / 2 + 34;
        String title = "NÍN THỞ";
        g.drawString(f, title, (w - f.width(title)) / 2, y - 14, 0xFFD9D2C3, false);
        g.fill(x - 1, y - 1, x + bw + 1, y + 4, 0xC0000000);
        g.fill(x, y, x + (int) (bw * k), y + 3, 0xFFB8B0A0);
        String hint = holdLong ? "giữ Shift · đừng bước · đừng xoay chuột" : "giữ Shift";
        g.drawString(f, hint, (w - f.width(hint)) / 2, y + 9, 0xFF8C857A, false);
    }

    static void meter(GuiGraphics g, int w, int h) {
        Font f = Minecraft.getInstance().font;
        int x = w / 2 - 50, y = h - 64;
        long t = System.currentTimeMillis();
        g.fill(x - 4, y - 14, x + 104, y + 10, 0x90000000);
        g.drawString(f, "SÓNG", x, y - 11, 0xFFB8B0A0, false);
        for (int i = 0; i < 25; i++) {
            float n = (float) Math.abs(Math.sin(t / 37.0 + i * 1.7) * Math.sin(t / 91.0 + i * 0.6));
            int hh = (int) (2 + n * (2 + scan * 16));
            int c = scan > 0.35f ? 0xFFC04030 : 0xFF8C857A;
            g.fill(x + i * 4, y + 6 - hh, x + i * 4 + 3, y + 6, c);
        }
    }

    private SeekFx() {}
}
