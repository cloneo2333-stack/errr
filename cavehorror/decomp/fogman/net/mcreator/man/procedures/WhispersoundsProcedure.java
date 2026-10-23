package net.mcreator.man.procedures;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraftforge.registries.ForgeRegistries;

public class WhispersoundsProcedure {
   public static void execute(LevelAccessor world, double x, double y, double z) {
      double random_number = 0.0;
      random_number = (double)Math.round(Mth.m_216263_(RandomSource.m_216327_(), 1.0, 2.0));
      if (random_number == 1.0) {
         if (world instanceof Level _level) {
            if (!_level.m_5776_()) {
               _level.m_5594_(
                  null,
                  BlockPos.m_274561_(x, y, z),
                  (SoundEvent)ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("man:short_whisper_1")),
                  SoundSource.AMBIENT,
                  1.0F,
                  1.0F
               );
            } else {
               _level.m_7785_(
                  x,
                  y,
                  z,
                  (SoundEvent)ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("man:short_whisper_1")),
                  SoundSource.AMBIENT,
                  1.0F,
                  1.0F,
                  false
               );
            }
         }
      } else if (random_number == 2.0 && world instanceof Level _levelx) {
         if (!_levelx.m_5776_()) {
            _levelx.m_5594_(
               null,
               BlockPos.m_274561_(x, y, z),
               (SoundEvent)ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("man:short_whisper_2")),
               SoundSource.AMBIENT,
               1.0F,
               1.0F
            );
         } else {
            _levelx.m_7785_(
               x, y, z, (SoundEvent)ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("man:short_whisper_2")), SoundSource.AMBIENT, 1.0F, 1.0F, false
            );
         }
      }
   }
}
