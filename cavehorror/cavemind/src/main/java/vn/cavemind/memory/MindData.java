package vn.cavemind.memory;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraftforge.server.ServerLifecycleHooks;

public final class MindData extends SavedData {
    private static final String NAME = "cavemind";
    private final Map<UUID, Profile> profiles = new HashMap<>();
    public int coop = -1;
    public int shared = 1;
    private static MindData cached;
    private static MinecraftServer cachedFor;

    public static MindData get(MinecraftServer server) {
        if (cached == null || cachedFor != server) {
            cached = server.overworld().getDataStorage().computeIfAbsent(MindData::load, MindData::new, NAME);
            cachedFor = server;
        }
        return cached;
    }

    public static Profile profile(ServerPlayer p) {
        MindData d = get(p.server);
        Profile pr = d.profiles.computeIfAbsent(p.getUUID(), Profile::new);
        pr.name = p.getGameProfile().getName();
        d.setDirty();
        return pr;
    }

    public static Profile profile(UUID id) {
        MinecraftServer s = ServerLifecycleHooks.getCurrentServer();
        if (s == null) return null;
        return get(s).profiles.get(id);
    }

    public static void dirty(MinecraftServer s) {
        get(s).setDirty();
    }

    public static void reset() {
        cached = null;
        cachedFor = null;
    }

    private static MindData load(CompoundTag t) {
        MindData d = new MindData();
        d.coop = t.contains("coop") ? t.getInt("coop") : -1;
        d.shared = Math.max(1, t.getInt("shared"));
        CompoundTag ps = t.getCompound("players");
        for (String k : ps.getAllKeys()) {
            try {
                UUID id = UUID.fromString(k);
                d.profiles.put(id, Profile.load(id, ps.getCompound(k)));
            } catch (IllegalArgumentException ignored) {
            }
        }
        return d;
    }

    @Override
    public CompoundTag save(CompoundTag t) {
        CompoundTag ps = new CompoundTag();
        for (Map.Entry<UUID, Profile> e : profiles.entrySet()) ps.put(e.getKey().toString(), e.getValue().save());
        t.put("players", ps);
        t.putInt("coop", coop);
        t.putInt("shared", shared);
        return t;
    }

    public Map<UUID, Profile> all() {
        return profiles;
    }
}
