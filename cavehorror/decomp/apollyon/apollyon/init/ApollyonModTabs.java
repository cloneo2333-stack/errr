package apollyon.init;

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
public class ApollyonModTabs {
   public static final DeferredRegister<CreativeModeTab> REGISTRY = DeferredRegister.create(Registries.f_279569_, "apollyon");

   @SubscribeEvent
   public static void buildTabContentsVanilla(BuildCreativeModeTabContentsEvent tabData) {
      if (tabData.getTabKey() == CreativeModeTabs.f_256731_) {
         tabData.m_246326_((ItemLike)ApollyonModItems.THE_DAMNED_SPAWN_EGG.get());
         tabData.m_246326_((ItemLike)ApollyonModItems.THE_INFECTOR_SPAWN_EGG.get());
         tabData.m_246326_((ItemLike)ApollyonModItems.THE_FALLEN_SPAWN_EGG.get());
      } else if (tabData.getTabKey() == CreativeModeTabs.f_256839_) {
         tabData.m_246326_((ItemLike)ApollyonModItems.SUSTENANCE.get());
         tabData.m_246326_((ItemLike)ApollyonModItems.STEW_OF_LIFE.get());
      } else if (tabData.getTabKey() == CreativeModeTabs.f_256776_) {
         tabData.m_246326_(((Block)ApollyonModBlocks.ROSE_OF_LIFE.get()).m_5456_());
      }
   }
}
