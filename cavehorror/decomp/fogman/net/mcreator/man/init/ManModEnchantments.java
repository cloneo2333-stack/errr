package net.mcreator.man.init;

import net.mcreator.man.enchantment.FogProtectionEnchantment;
import net.mcreator.man.enchantment.FogspeedEnchantment;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ManModEnchantments {
   public static final DeferredRegister<Enchantment> REGISTRY = DeferredRegister.create(ForgeRegistries.ENCHANTMENTS, "man");
   public static final RegistryObject<Enchantment> FOGSPEED = REGISTRY.register("fogspeed", () -> new FogspeedEnchantment());
   public static final RegistryObject<Enchantment> FOG_PROTECTION = REGISTRY.register("fog_protection", () -> new FogProtectionEnchantment());
}
