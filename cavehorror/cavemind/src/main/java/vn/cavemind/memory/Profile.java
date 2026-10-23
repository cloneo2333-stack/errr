package vn.cavemind.memory;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.LongArrayTag;
import net.minecraft.nbt.Tag;

public final class Profile {
    public final UUID id;
    public String name = "";

    public long playTicks;
    public long firstDay = -1;
    public int turnArounds;
    public int stillInDark;
    public int torchesPlaced;

    public final Map<Long, Integer> heat = new HashMap<>();
    public BlockPos home;

    public int manLevel;
    public long manNext = -1;
    public int manSessions, manSightings, manHunts, manEscapes, manCaught, manHurt;
    public int reactStare, reactFlee, reactApproach, reactAttack, reactIgnore;
    public long manLastHuntDay = -100;
    public int turnsWhileWatched;

    public int dStage;
    public int dNights;
    public long dLastNight = -1;
    public long dNextDay = -1;
    public BlockPos dShrine;
    public boolean dShrineFound;
    public int dStolen;
    public int dHurt;
    public long dHurtDay = -1;
    public int dBanished;
    public long dQuietUntilDay = -1;
    public int dVisits;
    public int dJournals;
    public long dEffigyDay = -1;
    public int dGames, dFound, dWins, dPunish, dDebt, dCaught, dMazeWins, dMazeToys;
    public boolean dPeace;
    public long dNameDay = -1;
    public int dAbandon;
    public double heroPressure;
    public boolean manPeace, heroPeace, endingSeen;
    public long heroCalled = -1;
    public BlockPos mazeRet;
    public int chapter = 1;
    public final int[] gate = new int[7];
    public long freeDay = -100;
    public int activeGames, activeWins, gifts;
    public long gateRetry;
    public double meter;
    public int meterStage;
    public long meterDueDay = -1;
    public int meterTries;

    public final List<Home> homes = new ArrayList<>();
    public long homeDay = -1;
    public long pages;
    public BlockPos pageAt;
    public int pageIdx = -1;
    public long pageNext = -1;
    public boolean mazeGuide;
    public int dHolds, dHoldFails;
    public boolean gotFinder;
    public long lastHuntCatch = -1;
    public ListTag mazeBag;
    public long pasted, cleaned;
    public int stampGifts;

    public static final class Home {
        public BlockPos pos, bed;
        public double score;
        public int nights;
        public long lastNight = -1;
    }

    public final List<Long> chests = new ArrayList<>();
    public BlockPos nest, trailFrom, trailTo, trailAt, peep;
    public boolean trailDone;
    public int peepDir = -1;
    public int anger;
    public int bold;
    public String lastResp = "";
    public long angerDay = -1;
    public long schemeNext = -1;
    public int schemes;
    public long wakeDay = -100;
    public double bellHeat;
    public long bellDay = -1;
    public int hidden;
    public java.util.Set<String> lore = new java.util.HashSet<>();
    public final List<Clue> clues = new ArrayList<>();
    public final ListTag loot = new ListTag();

    public static final class Clue {
        public String kind;
        public BlockPos pos;
        public long day;
        public int radius;
        public boolean found;
        public int seen;

        public Clue(String kind, BlockPos pos, long day, int radius) {
            this.kind = kind;
            this.pos = pos;
            this.day = day;
            this.radius = radius;
        }
    }

    public void clue(Clue c) {
        clues.removeIf(o -> o.kind.equals(c.kind) && !o.found && o.pos.distSqr(c.pos) < 64);
        clues.add(c);
        while (clues.size() > 24) clues.remove(0);
    }

    public void stash(net.minecraft.world.item.ItemStack st) {
        if (st.isEmpty()) return;
        loot.add(st.save(new CompoundTag()));
        while (loot.size() > 27) loot.remove(0);
    }

    public void anger(int n) {
        anger = Math.max(0, Math.min(100, anger + n));
    }

    public void bold(int n) {
        bold = Math.max(0, Math.min(100, bold + n));
    }

    public final Deque<Long> placedTorches = new ArrayDeque<>();
    public final List<Moment> moments = new ArrayList<>();

    public long heroNext = -1;
    public int heroSeen, heroShows;
    public final Map<String, Long> heroLast = new HashMap<>();
    public final Deque<long[]> diary = new ArrayDeque<>();
    public final Deque<long[]> placedLog = new ArrayDeque<>();
    public final Deque<Long> brokenLog = new ArrayDeque<>();

    public void placedBlock(BlockPos p, int state) {
        placedLog.addLast(new long[]{p.asLong(), state});
        while (placedLog.size() > 256) placedLog.removeFirst();
    }

    public void brokeBlock(BlockPos p) {
        brokenLog.addLast(p.asLong());
        while (brokenLog.size() > 256) brokenLog.removeFirst();
    }

    public void diary(BlockPos p, long day, int time) {
        diary.addLast(new long[]{p.asLong(), day, time});
        while (diary.size() > 240) diary.removeFirst();
    }

    public Profile(UUID id) {
        this.id = id;
    }

    public void moment(Moment m) {
        moments.add(m);
        while (moments.size() > 64) moments.remove(0);
    }

    public void visit(BlockPos p, int w) {
        long key = (((long) (p.getX() >> 4)) << 32) | ((p.getZ() >> 4) & 0xffffffffL);
        heat.merge(key, w, Integer::sum);
        if (heat.size() > 1500) {
            heat.entrySet().removeIf(e -> e.getValue() < 6);
            heat.replaceAll((k, v) -> v / 2);
        }
    }

    public int heatAt(int x, int z) {
        long key = (((long) (x >> 4)) << 32) | ((z >> 4) & 0xffffffffL);
        return heat.getOrDefault(key, 0);
    }

    public void torch(BlockPos p) {
        placedTorches.addLast(p.asLong());
        while (placedTorches.size() > 300) placedTorches.removeFirst();
        torchesPlaced++;
    }

    public CompoundTag save() {
        CompoundTag t = new CompoundTag();
        t.putString("name", name);
        t.putLong("play", playTicks);
        t.putLong("firstDay", firstDay);
        t.putInt("turns", turnArounds);
        t.putInt("stillDark", stillInDark);
        t.putInt("torches", torchesPlaced);
        long[] hk = new long[heat.size()];
        long[] hv = new long[heat.size()];
        int i = 0;
        for (Map.Entry<Long, Integer> e : heat.entrySet()) {
            hk[i] = e.getKey();
            hv[i++] = e.getValue();
        }
        t.put("heatK", new LongArrayTag(hk));
        t.put("heatV", new LongArrayTag(hv));
        if (home != null) t.putLong("home", home.asLong());
        t.putInt("manLevel", manLevel);
        t.putLong("manNext", manNext);
        t.putIntArray("man", new int[]{manSessions, manSightings, manHunts, manEscapes, manCaught, manHurt,
            reactStare, reactFlee, reactApproach, reactAttack, reactIgnore, turnsWhileWatched});
        t.putLong("manHuntDay", manLastHuntDay);
        t.putIntArray("d", new int[]{dStage, dNights, dStolen, dHurt, dBanished, dVisits, dJournals, dShrineFound ? 1 : 0});
        t.putLongArray("dl", new long[]{dLastNight, dNextDay, dHurtDay, dQuietUntilDay, dEffigyDay});
        if (dShrine != null) t.putLong("shrine", dShrine.asLong());
        t.putLongArray("ptorch", placedTorches.stream().mapToLong(Long::longValue).toArray());
        ListTag ms = new ListTag();
        for (Moment m : moments) ms.add(m.save());
        t.put("moments", ms);
        CompoundTag mm = new CompoundTag();
        mm.putLongArray("chests", chests.stream().mapToLong(Long::longValue).toArray());
        if (nest != null) mm.putLong("nest", nest.asLong());
        if (trailFrom != null) mm.putLong("tFrom", trailFrom.asLong());
        if (trailTo != null) mm.putLong("tTo", trailTo.asLong());
        if (trailAt != null) mm.putLong("tAt", trailAt.asLong());
        if (peep != null) mm.putLong("peep", peep.asLong());
        mm.putBoolean("tDone", trailDone);
        mm.putInt("peepDir", peepDir);
        mm.putInt("anger", anger);
        mm.putInt("bold", bold);
        mm.putLong("angerDay", angerDay);
        mm.putLong("schemeNext", schemeNext);
        mm.putInt("schemes", schemes);
        mm.putLong("wakeDay", wakeDay);
        mm.putDouble("bellHeat", bellHeat);
        mm.putLong("bellDay", bellDay);
        mm.putInt("hidden", hidden);
        mm.putString("lore", String.join(",", lore));
        ListTag cl = new ListTag();
        for (Clue c : clues) {
            CompoundTag ct = new CompoundTag();
            ct.putString("k", c.kind);
            ct.putLong("p", c.pos.asLong());
            ct.putLong("d", c.day);
            ct.putInt("r", c.radius);
            ct.putBoolean("f", c.found);
            cl.add(ct);
        }
        mm.put("clues", cl);
        mm.put("loot", loot.copy());
        t.put("man2", mm);
        CompoundTag h = new CompoundTag();
        h.putLong("next", heroNext);
        h.putInt("seen", heroSeen);
        h.putInt("shows", heroShows);
        CompoundTag last = new CompoundTag();
        for (Map.Entry<String, Long> e : heroLast.entrySet()) last.putLong(e.getKey(), e.getValue());
        h.put("last", last);
        long[] dy = new long[diary.size() * 3];
        int k = 0;
        for (long[] d : diary) {
            dy[k++] = d[0];
            dy[k++] = d[1];
            dy[k++] = d[2];
        }
        h.putLongArray("diary", dy);
        long[] pl = new long[placedLog.size() * 2];
        k = 0;
        for (long[] d : placedLog) {
            pl[k++] = d[0];
            pl[k++] = d[1];
        }
        h.putLongArray("placed", pl);
        h.putLongArray("broken", brokenLog.stream().mapToLong(Long::longValue).toArray());
        t.put("hero", h);
        t.putIntArray("d4", new int[]{dGames, dFound, dWins, dPunish, dDebt, dCaught, dMazeWins, dMazeToys, dPeace ? 1 : 0});
        t.putLong("d4n", dNameDay);
        t.putInt("d4a", dAbandon);
        t.putDouble("hp", heroPressure);
        t.putIntArray("peace", new int[]{manPeace ? 1 : 0, heroPeace ? 1 : 0, endingSeen ? 1 : 0});
        t.putLong("hcall", heroCalled);
        if (mazeRet != null) t.putLong("mret", mazeRet.asLong());
        CompoundTag ch = new CompoundTag();
        ch.putInt("n", chapter);
        ch.putIntArray("gate", gate);
        ch.putLong("free", freeDay);
        ch.putIntArray("c", new int[]{activeGames, activeWins, gifts});
        t.put("ch", ch);
        CompoundTag bar = new CompoundTag();
        bar.putDouble("v", meter);
        bar.putInt("s", meterStage);
        bar.putLong("d", meterDueDay);
        t.put("mbar", bar);
        CompoundTag v5 = new CompoundTag();
        ListTag hs = new ListTag();
        for (Home h5 : homes) {
            CompoundTag c = new CompoundTag();
            c.putLong("p", h5.pos.asLong());
            if (h5.bed != null) c.putLong("b", h5.bed.asLong());
            c.putDouble("s", h5.score);
            c.putInt("n", h5.nights);
            c.putLong("l", h5.lastNight);
            hs.add(c);
        }
        v5.put("homes", hs);
        v5.putLong("homeDay", homeDay);
        v5.putLong("pages", pages);
        if (pageAt != null) v5.putLong("pageAt", pageAt.asLong());
        v5.putInt("pageIdx", pageIdx);
        v5.putLong("pageNext", pageNext);
        v5.putBoolean("mazeGuide", mazeGuide);
        v5.putInt("dHolds", dHolds);
        v5.putBoolean("gotFinder", gotFinder);
        v5.putInt("dHoldFails", dHoldFails);
        if (mazeBag != null) v5.put("mazeBag", mazeBag);
        v5.putLong("pasted", pasted);
        v5.putLong("cleaned", cleaned);
        v5.putInt("stampGifts", stampGifts);
        t.put("v5", v5);
        t.putInt("ver", 5);
        return t;
    }

    public static Profile load(UUID id, CompoundTag t) {
        Profile p = new Profile(id);
        p.name = t.getString("name");
        p.playTicks = t.getLong("play");
        p.firstDay = t.contains("firstDay") ? t.getLong("firstDay") : -1;
        p.turnArounds = t.getInt("turns");
        p.stillInDark = t.getInt("stillDark");
        p.torchesPlaced = t.getInt("torches");
        long[] hk = t.getLongArray("heatK"), hv = t.getLongArray("heatV");
        for (int i = 0; i < Math.min(hk.length, hv.length); i++) p.heat.put(hk[i], (int) hv[i]);
        if (t.contains("home")) p.home = BlockPos.of(t.getLong("home"));
        p.manLevel = t.getInt("manLevel");
        p.manNext = t.contains("manNext") ? t.getLong("manNext") : -1;
        int[] m = t.getIntArray("man");
        if (m.length >= 12) {
            p.manSessions = m[0]; p.manSightings = m[1]; p.manHunts = m[2]; p.manEscapes = m[3]; p.manCaught = m[4]; p.manHurt = m[5];
            p.reactStare = m[6]; p.reactFlee = m[7]; p.reactApproach = m[8]; p.reactAttack = m[9]; p.reactIgnore = m[10]; p.turnsWhileWatched = m[11];
        }
        p.manLastHuntDay = t.contains("manHuntDay") ? t.getLong("manHuntDay") : -100;
        int[] d = t.getIntArray("d");
        if (d.length >= 8) {
            p.dStage = d[0]; p.dNights = d[1]; p.dStolen = d[2]; p.dHurt = d[3]; p.dBanished = d[4]; p.dVisits = d[5]; p.dJournals = d[6]; p.dShrineFound = d[7] == 1;
        }
        long[] dl = t.getLongArray("dl");
        if (dl.length >= 5) {
            p.dLastNight = dl[0]; p.dNextDay = dl[1]; p.dHurtDay = dl[2]; p.dQuietUntilDay = dl[3]; p.dEffigyDay = dl[4];
        }
        if (t.contains("shrine")) p.dShrine = BlockPos.of(t.getLong("shrine"));
        for (long l : t.getLongArray("ptorch")) p.placedTorches.addLast(l);
        ListTag ms = t.getList("moments", Tag.TAG_COMPOUND);
        for (int i = 0; i < ms.size(); i++) p.moments.add(Moment.load(ms.getCompound(i)));
        if (t.contains("man2")) {
            CompoundTag mm = t.getCompound("man2");
            for (long l : mm.getLongArray("chests")) p.chests.add(l);
            if (mm.contains("nest")) p.nest = BlockPos.of(mm.getLong("nest"));
            if (mm.contains("tFrom")) p.trailFrom = BlockPos.of(mm.getLong("tFrom"));
            if (mm.contains("tTo")) p.trailTo = BlockPos.of(mm.getLong("tTo"));
            if (mm.contains("tAt")) p.trailAt = BlockPos.of(mm.getLong("tAt"));
            if (mm.contains("peep")) p.peep = BlockPos.of(mm.getLong("peep"));
            p.trailDone = mm.getBoolean("tDone");
            p.peepDir = mm.contains("peepDir") ? mm.getInt("peepDir") : -1;
            p.anger = mm.getInt("anger");
            p.bold = mm.getInt("bold");
            p.angerDay = mm.contains("angerDay") ? mm.getLong("angerDay") : -1;
            p.schemeNext = mm.contains("schemeNext") ? mm.getLong("schemeNext") : -1;
            p.schemes = mm.getInt("schemes");
            p.wakeDay = mm.contains("wakeDay") ? mm.getLong("wakeDay") : -100;
            p.bellHeat = mm.getDouble("bellHeat");
            p.bellDay = mm.contains("bellDay") ? mm.getLong("bellDay") : -1;
            p.hidden = mm.getInt("hidden");
            for (String x : mm.getString("lore").split(",")) if (!x.isEmpty()) p.lore.add(x);
            ListTag cl = mm.getList("clues", Tag.TAG_COMPOUND);
            for (int i = 0; i < cl.size(); i++) {
                CompoundTag ct = cl.getCompound(i);
                Clue c = new Clue(ct.getString("k"), BlockPos.of(ct.getLong("p")), ct.getLong("d"), ct.getInt("r"));
                c.found = ct.getBoolean("f");
                p.clues.add(c);
            }
            ListTag lt = mm.getList("loot", Tag.TAG_COMPOUND);
            for (int i = 0; i < lt.size(); i++) p.loot.add(lt.getCompound(i));
        }
        int[] d4 = t.getIntArray("d4");
        if (d4.length >= 9) {
            p.dGames = d4[0]; p.dFound = d4[1]; p.dWins = d4[2]; p.dPunish = d4[3]; p.dDebt = d4[4]; p.dCaught = d4[5];
            p.dMazeWins = d4[6]; p.dMazeToys = d4[7]; p.dPeace = d4[8] == 1;
        }
        p.dNameDay = t.contains("d4n") ? t.getLong("d4n") : -1;
        p.dAbandon = t.getInt("d4a");
        p.heroPressure = t.getDouble("hp");
        int[] pc = t.getIntArray("peace");
        if (pc.length >= 3) {
            p.manPeace = pc[0] == 1;
            p.heroPeace = pc[1] == 1;
            p.endingSeen = pc[2] == 1;
        }
        p.heroCalled = t.contains("hcall") ? t.getLong("hcall") : -1;
        if (t.contains("mret")) p.mazeRet = BlockPos.of(t.getLong("mret"));
        if (t.contains("ch")) {
            CompoundTag ch = t.getCompound("ch");
            p.chapter = Math.max(1, Math.min(6, ch.getInt("n")));
            int[] g = ch.getIntArray("gate");
            System.arraycopy(g, 0, p.gate, 0, Math.min(g.length, p.gate.length));
            p.freeDay = ch.contains("free") ? ch.getLong("free") : -100;
            int[] c = ch.getIntArray("c");
            if (c.length >= 3) {
                p.activeGames = c[0];
                p.activeWins = c[1];
                p.gifts = c[2];
            }
        }
        if (t.contains("v5")) {
            CompoundTag v5 = t.getCompound("v5");
            ListTag hs = v5.getList("homes", Tag.TAG_COMPOUND);
            for (int i = 0; i < hs.size(); i++) {
                CompoundTag c = hs.getCompound(i);
                Home h5 = new Home();
                h5.pos = BlockPos.of(c.getLong("p"));
                if (c.contains("b")) h5.bed = BlockPos.of(c.getLong("b"));
                h5.score = c.getDouble("s");
                h5.nights = c.getInt("n");
                h5.lastNight = c.getLong("l");
                p.homes.add(h5);
            }
            p.homeDay = v5.getLong("homeDay");
            p.pages = v5.getLong("pages");
            if (v5.contains("pageAt")) p.pageAt = BlockPos.of(v5.getLong("pageAt"));
            p.pageIdx = v5.getInt("pageIdx");
            p.pageNext = v5.getLong("pageNext");
            p.mazeGuide = v5.getBoolean("mazeGuide");
            p.dHolds = v5.getInt("dHolds");
            p.gotFinder = v5.getBoolean("gotFinder");
            p.dHoldFails = v5.getInt("dHoldFails");
            if (v5.contains("mazeBag")) p.mazeBag = v5.getList("mazeBag", Tag.TAG_COMPOUND);
            p.pasted = v5.getLong("pasted");
            p.cleaned = v5.getLong("cleaned");
            p.stampGifts = v5.getInt("stampGifts");
        }
        if (t.contains("mbar")) {
            CompoundTag bar = t.getCompound("mbar");
            p.meter = Math.max(0, Math.min(100, bar.getDouble("v")));
            p.meterStage = Math.max(0, Math.min(4, bar.getInt("s")));
            p.meterDueDay = bar.getLong("d");
        }
        if (t.contains("hero")) {
            CompoundTag h = t.getCompound("hero");
            p.heroNext = h.contains("next") ? h.getLong("next") : -1;
            p.heroSeen = h.getInt("seen");
            p.heroShows = h.getInt("shows");
            CompoundTag last = h.getCompound("last");
            for (String key : last.getAllKeys()) p.heroLast.put(key, last.getLong(key));
            long[] dy = h.getLongArray("diary");
            for (int i = 0; i + 2 < dy.length; i += 3) p.diary.addLast(new long[]{dy[i], dy[i + 1], dy[i + 2]});
            long[] pl = h.getLongArray("placed");
            for (int i = 0; i + 1 < pl.length; i += 2) p.placedLog.addLast(new long[]{pl[i], pl[i + 1]});
            for (long l : h.getLongArray("broken")) p.brokenLog.addLast(l);
        }
        return p;
    }
}
