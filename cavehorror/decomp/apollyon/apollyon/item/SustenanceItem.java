package apollyon.item;

import apollyon.procedures.SustenanceWhenCreatedProcedure;
import apollyon.procedures.SustenanceWhenEatenProcedure;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodProperties.Builder;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.Item.Properties;
import net.minecraft.world.level.Level;

public class SustenanceItem extends Item {
   public SustenanceItem() {
      super(new Properties().m_41487_(64).m_41497_(Rarity.COMMON).m_41489_(new Builder().m_38760_(6).m_38758_(0.3F).m_38757_().m_38767_()));
   }

   public ItemStack m_5922_(ItemStack itemstack, Level world, LivingEntity entity) {
      ItemStack retval = super.m_5922_(itemstack, world, entity);
      double x = entity.m_20185_();
      double y = entity.m_20186_();
      double z = entity.m_20189_();
      SustenanceWhenEatenProcedure.execute(entity);
      return retval;
   }

   public void m_7836_(ItemStack itemstack, Level world, Player entity) {
      super.m_7836_(itemstack, world, entity);
      SustenanceWhenCreatedProcedure.execute(entity);
   }
}
