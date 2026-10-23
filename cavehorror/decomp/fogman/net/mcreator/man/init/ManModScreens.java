package net.mcreator.man.init;

import net.mcreator.man.client.gui.CassetteRecorderGuiScreen;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

@EventBusSubscriber(
   bus = Bus.MOD,
   value = {Dist.CLIENT}
)
public class ManModScreens {
   @SubscribeEvent
   public static void clientLoad(FMLClientSetupEvent event) {
      event.enqueueWork(() -> MenuScreens.m_96206_((MenuType)ManModMenus.CASSETTE_RECORDER_GUI.get(), CassetteRecorderGuiScreen::new));
   }
}
