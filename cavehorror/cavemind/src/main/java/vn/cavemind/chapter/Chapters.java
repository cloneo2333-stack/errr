package vn.cavemind.chapter;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BellBlock;
import net.minecraft.world.level.block.CandleBlock;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import vn.cavemind.CaveMind;
import vn.cavemind.Cfg;
import vn.cavemind.compat.Compat;
import vn.cavemind.damned.Game;
import vn.cavemind.director.Director;
import vn.cavemind.director.Dread;
import vn.cavemind.director.PaceLog;
import vn.cavemind.lore.Points;
import vn.cavemind.memory.MindData;
import vn.cavemind.memory.Profile;
import vn.cavemind.sense.Watch;
import vn.cavemind.world.Fx;

public final class Chapters {
    public static final int READY = 1, ARRIVED = 2, READ = 4, LIT = 8, KEY = 16, BUILT = 32, WON = 64, WAY = 128;
    public static final boolean[] PLACE = {false, false, true, false, true, false, true};
    static final int[] ALTAR_CANDLES = {0, 0, 0, 6, 0, 8, 0};
    public static final String[] TITLE = {"", "Những bộ rễ dõi theo", "Tiếng gõ bên dưới", "Nơi bầu trời vỡ ra", "Sợi chỉ của thời gian vỡ nát", "Hỗn mang bung xích", "Tận cùng của vạn vật"};
    static final String[] FRIENDS = {"Linh", "Tuấn", "Mai", "Bảo", "Hà", "Nam", "Khoa"};

    private static final Map<UUID, Long> DROP = new HashMap<>();
    private static final Map<UUID, Long> COMPASS = new HashMap<>();
    private static final Map<UUID, Long> BOOK = new HashMap<>();
    private static final Map<UUID, Long> SAID = new HashMap<>();
    private static final Map<UUID, Integer> LITSEEN = new HashMap<>();

    public static boolean on() {
        return Cfg.CHAPTERS.get();
    }

    public static void clear() {
        DROP.clear();
        COMPASS.clear();
        BOOK.clear();
        SAID.clear();
        LITSEEN.clear();
    }

    public static void forget(ServerPlayer p) {
        DROP.remove(p.getUUID());
        SAID.remove(p.getUUID());
        LITSEEN.remove(p.getUUID());
    }

    public static boolean has(Profile pr, int g, int f) {
        return g >= 2 && g <= 6 && (pr.gate[g] & f) != 0;
    }

    static void flag(ServerPlayer p, Profile pr, int g, int f) {
        pr.gate[g] |= f;
        MindData.dirty(p.server);
    }

    static void grant(ServerPlayer p, String id) {
        Compat.grant(p, "cavemind:chuong/" + id);
    }

    static void say(ServerPlayer p, String s, ChatFormatting c) {
        p.displayClientMessage(Component.literal(s).withStyle(c, ChatFormatting.ITALIC), true);
    }

    static void tell(ServerPlayer p, String s, ChatFormatting c) {
        p.displayClientMessage(Component.literal(s).withStyle(c), false);
    }

    static boolean throttle(ServerPlayer p, long now, int gap) {
        if (now - SAID.getOrDefault(p.getUUID(), -10000L) < gap) return false;
        SAID.put(p.getUUID(), now);
        return true;
    }

    static BlockPos home(ServerPlayer p, Profile pr) {
        if (p.getRespawnPosition() != null && p.getRespawnDimension() == Level.OVERWORLD) return p.getRespawnPosition();
        if (pr.home != null) return pr.home;
        return p.server.overworld().getSharedSpawnPos();
    }

    public static int invite(ServerPlayer p, int g) {
        if (!on() || g < 2 || g > 6) return 0;
        Profile pr = MindData.profile(p);
        if (pr.chapter >= g) {
            grant(p, "c" + g + "_moi");
            tell(p, "Chương " + g + " đã mở với bạn từ trước.", ChatFormatting.GRAY);
            return 1;
        }
        if (pr.chapter < g - 1) {
            tell(p, "Cần mở chương " + (g - 1) + " trước.", ChatFormatting.DARK_GRAY);
            return 0;
        }
        flag(p, pr, g, READY);
        grant(p, "c" + g + "_moi");
        ServerLevel lvl = p.server.overworld();
        String child = Points.CHILD[g];
        if (PLACE[g]) {
            Points.Point pt = Points.assign(lvl, g, home(p, pr));
            give(p, compassFor(lvl, pr, g, pt));
            COMPASS.put(p.getUUID(), (long) p.server.getTickCount());
            tell(p, "Lời mời của " + child + ": một nơi hẹn ở rất xa. La bàn sương chỉ tới cây đếm giữa đường trước, biển dưới gốc cây sẽ chỉ chặng sau.", ChatFormatting.LIGHT_PURPLE);
            tell(p, "Mở Sổ Chương [J] để xem chặng đường và những gì cần làm.", ChatFormatting.GRAY);
        } else {
            give(p, Points.journal(g));
            tell(p, "Lời mời của " + child + ": lần này bạn phải tự dựng điểm hẹn. Cách làm ở trong cuốn sổ.", ChatFormatting.LIGHT_PURPLE);
        }
        Fx.sound(p, "minecraft:block.bell.resonate", SoundSource.AMBIENT, p.position(), 0.35f, 0.7f);
        PaceLog.encounter(p, "chapter", "invite", "gate " + g);
        CaveMind.debug("{} received the chapter {} invitation", p.getGameProfile().getName(), g);
        return 1;
    }

    static void give(ServerPlayer p, ItemStack st) {
        if (!p.getInventory().add(st)) p.drop(st, false);
    }

    public static void tick(ServerPlayer p, Watch w, Profile pr, long now) {
        if (!on() || now % 20 != (p.getId() & 15) || p.isSpectator() || p.level().dimension() != Level.OVERWORLD) return;
        ServerLevel lvl = p.serverLevel();
        Points.tick(lvl, p);
        for (int g = 2; g <= 6; g++) {
            if (!PLACE[g] && has(pr, g, READY) && !has(pr, g, WON) && pr.chapter == g - 1 && !hasJournal(p, g) && now - BOOK.getOrDefault(p.getUUID(), -20000L) > 6000) {
                BOOK.put(p.getUUID(), now);
                give(p, Points.journal(g));
                say(p, "Cuốn sổ cách dựng điểm hẹn lại nằm trong túi bạn.", ChatFormatting.GRAY);
            }
            if (!has(pr, g, READY) || has(pr, g, WON) || !PLACE[g]) continue;
            Points.Point pt = Points.get(lvl, "g" + g);
            if (pt == null) continue;
            Points.Point way = Points.get(lvl, "w" + g);
            if (!has(pr, g, WAY) && !has(pr, g, ARRIVED) && way != null) {
                double wx = p.getX() - way.target.getX() - 0.5, wz = p.getZ() - way.target.getZ() - 0.5;
                if (wx * wx + wz * wz < 14 * 14) {
                    flag(p, pr, g, WAY);
                    retarget(p, g, pt.target);
                    tell(p, "Cây đếm giữa đường. Kim la bàn sương run lên rồi quay sang hướng " + Points.heading(way.target, pt.target) + ". Chặng sau còn " + Book.band(Math.sqrt(way.target.distSqr(pt.target))) + ".", ChatFormatting.LIGHT_PURPLE);
                    Fx.sound(p, "minecraft:block.amethyst_block.chime", SoundSource.PLAYERS, p.position(), 0.6f, 0.6f);
                }
            }
            if (!has(pr, g, ARRIVED) && !hasCompass(p, g) && now - COMPASS.getOrDefault(p.getUUID(), -20000L) > 12000) {
                give(p, compassFor(lvl, pr, g, pt));
                COMPASS.put(p.getUUID(), now);
                say(p, "La bàn sương lại nằm trong túi bạn.", ChatFormatting.GRAY);
            }
            if (!pt.built || pt.pos == null) continue;
            double dx = p.getX() - pt.pos.getX() - 0.5, dz = p.getZ() - pt.pos.getZ() - 0.5, dy = p.getY() - pt.pos.getY();
            double d2 = dx * dx + dz * dz;
            if (d2 > 40 * 40) continue;
            if (!has(pr, g, ARRIVED) && d2 < 15 * 15 && Math.abs(dy) < 16) {
                flag(p, pr, g, ARRIVED);
                grant(p, "c" + g + "_den");
                tell(p, "Bạn đã tới điểm hẹn của " + Points.CHILD[g] + ". Tìm cuốn sổ của " + Points.CHILD[g] + " và đọc nó.", ChatFormatting.LIGHT_PURPLE);
                Fx.sound(p, "minecraft:ambient.cave", SoundSource.AMBIENT, Vec3.atCenterOf(pt.pos), 0.5f, 0.7f);
            }
            int lit = Points.lit(lvl, pt), all = pt.candles.size();
            if (g == 4 && has(pr, g, ARRIVED) && !has(pr, g, LIT) && d2 < 24 * 24) Points.angel(lvl, p, pt);
            if (has(pr, g, ARRIVED) && has(pr, g, READ) && !has(pr, g, LIT)) {
                Integer last = LITSEEN.get(p.getUUID());
                if (last == null || last != g * 100 + lit) {
                    LITSEEN.put(p.getUUID(), g * 100 + lit);
                    if (last != null && lit > 0 && lit < all) say(p, "Nến đã thắp: " + lit + "/" + all, ChatFormatting.GOLD);
                }
                if (all > 0 && lit >= all) {
                    flag(p, pr, g, LIT);
                    grant(p, "c" + g + "_nen");
                    tell(p, "Ngọn nến cuối cùng bừng lên. Có thứ gì đó rơi xuống từ hốc cây đếm.", ChatFormatting.LIGHT_PURPLE);
                    drop(lvl, p, pt, g, now);
                }
            } else if (has(pr, g, LIT) && !hasKey(p, g) && d2 < 18 * 18 && now >= DROP.getOrDefault(p.getUUID(), 0L)) {
                drop(lvl, p, pt, g, now);
                say(p, "Món đồ chơi lại nằm dưới gốc cây đếm.", ChatFormatting.GRAY);
            }
        }
    }

    static void drop(ServerLevel lvl, ServerPlayer p, Points.Point pt, int g, long now) {
        if (pt.post == null) return;
        Direction f = pt.face >= 0 ? Direction.from2DDataValue(pt.face) : Direction.NORTH;
        Vec3 at = Vec3.atBottomCenterOf(pt.post.relative(f)).add(0, 2.2, 0);
        ItemEntity ie = new ItemEntity(lvl, at.x, at.y, at.z, new ItemStack(Keys.of(g)));
        ie.setDeltaMovement(0, 0, 0);
        ie.setTarget(p.getUUID());
        ie.setPickUpDelay(30);
        ie.setExtendedLifetime();
        lvl.addFreshEntity(ie);
        Profile pr = MindData.profile(p);
        flag(p, pr, g, KEY);
        DROP.put(p.getUUID(), now + 1200);
        Fx.sound(p, "minecraft:block.wood.fall", SoundSource.BLOCKS, at, 0.6f, 0.8f);
    }

    static boolean hasKey(ServerPlayer p, int g) {
        for (ItemStack s : p.getInventory().items) if (Keys.gateOf(s) == g) return true;
        for (ItemStack s : p.getInventory().offhand) if (Keys.gateOf(s) == g) return true;
        return false;
    }

    static boolean hasJournal(ServerPlayer p, int g) {
        for (List<ItemStack> l : List.of(p.getInventory().items, p.getInventory().offhand)) {
            for (ItemStack s : l) if (s.is(Items.WRITTEN_BOOK) && s.hasTag() && s.getTag().getInt("cavemind_gate") == g) return true;
        }
        return false;
    }

    static boolean hasCompass(ServerPlayer p, int g) {
        for (ItemStack s : p.getInventory().items) if (s.is(Items.COMPASS) && s.hasTag() && s.getTag().getInt("cavemind_gate") == g) return true;
        for (ItemStack s : p.getInventory().offhand) if (s.is(Items.COMPASS) && s.hasTag() && s.getTag().getInt("cavemind_gate") == g) return true;
        return false;
    }

    static boolean consume(ServerPlayer p, int g) {
        for (List<ItemStack> l : List.of(p.getInventory().items, p.getInventory().offhand)) {
            for (ItemStack s : l) {
                if (Keys.gateOf(s) == g) {
                    s.shrink(1);
                    return true;
                }
            }
        }
        return false;
    }

    static boolean night(ServerLevel lvl) {
        int t = Dread.time(lvl);
        return t >= 12600 && t <= 23300;
    }

    public static boolean use(ServerPlayer p, InteractionHand hand, BlockPos pos, BlockState st) {
        if (!on() || hand != InteractionHand.MAIN_HAND || p.level().dimension() != Level.OVERWORLD) return false;
        ServerLevel lvl = p.serverLevel();
        long now = p.server.getTickCount();
        Profile pr = MindData.profile(p);
        Points.Point lp = Points.lecternAt(lvl, pos);
        if (lp != null && lp.gatePoint()) {
            int g = lp.gate;
            if (lvl.getBlockEntity(pos) instanceof net.minecraft.world.level.block.entity.LecternBlockEntity le && !le.hasBook()) {
                Points.refill(lvl, pos, le, g);
                Fx.sound(p, "minecraft:item.book.put", SoundSource.BLOCKS, Vec3.atCenterOf(pos), 0.8f, 0.8f);
                say(p, "Cuốn sổ của " + Points.CHILD[g] + " lại nằm trên bục, như chưa từng bị lấy đi.", ChatFormatting.GRAY);
                return true;
            }
            if (has(pr, g, READY) && !has(pr, g, READ)) {
                flag(p, pr, g, READ);
                grant(p, "c" + g + "_doc");
                tell(p, "Cuốn sổ nói phải thắp " + lp.candles.size() + " ngọn nến ở điểm hẹn. Cầm bật lửa và đi tìm chúng.", ChatFormatting.LIGHT_PURPLE);
            }
            return false;
        }
        Points.Point pt = Points.postAt(lvl, pos);
        BlockPos tree = vn.cavemind.lore.Lore.part(lvl, "p", "tree");
        boolean playground = tree != null && tree.getX() == pos.getX() && tree.getZ() == pos.getZ() && pos.getY() >= tree.getY() && pos.getY() <= tree.getY() + 6;
        if (pt == null && !playground && !st.is(BlockTags.LOGS)) return false;
        ItemStack main = p.getMainHandItem(), off = p.getOffhandItem();
        int kg = Keys.gateOf(main) > 0 ? Keys.gateOf(main) : Keys.gateOf(off);
        boolean candle = main.is(ItemTags.CANDLES) || off.is(ItemTags.CANDLES);
        if (kg > 0) return gateGame(p, pr, lvl, pos, pt, kg, now);
        BlockPos own = pt == null && !playground && candle ? selfTree(lvl, pos) : null;
        if (candle && (pt != null || playground || own != null)) return freeGame(p, pr, lvl, pt != null ? pt.post : playground ? tree : own, main.is(ItemTags.CANDLES) ? main : off, now);
        if ((pt != null || playground) && throttle(p, now, 200)) say(p, "Cầm một cây nến, hoặc món đồ chơi của chương, rồi chạm vào cây đếm để rủ nó chơi.", ChatFormatting.GRAY);
        return false;
    }

    static BlockPos base(ServerLevel lvl, BlockPos pos) {
        BlockPos b = pos;
        while (b.getY() > lvl.getMinBuildHeight() && lvl.getBlockState(b.below()).is(BlockTags.LOGS)) b = b.below();
        return b;
    }

    static BlockPos selfTree(ServerLevel lvl, BlockPos pos) {
        BlockPos b = base(lvl, pos);
        int h = 0;
        while (h < 6 && lvl.getBlockState(b.above(h)).is(BlockTags.LOGS)) h++;
        if (h < 3) return null;
        for (BlockPos q : BlockPos.betweenClosed(b.offset(-5, -2, -5), b.offset(5, 4, 5))) {
            if (lvl.getBlockEntity(q) instanceof SignBlockEntity sb && names(sb, "Cây đếm")) return b.immutable();
        }
        return null;
    }

    static boolean gateGame(ServerPlayer p, Profile pr, ServerLevel lvl, BlockPos pos, Points.Point pt, int g, long now) {
        String child = Points.CHILD[g];
        if (has(pr, g, WON) || pr.chapter >= g) {
            say(p, "Ván của " + child + " bạn đã thắng rồi.", ChatFormatting.GRAY);
            return true;
        }
        if (!has(pr, g, READY)) {
            say(p, "Chưa tới lúc. Hoàn thành chương " + (g - 1) + " và nhận lời mời của " + child + " trong sổ nhiệm vụ.", ChatFormatting.DARK_GRAY);
            return true;
        }
        BlockPos post;
        if (PLACE[g]) {
            if (pt == null || !pt.gatePoint() || pt.gate != g) {
                say(p, "Món đồ chơi này thuộc về cây đếm ở điểm hẹn của " + child + ". La bàn sương chỉ đường.", ChatFormatting.DARK_GRAY);
                return true;
            }
            if (!has(pr, g, LIT)) {
                say(p, has(pr, g, READ) ? "Còn nến chưa thắp ở điểm hẹn." : "Đọc cuốn sổ của " + child + " trước đã.", ChatFormatting.DARK_GRAY);
                return true;
            }
            if (!night(lvl)) {
                say(p, "Nó chỉ ra chơi khi trời tối.", ChatFormatting.DARK_GRAY);
                return true;
            }
            post = pt.post;
        } else {
            BlockPos base = base(lvl, pos);
            List<String> miss = altar(lvl, base, g);
            if (!miss.isEmpty()) {
                tell(p, "Điểm hẹn của " + child + " chưa đủ: " + String.join("; ", miss) + ".", ChatFormatting.GOLD);
                return true;
            }
            post = base;
        }
        if (now < pr.gateRetry) {
            say(p, "Nó vừa chơi xong. Đợi một lát rồi rủ lại.", ChatFormatting.DARK_GRAY);
            return true;
        }
        String busy = busy(p);
        if (busy != null) {
            say(p, busy, ChatFormatting.DARK_GRAY);
            return true;
        }
        if (!Game.startActive(p, post, g)) {
            say(p, "Không có chỗ cho nó đứng cạnh cây đếm. Dọn bớt xung quanh gốc cây.", ChatFormatting.DARK_GRAY);
            return true;
        }
        if (!PLACE[g] && !has(pr, g, BUILT)) {
            flag(p, pr, g, BUILT);
            grant(p, "c" + g + "_dung");
            Points.altar(lvl, g, p.getUUID(), post);
        }
        pr.activeGames++;
        tell(p, "Bạn đưa món đồ chơi của " + child + " ra. Nó úp mặt vào cây và bắt đầu đếm. Đi trốn đi: ngồi thụp xuống ở chỗ tối, đừng nhìn nó, đừng bỏ đi.",
            ChatFormatting.LIGHT_PURPLE);
        return true;
    }

    static boolean freeGame(ServerPlayer p, Profile pr, ServerLevel lvl, BlockPos post, ItemStack candle, long now) {
        if (post == null) return false;
        long day = Dread.day(lvl);
        if (pr.freeDay == day) {
            say(p, "Hôm nay nó đã chơi với bạn rồi. Ngày mai quay lại.", ChatFormatting.DARK_GRAY);
            return true;
        }
        if (!night(lvl) && lvl.canSeeSky(post.above(3))) {
            say(p, "Nó chỉ ra chơi khi trời tối.", ChatFormatting.DARK_GRAY);
            return true;
        }
        String busy = busy(p);
        if (busy != null) {
            say(p, busy, ChatFormatting.DARK_GRAY);
            return true;
        }
        if (!Game.startActive(p, post, 0)) {
            say(p, "Không có chỗ cho nó đứng cạnh cây đếm.", ChatFormatting.DARK_GRAY);
            return true;
        }
        pr.freeDay = day;
        pr.activeGames++;
        if (!p.isCreative()) candle.shrink(1);
        tell(p, "Bạn đặt cây nến xuống gốc cây. Nó úp mặt vào cây và bắt đầu đếm. Thắng thì nó sẽ để lại quà.", ChatFormatting.LIGHT_PURPLE);
        return true;
    }

    static String busy(ServerPlayer p) {
        if (!Game.on()) return "The Damned đang tắt trong cài đặt.";
        if (Game.active(p)) return "Bạn đang trong một ván rồi.";
        if (vn.cavemind.damned.Haunt.busy(p)) return "Nó đang ở chỗ khác, quanh bạn. Đợi nó đi đã.";
        if (vn.cavemind.maze.Maze.inMaze(p) || vn.cavemind.damned.Hundred.active(p)) return "Không phải lúc này.";
        if (Compat.exempt(p)) return "Nó không chơi với người ở chế độ sáng tạo hay khán giả.";
        Director.State s = Director.state(p);
        if (s.act != null && !s.act.equals("damned")) return "Có thứ khác đang ở rất gần bạn. Nó không dám ra.";
        return null;
    }

    static String plain(String s) {
        String n = Normalizer.normalize(s.toLowerCase(Locale.ROOT).replace('đ', 'd'), Normalizer.Form.NFD);
        return n.replaceAll("\\p{M}", "").replaceAll("[^a-z]", "");
    }

    static boolean names(SignBlockEntity sb, String name) {
        String want = plain(name);
        StringBuilder all = new StringBuilder();
        for (int i = 0; i < 4; i++) {
            all.append(sb.getFrontText().getMessage(i, false).getString()).append(' ');
            all.append(sb.getBackText().getMessage(i, false).getString()).append(' ');
        }
        return plain(all.toString()).contains(want);
    }

    public static List<String> altar(ServerLevel lvl, BlockPos base, int g) {
        List<String> miss = new ArrayList<>();
        int h = 0;
        while (h < 6 && lvl.getBlockState(base.above(h)).is(BlockTags.LOGS)) h++;
        if (h < 3) miss.add("cây đếm phải có ba khúc gỗ chồng lên nhau (mới có " + h + ")");
        int lit = 0;
        boolean bell = false, sign = false;
        for (BlockPos q : BlockPos.betweenClosed(base.offset(-8, -3, -8), base.offset(8, 6, 8))) {
            int dx = Math.abs(q.getX() - base.getX()), dz = Math.abs(q.getZ() - base.getZ()), dy = q.getY() - base.getY();
            BlockState s = lvl.getBlockState(q);
            if (s.getBlock() instanceof CandleBlock) {
                if (s.getValue(CandleBlock.LIT) && dx <= 4 && dz <= 4 && dy >= -1 && dy <= 3) lit++;
            } else if (s.getBlock() instanceof BellBlock) {
                bell = true;
            } else if (!sign && dx <= 5 && dz <= 5 && s.hasBlockEntity() && lvl.getBlockEntity(q) instanceof SignBlockEntity sb && names(sb, Points.CHILD[g])) {
                sign = true;
            }
        }
        int need = ALTAR_CANDLES[g];
        if (lit < need) miss.add("cần " + need + " ngọn nến đang cháy quanh gốc trong 4 khối (mới có " + lit + ")");
        if (!bell) miss.add("cần một cái chuông trong 8 khối");
        if (!sign) miss.add("cần một tấm biển ghi tên " + Points.CHILD[g] + " trong 5 khối");
        if (g == 3 && !night(lvl) && lvl.canSeeSky(base.above(Math.max(h, 1)))) miss.add("chỉ chơi khi trời tối hoặc ở nơi không thấy trời");
        if (g == 5 && base.getY() >= 0) miss.add("phải dựng thật sâu, dưới độ cao 0 (đang ở " + base.getY() + ")");
        return miss;
    }

    public static void ended(ServerPlayer p, int g, boolean victory, BlockPos post, String why) {
        if (!on()) return;
        Profile pr = MindData.profile(p);
        long now = p.server.getTickCount();
        if (!victory) {
            String how = why != null && why.contains("chuông") ? "Bạn rung chuông, ván chơi dừng lại." : why != null && why.contains("tìm thấy") ? "Nó tìm thấy bạn." : why != null && why.contains("chạm") ? "Nó bắt được bạn." : "Ván chơi kết thúc, bạn chưa thắng.";
            if (g > 0) {
                pr.gateRetry = now + 1200;
                tell(p, how + " Món đồ chơi vẫn ở chỗ bạn: nghỉ một lát rồi rủ lại.", ChatFormatting.DARK_GRAY);
            } else {
                tell(p, how + " Ngày mai lại chơi.", ChatFormatting.DARK_GRAY);
            }
            MindData.dirty(p.server);
            return;
        }
        pr.activeWins++;
        grant(p, "tro_tim");
        if (g == 0) {
            boolean rare = pr.gifts == 0 || p.getRandom().nextDouble() < Cfg.RARE_CHANCE.get();
            String child = FRIENDS[p.getRandom().nextInt(FRIENDS.length)];
            if (rare) {
                Gifts.give(p, Gifts.rare(p, pr.chapter, child));
                pr.gifts++;
                grant(p, "qua_hiem");
                tell(p, "Nó để lại một món quà hiếm dưới gốc cây.", ChatFormatting.GOLD);
            } else {
                Gifts.give(p, Gifts.small(p, pr.chapter));
            }
            MindData.dirty(p.server);
            return;
        }
        unlock(p, pr, g, true);
        ServerLevel lvl = p.serverLevel();
        for (ServerPlayer o : lvl.players()) {
            if (o == p || o.distanceTo(p) > 40 || Compat.exempt(o)) continue;
            Profile po = MindData.profile(o);
            if (!has(po, g, READY) || has(po, g, WON) || PLACE[g] && !has(po, g, LIT) || !hasKey(o, g)) continue;
            tell(o, "Bạn trốn cùng " + p.getGameProfile().getName() + " và nó không tìm ra ai cả.", ChatFormatting.LIGHT_PURPLE);
            unlock(o, po, g, false);
        }
    }

    static void unlock(ServerPlayer p, Profile pr, int g, boolean host) {
        String child = Points.CHILD[g];
        if (!consume(p, g)) {
            tell(p, "Món đồ chơi của " + child + " không còn trong túi bạn. Nó không chịu mở cửa.", ChatFormatting.DARK_RED);
            return;
        }
        flag(p, pr, g, WON);
        pr.chapter = Math.max(pr.chapter, g);
        grant(p, "c" + g + "_mo");
        grant(p, "qua_hiem");
        Gifts.give(p, Gifts.rare(p, g, child));
        pr.gifts++;
        MindData.dirty(p.server);
        p.connection.send(new ClientboundSetTitlesAnimationPacket(10, 80, 30));
        p.connection.send(new ClientboundSetTitleTextPacket(Component.literal("Chương " + g).withStyle(ChatFormatting.LIGHT_PURPLE)));
        p.connection.send(new ClientboundSetSubtitleTextPacket(Component.literal(TITLE[g]).withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC)));
        tell(p, child + " được về nhà. Chương " + g + ": " + TITLE[g] + " đã mở.", ChatFormatting.LIGHT_PURPLE);
        List<EntityType<?>> news = Tiers.of(g);
        if (!news.isEmpty()) {
            MutableComponent line = Component.literal("Bóng tối dày thêm. Từ giờ quanh bạn sẽ có: ").withStyle(ChatFormatting.DARK_RED);
            int n = Math.min(8, news.size());
            for (int i = 0; i < n; i++) {
                if (i > 0) line.append(Component.literal(", ").withStyle(ChatFormatting.DARK_RED));
                line.append(Component.translatable(news.get(i).getDescriptionId()).withStyle(ChatFormatting.RED));
            }
            if (news.size() > n) line.append(Component.literal(" và " + (news.size() - n) + " loài khác.").withStyle(ChatFormatting.DARK_RED));
            p.displayClientMessage(line, false);
        }
        Fx.sound(p, "minecraft:block.bell.resonate", SoundSource.AMBIENT, p.position(), 0.6f, 1.2f);
        Tiers.snapshot(p.server);
        if (Book.together(p.server)) {
            vn.cavemind.memory.MindData d = MindData.get(p.server);
            d.shared = Math.max(d.shared, g);
            d.setDirty();
            for (ServerPlayer o : p.server.getPlayerList().getPlayers()) if (o != p) shareTo(o, MindData.profile(o), g, p.getGameProfile().getName());
        }
        PaceLog.encounter(p, "chapter", "unlock", "chapter " + g + (host ? "" : " (with a friend)"));
        CaveMind.debug("{} unlocked chapter {}", p.getGameProfile().getName(), g);
    }

    static ItemStack compassFor(ServerLevel lvl, Profile pr, int g, Points.Point pt) {
        Points.Point way = Points.get(lvl, "w" + g);
        if (way == null || has(pr, g, WAY) || has(pr, g, ARRIVED)) return Points.compass(lvl, pt, pt.target);
        return Points.compass(lvl, pt, way.target);
    }

    static void retarget(ServerPlayer p, int g, BlockPos to) {
        var inv = p.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack st = inv.getItem(i);
            if (st.is(Items.COMPASS) && st.hasTag() && st.getTag().getBoolean("cavemind") && st.getTag().getInt("cavemind_gate") == g) {
                st.getTag().put("LodestonePos", net.minecraft.nbt.NbtUtils.writeBlockPos(to));
            }
        }
    }

    public static void catchUp(ServerPlayer p, Profile pr) {
        if (!on()) return;
        vn.cavemind.memory.MindData d = MindData.get(p.server);
        if (pr.chapter >= d.shared) return;
        shareTo(p, pr, d.shared, null);
    }

    static void shareTo(ServerPlayer o, Profile po, int g, String by) {
        if (po.chapter >= g) return;
        for (int k = Math.max(2, po.chapter + 1); k <= g; k++) {
            po.gate[k] |= READY | WON;
            grant(o, "c" + k + "_moi");
            grant(o, "c" + k + "_mo");
        }
        po.chapter = g;
        MindData.dirty(o.server);
        o.connection.send(new ClientboundSetTitlesAnimationPacket(10, 70, 30));
        o.connection.send(new ClientboundSetTitleTextPacket(Component.literal("Chương " + g).withStyle(ChatFormatting.LIGHT_PURPLE)));
        o.connection.send(new ClientboundSetSubtitleTextPacket(Component.literal(TITLE[g]).withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC)));
        tell(o, (by == null ? "Nhóm của bạn" : by) + " đã mở chương " + g + ": " + TITLE[g] + ". Cả nhóm cùng sang chương mới.", ChatFormatting.LIGHT_PURPLE);
        Tiers.snapshot(o.server);
    }

    public static void setChapter(ServerPlayer p, int n) {
        Profile pr = MindData.profile(p);
        pr.chapter = Math.max(1, Math.min(6, n));
        for (int g = 2; g <= 6; g++) {
            if (g <= pr.chapter) {
                pr.gate[g] |= READY | WON;
                grant(p, "c" + g + "_moi");
                grant(p, "c" + g + "_mo");
            } else {
                pr.gate[g] = 0;
            }
        }
        MindData.dirty(p.server);
        Tiers.snapshot(p.server);
    }

    public static String describe(ServerPlayer p) {
        Profile pr = MindData.profile(p);
        StringBuilder sb = new StringBuilder("chương " + pr.chapter + " (" + TITLE[pr.chapter] + ")");
        int g = pr.chapter + 1;
        if (g <= 6) {
            sb.append(", cổng ").append(g).append(":");
            String[] names = {"mời", "tới", "đọc", "nến", "đồ chơi", "dựng", "thắng"};
            for (int i = 0; i < names.length; i++) if ((pr.gate[g] & (1 << i)) != 0) sb.append(' ').append(names[i]);
            Points.Point pt = Points.get(p.server.overworld(), "g" + g);
            if (PLACE[g] && pt != null) sb.append(", điểm hẹn ").append(pt.target.getX()).append(' ').append(pt.target.getZ()).append(pt.built ? " (đã dựng)" : "");
        }
        sb.append(", ván chủ động ").append(pr.activeWins).append('/').append(pr.activeGames).append(", quà hiếm ").append(pr.gifts);
        return sb.toString();
    }

    private Chapters() {}
}
