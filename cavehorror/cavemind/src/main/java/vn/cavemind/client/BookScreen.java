package vn.cavemind.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.math.Axis;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import vn.cavemind.CaveMind;

public final class BookScreen extends Screen {
    static final String[] TABS = {"Bắt đầu", "Mục tiêu", "Các chương", "Hướng dẫn", "Mê cung", "Vật phẩm", "Ghi chép"};
    static final int INK = 0xFF3B2A1E, RED = 0xFF7A1A14, FAINT = 0xFF8A7458, PAPER_A = 0xFFE9DCBD, PAPER_B = 0xFFD9C79F;
    static final int GUIDE = 0, CHAPTERS = 2, GEAR = 5, NOTES = 6;
    static final int DIRT_W = 14, DIRT_H = 8;
    static final java.util.Map<Integer, float[]> DIRT = new java.util.HashMap<>();
    static long scrawlDay = -1;

    record Entry(String id, String moon, int color, String name, String from, String use, String cost, String[] grid) {}

    static final Entry[] GEARS = {
        new Entry("cavemind:phong_vu_bieu_suong", null, 0, "Phong Vũ Biểu Sương",
            "Chế từ đồng, kính, la bàn và một Mảnh Xương Sương.",
            "Để trên thanh công cụ. Tạch… tạch… khi hắn đi vào vùng này, càng gần càng dồn dập. Mặt kính đóng băng là hắn ở ngay rìa sương.",
            "Không chỉ hướng, không đo mét. Bạn chỉ biết hắn đang ở quanh đây.",
            new String[]{"minecraft:copper_ingot", "minecraft:glass_pane", "minecraft:copper_ingot", "minecraft:glass_pane", "minecraft:compass", "minecraft:glass_pane", "minecraft:copper_ingot", "cavemind:manh_xuong_suong", "minecraft:copper_ingot"}),
        new Entry("cavemind:chuong_gio_xuong", null, 0, "Bùa Chuông Gió Xương",
            "Chế từ sợi chỉ, một khúc gỗ và ba Mảnh Xương Sương.",
            "Treo ở hiên nhà hay cửa hầm mỏ. Tự leng keng khi hắn tới trong 45 khối. Chặn hắn bịt hầm và tráo thịt thối trong 16 khối.",
            "Chặn ba lần thì đứt dây, phải chế cái mới.",
            new String[]{"", "minecraft:string", "", "cavemind:manh_xuong_suong", "minecraft:oak_log", "cavemind:manh_xuong_suong", "", "cavemind:manh_xuong_suong", ""}),
        new Entry("cavemind:may_thu_am", null, 0, "Máy Thu Âm",
            "Chế từ ống nhòm, hai Mảnh Đá Vọng Âm và kim loại tối của Born in Chaos.",
            "Trong mê cung: đứng yên, giữ chuột phải, quét chậm qua các ngã rẽ. Chĩa đúng chỗ nó đứng (16 khối, xuyên tường) thì máy rè và hình nhiễu hạt.",
            "Ăn pin máy quay gấp ba. Bước đi là mất sóng.",
            new String[]{"", "cavemind:manh_da_vong_am", "", "born_in_chaos_v1:dark_metal_nugget", "minecraft:spyglass", "born_in_chaos_v1:dark_metal_nugget", "", "cavemind:manh_da_vong_am", ""}),
        new Entry("cavemind:bi_day_cot", null, 0, "Viên Bi Lên Dây Cót",
            "Chế từ hạt sắt, đá đỏ và cán xương của Born in Chaos. Ra hai viên.",
            "Chuột phải để ném xa chừng mười khối. Một lát sau nó nảy lộc cộc khắp sàn, kéo thứ đang tìm bạn về phía đó. Dùng để thoát khỏi tủ hay lẻn qua ngã tư.",
            "Mỗi ván mê cung chỉ dùng được hai viên.",
            new String[]{"minecraft:iron_nugget", "born_in_chaos_v1:bone_handle", "minecraft:iron_nugget", "born_in_chaos_v1:bone_handle", "minecraft:redstone", "born_in_chaos_v1:bone_handle", "minecraft:iron_nugget", "born_in_chaos_v1:bone_handle", "minecraft:iron_nugget"}),
        new Entry("cavemind:lo_dom_dom", null, 0, "Lọ Đom Đóm",
            "Mỗi người có sẵn một lọ khi mới vào. Mất thì chế lại từ chai thủy tinh, đuốc và lá cây.",
            "Chuột phải: đom đóm bay ra, để lại vệt sáng về phía nơi bạn cần tới (điểm hẹn, sân chơi, trang nhật ký, nơi chưa ai tới). Shift + chuột phải để đổi thứ cần tìm.",
            "Chỉ bay chừng ba mươi khối rồi lượn vòng. Phải nghỉ mười hai giây mới thả lại được.",
            new String[]{"", "minecraft:oak_leaves", "", "", "minecraft:torch", "", "", "minecraft:glass_bottle", ""}),
        new Entry("cavemind:manh_da_vong_am", null, 0, "Mảnh Đá Vọng Âm",
            "Dưới lòng đất, khi nghe tiếng cuốc vọng trong vách đá: lần theo, rồi đập đúng viên đá đó.",
            "Nguyên liệu của Máy Thu Âm.",
            "Đêm Trăng Thợ Mỏ chắc chắn có, đêm thường chỉ thỉnh thoảng.", null),
        new Entry("cavemind:manh_xuong_suong", null, 0, "Mảnh Xương Sương",
            "Đánh đủ đòn để The Man phải bỏ chạy trong một đêm săn.",
            "Nguyên liệu của Phong Vũ Biểu và Chuông Gió Xương.",
            "Đêm Trăng Máu hắn chắc chắn rơi hai mảnh. Đêm thường thì tùy may.", null),
        new Entry(null, "miner_moon", 0xFFB3E0F0, "Trăng Thợ Mỏ",
            "Đêm Lunar cho mọi người đào nhanh hơn.",
            "Dưới hang, mỗi khối đá bạn đập làm ký ức của Herobrine dâng nhanh gấp ba. Tiếng cuốc, đuốc ma và bóng thợ mỏ tới dày hơn.",
            "Đi theo tiếng cuốc là cách chắc chắn nhất để có Mảnh Đá Vọng Âm.", null),
        new Entry(null, "blood_moon", 0xFFC0302A, "Trăng Máu",
            "Đêm Lunar làm quái vật hung hãn hơn.",
            "The Man đói tới cực độ và bớt thận trọng một nửa. Hắn bỏ nghỉ, lao về phía căn cứ của bạn.",
            "Dám ra ngoài đánh đuổi hắn thì chắc chắn được Mảnh Xương Sương.", null),
        new Entry(null, "crimson_moon", 0xFF8A1A3A, "Trăng Thẫm",
            "Đêm Lunar làm ranh giới giữa các chiều mỏng đi.",
            "Cột khói đỏ bốc lên từ sân chơi và các cây đếm, kèm tiếng trẻ con cười. Xuống giếng đêm nay: nó nghe rõ hơn.",
            "Thắng ván mê cung đêm nay thì gần như chắc có quà hiếm.", null)
    };
    int gear;

    record Thing(String id, String tip) {}

    record Card(String img, String title, String body, String tip, Thing... things) {}

    static final Card[] CARDS = {
        new Card("so", "Cuốn sổ này", "Phím J mở sổ. Không biết làm gì: xem trang Mục tiêu.", "Dấu đỏ ĐÃ TRẢI QUA: việc bạn đã gặp.",
            new Thing("cavemind:so_chuong", "Sổ Chương: mở bằng phím J"), new Thing("cavemind:lo_dom_dom", "Lọ đom đóm: bay về phía nơi bạn cần tới")),
        new Card("man", "Người trong sương", "Thanh THE MAN đầy là hắn tới.", "Rung chuông nhiều, hắn sẽ quen.",
            new Thing("minecraft:bell", "Chuông: rung để đuổi hắn"), new Thing("minecraft:lantern", "Ánh sáng giữ hắn ở xa hơn")),
        new Card("dem", "Đứa trẻ úp mặt vào cây", "Nó đang rủ bạn chơi trốn tìm.", "Đừng bỏ về, đừng đánh, đừng nhìn trộm.",
            new Thing("minecraft:candle", "Thắng ván, nó để lại một ngọn nến tắt")),
        new Card("tron", "Cách trốn", "Ngồi thụp ở chỗ tối, có che chắn.", "Chữ NÍN THỞ hiện: giữ Shift, đừng nhúc nhích."),
        new Card("chuong", "Mở chương mới", "Xong nhiệm vụ chương: nhận la bàn sương.", "Ở điểm hẹn: đọc sổ, thắp nến, nhặt đồ chơi.",
            new Thing("minecraft:compass", "La bàn sương chỉ đường tới điểm hẹn"), new Thing("minecraft:candle", "Thắp hết nến ở điểm hẹn"),
            new Thing("cavemind:con_quay_linh", "Món đồ chơi: cầm nó chạm vào cây đếm")),
        new Card("gieng", "Sân chơi bỏ hoang", "Vài ngày sau, nó hiện ra gần bạn.", "Đừng phá gì trong sân chơi. Nó nhớ.",
            new Thing("cavemind:lo_dom_dom", "Lọ đom đóm: chuột phải để thả, Shift + chuột phải để đổi thứ cần tìm"), new Thing("minecraft:chest", "Rương dưới cây đếm: luật chơi"), new Thing("minecraft:bell", "Chuông trường")),
        new Card("me", "Giờ Thứ Bốn Mươi Tám", "Nến trên nắp giếng, rung chuông trường, nhảy.", "Bảy đồ chơi về rương, rồi rung chuông.",
            new Thing("minecraft:candle", "Nến đang cháy đặt lên nắp giếng"), new Thing("minecraft:bell", "Rồi rung chuông trường")),
        new Card("trang", "Trang nhật ký rách", "Hai mươi bốn trang, mỗi trang một mảnh chuyện.", "Dán vào sổ: đủ 4, 8, 12 dấu ấn có quà.",
            new Thing("cavemind:trang_nhat_ky", "Trang nhật ký rách")),
        new Card("an", "Phần còn lại: tự tìm", "Có thứ nhớ những gì bạn đào.", "Phần sâu hơn nằm trong bóng tối.")
    };

    // Red arrows scrawled over each sketch: target point, label corner (both in the 160x96 picture), label of six words at most.
    record Mark(int tx, int ty, int lx, int ly, String text) {}

    static final Mark[][] MARKS = {
        {new Mark(82, 38, 4, 6, "J: mở sổ"), new Mark(34, 64, 4, 84, "lạc đường: thả đom đóm")},
        {new Mark(104, 36, 96, 4, "thanh đầy: hắn tới"), new Mark(30, 62, 2, 34, "tối: vào nhà"), new Mark(110, 58, 98, 84, "chuông → đuổi hắn")},
        {new Mark(100, 44, 104, 6, "đếm tới 48"), new Mark(84, 32, 4, 10, "đừng nhìn trộm"), new Mark(72, 76, 4, 84, "thắng: được nến")},
        {new Mark(68, 68, 4, 40, "Shift: ngồi im"), new Mark(104, 66, 96, 84, "núp sau vật che"), new Mark(130, 32, 84, 6, "tránh chỗ sáng")},
        {new Mark(32, 50, 50, 8, "la bàn → điểm hẹn"), new Mark(108, 66, 66, 84, "đọc sổ"), new Mark(132, 76, 104, 34, "thắp đủ nến")},
        {new Mark(70, 50, 4, 18, "nến cháy trên nắp"), new Mark(140, 50, 96, 8, "rung chuông trường"), new Mark(70, 76, 4, 84, "rồi nhảy xuống")},
        {new Mark(22, 18, 30, 30, "N: nhìn đêm, tốn pin"), new Mark(80, 62, 92, 82, "đừng nhìn quá 2 giây!")},
        {new Mark(60, 30, 76, 8, "khói mảnh = một trang"), new Mark(92, 78, 104, 56, "dán vào sổ")},
        {new Mark(80, 46, 96, 10, "nó đang nhìn")}
    };

    static final String[] TOYS = {"cavemind:so_chuong", "cavemind:con_quay_linh", "cavemind:bi_ve_tuan", "cavemind:dong_ho_mai", "cavemind:dieu_giay_bao", "cavemind:coi_go_nam"};
    static final String[][] RULES = {
        {"The Man", "Thanh đầy: hắn tới. Tối: ở trong nhà sáng đèn. Chuông đuổi hắn, rung nhiều hắn quen."},
        {"The Damned", "Ngồi thụp, đứng im, chỗ tối, đừng nhìn nó. Hoa hồng sống xua nó."},
        {"Herobrine", "Nó nhớ thứ bạn đào. Đừng đào thẳng xuống một mình."},
        {"Nhà của bạn", "Nơi bạn ngủ, ở lâu, cất đồ. Dọn nhà: vài đêm sau chúng biết."},
        {"Trang nhật ký", "Khói mảnh là một trang. Dán vào sổ để có dấu ấn."},
        {"Chơi chung / riêng", "Chung: cả nhóm cùng chương, cùng ván. Chủ thế giới chọn."}
    };
    static final String[][] MAZE = {
        {"Vào mê cung", "Nến cháy trên nắp giếng, rung chuông trường, nhảy."},
        {"Túi đồ", "Đồ của bạn được cất lại. Dưới đó chỉ có máy quay, máy thu âm, hai viên bi."},
        {"Mục tiêu", "Bảy đồ chơi về rương, rung chuông. Hoặc trốn tới hết giờ."},
        {"Máy quay", "[N] nhìn đêm, tốn pin. Tắt đi: chỉ thấy một mét rưỡi."},
        {"Đồ chơi khóc", "Nhặt nó trong 20 giây. Không thì nó chỉ chỗ bạn."},
        {"Thanh SỨC", "Chạy, nín thở đều tốn sức. Hết sức: thở hắt ra."},
        {"Thanh SỢ", "Trên 70%: hình rách, nó nghe tiếng bạn thở."},
        {"Chỗ trốn", "Tủ: ngồi thụp để nín thở. Trốn lì 50 giây: bạn sẽ ho."},
        {"Khi nó đuổi", "Rẽ gấp để cắt tầm nhìn. Đánh nó: nó khựng lại."}
    };

    JsonObject data;
    int tab, scroll, maxScroll;
    int bx, by, bw, bh;
    final List<int[]> tabBoxes = new ArrayList<>();
    int modeX, modeY, modeW;
    int card, cardFrom = -1, node = -1;
    boolean flipped;
    long opened, turned;
    final List<int[]> hits = new ArrayList<>();
    final List<Component> tips = new ArrayList<>();
    final List<int[]> tipBoxes = new ArrayList<>();
    int selNote = -1, scrubs, crayonBeat = -1;
    long glowAt, crayonAt;
    int[] dirtBox;

    public BookScreen(JsonObject data, int page) {
        super(Component.literal("Sổ Chương"));
        this.data = data;
        int p = data.has("page") ? data.get("page").getAsInt() : -1;
        this.tab = p >= 0 ? p : page >= 0 ? page : beginner() ? GUIDE : 1;
        if (this.tab < 0 || this.tab >= TABS.length) this.tab = 0;
        this.card = firstOpen();
        this.opened = System.currentTimeMillis();
        Minecraft mc = Minecraft.getInstance();
        if (mc.level != null) {
            long t = mc.level.getDayTime() % 24000, day = mc.level.getDayTime() / 24000;
            // around midnight, now and then, something writes over the page
            if (t > 17400 && t < 18800 && day != scrawlDay && mc.level.random.nextFloat() < 0.4f) {
                scrawlDay = day;
                crayonAt = opened + 900;
            }
        }
    }

    boolean lurk() {
        return data.has("lurk") && data.get("lurk").getAsBoolean();
    }

    long pasted() {
        return data.has("pasted") ? data.get("pasted").getAsLong() : 0L;
    }

    void refresh(JsonObject o) {
        data = o;
    }

    boolean done(int i) {
        JsonArray a = data.getAsJsonArray("start");
        return a != null && i < a.size() && a.get(i).getAsBoolean();
    }

    boolean beginner() {
        int n = 0;
        for (int i = 0; i < CARDS.length; i++) if (done(i)) n++;
        return n < 4;
    }

    int firstOpen() {
        for (int i = 0; i < CARDS.length - 1; i++) if (!done(i)) return i;
        return 0;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    protected void init() {
        bw = Math.min(width - 24, 420);
        bh = Math.min(height - 24, 248);
        bx = (width - bw) / 2;
        by = (height - bh) / 2;
    }

    static String str(JsonObject o, String k) {
        JsonElement e = o.get(k);
        return e == null || e.isJsonNull() ? "" : e.getAsString();
    }

    static ItemStack stack(String id) {
        var it = BuiltInRegistries.ITEM.get(new ResourceLocation(id));
        return it == Items.AIR ? ItemStack.EMPTY : new ItemStack(it);
    }

    static float ease(float t) {
        t = Mth.clamp(t, 0f, 1f);
        return 1f - (1f - t) * (1f - t) * (1f - t);
    }

    static boolean in(double mx, double my, int[] b) {
        return mx >= b[0] && mx < b[2] && my >= b[1] && my < b[3];
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        renderBackground(g);
        hits.clear();
        tips.clear();
        tipBoxes.clear();
        Font f = font;
        float open = ease((System.currentTimeMillis() - opened) / 260f);
        g.pose().pushPose();
        g.pose().translate(0, (1f - open) * 14f, 0);
        g.fill(bx - 6, by - 6, bx + bw + 6, by + bh + 6, 0xFF140A07);
        g.fill(bx - 5, by - 5, bx + bw + 5, by + bh + 5, 0xFF3A1D14);
        g.fill(bx - 3, by - 3, bx + bw + 3, by + bh + 3, 0xFF4A2619);
        for (int[] c : new int[][]{{bx - 5, by - 5}, {bx + bw - 3, by - 5}, {bx - 5, by + bh - 3}, {bx + bw - 3, by + bh - 3}}) g.fill(c[0], c[1], c[0] + 8, c[1] + 8, 0xFF8A6A3A);
        int mid = bx + bw / 2;
        g.fillGradient(bx, by, mid - 1, by + bh, PAPER_A, PAPER_B);
        g.fillGradient(mid + 1, by, bx + bw, by + bh, PAPER_A, PAPER_B);
        g.fillGradient(mid - 10, by, mid, by + bh, 0x00000000, 0x30000000);
        g.fillGradient(mid, by, mid + 10, by + bh, 0x30000000, 0x00000000);
        g.fill(mid - 1, by, mid + 1, by + bh, 0x60301810);
        for (int i = 0; i < 6; i++) g.fill(bx + 2, by + bh - 2 - i, bx + bw - 2, by + bh - 1 - i, (0x10 - i * 2) << 24);
        g.fill(bx + bw - 30, by + bh + 3, bx + bw - 22, by + bh + 20, 0xFF7A1A14);
        g.fill(bx + bw - 30, by + bh + 20, bx + bw - 26, by + bh + 24, 0xFF7A1A14);
        g.fill(bx + bw - 26, by + bh + 20, bx + bw - 22, by + bh + 22, 0xFF7A1A14);
        int lx = bx + 12, lw = bw / 2 - 24;
        int y = by + 12;
        g.drawString(f, "SỔ CHƯƠNG", lx, y, RED, false);
        flame(g, bx + bw / 2 - 26, by + 22, 1.0f);
        y += 11;
        g.drawString(f, str(data, "name"), lx, y, FAINT, false);
        y += 13;
        int ch = data.has("chapter") ? data.get("chapter").getAsInt() : 1;
        g.drawString(f, "Chương " + ch, lx, y, INK, false);
        y += 10;
        for (FormattedCharSequence s : f.split(FormattedText.of(str(data, "title"), Style.EMPTY.withItalic(true)), lw)) {
            g.drawString(f, s, lx, y, INK, false);
            y += 9;
        }
        y += 3;
        int mode = data.has("mode") ? data.get("mode").getAsInt() : -1;
        String badge = mode == 1 ? "CHƠI CHUNG" : mode == 0 ? "CHƠI RIÊNG" : "CHƯA CHỌN CÁCH CHƠI";
        int bwid = f.width(badge) + 8;
        boolean host = data.has("host") && data.get("host").getAsBoolean();
        boolean hovBadge = host && mx >= lx && mx < lx + bwid && my >= y && my < y + 12;
        g.fill(lx, y, lx + bwid, y + 12, mode == 1 ? 0xFF5A3A6A : mode == 0 ? 0xFF4A4A3A : 0xFF7A3A2A);
        if (hovBadge) g.fill(lx, y, lx + bwid, y + 12, 0x30FFFFFF);
        g.drawString(f, badge, lx + 4, y + 2, 0xFFF0E6D0, false);
        if (hovBadge) tip(Component.literal("Bấm để đổi cách chơi"), lx, y, lx + bwid, y + 12);
        modeX = lx;
        modeY = y;
        modeW = bwid;
        y += 20;
        tabBoxes.clear();
        for (int i = 0; i < TABS.length; i++) {
            boolean sel = i == tab, hov = mx >= lx - 4 && mx < lx + lw && my >= y - 2 && my < y + 12;
            if (sel) {
                g.fill(lx - 4, y - 2, lx + lw, y + 11, 0x307A1A14);
                g.fill(lx - 4, y - 2, lx - 2, y + 11, RED);
            } else if (hov) g.fill(lx - 4, y - 2, lx + lw, y + 11, 0x18000000);
            g.drawString(f, TABS[i], lx + 2 + (hov && !sel ? 2 : 0), y, sel ? RED : INK, false);
            if (i == GUIDE && beginner()) {
                int px = lx + 6 + f.width(TABS[i]);
                float b = 0.5f + 0.5f * Mth.sin(System.currentTimeMillis() / 220f);
                g.fill(px, y + 2, px + 4, y + 6, (int) (120 + 135 * b) << 24 | 0x7A1A14);
            }
            tabBoxes.add(new int[]{lx - 4, y - 2, lx + lw, y + 11});
            y += 14;
        }
        int found = data.has("found") ? data.get("found").getAsInt() : 0;
        g.drawString(f, "Trang nhật ký " + found + "/24", lx, by + bh - 22, FAINT, false);
        g.pose().pushPose();
        g.pose().translate(lx, by + bh - 11, 0);
        g.pose().scale(0.75f, 0.75f, 1);
        g.drawString(f, "[Esc] gấp sổ   [chuột] lật trang   [← →] lật thẻ", 0, 0, FAINT, false);
        g.pose().popPose();
        int cx = mid + 12, cy = by + 12, cw = bw / 2 - 24, chh = bh - 24;
        if (tab == GUIDE) guide(g, f, cx, cy, cw, chh, mx, my);
        else if (tab == CHAPTERS) chapters(g, f, cx, cy, cw, chh, mx, my);
        else if (tab == GEAR) gear(g, f, cx, cy, cw, chh, mx, my);
        else if (tab == NOTES) notes(g, f, cx, cy, cw, chh, mx, my);
        else content(g, f, cx, cy, cw, chh);
        doodle(g, lx + lw - 22, by + bh - 62, mx, my);
        long since = System.currentTimeMillis() - glowAt;
        if (since < 700) {
            int a = (int) (70 * (1f - since / 700f));
            g.fillGradient(bx, by, bx + bw, by + bh / 2, a << 24 | 0xFFD98A, (a / 3) << 24 | 0xFFD98A);
            g.fillGradient(bx, by + bh / 2, bx + bw, by + bh, (a / 3) << 24 | 0xFFD98A, a << 24 | 0xFFD98A);
        }
        g.pose().popPose();
        super.render(g, mx, my, pt);
        crayon(g, f);
        for (int i = 0; i < tipBoxes.size(); i++) {
            if (in(mx, my, tipBoxes.get(i))) {
                g.renderTooltip(f, tips.get(i), mx, my);
                break;
            }
        }
    }

    void tip(Component c, int x0, int y0, int x1, int y1) {
        tips.add(c);
        tipBoxes.add(new int[]{x0, y0, x1, y1});
    }

    void flame(GuiGraphics g, int x, int y, float s) {
        long t = System.currentTimeMillis();
        float fl = 0.75f + 0.25f * Mth.sin(t / 90f) * Mth.sin(t / 37f + 1.3f);
        int r = (int) (5 * s * fl) + 2;
        for (int i = r; i > 0; i--) {
            int a = (int) (28 * fl * (1f - i / (float) (r + 1)));
            g.fill(x - i, y - i, x + i + 1, y + i + 1, a << 24 | 0xE8A040);
        }
        int h = (int) (3 * s * fl) + 1;
        g.fill(x, y - h, x + 1, y + 1, 0xE0F0B050);
        g.fill(x - 1, y - h / 2, x + 2, y + 1, 0xC0D88030);
    }

    void stamp(GuiGraphics g, Font f, int x, int y) {
        String s = "ĐÃ TRẢI QUA";
        int w = f.width(s) + 6;
        g.pose().pushPose();
        g.pose().translate(x, y, 0);
        g.pose().mulPose(Axis.ZP.rotationDegrees(-11f));
        g.fill(-w / 2 - 1, -7, w / 2 + 1, 7, 0xB07A1A14);
        g.fill(-w / 2, -6, w / 2, 6, 0x40E9DCBD);
        g.drawString(f, s, -w / 2 + 3, -4, 0xFFF4E2C8, false);
        g.pose().popPose();
    }

    void guide(GuiGraphics g, Font f, int x, int y, int w, int h, int mx, int my) {
        long now = System.currentTimeMillis();
        float turn = ease((now - turned) / 220f);
        int dir = cardFrom < 0 || cardFrom == card ? 0 : card > cardFrom ? 1 : -1;
        int off = (int) ((1f - turn) * 18 * dir);
        Card c = CARDS[card];
        int iw = Math.min(w, 160), ih = iw * 96 / 160, ix = x + (w - iw) / 2 + off, iy = y;
        boolean hovImg = mx >= ix && mx < ix + iw && my >= iy && my < iy + ih;
        g.enableScissor(x - 2, y - 2, x + w + 2, y + h);
        int lift = hovImg ? -1 : 0;
        g.fill(ix + iw, iy + 2, ix + iw + 2, iy + ih + 2 + lift, 0x28000000);
        g.fill(ix + 2, iy + ih + lift, ix + iw + 2, iy + ih + 2 + lift, 0x28000000);
        g.fill(ix - 1, iy - 1 + lift, ix + iw + 1, iy + ih + 1 + lift, hovImg ? 0xFF7A1A14 : 0xFF8A7458);
        g.fill(ix, iy + lift, ix + iw, iy + ih + lift, 0xFFEDE2C6);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        g.blit(new ResourceLocation(CaveMind.MODID, "textures/gui/book/" + c.img() + ".png"), ix, iy + lift, iw, ih, 0, 0, 160, 96, 160, 96);
        RenderSystem.disableBlend();
        float k = iw / 160f;
        alive(g, f, c.img(), ix, iy + lift, k, mx, my, now);
        float drawn = ease((now - Math.max(turned, opened)) / 450f);
        for (Mark m : MARKS[card]) mark(g, f, ix, iy + lift, iw, ih, k, m, drawn);
        if (done(card) && card < CARDS.length - 1) stamp(g, f, ix + iw - 34, iy + 12);
        hits.add(new int[]{0, ix, iy, ix + iw, iy + ih});
        int ty = iy + ih + 4;
        g.drawString(f, (card + 1) + ". " + c.title(), x + off, ty, RED, false);
        ty += 12;
        String text = flipped ? c.tip() : c.body();
        int col = flipped ? 0xFF5A2A1A : INK;
        for (FormattedCharSequence s : f.split(FormattedText.of(text, flipped ? Style.EMPTY.withItalic(true) : Style.EMPTY), w)) {
            g.drawString(f, s, x + off, ty, col, false);
            ty += 9;
        }
        int ny = y + h - 12;
        int iyy = ny - 22;
        int tx = x;
        for (Thing t : c.things()) {
            ItemStack st = stack(t.id());
            if (st.isEmpty()) continue;
            boolean hov = mx >= tx && mx < tx + 18 && my >= iyy && my < iyy + 18;
            g.fill(tx, iyy, tx + 18, iyy + 18, hov ? 0x307A1A14 : 0x18000000);
            g.renderItem(st, tx + 1, iyy + 1);
            tip(Component.literal(t.tip()), tx, iyy, tx + 18, iyy + 18);
            tx += 21;
        }
        String hint = flipped ? "bấm vào tranh: lật lại" : "bấm vào tranh: lật xem mẹo";
        g.pose().pushPose();
        g.pose().translate(x + w - f.width(hint) * 0.75f, iyy + 6, 0);
        g.pose().scale(0.75f, 0.75f, 1);
        g.drawString(f, hint, 0, 0, hovImg ? RED : FAINT, false);
        g.pose().popPose();
        g.disableScissor();
        boolean hp = mx >= x && mx < x + 12 && my >= ny && my < ny + 11, hn = mx >= x + w - 12 && mx < x + w && my >= ny && my < ny + 11;
        arrow(g, x + 2, ny + 1, -1, card > 0 ? hp ? RED : INK : 0x40000000);
        arrow(g, x + w - 8, ny + 1, 1, card < CARDS.length - 1 ? hn ? RED : INK : 0x40000000);
        hits.add(new int[]{1, x, ny, x + 12, ny + 11});
        hits.add(new int[]{2, x + w - 12, ny, x + w, ny + 11});
        int dw = CARDS.length * 9, dx = x + (w - dw) / 2;
        for (int i = 0; i < CARDS.length; i++) {
            int px = dx + i * 9;
            boolean d = done(i) && i < CARDS.length - 1, cur = i == card, hov = mx >= px - 1 && mx < px + 7 && my >= ny && my < ny + 9;
            int colr = cur ? RED : d ? INK : FAINT;
            if (cur || d) g.fill(px, ny + 2, px + 6, ny + 8, colr);
            else {
                g.fill(px, ny + 2, px + 6, ny + 3, colr);
                g.fill(px, ny + 7, px + 6, ny + 8, colr);
                g.fill(px, ny + 2, px + 1, ny + 8, colr);
                g.fill(px + 5, ny + 2, px + 6, ny + 8, colr);
            }
            if (hov) g.fill(px - 1, ny + 1, px + 7, ny + 9, 0x307A1A14);
            hits.add(new int[]{10 + i, px - 1, ny, px + 7, ny + 9});
            tip(Component.literal((i + 1) + ". " + CARDS[i].title() + (d ? "  ✔" : "")), px - 1, ny, px + 7, ny + 9);
        }
    }

    void arrow(GuiGraphics g, int x, int y, int dir, int col) {
        for (int i = 0; i < 5; i++) {
            int xx = dir > 0 ? x + i : x + 4 - i;
            g.fill(xx, y + i, xx + 1, y + 9 - i, col);
        }
    }

    void alive(GuiGraphics g, Font f, String img, int x, int y, float k, int mx, int my, long now) {
        switch (img) {
            case "so" -> {
                flame(g, x + (int) (128 * k), y + (int) (66 * k), 1.2f * k);
                g.drawString(f, "J", x + (int) (28 * k), y + (int) (62 * k), RED, false);
            }
            case "man" -> {
                if (lurk()) eyes(g, x + (int) (104 * k), y + (int) (37 * k), Math.max(1, (int) (2 * k)), mx, my, now);
                for (int i = 0; i < 4; i++) {
                    float t = now / (2600f + i * 700f) + i * 1.7f;
                    int fx = x + (int) ((80 + Mth.sin(t) * 70) * k) - 24, fy = y + (int) ((58 + i * 5) * k);
                    g.fillGradient(fx, fy, fx + 48, fy + 4, 0x00E9DCBD, 0x50E9DCBD);
                    g.fillGradient(fx, fy + 4, fx + 48, fy + 8, 0x50E9DCBD, 0x00E9DCBD);
                }
            }
            case "dem" -> {
                int n = (int) (now / 650 % 48) + 1;
                for (int i = 0; i < 3; i++) {
                    int v = n - 2 + i;
                    if (v < 1) continue;
                    int a = 80 + i * 70;
                    g.drawString(f, "…" + vn.cavemind.damned.Game.num(v) + "…", x + (int) ((88 + i * 12) * k), y + (int) ((22 + i * 13) * k), a << 24 | 0x3B2A1E, false);
                }
            }
            case "tron" -> {
                boolean near = mx >= x && my >= y && mx < x + 160 * k && my < y + 96 * k;
                g.drawString(f, near ? "suỵt…" : "suỵt", x + (int) (50 * k), y + (int) (40 * k), near ? RED : INK, false);
            }
            case "chuong" -> {
                for (int cx : new int[]{118, 124, 140, 146}) flame(g, x + (int) (cx * k), y + (int) (74 * k), 0.6f * k);
                float a = Mth.sin(now / 900f) * 0.25f;
                int ccx = x + (int) (32 * k), ccy = y + (int) (46 * k);
                for (int i = 2; i < 12; i++) {
                    int px = ccx + (int) (Mth.sin(a) * i * k), py = ccy - (int) (Mth.cos(a) * i * k);
                    g.fill(px, py, px + 1, py + 1, 0xFF7A1A14);
                }
            }
            case "gieng" -> {
                flame(g, x + (int) (72 * k), y + (int) (47 * k), 0.9f * k);
                g.drawString(f, "SỐ 4", x + (int) (62 * k), y + (int) (18 * k), INK, false);
            }
            case "me" -> {
                if (now / 600 % 2 == 0) g.fill(x + (int) (19 * k), y + (int) (15 * k), x + (int) (25 * k), y + (int) (21 * k), 0xFFB02018);
                g.drawString(f, "REC", x + (int) (28 * k), y + (int) (14 * k), 0xFFE8DCC8, false);
                long s = now / 1000 % 3600;
                g.drawString(f, String.format("%02d:%02d", s / 60, s % 60), x + (int) (118 * k), y + (int) (80 * k), 0xFFE8DCC8, false);
            }
            case "trang" -> {
                flame(g, x + (int) (64 * k), y + (int) (63 * k), 0.8f * k);
                for (int i = 0; i < 6; i++) {
                    float t = (now / 2400f + i / 6f) % 1f;
                    int a = (int) (110 * (1f - t));
                    int px = x + (int) ((64 + Mth.sin(t * 7 + i) * 3 * t) * k), py = y + (int) ((60 - t * 56) * k), r = (int) ((2 + t * 5) * k);
                    g.fill(px - r, py - r, px + r, py + r, a << 24 | 0x6A625A);
                }
            }
            case "an" -> {
                int ex = x + (int) (80 * k), ey = y + (int) (46 * k);
                double ang = Math.atan2(my - ey, mx - ex), d = Math.min(3.0, Math.hypot(mx - ex, my - ey) / 30.0);
                for (int dy = -6; dy <= 6; dy++) {
                    int hw = (int) Math.sqrt(36 - dy * dy);
                    g.fill(ex - hw, ey + dy, ex + hw + 1, ey + dy + 1, 0xFFE9DCBD);
                }
                int px = ex + (int) Math.round(Math.cos(ang) * d * 1.3), py = ey + (int) Math.round(Math.sin(ang) * d);
                boolean blink = now / 90 % 50 == 0;
                if (blink) g.fill(ex - 6, ey, ex + 7, ey + 1, 0xFF3B2A1E);
                else for (int dy = -3; dy <= 3; dy++) {
                    int hw = (int) Math.sqrt(9 - dy * dy);
                    g.fill(px - hw, py + dy, px + hw + 1, py + dy + 1, 0xFF7A1A14);
                }
            }
            default -> {}
        }
    }

    // A red pen arrow from the note to the thing in the sketch, drawn in as the card turns.
    void mark(GuiGraphics g, Font f, int ix, int iy, int iw, int ih, float k, Mark m, float drawn) {
        float sc = k < 0.9f ? 0.66f : 0.75f;
        int tw = (int) (f.width(m.text()) * sc), th = (int) (9 * sc);
        int lx = Mth.clamp(ix + (int) (m.lx() * k), ix + 1, ix + iw - tw - 1), ly = Mth.clamp(iy + (int) (m.ly() * k), iy + 1, iy + ih - th - 1);
        int tx = ix + (int) (m.tx() * k), ty = iy + (int) (m.ty() * k);
        float sx = Mth.clamp(tx, lx, lx + tw), sy = ty < ly ? ly - 1 : ty > ly + th ? ly + th + 1 : ly + th / 2f;
        if (tx >= lx && tx <= lx + tw) sx = lx + tw / 2f;
        double len = Math.hypot(tx - sx, ty - sy);
        int col = 0xE0B0201A;
        if (len > 4) {
            double ux = (tx - sx) / len, uy = (ty - sy) / len;
            double end = (len - 2.5) * drawn;
            for (double d = 0; d < end; d += 0.6) {
                double wob = Math.sin(d * 0.35 + m.tx()) * 0.6;
                g.fill((int) (sx + ux * d - uy * wob), (int) (sy + uy * d + ux * wob), (int) (sx + ux * d - uy * wob) + 1, (int) (sy + uy * d + ux * wob) + 1, col);
            }
            if (drawn >= 1f) {
                double hx = sx + ux * (len - 2.5), hy = sy + uy * (len - 2.5);
                for (int sgn : new int[]{1, -1}) {
                    double ax = -ux * 0.8 + sgn * -uy * 0.6, ay = -uy * 0.8 + sgn * ux * 0.6;
                    for (double d = 0; d < 4; d += 0.6) g.fill((int) (hx + ax * d), (int) (hy + ay * d), (int) (hx + ax * d) + 1, (int) (hy + ay * d) + 1, col);
                }
            }
        }
        if (drawn < 0.35f) return;
        g.pose().pushPose();
        g.pose().translate(lx, ly, 0);
        g.pose().scale(sc, sc, 1);
        g.drawString(f, m.text(), 0, 0, 0xFFB0201A, false);
        g.pose().popPose();
    }

    void eyes(GuiGraphics g, int cx, int cy, int r, int mx, int my, long now) {
        for (int side : new int[]{-1, 1}) {
            int ex = cx + side * (r * 2 + 1);
            double ang = Math.atan2(my - cy, mx - ex), d = Math.min(r * 0.9, Math.hypot(mx - ex, my - cy) / 40.0);
            g.fill(ex - r, cy - r, ex + r + 1, cy + r + 1, 0xFFE9DCBD);
            int px = ex + (int) Math.round(Math.cos(ang) * d), py = cy + (int) Math.round(Math.sin(ang) * d);
            g.fill(px - Math.max(0, r / 2), py - Math.max(0, r / 2), px + Math.max(1, r / 2 + 1), py + Math.max(1, r / 2 + 1), 0xFF7A1A14);
        }
    }

    // A pencil sketch of the Man in the margin. When he is really standing out there, he looks at your cursor.
    void doodle(GuiGraphics g, int x, int y, int mx, int my) {
        int ink = 0x903B2A1E;
        for (int dy = 0; dy < 12; dy++) {
            int hw = (int) (4.5 * Math.sqrt(1 - Math.pow((dy - 6) / 6.5, 2)));
            g.fill(x - hw, y + dy, x - hw + 1, y + dy + 1, ink);
            g.fill(x + hw, y + dy, x + hw + 1, y + dy + 1, ink);
        }
        g.fill(x - 3, y, x + 4, y + 1, ink);
        g.fill(x - 2, y + 12, x + 3, y + 13, ink);
        g.fill(x - 7, y + 15, x + 8, y + 16, ink);
        g.fill(x - 7, y + 15, x - 6, y + 30, ink);
        g.fill(x + 7, y + 15, x + 8, y + 30, ink);
        g.fill(x, y + 13, x + 1, y + 15, ink);
        if (lurk()) eyes(g, x, y + 5, 1, mx, my, System.currentTimeMillis());
        else {
            g.fill(x - 3, y + 5, x - 1, y + 6, ink);
            g.fill(x + 2, y + 5, x + 4, y + 6, ink);
        }
    }

    // Red wax crayon scrawling itself across the book, then fading.
    void crayon(GuiGraphics g, Font f) {
        if (crayonAt <= 0) return;
        long e = System.currentTimeMillis() - crayonAt;
        if (e < 0) return;
        if (e > 4300) {
            crayonAt = 0;
            return;
        }
        String[] lines = {"ĐÓNG SỔ LẠI.", "NÓ ĐANG Ở SAU LƯNG."};
        int shown = (int) (e / 75);
        int a = e < 3000 ? 255 : (int) (255 * (1f - (e - 3000) / 1300f));
        if (shown % 5 == 0 && shown <= lines[0].length() + lines[1].length() && crayonBeat != shown) {
            crayonBeat = shown;
            Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.VILLAGER_WORK_CARTOGRAPHER, 0.6f + (shown % 3) * 0.1f, 0.7f));
        }
        java.util.Random r = new java.util.Random(4848);
        g.pose().pushPose();
        g.pose().translate(width / 2f, height / 2f - 8, 0);
        g.pose().mulPose(Axis.ZP.rotationDegrees(-7f));
        g.pose().scale(2.2f, 2.2f, 1f);
        int n = 0;
        for (int li = 0; li < lines.length; li++) {
            String ln = lines[li];
            int x0 = -f.width(ln) / 2, y0 = li * 11 - 10;
            for (int i = 0; i < ln.length(); i++, n++) {
                String ch = String.valueOf(ln.charAt(i));
                int jx = r.nextInt(3) - 1, jy = r.nextInt(3) - 1;
                if (n < shown) {
                    g.drawString(f, ch, x0 + jx, y0 + jy, (a * 3 / 5) << 24 | 0xE04030, false);
                    g.drawString(f, ch, x0 + jx, y0 + jy + 1, a << 24 | 0xA01010, false);
                }
                x0 += f.width(ch);
            }
        }
        g.pose().popPose();
    }

    static int noteIndex(JsonObject o) {
        return o.has("i") ? o.get("i").getAsInt() : -1;
    }

    // Ghi chép: 24 empty frames to paste found pages into, stamps that pay out, and pages you must scrub clean to read.
    void notes(GuiGraphics g, Font f, int x, int y, int w, int h, int mx, int my) {
        JsonArray notes = data.getAsJsonArray("notes");
        long pasted = pasted();
        int stamps = Long.bitCount(pasted);
        g.drawString(f, "DẤU ẤN " + stamps + "/30", x, y, RED, false);
        String[][] goals = {{"4", "sức +15%"}, {"8", "pin bền"}, {"12", "công thức"}};
        int gx = x + w;
        for (int i = goals.length - 1; i >= 0; i--) {
            boolean got = stamps >= Integer.parseInt(goals[i][0]);
            String lbl = goals[i][0] + ": " + goals[i][1];
            int lw = (int) (f.width(lbl) * 0.66f) + 4;
            gx -= lw + 2;
            g.fill(gx, y - 1, gx + lw, y + 8, got ? 0xFF7A1A14 : 0x20000000);
            g.pose().pushPose();
            g.pose().translate(gx + 2, y + 1, 0);
            g.pose().scale(0.66f, 0.66f, 1);
            g.drawString(f, lbl, 0, 0, got ? 0xFFF4E2C8 : FAINT, false);
            g.pose().popPose();
        }
        java.util.Map<Integer, JsonObject> found = new java.util.HashMap<>();
        if (notes != null) for (JsonElement e : notes) {
            JsonObject o = e.getAsJsonObject();
            if (noteIndex(o) >= 0) found.put(noteIndex(o), o);
        }
        if (selNote < 0 || !found.containsKey(selNote)) {
            selNote = -1;
            for (int i = 0; i < 24 && selNote < 0; i++) if (found.containsKey(i) && !found.get(i).get("c").getAsBoolean()) selNote = i;
            for (int i = 23; i >= 0 && selNote < 0; i--) if (found.containsKey(i)) selNote = i;
        }
        long now = System.currentTimeMillis();
        int cell = Math.min(22, w / 8), gy = y + 12;
        for (int i = 0; i < 24; i++) {
            int px = x + i % 8 * cell, py = gy + i / 8 * cell, s = cell - 3;
            JsonObject o = found.get(i);
            boolean have = o != null, stuck = (pasted & 1L << i) != 0, hov = mx >= px && mx < px + s && my >= py && my < py + s;
            if (!have) {
                for (int d = 0; d < s; d += 3) {
                    g.fill(px + d, py, px + d + 1, py + 1, 0x60000000 | FAINT & 0xFFFFFF);
                    g.fill(px + d, py + s - 1, px + d + 1, py + s, 0x60000000 | FAINT & 0xFFFFFF);
                    g.fill(px, py + d, px + 1, py + d + 1, 0x60000000 | FAINT & 0xFFFFFF);
                    g.fill(px + s - 1, py + d, px + s, py + d + 1, 0x60000000 | FAINT & 0xFFFFFF);
                }
                continue;
            }
            if (!stuck) {
                float b = 0.5f + 0.5f * Mth.sin(now / 240f + i);
                g.fill(px - 1, py - 1, px + s + 1, py + s + 1, (int) (60 + 120 * b) << 24 | 0xB0201A);
            }
            g.fill(px, py + 1, px + s, py + s, stuck ? 0xFFEFE4C8 : 0xFFD8C8A0);
            for (int t = 0; t < s; t += 2) g.fill(px + t, py, px + t + 1, py + 1 + (t / 2 % 2), stuck ? 0xFFEFE4C8 : 0xFFD8C8A0);
            for (int t = 3; t < s - 3; t += 3) g.fill(px + 2, py + t, px + s - 2, py + t + 1, 0x303B2A1E);
            if (stuck) {
                int cx = px + s / 2, cy = py + s / 2;
                for (int dy = -4; dy <= 4; dy++) {
                    int hw = (int) Math.sqrt(16 - dy * dy);
                    g.fill(cx - hw, cy + dy, cx + hw + 1, cy + dy + 1, 0xB07A1A14);
                }
                g.drawString(f, String.valueOf(i + 1), cx - f.width(String.valueOf(i + 1)) / 2, cy - 4, 0xFFF4E2C8, false);
            }
            if (i == selNote) g.fill(px - 2, py + s + 1, px + s + 2, py + s + 2, RED);
            if (hov) g.fill(px, py, px + s, py + s, 0x20FFFFFF);
            hits.add(new int[]{50 + i, px, py, px + s, py + s});
            tip(Component.literal("Trang " + (i + 1) + ": " + str(o, "h") + (stuck ? "" : "  (bấm để dán)")), px, py, px + s, py + s);
        }
        int ty = gy + cell * 3 + 4;
        g.fill(x, ty, x + w, ty + 1, 0x30000000);
        ty += 5;
        dirtBox = null;
        if (selNote < 0) {
            for (FormattedCharSequence q : f.split(FormattedText.of("Chưa có trang nào. Để ý những cột khói mảnh giữa rừng."), w)) {
                g.drawString(f, q, x, ty, FAINT, false);
                ty += 9;
            }
            return;
        }
        JsonObject o = found.get(selNote);
        g.drawString(f, (selNote + 1) + ". " + str(o, "h"), x, ty, RED, false);
        ty += 12;
        int bottom = y + h - 10;
        List<FormattedCharSequence> body = f.split(FormattedText.of(str(o, "b")), w);
        maxScroll = Math.max(0, body.size() * 9 - (bottom - ty));
        scroll = Mth.clamp(scroll, 0, maxScroll);
        g.enableScissor(x - 2, ty - 1, x + w + 2, bottom);
        int yy = ty - scroll;
        for (FormattedCharSequence q : body) {
            g.drawString(f, q, x, yy, INK, false);
            yy += 9;
        }
        boolean clean = o.get("c").getAsBoolean();
        if (!clean) {
            float[] dirt = DIRT.computeIfAbsent(selNote, k -> {
                float[] d = new float[DIRT_W * DIRT_H];
                java.util.Arrays.fill(d, 1f);
                return d;
            });
            int dh = Math.min(bottom - ty, Math.max(36, body.size() * 9));
            dirtBox = new int[]{x - 2, ty - 1, w + 4, dh};
            boolean blood = selNote % 3 == 0;
            java.util.Random r = new java.util.Random(selNote * 7919L);
            int cw = (w + 4) / DIRT_W + 1, chh = dh / DIRT_H + 1;
            for (int j = 0; j < DIRT_H; j++) {
                for (int i = 0; i < DIRT_W; i++) {
                    float a = dirt[j * DIRT_W + i];
                    for (int sj = 0; sj < 2; sj++) for (int si = 0; si < 2; si++) {
                        float jitter = 0.75f + r.nextFloat() * 0.25f;
                        if (a < 0.02f) continue;
                        int c = blood ? (r.nextBoolean() ? 0x4A1210 : 0x3A0C0A) : (r.nextBoolean() ? 0x3A2A1C : 0x2E2218);
                        int x0 = x - 2 + i * cw + si * cw / 2, y0 = ty - 1 + j * chh + sj * chh / 2;
                        g.fill(x0, y0, x0 + cw / 2 + 1, y0 + chh / 2 + 1, (int) (Math.min(1f, a) * jitter * 245) << 24 | c);
                    }
                }
            }
        }
        g.disableScissor();
        g.pose().pushPose();
        g.pose().translate(x, y + h - 8, 0);
        g.pose().scale(0.75f, 0.75f, 1);
        g.drawString(f, clean ? "bấm vào khung để đọc trang khác" : "giữ chuột trái, chà lên vết bẩn để đọc", 0, 0, clean ? FAINT : RED, false);
        g.pose().popPose();
    }

    void scrub(double mx, double my) {
        if (dirtBox == null || selNote < 0) return;
        float[] dirt = DIRT.get(selNote);
        if (dirt == null) return;
        double u = (mx - dirtBox[0]) / dirtBox[2] * DIRT_W, v = (my - dirtBox[1]) / dirtBox[3] * DIRT_H;
        if (u < -1 || v < -1 || u > DIRT_W + 1 || v > DIRT_H + 1) return;
        float sum = 0;
        for (int j = 0; j < DIRT_H; j++) {
            for (int i = 0; i < DIRT_W; i++) {
                double d = Math.hypot(i + 0.5 - u, (j + 0.5 - v) * 1.4);
                if (d < 1.8) dirt[j * DIRT_W + i] = Math.max(0f, dirt[j * DIRT_W + i] - (float) (0.22 * (1.8 - d)));
                sum += dirt[j * DIRT_W + i];
            }
        }
        if (++scrubs % 5 == 0) Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.BRUSH_GENERIC, 0.9f + (scrubs % 4) * 0.08f, 0.55f));
        if (sum / dirt.length > 0.16f) return;
        JsonArray notes = data.getAsJsonArray("notes");
        if (notes != null) for (JsonElement e : notes) if (noteIndex(e.getAsJsonObject()) == selNote) e.getAsJsonObject().addProperty("c", true);
        DIRT.remove(selNote);
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.BRUSH_SAND_COMPLETED, 1.1f, 0.7f));
        BookClient.ask(vn.cavemind.net.Net.Ask.CLEAN, selNote);
    }

    void paste(int idx) {
        long bit = 1L << idx;
        if ((pasted() & bit) != 0) return;
        data.addProperty("pasted", pasted() | bit);
        glowAt = System.currentTimeMillis();
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.AMETHYST_BLOCK_CHIME, 1.3f, 0.9f));
        page(0.7f);
        BookClient.ask(vn.cavemind.net.Net.Ask.PASTE, idx);
    }

    @Override
    public boolean mouseDragged(double mx, double my, int button, double dx, double dy) {
        if (tab == NOTES && button == 0) {
            scrub(mx, my);
            return true;
        }
        return super.mouseDragged(mx, my, button, dx, dy);
    }

    void chapters(GuiGraphics g, Font f, int x, int y, int w, int h, int mx, int my) {
        JsonArray chs = data.getAsJsonArray("chapters");
        if (chs == null) return;
        int cur = data.has("chapter") ? data.get("chapter").getAsInt() : 1;
        if (node < 0) node = Mth.clamp(cur + 1 <= 6 ? cur + 1 : cur, 1, 6) - 1;
        int[][] pos = new int[6][];
        for (int i = 0; i < 6; i++) {
            int px = x + 12 + i * (w - 24) / 5, py = y + 26 + (i % 2 == 0 ? -10 : 10);
            pos[i] = new int[]{px, py};
        }
        for (int i = 0; i < 5; i++) {
            boolean lit = i + 1 < cur;
            for (int s = 0; s <= 10; s++) {
                if (s % 2 == 1) continue;
                int px = pos[i][0] + (pos[i + 1][0] - pos[i][0]) * s / 10, py = pos[i][1] + (pos[i + 1][1] - pos[i][1]) * s / 10;
                g.fill(px - 1, py, px + 1, py + 1, lit ? RED : FAINT);
            }
        }
        long now = System.currentTimeMillis();
        for (int i = 0; i < 6; i++) {
            JsonObject o = chs.get(i).getAsJsonObject();
            boolean open = o.get("open").getAsBoolean(), next = i + 1 == cur + 1, sel = i == node;
            int px = pos[i][0], py = pos[i][1];
            boolean hov = mx >= px - 11 && mx < px + 11 && my >= py - 11 && my < py + 11;
            int r = sel ? 11 : 10;
            if (next) {
                float b = 0.5f + 0.5f * Mth.sin(now / 260f);
                g.fill(px - r - 2, py - r - 2, px + r + 2, py + r + 2, (int) (40 + 60 * b) << 24 | 0x7A1A14);
            }
            g.fill(px - r, py - r, px + r, py + r, sel ? RED : hov ? 0xFF5A3A2A : INK);
            g.fill(px - r + 1, py - r + 1, px + r - 1, py + r - 1, open ? 0xFFEFE4C8 : 0xFF4A3A2A);
            if (open) g.renderItem(stack(TOYS[i]), px - 8, py - 8);
            else g.drawString(f, "?", px - f.width("?") / 2, py - 4, 0xFFB8A888, false);
            boolean stamped = o.has("st") && o.get("st").getAsBoolean();
            if (open && !stamped) {
                float b = 0.5f + 0.5f * Mth.sin(now / 200f + i);
                g.fill(px + r - 4, py - r - 2, px + r + 2, py - r + 4, (int) (120 + 135 * b) << 24 | 0xB0201A);
            } else if (stamped) g.fill(px + r - 4, py - r - 2, px + r + 2, py - r + 4, 0xFF7A1A14);
            g.drawString(f, String.valueOf(i + 1), px - 2, py + r + 2, sel ? RED : FAINT, false);
            hits.add(new int[]{20 + i, px - 11, py - 11, px + 11, py + 11});
            tip(Component.literal("Chương " + (i + 1) + ": " + str(o, "t") + (open ? "" : " (khóa)")), px - 11, py - 11, px + 11, py + 11);
        }
        JsonObject o = chs.get(node).getAsJsonObject();
        boolean open = o.get("open").getAsBoolean();
        int ty = y + 58;
        g.fill(x, ty - 4, x + w, ty - 3, 0x30000000);
        g.drawString(f, "Chương " + (node + 1) + (open ? "" : "  (chưa mở)"), x, ty, open ? RED : FAINT, false);
        ty += 11;
        for (FormattedCharSequence s : f.split(FormattedText.of(str(o, "t"), Style.EMPTY.withItalic(true)), w)) {
            g.drawString(f, s, x, ty, INK, false);
            ty += 9;
        }
        ty += 4;
        String child = str(o, "c");
        String more;
        if (node == 0) more = "Chương mở đầu. Sống sót những đêm đầu và làm nhiệm vụ trong sổ nhiệm vụ để có lời mời đầu tiên.";
        else if (open) more = "Đã mở. Món đồ chơi của " + child + " giờ là chìa khóa cho các ván trốn tìm ở cây đếm.";
        else if (node == cur) more = "Chương tiếp theo. " + child + " đang chờ. Xem trang Mục tiêu để biết bước kế tiếp.";
        else more = "Còn khóa. " + child + " vẫn đang trốn ở đâu đó.";
        for (FormattedCharSequence s : f.split(FormattedText.of(more), w)) {
            g.drawString(f, s, x, ty, open || node == cur ? INK : FAINT, false);
            ty += 9;
        }
        if (open && node > 0) {
            ty += 6;
            g.fill(x, ty, x + 20, ty + 20, 0x18000000);
            g.renderItem(stack(TOYS[node]), x + 2, ty + 2);
            g.drawString(f, "Món đồ chơi của " + child, x + 26, ty + 6, INK, false);
        }
        if (open) {
            ty += 26;
            boolean stamped = o.has("st") && o.get("st").getAsBoolean();
            if (stamped) stamp(g, f, x + 40, ty + 4);
            else {
                String lbl = "[ Đóng dấu chương ]";
                int lw = f.width(lbl) + 6;
                boolean hov = mx >= x && mx < x + lw && my >= ty - 2 && my < ty + 11;
                float b = 0.5f + 0.5f * Mth.sin(now / 220f);
                g.fill(x, ty - 2, x + lw, ty + 11, hov ? 0xFF9A2A20 : (int) (150 + 100 * b) << 24 | 0x7A1A14);
                g.drawString(f, lbl, x + 3, ty, 0xFFF4E2C8, false);
                hits.add(new int[]{80 + node, x, ty - 2, x + lw, ty + 11});
                tip(Component.literal("Một dấu ấn mới cho cuốn sổ"), x, ty - 2, x + lw, ty + 11);
            }
        }
        g.pose().pushPose();
        g.pose().translate(x, y + h - 8, 0);
        g.pose().scale(0.75f, 0.75f, 1);
        g.drawString(f, "bấm vào từng chương để xem", 0, 0, FAINT, false);
        g.pose().popPose();
    }

    void moonDisk(GuiGraphics g, int cx, int cy, int r, int col, boolean glow) {
        long t = System.currentTimeMillis();
        if (glow) {
            float b = 0.5f + 0.5f * Mth.sin(t / 400f);
            for (int i = r + 5; i > r; i--) {
                int a = (int) ((30 + 40 * b) * (1f - (i - r) / 6f));
                for (int dy = -i; dy <= i; dy++) {
                    int hw = (int) Math.sqrt(i * i - dy * dy);
                    g.fill(cx - hw, cy + dy, cx + hw + 1, cy + dy + 1, a << 24 | col & 0xFFFFFF);
                }
            }
        }
        for (int dy = -r; dy <= r; dy++) {
            int hw = (int) Math.sqrt(r * r - dy * dy);
            g.fill(cx - hw, cy + dy, cx + hw + 1, cy + dy + 1, col);
        }
        for (int[] c : new int[][]{{-r / 3, -r / 4, r / 4}, {r / 4, r / 3, r / 5}, {r / 5, -r / 2, r / 6}}) {
            int rr = Math.max(1, c[2]);
            for (int dy = -rr; dy <= rr; dy++) {
                int hw = (int) Math.sqrt(rr * rr - dy * dy);
                g.fill(cx + c[0] - hw, cy + c[1] + dy, cx + c[0] + hw + 1, cy + c[1] + dy + 1, 0x30000000);
            }
        }
    }

    void gear(GuiGraphics g, Font f, int x, int y, int w, int h, int mx, int my) {
        String tonight = str(data, "moon");
        int n = GEARS.length, cell = Math.min(20, (w - 2) / n);
        for (int i = 0; i < n; i++) {
            Entry e = GEARS[i];
            int px = x + i * cell, py = y;
            boolean sel = i == gear, hov = mx >= px && mx < px + cell - 1 && my >= py && my < py + 19;
            g.fill(px, py, px + cell - 1, py + 19, sel ? RED : hov ? 0xFF5A3A2A : 0x30000000);
            g.fill(px + 1, py + 1, px + cell - 2, py + 18, sel ? 0xFFEFE4C8 : 0xFFE2D4B0);
            if (e.id() != null) g.renderItem(stack(e.id()), px + (cell - 17) / 2, py + 2);
            else moonDisk(g, px + (cell - 1) / 2, py + 9, 5, e.color(), e.moon().equals(tonight));
            hits.add(new int[]{30 + i, px, py, px + cell - 1, py + 19});
            tip(Component.literal(e.name() + (e.moon() != null && e.moon().equals(tonight) ? "  (đêm nay)" : "")), px, py, px + cell - 1, py + 19);
        }
        Entry e = GEARS[gear];
        int ty = y + 26;
        if (e.id() != null) {
            g.pose().pushPose();
            g.pose().translate(x, ty, 0);
            g.pose().scale(2f, 2f, 1f);
            g.renderItem(stack(e.id()), 0, 0);
            g.pose().popPose();
        } else moonDisk(g, x + 16, ty + 16, 13, e.color(), e.moon().equals(tonight));
        g.drawString(f, e.name(), x + 38, ty + 4, RED, false);
        if (e.moon() != null && e.moon().equals(tonight)) {
            int bw2 = f.width("ĐÊM NAY") + 6;
            g.fill(x + 38, ty + 15, x + 38 + bw2, ty + 26, 0xFF7A1A14);
            g.drawString(f, "ĐÊM NAY", x + 41, ty + 17, 0xFFF4E2C8, false);
        } else if (e.moon() != null) g.drawString(f, "đêm trăng của Lunar", x + 38, ty + 16, FAINT, false);
        ty += 38;
        int gridW = e.grid() != null ? 58 : 0;
        int tw = w - gridW;
        g.enableScissor(x - 2, ty - 2, x + w + 2, y + h);
        int yy = ty;
        for (String[] part : e.moon() != null ? new String[][]{{"Trăng này", e.from()}, {"Với những thứ trong sương", e.use()}, {"Đáng để liều", e.cost()}} : new String[][]{{"Lấy ở đâu", e.from()}, {"Dùng thế nào", e.use()}, {"Cái giá", e.cost()}}) {
            g.drawString(f, part[0], x, yy, RED, false);
            yy += 10;
            StringBuilder line = new StringBuilder();
            for (String word : part[1].split(" ")) {
                int lim = yy < ty + 92 ? tw : w;
                String next = line.length() == 0 ? word : line + " " + word;
                if (f.width(next) > lim && line.length() > 0) {
                    g.drawString(f, line.toString(), x, yy, INK, false);
                    yy += 9;
                    line = new StringBuilder(word);
                } else line = new StringBuilder(next);
            }
            if (line.length() > 0) {
                g.drawString(f, line.toString(), x, yy, INK, false);
                yy += 9;
            }
            yy += 3;
        }
        g.disableScissor();
        if (e.grid() != null) {
            int gx = x + w - 54, gy = ty;
            g.fill(gx - 2, gy - 2, gx + 54, gy + 54, 0x30000000);
            for (int i = 0; i < 9; i++) {
                int cx = gx + i % 3 * 18, cy = gy + i / 3 * 18;
                g.fill(cx, cy, cx + 17, cy + 17, 0xFFD2C29C);
                ItemStack st = e.grid()[i].isEmpty() ? ItemStack.EMPTY : stack(e.grid()[i]);
                if (!st.isEmpty()) {
                    g.renderItem(st, cx, cy);
                    tip(st.getHoverName(), cx, cy, cx + 17, cy + 17);
                }
            }
            g.drawString(f, "↓", gx + 24, gy + 57, FAINT, false);
            g.fill(gx + 17, gy + 67, gx + 37, gy + 87, 0xFF7A1A14);
            g.fill(gx + 18, gy + 68, gx + 36, gy + 86, 0xFFEFE4C8);
            g.renderItem(stack(e.id()), gx + 19, gy + 69);
        }
    }

    record Line(FormattedCharSequence s, int color, int gap, ItemStack icon) {
        Line(FormattedCharSequence s, int color, int gap) {
            this(s, color, gap, ItemStack.EMPTY);
        }
    }

    static final String[] RULE_ICONS = {"minecraft:bell", "minecraft:rose_bush", "minecraft:iron_pickaxe", "minecraft:red_bed", "cavemind:trang_nhat_ky", "minecraft:totem_of_undying"};
    static final String[] MAZE_ICONS = {"minecraft:candle", "minecraft:chest", "cavemind:con_quay_linh", "cavemind:pin_may_quay", "cavemind:dong_ho_mai", "minecraft:sugar", "minecraft:fermented_spider_eye",
        "minecraft:oak_door", "minecraft:iron_sword"};

    void iconHeading(List<Line> out, Font f, String s, int w, String icon) {
        ItemStack st = stack(icon);
        List<FormattedCharSequence> parts = f.split(FormattedText.of(s), w - 20);
        for (int i = 0; i < parts.size(); i++) out.add(new Line(parts.get(i), RED, i == 0 ? 6 : 0, i == 0 ? st : ItemStack.EMPTY));
    }

    void heading(List<Line> out, Font f, String s, int w) {
        for (FormattedCharSequence q : f.split(FormattedText.of(s), w)) out.add(new Line(q, RED, 0));
    }

    void body(List<Line> out, Font f, String s, int w, int color) {
        for (FormattedCharSequence q : f.split(FormattedText.of(s), w)) out.add(new Line(q, color, 0));
        out.add(new Line(FormattedCharSequence.EMPTY, 0, 5));
    }

    void sections(List<Line> out, Font f, JsonArray a, int w) {
        if (a == null) return;
        for (JsonElement e : a) {
            JsonObject o = e.getAsJsonObject();
            heading(out, f, str(o, "h"), w);
            body(out, f, str(o, "b"), w, INK);
        }
    }

    void content(GuiGraphics g, Font f, int x, int y, int w, int h) {
        List<Line> lines = new ArrayList<>();
        switch (tab) {
            case 1 -> {
                JsonArray steps = data.getAsJsonArray("steps");
                if (steps != null && !steps.isEmpty()) {
                    heading(lines, f, "Cổng chương tiếp theo", w);
                    for (JsonElement e : steps) {
                        JsonObject o = e.getAsJsonObject();
                        boolean d = o.get("d").getAsBoolean();
                        for (FormattedCharSequence q : f.split(FormattedText.of((d ? "✔ " : "○ ") + str(o, "t"), d ? Style.EMPTY.withStrikethrough(true) : Style.EMPTY), w)) {
                            lines.add(new Line(q, d ? FAINT : INK, 0));
                        }
                    }
                    lines.add(new Line(FormattedCharSequence.EMPTY, 0, 6));
                }
                sections(lines, f, data.getAsJsonArray("goals"), w);
            }
            case 3 -> {
                for (int i = 0; i < RULES.length; i++) {
                    iconHeading(lines, f, RULES[i][0], w, RULE_ICONS[i % RULE_ICONS.length]);
                    body(lines, f, RULES[i][1], w, INK);
                }
            }
            case 4 -> {
                for (int i = 0; i < MAZE.length; i++) {
                    iconHeading(lines, f, MAZE[i][0], w, MAZE_ICONS[i % MAZE_ICONS.length]);
                    body(lines, f, MAZE[i][1], w, INK);
                }
            }
            default -> {
                JsonArray notes = data.getAsJsonArray("notes");
                if (notes == null || notes.isEmpty()) body(lines, f, "Chưa có trang nào. Đi lang thang và để ý những cột khói mảnh giữa rừng.", w, FAINT);
                else sections(lines, f, notes, w);
            }
        }
        int total = 0;
        for (Line l : lines) total += 9 + l.gap();
        maxScroll = Math.max(0, total - h);
        scroll = Mth.clamp(scroll, 0, maxScroll);
        g.enableScissor(x - 2, y - 2, x + w + 2, y + h);
        int yy = y - scroll;
        for (Line l : lines) {
            if (!l.icon().isEmpty()) {
                yy += 3;
                if (yy > y - 16 && yy < y + h) {
                    g.fill(x, yy - 4, x + 16, yy + 12, 0x18000000);
                    g.renderItem(l.icon(), x, yy - 4);
                    g.drawString(font, l.s(), x + 20, yy, l.color(), false);
                }
            } else if (yy > y - 10 && yy < y + h) g.drawString(font, l.s(), x, yy, l.color(), false);
            yy += 9 + l.gap();
        }
        g.disableScissor();
        if (maxScroll > 0) {
            int th = Math.max(12, h * h / (total));
            int ty = y + (int) ((h - th) * (scroll / (float) maxScroll));
            g.fill(x + w + 4, y, x + w + 6, y + h, 0x20000000);
            g.fill(x + w + 4, ty, x + w + 6, ty + th, 0x807A1A14);
        }
    }

    void page(float pitch) {
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.BOOK_PAGE_TURN, pitch, 0.8f));
    }

    void go(int to) {
        to = Mth.clamp(to, 0, CARDS.length - 1);
        if (to == card) return;
        cardFrom = card;
        card = to;
        flipped = false;
        turned = System.currentTimeMillis();
        page(1.1f);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double d) {
        if (tab == GUIDE) {
            go(card - (int) Math.signum(d));
            return true;
        }
        scroll = Mth.clamp(scroll - (int) (d * 18), 0, maxScroll);
        return true;
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        for (int i = 0; i < tabBoxes.size(); i++) {
            if (in(mx, my, tabBoxes.get(i))) {
                if (tab != i) {
                    tab = i;
                    scroll = 0;
                    BookClient.lastPage = i;
                    page(0.8f);
                }
                return true;
            }
        }
        for (int[] h : hits) {
            if (!(mx >= h[1] && mx < h[3] && my >= h[2] && my < h[4])) continue;
            int id = h[0];
            if (id == 0) {
                flipped = !flipped;
                page(1.4f);
            } else if (id == 1) go(card - 1);
            else if (id == 2) go(card + 1);
            else if (id >= 10 && id < 20) go(id - 10);
            else if (id >= 30 && id < 30 + GEARS.length) {
                if (gear != id - 30) page(1.2f);
                gear = id - 30;
            } else if (id >= 20 && id < 26) {
                if (node != id - 20) page(1.2f);
                node = id - 20;
            } else if (id >= 50 && id < 74) {
                int idx = id - 50;
                if (selNote != idx) {
                    selNote = idx;
                    scroll = 0;
                    page(1.2f);
                }
                paste(idx);
            } else if (id >= 80 && id < 86) {
                paste(vn.cavemind.chapter.Book.STAMP_CHAPTER + id - 80);
                JsonArray chs = data.getAsJsonArray("chapters");
                if (chs != null) chs.get(id - 80).getAsJsonObject().addProperty("st", true);
            }
            return true;
        }
        if (tab == NOTES && button == 0 && dirtBox != null) {
            scrub(mx, my);
            return true;
        }
        if (data.has("host") && data.get("host").getAsBoolean() && mx >= modeX && mx < modeX + modeW && my >= modeY && my < modeY + 12) {
            Minecraft.getInstance().setScreen(new ModeScreen());
            return true;
        }
        return super.mouseClicked(mx, my, button);
    }

    @Override
    public boolean keyPressed(int key, int scan, int mods) {
        if (BookClient.KEY.matches(key, scan) || Minecraft.getInstance().options.keyInventory.matches(key, scan)) {
            onClose();
            return true;
        }
        if (tab == GUIDE && (key == org.lwjgl.glfw.GLFW.GLFW_KEY_LEFT || key == org.lwjgl.glfw.GLFW.GLFW_KEY_RIGHT)) {
            go(card + (key == org.lwjgl.glfw.GLFW.GLFW_KEY_RIGHT ? 1 : -1));
            return true;
        }
        if (tab == GUIDE && key == org.lwjgl.glfw.GLFW.GLFW_KEY_SPACE) {
            flipped = !flipped;
            page(1.4f);
            return true;
        }
        if (key == org.lwjgl.glfw.GLFW.GLFW_KEY_DOWN || key == org.lwjgl.glfw.GLFW.GLFW_KEY_PAGE_DOWN) {
            scroll = Mth.clamp(scroll + 27, 0, maxScroll);
            return true;
        }
        if (key == org.lwjgl.glfw.GLFW.GLFW_KEY_UP || key == org.lwjgl.glfw.GLFW.GLFW_KEY_PAGE_UP) {
            scroll = Mth.clamp(scroll - 27, 0, maxScroll);
            return true;
        }
        return super.keyPressed(key, scan, mods);
    }

    @Override
    public void onClose() {
        BookClient.lastPage = tab;
        super.onClose();
    }
}
