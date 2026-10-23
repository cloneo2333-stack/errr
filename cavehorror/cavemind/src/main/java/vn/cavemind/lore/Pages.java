package vn.cavemind.lore;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CandleBlock;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import vn.cavemind.CaveMind;
import vn.cavemind.chapter.Gifts;
import vn.cavemind.chapter.Keys;
import vn.cavemind.compat.Compat;
import vn.cavemind.memory.MindData;
import vn.cavemind.memory.Profile;
import vn.cavemind.sense.Watch;
import vn.cavemind.world.Fx;

public final class Pages {
    public static final String IDX = "cavemind_page", OWNER = "cavemind_page_owner";
    public static final String[] TITLE = {
        "Trang 1: Giếng số bốn", "Trang 2: Tám đứa trẻ", "Trang 3: Luật của trò chơi", "Trang 4: Người đếm", "Trang 5: Sương mù tháng mười",
        "Trang 6: Người trong sương", "Trang 7: Tiếng chuông", "Trang 8: Bông hồng sống", "Trang 9: Thợ mỏ không có mắt", "Trang 10: Những cái hang",
        "Trang 11: Nhà của Linh", "Trang 12: Đồng hồ của Mai", "Trang 13: Viên bi nứt", "Trang 14: Con diều giấy", "Trang 15: Cái còi gỗ",
        "Trang 16: Trạm khảo sát", "Trang 17: Thói quen", "Trang 18: Đêm không ngủ", "Trang 19: Tên bị xóa", "Trang 20: Mẹ của Khoa",
        "Trang 21: Bốn mươi tám nhịp", "Trang 22: Người lớn", "Trang 23: Lối ra", "Trang 24: Đứa thứ tám"
    };
    public static final String[] TEXT = {
        "Người ta đóng giếng thông gió số bốn vào một buổi chiều có sương. Không ai đếm lại số trẻ con trước khi đóng nắp.",
        "Linh, Tuấn, Mai, Bảo, Hà, Nam, Khoa. Và một đứa nữa. Không ai nhớ tên đứa thứ tám, chỉ nhớ nó luôn là đứa phải đếm.",
        "Ai thua thì đếm. Đếm tới bốn mươi tám rồi đi tìm. Ai bị tìm thấy thì thành người đếm tiếp theo.",
        "Nó vẫn đếm. Nếu bạn nghe tiếng đếm dưới đất, đừng trả lời. Nếu đã trả lời, hãy trốn thật giỏi.",
        "Sương tới vào tháng mười và không bao giờ đi hẳn. Những người đi vào sương thường quay về muộn hơn một chút so với lúc họ nghĩ.",
        "Hắn cao, gầy, và rất kiên nhẫn. Hắn không vội. Hắn học bạn ngủ ở đâu, ăn lúc nào, đi đường nào về nhà.",
        "Chuông làm hắn khó chịu. Rung nhiều quá thì hắn quen. Đừng phí tiếng chuông vào những đêm không cần.",
        "Mẹ dặn: nếu nó tới thì cầm bông hồng sống. Nó sẽ nhớ ra là nó buồn ngủ.",
        "Có một thợ mỏ đào mãi xuống dưới. Người ta nói anh ta không còn mắt, chỉ còn trí nhớ về những gì bạn đã đào.",
        "Đừng đào thẳng xuống một mình. Những cái hang nhớ tiếng cuốc của bạn và sẽ trả lại nó vào lúc bạn không ngờ.",
        "Linh có một con quay gỗ. Con quay ấy vẫn quay mỗi khi có người đi ngang nhà kho bên giếng cạn.",
        "Mai thích tháp đồng hồ. Đồng hồ dừng đúng lúc nắp giếng đóng lại, và chưa ai lên vặn nó.",
        "Tuấn có ba viên bi, một viên nứt. Nó nói viên nứt nhìn thấy được những thứ ở trong sương.",
        "Bảo làm diều bằng giấy báo cũ. Diều bay rất cao, cao tới chỗ không còn ngày và đêm.",
        "Nam thổi còi gỗ để gọi cả bọn về. Lần cuối cùng nó thổi, chỉ có bảy đứa quay lại.",
        "Những người đo sương dựng trạm dọc đường. Họ ghi nhật ký rất đều, cho tới một ngày thì không ghi nữa.",
        "Nếu bạn ngủ ở cùng một chỗ đủ lâu, hắn sẽ biết. Nếu bạn đổi chỗ ngủ liên tục, hắn sẽ đi theo mùi.",
        "Có những đêm bạn sẽ không dám nhắm mắt. Đó là những đêm hắn đã quyết định đến.",
        "Trên hàng rào sân chơi có tám cái tên. Một cái bị cạo đi. Mỗi lần có người thua, một cái tên mới được viết lên.",
        "Mẹ của Khoa đi tìm con mỗi tối. Bà mang theo đèn. Bà không bao giờ đi vào chỗ tối, và vì thế không bao giờ tìm thấy.",
        "Bốn mươi tám nhịp là đủ để chạy qua ba hành lang. Không đủ để quay lại.",
        "Người lớn không chơi trốn tìm. Người lớn chỉ đóng nắp giếng lại và nói rằng không có gì ở dưới đó.",
        "Không có lối ra. Chỉ có cách làm cho nó chán. Nó chán khi tìm đủ bảy món đồ chơi, hoặc khi chuông tự vang.",
        "Đứa thứ tám không giận. Nó chỉ không muốn chơi một mình. Ai chơi với nó đủ lâu sẽ biết tên nó."
    };
    public static final int TOTAL = TITLE.length;

    public static final class PageItem extends Item {
        public PageItem() {
            super(new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON));
        }

        @Override
        public void appendHoverText(ItemStack st, Level lvl, List<Component> lines, TooltipFlag flag) {
            int i = st.hasTag() ? st.getTag().getInt(IDX) : 0;
            if (i >= 0 && i < TOTAL) lines.add(Component.literal(TITLE[i]).withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
            lines.add(Component.literal("Nhặt lên để chép vào Sổ Chương.").withStyle(ChatFormatting.DARK_GRAY));
        }
    }

    public static String hint(ServerPlayer p, Profile pr) {
        int found = Long.bitCount(pr.pages);
        if (found >= TOTAL) return "Bạn đã tìm đủ mọi trang. Đọc lại chúng ở trang Ghi chép.";
        if (pr.pageAt == null) return "Chưa nghe thấy gì. Trang tiếp theo sẽ xuất hiện khi bạn đi lang thang một lúc.";
        double d = Math.sqrt(Math.pow(pr.pageAt.getX() - p.getX(), 2) + Math.pow(pr.pageAt.getZ() - p.getZ(), 2));
        String dir = Points.heading(p.blockPosition(), pr.pageAt);
        String far = d < 20 ? "ngay gần đây" : d < 60 ? "không xa, khoảng " + (int) (Math.round(d / 10) * 10) + " bước" : "khá xa, chừng " + (int) (Math.round(d / 20) * 20) + " bước";
        return "Một cột khói mảnh bốc lên ở hướng " + dir + ", " + far + ". Trang nằm dưới chân cột khói, cạnh ngọn nến trên đá rêu.";
    }

    static int next(Profile pr) {
        for (int i = 0; i < TOTAL; i++) if ((pr.pages & 1L << i) == 0) return i;
        return -1;
    }

    public static void tick(ServerPlayer p, Watch w, Profile pr, long now) {
        if (now % 20 != (p.getId() & 15) || p.level().dimension() != Level.OVERWORLD || p.isSpectator()) return;
        ServerLevel lvl = p.serverLevel();
        if (pr.pageAt != null) {
            double d2 = pr.pageAt.distToCenterSqr(p.position());
            if (d2 < 140 * 140 && lvl.isLoaded(pr.pageAt)) vn.cavemind.gear.Signal.smoke(p, pr.pageAt, false);
            if (d2 < 48 * 48 && lvl.isLoaded(pr.pageAt) && now % 200 < 20) {
                boolean there = !lvl.getEntitiesOfClass(ItemEntity.class, new AABB(pr.pageAt).inflate(3), ie -> ie.getItem().is(Keys.PAGE.get())).isEmpty();
                if (!there) place(lvl, pr, p, pr.pageAt, pr.pageIdx);
            }
            if (d2 > 600 * 600) {
                pr.pageAt = null;
                pr.pageNext = pr.playTicks + 1200;
            }
            return;
        }
        if (Compat.exempt(p) || vn.cavemind.maze.Maze.inMaze(p)) return;
        int idx = next(pr);
        if (idx < 0) return;
        if (pr.pageNext < 0) pr.pageNext = pr.playTicks + 2400;
        if (pr.playTicks < pr.pageNext) return;
        BlockPos at = choose(lvl, p, p.getRandom());
        if (at == null) {
            pr.pageNext = pr.playTicks + 400;
            return;
        }
        pr.pageAt = at;
        pr.pageIdx = idx;
        place(lvl, pr, p, at, idx);
        MindData.dirty(p.server);
        p.displayClientMessage(Component.literal("Một cột khói mảnh vừa bốc lên ở hướng " + Points.heading(p.blockPosition(), at) + ". Có một trang nhật ký dưới đó.").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC), true);
        CaveMind.debug("page {} for {} at {}", idx, p.getGameProfile().getName(), at);
    }

    static BlockPos choose(ServerLevel lvl, ServerPlayer p, RandomSource r) {
        for (int t = 0; t < 24; t++) {
            double a = r.nextDouble() * Math.PI * 2, d = 55 + r.nextDouble() * 65;
            int x = (int) Math.floor(p.getX() + Math.cos(a) * d), z = (int) Math.floor(p.getZ() + Math.sin(a) * d);
            BlockPos col = new BlockPos(x, 0, z);
            if (!lvl.isLoaded(col)) continue;
            int y = lvl.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
            BlockPos top = new BlockPos(x, y, z);
            var below = lvl.getBlockState(top.below());
            if (!below.isSolidRender(lvl, top.below()) || !lvl.getFluidState(top.below()).isEmpty() || !lvl.getBlockState(top).isAir()) continue;
            if (y < lvl.getSeaLevel() - 2) continue;
            return top;
        }
        return null;
    }

    static void place(ServerLevel lvl, Profile pr, ServerPlayer p, BlockPos at, int idx) {
        if (!lvl.isLoaded(at)) return;
        if (lvl.getBlockState(at.below()).isSolidRender(lvl, at.below())) lvl.setBlock(at.below(), Blocks.MOSSY_COBBLESTONE.defaultBlockState(), 3);
        BlockPos stone = at;
        if (lvl.getBlockState(stone).isAir()) lvl.setBlock(stone, Blocks.MOSSY_COBBLESTONE_SLAB.defaultBlockState(), 3);
        BlockPos c = stone.east();
        if (lvl.getBlockState(c).isAir() && lvl.getBlockState(c.below()).isSolidRender(lvl, c.below())) {
            lvl.setBlock(c, Blocks.CANDLE.defaultBlockState().setValue(CandleBlock.CANDLES, 2).setValue(CandleBlock.LIT, false), 3);
        }
        ItemStack st = new ItemStack(Keys.PAGE.get());
        CompoundTag t = st.getOrCreateTag();
        t.putInt(IDX, idx);
        t.putUUID(OWNER, p.getUUID());
        ItemEntity ie = new ItemEntity(lvl, at.getX() + 0.5, at.getY() + 0.6, at.getZ() + 0.5, st, 0, 0, 0);
        ie.setUnlimitedLifetime();
        ie.setPickUpDelay(0);
        lvl.addFreshEntity(ie);
    }

    static void chime(ServerPlayer p, BlockPos at) {
        Vec3 c = Vec3.atCenterOf(at);
        float[] notes = {0.7f, 0.94f, 0.84f, 1.12f};
        for (int i = 0; i < notes.length; i++) {
            final float pitch = notes[i];
            Fx.later(p.serverLevel(), i * 6, () -> Fx.sound(p, "minecraft:block.note_block.chime", SoundSource.AMBIENT, c, 0.35f, pitch));
        }
    }

    public static boolean pickup(ServerPlayer p, ItemEntity it) {
        ItemStack st = it.getItem();
        if (!st.is(Keys.PAGE.get())) return false;
        int idx = st.hasTag() ? st.getTag().getInt(IDX) : 0;
        if (idx < 0 || idx >= TOTAL) idx = 0;
        Profile pr = MindData.profile(p);
        it.discard();
        BlockPos was = it.blockPosition();
        if (st.hasTag() && st.getTag().hasUUID(OWNER) && !st.getTag().getUUID(OWNER).equals(p.getUUID())) {
            Profile owner = MindData.profile(st.getTag().getUUID(OWNER));
            if (owner != null && owner.pageAt != null && owner.pageAt.distSqr(was) < 16) {
                owner.pageAt = null;
                owner.pageNext = owner.playTicks + 1200;
            }
        }
        if (pr.pageAt != null && pr.pageAt.distSqr(was) < 16) {
            pr.pageAt = null;
            pr.pageNext = pr.playTicks + 3600 + p.getRandom().nextInt(4800);
        }
        if ((pr.pages & 1L << idx) != 0) {
            int other = next(pr);
            if (other < 0) {
                p.displayClientMessage(Component.literal("Bạn đã có trang này rồi.").withStyle(ChatFormatting.GRAY), true);
                return true;
            }
            idx = other;
        }
        pr.pages |= 1L << idx;
        int found = Long.bitCount(pr.pages);
        MindData.dirty(p.server);
        p.displayClientMessage(Component.literal(TITLE[idx]).withStyle(ChatFormatting.GOLD), false);
        p.displayClientMessage(Component.literal(TEXT[idx]).withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC), false);
        p.displayClientMessage(Component.literal("Đã chép vào Sổ Chương (" + found + "/" + TOTAL + "). Mở bằng phím [J].").withStyle(ChatFormatting.DARK_GRAY), false);
        Fx.sound(p, "minecraft:item.book.page_turn", SoundSource.PLAYERS, p.position(), 0.8f, 0.8f);
        give(p, new ItemStack(Items.CANDLE, 2));
        give(p, new ItemStack(Keys.PIN.get(), 1));
        if (found % 4 == 0) Gifts.give(p, Gifts.small(p, pr.chapter));
        if (found == 12 || found == TOTAL) {
            Gifts.give(p, Gifts.rare(p, pr.chapter, "Khoa"));
            p.displayClientMessage(Component.literal("Giữa các trang kẹp một món quà hiếm.").withStyle(ChatFormatting.GOLD), false);
        }
        Compat.grant(p, "cavemind:chuong/trang_" + Math.min(found, 24));
        return true;
    }

    static void give(ServerPlayer p, ItemStack st) {
        if (!p.getInventory().add(st)) p.drop(st, false);
    }

    private Pages() {}
}
