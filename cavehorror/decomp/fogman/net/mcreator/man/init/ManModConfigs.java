package net.mcreator.man.init;

import net.mcreator.man.configuration.ManconfigConfiguration;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;
import net.minecraftforge.fml.config.ModConfig.Type;
import net.minecraftforge.fml.event.lifecycle.FMLConstructModEvent;

@EventBusSubscriber(
   modid = "man",
   bus = Bus.MOD
)
public class ManModConfigs {
   @SubscribeEvent
   public static void register(FMLConstructModEvent event) {
      event.enqueueWork(() -> ModLoadingContext.get().registerConfig(Type.COMMON, ManconfigConfiguration.SPEC, "man_config.toml"));
   }
}
