package vn.cavemind.client;

import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.HitResult;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import vn.cavemind.CaveMind;
import vn.cavemind.net.Net;

// A swing at the air never reaches the server on its own; the Man needs to know when a heavy weapon was swung and missed.
@Mod.EventBusSubscriber(modid = CaveMind.MODID, value = Dist.CLIENT)
public final class SwingClient {
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void attack(InputEvent.InteractionKeyMappingTriggered ev) {
        if (ev.isCanceled() || !ev.isAttack()) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.hitResult != null && mc.hitResult.getType() != HitResult.Type.MISS) return;
        if (mc.player.getAttackStrengthScale(0f) < 0.9f || !BookClient.online()) return;
        Net.CH.sendToServer(new Net.Ask(Net.Ask.SWING, 0));
    }

    private SwingClient() {}
}
