package net.mcreator.man.procedures;

import javax.annotation.Nullable;
import net.mcreator.man.init.ManModEnchantments;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraftforge.event.TickEvent.Phase;
import net.minecraftforge.event.TickEvent.PlayerTickEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

@EventBusSubscriber
public class EntchantmentTickProcedure {
   @SubscribeEvent
   public static void onPlayerTick(PlayerTickEvent event) {
      if (event.phase == Phase.END) {
         execute(event, event.player);
      }
   }

   public static void execute(Entity entity) {
      execute(null, entity);
   }

   private static void execute(@Nullable Event event, Entity entity) {
      if (entity != null) {
         if (EnchantmentHelper.m_44843_(
               (Enchantment)ManModEnchantments.FOGSPEED.get(),
               entity instanceof LivingEntity _entGetArmor ? _entGetArmor.m_6844_(EquipmentSlot.FEET) : ItemStack.f_41583_
            )
            != 0) {
            ((LivingEntity)entity).m_21051_(Attributes.f_22279_).m_22100_(0.135);
         } else {
            ((LivingEntity)entity).m_21051_(Attributes.f_22279_).m_22100_(0.1);
         }
      }
   }
}
