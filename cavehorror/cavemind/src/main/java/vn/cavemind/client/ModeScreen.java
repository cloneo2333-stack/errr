package vn.cavemind.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.FormattedCharSequence;
import vn.cavemind.net.Net;

public final class ModeScreen extends Screen {
    static final String[][] CARDS = {
        {"CHƠI CHUNG", "Một người mở chương, cả nhóm cùng sang chương mới.", "Nhảy xuống giếng cùng lúc để trốn tìm chung một ván.", "Hợp khi cả nhóm đi cùng nhau."},
        {"CHƠI RIÊNG", "Mỗi người tự đi chương của mình.", "Mỗi người tự rủ nó chơi, tự nhận đồ chơi.", "Hợp khi mỗi người một ngả."}
    };
    int cw, chh, x0, x1, y0;
    long opened;

    public ModeScreen() {
        super(Component.literal("Cách chơi"));
    }

    @Override
    protected void init() {
        cw = Math.min(170, (width - 60) / 2);
        chh = 130;
        x0 = width / 2 - cw - 10;
        x1 = width / 2 + 10;
        y0 = height / 2 - chh / 2 + 10;
        opened = System.currentTimeMillis();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        g.fill(0, 0, width, height, 0xE0080506);
        Font f = font;
        float k = Math.min(1f, (System.currentTimeMillis() - opened) / 500f);
        int a = (int) (k * 255);
        String t = "BẠN MUỐN CHƠI THẾ NÀO?";
        g.pose().pushPose();
        g.pose().translate(width / 2f, y0 - 42, 0);
        g.pose().scale(1.5f, 1.5f, 1);
        g.drawString(f, t, -f.width(t) / 2, 0, a << 24 | 0xC83A2A, false);
        g.pose().popPose();
        String sub = "Chủ thế giới chọn một lần cho cả nhóm. Đổi lại được trong Sổ Chương.";
        g.drawString(f, sub, width / 2 - f.width(sub) / 2, y0 - 22, a << 24 | 0x8A8A8A, false);
        for (int i = 0; i < 2; i++) {
            int x = i == 0 ? x0 : x1;
            boolean hov = mx >= x && mx < x + cw && my >= y0 && my < y0 + chh;
            g.fill(x - 1, y0 - 1, x + cw + 1, y0 + chh + 1, hov ? 0xFFC83A2A : 0xFF3A2A2A);
            g.fillGradient(x, y0, x + cw, y0 + chh, 0xFF1E1414, 0xFF120C0C);
            g.drawString(f, CARDS[i][0], x + cw / 2 - f.width(CARDS[i][0]) / 2, y0 + 12, hov ? 0xFFF0D0C0 : 0xFFD0B0A0, false);
            g.fill(x + 16, y0 + 26, x + cw - 16, y0 + 27, 0x60C83A2A);
            int y = y0 + 36;
            for (int l = 1; l < CARDS[i].length; l++) {
                for (FormattedCharSequence s : f.split(FormattedText.of(CARDS[i][l]), cw - 24)) {
                    g.drawString(f, s, x + 12, y, l == CARDS[i].length - 1 ? 0xFF8A7A6A : 0xFFC8C0B8, false);
                    y += 10;
                }
                y += 4;
            }
        }
        super.render(g, mx, my, pt);
    }

    @Override
    public boolean mouseClicked(double mx, double my, int b) {
        for (int i = 0; i < 2; i++) {
            int x = i == 0 ? x0 : x1;
            if (mx >= x && mx < x + cw && my >= y0 && my < y0 + chh) {
                BookClient.ask(Net.Ask.MODE, i == 0 ? 1 : 0);
                Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.BELL_RESONATE, 0.8f, 0.6f));
                onClose();
                return true;
            }
        }
        return super.mouseClicked(mx, my, b);
    }
}
