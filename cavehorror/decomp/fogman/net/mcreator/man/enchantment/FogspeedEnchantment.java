package net.mcreator.man.enchantment;

import java.util.List;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentCategory;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.enchantment.Enchantment.Rarity;

public class FogspeedEnchantment extends Enchantment {
   public FogspeedEnchantment(EquipmentSlot... slots) {
      super(Rarity.VERY_RARE, EnchantmentCategory.ARMOR_FEET, slots);
   }

   protected boolean m_5975_(Enchantment ench) {
      return this != ench && !List.of(Enchantments.f_44976_, Enchantments.f_220304_).contains(ench);
   }

   public boolean m_6591_() {
      return true;
   }

   public boolean m_6592_() {
      return false;
   }

   public boolean m_6594_() {
      return false;
   }
}
