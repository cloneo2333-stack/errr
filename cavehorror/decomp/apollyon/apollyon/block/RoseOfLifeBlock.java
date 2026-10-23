package apollyon.block;

import apollyon.procedures.RoseOfLifeWhenBrokenProcedure;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.FlowerBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockBehaviour.OffsetType;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.common.util.ForgeSoundType;
import net.minecraftforge.registries.ForgeRegistries;

public class RoseOfLifeBlock extends FlowerBlock {
   public RoseOfLifeBlock() {
      super(
         () -> MobEffects.f_19616_,
         2400,
         Properties.m_284310_()
            .m_284180_(MapColor.f_283915_)
            .m_60918_(
               new ForgeSoundType(
                  1.0F,
                  1.0F,
                  () -> (SoundEvent)ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("apollyon:break_rose")),
                  () -> (SoundEvent)ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("block.grass.step")),
                  () -> (SoundEvent)ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("apollyon:place_rose")),
                  () -> (SoundEvent)ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("block.grass.hit")),
                  () -> (SoundEvent)ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("block.grass.fall"))
               )
            )
            .m_60966_()
            .m_60982_((bs, br, bp) -> true)
            .m_60991_((bs, br, bp) -> true)
            .m_60910_()
            .m_280170_()
            .m_222979_(OffsetType.XZ)
            .m_278166_(PushReaction.DESTROY)
      );
   }

   public int m_53522_() {
      return 2400;
   }

   public int getFlammability(BlockState state, BlockGetter world, BlockPos pos, Direction face) {
      return 100;
   }

   public int getFireSpreadSpeed(BlockState state, BlockGetter world, BlockPos pos, Direction face) {
      return 60;
   }

   public boolean onDestroyedByPlayer(BlockState blockstate, Level world, BlockPos pos, Player entity, boolean willHarvest, FluidState fluid) {
      boolean retval = super.onDestroyedByPlayer(blockstate, world, pos, entity, willHarvest, fluid);
      RoseOfLifeWhenBrokenProcedure.execute(world, (double)pos.m_123341_(), (double)pos.m_123342_(), (double)pos.m_123343_());
      return retval;
   }

   public void m_5581_(Level world, BlockState blockstate, BlockHitResult hit, Projectile entity) {
      RoseOfLifeWhenBrokenProcedure.execute(world, (double)hit.m_82425_().m_123341_(), (double)hit.m_82425_().m_123342_(), (double)hit.m_82425_().m_123343_());
   }
}
