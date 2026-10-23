package apollyon.init;

import apollyon.client.renderer.TheDamnedRenderer;
import apollyon.client.renderer.TheFallenRenderer;
import apollyon.client.renderer.TheInfectorRenderer;
import net.minecraft.world.entity.EntityType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent.RegisterRenderers;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;

@EventBusSubscriber(
   bus = Bus.MOD,
   value = {Dist.CLIENT}
)
public class ApollyonModEntityRenderers {
   @SubscribeEvent
   public static void registerEntityRenderers(RegisterRenderers event) {
      event.registerEntityRenderer((EntityType)ApollyonModEntities.THE_DAMNED.get(), TheDamnedRenderer::new);
      event.registerEntityRenderer((EntityType)ApollyonModEntities.THE_INFECTOR.get(), TheInfectorRenderer::new);
      event.registerEntityRenderer((EntityType)ApollyonModEntities.THE_FALLEN.get(), TheFallenRenderer::new);
   }
}
