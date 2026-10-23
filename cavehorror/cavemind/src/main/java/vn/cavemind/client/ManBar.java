package vn.cavemind.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.Mth;
import vn.cavemind.net.Net;

public final class ManBar {
    static float value = -1, shown = -1, flash;
    static int stage;
    static boolean due;
    static String label = "";
    static long changed;

    public static void on(Net.Bar b) {
        if (b.value() >= 0 && (int) b.value() != (int) value || b.due() != due || !b.label().equals(label)) changed = System.currentTimeMillis();
        if (b.value() >= 0 && b.stage() != stage && value >= 0) flash = 1f;
        value = b.value();
        stage = b.stage();
        due = b.due();
        label = b.label();
    }

    static void render(GuiGraphics g, float pt, int w, int h) {
        Minecraft mc = Minecraft.getInstance();
        if (value < 0 || mc.player == null || mc.options.hideGui || ClientFx.maze() || mc.screen != null && !mc.screen.isPauseScreen()) return;
        if (shown < 0) shown = value;
        shown += (value - shown) * 0.05f;
        flash = Math.max(0, flash - 0.01f);
        long now = System.currentTimeMillis();
        boolean fresh = now - changed < 8000;
        float alpha = due || fresh ? 1f : 0.55f;
        float pulse = due ? 0.65f + 0.35f * Mth.sin(now / 240f) : 1f;
        Font f = mc.font;
        int x = 8, y = 8, bw = 112, bh = 4;
        int a = (int) (alpha * 255);
        int lw = Math.max(bw, (int) (f.width(label) * 0.75f));
        g.fillGradient(x - 5, y - 4, x + lw + 6, y + 34, (int) (alpha * 150) << 24 | 0x0A0606, (int) (alpha * 40) << 24 | 0x0A0606);
        g.fill(x - 5, y - 4, x - 4, y + 34, (int) (alpha * 200) << 24 | (due ? 0xA01818 : 0x5A1414));
        g.drawString(f, "THE MAN", x, y, a << 24 | 0xD8D0C8, false);
        String[] roman = {"I", "II", "III", "IV"};
        String st = stage >= 4 ? "" : roman[Math.min(3, stage)];
        g.drawString(f, st, x + bw - f.width(st), y, a << 24 | 0x8A7A7A, false);
        int by = y + 12;
        g.fill(x - 1, by - 1, x + bw + 1, by + bh + 1, (int) (alpha * 200) << 24 | 0x140A0A);
        int fill = (int) (bw * Mth.clamp(shown / 100f, 0, 1));
        int top = due ? 0xD02222 : 0x9A1A1A, bot = due ? 0x701010 : 0x4A0A0A;
        g.fillGradient(x, by, x + fill, by + bh, (int) (alpha * pulse * 255) << 24 | top, (int) (alpha * pulse * 255) << 24 | bot);
        if (fill > 1 && due) g.fill(x + fill - 1, by - 1, x + fill, by + bh + 1, (int) (alpha * pulse * 255) << 24 | 0xFF6060);
        for (int i = 1; i <= 3; i++) {
            int nx = x + bw * i / 4;
            boolean done = stage >= i;
            g.fill(nx, by - 2, nx + 1, by + bh + 2, a << 24 | (done ? 0x4A3A3A : 0xC8C0B8));
        }
        if (flash > 0) g.fill(x, by, x + bw, by + bh, (int) (flash * 180) << 24 | 0xFFFFFF);
        g.pose().pushPose();
        g.pose().translate(x, by + bh + 5, 0);
        g.pose().scale(0.75f, 0.75f, 1f);
        g.drawString(f, label, 0, 0, a << 24 | (due ? 0xE85050 : 0xA89A92), false);
        g.pose().popPose();
    }

    private ManBar() {}
}
