package net.mcreator.man.init;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;
import net.minecraftforge.registries.DeferredRegister;

@EventBusSubscriber(
   bus = Bus.MOD
)
public class ManModTabs {
   public static final DeferredRegister<CreativeModeTab> REGISTRY = DeferredRegister.create(Registries.f_279569_, "man");

   @SubscribeEvent
   public static void buildTabContentsVanilla(BuildCreativeModeTabContentsEvent tabData) {
      if (tabData.getTabKey() == CreativeModeTabs.f_256791_) {
         tabData.m_246326_(((Block)ManModBlocks.CASSETTE_RECORDER_BLOCK.get()).m_5456_());
      } else if (tabData.getTabKey() == CreativeModeTabs.f_256731_) {
         tabData.m_246326_((ItemLike)ManModItems.MANFROMTHEFOG_SPAWN_EGG.get());
         tabData.m_246326_((ItemLike)ManModItems.MANFROMTHEFOGBACK_SPAWN_EGG.get());
         tabData.m_246326_((ItemLike)ManModItems.MFTFHANG_SPAWN_EGG.get());
      } else if (tabData.getTabKey() == CreativeModeTabs.f_256869_) {
         tabData.m_246326_((ItemLike)ManModItems.TEAROFTHEMAN.get());
         tabData.m_246326_((ItemLike)ManModItems.CASSETTE_1SAFETYINSTRUCTIONS.get());
      }
   }
}
