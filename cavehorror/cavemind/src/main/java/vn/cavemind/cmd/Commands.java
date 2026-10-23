package vn.cavemind.cmd;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;

public final class Commands {
    public static void register(CommandDispatcher<CommandSourceStack> d) {
        d.register(literal("cavemind").requires(s -> s.hasPermission(2))
            .then(literal("hero").then(literal("shrine").executes(c -> {
                ServerPlayer p = c.getSource().getPlayerOrException();
                vn.cavemind.hero.Herobrine.summoned(p);
                return 1;
            })))
            .then(literal("trontim").executes(c -> trontim(c.getSource(), c.getSource().getPlayerOrException()))
                .then(argument("who", EntityArgument.player())
                    .executes(c -> trontim(c.getSource(), EntityArgument.getPlayer(c, "who")))))
            .then(literal("trang").then(argument("moon", StringArgumentType.word())
                .suggests((c, b) -> net.minecraft.commands.SharedSuggestionProvider.suggest(new String[]{"miner_moon", "blood_moon", "crimson_moon", "tat"}, b)).executes(c -> {
                String m = StringArgumentType.getString(c, "moon");
                vn.cavemind.world.Moon.force(m.equals("tat") ? null : m);
                c.getSource().sendSuccess(() -> Component.literal(m.equals("tat") ? "Trả lại trăng thật." : "Giả lập trăng: " + m), true);
                return 1;
            })))
            .then(literal("why").executes(c -> why(c.getSource(), c.getSource().getPlayerOrException()))
                .then(argument("who", EntityArgument.player())
                    .executes(c -> why(c.getSource(), EntityArgument.getPlayer(c, "who")))))
            .then(literal("chuong")
                .then(literal("moi").then(argument("who", EntityArgument.players()).then(argument("n", IntegerArgumentType.integer(2, 6)).executes(c -> {
                    int n = IntegerArgumentType.getInteger(c, "n"), done = 0;
                    for (ServerPlayer p : EntityArgument.getPlayers(c, "who")) done += vn.cavemind.chapter.Chapters.invite(p, n);
                    return done;
                }))))
                .then(literal("xem").executes(c -> show(c.getSource(), c.getSource().getPlayerOrException()))
                    .then(argument("who", EntityArgument.player()).executes(c -> show(c.getSource(), EntityArgument.getPlayer(c, "who")))))
                .then(literal("dat").then(argument("who", EntityArgument.player()).then(argument("n", IntegerArgumentType.integer(1, 6)).executes(c -> {
                    ServerPlayer p = EntityArgument.getPlayer(c, "who");
                    vn.cavemind.chapter.Chapters.setChapter(p, IntegerArgumentType.getInteger(c, "n"));
                    return show(c.getSource(), p);
                }))))
                .then(literal("xay").then(argument("id", StringArgumentType.word()).executes(c -> {
                    CommandSourceStack src = c.getSource();
                    String id = StringArgumentType.getString(c, "id");
                    if (!id.matches("[gw][2-6]")) {
                        src.sendFailure(Component.literal("id phải là g2..g6 hoặc w2..w6"));
                        return 0;
                    }
                    BlockPos at = BlockPos.containing(src.getPosition()).below();
                    Direction f = Direction.fromYRot(src.getRotation().y + 180f);
                    vn.cavemind.lore.Points.buildNow(src.getLevel(), id, at, f);
                    src.sendSuccess(() -> Component.literal("Đã dựng " + id + " tại " + at.toShortString()), true);
                    return 1;
                })))));
    }

    static int show(CommandSourceStack src, ServerPlayer p) {
        String line = "[Cave Mind] " + p.getGameProfile().getName() + ": " + vn.cavemind.chapter.Chapters.describe(p);
        src.sendSuccess(() -> Component.literal(line), false);
        return 1;
    }

    static int why(CommandSourceStack src, ServerPlayer p) {
        java.util.List<String> lines = new java.util.ArrayList<>();
        lines.add("[Cave Mind] " + p.getGameProfile().getName());
        lines.add("Nhịp: " + vn.cavemind.director.Tension.describe(p));
        String man = vn.cavemind.man.ManMind.describe(p);
        lines.add("The Man: " + (man != null ? man : vn.cavemind.life.Life.describe(p.server, p)));
        lines.add("The Damned: " + vn.cavemind.damned.Haunt.describe(p));
        lines.add("Herobrine: " + vn.cavemind.hero.Herobrine.describe(p) + "; " + vn.cavemind.hero.Echo.describe(p));
        String game = vn.cavemind.damned.Game.describe(p);
        String maze = vn.cavemind.maze.Maze.describe(p);
        lines.add("Đứa trẻ: " + (maze != null ? maze : game != null ? game : vn.cavemind.life.Child.describe(p.server, p)));
        lines.add("Kết: " + vn.cavemind.lore.Endings.describe(p));
        lines.add("Chương: " + vn.cavemind.chapter.Chapters.describe(p));
        vn.cavemind.director.Tension.Mind m = vn.cavemind.director.Tension.mind(p);
        if (!m.log.isEmpty()) lines.add("Gần đây: " + String.join(" | ", new java.util.ArrayList<>(m.log).subList(Math.max(0, m.log.size() - 5), m.log.size())));
        for (String l : lines) src.sendSuccess(() -> Component.literal(l), false);
        return 1;
    }

    static int trontim(CommandSourceStack src, ServerPlayer p) {
        if (!vn.cavemind.damned.Game.on()) {
            src.sendFailure(Component.literal("Mod The Damned (Apollyon) chưa chạy hoặc trò trốn tìm đang tắt trong config."));
            return 0;
        }
        if (vn.cavemind.damned.Game.active(p)) {
            src.sendFailure(Component.literal(p.getGameProfile().getName() + " đang trong một ván rồi."));
            return 0;
        }
        if (p.level().dimension() != net.minecraft.world.level.Level.OVERWORLD) {
            src.sendFailure(Component.literal("Ván trốn tìm chỉ bắt đầu ở Overworld."));
            return 0;
        }
        if (vn.cavemind.compat.Compat.exempt(p)) {
            src.sendFailure(Component.literal("Nó không chơi với người ở chế độ Sáng tạo hoặc Khán giả. Gõ /gamemode survival rồi thử lại."));
            return 0;
        }
        if (!vn.cavemind.director.Director.claim(p, "damned")) {
            src.sendFailure(Component.literal(p.getGameProfile().getName() + " đang bận một cuộc chạm trán khác (The Man, Herobrine...). Đợi nó kết thúc rồi thử lại."));
            return 0;
        }
        vn.cavemind.director.Director.abort(p, "damned");
        boolean ok = vn.cavemind.damned.Game.start(p, "lệnh thử");
        if (ok) src.sendSuccess(() -> Component.literal("Nó đang đứng đâu đó gần " + p.getGameProfile().getName() + ", sắp đếm."), true);
        else src.sendFailure(Component.literal("Không mở được ván (không có chỗ đứng hợp lệ trong 9–26 khối quanh người chơi). Đi sang chỗ khác rồi thử lại."));
        return ok ? 1 : 0;
    }

    private Commands() {}
}
