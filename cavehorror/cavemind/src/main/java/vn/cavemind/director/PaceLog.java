package vn.cavemind.director;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.storage.LevelResource;
import vn.cavemind.CaveMind;

public final class PaceLog {
    private static final List<String> PENDING = new ArrayList<>();
    private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("MM-dd HH:mm:ss");

    static void line(ServerPlayer p, String kind, String text) {
        synchronized (PENDING) {
            PENDING.add(LocalDateTime.now().format(TS) + "\t" + p.server.getTickCount() / 20 + "\t" + p.getGameProfile().getName() + "\t" + kind + "\t" + text);
        }
    }

    static void phase(ServerPlayer p, Tension.Phase from, Tension.Phase to, double t, String why) {
        line(p, "phase", from + ">" + to + "\t" + Math.round(t) + "\t" + why);
    }

    static void sample(ServerPlayer p, Tension.Mind m) {
        line(p, "tension", m.phase + "\t" + Math.round(m.tension) + "\t" + String.format(java.util.Locale.ROOT, "%.1f", m.menace));
    }

    public static void encounter(ServerPlayer p, String who, String what, String cause) {
        line(p, "meet", who + "\t" + what + "\t" + cause);
    }

    public static void flush(MinecraftServer server) {
        List<String> out;
        synchronized (PENDING) {
            if (PENDING.isEmpty()) return;
            out = new ArrayList<>(PENDING);
            PENDING.clear();
        }
        Path f = server.getWorldPath(LevelResource.ROOT).resolve("cavemind_pace.log");
        try {
            if (Files.exists(f) && Files.size(f) > 4_000_000L) Files.move(f, f.resolveSibling("cavemind_pace.old.log"), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            Files.write(f, out, StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException e) {
            CaveMind.LOG.warn("[CaveMind] pace log failed: {}", e.toString());
        }
    }

    private PaceLog() {}
}
