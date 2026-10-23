package net.mcreator.man.init;

import net.mcreator.man.block.entity.CassetteRecorderBlockEntity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.BlockEntityType.BlockEntitySupplier;
import net.minecraft.world.level.block.entity.BlockEntityType.Builder;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ManModBlockEntities {
   public static final DeferredRegister<BlockEntityType<?>> REGISTRY = DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, "man");
   public static final RegistryObject<BlockEntityType<?>> CASSETTE_RECORDER_BLOCK = register(
      "cassette_recorder_block", ManModBlocks.CASSETTE_RECORDER_BLOCK, CassetteRecorderBlockEntity::new
   );

   private static RegistryObject<BlockEntityType<?>> register(String registryname, RegistryObject<Block> block, BlockEntitySupplier<?> supplier) {
      return REGISTRY.register(registryname, () -> Builder.m_155273_(supplier, new Block[]{(Block)block.get()}).m_58966_(null));
   }
}
