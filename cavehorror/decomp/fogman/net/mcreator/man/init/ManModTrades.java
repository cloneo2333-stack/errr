package net.mcreator.man.init;

import java.util.List;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.common.BasicItemListing;
import net.minecraftforge.event.village.VillagerTradesEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;

@EventBusSubscriber(
   bus = Bus.FORGE
)
public class ManModTrades {
   @SubscribeEvent
   public static void registerTrades(VillagerTradesEvent event) {
      if (event.getType() == ManModVillagerProfessions.CULTIST.get()) {
         ((List)event.getTrades().get(1)).add(new BasicItemListing(new ItemStack(Items.f_42616_, 2), new ItemStack(Blocks.f_50737_), 10, 1, 0.05F));
         ((List)event.getTrades().get(1))
            .add(new BasicItemListing(new ItemStack(Items.f_42616_, 8), new ItemStack((ItemLike)ManModBlocks.CASSETTE_RECORDER_BLOCK.get()), 1, 5, 0.07F));
         ((List)event.getTrades().get(2))
            .add(new BasicItemListing(new ItemStack(Items.f_42616_, 5), new ItemStack(Items.f_42410_), new ItemStack(Items.f_42436_), 20, 10, 0.1F));
         ((List)event.getTrades().get(3)).add(new BasicItemListing(new ItemStack(Items.f_42616_, 24), new ItemStack(Items.f_42678_), 7, 20, 0.05F));
         ((List)event.getTrades().get(3)).add(new BasicItemListing(new ItemStack(Items.f_42616_, 24), new ItemStack(Items.f_42680_), 7, 20, 0.05F));
         ((List)event.getTrades().get(4))
            .add(new BasicItemListing(new ItemStack(Items.f_42616_, 40), new ItemStack(Items.f_42590_), new ItemStack(Items.f_42735_), 5, 30, 0.05F));
         ((List)event.getTrades().get(4)).add(new BasicItemListing(new ItemStack(Items.f_42616_, 30), new ItemStack(Blocks.f_50070_), 5, 25, 0.05F));
         ((List)event.getTrades().get(5)).add(new BasicItemListing(new ItemStack(Items.f_42616_, 64), new ItemStack(Blocks.f_50729_), 5, 6, 0.05F));
         ((List)event.getTrades().get(4))
            .add(new BasicItemListing(new ItemStack(Items.f_42616_, 50), new ItemStack(Items.f_42417_, 9), new ItemStack(Items.f_42747_), 3, 35, 0.15F));
         ((List)event.getTrades().get(5))
            .add(new BasicItemListing(new ItemStack(Items.f_42616_, 64), new ItemStack(Items.f_42714_, 10), new ItemStack(Items.f_42741_), 1, 5, 0.05F));
      }
   }
}
