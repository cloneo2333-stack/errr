package vn.cavemind.chapter;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.entity.living.MobSpawnEvent;
import net.minecraftforge.registries.ForgeRegistries;
import vn.cavemind.CaveMind;
import vn.cavemind.Cfg;
import vn.cavemind.memory.MindData;

public final class Tiers {
    public static final String LOCKED = "cavemind_locked";
    private static Map<ResourceLocation, Integer> tiers;
    private static final Map<String, Integer> SPACES = new HashMap<>();
    private static volatile Map<ResourceKey<Level>, double[][]> seen = Map.of();
    private static long held;

    static synchronized Map<ResourceLocation, Integer> map() {
        if (tiers != null) return tiers;
        Map<ResourceLocation, Integer> m = new HashMap<>();
        try (InputStream in = Tiers.class.getResourceAsStream("/cavemind_tiers.json")) {
            if (in != null) {
                JsonObject o = JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
                for (Map.Entry<String, JsonElement> e : o.entrySet()) {
                    int t = Integer.parseInt(e.getKey());
                    for (JsonElement id : e.getValue().getAsJsonArray()) {
                        String k = id.getAsString();
                        if (k.endsWith(":*")) SPACES.put(k.substring(0, k.length() - 2), t);
                        else m.put(new ResourceLocation(k), t);
                    }
                }
            }
        } catch (Throwable t) {
            CaveMind.LOG.warn("[CaveMind] could not read chapter spawn tiers", t);
        }
        tiers = m;
        return m;
    }

    public static int tier(EntityType<?> type) {
        ResourceLocation id = ForgeRegistries.ENTITY_TYPES.getKey(type);
        if (id == null) return 1;
        Integer t = map().get(id);
        return t != null ? t : SPACES.getOrDefault(id.getNamespace(), 1);
    }

    public static List<EntityType<?>> of(int chapter) {
        List<EntityType<?>> out = new ArrayList<>();
        for (Map.Entry<ResourceLocation, Integer> e : map().entrySet()) {
            if (e.getValue() != chapter || !ForgeRegistries.ENTITY_TYPES.containsKey(e.getKey())) continue;
            out.add(ForgeRegistries.ENTITY_TYPES.getValue(e.getKey()));
        }
        return out;
    }

    public static void snapshot(MinecraftServer server) {
        Map<ResourceKey<Level>, List<double[]>> by = new HashMap<>();
        for (ServerPlayer p : server.getPlayerList().getPlayers()) {
            if (p.isSpectator()) continue;
            by.computeIfAbsent(p.level().dimension(), k -> new ArrayList<>()).add(new double[]{p.getX(), p.getZ(), MindData.profile(p).chapter});
        }
        Map<ResourceKey<Level>, double[][]> out = new HashMap<>();
        by.forEach((k, v) -> out.put(k, v.toArray(new double[0][])));
        seen = out;
    }

    public static int allowed(ResourceKey<Level> dim, double x, double z) {
        if (!Cfg.CHAPTERS.get() || !Cfg.CHAPTER_SPAWNS.get()) return 6;
        double[][] ps = seen.get(dim);
        if (ps == null) return 1;
        int best = 1;
        for (double[] p : ps) {
            double dx = p[0] - x, dz = p[1] - z;
            if (dx * dx + dz * dz <= 144 * 144) best = Math.max(best, (int) p[2]);
        }
        return best;
    }

    static boolean gated(MobSpawnType t) {
        return switch (t) {
            case NATURAL, CHUNK_GENERATION, MOB_SUMMONED, REINFORCEMENT, PATROL, JOCKEY, EVENT -> true;
            default -> false;
        };
    }

    static boolean zoned(Mob m, ServerLevel lvl, double x, double y, double z) {
        ResourceLocation id = ForgeRegistries.ENTITY_TYPES.getKey(m.getType());
        if (id == null || lvl.dimension() != Level.OVERWORLD) return true;
        net.minecraft.core.BlockPos at = net.minecraft.core.BlockPos.containing(x, y + 1, z);
        long tod = lvl.getDayTime() % 24000;
        boolean night = tod > 12800 && tod < 23200;
        return switch (id.getNamespace()) {
            case "anomaly" -> y < 8 && !lvl.canSeeSky(at);
            case "the_knocker" -> night && lvl.canSeeSky(at);
            case "cave_dweller" -> !lvl.canSeeSky(at) && y < 56;
            case "weeping_angels" -> lvl.canSeeSky(at) || y > 40 || m.getPersistentData().getBoolean("cavemind_guard");
            default -> true;
        };
    }

    public static void check(MobSpawnEvent.FinalizeSpawn e) {
        if (!Cfg.CHAPTERS.get() || !Cfg.CHAPTER_SPAWNS.get() || !gated(e.getSpawnType())) return;
        Mob m = e.getEntity();
        ServerLevel lvl = e.getLevel().getLevel();
        if (!zoned(m, lvl, e.getX(), e.getY(), e.getZ())) {
            e.setSpawnCancelled(true);
            m.getPersistentData().putBoolean(LOCKED, true);
            return;
        }
        int t = tier(m.getType());
        if (t <= 1) return;
        if (t <= allowed(lvl.dimension(), e.getX(), e.getZ())) return;
        e.setSpawnCancelled(true);
        m.getPersistentData().putBoolean(LOCKED, true);
        if (++held % 200 == 1) CaveMind.debug("chapter tiers held back {} (tier {}) at {} {} ({} so far)", ForgeRegistries.ENTITY_TYPES.getKey(m.getType()), t, (int) e.getX(), (int) e.getZ(), held);
    }

    private Tiers() {}
}
