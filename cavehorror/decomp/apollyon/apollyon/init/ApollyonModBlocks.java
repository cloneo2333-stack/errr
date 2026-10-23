package apollyon.init;

import apollyon.block.RoseOfLifeBlock;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ApollyonModBlocks {
   public static final DeferredRegister<Block> REGISTRY = DeferredRegister.create(ForgeRegistries.BLOCKS, "apollyon");
   public static final RegistryObject<Block> ROSE_OF_LIFE = REGISTRY.register("rose_of_life", () -> new RoseOfLifeBlock());
}
