package apollyon.item;

import apollyon.procedures.StewOfLifeWhenConsumedProcedure;
import apollyon.procedures.StewOfLifeWhenCraftedProcedure;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodProperties.Builder;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.Item.Properties;
import net.minecraft.world.level.Level;

public class StewOfLifeItem extends Item {
   public StewOfLifeItem() {
      super(new Properties().m_41487_(1).m_41497_(Rarity.COMMON).m_41489_(new Builder().m_38760_(6).m_38758_(6.0F).m_38765_().m_38767_()));
   }

   public ItemStack m_5922_(ItemStack itemstack, Level world, LivingEntity entity) {
      ItemStack retval = new ItemStack(Items.f_42399_);
      super.m_5922_(itemstack, world, entity);
      double x = entity.m_20185_();
      double y = entity.m_20186_();
      double z = entity.m_20189_();
      StewOfLifeWhenConsumedProcedure.execute(world, x, y, z, entity);
      if (itemstack.m_41619_()) {
         return retval;
      } else {
         if (entity instanceof Player player && !player.m_150110_().f_35937_ && !player.m_150109_().m_36054_(retval)) {
            player.m_36176_(retval, false);
         }

         return itemstack;
      }
   }

   public void m_7836_(ItemStack itemstack, Level world, Player entity) {
      super.m_7836_(itemstack, world, entity);
      StewOfLifeWhenCraftedProcedure.execute(entity);
   }
}
