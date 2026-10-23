package net.mcreator.man.init;

import net.mcreator.man.item.Cassette1safetyinstructionsItem;
import net.mcreator.man.item.TearofthemanItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Item.Properties;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.common.ForgeSpawnEggItem;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ManModItems {
   public static final DeferredRegister<Item> REGISTRY = DeferredRegister.create(ForgeRegistries.ITEMS, "man");
   public static final RegistryObject<Item> MANFROMTHEFOG_SPAWN_EGG = REGISTRY.register(
      "manfromthefog_spawn_egg", () -> new ForgeSpawnEggItem(ManModEntities.MANFROMTHEFOG, -6731008, -10936320, new Properties())
   );
   public static final RegistryObject<Item> TEAROFTHEMAN = REGISTRY.register("tearoftheman", () -> new TearofthemanItem());
   public static final RegistryObject<Item> MANFROMTHEFOGBACK_SPAWN_EGG = REGISTRY.register(
      "manfromthefogback_spawn_egg", () -> new ForgeSpawnEggItem(ManModEntities.MANFROMTHEFOGBACK, -6731008, -10936320, new Properties())
   );
   public static final RegistryObject<Item> CASSETTE_1SAFETYINSTRUCTIONS = REGISTRY.register(
      "cassette_1safetyinstructions", () -> new Cassette1safetyinstructionsItem()
   );
   public static final RegistryObject<Item> CASSETTE_RECORDER_BLOCK = block(ManModBlocks.CASSETTE_RECORDER_BLOCK);
   public static final RegistryObject<Item> MFTFHANG_SPAWN_EGG = REGISTRY.register(
      "mftfhang_spawn_egg", () -> new ForgeSpawnEggItem(ManModEntities.MFTFHANG, -6731008, -10936320, new Properties())
   );

   private static RegistryObject<Item> block(RegistryObject<Block> block) {
      return REGISTRY.register(block.getId().m_135815_(), () -> new BlockItem((Block)block.get(), new Properties()));
   }
}
