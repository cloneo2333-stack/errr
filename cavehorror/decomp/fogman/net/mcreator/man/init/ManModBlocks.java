package net.mcreator.man.init;

import net.mcreator.man.block.CassetteRecorderBlock;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ManModBlocks {
   public static final DeferredRegister<Block> REGISTRY = DeferredRegister.create(ForgeRegistries.BLOCKS, "man");
   public static final RegistryObject<Block> CASSETTE_RECORDER_BLOCK = REGISTRY.register("cassette_recorder_block", () -> new CassetteRecorderBlock());
}
