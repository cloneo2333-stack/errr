package vn.cavemind.man;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
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

    // What the player will find, so a visit is never just a line of text.
    static final Map<String, String> HINT = Map.ofEntries(
        Map.entry("trail", "Có một lối mòn mới dẫn tới cửa sổ nhà bạn."),
        Map.entry("nest", "Ở bìa rừng gần nhà có thứ gì như một cái ổ."),
        Map.entry("peep", "Bức tường cạnh giường bị khoét một lỗ."),
        Map.entry("spoil", "Đồ ăn trong rương có mùi lạ."),
        Map.entry("harvest", "Ruộng bị giẫm nát một vệt."),
        Map.entry("door", "Cửa nhà bạn đang mở."),
        Map.entry("torch", "Vài ngọn đuốc quanh nhà bị nhổ đi, cắm thành hàng dẫn ra xa."),
        Map.entry("inside", "Có người đã vào trong nhà. Để ý cạnh giường và các rương."),
        Map.entry("prints", "Quanh nhà có một vòng dấu chân lạ."),
        Map.entry("candle", "Trong nhà có một ngọn nến ai đó vừa thắp."));

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
        finish(p, stage, DONE[stage]);
    }

    static void finish(ServerPlayer p, int stage, String text) {
        if (!on()) return;
        Profile pr = MindData.profile(p);
        if (due(pr) != stage) return;
        pr.meterStage = stage;
        pr.meterTries = 0;
        pr.visitAway = false;
        PaceLog.encounter(p, "man", "bar done", "stage " + stage);
        p.displayClientMessage(Component.literal(text).withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC), false);
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
        if (!Double.isNaN(pr.lastX) && Math.hypot(p.getX() - pr.lastX, p.getZ() - pr.lastZ) > 100) jumped(p, false);
        pr.lastX = p.getX();
        pr.lastZ = p.getZ();
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
        if (d == 1 && (now % 600 < 20 || pr.visitAway && now % 100 < 20)) traces(p, w, pr, now % 600 < 20);
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

    // Home for the Man's visits: the lived-in home, else the base the player built, else their bed's respawn point.
    static BlockPos homeOf(ServerPlayer p, Profile pr) {
        if (pr.home != null) return pr.home;
        vn.cavemind.life.LifeData.Base b = vn.cavemind.life.Bases.owned(vn.cavemind.life.LifeData.get(p.server), p);
        if (b != null) return b.center;
        return p.getRespawnDimension() == Level.OVERWORLD ? p.getRespawnPosition() : null;
    }

    // Stage 1, "he will visit your home while you are away". It only counts once something at home has really changed.
    // While you are far off he comes over, works for a few minutes and leaves. Walk back in time and he may still be
    // there, next to what he did: see him first and you have caught him in the act. Respawn or teleport straight home
    // and there is nobody to catch, he left a step ahead of you.
    static void traces(ServerPlayer p, Watch w, Profile pr, boolean counted) {
        if (vn.cavemind.life.Glimpse.caughtFor(p)) return;
        if (counted) pr.meterTries++;
        BlockPos home = homeOf(p, pr);
        if (home == null) {
            if (counted && pr.meterTries >= 6 && vn.cavemind.life.Life.campVisit(p))
                finish(p, 1, "Quanh chỗ bạn vừa đứng có một vòng dấu chân lạ. Hắn đang tìm xem nhà bạn ở đâu.");
            return;
        }
        ServerLevel lvl = p.serverLevel();
        long now = p.server.getTickCount();
        double dist = Math.sqrt(home.distSqr(p.blockPosition()));
        boolean loaded = lvl.hasChunksAt(home.offset(-20, 0, -20), home.offset(20, 0, 20));
        if (dist > 48 && !loaded) {
            if (!pr.visitAway) {
                pr.visitAway = true;
                pr.visitT0 = now + 400 + p.getRandom().nextInt(1200);
                pr.visitEnd = pr.visitT0 + 1800 + p.getRandom().nextInt(1800);
                CaveMind.debug("{}'s home visit: he gets there in {}s and stays {}s", p.getGameProfile().getName(), (pr.visitT0 - now) / 20, (pr.visitEnd - pr.visitT0) / 20);
            }
            return;
        }
        boolean jumped = now - pr.lastJump < 200 && dist < 40;
        if (pr.visitAway) {
            if (now < pr.visitT0) {
                if (jumped || dist < 72) pr.visitAway = false;
                return;
            }
            if (jumped) {
                String did = visit(p, w, pr, home, true);
                if (did == null) {
                    pr.visitAway = false;
                    return;
                }
                String lead = now - pr.lastRespawn < 200 ? "Bạn tỉnh dậy ở nhà. Có người vừa ở đây, trước bạn một bước." : "Bạn về tới nhà ngay sau khi có người vừa rời đi.";
                finish(p, 1, lead + " " + HINT.getOrDefault(did, ""));
                return;
            }
            if (now <= pr.visitEnd && dist > 72) return;
            String did = visit(p, w, pr, home, true);
            if (did == null) {
                pr.visitAway = false;
                return;
            }
            if (now > pr.visitEnd || !catchMe(p, did, home)) finish(p, 1, DONE[1] + " " + HINT.getOrDefault(did, ""));
            return;
        }
        boolean freely = dist > 40;
        if (!freely && pr.meterTries < 8) return;
        String did = visit(p, w, pr, home, freely);
        if (did != null) {
            if (freely && now - pr.lastJump > 200 && p.getRandom().nextFloat() < 0.6f && catchMe(p, did, home)) return;
            finish(p, 1, DONE[1] + " " + HINT.getOrDefault(did, ""));
            return;
        }
        if (pr.meterTries >= 40) finish(p, 1, "Hắn lảng vảng quanh nhà bạn mấy đêm liền nhưng chưa tìm được lối vào.");
    }

    // Leaves his body next to what he just did; the stage closes when that scene ends.
    static boolean catchMe(ServerPlayer p, String did, BlockPos home) {
        BlockPos spot = List.of("door", "torch", "inside", "prints", "candle").contains(did) ? vn.cavemind.life.Life.lastSpot : Schemes.lastSpot;
        String hint = HINT.getOrDefault(did, "");
        return vn.cavemind.life.Glimpse.caught(p, spot != null ? spot : home, seen -> finish(p, 1, seen
            ? "Bạn bắt quả tang hắn ngay ở nhà mình. Hắn đứng sững lại, rồi tan vào sương. " + hint
            : DONE[1] + " " + hint));
    }

    // Respawning, changing dimension or teleporting: you arrive somewhere without walking there.
    public static void jumped(ServerPlayer p, boolean respawn) {
        Profile pr = MindData.profile(p);
        long now = p.server.getTickCount();
        pr.lastJump = now;
        if (respawn) pr.lastRespawn = now;
        pr.lastX = Double.NaN;
        if (vn.cavemind.life.Glimpse.caughtFor(p)) vn.cavemind.life.Glimpse.cancel(p);
    }

    static String visit(ServerPlayer p, Watch w, Profile pr, BlockPos home, boolean freely) {
        BlockPos was = pr.home;
        if (was == null) pr.home = home;
        try {
            List<String> order = new ArrayList<>(List.of("trail", "nest", "peep", "spoil", "harvest", "life"));
            Collections.shuffle(order, new java.util.Random(p.getRandom().nextLong()));
            for (String k : order) {
                if (k.equals("life")) {
                    String did = vn.cavemind.life.Life.homeVisit(p, home, freely);
                    if (did != null) return did;
                    continue;
                }
                boolean ready = switch (k) {
                    case "trail" -> !pr.trailDone && Cfg.MAN_SCHEMES.get();
                    case "nest" -> pr.trailFrom != null && pr.nest == null && Cfg.MAN_SCHEMES.get();
                    case "spoil" -> !pr.chests.isEmpty() && Cfg.MAN_STEAL.get();
                    case "harvest" -> Cfg.MAN_STEAL.get();
                    default -> pr.peep == null && Cfg.MAN_SCHEMES.get();
                };
                if (ready && Schemes.run(p, w, pr, k, freely)) return k;
            }
            return null;
        } finally {
            if (was == null) pr.home = null;
        }
    }

    // Stages 2 and 3 count when the visit really happened (he came close, or tested you in the dark);
    // after three empty visits he is considered to have made his point.
    public static void visited(ServerPlayer p, int stage, boolean real) {
        if (!on()) return;
        Profile pr = MindData.profile(p);
        if (due(pr) != stage) return;
        if (real || ++pr.meterTries >= 3) complete(p, stage);
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
