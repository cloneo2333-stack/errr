package net.mcreator.man.init;

import net.mcreator.man.client.renderer.ManaggresiveRenderer;
import net.mcreator.man.client.renderer.ManfromthefogRenderer;
import net.mcreator.man.client.renderer.ManfromthefogbackRenderer;
import net.mcreator.man.client.renderer.MftfhangRenderer;
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
public class ManModEntityRenderers {
   @SubscribeEvent
   public static void registerEntityRenderers(RegisterRenderers event) {
      event.registerEntityRenderer((EntityType)ManModEntities.MANFROMTHEFOG.get(), ManfromthefogRenderer::new);
      event.registerEntityRenderer((EntityType)ManModEntities.MANAGGRESIVE.get(), ManaggresiveRenderer::new);
      event.registerEntityRenderer((EntityType)ManModEntities.MANFROMTHEFOGBACK.get(), ManfromthefogbackRenderer::new);
      event.registerEntityRenderer((EntityType)ManModEntities.MFTFHANG.get(), MftfhangRenderer::new);
   }
}
