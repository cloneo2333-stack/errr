package vn.cavemind.eye;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.zip.GZIPOutputStream;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.registries.ForgeRegistries;
import vn.cavemind.CaveMind;
import vn.cavemind.Cfg;
import vn.cavemind.compat.Compat;
import vn.cavemind.director.Director;
import vn.cavemind.sense.Sight;
import vn.cavemind.sense.Watch;
import vn.cavemind.world.Motion;
import vn.cavemind.world.Spots;

public final class Eye {
    public static volatile Path dir;
    private static final java.util.regex.Pattern POS = java.util.regex.Pattern.compile("BlockPos[{]x=(-?[0-9]+), y=(-?[0-9]+), z=(-?[0-9]+)[}]");
    public static final java.util.Set<UUID> MANAGED = java.util.concurrent.ConcurrentHashMap.newKeySet();
    private static BufferedWriter out;
    private static long lastFlush;
    private static final Map<UUID, long[]> MAPS = new HashMap<>();
    private static final Map<UUID, Long> LAST_SHOT = new HashMap<>();
    private static int shots;
    private static final String[] SHOT_ON = {" begins ", " sees ", "arrives", "frenzy begins", "closes in", "noticed", " at BlockPos{", "left a sign", "lunge", "strike"};

    public static boolean on() {
        return dir != null;
    }

    public static boolean wanted() {
        return Cfg.EYE.get() || Cfg.DEV.get();
    }

    public static synchronized void open(MinecraftServer server) {
        close();
        if (!wanted()) return;
        try {
            Path d = FMLPaths.GAMEDIR.get().resolve("cavemind_eye").resolve(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss")));
            Files.createDirectories(d.resolve("screenshots"));
            out = new BufferedWriter(new OutputStreamWriter(new GZIPOutputStream(Files.newOutputStream(d.resolve("server.jsonl.gz")), 1 << 16, true), StandardCharsets.UTF_8));
            dir = d;
            shots = 0;
            JsonObject o = new JsonObject();
            o.addProperty("k", "start");
            o.addProperty("mod", net.minecraftforge.fml.ModList.get().getModFileById(CaveMind.MODID).getFile().getFileName());
            o.addProperty("dev", Cfg.DEV.get());
            o.addProperty("world", server.getWorldData().getLevelName());
            o.addProperty("when", LocalDateTime.now().toString());
            write(o);
            CaveMind.LOG.info("[CaveMind] eye recording to {}", d);
        } catch (IOException ex) {
            CaveMind.LOG.warn("[CaveMind] eye could not open", ex);
            dir = null;
            out = null;
        }
    }

    public static synchronized void close() {
        if (out != null) {
            try {
                out.close();
            } catch (IOException ignored) {
            }
        }
        out = null;
        dir = null;
        MAPS.clear();
        LAST_SHOT.clear();
        MANAGED.clear();
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> EyeClient::close);
    }

    private static synchronized void write(JsonObject o) {
        if (out == null) return;
        try {
            out.write(o.toString());
            out.write('\n');
            long now = System.currentTimeMillis();
            if (now - lastFlush > 4000) {
                out.flush();
                lastFlush = now;
            }
        } catch (IOException ex) {
            CaveMind.LOG.warn("[CaveMind] eye write failed, stopping", ex);
            close();
        }
    }

    public static void event(String line) {
        if (!on()) return;
        MinecraftServer srv = net.minecraftforge.server.ServerLifecycleHooks.getCurrentServer();
        JsonObject o = new JsonObject();
        o.addProperty("k", "ev");
        if (srv != null) {
            o.addProperty("t", srv.getTickCount());
            o.addProperty("g", srv.overworld().getGameTime());
        }
        o.addProperty("s", line);
        if (srv != null) {
            java.util.regex.Matcher m = POS.matcher(line);
            if (m.find()) {
                BlockPos q = new BlockPos(Integer.parseInt(m.group(1)), Integer.parseInt(m.group(2)), Integer.parseInt(m.group(3)));
                JsonObject seen = new JsonObject();
                for (ServerPlayer p : srv.getPlayerList().getPlayers()) {
                    if (p.level() != srv.overworld() && !line.contains(p.getGameProfile().getName())) continue;
                    Vec3 c = Vec3.atCenterOf(q);
                    Sight si = Sight.of(p, Watch.of(p), c);
                    JsonObject so = new JsonObject();
                    if (si.on) so.addProperty("on", true);
                    if (Sight.clear(p.level(), p.getEyePosition(), c, p) || Sight.clear(p.level(), p.getEyePosition(), c.add(0, 1, 0), p)) so.addProperty("clear", true);
                    so.addProperty("d", r1(Math.sqrt(p.distanceToSqr(c))));
                    seen.add(p.getGameProfile().getName(), so);
                }
                o.add("at", seen);
            }
        }
        write(o);
        if (srv == null) return;
        for (String k : SHOT_ON) {
            if (line.contains(k)) {
                for (ServerPlayer p : srv.getPlayerList().getPlayers()) {
                    if (line.contains(p.getGameProfile().getName())) shot(p, k.trim().replace("BlockPos{", "spawn"), false);
                }
                break;
            }
        }
    }

    public static void say(ServerPlayer p, String text) {
        if (!on()) return;
        JsonObject o = new JsonObject();
        o.addProperty("k", "say");
        o.addProperty("t", p.server.getTickCount());
        o.addProperty("g", p.serverLevel().getGameTime());
        o.addProperty("p", p.getGameProfile().getName());
        o.addProperty("s", text);
        write(o);
    }

    public static void mark(ServerPlayer p, String text) {
        JsonObject o = new JsonObject();
        o.addProperty("k", "mark");
        o.addProperty("t", p.server.getTickCount());
        o.addProperty("g", p.serverLevel().getGameTime());
        o.addProperty("p", p.getGameProfile().getName());
        o.addProperty("s", text);
        write(o);
        shot(p, "mark", true);
    }

    static void shot(ServerPlayer p, String tag, boolean force) {
        long now = p.server.getTickCount();
        Long last = LAST_SHOT.get(p.getUUID());
        if (!force && (shots >= 600 || last != null && now - last < 30)) return;
        LAST_SHOT.put(p.getUUID(), now);
        shots++;
        String name = now + "_" + tag.replaceAll("[^A-Za-z0-9]+", "_") + ".png";
        JsonObject o = new JsonObject();
        o.addProperty("k", "shot");
        o.addProperty("t", now);
        o.addProperty("g", p.serverLevel().getGameTime());
        o.addProperty("p", p.getGameProfile().getName());
        o.addProperty("f", name);
        o.addProperty("tag", tag);
        write(o);
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> EyeClient.request(name));
    }

    public static void tick(ServerPlayer p, Watch w, long now) {
        if (!on()) return;
        ServerLevel lvl = p.serverLevel();
        List<Entity> ents = lvl.getEntitiesOfClass(Entity.class, p.getBoundingBox().inflate(160, 96, 160), Compat::managed);
        JsonObject act = Director.eye(p);
        JsonObject man = vn.cavemind.man.ManMind.eye(p);
        JsonObject dmn = vn.cavemind.damned.Haunt.eye(p);
        JsonObject hero = vn.cavemind.hero.Herobrine.eye(p, w);
        JsonObject day = vn.cavemind.day.Daylight.eye(p);
        boolean busy = !ents.isEmpty() || act.has("act") || man != null || dmn != null || hero != null || day != null;
        if (now % (busy ? 2 : 20) != 0) return;
        JsonObject o = new JsonObject();
        o.addProperty("k", "f");
        o.addProperty("t", now);
        o.addProperty("g", lvl.getGameTime());
        o.addProperty("tod", lvl.getDayTime() % 24000);
        o.addProperty("p", p.getGameProfile().getName());
        o.add("pos", vec(p.position()));
        o.addProperty("yr", r1(p.getYRot()));
        o.addProperty("xr", r1(p.getXRot()));
        o.addProperty("fov", w.fov);
        o.addProperty("asp", r2(w.aspect));
        o.addProperty("spd", r2(w.speed));
        o.addProperty("env", (w.outside ? "o" : "") + (w.indoors ? "i" : "") + (w.underground ? "u" : "") + (w.confined ? "c" : ""));
        o.addProperty("li", w.light);
        o.addProperty("hp", r1(p.getHealth()));
        if (Compat.exempt(p)) o.addProperty("ex", true);
        if (p.isSleeping()) o.addProperty("sl", true);
        if (p.containerMenu != p.inventoryMenu) o.addProperty("menu", true);
        o.addProperty("rain", lvl.isRaining());
        o.add("dir", act);
        if (man != null) o.add("man", man);
        if (dmn != null) o.add("dmn", dmn);
        if (hero != null) o.add("hero", hero);
        if (day != null) o.add("day", day);
        if (!ents.isEmpty()) {
            JsonArray arr = new JsonArray();
            boolean seen = false;
            for (Entity e : ents) {
                MANAGED.add(e.getUUID());
                JsonObject eo = entity(p, w, e);
                if (eo.has("on") && eo.get("vis").getAsDouble() >= 0.4) seen = true;
                arr.add(eo);
            }
            o.add("ents", arr);
            if (seen && now % 600 == 0) shot(p, "seen", false);
        }
        write(o);
        map(p, w, now);
    }

    private static JsonObject entity(ServerPlayer p, Watch w, Entity e) {
        JsonObject o = new JsonObject();
        o.addProperty("id", e.getUUID().toString().substring(0, 8));
        o.addProperty("r", Compat.role(e));
        ResourceLocation id = ForgeRegistries.ENTITY_TYPES.getKey(e.getType());
        o.addProperty("ty", id == null ? "?" : id.getPath());
        o.add("pos", vec(e.position()));
        o.addProperty("yr", r1(e.getYRot()));
        o.addProperty("hy", r1(e.getYHeadRot()));
        o.addProperty("h", r2(e.getBbHeight()));
        String an = Motion.current(e);
        if (!an.isEmpty()) o.addProperty("an", an);
        Sight si = Sight.of(p, w, e.getBoundingBox().getCenter());
        double vis = Sight.visible(p, e);
        if (si.on) o.addProperty("on", true);
        o.addProperty("sx", r2(si.sx));
        o.addProperty("sy", r2(si.sy));
        o.addProperty("vis", r2(vis));
        o.addProperty("d", r1(Math.sqrt(e.distanceToSqr(p))));
        if (e.onGround()) o.addProperty("gnd", true);
        if (e.isInWall()) o.addProperty("wall", true);
        if (e.isInWater()) o.addProperty("wat", true);
        if (e.isInLava()) o.addProperty("lava", true);
        BlockPos feet = e.blockPosition();
        if (!Spots.standable(p.serverLevel(), feet, Math.max(1, (int) Math.ceil(e.getBbHeight() * 0.9)))) o.addProperty("bad", true);
        if (e instanceof Mob m && m.getNavigation().isInProgress()) o.addProperty("nav", true);
        if (e.getPersistentData().hasUUID(Compat.OWNER) && !e.getPersistentData().getUUID(Compat.OWNER).equals(p.getUUID())) o.addProperty("other", true);
        return o;
    }

    private static void map(ServerPlayer p, Watch w, long now) {
        long[] last = MAPS.get(p.getUUID());
        BlockPos at = p.blockPosition();
        if (last != null && now - last[0] < 1200 && Math.abs(at.getX() - last[1]) < 24 && Math.abs(at.getY() - last[2]) < 6 && Math.abs(at.getZ() - last[3]) < 24) return;
        if (last != null && now - last[0] < 100) return;
        MAPS.put(p.getUUID(), new long[]{now, at.getX(), at.getY(), at.getZ()});
        ServerLevel lvl = p.serverLevel();
        int R = 80, N = R * 2 + 1;
        byte[] col = new byte[N * N * 3];
        byte[] hgt = new byte[N * N * 2];
        byte[] cave = new byte[N * N];
        BlockPos.MutableBlockPos q = new BlockPos.MutableBlockPos();
        int py = at.getY();
        for (int dz = -R; dz <= R; dz++) {
            for (int dx = -R; dx <= R; dx++) {
                int x = at.getX() + dx, z = at.getZ() + dz, i = (dz + R) * N + (dx + R);
                if (!lvl.hasChunkAt(q.set(x, py, z))) {
                    hgt[i * 2] = (byte) 0x80;
                    continue;
                }
                int y = lvl.getHeight(Heightmap.Types.WORLD_SURFACE, x, z) - 1;
                BlockState st = lvl.getBlockState(q.set(x, y, z));
                int c = st.getMapColor(lvl, q).col;
                col[i * 3] = (byte) (c >> 16);
                col[i * 3 + 1] = (byte) (c >> 8);
                col[i * 3 + 2] = (byte) c;
                int rel = Math.max(-32767, Math.min(32767, y - py));
                hgt[i * 2] = (byte) (rel >> 8);
                hgt[i * 2 + 1] = (byte) rel;
                boolean f = lvl.getBlockState(q.set(x, py, z)).getCollisionShape(lvl, q).isEmpty();
                boolean h = lvl.getBlockState(q.set(x, py + 1, z)).getCollisionShape(lvl, q).isEmpty();
                boolean g = !lvl.getBlockState(q.set(x, py - 1, z)).getCollisionShape(lvl, q).isEmpty();
                cave[i] = (byte) ((f ? 1 : 0) | (h ? 2 : 0) | (g ? 4 : 0));
            }
        }
        JsonObject o = new JsonObject();
        o.addProperty("k", "map");
        o.addProperty("t", now);
        o.addProperty("g", lvl.getGameTime());
        o.addProperty("p", p.getGameProfile().getName());
        o.addProperty("cx", at.getX());
        o.addProperty("cy", py);
        o.addProperty("cz", at.getZ());
        o.addProperty("r", R);
        Base64.Encoder b64 = Base64.getEncoder();
        o.addProperty("col", b64.encodeToString(col));
        o.addProperty("hgt", b64.encodeToString(hgt));
        o.addProperty("cave", b64.encodeToString(cave));
        write(o);
    }

    public static JsonArray vec(Vec3 v) {
        JsonArray a = new JsonArray();
        a.add(r2(v.x));
        a.add(r2(v.y));
        a.add(r2(v.z));
        return a;
    }

    static double r1(double v) {
        return Math.round(v * 10) / 10.0;
    }

    static double r2(double v) {
        return Math.round(v * 100) / 100.0;
    }

    private Eye() {}
}
