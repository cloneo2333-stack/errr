package vn.cavemind.chapter;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import vn.cavemind.CaveMind;
import vn.cavemind.lore.Pages;
import vn.cavemind.lore.Points;
import vn.cavemind.man.Meter;
import vn.cavemind.maze.Maze;
import vn.cavemind.memory.MindData;
import vn.cavemind.memory.Profile;
import vn.cavemind.net.Net;
import vn.cavemind.world.Fx;

public final class Book {
    public static final String TAG = "cavemind_book";
    static final String[] PLACE = {"", "", "nhà kho bên giếng cạn", "", "tháp đồng hồ đổ", "", "trường làng trong sương"};
    private static final Map<UUID, Long> ASKED = new HashMap<>();

    public static final class BookItem extends Item {
        public BookItem() {
            super(new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON).fireResistant());
        }

        @Override
        public InteractionResultHolder<ItemStack> use(Level lvl, Player p, InteractionHand hand) {
            if (p instanceof ServerPlayer sp) open(sp, -1);
            return InteractionResultHolder.sidedSuccess(p.getItemInHand(hand), lvl.isClientSide());
        }

        @Override
        public void appendHoverText(ItemStack st, Level lvl, List<Component> lines, TooltipFlag flag) {
            lines.add(Component.literal("Ghi lại mọi điều bạn cần biết để sống sót.").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
            lines.add(Component.literal("Không thể vứt bỏ. Mở bằng chuột phải hoặc phím [J].").withStyle(ChatFormatting.DARK_GRAY));
        }

        @Override
        public boolean isFoil(ItemStack st) {
            return false;
        }
    }

    public static boolean is(ItemStack st) {
        return !st.isEmpty() && st.getItem() == Keys.SO.get();
    }

    public static void clear() {
        ASKED.clear();
    }

    static boolean has(ServerPlayer p) {
        var inv = p.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) if (is(inv.getItem(i))) return true;
        return is(p.containerMenu.getCarried());
    }

    public static void ensure(ServerPlayer p) {
        if (p.isSpectator() || Maze.inMaze(p)) return;
        Profile pr = MindData.profile(p);
        if (!pr.gotFinder) {
            ItemStack jar = new ItemStack(vn.cavemind.gear.Gear.FINDER.get());
            if (p.getInventory().add(jar)) {
                pr.gotFinder = true;
                p.displayClientMessage(Component.literal("Trong túi bạn có một lọ đom đóm. Bấm chuột phải để thả nó ra: nó bay về phía nơi bạn cần tới.").withStyle(ChatFormatting.YELLOW), false);
            }
        }
        if (has(p)) return;
        ItemStack st = new ItemStack(Keys.SO.get());
        if (!p.getInventory().add(st)) {
            int slot = p.getInventory().getFreeSlot();
            if (slot < 0) return;
            p.getInventory().setItem(slot, st);
        }
    }

    public static boolean host(ServerPlayer p) {
        MinecraftServer s = p.server;
        return s.isSingleplayerOwner(p.getGameProfile()) || p.hasPermissions(2) || s.getPlayerList().getPlayerCount() == 1 && !s.isDedicatedServer();
    }

    public static int mode(MinecraftServer s) {
        return MindData.get(s).coop;
    }

    public static boolean together(MinecraftServer s) {
        return mode(s) == 1;
    }

    public static void login(ServerPlayer p) {
        ensure(p);
        Profile pr = MindData.profile(p);
        if (together(p.server)) Chapters.catchUp(p, pr);
        if (mode(p.server) < 0 && host(p)) ask(p);
    }

    static void ask(ServerPlayer p) {
        ASKED.put(p.getUUID(), (long) p.server.getTickCount());
        Net.send(p, new Net.Book(Net.Book.MODE, ""));
    }

    public static void tick(ServerPlayer p, Profile pr, long now) {
        if (now % 100 != (p.getId() % 100)) return;
        ensure(p);
        if (together(p.server)) Chapters.catchUp(p, pr);
        if (mode(p.server) < 0 && host(p) && now - ASKED.getOrDefault(p.getUUID(), -100000L) > 6000 && p.containerMenu == p.inventoryMenu) ask(p);
    }

    public static void ask(ServerPlayer p, int op, int arg) {
        switch (op) {
            case Net.Ask.OPEN -> open(p, arg);
            case Net.Ask.MODE -> {
                if (!host(p)) {
                    p.displayClientMessage(Component.literal("Chỉ chủ thế giới mới chọn được cách chơi.").withStyle(ChatFormatting.GRAY), true);
                    return;
                }
                setMode(p.server, arg == 1 ? 1 : 0, p);
            }
            default -> {}
        }
    }

    public static void setMode(MinecraftServer s, int m, ServerPlayer by) {
        MindData d = MindData.get(s);
        boolean changed = d.coop != m;
        d.coop = m;
        d.setDirty();
        if (!changed) return;
        String text = m == 1 ? "Cách chơi: CHƠI CHUNG. Chương mở khóa đồng bộ cho cả nhóm, trốn tìm chung một ván." : "Cách chơi: CHƠI RIÊNG. Mỗi người tự đi chương của mình.";
        for (ServerPlayer o : s.getPlayerList().getPlayers()) {
            o.displayClientMessage(Component.literal(text).withStyle(ChatFormatting.LIGHT_PURPLE), false);
            if (m == 1) Chapters.catchUp(o, MindData.profile(o));
        }
        CaveMind.LOG.info("[CaveMind] play mode set to {} by {}", m == 1 ? "together" : "single", by == null ? "server" : by.getGameProfile().getName());
    }

    public static void open(ServerPlayer p, int page) {
        if (!Net.modded(p)) {
            p.displayClientMessage(Component.literal("Cần cài Cave Mind phía máy bạn để mở sổ.").withStyle(ChatFormatting.GRAY), true);
            return;
        }
        Fx.sound(p, "minecraft:item.book.page_turn", SoundSource.PLAYERS, p.position(), 0.6f, 0.9f);
        Net.send(p, new Net.Book(Net.Book.OPEN, build(p, page).toString()));
    }

    static JsonObject sec(String h, String b) {
        JsonObject o = new JsonObject();
        o.addProperty("h", h);
        o.addProperty("b", b);
        return o;
    }

    static JsonObject step(String t, boolean done) {
        JsonObject o = new JsonObject();
        o.addProperty("t", t);
        o.addProperty("d", done);
        return o;
    }

    public static String band(double d) {
        if (d < 40) return "ngay quanh đây";
        if (d < 150) return "không xa lắm";
        if (d < 320) return "vài trăm bước";
        if (d < 600) return "khá xa, chuẩn bị đồ ăn và đuốc";
        return "rất xa, đi cả ngày đường";
    }

    static String where(BlockPos from, BlockPos to) {
        double d = Math.sqrt(Math.pow(to.getX() - from.getX(), 2) + Math.pow(to.getZ() - from.getZ(), 2));
        return "hướng " + Points.heading(from, to) + ", " + band(d);
    }

    public static JsonObject build(ServerPlayer p, int page) {
        Profile pr = MindData.profile(p);
        ServerLevel ow = p.server.overworld();
        JsonObject o = new JsonObject();
        boolean maze = Maze.inMaze(p);
        o.addProperty("page", page >= 0 ? page : maze ? 4 : -1);
        o.addProperty("mode", mode(p.server));
        o.addProperty("host", host(p));
        o.addProperty("chapter", pr.chapter);
        o.addProperty("title", Chapters.TITLE[Math.max(1, Math.min(6, pr.chapter))]);
        o.addProperty("name", p.getGameProfile().getName());
        JsonArray goals = new JsonArray();
        JsonArray steps = new JsonArray();
        int g = pr.chapter + 1;
        if (maze) {
            goals.add(sec("Bạn đang ở Giờ Thứ Bốn Mươi Tám", "Nhặt đủ bảy món đồ chơi, mang về rương ở phòng giếng rồi rung chuông. Hoặc trốn cho tới khi chuông tự vang. Xem trang Mê cung để biết cách sống sót."));
        }
        if (g <= 6) {
            String child = Points.CHILD[g];
            boolean place = Chapters.PLACE[g];
            steps.add(step("Nhận lời mời của " + child, Chapters.has(pr, g, Chapters.READY)));
            if (!Chapters.has(pr, g, Chapters.READY)) {
                goals.add(sec("Chờ lời mời của " + child, "Hoàn thành các nhiệm vụ chương " + pr.chapter + " trong sổ nhiệm vụ để " + child + " gửi lời mời mở chương " + g + ". Trong lúc chờ, hãy tìm trang nhật ký rách và các trạm khảo sát bỏ hoang."));
            } else if (place) {
                Points.Point pt = Points.get(ow, "g" + g), w = Points.get(ow, "w" + g);
                boolean way = Chapters.has(pr, g, Chapters.WAY) || Chapters.has(pr, g, Chapters.ARRIVED);
                steps.add(step("Tới cây đếm giữa đường", way));
                steps.add(step("Tới " + PLACE[g], Chapters.has(pr, g, Chapters.ARRIVED)));
                steps.add(step("Đọc cuốn sổ của " + child, Chapters.has(pr, g, Chapters.READ)));
                steps.add(step("Thắp hết nến ở điểm hẹn", Chapters.has(pr, g, Chapters.LIT)));
                steps.add(step("Nhặt món đồ chơi dưới cây đếm", Chapters.has(pr, g, Chapters.LIT) && Chapters.hasKey(p, g) || Chapters.has(pr, g, Chapters.WON)));
                steps.add(step("Thắng ván trốn tìm ban đêm", Chapters.has(pr, g, Chapters.WON)));
                if (!way && w != null) {
                    goals.add(sec("Chặng một: cây đếm giữa đường", "La bàn sương chỉ tới một cây chết cắm biển, " + where(p.blockPosition(), w.target) + ". Biển dưới gốc cây sẽ chỉ chặng tiếp theo. Mất la bàn thì chờ một lúc, nó sẽ quay lại túi bạn."));
                } else if (!Chapters.has(pr, g, Chapters.ARRIVED) && pt != null) {
                    goals.add(sec("Chặng hai: " + PLACE[g], "La bàn đã đổi hướng. Nơi hẹn nằm " + where(p.blockPosition(), pt.target) + ". Nó chỉ hiện ra khi bạn tới đủ gần."));
                } else if (!Chapters.has(pr, g, Chapters.READ)) {
                    goals.add(sec("Đọc cuốn sổ", "Cuốn sổ của " + child + " nằm trên bục đọc ở điểm hẹn. Đọc hết để biết nến giấu ở đâu."));
                } else if (!Chapters.has(pr, g, Chapters.LIT) && pt != null) {
                    goals.add(sec("Thắp nến", "Đã thắp " + Points.lit(ow, pt) + " trên " + pt.candles.size() + " ngọn nến. Bật lửa có trong thùng gỗ gần bục đọc."));
                } else if (!Chapters.has(pr, g, Chapters.WON)) {
                    goals.add(sec("Rủ nó chơi", "Khi trời tối, cầm món đồ chơi của " + child + " và chạm vào cây đếm. Trốn cho tới khi nó bỏ cuộc: ngồi thụp, đứng yên, ở chỗ tối, đừng nhìn nó."));
                }
            } else {
                int candles = Chapters.ALTAR_CANDLES[g];
                boolean built = Chapters.has(pr, g, Chapters.BUILT);
                steps.add(step("Chế tạo món đồ chơi của " + child, Chapters.hasKey(p, g) || Chapters.has(pr, g, Chapters.WON)));
                steps.add(step("Dựng điểm hẹn của riêng bạn", built));
                steps.add(step("Thắng ván trốn tìm", Chapters.has(pr, g, Chapters.WON)));
                goals.add(sec("Tự dựng điểm hẹn", "Chồng 3 khúc gỗ làm cây đếm, đặt quanh nó " + candles + " ngọn nến đang cháy, một cái chuông và một tấm biển ghi tên " + child + ". "
                    + (g == 3 ? "Chỉ chơi được ban đêm hoặc ở nơi không thấy trời." : "Phải dựng thật sâu, dưới độ cao 0.") + " Công thức món đồ chơi nằm trong cuốn sổ hướng dẫn của " + child + "."));
            }
        } else {
            goals.add(sec("Mọi chương đã mở", "Còn ba cái kết: đếm tới một trăm ở cây đếm, rung chuông trạm ba khi hắn ở gần, và đối mặt với Herobrine ở mỏ lúc nửa đêm."));
        }
        int found = Long.bitCount(pr.pages);
        String hint = Pages.hint(p, pr);
        goals.add(sec("Trang nhật ký rách: " + found + "/" + Pages.TOTAL, hint));
        if (Meter.on()) goals.add(sec("The Man", Meter.status(pr)));
        o.add("goals", goals);
        o.add("steps", steps);
        JsonArray chs = new JsonArray();
        for (int i = 1; i <= 6; i++) {
            JsonObject c = new JsonObject();
            c.addProperty("n", i);
            c.addProperty("t", Chapters.TITLE[i]);
            c.addProperty("c", i >= 2 ? Points.CHILD[i] : "");
            c.addProperty("open", pr.chapter >= i);
            chs.add(c);
        }
        o.add("chapters", chs);
        JsonArray notes = new JsonArray();
        for (int i = 0; i < Pages.TOTAL; i++) if ((pr.pages & 1L << i) != 0) notes.add(sec(Pages.TITLE[i], Pages.TEXT[i]));
        o.add("notes", notes);
        o.addProperty("found", found);
        JsonArray start = new JsonArray();
        start.add(true);
        start.add(pr.meterStage >= 1);
        start.add(pr.dGames > 0);
        start.add(pr.dWins > 0);
        start.add(pr.chapter >= 2 || Chapters.has(pr, 2, Chapters.READY));
        start.add(pr.lore.contains("p"));
        start.add(pr.mazeGuide);
        start.add(found > 0);
        start.add(pr.chapter >= 6);
        o.add("start", start);
        o.addProperty("moon", vn.cavemind.world.Moon.now(p.serverLevel()));
        return o;
    }

    private Book() {}
}
