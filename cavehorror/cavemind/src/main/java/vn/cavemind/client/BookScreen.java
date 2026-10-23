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
    static final int GUIDE = 0, CHAPTERS = 2, GEAR = 5;

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
        new Card("so", "Cuốn sổ này", "Bấm J hoặc chuột phải để mở. Sổ không vứt được, mất thì tự quay lại túi. Lúc nào không biết làm gì, mở trang Mục tiêu.",
            "Lật từng thẻ ở trang này theo thứ tự. Dấu đỏ ĐÃ TRẢI QUA hiện lên khi bạn đã thật sự gặp chuyện đó. Lạc đường thì thả lọ đom đóm.",
            new Thing("cavemind:so_chuong", "Sổ Chương: mở bằng phím J"), new Thing("cavemind:lo_dom_dom", "Lọ đom đóm: bay về phía nơi bạn cần tới")),
        new Card("man", "Người trong sương", "Thanh THE MAN ở góc màn hình đầy dần khi bạn sống sót. Tối thì ở trong nhà có đèn và đóng cửa. Ngủ không né được hắn: hắn tới từ lúc chạng vạng.",
            "Nghe hắn ở gần thì rung chuông để đuổi. Rung mãi thì hắn quen tiếng chuông và không sợ nữa.",
            new Thing("minecraft:bell", "Chuông: rung để đuổi hắn"), new Thing("minecraft:lantern", "Ánh sáng giữ hắn ở xa hơn")),
        new Card("dem", "Đứa trẻ úp mặt vào cây", "Nghe tiếng đếm và thấy một đứa trẻ úp mặt vào thân cây: nó đang rủ bạn chơi trốn tìm. Nó đếm tới bốn mươi tám rồi đi tìm.",
            "Đừng bỏ về nhà, đừng đào hay đánh nó, đừng nhìn trộm lúc nó đếm. Phạm luật nhiều lần là nó đi săn bạn.",
            new Thing("minecraft:candle", "Thắng ván, nó để lại một ngọn nến tắt")),
        new Card("tron", "Cách trốn", "Núp sau cây, bụi, đá hoặc vào nhà. Giữ Shift ngồi thụp và đứng im. Đèn quanh bạn sẽ tắt: chỗ tối, có che chắn là chỗ tốt.",
            "Khi chữ NÍN THỞ hiện lên, nó đang đứng ngay sau lưng bạn. Giữ Shift, không bước, không xoay chuột cho tới khi tiếng thở đi xa."),
        new Card("chuong", "Mở chương mới", "Làm nhiệm vụ của chương trong sổ nhiệm vụ. Xong thì một đứa trẻ gửi lời mời và la bàn sương vào túi bạn. Đi theo la bàn tới điểm hẹn.",
            "Ở điểm hẹn: đọc cuốn sổ trên bục, thắp hết nến, nhặt món đồ chơi dưới cây đếm. Tối đến, cầm đồ chơi chạm vào cây để rủ nó chơi.",
            new Thing("minecraft:compass", "La bàn sương chỉ đường tới điểm hẹn"), new Thing("minecraft:candle", "Thắp hết nến ở điểm hẹn"),
            new Thing("cavemind:con_quay_linh", "Món đồ chơi: cầm nó chạm vào cây đếm")),
        new Card("gieng", "Sân chơi bỏ hoang", "Sau vài ngày, một sân chơi có cây đếm, giếng sắt số 4 và chuông trường hiện ra đâu đó gần bạn. Tìm cột khói mảnh bốc lên giữa rừng, hoặc thả lọ đom đóm.",
            "Luật chơi nằm trong cái rương dưới gốc cây đếm. Tấm biển ở đó không nói dối. Đừng phá thứ gì trong sân chơi, nó nhớ đấy.",
            new Thing("cavemind:lo_dom_dom", "Lọ đom đóm: chuột phải để thả, Shift + chuột phải để đổi thứ cần tìm"), new Thing("minecraft:chest", "Rương dưới cây đếm: luật chơi"), new Thing("minecraft:bell", "Chuông trường")),
        new Card("me", "Giờ Thứ Bốn Mươi Tám", "Đặt nến đang cháy lên nắp giếng rồi rung chuông trường: nắp mở, nhảy xuống là vào mê cung. Bạn bè nhảy kịp sẽ chơi cùng ván.",
            "Dưới đó tối đen: bấm N bật nhìn đêm, nhặt pin. Gom bảy món đồ chơi về rương rồi rung chuông, hoặc trốn tới khi chuông tự vang.",
            new Thing("minecraft:candle", "Nến đang cháy đặt lên nắp giếng"), new Thing("minecraft:bell", "Rồi rung chuông trường")),
        new Card("trang", "Trang nhật ký rách", "Thấy một cột khói mảnh bốc lên là có một trang ở dưới đó, cạnh ngọn nến trên tấm đá rêu. Có hai mươi bốn trang, mỗi trang kể một mảnh chuyện.",
            "Mỗi trang cho nến và pin. Cứ bốn trang có một món quà, trang mười hai và hai mươi bốn có quà hiếm.",
            new Thing("cavemind:trang_nhat_ky", "Trang nhật ký rách")),
        new Card("an", "Phần còn lại: tự tìm", "Có thứ nhớ những gì bạn đào. Một bông hồng còn sống đáng giá hơn bạn nghĩ. Có ba cách để mọi chuyện kết thúc.",
            "Sổ này chỉ kể tới đây. Phần sâu hơn nằm ngoài kia, trong bóng tối.")
    };
    static final String[] TOYS = {"cavemind:so_chuong", "cavemind:con_quay_linh", "cavemind:bi_ve_tuan", "cavemind:dong_ho_mai", "cavemind:dieu_giay_bao", "cavemind:coi_go_nam"};
    static final String[][] RULES = {
        {"The Man, người trong sương", "Hắn cao, gầy và rất kiên nhẫn. Thanh THE MAN ở góc màn hình đầy dần khi bạn sống sót. Mỗi mốc là một việc hắn chắc chắn làm: ghé nhà khi bạn vắng, tới thật gần lúc trời tối, thử bạn trong bóng tối, rồi một đêm săn không thể ngủ qua. Ngủ không giúp bạn né hắn: hắn tới từ lúc chạng vạng. Rung chuông để đuổi hắn, nhưng rung nhiều thì hắn quen."},
        {"The Damned, đứa trẻ thứ tám", "Nó chỉ muốn có người chơi cùng. Khi nó rủ chơi trốn tìm: ngồi thụp, đứng yên, ở chỗ tối, và đừng nhìn thẳng vào nó. Cầm bông hồng sống để xua nó đi."},
        {"Herobrine", "Nó nhớ những gì bạn đào và những gì bạn xây. Đào thẳng xuống một mình là gọi nó tới. Những tấm biển lạ là nó đang nói chuyện với bạn."},
        {"Nhà của bạn", "Những thứ trong sương nhận ra nhà bạn từ nơi bạn ngủ nhiều đêm, nơi bạn ở lâu và nơi bạn cất đồ. Ngủ tạm một đêm ở chỗ khác không làm chúng lạc dấu. Dọn hẳn sang nhà mới thì vài đêm sau chúng sẽ biết."},
        {"Trang nhật ký rách", "Hai mươi bốn trang nằm rải rác quanh bạn, cạnh một ngọn nến trên đá rêu. Mỗi trang có một cột khói mảnh bốc lên, nhìn thấy từ xa. Mỗi trang cho nến và pin, cứ bốn trang có một món quà, trang thứ mười hai và hai mươi bốn có quà hiếm."},
        {"Chơi chung hay chơi riêng", "Chơi chung: khi một người mở chương, cả nhóm cùng sang chương đó, và ai nhảy xuống giếng trong lúc nắp mở sẽ chơi cùng một ván trốn tìm. Chơi riêng: mỗi người tự đi chương của mình. Chủ thế giới chọn khi mới vào và đổi được ở đây."}
    };
    static final String[][] MAZE = {
        {"Vào mê cung", "Đặt một ngọn nến đang cháy ngay trên nắp giếng sắt số bốn ở sân chơi, rồi rung cái chuông của trường. Nắp mở trong một lúc. Nhảy xuống. Bạn bè nhảy theo trước khi nắp đóng sẽ chơi cùng ván."},
        {"Mục tiêu", "Nó đếm bốn mươi tám nhịp rồi đi tìm. Nhặt đủ bảy món đồ chơi, mang về rương ở phòng giếng và rung chuông. Nếu không, hãy trốn cho tới khi chuông tự vang."},
        {"Máy quay", "Mê cung tối đen. Bấm [N] để bật nhìn đêm, nó ăn pin rất nhanh. Pin nằm rải rác ở những chỗ tối nhất. Hết pin thì bạn chỉ còn tai."},
        {"Thanh SỨC", "Chạy và nín thở đều tốn sức. Hết sức bạn sẽ thở hắt ra, nó nghe thấy, và bạn phải đi chậm một lúc. Đừng chạy hết sức rồi mới chui vào tủ."},
        {"Thanh SỢ", "Nỗi sợ tăng khi nó ở gần, khi bạn ở trong bóng tối không có máy quay, và vọt lên khi nó thấy bạn. Sợ quá bạn sẽ thở dốc, và nó nghe được tiếng thở ấy."},
        {"Chỗ trốn", "Tủ áo, tủ khóa trong lớp học, buồng tắm, và những hốc tối giữa hai kệ sách. Trong tủ: khi nó đứng ngoài cửa áp tai nghe, hãy ngồi thụp để nín thở. Trong hốc tối: ngồi thụp và đứng yên. Nó có thể giả vờ bỏ đi rồi đứng chờ ngay ngoài cửa."},
        {"Khi nó đuổi", "Nó lao tới rất nhanh khi ở gần. Chạy qua chỗ rẽ để cắt tầm nhìn, đánh nó một cái sẽ làm nó khựng lại. Bị bắt thì bạn bị đẩy ra ngoài, những người còn lại vẫn chơi tiếp."}
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

    public BookScreen(JsonObject data, int page) {
        super(Component.literal("Sổ Chương"));
        this.data = data;
        int p = data.has("page") ? data.get("page").getAsInt() : -1;
        this.tab = p >= 0 ? p : page >= 0 ? page : beginner() ? GUIDE : 1;
        if (this.tab < 0 || this.tab >= TABS.length) this.tab = 0;
        this.card = firstOpen();
        this.opened = System.currentTimeMillis();
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
        else content(g, f, cx, cy, cw, chh);
        g.pose().popPose();
        super.render(g, mx, my, pt);
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
    static final String[] MAZE_ICONS = {"minecraft:candle", "cavemind:con_quay_linh", "cavemind:pin_may_quay", "minecraft:sugar", "minecraft:fermented_spider_eye", "minecraft:oak_door", "minecraft:iron_sword"};

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
            }
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
