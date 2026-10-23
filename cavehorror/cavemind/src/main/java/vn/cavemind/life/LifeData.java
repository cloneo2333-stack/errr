package vn.cavemind.life;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

public final class LifeData extends SavedData {
    private static final String NAME = "cavemind_life";
    private static LifeData cached;
    private static MinecraftServer cachedFor;

    public double x, y, z;
    public boolean placed;
    public BlockPos lair;
    public String goal = "rest";
    public long goalSince;
    public UUID focus;
    public double curiosity = 30, hunger = 20, caution;
    public long lastNightStudied = -1;
    public int nightsAlive;
    public long lastDay = -1;
    public long clockRaw = Long.MIN_VALUE, clockOffset;
    public long beat;
    public long lastMan, lastDamned, lastHero, lastStage;
    public double longing, memory;
    public long restUntil;
    public double cx, cy, cz, lonely = 30;
    public boolean childPlaced;
    public long childRestUntil;
    public BlockPos childHome;
    public double bellHeat;
    public long bellDay = -1;
    public final List<Base> bases = new ArrayList<>();

    public static final class Base {
        public BlockPos center;
        public int radius = 20;
        public final Set<UUID> owners = new HashSet<>();
        public int study;
        public long lastStudyNight = -1;
        public int studyTicks;
        public int builds;
        public int prints, torchesTaken, insideVisits, sitIns;
        public BlockPos torchTrail;
        public long sitArmedDay = -1;
        public long lastDamnedNight = -1;
        public long torchNight = -2;

        CompoundTag save() {
            CompoundTag t = new CompoundTag();
            t.put("c", NbtUtils.writeBlockPos(center));
            t.putInt("r", radius);
            ListTag o = new ListTag();
            for (UUID u : owners) o.add(NbtUtils.createUUID(u));
            t.put("o", o);
            t.putInt("st", study);
            t.putLong("sn", lastStudyNight);
            t.putInt("stk", studyTicks);
            t.putInt("b", builds);
            t.putInt("pr", prints);
            t.putInt("tt", torchesTaken);
            t.putInt("iv", insideVisits);
            t.putInt("si", sitIns);
            if (torchTrail != null) t.put("tr", NbtUtils.writeBlockPos(torchTrail));
            t.putLong("sa", sitArmedDay);
            t.putLong("dn", lastDamnedNight);
            return t;
        }

        static Base load(CompoundTag t) {
            Base b = new Base();
            b.center = NbtUtils.readBlockPos(t.getCompound("c"));
            b.radius = t.getInt("r");
            for (Tag u : t.getList("o", Tag.TAG_INT_ARRAY)) b.owners.add(NbtUtils.loadUUID(u));
            b.study = t.getInt("st");
            b.lastStudyNight = t.getLong("sn");
            b.studyTicks = t.getInt("stk");
            b.builds = t.getInt("b");
            b.prints = t.getInt("pr");
            b.torchesTaken = t.getInt("tt");
            b.insideVisits = t.getInt("iv");
            b.sitIns = t.getInt("si");
            if (t.contains("tr")) b.torchTrail = NbtUtils.readBlockPos(t.getCompound("tr"));
            b.sitArmedDay = t.getLong("sa");
            b.lastDamnedNight = t.contains("dn") ? t.getLong("dn") : -1;
            return b;
        }
    }

    public static LifeData get(MinecraftServer server) {
        if (cached == null || cachedFor != server) {
            cached = server.overworld().getDataStorage().computeIfAbsent(LifeData::load, LifeData::new, NAME);
            cachedFor = server;
        }
        return cached;
    }

    public static void reset() {
        cached = null;
        cachedFor = null;
    }

    public BlockPos pos() {
        return BlockPos.containing(x, y, z);
    }

    public void at(double nx, double ny, double nz) {
        x = nx;
        y = ny;
        z = nz;
        placed = true;
        setDirty();
    }

    private static LifeData load(CompoundTag t) {
        LifeData d = new LifeData();
        d.x = t.getDouble("x");
        d.y = t.getDouble("y");
        d.z = t.getDouble("z");
        d.placed = t.getBoolean("placed");
        if (t.contains("lair")) d.lair = NbtUtils.readBlockPos(t.getCompound("lair"));
        d.goal = t.getString("goal");
        if (d.goal.isEmpty()) d.goal = "rest";
        d.goalSince = t.getLong("gs");
        if (t.hasUUID("focus")) d.focus = t.getUUID("focus");
        d.curiosity = t.getDouble("cur");
        d.hunger = t.getDouble("hun");
        d.caution = t.getDouble("cau");
        d.lastNightStudied = t.getLong("lns");
        d.nightsAlive = t.getInt("na");
        d.lastDay = t.contains("ld") ? t.getLong("ld") : -1;
        d.clockRaw = t.contains("cr") ? t.getLong("cr") : Long.MIN_VALUE;
        d.clockOffset = t.getLong("co");
        d.beat = t.getLong("beat");
        d.lastMan = t.getLong("lm");
        d.lastDamned = t.getLong("lda");
        d.lastHero = t.getLong("lh");
        d.lastStage = t.getLong("ls");
        d.longing = t.getDouble("lon");
        d.memory = t.getDouble("mem");
        d.restUntil = t.getLong("rest");
        d.cx = t.getDouble("cx");
        d.cy = t.getDouble("cy");
        d.cz = t.getDouble("cz");
        d.childPlaced = t.getBoolean("cp");
        d.lonely = t.contains("lonely") ? t.getDouble("lonely") : 30;
        d.childRestUntil = t.getLong("crest");
        if (t.contains("chome")) d.childHome = NbtUtils.readBlockPos(t.getCompound("chome"));
        d.bellHeat = t.getDouble("bh");
        d.bellDay = t.contains("bd") ? t.getLong("bd") : -1;
        for (Tag b : t.getList("bases", Tag.TAG_COMPOUND)) d.bases.add(Base.load((CompoundTag) b));
        return d;
    }

    @Override
    public CompoundTag save(CompoundTag t) {
        t.putDouble("x", x);
        t.putDouble("y", y);
        t.putDouble("z", z);
        t.putBoolean("placed", placed);
        if (lair != null) t.put("lair", NbtUtils.writeBlockPos(lair));
        t.putString("goal", goal);
        t.putLong("gs", goalSince);
        if (focus != null) t.putUUID("focus", focus);
        t.putDouble("cur", curiosity);
        t.putDouble("hun", hunger);
        t.putDouble("cau", caution);
        t.putLong("lns", lastNightStudied);
        t.putInt("na", nightsAlive);
        t.putLong("ld", lastDay);
        t.putLong("cr", clockRaw);
        t.putLong("co", clockOffset);
        t.putLong("beat", beat);
        t.putLong("lm", lastMan);
        t.putLong("lda", lastDamned);
        t.putLong("lh", lastHero);
        t.putLong("ls", lastStage);
        t.putDouble("lon", longing);
        t.putDouble("mem", memory);
        t.putLong("rest", restUntil);
        t.putDouble("cx", cx);
        t.putDouble("cy", cy);
        t.putDouble("cz", cz);
        t.putBoolean("cp", childPlaced);
        t.putDouble("lonely", lonely);
        t.putLong("crest", childRestUntil);
        if (childHome != null) t.put("chome", NbtUtils.writeBlockPos(childHome));
        t.putDouble("bh", bellHeat);
        t.putLong("bd", bellDay);
        t.putInt("ver", 4);
        ListTag bl = new ListTag();
        for (Base b : bases) bl.add(b.save());
        t.put("bases", bl);
        return t;
    }
}
