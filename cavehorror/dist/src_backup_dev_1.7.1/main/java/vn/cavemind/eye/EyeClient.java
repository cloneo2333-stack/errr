package vn.cavemind.eye;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.zip.GZIPOutputStream;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLivingEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import vn.cavemind.CaveMind;
import vn.cavemind.client.HeroEntity;

@Mod.EventBusSubscriber(modid = CaveMind.MODID, value = Dist.CLIENT)
public final class EyeClient {
    private static final ConcurrentLinkedQueue<String> SHOTS = new ConcurrentLinkedQueue<>();
    private static final Set<String> RENDERED = new HashSet<>();
    private static final Set<String> PRESENT = new HashSet<>();
    private static final Set<String> HEROES = new HashSet<>();
    private static Path openFor;
    private static BufferedWriter out;
    private static long lastFlush;

    public static void request(String name) {
        SHOTS.add(name);
    }

    public static synchronized void close() {
        if (out != null) {
            try {
                out.close();
            } catch (IOException ignored) {
            }
        }
        out = null;
        openFor = null;
        SHOTS.clear();
    }

    @SubscribeEvent
    public static void rendered(RenderLivingEvent.Post<?, ?> e) {
        if (Eye.dir == null) return;
        Entity ent = e.getEntity();
        if (ent instanceof HeroEntity) {
            HEROES.add(Eye.r2(ent.getX()) + "," + Eye.r2(ent.getY()) + "," + Eye.r2(ent.getZ()));
            return;
        }
    }

    @SubscribeEvent
    public static void stage(net.minecraftforge.client.event.RenderLevelStageEvent e) {
        if (Eye.dir == null || e.getStage() != net.minecraftforge.client.event.RenderLevelStageEvent.Stage.AFTER_ENTITIES) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || Eye.MANAGED.isEmpty()) return;
        for (Entity ent : mc.level.entitiesForRendering()) {
            if (!Eye.MANAGED.contains(ent.getUUID())) continue;
            String id = ent.getUUID().toString().substring(0, 8);
            PRESENT.add(id);
            if (!ent.isInvisible() && e.getFrustum().isVisible(ent.getBoundingBoxForCulling())) RENDERED.add(id);
        }
    }

    @SubscribeEvent
    public static void render(TickEvent.RenderTickEvent e) {
        if (e.phase != TickEvent.Phase.END) return;
        String name = SHOTS.poll();
        if (name == null) return;
        Path d = Eye.dir;
        Minecraft mc = Minecraft.getInstance();
        if (d == null || mc.level == null) return;
        try {
            Screenshot.grab(d.toFile(), name, mc.getMainRenderTarget(), msg -> {});
        } catch (Throwable t) {
            CaveMind.LOG.warn("[CaveMind] eye screenshot failed", t);
        }
    }

    @SubscribeEvent
    public static void tick(TickEvent.ClientTickEvent e) {
        if (e.phase != TickEvent.Phase.END) return;
        Path d = Eye.dir;
        Minecraft mc = Minecraft.getInstance();
        if (d == null || mc.level == null || mc.player == null) {
            RENDERED.clear();
            PRESENT.clear();
            HEROES.clear();
            return;
        }
        if (!d.equals(openFor)) open(d);
        if (out == null) return;
        Camera cam = mc.gameRenderer.getMainCamera();
        JsonObject o = new JsonObject();
        o.addProperty("k", "c");
        o.addProperty("g", mc.level.getGameTime());
        o.add("cam", Eye.vec(cam.getPosition()));
        o.addProperty("yr", Eye.r1(cam.getYRot()));
        o.addProperty("xr", Eye.r1(cam.getXRot()));
        o.addProperty("fov", mc.options.fov().get());
        o.addProperty("w", mc.getWindow().getWidth());
        o.addProperty("h", mc.getWindow().getHeight());
        if (!mc.options.getCameraType().isFirstPerson()) o.addProperty("third", true);
        if (mc.screen != null) o.addProperty("scr", mc.screen.getClass().getSimpleName());
        if (mc.isPaused()) o.addProperty("pause", true);
        if (!RENDERED.isEmpty()) {
            JsonArray a = new JsonArray();
            for (String s : RENDERED) a.add(s);
            o.add("r", a);
        }
        if (!PRESENT.isEmpty()) {
            JsonArray a = new JsonArray();
            for (String s : PRESENT) a.add(s);
            o.add("pr", a);
        }
        if (!HEROES.isEmpty()) {
            JsonArray a = new JsonArray();
            for (String s : HEROES) a.add(s);
            o.add("hero", a);
        }
        RENDERED.clear();
        PRESENT.clear();
        HEROES.clear();
        write(o);
    }

    private static synchronized void open(Path d) {
        close();
        try {
            out = new BufferedWriter(new OutputStreamWriter(new GZIPOutputStream(Files.newOutputStream(d.resolve("client.jsonl.gz")), 1 << 16, true), StandardCharsets.UTF_8));
            openFor = d;
        } catch (IOException ex) {
            CaveMind.LOG.warn("[CaveMind] eye client could not open", ex);
            out = null;
            openFor = d;
        }
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
            close();
        }
    }

    private EyeClient() {}
}
