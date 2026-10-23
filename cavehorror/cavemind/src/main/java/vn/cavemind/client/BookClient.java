package vn.cavemind.client;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;
import vn.cavemind.CaveMind;
import vn.cavemind.net.Net;

@Mod.EventBusSubscriber(modid = CaveMind.MODID, value = Dist.CLIENT)
public final class BookClient {
    public static final KeyMapping KEY = new KeyMapping("key.cavemind.book", KeyConflictContext.IN_GAME, InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_J, "key.categories.cavemind");
    static int lastPage = -1;
    static boolean pendingMode;

    public static void on(Net.Book b) {
        Minecraft mc = Minecraft.getInstance();
        switch (b.kind()) {
            case Net.Book.OPEN -> {
                JsonObject o;
                try {
                    o = JsonParser.parseString(b.json()).getAsJsonObject();
                } catch (Exception ex) {
                    CaveMind.LOG.warn("[CaveMind] bad book data", ex);
                    return;
                }
                if (mc.screen instanceof BookScreen bs) bs.refresh(o);
                else mc.setScreen(new BookScreen(o, lastPage));
            }
            case Net.Book.UPDATE -> {
                if (!(mc.screen instanceof BookScreen bs)) return;
                try {
                    bs.refresh(JsonParser.parseString(b.json()).getAsJsonObject());
                } catch (Exception ex) {
                    CaveMind.LOG.warn("[CaveMind] bad book data", ex);
                }
            }
            case Net.Book.MODE -> {
                if (mc.screen == null || mc.screen instanceof BookScreen) mc.setScreen(new ModeScreen());
                else pendingMode = true;
            }
            default -> {}
        }
    }

    static boolean online() {
        Minecraft mc = Minecraft.getInstance();
        return mc.getConnection() != null && Net.CH.isRemotePresent(mc.getConnection().getConnection());
    }

    static void ask(int op, int arg) {
        if (online()) Net.CH.sendToServer(new Net.Ask(op, arg));
    }

    @SubscribeEvent
    public static void tick(TickEvent.ClientTickEvent e) {
        if (e.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            pendingMode = false;
            return;
        }
        if (pendingMode && mc.screen == null) {
            pendingMode = false;
            mc.setScreen(new ModeScreen());
        }
        while (KEY.consumeClick()) if (mc.screen == null) ask(Net.Ask.OPEN, -1);
    }

    private BookClient() {}
}
