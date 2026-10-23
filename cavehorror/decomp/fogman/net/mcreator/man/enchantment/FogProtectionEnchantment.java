package net.mcreator.man.enchantment;

import java.util.List;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentCategory;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.enchantment.Enchantment.Rarity;

public class FogProtectionEnchantment extends Enchantment {
   public FogProtectionEnchantment(EquipmentSlot... slots) {
      super(Rarity.VERY_RARE, EnchantmentCategory.ARMOR, slots);
   }

   public int m_7205_(int level, DamageSource source) {
      return level * 6;
   }

   protected boolean m_5975_(Enchantment ench) {
      return this != ench && !List.of(Enchantments.f_44965_).contains(ench);
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
