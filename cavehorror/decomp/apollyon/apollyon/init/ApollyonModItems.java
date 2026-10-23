package apollyon.init;

import apollyon.item.StewOfLifeItem;
import apollyon.item.SustenanceItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Item.Properties;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.common.ForgeSpawnEggItem;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ApollyonModItems {
   public static final DeferredRegister<Item> REGISTRY = DeferredRegister.create(ForgeRegistries.ITEMS, "apollyon");
   public static final RegistryObject<Item> THE_DAMNED_SPAWN_EGG = REGISTRY.register(
      "the_damned_spawn_egg", () -> new ForgeSpawnEggItem(ApollyonModEntities.THE_DAMNED, -16777216, -3355444, new Properties())
   );
   public static final RegistryObject<Item> THE_INFECTOR_SPAWN_EGG = REGISTRY.register(
      "the_infector_spawn_egg", () -> new ForgeSpawnEggItem(ApollyonModEntities.THE_INFECTOR, -13096680, -1, new Properties())
   );
   public static final RegistryObject<Item> THE_FALLEN_SPAWN_EGG = REGISTRY.register(
      "the_fallen_spawn_egg", () -> new ForgeSpawnEggItem(ApollyonModEntities.THE_FALLEN, -16777216, -10066330, new Properties())
   );
   public static final RegistryObject<Item> SUSTENANCE = REGISTRY.register("sustenance", () -> new SustenanceItem());
   public static final RegistryObject<Item> ROSE_OF_LIFE = block(ApollyonModBlocks.ROSE_OF_LIFE);
   public static final RegistryObject<Item> STEW_OF_LIFE = REGISTRY.register("stew_of_life", () -> new StewOfLifeItem());

   private static RegistryObject<Item> block(RegistryObject<Block> block) {
      return REGISTRY.register(block.getId().m_135815_(), () -> new BlockItem((Block)block.get(), new Properties()));
   }
}
