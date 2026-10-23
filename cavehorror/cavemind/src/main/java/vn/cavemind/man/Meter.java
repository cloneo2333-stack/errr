package vn.cavemind.man;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import vn.cavemind.CaveMind;
import vn.cavemind.Cfg;
import vn.cavemind.compat.Compat;
import vn.cavemind.director.Dread;
import vn.cavemind.director.PaceLog;
import vn.cavemind.memory.MindData;
import vn.cavemind.memory.Profile;
import vn.cavemind.net.Net;
import vn.cavemind.sense.Watch;
import vn.cavemind.world.Fx;

public final class Meter {
    static final String[] RISE = {
        "Hắn đang quan sát bạn",
        "Hắn học thói quen của bạn",
        "Hắn đã biết chỗ bạn ngủ",
        "Hắn mất dần kiên nhẫn"
    };
    static final String[] DUE = {
        "",
        "Hắn sắp ghé nhà bạn khi bạn vắng mặt",
        "Khi trời tối, hắn sẽ tới thật gần",
        "Đêm nay hắn sẽ thử bạn trong bóng tối",
        "ĐÊM SĂN: không thể ngủ. Giữ ánh sáng, rung chuông, sống sót"
    };
    static final String[] DONE = {
        "",
        "Có người đã ghé nhà bạn.",
        "Hắn đã tới. Hắn biết đường rồi.",
        "Hắn đã thử bạn. Lần sau hắn sẽ không chỉ nhìn.",
        "Bạn đã sống qua đêm săn. Hắn lùi lại, nhưng không đi xa."
    };

    public static boolean test;

    public static boolean on() {
        return Cfg.MAN_BAR.get() && (test || Compat.manActive());
    }

    public static int due(Profile pr) {
        int next = pr.meterStage + 1;
        return next <= 4 && pr.meter >= next * 25 - 0.001 ? next : 0;
    }

    public static boolean huntDue(ServerPlayer p) {
        return on() && due(MindData.profile(p)) == 4 && canHunt(p);
    }

    static boolean canHunt(ServerPlayer p) {
        Profile pr = MindData.profile(p);
        return Cfg.MAN_HUNT.get() && !pr.manPeace && p.serverLevel().getDifficulty() != net.minecraft.world.Difficulty.PEACEFUL;
    }

    public static boolean wantsVisit(ServerPlayer p) {
        if (!on()) return false;
        int d = due(MindData.profile(p));
        return d == 2 || d == 3 || d == 4 && canHunt(p);
    }

    public static boolean dark(ServerPlayer p, Watch w) {
        ServerLevel lvl = p.serverLevel();
        int t = Dread.time(lvl);
        return t >= 11000 && t < 23300 || w.underground || lvl.isThundering();
    }

    public static String status(Profile pr) {
        boolean met = pr.manSightings > 0 || pr.manSessions > 0 || pr.manLevel >= 2;
        if (!met) return "Bạn chưa gặp hắn. Hắn đã thấy bạn rồi.";
        int d = due(pr);
        String now = d > 0 ? DUE[d] : RISE[Math.min(3, pr.meterStage)];
        return now + ". Thanh đầy " + (int) pr.meter + "/100. Ngủ qua đêm không làm hắn quên: hắn sẽ tới từ lúc chạng vạng, trước khi bạn kịp lên giường.";
    }

    public static void add(ServerPlayer p, double v, String why) {
        if (!on() || p.level().dimension() != Level.OVERWORLD) return;
        Profile pr = MindData.profile(p);
        double cap = Math.min(100, (pr.meterStage + 1) * 25);
        double before = pr.meter;
        pr.meter = Math.max(pr.meterStage * 25, Math.min(cap, pr.meter + v));
        if ((int) before / 5 != (int) pr.meter / 5) CaveMind.debug("{}'s Man bar {} -> {} ({})", p.getGameProfile().getName(), (int) before, (int) pr.meter, why);
        if (due(pr) > 0 && before < cap - 0.001 && pr.meter >= cap - 0.001) reached(p, pr);
    }

    static void reached(ServerPlayer p, Profile pr) {
        int d = due(pr);
        pr.meterTries = 0;
        if (d == 4) pr.meterDueDay = Dread.day(p.serverLevel());
        PaceLog.encounter(p, "man", "bar " + d * 25, DUE[d]);
        p.displayClientMessage(Component.literal(DUE[d]).withStyle(d == 4 ? ChatFormatting.DARK_RED : ChatFormatting.GRAY, ChatFormatting.ITALIC), true);
        if (d >= 3) Fx.sound(p, "man:long_whisper", SoundSource.HOSTILE, p.position().add(p.getLookAngle().scale(-6)), 0.35f, 0.7f);
    }

    public static void complete(ServerPlayer p, int stage) {
        if (!on()) return;
        Profile pr = MindData.profile(p);
        if (due(pr) != stage) return;
        pr.meterStage = stage;
        pr.meterTries = 0;
        PaceLog.encounter(p, "man", "bar done", "stage " + stage);
        p.displayClientMessage(Component.literal(DONE[stage]).withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC), false);
        if (stage == 4) {
            pr.meterStage = 0;
            pr.meter = 18;
            pr.meterDueDay = -1;
        }
        send(p, pr);
    }

    public static void slept(List<ServerPlayer> ps) {
        for (ServerPlayer p : ps) add(p, 16, "ngủ qua đêm");
    }

    public static void seen(ServerPlayer p, double v) {
        add(p, v, "thấy hắn");
    }

    public static void bell(ServerPlayer p) {
        if (!on()) return;
        Profile pr = MindData.profile(p);
        if (due(pr) != 0) return;
        pr.meter = Math.max(pr.meterStage * 25, pr.meter - 8);
    }

    public static void tick(ServerPlayer p, Watch w, Profile pr, long now) {
        if (now % 20 != (p.getId() & 15) || p.level().dimension() != Level.OVERWORLD || p.isSpectator()) return;
        if (!on()) {
            if (now % 100 == (p.getId() & 15)) Net.send(p, new Net.Bar(-1, 0, false, ""));
            return;
        }
        ServerLevel lvl = p.serverLevel();
        boolean met = pr.manSightings > 0 || pr.manSessions > 0 || pr.manLevel >= 2;
        if (!met && pr.meter < 24 && !Compat.exempt(p) && !p.isSleeping() && w.idle < 1200) {
            pr.meter = Math.min(24, pr.meter + 100.0 / (Cfg.MAN_BAR_MINUTES.get() * 60.0) * 0.4);
        }
        if (met && !Compat.exempt(p) && !p.isSleeping() && w.idle < 1200 && !ManMind.active(p)) {
            double rate = 100.0 / (Cfg.MAN_BAR_MINUTES.get() * 60.0);
            if (dark(p, w)) rate *= 1.5;
            if (w.underground) rate *= 1.3;
            if (pr.manLevel < 2) rate *= 0.5;
            add(p, rate, "thời gian");
        }
        int d = due(pr);
        if (d == 1 && now % 600 < 20) traces(p, w, pr);
        if (d == 4 && !canHunt(p)) {
            pr.meter = 99;
        }
        if (d == 4 && pr.meterDueDay >= 0 && Dread.day(lvl) - pr.meterDueDay >= 3) {
            pr.meterStage = 3;
            pr.meter = 80;
            pr.meterDueDay = -1;
            p.displayClientMessage(Component.literal("Hắn không tìm được lúc ra tay. Hắn sẽ quay lại.").withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC), false);
        }
        if (now % 40 < 20) send(p, pr);
    }

    static void traces(ServerPlayer p, Watch w, Profile pr) {
        String[] order = {"trail", "nest", "spoil", "harvest", "peep"};
        for (String k : order) {
            boolean ready = switch (k) {
                case "trail" -> pr.home != null && !pr.trailDone && Cfg.MAN_SCHEMES.get();
                case "nest" -> pr.trailFrom != null && pr.nest == null && Cfg.MAN_SCHEMES.get();
                case "spoil" -> !pr.chests.isEmpty() && Cfg.MAN_STEAL.get();
                case "harvest" -> pr.home != null && Cfg.MAN_STEAL.get();
                default -> pr.home != null && pr.peep == null && Cfg.MAN_SCHEMES.get();
            };
            if (!ready) continue;
            if (Schemes.run(p, w, pr, k, true)) {
                complete(p, 1);
                return;
            }
        }
        if (++pr.meterTries >= 6) complete(p, 1);
    }

    static void send(ServerPlayer p, Profile pr) {
        boolean met = pr.manSightings > 0 || pr.manSessions > 0 || pr.manLevel >= 2;
        if (!met) {
            Net.send(p, new Net.Bar((float) pr.meter, 0, false, "Bạn chưa thấy hắn. Hắn thì đã thấy bạn."));
            return;
        }
        int d = due(pr);
        String label = d > 0 ? DUE[d] : RISE[Math.min(3, pr.meterStage)];
        Net.send(p, new Net.Bar((float) pr.meter, pr.meterStage, d > 0, label));
    }

    private Meter() {}
}
