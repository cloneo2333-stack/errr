package vn.cavemind.cmd;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.fml.loading.FMLPaths;
import vn.cavemind.damned.Haunt;
import vn.cavemind.damned.Journal;
import vn.cavemind.damned.Shrine;
import vn.cavemind.director.Director;
import vn.cavemind.director.Dread;
import vn.cavemind.man.ManMind;
import vn.cavemind.memory.MindData;
import vn.cavemind.memory.Profile;
import vn.cavemind.sense.Watch;

import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;

public final class Commands {
    public static void register(CommandDispatcher<CommandSourceStack> d) {
        d.register(literal("cavemind").requires(s -> s.hasPermission(2) || vn.cavemind.Cfg.DEV.get())
            .then(literal("status").executes(Commands::status))
            .then(literal("dev").executes(c -> {
                boolean on = !vn.cavemind.Cfg.DEV.get();
                vn.cavemind.Cfg.DEV.set(on);
                vn.cavemind.Cfg.DEV.save();
                return ok(c, "dev mode: " + on);
            }))
            .then(literal("test").executes(c -> {
                ServerPlayer p = c.getSource().getPlayerOrException();
                boolean on = vn.cavemind.compat.Compat.TEST.add(p.getUUID());
                if (!on) vn.cavemind.compat.Compat.TEST.remove(p.getUUID());
                return ok(c, "test mode (creative counts as survival): " + on);
            }))
            .then(literal("dump").executes(Commands::dump))
            .then(literal("eye").executes(c -> ok(c, "eye: " + (vn.cavemind.eye.Eye.dir == null ? "off (set eye=true or dev mode, then rejoin)" : vn.cavemind.eye.Eye.dir.toString())))
                .then(literal("mark").then(argument("text", StringArgumentType.greedyString()).executes(c -> {
                    vn.cavemind.eye.Eye.mark(c.getSource().getPlayerOrException(), StringArgumentType.getString(c, "text"));
                    return ok(c, "eye mark");
                }))))
            .then(literal("man")
                .then(literal("arrive").executes(c -> man(c, "arrive")))
                .then(literal("leave").executes(c -> man(c, "leave")))
                .then(literal("level").then(argument("n", IntegerArgumentType.integer(0, 10)).executes(c -> {
                    ServerPlayer p = c.getSource().getPlayerOrException();
                    MindData.profile(p).manLevel = IntegerArgumentType.getInteger(c, "n");
                    return ok(c, "man level set");
                })))
                .then(literal("anger").then(argument("n", IntegerArgumentType.integer(0, 100)).executes(c -> {
                    ServerPlayer p = c.getSource().getPlayerOrException();
                    MindData.profile(p).anger = IntegerArgumentType.getInteger(c, "n");
                    return ok(c, "anger set");
                })))
                .then(literal("bold").then(argument("n", IntegerArgumentType.integer(0, 100)).executes(c -> {
                    ServerPlayer p = c.getSource().getPlayerOrException();
                    MindData.profile(p).bold = IntegerArgumentType.getInteger(c, "n");
                    return ok(c, "boldness set");
                })))
                .then(literal("scheme").then(argument("kind", StringArgumentType.word()).executes(c -> {
                    ServerPlayer p = c.getSource().getPlayerOrException();
                    String k = StringArgumentType.getString(c, "kind");
                    boolean done = vn.cavemind.man.Schemes.run(p, Watch.of(p), MindData.profile(p), k, true);
                    return ok(c, "scheme " + k + ": " + done + " (trail nest spoil harvest peep seal animal wake)");
                })))
                .then(argument("tactic", StringArgumentType.word()).executes(c -> man(c, StringArgumentType.getString(c, "tactic")))))
            .then(literal("damned")
                .then(literal("stage").then(argument("n", IntegerArgumentType.integer(0, 4)).executes(c -> {
                    ServerPlayer p = c.getSource().getPlayerOrException();
                    Profile pr = MindData.profile(p);
                    pr.dStage = IntegerArgumentType.getInteger(c, "n");
                    pr.dNights = new int[]{0, 2, 4, 6, 8}[pr.dStage];
                    pr.dQuietUntilDay = -1;
                    return ok(c, "haunting stage set");
                })))
                .then(literal("shrine").executes(c -> {
                    ServerPlayer p = c.getSource().getPlayerOrException();
                    Profile pr = MindData.profile(p);
                    var s = Shrine.build(p, pr);
                    if (s != null) {
                        pr.dShrine = s;
                        pr.dShrineFound = false;
                    }
                    return ok(c, "shrine: " + s);
                }))
                .then(literal("journal").executes(c -> {
                    ServerPlayer p = c.getSource().getPlayerOrException();
                    p.getInventory().add(Journal.write(p, MindData.profile(p), false));
                    return ok(c, "journal given");
                }))
                .then(literal("banish").executes(c -> {
                    Haunt.banish(c.getSource().getPlayerOrException(), "command");
                    return ok(c, "banished");
                }))
                .then(argument("scene", StringArgumentType.word()).executes(c -> {
                    ServerPlayer p = c.getSource().getPlayerOrException();
                    try {
                        Haunt.Scene sc = Haunt.Scene.valueOf(StringArgumentType.getString(c, "scene").toUpperCase(Locale.ROOT));
                        return ok(c, sc + ": " + Haunt.force(p, sc));
                    } catch (IllegalArgumentException ex) {
                        return ok(c, "scenes: mirror beckon visit corner presence candle doorway stare");
                    }
                })))
            .then(literal("day").then(argument("piece", StringArgumentType.word()).executes(c -> {
                ServerPlayer p = c.getSource().getPlayerOrException();
                String id = StringArgumentType.getString(c, "piece");
                return ok(c, "day " + id + ": " + vn.cavemind.day.Daylight.force(p, id) + " (field seek ritual village house inside echo twin noon quiet)");
            })))
            .then(literal("hero").then(argument("what", StringArgumentType.word()).executes(c -> {
                ServerPlayer p = c.getSource().getPlayerOrException();
                String what = StringArgumentType.getString(c, "what");
                if (what.equals("status")) return ok(c, vn.cavemind.hero.Herobrine.status(p));
                if (what.equals("shrine")) {
                    vn.cavemind.hero.Herobrine.summoned(p);
                    return ok(c, "the shrine is lit");
                }
                boolean done = vn.cavemind.hero.Herobrine.start(p, Watch.of(p), MindData.profile(p), what, p.server.getTickCount(), true);
                return ok(c, "hero " + what + ": " + done + " (horizon walker miner builder replay bed shoulder sign tunnel leafless status)");
            })))
            .then(literal("lore").then(argument("what", StringArgumentType.word()).executes(c -> {
                ServerPlayer p = c.getSource().getPlayerOrException();
                String what = StringArgumentType.getString(c, "what");
                if (what.equals("status")) return ok(c, vn.cavemind.lore.Lore.status(p));
                return ok(c, "lore " + what + ": " + vn.cavemind.lore.Lore.force(p, what) + " (s1 g s2 h s3 status)");
            })))
            .then(literal("dread").then(argument("kind", StringArgumentType.word()).executes(c -> {
                ServerPlayer p = c.getSource().getPlayerOrException();
                Dread.force(p, StringArgumentType.getString(c, "kind"));
                return ok(c, "dread forced");
            }))));
    }

    private static int man(CommandContext<CommandSourceStack> c, String what) throws CommandSyntaxException {
        ServerPlayer p = c.getSource().getPlayerOrException();
        switch (what) {
            case "arrive" -> ManMind.forceArrive(p, null);
            case "leave" -> ManMind.dismiss(p);
            default -> {
                try {
                    ManMind.forceArrive(p, ManMind.Tac.valueOf(what.toUpperCase(Locale.ROOT)));
                } catch (IllegalArgumentException ex) {
                    return ok(c, "tactics: watch peek shadow peripheral ahead behind window knock hunt track doing peep witness lure");
                }
            }
        }
        return ok(c, "man: " + what);
    }

    private static int status(CommandContext<CommandSourceStack> c) throws CommandSyntaxException {
        ServerPlayer p = c.getSource().getPlayerOrException();
        c.getSource().sendSuccess(() -> Component.literal(line(p)), false);
        return 1;
    }

    private static String line(ServerPlayer p) {
        Watch w = Watch.of(p);
        Director.State s = Director.state(p);
        return "[CaveMind] " + p.getGameProfile().getName()
            + "\n director: tension=" + (int) s.tension + " target=" + (int) s.target + " phase=" + s.phase + " act=" + s.act + " others=" + s.others.size() + " blocked=" + Director.blocked(p) + " scared_today=" + (int) s.scareToday
            + "\n sense: fov=" + (int) w.fov + " modded=" + w.modded + " light=" + w.light + " underground=" + w.underground + " indoors=" + w.indoors
            + " confined=" + w.confined + " turns=" + w.turnArounds
            + "\n man: " + ManMind.status(p)
            + "\n damned: " + Haunt.status(p)
            + "\n daylight: " + vn.cavemind.day.Daylight.status(p)
            + "\n herobrine: " + vn.cavemind.hero.Herobrine.status(p);
    }

    private static int dump(CommandContext<CommandSourceStack> c) {
        StringBuilder sb = new StringBuilder();
        for (ServerPlayer p : c.getSource().getServer().getPlayerList().getPlayers()) sb.append(line(p)).append("\n\n");
        sb.append("recent:\n");
        synchronized (vn.cavemind.CaveMind.RECENT) {
            for (String s : vn.cavemind.CaveMind.RECENT) sb.append("  ").append(s).append('\n');
        }
        Path out = FMLPaths.GAMEDIR.get().resolve("cavemind_status.txt");
        try {
            Files.writeString(out, sb.toString(), StandardCharsets.UTF_8);
        } catch (IOException ex) {
            return 0;
        }
        return 1;
    }

    private static int ok(CommandContext<CommandSourceStack> c, String msg) {
        vn.cavemind.CaveMind.debug("command: {}", msg);
        c.getSource().sendSuccess(() -> Component.literal("[CaveMind] " + msg), false);
        return 1;
    }

    private Commands() {}
}
