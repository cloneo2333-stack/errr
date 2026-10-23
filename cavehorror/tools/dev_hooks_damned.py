import os

BASE = r'E:\Document\New project 5\Claude\1\cavehorror\cavemind\src\main\java\vn\cavemind\damned'
s = ''


def rep(old, new):
    global s
    assert old in s, ('missing', old[:90])
    s = s.replace(old, new, 1)


def load(name):
    global s
    s = open(os.path.join(BASE, name), encoding='utf-8').read()


def save(name):
    open(os.path.join(BASE, name), 'w', encoding='utf-8').write(s)


load('Haunt.java')
rep('        if (day < Cfg.DAMNED_START_DAY.get() || pr.playTicks < 24000) return;',
    '        if (!Dev.on() && (day < Cfg.DAMNED_START_DAY.get() || pr.playTicks < 24000)) return;')
rep('        boolean scene = r.nextDouble() < Cfg.DAMNED_NIGHT_CHANCE.get();\n        pl.at = scene ? 13400 + r.nextInt(6500) : -1;',
    '        boolean scene = Dev.on() || r.nextDouble() < Cfg.DAMNED_NIGHT_CHANCE.get();\n'
    '        pl.at = scene ? (Dev.on() ? 13100 + r.nextInt(1400) : 13400 + r.nextInt(6500)) : -1;')
rep('        pl.tracesAt = 14000 + r.nextInt(8000);',
    '        pl.tracesAt = Dev.on() ? 13500 + r.nextInt(1500) : 14000 + r.nextInt(8000);')
rep('        CaveMind.debug("haunting plan for {}: scene@{} traces={} visit={}", p.getGameProfile().getName(), pl.at, pl.traces, pl.visit);',
    '        CaveMind.debug("haunting plan for {}: scene@{} traces={} visit={}", p.getGameProfile().getName(), pl.at, pl.traces, pl.visit);\n'
    '        Dev.say(p, "Đêm nay The Damned (giai đoạn " + pr.dStage + "): " + (pl.at < 0 ? "không có cảnh" : "cảnh lúc " + Journal.clock(pl.at))\n'
    '            + (pl.traces ? ", để lại dấu vết lúc " + Journal.clock(pl.tracesAt) : "") + (pl.visit ? ", sẽ viếng khi bạn ngủ" : "")\n'
    '            + (pl.corner ? ", đứng trong góc lúc bạn thức dậy" : ""));')
rep('                CaveMind.debug("something calls from far away for {}", p.getGameProfile().getName());',
    '                CaveMind.debug("something calls from far away for {}", p.getGameProfile().getName());\n'
    '                Dev.say(p, "The Damned gọi từ rất xa (giai đoạn 0: chỉ có âm thanh)");')
rep('        RUNS.put(p.getUUID(), r);\n        CaveMind.debug("haunting {} begins for {}", sc, p.getGameProfile().getName());',
    '        RUNS.put(p.getUUID(), r);\n        CaveMind.debug("haunting {} begins for {}", sc, p.getGameProfile().getName());\n'
    '        Entity db = body(p, r);\n'
    '        Dev.say(p, "The Damned: " + vi(sc) + (db != null ? " — " + Dev.where(p, db.position()) : ""));')
rep('            CaveMind.debug("haunting {} could not start for {} (no place)", sc, p.getGameProfile().getName());',
    '            CaveMind.debug("haunting {} could not start for {} (no place)", sc, p.getGameProfile().getName());\n'
    '            Dev.say(p, "The Damned: " + vi(sc) + " chưa bắt đầu được (không có chỗ/điều kiện), sẽ thử lại");')
rep('            pr.dStage = st;\n            CaveMind.debug("haunting of {} reaches stage {}", p.getGameProfile().getName(), st);',
    '            pr.dStage = st;\n            CaveMind.debug("haunting of {} reaches stage {}", p.getGameProfile().getName(), st);\n'
    '            Dev.say(p, "The Damned lên giai đoạn " + st);')
rep('        if (!lvl.addFreshEntity(e)) {\n            Compat.ONLY_MAP.remove(e.getUUID());\n            CaveMind.debug("the Damned was refused',
    '        Dev.glow(e);\n        if (!lvl.addFreshEntity(e)) {\n            Compat.ONLY_MAP.remove(e.getUUID());\n            CaveMind.debug("the Damned was refused')
rep('                CaveMind.debug("it visits {} in their sleep", p.getGameProfile().getName());',
    '                CaveMind.debug("it visits {} in their sleep", p.getGameProfile().getName());\n'
    '                Dev.say(p, "The Damned đang đứng ở cuối giường bạn");')
rep('            CaveMind.debug("it waits in the corner for {}", p.getGameProfile().getName());',
    '            CaveMind.debug("it waits in the corner for {}", p.getGameProfile().getName());\n'
    '            Dev.say(p, "The Damned đứng quay mặt vào góc phòng, gần bạn");')
rep('        Fx.sound(p, "apollyon:damage_noise", SoundSource.HOSTILE, e.position(), 0.8f, 0.6f);',
    '        Dev.say(p, "Bạn đánh The Damned — nó không phản công, chỉ biến mất (và nhớ chuyện này)");\n'
    '        Fx.sound(p, "apollyon:damage_noise", SoundSource.HOSTILE, e.position(), 0.8f, 0.6f);')
rep('        CaveMind.debug("{} banished the Damned ({})", p.getGameProfile().getName(), how);',
    '        CaveMind.debug("{} banished the Damned ({})", p.getGameProfile().getName(), how);\n'
    '        Dev.say(p, "Đã trục xuất The Damned (" + how + "). Yên tĩnh tới ngày " + pr.dQuietUntilDay);')
rep('    static Entity body(ServerPlayer p, Run r) {',
    '    static String vi(Scene sc) {\n'
    '        return switch (sc) {\n'
    '            case MIRROR -> "gương soi (bắt chước mọi cử động của bạn)";\n'
    '            case BECKON -> "dẫn lối tới đền thờ";\n'
    '            case VISIT -> "viếng lúc ngủ";\n'
    '            case CORNER -> "đứng trong góc";\n'
    '            case PRESENCE -> "hơi thở sau lưng (đứng yên trong tối)";\n'
    '            case CANDLE -> "ngọn nến tắt trước mắt";\n'
    '            case DOORWAY -> "đứng ở ô cửa tối";\n'
    '            case STARE -> "mọi sinh vật quay sang nhìn bạn";\n'
    '        };\n'
    '    }\n\n'
    '    public static String devLine(ServerPlayer p) {\n'
    '        Profile pr = MindData.profile(p);\n'
    '        Run r = RUNS.get(p.getUUID());\n'
    '        Plan pl = PLANS.get(p.getUUID());\n'
    '        String head = "Damned gđ" + pr.dStage;\n'
    '        if (r != null) {\n'
    '            Entity e = body(p, r);\n'
    '            return head + ": " + r.scene + (e != null ? " " + Dev.where(p, e.position()) : "");\n'
    '        }\n'
    '        return head + (pl != null && pl.at >= 0 ? ": cảnh lúc " + Journal.clock(pl.at) : ": chờ đêm");\n'
    '    }\n\n'
    '    static Entity body(ServerPlayer p, Run r) {')
s = s.replace('import vn.cavemind.director.Director;', 'import vn.cavemind.dev.Dev;\nimport vn.cavemind.director.Director;', 1)
save('Haunt.java')

load('Shrine.java')
rep('            CaveMind.debug("the Damned built a shrine for {} at {}", p.getGameProfile().getName(), c);',
    '            CaveMind.debug("the Damned built a shrine for {} at {}", p.getGameProfile().getName(), c);\n'
    '            vn.cavemind.dev.Dev.say(p, "The Damned đã dựng đền thờ — " + vn.cavemind.dev.Dev.where(p, Vec3.atCenterOf(c))'
    ' + " tại " + c.getX() + " " + c.getY() + " " + c.getZ());')
save('Shrine.java')

load('Traces.java')
rep("        CaveMind.debug(\"night traces at {}'s home: {}\", p.getGameProfile().getName(), done);",
    "        CaveMind.debug(\"night traces at {}'s home: {}\", p.getGameProfile().getName(), done);\n"
    '        vn.cavemind.dev.Dev.say(p, "Dấu vết đêm quanh nhà: " + done + " thứ (nến tắt / đuốc mất / khung tranh xoay)");')
save('Traces.java')

load('Effigy.java')
rep('                CaveMind.debug("an effigy of {} waits outside at {}", p.getGameProfile().getName(), feet);',
    '                CaveMind.debug("an effigy of {} waits outside at {}", p.getGameProfile().getName(), feet);\n'
    '                vn.cavemind.dev.Dev.say(p, "Hình nộm mang đầu bạn đứng ngoài nhà tại " + feet.getX() + " " + feet.getY() + " " + feet.getZ());')
save('Effigy.java')
print('ok')
